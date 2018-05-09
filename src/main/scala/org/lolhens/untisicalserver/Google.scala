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

  val calendar: Task[CalendarListEntry] =
    for {
      calendars <- calendarManager.listCalendars()
    } yield
      calendars.find(e => CalendarManager.calendarName(e) == "FS-15B Stundenplan").get

  lazy val schools: List[School] = Config.load.schools

  lazy val schoolClass: SchoolClass = schools.flatMap(_.classes).find(_.ref == "fs15b").get

  val updateCalendar: Task[Unit] = {
    for {
      calendarFibre <- Observable.fromTask(calendar.fork)
      calendars <- Observable.fromTask(schoolClass.calendars.calendars)
      (week, cal) <- Observable.fromIterable(calendars.toSeq)
      events = cal.events.map(_.toGEvent)
      calendar <- Observable.fromTask(calendarFibre.join)
    } yield
      calendarManager.updateWeek(calendar, week, events)
  }
    .mapParallelUnordered(16)(identity)
    .completedL

  def updateCalendarContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(10.seconds, interval, ())
      .mapParallelUnordered(1)(_ => updateCalendar)
      .completedL
}
