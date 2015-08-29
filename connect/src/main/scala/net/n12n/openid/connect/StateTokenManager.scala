package net.n12n.openid.connect

import spray.http.Uri

trait StateTokenManager {
  /**
   * Create an anti-forgery state token containing the
   * original URI the user requested.
   *
   * @param requestUri original URI the user requested.
   * @return token, not URI encoded.
   */
  def createStateToken(requestUri: Uri): String

  /**
   * Validate state token and extract URI.
   *
   * @param state State token, not URI encoded.
   * @return URI contained in the state token or `None` if
   *         the token was not valid.
   */
  def validateStateToken(state: String): Option[Uri]

}
