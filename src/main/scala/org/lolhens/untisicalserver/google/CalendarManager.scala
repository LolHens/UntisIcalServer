package org.lolhens.untisicalserver.google

import java.time._
import java.util.{Date, TimeZone}

import com.google.api.client.googleapis.batch.BatchRequest
import com.google.api.client.googleapis.batch.json.JsonBatchCallback
import com.google.api.client.googleapis.json.GoogleJsonError
import com.google.api.client.http.HttpHeaders
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.{CalendarListEntry, Event, EventDateTime}
import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.eval.Task
import net.fortuna.ical4j.model.component.VEvent
import org.lolhens.untisicalserver.google.CalendarManager._
import org.lolhens.untisicalserver.ical.WeekOfYear

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
                 max: LocalDateTime = null): Task[List[Event]] = Task {
    calendarService
      .events()
      .list(calendar.getId)
      .setTimeMin(min)
      .setTimeMax(max)
      .execute()
      .getItems
      .asScala
      .toList
  }

  def listEvents(calendar: CalendarListEntry,
                 week: WeekOfYear): Task[List[Event]] =
    listEvents(calendar, week.localDateMin, week.localDateMax)

  def test(week: WeekOfYear) = ((week.localDateMin: LocalDateTime): DateTime, (week.localDateMax: LocalDateTime): DateTime)

  private def openBatch(batch: BatchRequest): BatchRequest = Option(batch).getOrElse(calendarService.batch())

  private def closeBatch(openedBatch: BatchRequest, batch: BatchRequest): Task[Unit] = {
    if (batch == null && openedBatch.size() > 0) Task(openedBatch.execute())
    else Task.now()
  }

  def removeEvent(calendar: CalendarListEntry, event: Event, batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    calendarService
      .events()
      .delete(calendar.getId, event.getId)
      .queue(openedBatch, emptyCallback)
    closeBatch(openedBatch, batch)
  }

  def removeEvents(calendar: CalendarListEntry, events: List[Event], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    Task.sequence(events.map(removeEvent(calendar, _, openedBatch)))
    closeBatch(openedBatch, batch)
  }

  def addEvent(calendar: CalendarListEntry, event: Event, batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    calendarService
      .events()
      .insert(calendar.getId, event)
      .queue(openedBatch, emptyCallback)
    closeBatch(openedBatch, batch)
  }

  def addEvents(calendar: CalendarListEntry, events: List[Event], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    Task.sequence(events.map(addEvent(calendar, _, openedBatch)))
    closeBatch(openedBatch, batch)
  }

  def updateWeek(calendar: CalendarListEntry, week: WeekOfYear, events: List[Event], batch: BatchRequest = null): Task[Unit] = {
    val openedBatch = openBatch(batch)
    for {
      oldEvents <- listEvents(calendar, week)
      _ <- removeEvents(calendar, oldEvents, openedBatch)
      _ <- addEvents(calendar, filter(events, week), openedBatch)
      _ <- closeBatch(openedBatch, batch)
    } yield ()
  }
}

object CalendarManager {
  private lazy val parallelism = Runtime.getRuntime.availableProcessors() + 2

  private def emptyCallback[T] = new JsonBatchCallback[T] {
    override def onFailure(e: GoogleJsonError, responseHeaders: HttpHeaders) = ()
    override def onSuccess(t: T, responseHeaders: HttpHeaders) = ()
  }

  private def parallel[T](tasks: List[Task[T]], unordered: Boolean = false): Task[List[T]] = Task.sequence {
    tasks.sliding(parallelism, parallelism).map(e =>
      if (unordered) Task.gatherUnordered(e)
      else Task.gather(e)
    )
  }.map(_.toList.flatten)

  def calendarName(calendar: CalendarListEntry): String =
    Option(calendar.getSummaryOverride).getOrElse(calendar.getSummary)

  private def zoneOffset: ZoneOffset = OffsetDateTime.now().getOffset

  implicit def dateTimeFromLocalDateTime(localDateTime: LocalDateTime): DateTime =
    if (localDateTime == null) null else {
      val offset = zoneOffset
      new DateTime(Date.from(localDateTime.toInstant(offset)), TimeZone.getTimeZone(offset))
    }

  implicit def localDateTimeFromLocalDate(localDate: LocalDate): LocalDateTime =
    LocalDateTime.of(localDate, LocalTime.MIDNIGHT)

  implicit def eventDateTimeFromLocalDateTime(localDateTime: LocalDateTime): EventDateTime = {
    val eventDateTime = new EventDateTime()
    eventDateTime.setDateTime(localDateTime)
    eventDateTime
  }

  def localDateTime(dateTime: DateTime): LocalDateTime = LocalDateTime.ofEpochSecond(
    dateTime.getValue / 1000,
    (dateTime.getValue % 1000).toInt * 1000,
    ZoneOffset.ofHoursMinutes(dateTime.getTimeZoneShift / 60, dateTime.getTimeZoneShift % 60)
  )

  def toEvent(vEvent: VEvent): Event = {
    val event = new Event()

    import org.lolhens.untisicalserver.util.Utils._
    event.setSummary(vEvent.getSummary.getValue)
    event.setDescription(vEvent.getDescription.getValue)
    event.setLocation(vEvent.getLocation.getValue)
    event.setStart(vEvent.getStartDate.getDate.toLocalDateTime)
    event.setEnd(vEvent.getEndDate.getDate.toLocalDateTime)

    event
  }

  def toEvents(vEvents: List[VEvent]): List[Event] = vEvents.map(toEvent)

  def filter(events: List[Event], min: LocalDateTime, max: LocalDateTime): List[Event] =
    events.filter { event =>
      val start = localDateTime(Option(event.getStart.getDateTime).getOrElse(event.getStart.getDate))
      (min == null || start.isEqual(min) || start.isAfter(min)) &&
        (max == null || start.isEqual(max) || start.isBefore(max))
    }

  def filter(events: List[Event], week: WeekOfYear): List[Event] =
    filter(events, week.localDateMin, week.localDateMax)
}
