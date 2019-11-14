/*
 * Copyright 2019 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package skolems

import scala.util.control.NoStackTrace

trait Forall[F[_]] {
  def apply[A]: F[A]
}

object Forall {

  /**
   * The type signature looks unbelievably confusing, but the usage is pretty easy:
   *
   * Forall[Option](_(None)): Forall[Option]
   *
   * Replace `None` with whatever your body is. Assuming the types can infer and you
   * don't need to refer to A by name, this is a perfectly reasonable approach, and a
   * lot lighter than using the trait encoding.
   */
  def apply[F[_]](p: ¬[¬[F[A]] forSome { type A }]): Forall[F] = new Forall[F] {
    def apply[A] = {
      case class Marker(arg: F[A]) extends NoStackTrace
      try {
        p((arg: F[A]) => throw Marker(arg))
      } catch {
        case Marker(arg) => arg
      }
    }
  }
}
