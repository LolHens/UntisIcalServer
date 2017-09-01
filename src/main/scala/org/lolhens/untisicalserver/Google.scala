package org.lolhens.untisicalserver

import com.google.api.services.calendar.{Calendar => CalendarService}
import monix.execution.Scheduler.Implicits.global
import org.lolhens.untisicalserver.data.config.Config
import org.lolhens.untisicalserver.google.{Authorize, CalendarManager}
import org.lolhens.untisicalserver.util.Utils

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

object Google {
  lazy val calendarManager = CalendarManager(Authorize.getCalendarService("UntisIcalServer", readonly = false).get)

  val interval: FiniteDuration = 10.seconds // 2.minutes

  def updateCalendar(): Unit = {
    //Utils.setLogLevel

    val calendar = Await.result(calendarManager.listCalendars().runAsync, Duration.Inf)
      .find(e => CalendarManager.calendarName(e) == "FS-15B Stundenplan").get

    val schoolClass = Config.load.schools.flatMap(_.classes).find(_.ref == "fs15b").get

    /*println(calendar)
    println((WeekOfYear.now + 1).localDateMin)
    println((WeekOfYear.now + 1).localDateMax)
    //println(calendarManager.test(WeekOfYear.now))
    val t = for (e <- calendarManager.listEvents(calendar, WeekOfYear.now);
    //_ <- calendarManager.clear(calendar);
    //_ <- calendarManager.removeEvents(calendar, e);
    _ = println(e)) yield e

    Await.result(t.runAsync, Duration.Inf)*/

    while (true) {
      println("loop")
      Try {
        val calendars = schoolClass.iCalProvider.all

        Await.result(Utils.parallel(
          for {
            (week, cal) <- calendars.toList
            events = cal.events.map(_.toGEvent)
          } yield {
            //println(s"week $week ${week.localDateMin}: ${events.size} events")
            calendarManager.updateWeek(calendar, week, events)
          },
          unordered = true
        ).runAsync, 5.minutes)
      }.failed.foreach(_.printStackTrace())
      println("looped")

      Try(Thread.sleep(interval.toMillis))
    }
  }
}
