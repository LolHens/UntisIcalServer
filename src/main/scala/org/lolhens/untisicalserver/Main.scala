package org.lolhens.untisicalserver

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import monix.execution.schedulers.AsyncScheduler
import monix.execution.{ExecutionModel, Scheduler, UncaughtExceptionReporter}
import monix.reactive.Observable
import org.lolhens.untisicalserver.data.config.Config
import org.lolhens.untisicalserver.google.Google
import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.util.Utils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Utils.setLogLevel

    val config = Config.load

    val iCalServer = new ICalServer(config)

    executeParallelBlocking(Seq(
      retry("Calendar Fetcher", loop(Task.gather(config.schools.map(_.updateCache)), 30.seconds)),
      retry("ICal Server", iCalServer.start),
    ) ++ Seq(
      retry("Google Calendar Server", loop(Google.updateCalendars, 30.seconds, initialDelay = 60.seconds))
    ).filter(_ => config.googleservice))

    println("program ended")
  }

  private def createNewScheduler: Scheduler = AsyncScheduler(
    Scheduler.DefaultScheduledExecutor,
    ExecutionContext.fromExecutor(null),
    UncaughtExceptionReporter.default,
    ExecutionModel.Default
  )

  private def loop[T](task: Task[T],
                      interval: FiniteDuration,
                      initialDelay: FiniteDuration = Duration.Zero): Task[Unit] =
    Observable.timerRepeated(initialDelay, interval, ())
      .mapEval(_ => task)
      .completedL

  private def retry[T](name: String, task: Task[T]): Task[T] =
    task
      .doOnFinish { errOption =>
        println(s"$name ended")
        errOption.foreach(_.printStackTrace())
        errOption.map(Task.raiseError).getOrElse(Task.unit)
      }
      .onErrorRestartLoop(0) { (_, _, retry) => retry(0).delayExecution(5.seconds) }

  private def executeParallelBlocking(tasks: Seq[Task[Unit]]): Unit =
    Task.gatherUnordered(tasks.map(_.executeOn(createNewScheduler))).runSyncUnsafe(Duration.Inf)
}

