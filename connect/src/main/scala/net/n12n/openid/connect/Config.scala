package net.n12n.openid.connect

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import spray.http.Uri

object Config {
  private[connect] val config = ConfigFactory.load()
  val redirectUri = Uri(config.getString("net.n12n.openid.connect.redirect-uri"))
  val clientId = config.getString("net.n12n.openid.connect.client-id")
  val clientSecret = config.getString("net.n12n.openid.connect.client-secret")
  val scope = config.getString("net.n12n.openid.connect.scope")
}
