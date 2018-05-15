package org.lolhens.untisicalserver.data

import java.time.Instant

import com.google.api.services.calendar.model.{Event => GEvent}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property._
import org.lolhens.untisicalserver.util.GoogleConverters._
import org.lolhens.untisicalserver.util.ICalConverters._
import org.lolhens.untisicalserver.util.Utils
import org.lolhens.untisicalserver.util.Utils._

case class Event(summary: String,
                 description: String,
                 location: String,
                 start: Instant,
                 end: Instant) {
  def toVEvent: VEvent = {
    val vEvent = new VEvent()
    vEvent.addProperty(new Summary(summary))
    vEvent.addProperty(new Description(description))
    vEvent.addProperty(new Location(location))
    vEvent.addProperty(new DtStart(start.atOffset(Utils.zoneOffset).toICalDateTime))
    vEvent.addProperty(new DtEnd(end.atOffset(Utils.zoneOffset).toICalDateTime))
    vEvent
  }

  def toGEvent: GEvent = {
    val gEvent = new GEvent()

    gEvent.setSummary(summary)
    gEvent.setDescription(description)
    gEvent.setLocation(location)
    gEvent.setStart(start.atOffset(Utils.zoneOffset).toGoogleDateTime.toEventDateTime)
    gEvent.setEnd(end.atOffset(Utils.zoneOffset).toGoogleDateTime.toEventDateTime)

    gEvent
  }

  def line: String = toString
    .replaceAll("\r|\n", "")
}

object Event {
  def fromVEvent(vEvent: VEvent): Event = Event(
    vEvent.getSummary.getValue,
    vEvent.getDescription.getValue,
    vEvent.getLocation.getValue,
    vEvent.getStartDate.getDate.toInstant,
    vEvent.getEndDate.getDate.toInstant
  )

  def fromGEvent(gEvent: GEvent): Event = Event(
    gEvent.getSummary,
    gEvent.getDescription,
    gEvent.getLocation,
    gEvent.getStart.toGoogleDateTime.toDateTime.toInstant,
    gEvent.getEnd.toGoogleDateTime.toDateTime.toInstant
  )
}
