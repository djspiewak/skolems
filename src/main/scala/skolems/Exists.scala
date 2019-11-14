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

trait Exists[+F[_]] {
  type A
  def apply(): F[A]
}

object Exists {
  def apply[F[_]]: PartiallyApplied[F] =
    new PartiallyApplied[F]

  final class PartiallyApplied[F[_]] {
    def apply[A0](fa: F[A0]): Exists[F] =
      new Exists[F] {
        type A = A0
        def apply() = fa
      }
  }
}
