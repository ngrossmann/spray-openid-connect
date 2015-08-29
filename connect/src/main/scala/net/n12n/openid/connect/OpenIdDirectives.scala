package net.n12n.openid.connect

import akka.actor.ActorSystem
import net.n12n.openid.connect.json.{JsonWebToken, TokenExchange}
import spray.http._
import spray.client.pipelining._
import spray.http._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing._
import shapeless._

import scala.concurrent.{ExecutionContext, Future}
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
            Config.redirectUri.resolvedAgainst(ctx.request.uri)),
            StatusCodes.Found)
        }
      }
      authenticationRoute
    }
  }

  private def authorizationUri(discoveryDocument: DiscoveryDocument,
    stateToken: String, redirectUri: Uri): Uri = {
    val builder = Uri.Query.newBuilder
    builder += ("client_id" -> Config.clientId, "response_type" -> "code",
      "scope" -> Config.scope, "redirect_uri" -> redirectUri.toString(),
      "state" -> stateToken)
    discoveryDocument.authorization_endpoint.withQuery(builder.result())
  }

  // TODO: Implement state token creation
  protected def createStateToken(requestUri: Uri): String = requestUri.toString()
}

object OpenIdDirectives extends OpenIdDirectives
