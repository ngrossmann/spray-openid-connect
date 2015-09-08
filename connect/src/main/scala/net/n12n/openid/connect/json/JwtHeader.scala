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
 * Json web-token header. See
 * [[https://tools.ietf.org/html/draft-jones-json-web-signature-04#page-6]]
 * for details.
 *
 * @param alg cryptographic algorithm used, see `Alg` child object for valid values.
 * @param typ type of signed content.
 * @param kid key ID, a hint indicating which key owned
 *            by the signer should be used.
 */
case class JwtHeader(alg: String, typ: Option[String], kid: Option[String]) {
  def javaAlg = JwtHeader.alg2java(alg)
}

object JwtHeader extends DefaultJsonProtocol {
  implicit val toJson = jsonFormat3(JwtHeader.apply)

  /**
   * Supported `alg` values.
   */
  object Alg {
    val RsaSha256 = "RS256"
    val RsaSha384 = "RS384"
  }

  def alg2java(alg: String): Option[String] = alg match {
    case Alg.RsaSha256 => Some("SHA256withRSA")
    case Alg.RsaSha384 => Some("SHA384withRSA")
    case _ => None
  }
}
