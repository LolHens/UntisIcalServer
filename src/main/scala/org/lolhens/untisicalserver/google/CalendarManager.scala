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
                            min: OffsetDateTime = null,
                            max: OffsetDateTime = null,
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
          .onErrorRestartLoop(3) { (err, maxRetries, retry) =>
            if (maxRetries > 0) retry(maxRetries - 1).delayExecution(2.second)
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
                 min: OffsetDateTime = null,
                 max: OffsetDateTime = null): Observable[GEvent] =
    listAllEvents(calendar, min, max, showDeleted = false).filter(_.getStatus != "cancelled")

  def listDeletedEvents(calendar: CalendarId,
                        min: OffsetDateTime,
                        max: OffsetDateTime): Observable[GEvent] =
    listAllEvents(calendar, min, max, showDeleted = true).filter(e =>
      e.getStatus == "cancelled" &&
        !Option(e.getEnd)
          .flatMap(e => Option(e.getDate))
          .map(_.toString)
          .contains("2000-01-02")
    )

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

  def purgeEvents(calendar: CalendarId,
                  events: Observable[GEvent],
                  threshold: Int = 10): Task[Unit] = {
    (for {
      _ <- Observable.fromTask(events.take(threshold).countL)
        .filter(size => size >= threshold)
      _ = println("purging " + calendar.name)
      _ <- Observable.fromTask {
        createCalendar("(trash)").bracket { tmpCalendar =>
          events.bufferTumbling(1000).mapParallelUnordered(4) { events =>
            batched { implicit batch =>
              for {
                _ <- Task.sequence(
                  for (event <- events)
                    yield {
                      println("removing: " + event.toString.replaceAll("\\n|\\r\\n", "").take(1000))
                      moveEvent(calendar, event, tmpCalendar.id)
                    }
                )
                _ = println("purged " + calendar.name)
              } yield ()
            }(null)
          }.completedL
        }(tmpCalendar => deleteCalendar(tmpCalendar.id))
      }
    } yield ()).completedL
  }

  def purgeCalendar(calendar: CalendarId,
                    min: OffsetDateTime = null,
                    max: OffsetDateTime = null): Task[Unit] =
    purgeEvents(calendar, listDeletedEvents(calendar, min, max))

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
      (for {
        //ci <- circuitBreaker
        r <- Task(openedBatch.execute())
      } yield r)
        .onErrorRestartLoop(3) { (err, maxRetries, retry) =>
          if (maxRetries > 0) retry(maxRetries - 1).delayExecution(5.second)
          else Task.raiseError(err)
        }
    else Task.now()
  }

  private def batched[T](f: BatchRequest => Task[T])
                        (implicit batch: BatchRequest): Task[T] = {
    val openedBatch = openBatch(batch)
    for {
      r <- f(openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield r
  }

  private def batchRequest[T](request: AbstractGoogleJsonClientRequest[T])
                             (implicit batch: BatchRequest): Task[Unit] =
    batched { batch =>
      for {
        //ci <- circuitBreaker
        r <- Task(request.queue(batch, emptyCallback))
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

      override def toString: String = s"KeepOld(${Event.fromGEvent(oldEvent).line})"
    }

    case class Replace(oldEvent: GEvent, newEvent: GEvent) extends UpdateEventResult {
      override def remainingOldEvents(events: List[GEvent]): List[GEvent] = events.filterNot(_ == oldEvent)

      override def toString: String = s"KeepOld(${Event.fromGEvent(oldEvent).line},${Event.fromGEvent(newEvent).line})"
    }

    case class AddNew(newEvent: GEvent) extends UpdateEventResult {
      override def remainingOldEvents(events: List[GEvent]): List[GEvent] = events

      override def toString: String = s"KeepOld(${Event.fromGEvent(newEvent).line})"
    }

  }

  private def findEventUpdate(oldEvents: List[GEvent],
                              event: GEvent): UpdateEventResult = {
    val newEvent = Event.fromGEvent(event)

    def _keepOld: Option[UpdateEventResult] = oldEvents.find { oldEvent =>
      Event.fromGEvent(oldEvent) == newEvent
    }.map(UpdateEventResult.KeepOld(_))

    def _replace: Option[UpdateEventResult] = oldEvents.find { oldEvent =>
      oldEvent.getStart.toDateTime == event.getStart.toDateTime &&
        oldEvent.getEnd.toDateTime == event.getEnd.toDateTime
    }.map(UpdateEventResult.Replace(_, event))


    def _addNew: UpdateEventResult =
      _keepOld.orElse(_replace).getOrElse(UpdateEventResult.AddNew(event))

    _addNew
  }

  def updateEvents(calendar: CalendarId,
                   oldEvents: List[GEvent],
                   newEvents: List[GEvent])
                  (implicit batch: BatchRequest = null): Task[Unit] =
    batched { implicit batch =>
      val eventUpdates: Observable[UpdateEventResult] =
        Observable.fromIterable(newEvents)
          .mapTask(newEvent => Task(findEventUpdate(oldEvents, newEvent)))

      for {
        /*e <- eventUpdates.toListL
        _ = if (e.exists(u => !u.isInstanceOf[UpdateEventResult.KeepOld])) {
          println(e.mkString("\n"))
          println(oldEvents.map(Event.fromGEvent).map(_.line).mkString("\n"))
        }*/
        keep <- eventUpdates.mapTask[Option[GEvent]] {
          case UpdateEventResult.KeepOld(oldEvent) =>
            Task.now(Some(oldEvent))

          case UpdateEventResult.Replace(oldEvent, newEvent) =>
            for {
              _ <- patchEvent(calendar, oldEvent, newEvent)
              _ = println("replaced " + Event.fromGEvent(oldEvent).line + " with " + Event.fromGEvent(newEvent).line)
            } yield Some(oldEvent)

          case UpdateEventResult.AddNew(newEvent) =>
            for {
              _ <- addEvent(calendar, newEvent)
              _ = println("added new " + Event.fromGEvent(newEvent).line)
            } yield None
        }
          .flatMap(Observable.fromIterable(_))
          .toListL

        remove = oldEvents diff keep

        _ <- removeEvents(calendar, remove)
      } yield ()
    }

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
      for {
        _ <- purgeCalendar(calendar)
        //_ = println("Updating " + calendar.name + " " + week.startDate)
        //_ = println(events.toString().replace("\n", ""))
        oldEvents <- listEvents(calendar, week).toListL
        //_ = println(oldEvents)
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
      println(e.getMessage + " " + e.getErrors.asScala.toList.map(e => e.getMessage + ": " + e.getReason + s" (${e.toPrettyString.replaceAll("\\n|\\r\\n", "")})"))

    override def onSuccess(t: T, responseHeaders: HttpHeaders): Unit = ()
  }

  def filter(events: List[GEvent], min: OffsetDateTime, max: OffsetDateTime): List[GEvent] =
    events.filter { event =>
      val start = Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate).toDateTime
      (min == null || start.isEqual(min) || start.isAfter(min)) &&
        (max == null || start.isEqual(max) || start.isBefore(max))
    }

  def filter(events: List[GEvent], date: LocalDate): List[GEvent] =
    filter(events, date.dayStart, date.dayStart.plusDays(1))

  def filter(events: List[GEvent], week: WeekOfYear): List[GEvent] =
    filter(events, week.startDate.dayStart, week.endDate.dayStart)

  case class CalendarId(id: String)(val name: String)

  object CalendarId {

    implicit class CalendarIdOps(val calendar: Calendar) extends AnyVal {
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
