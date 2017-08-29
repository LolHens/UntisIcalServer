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

  def clear(calendar: CalendarListEntry, batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    calendarService
      .calendars()
      .clear(calendar.getId)
      .queue(openedBatch, emptyCallback)
    closeBatch(openedBatch, batch)
  }

  def removeEvent(calendar: CalendarListEntry, event: GEvent, batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    calendarService
      .events()
      .delete(calendar.getId, event.getId)
      .queue(openedBatch, emptyCallback)
    closeBatch(openedBatch, batch)
  }

  def removeEvents(calendar: CalendarListEntry, events: List[GEvent], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    Task.sequence(events.map(removeEvent(calendar, _, openedBatch)))
    closeBatch(openedBatch, batch)
  }

  def addEvent(calendar: CalendarListEntry, event: GEvent, batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    calendarService
      .events()
      .insert(calendar.getId, event)
      .queue(openedBatch, emptyCallback)
    closeBatch(openedBatch, batch)
  }

  def addEvents(calendar: CalendarListEntry, events: List[GEvent], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    Task.sequence(events.map(addEvent(calendar, _, openedBatch)))
    closeBatch(openedBatch, batch)
  }

  def updateWeek(calendar: CalendarListEntry, week: WeekOfYear, events: List[GEvent], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    for {
      oldEvents <- listEvents(calendar, week)
      _ = println(s"removing ${oldEvents.size} events; adding ${events.size}")
      _ <- removeEvents(calendar, oldEvents, openedBatch)
      _ <- addEvents(calendar, filter(events, week), openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield ()
  }
}

object CalendarManager {
  private lazy val parallelism = Runtime.getRuntime.availableProcessors() + 2

  private def emptyCallback[T] = new JsonBatchCallback[T] {
    override def onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders): Unit = ()
    override def onSuccess(t: T, responseHeaders: HttpHeaders): Unit = ()
  }

  private def parallel[T](tasks: List[Task[T]], unordered: Boolean = false): Task[List[T]] = Task.sequence {
    tasks.sliding(parallelism, parallelism).map(e =>
      if (unordered) Task.gatherUnordered(e)
      else Task.gather(e)
    )
  }.map(_.toList.flatten)

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
