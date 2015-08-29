package net.n12n.openid.connect.json

import java.math.BigInteger
import java.security.{KeyFactory, PublicKey}
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

import spray.json.DefaultJsonProtocol

/**
 * Json Web Key.
 *
 * See [[https://tools.ietf.org/html/rfc7517]].
 *
 * @param kty key type (`RSA`)
 * @param alg key algorithm (`RS256`)
 * @param use key use, e.g. `sig` for signing.
 * @param kid key ID.
 * @param n modulus of RSA key.
 * @param e exponent of RSA key.
 */
case class WebKey(kty: String, alg: String, use: String, kid: String,
  n: Option[String], e: Option[String]) {

  def toPubKey(): Option[PublicKey] = {
    val decoder = Base64.getUrlDecoder
    kty match {
      case "RSA" =>
        n.map(base64UInt(_, decoder)).flatMap(mod => e.map(exp => (mod, base64UInt(exp, decoder)))).
          map(t => rsaPubKey(t._1, t._2))
      case _ => None
    }
  }

  private def base64UInt(text: String, decoder: Base64.Decoder): BigInteger = {
    val array = decoder.decode(text)
    if (array(0) < 0) {
      val zero = Array[Byte](0)
      new BigInteger(zero ++ array)
    } else {
      new BigInteger(array)
    }
  }

  private def rsaPubKey(mod: BigInteger, exp: BigInteger): PublicKey =
    KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(mod, exp))
}

object WebKey extends DefaultJsonProtocol {
  implicit def toJson = jsonFormat6(WebKey.apply)
}