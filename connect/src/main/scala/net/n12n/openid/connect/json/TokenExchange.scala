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