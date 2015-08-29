package net.n12n.openid.connect.json

import spray.json.DefaultJsonProtocol

case class Claims(iss: String, at_hash: Option[String], email_verified: Option[Boolean],
  sub: String, azp: Option[String], email: Option[String], aud: String, iat: Long,
  exp: Long, hd: Option[String])

object Claims extends DefaultJsonProtocol {
  implicit val toJson = jsonFormat10(Claims.apply)
}