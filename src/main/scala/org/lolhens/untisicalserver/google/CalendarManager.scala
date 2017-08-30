package org.lolhens.untisicalserver.google

import java.time._

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.HttpHeaders
import com.google.api.services.calendar.model.{CalendarListEntry, Event => GEvent}
import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.eval.Task
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
                 week: WeekOfYear): Task[List[GEvent]] =
    listEvents(calendar, week.localDateMin.midnight, week.localDateMax.midnight)

  private def openBatch(batch: BatchRequest): BatchRequest = Option(batch).getOrElse(calendarService.batch())

  private def closeBatch(openedBatch: BatchRequest, batch: BatchRequest): Task[Unit] = {
    if (batch == null && openedBatch.size() > 0) Task(openedBatch.execute())
    else Task.now()
  }

  private def batched(batch: BatchRequest)(f: (BatchRequest) => Task[Unit]): Task[Unit] = {
    val openedBatch = openBatch(batch)
    for {
      _ <- f(openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield ()
  }

  def clear(calendar: CalendarListEntry, batch: BatchRequest = null): Task[Unit] = batched(batch)(batch => Task.now {
    calendarService
      .calendars()
      .clear(calendar.getId)
      .queue(batch, emptyCallback)
  })

  def removeEvent(calendar: CalendarListEntry, event: GEvent, batch: BatchRequest = null): Task[Unit] =
    batched(batch)(batch => Task.now {
      calendarService
        .events()
        .delete(calendar.getId, event.getId)
        .queue(batch, emptyCallback)
    })

  def removeEvents(calendar: CalendarListEntry, events: List[GEvent], batch: BatchRequest = null): Task[Unit] =
    batched(batch) { batch =>
      Task.gather(events.map(removeEvent(calendar, _, batch)))
        .map(_ => ())
    }

  def addEvent(calendar: CalendarListEntry, event: GEvent, batch: BatchRequest = null): Task[Unit] =
    batched(batch)(batch => Task.now {
      calendarService
        .events()
        .insert(calendar.getId, event)
        .queue(batch, emptyCallback)
    })

  def addEvents(calendar: CalendarListEntry, events: List[GEvent], batch: BatchRequest = null): Task[Unit] =
    batched(batch) { batch =>
      Task.gather(events.map(addEvent(calendar, _, batch)))
        .map(_ => ())
    }

  def updateWeek(calendar: CalendarListEntry,
                 week: WeekOfYear,
                 events: List[GEvent],
                 batch: BatchRequest = null): Task[Unit] = batched(batch) { batch =>
    for {
      oldEvents <- listEvents(calendar, week)
      _ <- removeEvents(calendar, oldEvents, batch)
      _ <- addEvents(calendar, filter(events, week), batch)

      newEvents <- listEvents(calendar, week)
      _ = println(s"week $week ${week.localDateMin}: removed ${oldEvents.size} events; adding ${events.size}; added ${newEvents.size} events")
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

  def filter(events: List[GEvent], week: WeekOfYear): List[GEvent] =
    filter(events, week.localDateMin.midnight, week.localDateMax.midnight)
}
