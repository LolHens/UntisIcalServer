package org.lolhens.untisicalserver

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl._

/**
  * Created by pierr on 29.08.2016.
  */
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

    def apply() = {
      val context = SSLContext.getInstance("TLS")
      context.init(null, _trustManagers, new SecureRandom())
      context
    }
  }

}
