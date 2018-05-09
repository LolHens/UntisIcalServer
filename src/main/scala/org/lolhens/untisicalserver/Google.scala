package org.lolhens.untisicalserver

import com.google.api.services.calendar.model.CalendarListEntry
import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.{Config, School, SchoolClass}
import org.lolhens.untisicalserver.google.{Authorize, CalendarManager}

import scala.concurrent.duration._

object Google {
  lazy val calendarManager = CalendarManager(Authorize.getCalendarService("UntisIcalServer", readonly = false).get)

  //Utils.setLogLevel

  val calendarEntryTask: Task[CalendarListEntry] =
    (for (calendars <- calendarManager.listCalendars()) yield
      calendars.find(e => CalendarManager.calendarName(e) == "FS-15B Stundenplan").get)
      .memoize

  lazy val schools: List[School] = Config.load.schools

  lazy val schoolClass: SchoolClass = schools.flatMap(_.classes).find(_.ref == "fs15b").get

  val updateCalendar: Task[Unit] = {
    for {
      calendarEntry <- Observable.fromTask(calendarEntryTask)
      _ = println("g: aquired calendar")
      calendars <- Observable.fromTask(schoolClass.calendars.calendars)
      (week, calendar) <- Observable.fromIterable(calendars.toSeq)
      events = calendar.events.map(_.toGEvent)
    } yield
      calendarManager.updateWeek(calendarEntry, week, events)
  }
    .mapParallelUnordered(16)(_.map{e => println("updated calendar"); e})
    .completedL

  def updateCalendarContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(10.seconds, interval, ())
      .mapParallelUnordered(1)(_ => updateCalendar)
      .completedL
}
