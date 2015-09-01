package net.n12n.openid.connect

import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

import scala.concurrent.duration._
import net.n12n.openid.connect.json.TokenExchange
import spray.caching.{LruCache, Cache}
import spray.http.HttpHeaders.`Set-Cookie`
import spray.http.{Uri, DateTime, HttpCookie}
import spray.routing.RequestContext

import scala.concurrent.{ExecutionContext, Future}

/**
 * [net.n12n.openid.connect.UserSessionStore] using [spray.caching.LruCache].
 * @param cache user session cache.
 * @param stateCache state cache, storing state during authentication.
 * @param ec execution context.
 */
class SimpleSessionStore(cache: Cache[OidUserContext], stateCache: Cache[Uri],
                         cookieDomain: Option[String], ec: ExecutionContext)
  extends UserSessionStore[OidUserContext] {
  override implicit val executionContext = ec
  val cookieName = "net.n12.openid.connect.session"
  val sessionTimeout = 20 minutes
  val path = "/"

  override def fromRequest(ctx: RequestContext): Option[Future[OidUserContext]] = {
    ctx.request.cookies.find(_.name == cookieName).flatMap(
      cookie => cache.get(cookie.content))
  }

  /**
   * Start new session for user.
   * - Create and set session cookie.
   * - Update stored user-context
   *
   * The access token may be used to request user profile information
   * from the identity provider.
   *
   * @todo How to redirect to a new user sign-in page?
   *
   * @param ctx request context.
   * @param tokenExchange access token and id token.
   * @return new request context with session cookie.
   */
  override def startSession(ctx: RequestContext, tokenExchange: TokenExchange):
    RequestContext = {
    val idToken = tokenExchange.id
    val userContext = OidUserContext(idToken.claims.sub, tokenExchange.access_token,
      idToken.claims.email)
    val sessionId = UUID.randomUUID().toString
    cache(sessionId, () => Future.successful(userContext))
    ctx.withHttpResponseHeadersMapped(`Set-Cookie`(cookie(ctx.request.uri, sessionId)) :: _)
  }

  private def cookie(requestUri: Uri, value: String) = {
    val secure = requestUri.scheme == "https"
    HttpCookie(cookieName, value,
      None, None, cookieDomain, Some(path), secure, /*httpOnly*/ false, None)
  }

  /**
   * Create anti-forgery state token.
   * @param uri Request URI to be stored with state token
   * @return State token to be added to URI.
   */
  override def stateToken(uri: Uri): String = {
    val token = UUID.randomUUID().toString
    stateCache(token, () => Future.successful(uri))
    token
  }

  override def validateStateToken(state: String): Future[Uri] = {
    stateCache.get(state).getOrElse(Future.failed(
      new IllegalStateException(s"Invalid state token ${state}")))
  }
}

object SimpleSessionStore {
  def apply(config: com.typesafe.config.Config)
           (implicit executionContext: ExecutionContext) = {
    val cookieDomain = config.getString(
      "net.n12n.openid.connect.session.cookie-domain")

    new SimpleSessionStore(
      LruCache(
        config.getInt("net.n12n.openid.connect.session.max-cache-size"),
        config.getInt("net.n12n.openid.connect.session.initial-cache-size"),
        Duration.Inf,
        config.getDuration("net.n12n.openid.connect.session.timeout",
          TimeUnit.MILLISECONDS).millis
      ),
      LruCache(
        config.getInt("net.n12n.openid.connect.state-token.max-cache-size"),
        config.getInt("net.n12n.openid.connect.state-token.initial-cache-size"),
        Duration.Inf,
        config.getDuration("net.n12n.openid.connect.state-token.timeout",
          TimeUnit.MILLISECONDS).millis
      ),
      if (cookieDomain == "") None else Some(cookieDomain),
      executionContext)
  }
}