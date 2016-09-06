package org.lolhens.untisicalserver.http.client

import dispatch.Http
import dispatch.as
import dispatch.url
import org.lolhens.untisicalserver.http.FakeSSL
import org.lolhens.untisicalserver.http.client.StringReceiver._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 29.08.2016.
  */
class StringReceiver(timeout: Duration = defaultTimeout) {
  private val http = Http.configure(_
    .setSSLContext(FakeSSL.FakeSSLContext())
    .setHostnameVerifier(FakeSSL.FakeHostnameVerifier)
    .setFollowRedirect(true)
    .setConnectTimeout(timeout.toMillis.toInt)
    .setRequestTimeout(timeout.toMillis.toInt))

  def receive(_url: String): Future[String] = {
    val svc = url(_url)
    http(svc OK as.String)
  }
}

object StringReceiver {
  private val defaultTimeout = 5 minutes
}