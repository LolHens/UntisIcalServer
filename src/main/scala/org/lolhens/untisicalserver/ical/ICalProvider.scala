package org.lolhens.untisicalserver.ical

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, ThrottleMode}
import monix.execution.atomic.Atomic
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.language.postfixOps

class ICalProvider(val schoolClass: SchoolClass, interval: FiniteDuration) {
  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  val calendarCache = Atomic(Map.empty[WeekOfYear, Calendar])
  val currentCalendar = Atomic(Calendar.empty)

  Source.repeat(schoolClass)
    .throttle(1, interval, 1, ThrottleMode.Shaping)
    .via(ICalReceiver.currentCalendars())
    .mapConcat {
      case (schoolClass, weeksAndcalendars) =>
        weeksAndcalendars
          .map(e => (schoolClass, e._1, e._2))
    }
    .via(ICalTransformer.flow)
    .flatMapConcat {
      case (week, calendar) =>
        Source.single(calendar)
          .map(_.events)
          .mapAsync(1)(events => ICalEventMerger.mergeEvents(events).runAsync)
          .map { newEvents =>
            (week, calendar.copy(events = newEvents))
          }
    }
    .groupedWithin(Int.MaxValue, 10.seconds)
    .map { weeksAndCalendars =>
      val cache = calendarCache.transformAndGet { cache =>
        cache ++ weeksAndCalendars
      }

      cache.values.toList
    }
    .filter(_.nonEmpty)
    .map { calendars =>
      val splicedCalendar = Calendar(calendars.flatMap(_.events))
      currentCalendar.set(splicedCalendar)
    }
    .to(Sink.ignore)
    .run()

  def apply(): Calendar = currentCalendar.get

  def all: Map[WeekOfYear, Calendar] = calendarCache.get
}
