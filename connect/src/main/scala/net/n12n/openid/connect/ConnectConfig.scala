package net.n12n.openid.connect

import spray.http.Uri

/**
 * Client configuration container.
 */
class ConnectConfig[T](config: com.typesafe.config.Config,
                       val sessionStore: UserSessionStore[T],
                       val discoveryDocument: DiscoveryDocument) {
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
}

object ConnectConfig {
  def apply[T](config: com.typesafe.config.Config,
               sessionStore: UserSessionStore[T],
               discoveryDocument: DiscoveryDocument) =
  new ConnectConfig[T](config, sessionStore, discoveryDocument)
}
