package org.lolhens.untisicalserver

import org.lolhens.untisicalserver.data.config.Config
import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.util.Utils

import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Utils.setLogLevel

    val config = Config.load

    val iCalServer = new ICalServer(config)
    iCalServer.start()

    Google.updateCalendar()
  }
}

