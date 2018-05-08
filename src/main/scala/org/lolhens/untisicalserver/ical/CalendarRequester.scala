package org.lolhens.untisicalserver.ical

import java.time.LocalDate

import akka.NotUsed
import akka.stream.scaladsl.Flow
import monix.eval.{Task, TaskCircuitBreaker}
import monix.execution.FutureUtils.extensions._
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass
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

  def iCalUrl(schoolClass: SchoolClass, date: LocalDate): String =
    s"https://mese.webuntis.com/WebUntis/Ical.do?school=${schoolClass.school.id}&elemType=1&elemId=${schoolClass.id}&rpt_sd=${dateString(date)}"

  private val stringReceiver = new StringReceiver(10 seconds)

  private val circuitBreaker = TaskCircuitBreaker(
    maxFailures = 5,
    resetTimeout = 10.seconds
  ).memoize

  def request(schoolClass: SchoolClass,
              week: WeekOfYear): Task[Calendar] = {
    val url = iCalUrl(schoolClass, week.localDateMin)

    val calendar = stringReceiver.receive(url)
      .flatMap(icalString =>
        Task.fromTry(Calendar.parse(icalString))
      )

    for {
      ci <- circuitBreaker
      r <- ci.protect(calendar)
    } yield r
  }
}
