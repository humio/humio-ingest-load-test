package com.humio.perftest

import java.security.cert.{ CertificateException, X509Certificate }
import javax.net.ssl.{ HostnameVerifier, HttpsURLConnection, SSLContext, SSLSession, TrustManager, X509TrustManager }

object NoTlsCheck {
  def init(): Unit = {
    val tm: X509TrustManager = new X509TrustManager() {
      override def checkClientTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
      override def checkServerTrusted(chain: Array[X509Certificate], authType: String): Unit = {}
      override def getAcceptedIssuers: Array[X509Certificate] = null
    }
    val trustAllCerts: Array[TrustManager] = Array[TrustManager](tm)

    val sc = SSLContext.getInstance("SSL")
    sc.init(null, trustAllCerts, new java.security.SecureRandom())
    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory)
    // Create all-trusting host name verifier
    val validHosts: HostnameVerifier = new HostnameVerifier() {
      override def verify(arg0: String, arg1: SSLSession) = true
    }
    // All hosts will be valid
    HttpsURLConnection.setDefaultHostnameVerifier(validHosts)
  }
}

