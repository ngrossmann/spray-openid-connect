package net.n12n.openid.connect

import java.security.{KeyStore, PublicKey}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Public key store always returning the same key.
 */
class StaticPubKeyStore(pubKey: PublicKey) extends PubKeyStore {
  override protected implicit val executionContext =
    scala.concurrent.ExecutionContext.global

  /**
   * Look-up key by ID.
   * @param kid key ID
   * @return Some(key) or None if the key is not known
   */
  override def key(kid: String): Future[Option[PublicKey]] =
    Future.successful(Some(pubKey))

  /**
   * Retrieve list of all known public keys.
   * @return list of public keys.
   */
  override def keys(): Future[List[PublicKey]] =
    Future.successful(List(pubKey))

}
