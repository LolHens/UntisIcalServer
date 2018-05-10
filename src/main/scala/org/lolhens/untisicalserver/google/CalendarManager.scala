package org.lolhens.untisicalserver.google

import java.time._

import cats.implicits._
import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.googleapis.services.json.AbstractGoogleJsonClientRequest
import com.google.api.client.http.{HttpHeaders, HttpRequestInitializer}
import com.google.api.services.calendar.model.{Calendar, CalendarListEntry, Event => GEvent}
import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.eval.{Task, TaskCircuitBreaker}
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.Event
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId._
import org.lolhens.untisicalserver.google.CalendarManager._
import org.lolhens.untisicalserver.ical.WeekOfYear
import org.lolhens.untisicalserver.util.GoogleConverters._
import org.lolhens.untisicalserver.util.Utils._

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.implicitConversions

case class CalendarManager(calendarService: CalendarService) {
  def listCalendars(): Task[Map[String, CalendarListEntry]] = Task {
    calendarService
      .calendarList()
      .list()
      .execute()
      .getItems
      .asScala
      .map(calendar => calendar.name -> calendar)
      .toMap
  }

  def createCalendar(name: String): Task[Calendar] = Task {
    val calendar = new Calendar()

    calendar.setSummary(name)

    calendarService
      .calendars()
      .insert(calendar)
      .execute()
  }.timeout(10.seconds)
    .onErrorRestartLoop(5) { (err, maxRetries, retry) =>
      if (maxRetries > 0) retry(maxRetries - 1).delayExecution(5.second)
      else Task.raiseError(err)
    }

  def deleteCalendar(calendar: CalendarId): Task[Unit] = batchRequest {
    calendarService
      .calendars()
      .delete(calendar.id)
  }(null)
    .onErrorRestartLoop(12) { (err, maxRetries, retry) =>
      if (maxRetries > 0) retry(maxRetries - 1).delayExecution(5.second)
      else Task.raiseError(err)
    }

  private def listAllEvents(calendar: CalendarId,
                            min: LocalDateTime = null,
                            max: LocalDateTime = null,
                            showDeleted: Boolean): Observable[GEvent] = {
    def pageEvents(pageToken: String): Task[(Option[String], List[GEvent])] =
      for {
        events <- Task {
          calendarService
            .events()
            .list(calendar.id)
            .setTimeMin(min.toGoogleDateTime)
            .setTimeMax(max.toGoogleDateTime)
            .setPageToken(pageToken)
            .setShowDeleted(showDeleted)
            .setSingleEvents(true)
            .execute()
        }
          .timeout(30.seconds)
          .onErrorRestartLoop(5) { (err, maxRetries, retry) =>
            if (maxRetries > 0) retry(maxRetries - 1).delayExecution(5.second)
            else Task.raiseError(err)
          }
        nextPageToken = Option(events.getNextPageToken)
        eventsList = events.getItems.asScala.toList
        //_ = println(s"page: ${showDeleted} " + eventsList)
      } yield
        nextPageToken -> eventsList

    val firstPageToken: String = null

    Observable.tailRecM(firstPageToken) { pageToken =>
      for {
        (nextPageToken, events) <- Observable.fromTask(pageEvents(pageToken))
        rec <-
          Observable.fromIterable(events).map(Either.right) ++
            Observable.fromIterable(nextPageToken).map(Either.left)
      } yield rec
    }
  }

  def listEvents(calendar: CalendarId,
                 min: LocalDateTime = null,
                 max: LocalDateTime = null): Observable[GEvent] =
    listAllEvents(calendar, min, max, showDeleted = false) //.filter(_.getStatus != "cancelled")

  def listDeletedEvents(calendar: CalendarId,
                        min: LocalDateTime,
                        max: LocalDateTime): Observable[GEvent] =
    listAllEvents(calendar, min, max, showDeleted = true).filter(_.getStatus == "cancelled")

  def listEvents(calendar: CalendarId,
                 date: LocalDate): Observable[GEvent] =
    listEvents(calendar, date.dayStart, date.dayStart.plusDays(1))

  def listEvents(calendar: CalendarId,
                 week: WeekOfYear): Observable[GEvent] =
    listEvents(calendar, week.startDate.dayStart, week.endDate.dayStart)

  def moveEvent(calendar: CalendarId, event: GEvent, destination: CalendarId)
               (implicit batch: BatchRequest = null): Task[Unit] = batchRequest {
    calendarService
      .events()
      .move(calendar.id, event.getId, destination.id)
  }

  def removeEventsCompletely(calendar: CalendarId, events: Observable[GEvent]): Task[Unit] = {
    (for {
      _ <- Observable.fromTask(events.take(10).countL)
        .filter(size => size >= 10)
      _ = println("tmp " + calendar.name)
      _ <- Observable.fromTask {
        createCalendar("(trash)").bracket { tmpCalendar =>
          events.bufferTumbling(2000).mapParallelUnordered(8) { events =>
            batched { implicit batch =>
              for {
                _ <- Task.sequence(
                  for (event <- events)
                    yield {
                      println("removing: " + event.toString.replaceAll("[^a-zA-Z0-9 :,;-_#+]", "").take(10000))
                      moveEvent(calendar, event, tmpCalendar.id)
                    }
                )
                _ = println("REMOVED!!!!!!!! " + calendar.name)
              } yield ()
            }(null)
          }.completedL
        }(tmpCalendar => deleteCalendar(tmpCalendar.id))
      }
    } yield ()).completedL
  }

  def purgeCalendar(calendar: CalendarId,
                    min: LocalDateTime = null,
                    max: LocalDateTime = null): Task[Unit] =
    removeEventsCompletely(calendar, listDeletedEvents(calendar, min, max))

  def purgeCalendar(calendar: CalendarId, week: WeekOfYear): Task[Unit] =
    purgeCalendar(calendar, week.startDate.dayStart, week.endDate.dayStart)

  private def openBatch(batch: BatchRequest): BatchRequest = Option(batch).getOrElse {
    new BatchRequest(
      GoogleNetHttpTransport.newTrustedTransport(),
      calendarService.getGoogleClientRequestInitializer.asInstanceOf[HttpRequestInitializer]
    )
  }

  val circuitBreaker: Task[TaskCircuitBreaker] = TaskCircuitBreaker(
    maxFailures = 2,
    resetTimeout = 10.seconds
  )

  private def closeBatch(openedBatch: BatchRequest, batch: BatchRequest): Task[Unit] = {
    if (batch == null && openedBatch.size() > 0)
      Task(openedBatch.execute())
        .onErrorRestartLoop(5) { (err, maxRetries, retry) =>
          if (maxRetries > 0) retry(maxRetries - 1).delayExecution(5.second)
          else Task.raiseError(err)
        }
    else Task.now()
  }

  private def batched(f: BatchRequest => Task[Unit])
                     (implicit batch: BatchRequest): Task[Unit] = {
    val openedBatch = openBatch(batch)
    for {
      _ <- f(openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield ()
  }

  private def batchRequest[T](request: AbstractGoogleJsonClientRequest[T])
                             (implicit batch: BatchRequest): Task[Unit] =
    batched { batch =>
      for {
        ci <- circuitBreaker
        r <- ci.protect(Task.now(request.queue(batch, emptyCallback)))
      } yield r
    }

  def clear(calendar: CalendarId)
           (implicit batch: BatchRequest = null): Task[Unit] = batchRequest {
    calendarService
      .calendars()
      .clear(calendar.id)
  }

  def removeEvent(calendar: CalendarId, event: GEvent)
                 (implicit batch: BatchRequest = null): Task[Unit] = batchRequest {
    calendarService
      .events()
      .delete(calendar.id, event.getId)
  }

  def removeEvents(calendar: CalendarId, events: List[GEvent])
                  (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      Task.gather(events.map(removeEvent(calendar, _)))
        .map(_ => ())
    }

  def addEvent(calendar: CalendarId, event: GEvent)
              (implicit batch: BatchRequest = null): Task[Unit] = batchRequest {
    calendarService
      .events()
      .insert(calendar.id, event)
  }

  def addEvents(calendar: CalendarId, events: List[GEvent])
               (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      Task.gather(events.map(addEvent(calendar, _)))
        .map(_ => ())
    }

  def patchEvent(calendar: CalendarId,
                 oldEvent: GEvent,
                 newEvent: GEvent)
                (implicit batch: BatchRequest = null): Task[Unit] = batchRequest {
    calendarService
      .events()
      .update(calendar.id, oldEvent.getId, newEvent)
  }

  def patchEvents(calendar: CalendarId, events: Map[GEvent, GEvent])
                 (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      Task.gather(events.map(e => patchEvent(calendar, e._1, e._2)))
        .map(_ => ())
    }

  private abstract class UpdateEventResult {
    def remainingOldEvents(events: List[GEvent]): List[GEvent]
  }

  private object UpdateEventResult {

    case class KeepOld(oldEvent: GEvent) extends UpdateEventResult {
      override def remainingOldEvents(events: List[GEvent]): List[GEvent] = events.filterNot(_ == oldEvent)
    }

    case class Replace(oldEvent: GEvent, newEvent: GEvent) extends UpdateEventResult {
      override def remainingOldEvents(events: List[GEvent]): List[GEvent] = events.filterNot(_ == oldEvent)
    }

    case class AddNew(newEvent: GEvent) extends UpdateEventResult {
      override def remainingOldEvents(events: List[GEvent]): List[GEvent] = events
    }

  }

  private def findEventUpdate(oldEvents: List[GEvent],
                              event: GEvent): UpdateEventResult = {
    val newEvent = Event.fromGEvent(event)

    def _keepOld = oldEvents.find { oldEvent =>
      Event.fromGEvent(oldEvent) == newEvent
    }.map(UpdateEventResult.KeepOld(_))

    def _replace = oldEvents.find { oldEvent =>
      oldEvent.getStart.toLocalDateTime == event.getStart.toLocalDateTime &&
        oldEvent.getEnd.toLocalDateTime == event.getEnd.toLocalDateTime
    }.map(UpdateEventResult.Replace(_, event))


    def _addNew = _keepOld.orElse(_replace).getOrElse(UpdateEventResult.AddNew(event))

    _addNew
  }

  def updateEvents(calendar: CalendarId,
                   oldEvents: List[GEvent],
                   newEvents: List[GEvent])
                  (implicit batch: BatchRequest = null): Task[Unit] =
    batched(implicit batch => Task {
      val eventUpdates = newEvents.map(findEventUpdate(oldEvents, _))
      val removeOld = eventUpdates.foldLeft(oldEvents) { (lastOldEvents, eventUpdate) =>
        eventUpdate match {
          case UpdateEventResult.KeepOld(oldEvent) =>
            //println("keep old")
            eventUpdate.remainingOldEvents(lastOldEvents)

          case UpdateEventResult.Replace(oldEvent, newEvent) =>
            println("replace " + Event.fromGEvent(oldEvent).line + " with " + Event.fromGEvent(newEvent).line)
            patchEvent(calendar, oldEvent, newEvent)
            eventUpdate.remainingOldEvents(lastOldEvents)

          case UpdateEventResult.AddNew(newEvent) =>
            println("add new")
            addEvent(calendar, newEvent)
            eventUpdate.remainingOldEvents(lastOldEvents)

        }
      }

      removeEvents(calendar, removeOld)
    })

  def updateDay(calendar: CalendarId,
                date: LocalDate,
                events: List[GEvent])
               (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      for {
        oldEvents <- listEvents(calendar, date).toListL
        _ <- updateEvents(calendar, oldEvents, filter(events, date))
      } yield ()
    }

  def updateWeek(calendar: CalendarId,
                 week: WeekOfYear,
                 events: List[GEvent])
                (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      println(week.startDate + " new: " + events.toString.replaceAll("\\n|\\r\\n", ""))
      for {
        _ <- purgeCalendar(calendar)
        _ = println("testtesttest " + calendar.name)
        oldEvents <- listEvents(calendar, week).toListL
        //_ = println(week.startDate + " old: " + oldEvents)
        _ <- updateEvents(calendar, oldEvents, filter(events, week))
      } yield ()
    }

  def readdWeek(calendar: CalendarId,
                week: WeekOfYear,
                events: List[GEvent])
               (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      for {
        oldEvents <- listEvents(calendar, week).toListL
        _ <- removeEvents(calendar, oldEvents)
        _ <- addEvents(calendar, filter(events, week))
        _ = println(s"week $week ${week.startDate}: removed ${oldEvents.size} events; adding ${events.size}")
      } yield ()
    }
}

object CalendarManager {
  private def emptyCallback[T] = new JsonBatchCallback[T] {
    override def onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders): Unit =
      println(e.getMessage)

    override def onSuccess(t: T, responseHeaders: HttpHeaders): Unit = ()
  }

  def filter(events: List[GEvent], min: LocalDateTime, max: LocalDateTime): List[GEvent] =
    events.filter { event =>
      val start = Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate).toLocalDateTime
      (min == null || start.isEqual(min) || start.isAfter(min)) &&
        (max == null || start.isEqual(max) || start.isBefore(max))
    }

  def filter(events: List[GEvent], date: LocalDate): List[GEvent] =
    filter(events, date.dayStart, date.dayStart.plusDays(1))

  def filter(events: List[GEvent], week: WeekOfYear): List[GEvent] =
    filter(events, week.startDate.dayStart, week.endDate.dayStart)

  case class CalendarId(id: String)(val name: String)

  object CalendarId {

    implicit class CalendarOps(val calendar: Calendar) extends AnyVal {
      def id: CalendarId = CalendarId(calendar.getId)(name)

      def name: String = calendar.getSummary
    }

    implicit class CalendarListEntryOps(val calendarListEntry: CalendarListEntry) extends AnyVal {
      def id: CalendarId = CalendarId(calendarListEntry.getId)(name)

      def name: String =
        Option(calendarListEntry.getSummaryOverride)
          .getOrElse(calendarListEntry.getSummary)
    }

  }

}
