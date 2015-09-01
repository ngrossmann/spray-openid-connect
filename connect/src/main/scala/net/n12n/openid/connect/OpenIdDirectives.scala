package net.n12n.openid.connect

import spray.http._
import spray.routing._
import shapeless._

import scala.util.{Failure, Success}

trait OpenIdDirectives {

  def authenticate[T](sessionStore: UserSessionStore[T],
    discoveryDocument: DiscoveryDocument):
    Directive1[T] = new Directive1[T] {
    import sessionStore.executionContext
    override def happly(f: (::[T, HNil]) => Route): Route = {
      val authenticationRoute: Route = ctx => {
        sessionStore.fromRequest(ctx) match {
          case Some(future) => future.onComplete {
            case Success(userContext) => f(userContext :: HNil)(ctx)
            case Failure(e) => ctx.failWith(e)
          }
          case None => ctx.redirect(authorizationUri(discoveryDocument,
            sessionStore.stateToken(ctx.request.uri),
            Config.redirectUri(ctx.request.uri)),
            StatusCodes.Found)
        }
      }
      authenticationRoute
    }
  }

  /**
   * Redirect to authorization provider.
   * @param discoveryDocument discovery document with the providers
   *                          authorization end-point.
   * @param stateToken anti-forgery state token.
   * @param redirectUri Open ID connect callback end-point.
   * @return URI to redirect client to.
   */
  private def authorizationUri(discoveryDocument: DiscoveryDocument,
    stateToken: String, redirectUri: Uri): Uri = {
    val builder = Uri.Query.newBuilder
    builder += ("client_id" -> Config.clientId, "response_type" -> "code",
      "scope" -> Config.scope, "redirect_uri" -> redirectUri.toString(),
      "state" -> stateToken)
    discoveryDocument.authorization_endpoint.withQuery(builder.result())
  }
}

object OpenIdDirectives extends OpenIdDirectives
