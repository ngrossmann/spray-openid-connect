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

import spray.http.Uri

trait StateTokenManager {
  /**
   * Create an anti-forgery state token containing the
   * original URI the user requested.
   *
   * @param requestUri original URI the user requested.
   * @return token, not URI encoded.
   */
  def createStateToken(requestUri: Uri): String

  /**
   * Validate state token and extract URI.
   *
   * @param state State token, not URI encoded.
   * @return URI contained in the state token or `None` if
   *         the token was not valid.
   */
  def validateStateToken(state: String): Option[Uri]

}
