package org.lolhens.untisicalserver

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
    val iCalProvider = new ICalProvider("nixdorf_bk_essen", 183)
    iCalProvider.forRange(2016, -100 to 100) onSuccess {
      case values =>
        println(values)
    }
  }
}

