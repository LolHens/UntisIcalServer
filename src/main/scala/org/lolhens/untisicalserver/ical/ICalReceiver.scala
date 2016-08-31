package org.lolhens.untisicalserver.ical

import java.io.StringReader
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.http.client.StringReceiver
import org.lolhens.untisicalserver.ical.ICalReceiver._
import org.lolhens.untisicalserver.util.SchoolClass

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class ICalReceiver(val schoolClass: SchoolClass) {
  def apply(year: Int, week: Int): Future[Option[Calendar]] = {
    val date = dateOfWeek(year, week)

    def padInt(int: Int, digits: Int): String =
      int.toString.reverse.padTo(digits, "0").reverse.mkString

    val yearString = padInt(date.getYear, 4)
    val monthString = padInt(date.getMonthValue, 2)
    val dayString = padInt(date.getDayOfMonth, 2)

    val iCalUrl =
      s"https://mese.webuntis.com/WebUntis/Ical.do?school=${schoolClass.school}&elemType=1&elemId=${schoolClass.classId}&rpt_sd=$yearString-$monthString-$dayString"

    StringReceiver.receive(iCalUrl)(20 seconds).map { iCalString =>
      Some(new CalendarBuilder().build(new StringReader(iCalString)))
    }.fallbackTo(Future.successful(None))
  }

  def forRange(year: Int, weeks: Range): Future[List[Calendar]] = Future.sequence(weeks.map(apply(year, _)).toList).map(_.flatMap(_.toList))

  def currentCalendars(back: Int = 10, forward: Int = 40): Future[List[Calendar]] = {
    val now = LocalDate.now()

    val weekFields = WeekFields.of(Locale.getDefault())
    val week = now.get(weekFields.weekOfWeekBasedYear())

    forRange(now.getYear, (week - back) to (week + forward))
  }
}

object ICalReceiver {
  private def dateOfWeek(year: Int, week: Int): LocalDate = {
    val weekFields = WeekFields.of(Locale.getDefault())
    LocalDate.now()
      .withYear(year)
      .`with`(weekFields.weekOfYear(), 1)
      .`with`(weekFields.dayOfWeek(), 1)
      .plusWeeks(week - 1)
  }
}
