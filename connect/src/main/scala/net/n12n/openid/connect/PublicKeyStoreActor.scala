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

import java.security.PublicKey

import akka.actor.Status.Failure
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.pattern.pipe
import spray.client.pipelining._
import spray.http.{HttpRequest, Uri}
import spray.httpx.SprayJsonSupport._

import scala.collection.immutable.HashMap
import scala.concurrent.Future

import net.n12n.openid.connect.json.{JwkSet, WebKey}

object PublicKeyStoreActor {
  private[PublicKeyStoreActor] case object Update
  private[PublicKeyStoreActor] case class Update(keys: List[WebKey])
  case object ListKeys
  case class GetKey(kid: String)

  case class KeyList(keys: List[PublicKey])
  case class Key(kid: String, key: Option[PublicKey])
  def props(jwksUri: Uri) = Props(classOf[PublicKeyStoreActor], jwksUri)
}

class PublicKeyStoreActor(jwksUri: Uri) extends Actor with ActorLogging {
  import PublicKeyStoreActor._
  implicit val ec = context.system.dispatcher

  var keyStore = HashMap[String, PublicKey]()
  var pending: List[(ActorRef, String)] = Nil

  override def preStart(): Unit = {
    self ! Update
  }

  override def receive: Receive = {
    case Update =>
      log.debug("Requesting certs update")
      getKeys(jwksUri).map(Update(_)) pipeTo self
    case Update(webKeys) =>
      log.debug("Received new certificates {}", webKeys)
      keyStore = HashMap(webKeys.flatMap(k => k.toPubKey().map((k.kid, _))):_*)
      pending.foreach(t => t._1 ! Key(t._2, keyStore.get(t._2)))
      pending = Nil
    case ListKeys => sender ! KeyList(keyStore.values.toList)
    case GetKey(kid) =>
      keyStore.get(kid) match {
      case Some(key) =>
        sender ! Key(kid, Some(key))
      case None =>
        log.info("Key {} not found updating cache", kid)
        pending = (sender, kid) :: pending
        self ! Update
    }
    case e: Failure =>
      log.error(e.cause, "Certificate update failed")
      pending.foreach(_._1 ! e)
  }

  protected def getKeys(uri: Uri): Future[List[WebKey]] = {
    import JwkSet._
    import WebKey._

    val pipeline: HttpRequest => Future[JwkSet] =
      sendReceive ~> unmarshal[JwkSet]
    pipeline(Get(uri)).map(_.keys)
  }
}
