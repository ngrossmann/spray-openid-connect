package net.n12n.openid.connect

import java.security.{KeyPairGenerator, KeyPair, KeyStore}
import java.util.concurrent.TimeUnit

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

import akka.actor.ActorSystem
import org.scalatest.{Suite, Matchers, FlatSpecLike}
import spray.caching.LruCache
import spray.http._
import spray.routing.{MissingQueryParamRejection, HttpService}
import spray.testkit.{ScalatestRouteTest}

import net.n12n.openid.connect.json.{Claims, JwtHeader, JsonWebToken, TokenExchange}

import HttpHeaders._

class OpenIdDirectivesSpec extends Suite with ScalatestRouteTest with FlatSpecLike
  with HttpService with Matchers {
  val keyPassword = "changeit".toCharArray
  val keyStore = KeyStore.getInstance("JKS")
  keyStore.load(ClassLoader.getSystemResourceAsStream("test.jks"), keyPassword)
  val keyPair = createKeyPair()

  class TestCallbackRoute(config: ConnectConfig[_], system: ActorSystem) extends
    CallbackRoute(config, new StaticPubKeyStore(keyPair.getPublic))(system) {

    override protected def sendRequest(request: HttpRequest)(implicit system: ActorSystem): Future[TokenExchange] = {
      if (request.method == HttpMethods.POST) {
        if (request.entity.asString.contains("code=the_code")) {
          Future.successful(TokenExchange("access_token", "Bearer", Some(100),
            createIdToken().encoded()))
        } else {
          Future.failed(new Exception(s"Invalid code for token exchange ${request.entity.asString}"))
        }
      } else {
        Future.failed(new Exception("Token exchange must use POST"))
      }
    }
  }

  def actorRefFactory = system

  val discoveryDocument = DiscoveryDocument(
    Uri("https://accounts.google.com/o/oauth2/v2/auth"),
    Uri("https://www.googleapis.com/oauth2/v4/token"),
    Uri("https://www.googleapis.com/oauth2/v3/userinfo"),
    Uri("https://www.googleapis.com/oauth2/v3/certs"))
  val cache = LruCache[OidUserContext](1, 1, Duration.Inf, 1 second)
  val stateCache = LruCache[Uri](1, 1, Duration.Inf, 1 second)
  val config = ConnectConfig(system.settings.config,
    new SimpleSessionStore(cache, stateCache, None, system.dispatcher),
    discoveryDocument)
  val route = {
    new TestCallbackRoute(config, system) ~
    OpenIdDirectives.authenticate(config) { user =>
      path("protected") {
        get {
          complete(user.sub)
        }
      }
    }
  }

  "/protected" should s"return redirect to ${discoveryDocument.authorization_endpoint}" in {
    Get("/protected") ~>route ~> check {
      status should be(StatusCodes.Found)
      header[Location].flatMap(_.uri.query.get("state")).
        flatMap(stateCache.get(_)).map(status => Await.result(status, 1 second)) shouldNot be(None)
    }
  }

  "/protected with session" should "return OK" in {
    val userContext = OidUserContext("sub", "access-token", Some("someone@email"))
    cache("session-id", () => Future.successful(userContext))
    val cookie = HttpCookie("net.n12.openid.connect.session", "session-id",
      None, None, None, None, false, false, None)
    Get("/protected").withHeaders(HttpHeaders.`Cookie`(cookie)) ~> route ~> check {
      status should be(StatusCodes.OK)
    }
  }

  "/opend-id-connect" should "create user session" in {
    stateCache("state-id", () => Future.successful(Uri("http://localhost:8080/protected")))
    Get("/open-id-connect?state=state-id&code=the_code") ~>
      route ~> check {
      header[`Set-Cookie`].map(_.cookie) should matchPattern {
        case Some(HttpCookie("net.n12.openid.connect.session", _, _, _, _, _, _, _, _)) =>
      }
    }
  }

  "/open-id-connect" should "reject when state is missing" in {
    stateCache("state-id", () => Future.successful(Uri("http://localhost:8080/protected")))
    Get("/open-id-connect?code=the_code") ~>
      route ~> check {
      status should be(StatusCodes.InternalServerError)
    }
  }

  def createIdToken(): JsonWebToken = {
    val keyProtection = new KeyStore.PasswordProtection(keyPassword)
    val claims = Claims("https://accounts.google.com", None,
      Some(true), "sub", Some("azp"), Some("someone@email"),
      "aud", 0L, 0L, Some("hd"))
    val token = JsonWebToken(claims, keyPair.getPrivate, Some("mykey"))
    token
  }

  def createKeyPair(): KeyPair = {
    val generator = KeyPairGenerator.getInstance("RSA")
    generator.initialize(2048)
    generator.genKeyPair()
  }
}
