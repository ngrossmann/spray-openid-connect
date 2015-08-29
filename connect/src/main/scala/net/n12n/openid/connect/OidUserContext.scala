package net.n12n.openid.connect

/**
 *
 * @param sub subject (sub) from ID token.
 * @param accessToken access token.
 * @param email user's email address.
 */
case class OidUserContext(sub: String, accessToken: String, email: Option[String])
