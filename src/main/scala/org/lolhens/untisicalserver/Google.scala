package org.lolhens.untisicalserver

import com.google.api.services.calendar.model.CalendarListEntry
import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.{Config, School, SchoolClass}
import org.lolhens.untisicalserver.google.{Authorize, CalendarManager}
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId._
import scala.concurrent.duration._

object Google {
  lazy val calendarManager = CalendarManager(Authorize.getCalendarService("UntisIcalServer", readonly = false).get)

  //Utils.setLogLevel

  val calendarEntryTask: Task[CalendarListEntry] =
    calendarManager.listCalendars()
      .map(_.apply("FS-15B Stundenplan"))
      .memoize

  lazy val schools: List[School] = Config.load.schools

  lazy val schoolClass: SchoolClass = schools.flatMap(_.classes).find(_.ref == "fs15b").get

  val updateCalendar: Task[Unit] = {
    for {
      calendarEntry <- Observable.fromTask(calendarEntryTask)
      calendars <- Observable.fromTask(schoolClass.calendars.calendars)
      (week, calendar) <- Observable.fromIterable(calendars.toSeq)
      events = calendar.events.map(_.toGEvent)
    } yield
      calendarManager.updateWeek(calendarEntry.id, week, events)
  }
    //.mapParallelUnordered(16)(identity)
    .mapTask(identity)
    .completedL

  def updateCalendarContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(10.seconds, interval, ())
      .mapTask(_ => updateCalendar)
      .completedL
}
