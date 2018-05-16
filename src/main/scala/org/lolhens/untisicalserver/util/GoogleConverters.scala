package org.lolhens.untisicalserver.util

import java.time._
import java.time.temporal.{ChronoField, TemporalField}
import java.util.{Date, TimeZone}

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.EventDateTime

object GoogleConverters {

  implicit class GoogleDateTimeOs(val dateTime: DateTime) extends AnyVal {
    def toDateTime: OffsetDateTime = {
      val zoneOffset = ZoneOffset.ofTotalSeconds(dateTime.getTimeZoneShift * 60)
      Instant.ofEpochMilli(dateTime.getValue).atOffset(zoneOffset)
    }

    def toEventDateTime: EventDateTime = {
      val eventDateTime = new EventDateTime()
      eventDateTime.setDateTime(dateTime)
      eventDateTime
    }
  }

  implicit class EventDateTimeOps(val eventDateTime: EventDateTime) extends AnyVal {
    def toGoogleDateTime: DateTime = Option(eventDateTime.getDateTime).getOrElse(eventDateTime.getDate)

    def toDateTime: OffsetDateTime = toGoogleDateTime.toDateTime
  }

  implicit class GoogleOffsetDateTimeOps(val dateTime: OffsetDateTime) extends AnyVal {
    def toGoogleDateTime: DateTime =
      if (dateTime == null) null
      else new DateTime(Date.from(dateTime.toInstant), TimeZone.getTimeZone(dateTime.getOffset))
  }

}
