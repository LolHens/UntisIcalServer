package org.lolhens.untisicalserver

import ch.qos.logback.classic.{Level, Logger}
import org.lolhens.untisicalserver.data.SchoolClass
import org.lolhens.untisicalserver.http.server.ICalServer
import org.lolhens.untisicalserver.ical.CachedICalProvider
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  val iCalProvider = new CachedICalProvider(SchoolClass.nixdorfFs15b, 1 minute)
  val iCalServer = new ICalServer(iCalProvider)

  def main(args: Array[String]): Unit = {
    val root = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[Logger]
    root.setLevel(Level.INFO)

    iCalServer.start()
  }
}

