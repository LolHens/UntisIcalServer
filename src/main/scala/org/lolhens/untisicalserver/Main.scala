package org.lolhens.untisicalserver

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.schedulers.AsyncScheduler
import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}
import org.lolhens.untisicalserver.data.config.Config
import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.util.Utils

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Utils.setLogLevel

    val config = Config.load

    def newScheduler: Scheduler = AsyncScheduler(
      Scheduler.DefaultScheduledExecutor,
      ExecutionContext.fromExecutor(null),
      UncaughtExceptionReporter.LogExceptionsToStandardErr,
      ExecutionModel.Default
    )

    val err0 = Task.sequence(config.schools.map(_.updateCacheContinuously(30.seconds)))

    val iCalServer = new ICalServer(config)
    val err1 = iCalServer.start

    val err2 = Google.updateCalendarContinuously(30.seconds)

    Await.result(Task.gatherUnordered(Seq(
      err0.executeOn(newScheduler).doOnFinish(_ => Task(println("tast0 ended"))),
      err1.executeOn(newScheduler).doOnFinish(_ => Task(println("tast1 ended"))),
      err2.executeOn(newScheduler).doOnFinish(_ => Task(println("tast2 ended")))
    )).runAsync, Duration.Inf)

    println("ended")
  }
}

