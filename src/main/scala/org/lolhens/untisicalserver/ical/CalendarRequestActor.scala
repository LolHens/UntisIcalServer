package org.lolhens.untisicalserver.ical

import java.io.StringReader
import java.time.LocalDate

import akka.actor.{Actor, ActorRef, ActorRefFactory, Props}
import monix.execution.FutureUtils.extensions._
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.http.client.StringReceiver
import org.lolhens.untisicalserver.ical.CalendarRequestActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

/**
  * Created by pierr on 24.03.2017.
  */
class CalendarRequestActor extends Actor {
  def padInt(int: Int, digits: Int): String = int.toString.reverse.padTo(digits, "0").reverse.mkString

  def dateString(date: LocalDate): String =
    s"${padInt(date.getYear, 4)}-${padInt(date.getMonthValue, 2)}-${padInt(date.getDayOfMonth, 2)}"

  def iCalUrl(school: String, classId: String, date: LocalDate): String =
    s"https://mese.webuntis.com/WebUntis/Ical.do?school=$school&elemType=1&elemId=$classId&rpt_sd=${dateString(date)}"

  override def receive: Receive = {
    case RequestCalendar(schoolClass, week) =>
      val url = iCalUrl(schoolClass.school, schoolClass.classId.toString, week.localDate)

      stringReceiver.receive(url)
        .materialize.foreach(self ! _)

      val lastSender = sender()

      context.become {
        case Success(icalString: String) =>
          lastSender ! Try(parseCalendar(icalString))
          context.unbecome()

        case failure@Failure(_) =>
          lastSender ! failure
          context.unbecome()
      }
  }
}

object CalendarRequestActor {
  private val stringReceiver = new StringReceiver(10 seconds)

  private def parseCalendar(string: String): Calendar = synchronized {
    new CalendarBuilder().build(new StringReader(string))
  }

  val props: Props = Props[CalendarRequestActor]

  def actor(implicit actorSystem: ActorRefFactory): ActorRef = actorSystem.actorOf(props)

  case class RequestCalendar(schoolClass: SchoolClass, week: WeekOfYear)

}
