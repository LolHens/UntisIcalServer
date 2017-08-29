package org.lolhens.untisicalserver.util

import java.time.{LocalDateTime, ZoneOffset}
import java.util.{Date, TimeZone}

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.EventDateTime

object GoogleConverters {

  implicit class RichDateTime(val dateTime: DateTime) extends AnyVal {
    def toLocalDateTime: LocalDateTime = LocalDateTime.ofEpochSecond(
      dateTime.getValue / 1000,
      (dateTime.getValue % 1000).toInt * 1000,
      ZoneOffset.ofHoursMinutes(dateTime.getTimeZoneShift / 60, dateTime.getTimeZoneShift % 60)
    )

    def toEventDateTime: EventDateTime = {
      val eventDateTime = new EventDateTime()
      eventDateTime.setDateTime(dateTime)
      eventDateTime
    }
  }

  implicit class RichEventDateTime(val eventDateTime: EventDateTime) extends AnyVal {
    def toGoogleDateTime: DateTime = Option(eventDateTime.getDateTime).getOrElse(eventDateTime.getDate)
  }

  implicit class RichGoogleLocalDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    def toGoogleDateTime: DateTime =
      if (localDateTime == null) null
      else {
        val offset = Utils.zoneOffset
        new DateTime(Date.from(localDateTime.toInstant(offset)), TimeZone.getTimeZone(offset))
      }
  }

}
