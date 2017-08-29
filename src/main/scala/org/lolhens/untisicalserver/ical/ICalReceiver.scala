package org.lolhens.untisicalserver.ical

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass

import scala.language.postfixOps

object ICalReceiver {
  lazy val forWeek: Flow[(SchoolClass, WeekOfYear), (SchoolClass, WeekOfYear, Option[Calendar]), NotUsed] =
    Flow[(SchoolClass, WeekOfYear)]
      .flatMapConcat {
        case (schoolClass, week) =>
          Source.single((schoolClass, week))
            .via(CalendarRequester.flow)
            .map(e => (schoolClass, week, Some(e._2.get)))
            .recoverWithRetries(5, {
              case _ => Source.single((schoolClass, week, None))
            })
      }


  lazy val forWeekRange: Flow[(SchoolClass, WeekRange), (SchoolClass, List[(WeekOfYear, Calendar)]), NotUsed] =
    Flow[(SchoolClass, WeekRange)]
      .flatMapConcat {
        case (schoolClass, weeks) =>
          Source.repeat(schoolClass)
            .zip(Source(weeks.toList))
            .via(forWeek)
            .fold(List.empty[(WeekOfYear, Calendar)]) { (last, calendarOption) =>
              last ++ calendarOption._3.map(e => (calendarOption._2, e)).toList
            }
            .map(e => (schoolClass, e))
      }

  def currentCalendars(back: Int = 20, forward: Int = 10): Flow[SchoolClass, (SchoolClass, List[(WeekOfYear, Calendar)]), NotUsed] =
    Flow[SchoolClass]
      .zip(
        Source.repeat(WeekRange(WeekOfYear.now - back, WeekOfYear.now + forward))
      )
      .via(forWeekRange)
}
