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

import java.security.{PrivateKey, Signature, KeyStore}
import java.util.Base64

import spray.json._
import JwtHeader._


/**
 * Json Web Token, see [http://openid.net/specs/draft-jones-json-web-token-07.html].
 * @param header token meta-data.
 * @param claims user claims.
 * @param content the signed part of the token, header and claims BASE64 encoded.
 * @param signature token signature.
 */
case class JsonWebToken(val header: JwtHeader, val claims: Claims,
  content: String, signature: Array[Byte]) {
  def encoded(): String = {
    val encoder = Base64.getUrlEncoder
    content + "." + encoder.encodeToString(signature)
  }
}

object JsonWebToken {
  /**
   * Read Json Web Token from base64 encoded string.
   * @param token base64 encoded string.
   * @return Json Web Token, not validated.
   */
  def apply(token: String): JsonWebToken = {
    val decoder = Base64.getUrlDecoder
    val parts = token.split("\\.")
    if (parts.length == 3) {
      val signedContent = s"${parts(0)}.${parts(1)}"
      val signature = decoder.decode(parts(2))
      val header = new String(decoder.decode(parts(0)), "utf-8").
        parseJson.convertTo[JwtHeader]
      val claims = new String(decoder.decode(parts(1))).
        parseJson.convertTo[Claims]
      JsonWebToken(header, claims, signedContent, signature)
    } else {
      throw new IllegalArgumentException(s"${token} is not a valid web-token")
    }
  }

  /**
   * Create a signed web-token.
   * @param claims claims of token.
   * @param privateKey key used to sign token.
   * @param kid optional key ID.
   * @return signed token.
   */
  def apply(claims: Claims, privateKey: PrivateKey, kid: Option[String]): JsonWebToken = {
    val encoder = Base64.getUrlEncoder.withoutPadding()
    val header = JwtHeader(JwtHeader.Alg.RsaSha256, None, kid)
    val content =
      encoder.encodeToString(header.toJson.compactPrint.getBytes("utf-8")) +
      "." + encoder.encodeToString(claims.toJson.compactPrint.getBytes("utf-8"))

    val alg = Signature.getInstance("SHA256withRSA")
    alg.initSign(privateKey)
    alg.update(content.getBytes("utf-8"))
    val signature = alg.sign()
    JsonWebToken(header, claims, content, signature)
  }
}
