package net.n12n.openid

/**
 * Utility functions.
 */
package object connect {

  def checkEmpty[T <: Iterable[_]](message: String, i: T): T = {
    if (i.isEmpty)
      throw new IllegalStateException(message)
    else
      i
  }
}
