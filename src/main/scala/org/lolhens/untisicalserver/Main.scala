package org.lolhens.untisicalserver

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

import dispatch._
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.ical.{ICalProvider, ICalSplicer, ICalTransformer}

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    weekCalendars(SchoolClass("nixdorf_bk_essen", 183))
      .map(calendars =>
        ICalSplicer(calendars
          .map(ICalTransformer(_))))
      .onSuccess {
        case values =>
          println(values)
      }
  }

  def weekCalendars(schoolClass: SchoolClass): Future[List[Calendar]] = {
    val iCalProvider = new ICalProvider(schoolClass)

    val now = LocalDate.now()

    val weekFields = WeekFields.of(Locale.getDefault())
    val week = now.get(weekFields.weekOfWeekBasedYear())

    iCalProvider.forRange(now.getYear, (week - 10) to (week + 40))
  }
}

