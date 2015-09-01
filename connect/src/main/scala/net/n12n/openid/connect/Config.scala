package net.n12n.openid.connect

import java.util.concurrent.TimeUnit

import com.typesafe.config.ConfigFactory
import spray.http.Uri

/**
 * Utility class to read configuration.
 */
object Config {
  private[connect] val config = ConfigFactory.load()
  /** OAuth callback path. `net.n12n.openid.connect.callback-path`*/
  val callback = Uri(config.getString("net.n12n.openid.connect.callback-path"))
  /** Google client ID of application. `net.n12n.openid.connect.client-id`*/
  val clientId = config.getString("net.n12n.openid.connect.client-id")
  /** Google client secrete. `net.n12n.openid.connect.client-secret` */
  val clientSecret = config.getString("net.n12n.openid.connect.client-secret")
  /**
   * Scope parameter used in authentication request.
   * `net.n12n.openid.connect.scope`
   */
  val scope = config.getString("net.n12n.openid.connect.scope")

  /**
   * Build callback URI based on request scheme and authority and
   * [[net.n12n.openid.connect.Config.callback]]
   * @param requestUri
   * @return
   */
  def redirectUri(requestUri: Uri): Uri = callback.resolvedAgainst(requestUri)
}
