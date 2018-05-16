package org.lolhens.untisicalserver.data.config

import monix.eval.Task
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.Calendar
import org.lolhens.untisicalserver.ical.WeekOfYear

import scala.concurrent.duration._

case class School(id: String,
                  ref: String,
                  teachers: Map[String, String],
                  classes: List[SchoolClass]) {
  classes.foreach(_._school = this)

  val updateCache: Task[Map[SchoolClass, Map[WeekOfYear, Calendar]]] =
    Observable.fromIterable(classes)
      .mapTask(schoolClass =>
        for {
          calendars <- schoolClass.updateCache
        } yield
          schoolClass -> calendars
      )
      .toListL
      .map(_.toMap)

  def updateCacheContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(0.seconds, interval, ())
      .mapTask(_ => updateCache)
      .completedL
}
