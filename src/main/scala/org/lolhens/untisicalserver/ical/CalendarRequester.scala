package org.lolhens.untisicalserver.ical

import java.time.LocalDate

import monix.eval.{Task, TaskCircuitBreaker}
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.data.config.SchoolClass
import org.lolhens.untisicalserver.http.client.StringReceiver

import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 24.03.2017.
  */
object CalendarRequester {
  def iCalUrl(schoolClass: SchoolClass, week: WeekOfYear): String = {
    val school = schoolClass.school.id
    val elemId = schoolClass.id
    val date = week.midDate.toString
    s"https://mese.webuntis.com/WebUntis/Ical.do?school=$school&elemType=1&elemId=$elemId&rpt_sd=$date"
  }

  private val stringReceiver = new StringReceiver(10 seconds)

  private val circuitBreaker = TaskCircuitBreaker(
    maxFailures = 5,
    resetTimeout = 10.seconds
  ).memoize

  def request(schoolClass: SchoolClass,
              week: WeekOfYear): Task[Calendar] = {
    val url = iCalUrl(schoolClass, week)

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
