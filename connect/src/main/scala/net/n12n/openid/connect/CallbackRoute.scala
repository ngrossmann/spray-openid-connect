/*
 * Copyright 2015 Niklas Grossmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

class CallbackRoute(config: ConnectConfig[_], pubKeyStore: PubKeyStore)
                   (implicit system: ActorSystem) extends Route {
  import system.dispatcher
  val log = LoggingContext.fromActorSystem(system)

  override def apply(ctx: RequestContext): Unit = {
    if (ctx.request.uri.path == config.callback.path) {
      val query = ctx.request.uri.query
      val params: Option[(String, String)] =
        query.get("state").zip(query.get("code")).headOption
      params match {
        case Some((state, code)) =>
          config.sessionStore.validateStateToken(state).flatMap(
            uri => requestAccessToken(code, config.callback.resolvedAgainst(ctx.request.uri)).map((_, uri))).onComplete {
            case Success((token, uri)) =>
              log.debug("Starting session for {}, redirecting to {}",
                token.id.claims.email, uri)
              config.sessionStore.startSession(ctx, token).redirect(uri, StatusCodes.Found)
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
      log.debug("Path {} did not match {}", ctx.request.uri.path, config.callback)
      ctx.reject()
    }
  }

  private def requestAccessToken(code: String, redirectUri: Uri)(
                                implicit system: ActorSystem):
  Future[TokenExchange] = {
    log.debug("Requesting access-token for {}", code)
    val params = FormData(Map("code" -> code,
      "client_id" -> config.clientId,
      "client_secret" -> config.clientSecret,
      "redirect_uri" -> redirectUri.toString(),
      "grant_type" -> "authorization_code"))
    sendRequest(Post(config.discoveryDocument.token_endpoint, params)).flatMap {te =>
      pubKeyStore.validate(te.id, config.clientId, config.issuer).map{ _ match {
        case Right(token) => te
        case Left(reason) =>
          throw new SecurityException(s"Token validation failed: $reason")
      }}
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
  def authenticationCallbackRoute(config: ConnectConfig[_])
                                 (implicit system: ActorSystem) =
    new CallbackRoute(config,
      new CachingPubKeyStore(system, config.discoveryDocument.jwks_uri)
  )
}