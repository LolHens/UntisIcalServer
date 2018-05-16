package org.lolhens.untisicalserver

import com.google.api.services.calendar.model.CalendarListEntry
import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.{Config, School, SchoolClass}
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId._
import org.lolhens.untisicalserver.google.{Authorize, CalendarManager}

import scala.concurrent.duration._

object Google {
  lazy val calendarManagerTask: Task[CalendarManager] = Task(CalendarManager(
    Authorize.getCalendarService("UntisIcalServer", readonly = false).get
  )).memoizeOnSuccess

  //Utils.setLogLevel

  val calendarEntryTask: Task[CalendarId] =
    (for {
      calendarManager <- calendarManagerTask
      calendars <- calendarManager.listCalendars
      calendar = calendars("FS-15B Stundenplan")
    } yield calendar)
      .memoizeOnSuccess

  lazy val schools: List[School] = Config.load.schools

  lazy val schoolClass: SchoolClass = schools.flatMap(_.classes).find(_.ref == "fs15b").get

  val updateCalendar: Task[Unit] = {
    for {
      calendarManager <- Observable.fromTask(calendarManagerTask)
      calendarId <- Observable.fromTask(calendarEntryTask)
      calendars <- Observable.fromTask(schoolClass.calendars.calendars)
      _ = println(s"Updating calendar ${calendarId.name}")
      _ <- Observable.fromTask(calendarManager.purgeCalendar(calendarId))
      (week, calendar) <- Observable.fromIterable(calendars.toSeq)
      events = calendar.events.map(_.toGEvent)
    } yield
      calendarManager.updateWeek(calendarId, week, events)
  }
    //.mapParallelUnordered(16)(identity)
    .mapTask(identity)
    .completedL

  def updateCalendarContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(30.seconds, interval, ())
      .mapTask(_ => updateCalendar)
      .completedL
}
