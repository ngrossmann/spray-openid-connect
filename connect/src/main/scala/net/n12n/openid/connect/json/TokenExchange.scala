package net.n12n.openid.connect.json

import spray.json.DefaultJsonProtocol

/**
 * Token exchange response.
 *
 * @param access_token Access token for user info endpoint
 * @param token_type `Bearer`
 * @param expires_in Expiration time of the Access Token in seconds since
 *                   the response was generated.
 * @param id_token Json web-token containing user id.
 */
case class TokenExchange(access_token: String, token_type: String,
  expires_in: Option[Int], id_token: String) {
  def id = JsonWebToken(id_token)
}

object TokenExchange extends DefaultJsonProtocol {
  implicit val toJson = jsonFormat4(TokenExchange.apply)
}