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

import java.security.PublicKey
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import spray.http.Uri
import spray.util.LoggingContext

import scala.concurrent.Future

/**
 * A key-store which queries the
 * [[net.n12n.openid.connect.DiscoveryDocument.jwks_uri jwks_uri]] and caches
 * the keys in memory. If an unknown key ID is requested keys are retrieved
 * again.
 *
 * @param system actor system.
 * @param jwksUri URI to retrieve the public keys used by the provider.
 *
 * @see [[net.n12n.openid.connect.PublicKeyStoreActor]]
 *
 */
class CachingPubKeyStore(system: ActorSystem, jwksUri: Uri) extends PubKeyStore {
  override implicit val executionContext = system.dispatcher
  implicit val timeout = akka.util.Timeout(system.settings.config.getDuration(
    "net.n12n.openid.connect.key-store.request-timeout", TimeUnit.MILLISECONDS),
    TimeUnit.MILLISECONDS)
  val actor = system.actorOf(PublicKeyStoreActor.props(jwksUri), "PublicKeyStoreActor")

  /**
   * Retrieve list of all known public keys.
   * @return list of public keys.
   */
  override def keys(): Future[List[PublicKey]] =
    (actor ? PublicKeyStoreActor.ListKeys).mapTo[PublicKeyStoreActor.KeyList].map(_.keys)

  /**
   * Look-up key by ID.
   * @param kid key ID
   * @return Some(key) or None if the key is not known
   */
  override def key(kid: String): Future[Option[PublicKey]] = {
    LoggingContext.fromActorSystem(system).debug("Getting key {}", kid)
    (actor ? PublicKeyStoreActor.GetKey(kid)).mapTo[PublicKeyStoreActor.Key].map(_.key)
  }
}
