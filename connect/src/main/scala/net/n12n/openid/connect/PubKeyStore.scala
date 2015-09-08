package net.n12n.openid.connect

import java.security.{Signature, PublicKey}

import net.n12n.openid.connect.json.{JwtHeader, JsonWebToken}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

trait PubKeyStore {
  protected implicit val executionContext: ExecutionContext
  /**
   * Retrieve list of all known public keys.
   * @return list of public keys.
   */
  def keys(): Future[List[PublicKey]]

  /**
   * Look-up key by ID.
   * @param kid key ID
   * @return Some(key) or None if the key is not known
   */
  def key(kid: String): Future[Option[PublicKey]]

  /**
   * Validate ID token.
   * @param token json web token.
   * @param clientId client ID.
   * @param issPattern
   * @return
   */
  def validate(token: JsonWebToken, clientId: String, issPattern: Regex):
  Future[Either[String, JsonWebToken]] = {
    val alg = token.header.javaAlg.map(Signature.getInstance(_))

    if (token.claims.aud != clientId) {
      Future.successful(Left(
        s"aud claim `${token.claims.aud}` does not match client-id `$clientId`"))
    } else if (issPattern.findFirstIn(token.claims.iss).isEmpty) {
      Future.successful(Left(
        s"iss claim does not match ${token.claims.iss} ${issPattern.pattern}"))
    } else if (token.claims.exp >= System.currentTimeMillis() * 1000) {
      Future.successful(Left(s"token expired at ${token.claims.exp}"))
    } else {
      // TODO if kid is provided but the key is not found, should
      // the list of all keys be tried or an error returned?
      val pubKeys: Future[List[PublicKey]] = token.header.kid.map(key(_).
        map(_.toList)).getOrElse(keys()).map(
          checkEmpty("No certificates found", _))

      def validate(signatureAlg: Signature, pubKey: PublicKey,
                   content: Array[Byte], signature: Array[Byte]): Boolean = {
        signatureAlg.initVerify(pubKey)
        signatureAlg.update(content)
        signatureAlg.verify(signature)
      }

      val content = token.content.getBytes("utf-8")
      pubKeys.map(_.exists(validate(alg.get, _, content, token.signature))).
        map(if (_) Right(token) else Left("Signature validation failed"))
    }
  }
}
