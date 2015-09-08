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

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.{HttpRequest, Uri}
import spray.json.{JsValue, RootJsonFormat, DefaultJsonProtocol}
import spray.httpx.SprayJsonSupport._
import scala.concurrent.Future

case class DiscoveryDocument(authorization_endpoint: Uri, token_endpoint: Uri,
  userinfo_endpoint: Uri, jwks_uri: Uri)

object DiscoveryDocument extends DefaultJsonProtocol {
  implicit val uriFormat: RootJsonFormat[Uri] = new RootJsonFormat[Uri] {

    override def read(json: JsValue): Uri = Uri(StringJsonFormat.read(json))

    override def write(uri: Uri): JsValue = StringJsonFormat.write(uri.toString())
  }

  implicit val toJson = jsonFormat4(DiscoveryDocument.apply)

  def fetch(uri: Uri)(implicit system: ActorSystem): Future[DiscoveryDocument] = {
    import system.dispatcher
    val pipeline: HttpRequest => Future[DiscoveryDocument] =
      sendReceive ~> unmarshal[DiscoveryDocument]
    pipeline(Get(uri))
  }
}
