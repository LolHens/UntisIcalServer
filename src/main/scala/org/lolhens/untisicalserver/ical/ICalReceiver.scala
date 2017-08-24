package org.lolhens.untisicalserver.ical

import akka.actor.{ActorRef, ActorRefFactory}
import akka.pattern.ask
import akka.stream.scaladsl.{Flow, Source}
import akka.util.Timeout
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ICalReceiver {
  def forWeek = Flow[(SchoolClass, WeekOfYear)]
    .via(CalendarRequester.flow)
      .map(e => Some(e.get))
      .recoverWithRetries(5, {
        case e => Source.single(None)
      })

  def forWeekRange = Flow[(SchoolClass, WeekRange)]

  def forRange(weeks: WeekRange): Future[List[Calendar]] = Future.sequence(weeks.toList.map(apply)).map(_.flatMap(_.toList))

  def currentCalendars(back: Int = 2, forward: Int = -1): Future[List[Calendar]] =
    forRange(WeekRange(WeekOfYear.now - back, WeekOfYear.now + forward))
}
