package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import monix.eval.Task
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass

import scala.language.postfixOps

object ICalReceiver {
  def forWeek(schoolClass: SchoolClass,
              week: WeekOfYear): Task[Option[Calendar]] =
    CalendarRequester.task(schoolClass, week)
      .dematerialize
      .onErrorRestart(5)
      .materialize
      .map(_.toOption)

  def forWeekRangeTask(schoolClass: SchoolClass,
                       weekRange: WeekRange): Task[Map[WeekOfYear, Calendar]] =
    for {
      events <- Task.sequence(
        for {
          week <- weekRange.toList
          calendarOptionTask = forWeek(schoolClass, week)
        } yield for {
          calendarOption <- calendarOptionTask
        } yield for {
          calendar <- calendarOption
        } yield
          week -> calendar
      )
    } yield
      events.flatMap(_.toSeq).toMap

  def currentCalendarsTask(schoolClass: SchoolClass,
                           back: Int = 20,
                           forward: Int = 10): Task[Map[WeekOfYear, Calendar]] =
    forWeekRangeTask(schoolClass, WeekRange(WeekOfYear.now - back, WeekOfYear.now + forward))
}
