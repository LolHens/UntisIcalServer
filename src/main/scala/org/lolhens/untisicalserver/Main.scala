package org.lolhens.untisicalserver

import monix.eval.Task
import monix.execution.Scheduler.Implicits.global
import org.lolhens.untisicalserver.data.config.Config
import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.util.Utils

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Utils.setLogLevel

    val config = Config.load

    val err0 = Task.sequence(config.schools.map(_.updateCacheContinuously(30.seconds)))

    val iCalServer = new ICalServer(config)
    val err1 = iCalServer.start

    val err2 = Google.updateCalendarContinuously(30.seconds)

    Await.result((for {
      err0Fibre <- err0.fork
      err1Fibre <- err1.fork
      err2Fibre <- err2.fork
      err0 <- err0Fibre.join
      err1 <- err1Fibre.join
      err2 <- err2Fibre.join
    } yield ()).runAsync, Duration.Inf)
  }
}

