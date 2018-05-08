package org.lolhens.untisicalserver.data.config

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable

import scala.concurrent.duration._

case class School(id: String,
                  ref: String,
                  teachers: Map[String, String],
                  classes: List[SchoolClass]) {
  classes.foreach(_._school = this)

  val updateCache: Task[Unit] =
    Observable.concat(classes.map(_.calendars.updateCache): _*).completedL

  def updateCacheContinuously(interval: FiniteDuration): Task[Unit] =
    Observable.timerRepeated(0.seconds, interval, ())
      .mapParallelUnordered(1)(_ => updateCache)
      .completedL

  def runUpdateCacheContinuously(interval: FiniteDuration): Unit =
    updateCacheContinuously(interval).runAsync
}
