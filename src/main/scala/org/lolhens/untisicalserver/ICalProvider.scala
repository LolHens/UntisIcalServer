package org.lolhens.untisicalserver

import java.io.StringReader
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.ICalProvider._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ICalProvider(val schoolName: String,
                   val classId: Int) {

  def apply(year: Int, week: Int): Future[Calendar] = {
    val date = dateOfWeek(year, week)

    def padInt(int: Int, digits: Int): String = int.toString.reverse.padTo(digits, "0").reverse.mkString

    val yearString = padInt(date.getYear, 4)
    val monthString = padInt(date.getMonthValue, 2)
    val dayString = padInt(date.getDayOfMonth, 2)

    val iCalUrl = s"https://mese.webuntis.com/WebUntis/Ical.do?school=$schoolName&elemType=1&elemId=$classId&rpt_sd=$yearString-$monthString-$dayString"

    StringReceiver.receive(iCalUrl).map { iCalString =>
      new CalendarBuilder().build(new StringReader(iCalString))
    }
  }

  def forRange(year: Int, weeks: Range): Future[List[Calendar]] = Future.sequence(weeks.map(apply(year, _)).toList)
}

object ICalProvider {
  private def dateOfWeek(year: Int, week: Int): LocalDate = {
    val weekFields = WeekFields.of(Locale.getDefault())
    LocalDate.now()
      .withYear(year)
      .`with`(weekFields.weekOfYear(), 1)
      .`with`(weekFields.dayOfWeek(), 1)
      .plusWeeks(week - 1)
  }
}
