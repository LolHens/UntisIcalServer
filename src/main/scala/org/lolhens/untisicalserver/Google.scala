package org.lolhens.untisicalserver

import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.google.{Authorize, CalendarManager}
import org.lolhens.untisicalserver.ical.WeekOfYear
import org.lolhens.untisicalserver.util.Utils
import org.lolhens.untisicalserver.util.Utils._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object Google {
  lazy val calendarManager = CalendarManager(Authorize.getCalendarService("UntisIcalServer", readonly = false).get)

  def updateCalendar(): Unit = {
    //Utils.setLogLevel

    val calendar = Await.result(calendarManager.listCalendars().runAsync, Duration.Inf)
      .find(e => CalendarManager.calendarName(e) == "FS-15B Stundenplan").get

    val schoolClass = SchoolClass.classes("fs15b")

    println(calendar)
    println((WeekOfYear.now + 1).localDateMin)
    println((WeekOfYear.now + 1).localDateMax)
    println(calendarManager.test(WeekOfYear.now))
    val t = for (e <- calendarManager.listEvents(calendar, WeekOfYear.now);
    _ = println(e)) yield e

    Await.result(t.runAsync, Duration.Inf)

    /*while (true) {
      Try {
        val calendars = schoolClass.iCalProvider.all

        Await.result(Task.sequence(
          for {
            (week, cal) <- calendars.toList
            events = CalendarManager.toEvents(cal.events)
          } yield {
            calendarManager.updateWeek(calendar, week, events)
          }
        ).runAsync, 5.minutes)
      }.failed.foreach(_.printStackTrace())

      Try(Thread.sleep(2.minutes.toMillis))
    }*/
  }
}
