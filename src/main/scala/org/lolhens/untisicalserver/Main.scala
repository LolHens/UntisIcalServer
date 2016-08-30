package org.lolhens.untisicalserver

import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

import dispatch._
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.ical.ICalReceiver
import org.lolhens.untisicalserver.ical.ICalSplicer
import org.lolhens.untisicalserver.ical.ICalTransformer

import scala.concurrent.ExecutionContext.Implicits.global


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  val nixdorfFs15b = SchoolClass("nixdorf_bk_essen", "FS-15B", 183)

  def main(args: Array[String]): Unit = {
    weekCalendars(nixdorfFs15b)
      .map(calendars =>
        ICalSplicer(calendars
          .map(ICalTransformer(_))))
      .onSuccess {
        case calendar =>
          println(calendar)
      }
  }

  def weekCalendars(schoolClass: SchoolClass): Future[List[Calendar]] = {
    val iCalProvider = new ICalReceiver(schoolClass)

    val now = LocalDate.now()

    val weekFields = WeekFields.of(Locale.getDefault())
    val week = now.get(weekFields.weekOfWeekBasedYear())

    iCalProvider.forRange(now.getYear, (week - 10) to (week + 40))
  }
}

