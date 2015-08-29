package net.n12n.openid.connect.json

import spray.json.DefaultJsonProtocol

/**
 * Json web key set.
 *
 * An array of json web keys, see
 * [[https://tools.ietf.org/html/rfc7517#section-5]].
 */
case class JwkSet(keys: List[WebKey])

object JwkSet extends DefaultJsonProtocol {
  implicit def toJson = jsonFormat1(JwkSet.apply)
}
