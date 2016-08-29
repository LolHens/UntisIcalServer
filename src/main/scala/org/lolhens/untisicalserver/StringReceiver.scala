package org.lolhens.untisicalserver

import dispatch.{Http, as, url}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by pierr on 29.08.2016.
  */
object StringReceiver {
  def receive(_url: String): Future[String] = {
    val svc = url(_url)
    Http.configure(_
      .setSSLContext(FakeSSL.FakeSSLContext())
      .setHostnameVerifier(FakeSSL.FakeHostnameVerifier)
      .setFollowRedirect(true))(svc OK as.String)
  }
}
