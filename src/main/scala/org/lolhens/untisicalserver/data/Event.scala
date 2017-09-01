package org.lolhens.untisicalserver.data

import java.time.LocalDateTime

import com.google.api.services.calendar.model.{Event => GEvent}
import net.fortuna.ical4j.model.component.VEvent
import org.lolhens.untisicalserver.util.GoogleConverters._
import org.lolhens.untisicalserver.util.ICalConverters._
import org.lolhens.untisicalserver.util.Utils._

case class Event(summary: String,
                 description: String,
                 location: String,
                 start: LocalDateTime,
                 end: LocalDateTime) {
  def toVEvent: VEvent = {
    val vEvent = new VEvent()
    vEvent.getSummary.setValue(summary)
    vEvent.getDescription.setValue(description)
    vEvent.getLocation.setValue(location)
    vEvent.getStartDate.setDate(start.toICalDate)
    vEvent.getEndDate.setDate(end.toICalDate)
    vEvent
  }

  def toGEvent: GEvent = {
    val gEvent = new GEvent()

    gEvent.setSummary(summary)
    gEvent.setDescription(description)
    gEvent.setLocation(location)
    gEvent.setStart(start.toGoogleDateTime.toEventDateTime)
    gEvent.setEnd(end.toGoogleDateTime.toEventDateTime)

    gEvent
  }

  def line: String = toString
    .replaceAllLiterally("\r", "")
    .replaceAllLiterally("\n", "")
}

object Event {
  def fromVEvent(vEvent: VEvent): Event = Event(
    vEvent.getSummary.getValue,
    vEvent.getDescription.getValue,
    vEvent.getLocation.getValue,
    vEvent.getStartDate.getDate.toLocalDateTime,
    vEvent.getEndDate.getDate.toLocalDateTime
  )

  def fromGEvent(gEvent: GEvent): Event = Event(
    gEvent.getSummary,
    gEvent.getDescription,
    gEvent.getLocation,
    gEvent.getStart.toGoogleDateTime.toLocalDateTime,
    gEvent.getEnd.toGoogleDateTime.toLocalDateTime
  )
}
