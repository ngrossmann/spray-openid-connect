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

case class Claims(iss: String, at_hash: Option[String], email_verified: Option[Boolean],
  sub: String, azp: Option[String], email: Option[String], aud: String, iat: Long,
  exp: Long, hd: Option[String])

object Claims extends DefaultJsonProtocol {
  implicit val toJson = jsonFormat10(Claims.apply)
}