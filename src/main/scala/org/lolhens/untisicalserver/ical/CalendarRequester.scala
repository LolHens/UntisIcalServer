package org.lolhens.untisicalserver.ical

import java.io.StringReader
import java.time.LocalDate

import akka.NotUsed
import akka.stream.scaladsl.Flow
import monix.execution.FutureUtils.extensions._
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.http.client.StringReceiver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by pierr on 24.03.2017.
  */
object CalendarRequester {
  def padInt(int: Int, digits: Int): String = int.toString.reverse.padTo(digits, "0").reverse.mkString

  def dateString(date: LocalDate): String =
    s"${padInt(date.getYear, 4)}-${padInt(date.getMonthValue, 2)}-${padInt(date.getDayOfMonth, 2)}"

  def iCalUrl(school: String, classId: String, date: LocalDate): String =
    s"https://mese.webuntis.com/WebUntis/Ical.do?school=$school&elemType=1&elemId=$classId&rpt_sd=${dateString(date)}"

  private val stringReceiver = new StringReceiver(10 seconds)

  private def parseCalendar(string: String): Calendar = synchronized {
    new CalendarBuilder().build(new StringReader(string))
  }

  val flow: Flow[(SchoolClass, WeekOfYear), (SchoolClass, Try[Calendar]), NotUsed] =
    Flow[(SchoolClass, WeekOfYear)]
      .map {
        case (schoolClass, week) =>
          (schoolClass, iCalUrl(schoolClass.school, schoolClass.classId.toString, week.localDate))
      }
      .mapAsync(8) {
        case (schoolClass, url) =>
          stringReceiver.receive(url).materialize
            .map { icalStringTry =>
              (schoolClass, icalStringTry)
            }
      }.map {
      case (schoolClass, Success(icalString: String)) =>
        (schoolClass, Try(parseCalendar(icalString)))

      case (schoolClass, Failure(e)) =>
        (schoolClass, Failure(e))
    }
}
