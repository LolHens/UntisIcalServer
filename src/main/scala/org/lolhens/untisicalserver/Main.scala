package org.lolhens.untisicalserver

import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.util.Utils

import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  val iCalServer = new ICalServer()

  def main(args: Array[String]): Unit = {
    Utils.setLogLevel

    //iCalServer.start()

    Google.updateCalendar()
  }
}

