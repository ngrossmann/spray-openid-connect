package net.n12n.openid.connect

import net.n12n.openid.connect.json.TokenExchange
import spray.http.Uri
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * Manage session and user data.
 *
 * How to manage sessions:
 *
 * Option 1:
 * Servlet engin like. The cookie contains a unique ID, the server
 * maintains a hashmap which maps this ID to session data, stored
 * either in memory or a DB.
 * Session data contains:
 *   - sub
 *   - email
 *   - access token.
 *
 * Option 2:
 * Encrypt session data and store it in a cookie.
 *
 * - Manage session (update and expire) and update session-cookie
 * - Store user-context in cache, on disk, in DB, ....
 * - Request user profile information from identity provider and
 *  update the stored user-context.
 *
 *  Primary key to identify a user is the `sub` from the ID token.
 *
 * @tparam T user-context
 */
trait UserSessionStore[T] {
  implicit val executionContext: ExecutionContext

  /**
   * Retrieve cookie from request and map it to user-data.
   * - Must ensure cookie is valid.
   * - Must ensure session did not expire.
   * @param ctx request context.
   * @return `Some(userContext)` if cookie was valid and session not expired
   *        `None` otherwise.
   */
  def fromRequest(ctx: RequestContext): Option[Future[T]]

  /**
   * Start new session for user.
   * - Create and set session cookie.
   * - Update stored user-context
   *
   * The access token may be used to request user profile information
   * from the identity provider.
   *
   * @param ctx request context.
   * @param tokenExchange access token and id token.
   * @return new request context with session cookie.
   */
  def startSession(ctx: RequestContext, tokenExchange: TokenExchange): RequestContext

  /**
   * Create anti-forgery state token.
   * @param uri Request URI to be stored with state token
   * @return State token to be added to URI.
   */
  def stateToken(uri: Uri): String


  def validateStateToken(state: String): Future[Uri]

}
