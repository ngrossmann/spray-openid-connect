/*
 * Copyright 2015 Niklas Grossmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
  /** ID token issuer (iss claim) pattern. */
  val issuer = config.getString("net.n12n.openid.connect.issuer").r
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
