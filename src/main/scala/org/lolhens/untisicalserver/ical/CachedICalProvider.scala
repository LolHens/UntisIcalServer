package org.lolhens.untisicalserver.ical

import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.{CachedValue, SchoolClass}

import scala.concurrent.duration.Duration

/**
  * Created by pierr on 31.08.2016.
  */
class CachedICalProvider(schoolClass: SchoolClass,
                         val timeout: Duration) extends ICalProvider(schoolClass) {
  private val cachedCalendar = CachedValue(super.apply(), timeout)

  override def apply(): Calendar = cachedCalendar()
}
