package org.lolhens.untisicalserver.util

import java.time.LocalDateTime

import net.fortuna.ical4j.model.Date
import org.lolhens.untisicalserver.util.Utils._

object ICalConverters {

  implicit class RichICalLocalDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    def toICalDate: Date =
      if (localDateTime == null) null
      else new Date(localDateTime.toDate)
  }

}
