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
