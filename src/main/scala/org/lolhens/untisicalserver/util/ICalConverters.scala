package org.lolhens.untisicalserver.util

import java.time.LocalDateTime

import net.fortuna.ical4j.model.DateTime
import org.lolhens.untisicalserver.util.Utils._

object ICalConverters {

  implicit class RichICalLocalDateTime(val localDateTime: LocalDateTime) extends AnyVal {
    def toICalDateTime: DateTime =
      if (localDateTime == null) null
      else new DateTime(localDateTime.toDate)
  }

}
