package org.lolhens.untisicalserver.util

import java.time.{OffsetDateTime, ZoneId}

import net.fortuna.ical4j.model._

object ICalConverters {
  private lazy val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()

  private def timeZone(zoneId: ZoneId): TimeZone = timeZoneRegistry.getTimeZone(zoneId.getId)

  implicit class InstantToICalDateTime(val dateTime: OffsetDateTime) extends AnyVal {
    def toICalDateTime: DateTime =
      if (dateTime == null) null
      else {
        val icalDateTime = new DateTime()
        icalDateTime.setTimeZone(timeZone(dateTime.getOffset))
        icalDateTime.setTime(dateTime.toInstant.toEpochMilli)
        icalDateTime
      }
  }

}
