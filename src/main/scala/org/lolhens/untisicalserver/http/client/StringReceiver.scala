package org.lolhens.untisicalserver.http.client

import dispatch.Http
import dispatch.as
import dispatch.url
import org.lolhens.untisicalserver.http.FakeSSL

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 29.08.2016.
  */
object StringReceiver {
  private val defaultTimeout = 5 minutes

  def receive(_url: String)(timeout: Duration = defaultTimeout): Future[String] = {
    val svc = url(_url)
    Http.configure(_
      .setSSLContext(FakeSSL.FakeSSLContext())
      .setHostnameVerifier(FakeSSL.FakeHostnameVerifier)
      .setFollowRedirect(true)
      .setConnectTimeout(timeout.toMillis.toInt)
      .setRequestTimeout(timeout.toMillis.toInt))(svc OK as.String)
  }
}
