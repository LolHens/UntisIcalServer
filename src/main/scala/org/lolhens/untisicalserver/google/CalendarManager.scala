package org.lolhens.untisicalserver.google

import java.time._

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.{HttpHeaders, HttpRequestInitializer}
import com.google.api.services.calendar.model.{CalendarListEntry, Event => GEvent}
import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.eval.Task
import org.lolhens.untisicalserver.data.Event
import org.lolhens.untisicalserver.google.CalendarManager._
import org.lolhens.untisicalserver.ical.WeekOfYear
import org.lolhens.untisicalserver.util.GoogleConverters._
import org.lolhens.untisicalserver.util.Utils._

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.language.implicitConversions

case class CalendarManager(calendarService: CalendarService) {
  def listCalendars(): Task[List[CalendarListEntry]] = Task {
    calendarService
      .calendarList()
      .list()
      .execute()
      .getItems
      .asScala
      .toList
  }

  def listEvents(calendar: CalendarListEntry,
                 min: LocalDateTime = null,
                 max: LocalDateTime = null): Task[List[GEvent]] = Task {
    @tailrec
    def getEventsRec(items: List[GEvent] = Nil,
                     nextPageToken: Option[String] = Some(null)): List[GEvent] =
      nextPageToken match {
        case Some(pageToken) =>
          val events =
            calendarService
              .events()
              .list(calendar.getId)
              .setTimeMin(min.toGoogleDateTime)
              .setTimeMax(max.toGoogleDateTime)
              .setPageToken(pageToken)
              .execute()

          getEventsRec(items ++ events.getItems.asScala.toList, Option(events.getNextPageToken))

        case None =>
          items
      }

    getEventsRec()
  }

  def listEvents(calendar: CalendarListEntry,
                 date: LocalDate): Task[List[GEvent]] =
    listEvents(calendar, date.dayStart, date.dayStart.plusDays(1))

  def listEvents(calendar: CalendarListEntry,
                 week: WeekOfYear): Task[List[GEvent]] =
    listEvents(calendar, week.localDateMin.dayStart, week.localDateMax.dayStart)

  private def openBatch(batch: BatchRequest): BatchRequest = Option(batch).getOrElse {
    val batch = new BatchRequest(GoogleNetHttpTransport.newTrustedTransport(),
      calendarService.getGoogleClientRequestInitializer.asInstanceOf[HttpRequestInitializer])

    //calendarService.batch()

    batch
  }

  private def closeBatch(openedBatch: BatchRequest, batch: BatchRequest): Task[Unit] = {
    if (batch == null && openedBatch.size() > 0) Task(openedBatch.execute())
    else Task.now()
  }

  private def batched(f: BatchRequest => Task[Unit])(implicit batch: BatchRequest): Task[Unit] = {
    val openedBatch = openBatch(batch)
    for {
      _ <- f(openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield ()
  }

  def clear(calendar: CalendarListEntry)
           (implicit batch: BatchRequest = null): Task[Unit] = batched(batch => Task.now {
    calendarService
      .calendars()
      .clear(calendar.getId)
      .queue(batch, emptyCallback)
  })

  def removeEvent(calendar: CalendarListEntry, event: GEvent)
                 (implicit batch: BatchRequest = null): Task[Unit] =
    batched(batch => Task.now {
      calendarService
        .events()
        .delete(calendar.getId, event.getId)
        .queue(batch, emptyCallback)
    })

  def removeEvents(calendar: CalendarListEntry, events: List[GEvent])
                  (implicit batch: BatchRequest = null): Task[Unit] =
    batched { batch =>
      Task.gather(events.map(removeEvent(calendar, _)))
        .map(_ => ())
    }

  def addEvent(calendar: CalendarListEntry, event: GEvent)
              (implicit batch: BatchRequest = null): Task[Unit] =
    batched(batch => Task.now {
      calendarService
        .events()
        .insert(calendar.getId, event)
        .queue(batch, emptyCallback)
    })

  def addEvents(calendar: CalendarListEntry, events: List[GEvent])
               (implicit batch: BatchRequest = null): Task[Unit] =
    batched { batch =>
      Task.gather(events.map(addEvent(calendar, _)))
        .map(_ => ())
    }

  def patchEvent(calendar: CalendarListEntry,
                 oldEvent: GEvent,
                 newEvent: GEvent)
                (implicit batch: BatchRequest = null): Task[Unit] =
    batched(batch => Task.now {
      calendarService
        .events()
        .update(calendar.getId, oldEvent.getId, newEvent)
        .queue(batch, emptyCallback)
    })

  def patchEvents(calendar: CalendarListEntry, events: Map[GEvent, GEvent])
                 (implicit batch: BatchRequest = null): Task[Unit] =
    batched { batch =>
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

  def updateEvents(calendar: CalendarListEntry,
                   oldEvents: List[GEvent],
                   newEvents: List[GEvent])
                  (implicit batch: BatchRequest = null): Task[Unit] = batched(batch => Task {
    val eventUpdates = newEvents.map(findEventUpdate(oldEvents, _))
    val removeOld = eventUpdates.foldLeft(oldEvents) { (lastOldEvents, eventUpdate) =>
      eventUpdate match {
        case UpdateEventResult.KeepOld(oldEvent) =>
          println("keep old")
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

  def updateDay(calendar: CalendarListEntry,
                date: LocalDate,
                events: List[GEvent])
               (implicit batch: BatchRequest = null): Task[Unit] = batched { batch =>
    for {
      oldEvents <- listEvents(calendar, date)
      _ <- updateEvents(calendar, oldEvents, filter(events, date))
    } yield ()
  }

  def updateWeek(calendar: CalendarListEntry,
                 week: WeekOfYear,
                 events: List[GEvent])
                (implicit batch: BatchRequest = null): Task[Unit] = batched { batch =>
    for {
      oldEvents <- listEvents(calendar, week)
      _ <- updateEvents(calendar, oldEvents, filter(events, week))
    } yield ()
  }

  def updateWeek2(calendar: CalendarListEntry,
                  week: WeekOfYear,
                  events: List[GEvent])
                 (implicit batch: BatchRequest = null): Task[Unit] = batched { batch =>
    for {
      oldEvents <- listEvents(calendar, week)
      _ <- removeEvents(calendar, oldEvents)
      _ <- addEvents(calendar, filter(events, week))

      //newEvents <- listEvents(calendar, week)
      _ = println(s"week $week ${week.localDateMin}: removed ${oldEvents.size} events; adding ${events.size}")
    } yield ()
  }
}

object CalendarManager {
  private def emptyCallback[T] = new JsonBatchCallback[T] {
    override def onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders): Unit =
      println(e.getMessage)

    override def onSuccess(t: T, responseHeaders: HttpHeaders): Unit = ()
  }

  def calendarName(calendar: CalendarListEntry): String =
    Option(calendar.getSummaryOverride).getOrElse(calendar.getSummary)

  def filter(events: List[GEvent], min: LocalDateTime, max: LocalDateTime): List[GEvent] =
    events.filter { event =>
      val start = Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate).toLocalDateTime
      (min == null || start.isEqual(min) || start.isAfter(min)) &&
        (max == null || start.isEqual(max) || start.isBefore(max))
    }

  def filter(events: List[GEvent], date: LocalDate): List[GEvent] =
    filter(events, date.dayStart, date.dayStart.plusDays(1))

  def filter(events: List[GEvent], week: WeekOfYear): List[GEvent] =
    filter(events, week.localDateMin.dayStart, week.localDateMax.dayStart)
}
