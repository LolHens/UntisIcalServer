package org.lolhens.untisicalserver

import org.lolhens.untisicalserver.ical.CachedICalProvider

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  val nixdorfFs15b = SchoolClass("nixdorf_bk_essen", "FS-15B", 183)

  def main(args: Array[String]): Unit = {
    val calendar = new CachedICalProvider(nixdorfFs15b, 1 minute)()

    println(calendar)
  }
}

