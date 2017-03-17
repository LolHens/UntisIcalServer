package org.lolhens.untisicalserver.ical

import akka.actor.Actor
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.ical.CalendarActor._

/**
  * Created by pierr on 17.03.2017.
  */
class CalendarActor extends Actor {
  var calendars: Map[WeekOfYear, Calendar] = Map()

  override def receive: Receive = {
    case UpdateCalendar(week, calendar) =>
      calendars = calendars + (week -> calendar)

    case GetCalendar =>
      val merged =

      sender() ! null

    case PurgeOldCalendars =>
      ???
  }
}

object CalendarActor {

  case class WeekOfYear(year: Int, month: Int, week: Int)

  case class UpdateCalendar(week: WeekOfYear, calendar: Calendar)

  case object GetCalendar

  private object PurgeOldCalendars

}
