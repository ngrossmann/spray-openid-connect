package net.n12n.openid.connect

import java.security.{Signature, PublicKey}

import net.n12n.openid.connect.json.{JwtHeader, JsonWebToken}

import scala.concurrent.{ExecutionContext, Future}

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

  def validate(token: JsonWebToken): Future[Boolean] = {
    val alg = token.header.javaAlg.map(Signature.getInstance(_))

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
    pubKeys.map(_.exists(validate(alg.get, _, content, token.signature)))
  }
}
