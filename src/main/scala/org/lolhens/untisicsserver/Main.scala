package org.lolhens.untisicsserver

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

import dispatch._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}


/**
  * Created by pierr on 29.08.2016.
  */
object Main {
  def main(args: Array[String]): Unit = {
    val iCalString = StringReceiver.receive("https://mese.webuntis.com/WebUntis/Ical.do?school=nixdorf_bk_essen&elemType=1&elemId=183&rpt_sd=2016-08-29")
    iCalString.map(ICalFile.parse(_))
  }
}

