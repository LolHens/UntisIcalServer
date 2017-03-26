package org.lolhens.untisicalserver.ical

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.property.{CalScale, ProdId, Version}
import org.lolhens.untisicalserver.ical.CalendarCacheActor._

import scala.collection.JavaConverters._

/**
  * Created by pierr on 17.03.2017.
  */
class CalendarCacheActor extends Actor {
  var calendars: Map[WeekOfYear, Calendar] = Map()

  override def receive: Receive = {
    case UpdateCalendar(week, calendar) =>
      calendars = calendars + (week -> calendar)

    case GetCalendar =>
      val mergedCalendar: Calendar = {
        val calendar = new Calendar()

        calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"))
        calendar.getProperties().add(Version.VERSION_2_0)
        calendar.getProperties().add(CalScale.GREGORIAN)

        calendar.getComponents().addAll(calendars.values.toList.flatMap(_.getComponents().asScala).asJava)

        calendar
      }

      sender() ! mergedCalendar

    case PurgeOldCalendars =>
      ???
  }
}

object CalendarCacheActor {
  val props: Props = Props[CalendarCacheActor]

  def actor(actorSystem: ActorRefFactory): ActorRef = actorSystem.actorOf(props)

  case class UpdateCalendar(week: WeekOfYear, calendar: Calendar)

  case object GetCalendar

  private object PurgeOldCalendars

}
