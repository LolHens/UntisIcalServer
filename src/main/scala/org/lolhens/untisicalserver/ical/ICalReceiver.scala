package org.lolhens.untisicalserver.ical

import java.io.StringReader
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.http.client.StringReceiver
import org.lolhens.untisicalserver.ical.ICalReceiver._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ICalReceiver(val schoolClass: SchoolClass) {
  def apply(year: Int, week: Int): Future[Option[Calendar]] = {
    def dateString(date: LocalDate) = {
      def padInt(int: Int, digits: Int): String = int.toString.reverse.padTo(digits, "0").reverse.mkString

      s"${padInt(date.getYear, 4)}-${padInt(date.getMonthValue, 2)}-${padInt(date.getDayOfMonth, 2)}"
    }

    def iCalUrl(school: String, classId: String, date: LocalDate): String =
      s"https://mese.webuntis.com/WebUntis/Ical.do?school=$school&elemType=1&elemId=$classId&rpt_sd=${dateString(date)}"

    val url = iCalUrl(schoolClass.school, schoolClass.classId.toString, dateOfWeek(year, week))

    def receive: Future[Option[Calendar]] = stringReceiver.receive(url).map { iCalString =>
      Try(parseCalendar(iCalString)) match {
        case Success(result) =>
          Some(result)

        case Failure(exception) =>
          // Parsing failed
          exception.printStackTrace()
          None
      }
    }

    Future {
      (0 until 10).foldLeft[Option[Option[Calendar]]](None) {
        case (None, _) =>
          Try(Await.result(receive, 20 seconds)) match {
            case Success(result) =>
              Some(result)

            case Failure(exception) =>
              // Connection failed
              exception.printStackTrace()
              None
          }

        case (result@Some(_), _) =>
          result
      }.flatten
    }
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
  private val stringReceiver = new StringReceiver(10 seconds)

  private def parseCalendar(string: String): Calendar = synchronized {
    new CalendarBuilder().build(new StringReader(string))
  }

  private def dateOfWeek(year: Int, week: Int): LocalDate = {
    val weekFields = WeekFields.of(Locale.getDefault())
    LocalDate.now()
      .withYear(year)
      .`with`(weekFields.weekOfYear(), 1)
      .`with`(weekFields.dayOfWeek(), 1)
      .plusWeeks(week - 1)
  }
}
