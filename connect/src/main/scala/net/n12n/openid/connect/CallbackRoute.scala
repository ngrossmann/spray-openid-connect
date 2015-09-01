package net.n12n.openid.connect

import akka.actor.ActorSystem
import net.n12n.openid.connect.json.TokenExchange
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing.AuthenticationFailedRejection.CredentialsRejected
import spray.routing.{MissingQueryParamRejection, AuthenticationFailedRejection, RequestContext, Route}
import spray.util.LoggingContext

import scala.concurrent.Future
import scala.util.{Failure, Success}

class CallbackRoute(sessionStore: UserSessionStore[_],
    discoveryDocument: DiscoveryDocument, pubKeyStore: PubKeyStore)(
    implicit system: ActorSystem) extends Route {
  import system.dispatcher
  val log = LoggingContext.fromActorSystem(system)

  override def apply(ctx: RequestContext): Unit = {
    if (ctx.request.uri.path == Config.callback.path) {
      val query = ctx.request.uri.query
      val params: Option[(String, String)] =
        query.get("state").zip(query.get("code")).headOption
      params match {
        case Some((state, code)) =>
          sessionStore.validateStateToken(state).flatMap(
            uri => requestAccessToken(code, discoveryDocument,
            Config.redirectUri(ctx.request.uri)).map((_, uri))).onComplete {
            case Success((token, uri)) =>
              log.debug("Starting session for {}, redirecting to {}",
                token.id.claims.email, uri)
              sessionStore.startSession(ctx, token).redirect(uri, StatusCodes.Found)
            case Failure(e) =>
              log.warning("User authentication failed: {}", e.getMessage)
              ctx.failWith(e)
          }
        case None =>
          log.debug("Could not extract state and access code from {}",
            ctx.request.uri)
          ctx.failWith(new IllegalUriException("Invalid OpenID Connect response"))
      }
    } else {
      log.debug("Path {} did not match {}", ctx.request.uri.path,
        Config.callback)
      ctx.reject()
    }
  }

  private def requestAccessToken(code: String,
                                 discoveryDocument: DiscoveryDocument,
                                  redirectUri: Uri)(
                                implicit system: ActorSystem):
  Future[TokenExchange] = {
    log.debug("Requesting access-token for {}", code)
    val params = FormData(Map("code" -> code, "client_id" -> Config.clientId,
      "client_secret" -> Config.clientSecret,
      "redirect_uri" -> redirectUri.toString(),
      "grant_type" -> "authorization_code"))
    sendRequest(Post(discoveryDocument.token_endpoint, params)).flatMap {te =>
      pubKeyStore.validate(te.id).map(
        if(_) te else throw new SecurityException("Token validation failed"))
    }
  }

  protected def sendRequest(request: HttpRequest)(implicit system: ActorSystem):
    Future[TokenExchange] = {
    import TokenExchange._
    import system.dispatcher
    val pipeline: HttpRequest => Future[TokenExchange] = sendReceive ~> unmarshal[TokenExchange]
    pipeline(request)
  }
}

object CallbackRoute {
  def authenticationCallbackRoute[T](sessionStore: UserSessionStore[T],
    discoveryDocument: DiscoveryDocument)(implicit system: ActorSystem) =
      new CallbackRoute(sessionStore, discoveryDocument,
        new CachingPubKeyStore(system, discoveryDocument.jwks_uri)
  )
}