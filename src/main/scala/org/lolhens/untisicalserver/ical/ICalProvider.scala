package org.lolhens.untisicalserver.ical

import akka.actor.ActorSystem
import net.fortuna.ical4j.model.Calendar
import org.lolhens.untisicalserver.data.SchoolClass

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

class ICalProvider(val schoolClass: SchoolClass) {
  implicit val actorSystem = ActorSystem()

  def apply(): Calendar = {
    val currentCalendars =
      new ICalReceiver(schoolClass).currentCalendars()

    val calendarFuture = currentCalendars.map { calendars =>
      val calendar = ICalSplicer(
        calendars.map(ICalTransformer(schoolClass, _))
      )

      calendar.setComponents(ICalEventMerger(calendar.getComponents.asScala.toList))

      calendar
    }

    calendarFuture.onFailure {
      case e: Exception =>
        println(e)
    }

    Await.result(calendarFuture, 5 minutes)
  }
}
