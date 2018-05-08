package org.lolhens.untisicalserver.ical

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.atomic.Atomic
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

class CalendarCache(val schoolClass: SchoolClass,
                    val interval: FiniteDuration) {
  val provider = CalendarProvider(schoolClass)

  private val calendarCache = Atomic(Map.empty[WeekOfYear, Calendar])

  val updateCache: Observable[Map[WeekOfYear, Calendar]] =
    for {
      (week, calendar) <- provider.calendars
      _ = println(s"CALENDAR OF ${schoolClass.name} FOR WEEK $week")
    } yield calendarCache.transformAndGet { cache =>
      cache.updated(week, calendar)
    }

  val updateCacheContinuously: Task[Unit] =
    Observable.timerRepeated(0.seconds, interval, updateCache)
      .flatten
      .foreachL(_ => ())

  val calendars: Task[Map[WeekOfYear, Calendar]] =
    Task(calendarCache.get)

  def calendarsNow(): Map[WeekOfYear, Calendar] =
    Await.result(calendars.runAsync, 10.minutes)

  val calendar: Task[Calendar] =
    for {
      calendars <- calendars
    } yield
      Calendar(calendars.values.flatMap(_.events).toList)

  def calendarNow(): Calendar =
    Await.result(calendar.runAsync, 10.minutes)
}
