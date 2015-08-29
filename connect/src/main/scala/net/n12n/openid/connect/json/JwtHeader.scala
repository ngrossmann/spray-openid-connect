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
