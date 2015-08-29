package net.n12n.openid.connect.test

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.{KeyManager, TrustManagerFactory, KeyManagerFactory, SSLContext}

import akka.actor.ActorSystem
import spray.http.{Uri, HttpHeaders}
import spray.routing.{RequestContext, SimpleRoutingApp}
import net.n12n.openid.connect._
import spray.util.LoggingContext

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App with SimpleRoutingApp {
  implicit val system = ActorSystem("openid-connect-test")
  import system.dispatcher

  val discoveryDocument = Await.result(DiscoveryDocument.fetch(
    Uri(system.settings.config.getString("discovery-uri"))), 5 seconds)

  system.log.info(s"Discovery document: $discoveryDocument")

  val userSessionStore = SimpleSessionStore(system.dispatcher)

  implicit val sslContext: SSLContext = {
    val context = SSLContext.getInstance("TLSv1.2")
    val keyStore = KeyStore.getInstance("JKS")
    val password = "changeit".toCharArray
    val stream = Thread.currentThread().getContextClassLoader.
      getResourceAsStream("localhost.jks")
    if (stream == null || stream.available() <= 0)
      throw new IllegalArgumentException("keystore not found: " + stream)
    keyStore.load(stream,
      password)
    val kmf = KeyManagerFactory.getInstance("SunX509")
    kmf.init(keyStore, password)
    val tmf = TrustManagerFactory.getInstance("SunX509")
    tmf.init(keyStore)
    context.init(kmf.getKeyManagers, null, null)
    context
  }

  startServer(interface = "0.0.0.0", port = 8080) {
    CallbackRoute.authenticationCallbackRoute(userSessionStore, discoveryDocument) ~
    OpenIdDirectives.authenticate(userSessionStore, discoveryDocument) { user: OidUserContext =>
      path("protected") {
        get {
          complete(s"${user.sub}: ${user.email.getOrElse("")}")
        }
      }
    }
  }
}
