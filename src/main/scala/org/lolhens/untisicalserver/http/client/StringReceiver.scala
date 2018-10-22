package org.lolhens.untisicalserver.http.client

import java.security.SecureRandom
import java.security.cert.X509Certificate

import com.ning.http.util.ProxyUtils
import dispatch.{Http, as, url}
import javax.net.ssl._
import monix.eval.Task
import org.lolhens.untisicalserver.http.client.StringReceiver._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.language.postfixOps

/**
  * Created by pierr on 29.08.2016.
  */
class StringReceiver(timeout: Duration = defaultTimeout) {
  private val http = Http.configure(_
    .setProxyServerSelector(ProxyUtils.getJdkDefaultProxyServerSelector)
    .setSSLContext(FakeSSL.FakeSSLContext())
    .setHostnameVerifier(FakeSSL.FakeHostnameVerifier)
    .setFollowRedirect(true)
    .setConnectTimeout(timeout.toMillis.toInt)
    .setRequestTimeout(timeout.toMillis.toInt))

  def receive(_url: String): Task[String] = {
    val svc = url(_url)
    Task.deferFuture(http(svc OK as.String))
  }
}

object StringReceiver {
  private val defaultTimeout = 5 minutes

  object FakeSSL {

    object FakeHostnameVerifier extends HostnameVerifier {
      def verify(hostname: String, session: SSLSession): Boolean = true
    }

    private class FakeX509TrustManager extends X509TrustManager {
      def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

      def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = ()

      def getAcceptedIssuers: Array[X509Certificate] = Array.empty
    }

    object FakeSSLContext {
      private lazy val _trustManagers = Array[TrustManager](new FakeX509TrustManager())

      def apply(): SSLContext = {
        val context = SSLContext.getInstance("TLS")
        context.init(null, _trustManagers, new SecureRandom())
        context
      }
    }

  }

}