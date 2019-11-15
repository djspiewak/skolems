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

  def raise[G[_], B](implicit ev: F[τ] <:< ((G[E] => B) forSome { type E })): ∀[G] => B = {
    // this actually doesn't work if you write it as a type lambda (probably scalac bugs)
    type L[α] = F[α] <:< ((G[E] => B) forSome { type E })

    Exists.raise(Exists[λ[α => G[α] => B]](∀.of[L](ev)[A](apply())))
  }
}

object Exists {

  /**
   * A forgetful constructor which packs a concrete value into an existential.
   * This is mostly useful for explicitly assisting the compiler in sorting all
   * of this out.
   */
  def apply[F[_]]: PartiallyApplied[F] =
    new PartiallyApplied[F]

  final class PartiallyApplied[F[_]] {
    def apply[A0](fa: F[A0]): Exists[F] =
      new Exists[F] {
        type A = A0
        def apply() = fa
      }
  }

  def raise[F[_], B](f: Exists[λ[α => F[α] => B]]): ∀[F] => B =
    af => f()(af[f.A])

  def lower[F[_], B](f: ∀[F] => B): Exists[λ[α => F[α] => B]] =
    Exists[λ[α => F[α] => B]]((fa: F[τ]) => f(∀.of(fa)))

  def lowerE[F[_], B](f: ∀[F] => B): (F[A] => B) forSome { type A } =
    lower[F, B](f)()

  implicit def of[F[_], A](implicit F: F[A]): Exists[F] = Exists[F](F)

  // non-implicit parameter version designed to provide nicer syntax
  implicit def ofDirect[F[_], A](F: F[A]): Exists[F] = of(F)

  /**
   * Utilities to implicitly materialize native `forSome` contexts.
   */
  object Implicits {
    implicit def materialize[F[_]](implicit F: Exists[F]): F[A] forSome { type A } = F()
  }
}
