package org.lolhens.untisicalserver.util

import java.time.OffsetDateTime

import net.fortuna.ical4j.model.DateTime

object ICalConverters {

  implicit class InstantToICalDateTime(val dateTime: OffsetDateTime) extends AnyVal {
    def toICalDateTime: DateTime =
      if (dateTime == null) null
      else {
        println(dateTime.toString)
        new DateTime(dateTime.toString)
      }
  }

}
