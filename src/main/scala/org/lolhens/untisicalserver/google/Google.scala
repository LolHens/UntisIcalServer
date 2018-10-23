package org.lolhens.untisicalserver.google

import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.{Config, SchoolClass}
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId
import org.lolhens.untisicalserver.google.CalendarManager.CalendarId._

import scala.concurrent.duration._

object Google {
  lazy val calendarManagerTask: Task[CalendarManager] = Task(CalendarManager(
    Authorize.getCalendarService("UntisIcalServer", readonly = false).get
  )).memoizeOnSuccess

  //Utils.setLogLevel

  def calendarName(schoolClass: SchoolClass): String = s"${schoolClass.name} Stundenplan"

  lazy val calendarsTask: Task[Map[SchoolClass, CalendarId]] = for {
    schools <- Task(Config.load.schools)
    calendarManager <- calendarManagerTask
    calendars <- calendarManager.listCalendars
    classCalendars <- Task.sequence {
      for {
        school <- schools
        schoolClass <- school.classes
      } yield for {
        calendarId <- calendars.get(calendarName(schoolClass)).map(Task.now).getOrElse {
          calendarManager.createCalendar(calendarName(schoolClass)).map(_.id)
        }
      } yield
        schoolClass -> calendarId
    }
  } yield
    classCalendars.toMap

  def updateCalendar(schoolClass: SchoolClass, calendarId: CalendarId): Task[Unit] = {
    for {
      calendarManager <- Observable.fromTask(calendarManagerTask)
      calendars <- Observable.fromTask(schoolClass.calendars.calendars)
      _ = println(s"Updating calendar ${calendarId.name}")
      //_ = println(calendars)
      _ <- Observable.fromTask(calendarManager.purgeCalendar(calendarId))
      (week, calendar) <- Observable.fromIterable(calendars.toSeq)
      events = calendar.events.map(_.toGEvent)
      _ <- Observable.fromTask(calendarManager.updateWeek(calendarId, week, events))
    } yield ()
  }
    .completedL

  val updateCalendars: Task[Unit] = {
    for {
      calendars <- Observable.fromTask(calendarsTask)
      (schoolClass, calendarId) <- Observable.fromIterable(calendars)
      _ <- Observable.fromTask(updateCalendar(schoolClass, calendarId))
    } yield ()
  }
    .completedL

  def updateCalendarsContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(30.seconds, interval, ())
      .mapTask(_ => updateCalendars)
      .completedL
}
