package org.lolhens.untisicalserver.data

import java.io.StringReader

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.{CalScale, ProdId, Version}
import net.fortuna.ical4j.model.{Calendar => ICalCalendar}

import scala.collection.JavaConverters._
import scala.util.Try

case class Calendar(events: List[Event]) {
  def toICalCalendar: ICalCalendar = {
    val calendar = new ICalCalendar()

    calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"))
    calendar.getProperties().add(Version.VERSION_2_0)
    calendar.getProperties().add(CalScale.GREGORIAN)

    calendar.getComponents.addAll(events.map(_.toVEvent).asJava)

    calendar
  }

  def icalString: String = toICalCalendar.toString

  def ++(calendar: Calendar): Calendar = Calendar(events ++ calendar.events)
}

object Calendar {
  val empty = Calendar(Nil)

  def fromICalCalendar(iCalCalendar: ICalCalendar): Calendar = Calendar(
    iCalCalendar.getComponents.asScala.toList.collect {
      case vEvent: VEvent => Event.fromVEvent(vEvent)
    }
  )

  def parse(string: String): Try[Calendar] = Try(synchronized {
    fromICalCalendar(new CalendarBuilder().build(new StringReader(string)))
  })
}
