package org.lolhens.untisicalserver.http.client

import dispatch.{Http, as, url}
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import monix.eval.Task
import org.asynchttpclient.util.ProxyUtils
import org.lolhens.untisicalserver.http.client.StringReceiver._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 29.08.2016.
  */
class StringReceiver(timeout: Duration = defaultTimeout) {
  private val http = Http.withConfiguration(_
    .setProxyServerSelector(ProxyUtils.getJdkDefaultProxyServerSelector)
    .setSslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build)
    .setFollowRedirect(true)
    .setConnectTimeout(timeout.toMillis.toInt)
    .setRequestTimeout(timeout.toMillis.toInt)
  )

  def receive(_url: String): Task[String] = {
    val svc = url(_url)
    Task.deferFuture(http(svc OK as.String))
  }
}

object StringReceiver {
  private val defaultTimeout = 5 minutes
}