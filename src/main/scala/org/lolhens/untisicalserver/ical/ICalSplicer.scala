package org.lolhens.untisicalserver.ical

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.CalScale
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Version

import scala.collection.JavaConversions._

/**
  * Created by pierr on 30.08.2016.
  */
object ICalSplicer {
  def apply(calendars: List[Calendar]): Calendar = {
    val calendar = new Calendar()

    calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"))
    calendar.getProperties().add(Version.VERSION_2_0)
    calendar.getProperties().add(CalScale.GREGORIAN)

    calendar.getComponents().addAll(calendars.flatMap(_.getComponents()))

    calendar
  }
}