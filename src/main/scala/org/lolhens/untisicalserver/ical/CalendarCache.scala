package org.lolhens.untisicalserver.ical

import monix.eval.Task
import monix.execution.atomic.Atomic
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass

import scala.concurrent.duration.FiniteDuration

class CalendarCache(val schoolClass: SchoolClass,
                    val interval: FiniteDuration) {
  val provider = CalendarProvider(schoolClass)

  private val calendarCache = Atomic(Map.empty[WeekOfYear, Calendar])

  val updateCache: Observable[Map[WeekOfYear, Calendar]] =
    for {
      (week, calendar) <- provider.calendars
      //_ = println(s"CALENDAR OF ${schoolClass.name} FOR WEEK $week")
    } yield calendarCache.transformAndGet { cache =>
      cache.updated(week, calendar)
    }

  val calendars: Task[Map[WeekOfYear, Calendar]] =
    Task(calendarCache.get)

  val calendar: Task[Calendar] =
    for {
      calendars <- calendars
    } yield
      Calendar(calendars.toList.sortBy(_._1).flatMap(_._2.events))
}
