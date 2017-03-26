package org.lolhens.untisicalserver.ical

import akka.actor.{ActorRef, ActorRefFactory}
import akka.pattern.ask
import akka.util.Timeout
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

class ICalReceiver(val schoolClass: SchoolClass)(implicit actorSystem: ActorRefFactory) {
  val requestActor: ActorRef = CalendarRequestActor.actor

  def apply(week: WeekOfYear): Future[Option[Calendar]] = {
    def receive: Future[Option[Calendar]] = {
      implicit val timeout = Timeout(10 seconds)

      val result = (requestActor ? CalendarRequestActor.RequestCalendar(schoolClass, week))
        .mapTo[Try[Calendar]]

      result.onComplete(println)

      result.map(_.toOption)
    }

    Future {
      (0 until 1).foldLeft[Option[Option[Calendar]]](None) {
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

  def forRange(weeks: WeekRange): Future[List[Calendar]] = Future.sequence(weeks.toList.map(apply)).map(_.flatMap(_.toList))

  def currentCalendars(back: Int = 2, forward: Int = -1): Future[List[Calendar]] =
    forRange(WeekRange(WeekOfYear.now - back, WeekOfYear.now + forward))
}
