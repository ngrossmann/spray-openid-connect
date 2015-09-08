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

import spray.http._
import spray.routing._
import shapeless._

import scala.util.{Failure, Success}

trait OpenIdDirectives {

  /**
   * Ensure user is authenticated or redirect to authentication provider.
   * Inner routes should have take a the user-context of type T as parameter.
   *
   * @param config connect configuration.
   * @tparam T user-context object
   * @return authentication directive.
   */
  def authenticate[T](config: ConnectConfig[T]):
    Directive1[T] = new Directive1[T] {
    import config.sessionStore.executionContext
    override def happly(f: (::[T, HNil]) => Route): Route = {
      val authenticationRoute: Route = ctx => {
        config.sessionStore.fromRequest(ctx) match {
          case Some(future) => future.onComplete {
            case Success(userContext) => f(userContext :: HNil)(ctx)
            case Failure(e) => ctx.failWith(e)
          }
          case None => ctx.redirect(authorizationUri(config.discoveryDocument,
            config.sessionStore.stateToken(ctx.request.uri),
            config.callback.resolvedAgainst(ctx.request.uri),
            config.scope,
            config.clientId),
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
    stateToken: String, redirectUri: Uri, scope: String, clientId: String): Uri = {
    val builder = Uri.Query.newBuilder
    builder += ("client_id" -> clientId, "response_type" -> "code",
      "scope" -> scope, "redirect_uri" -> redirectUri.toString(),
      "state" -> stateToken)
    discoveryDocument.authorization_endpoint.withQuery(builder.result())
  }
}

object OpenIdDirectives extends OpenIdDirectives
