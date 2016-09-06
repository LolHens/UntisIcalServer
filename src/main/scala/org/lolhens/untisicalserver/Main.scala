package org.lolhens.untisicalserver

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.lolhens.untisicalserver.http.server.ICalServer
import org.slf4j.LoggerFactory

import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  val iCalServer = new ICalServer()

  def main(args: Array[String]): Unit = {
    val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    root.setLevel(Level.INFO)

    iCalServer.start()
  }
}

