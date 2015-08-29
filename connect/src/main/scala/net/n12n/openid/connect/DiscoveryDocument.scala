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
