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

  /**
   * Tricksy overload for when you want everything to "just work(tm)".
   * The implicit modifiers are to allow the compiler to materialize
   * things through implicit search when relevant; don't be afraid to
   * call this function explicitly.
   */
  implicit def apply[F[_], A](implicit fa: F[A]): Exists[F] =
    apply[F](fa)

  // non-implicit parameter version designed to provide nicer syntax
  implicit def coerce[F[_], A](F: F[A]): Exists[F] = apply(F)

  def raise[F[_], B](f: Exists[λ[α => F[α] => B]]): ∀[F] => B =
    af => f()(af[f.A])

  // This cast is required because Scala's type inference will not allow
  // the parameter from the ∀ invocation (which is unreferenceable) to
  // flow "outward" and form the constrained input type of the function.
  // So because we don't have non-local inference in Scala, we need to
  // play tricks with erasure and cast from F[Any].
  def lower[F[_], B](f: ∀[F] => B): Exists[λ[α => F[α] => B]] =
    Exists[λ[α => F[α] => B]]((fa: F[Any]) => f(∀[F](fa.asInstanceOf)))

  def lowerE[F[_], B](f: ∀[F] => B): (F[A] => B) forSome { type A } =
    lower[F, B](f)()

  /**
   * Utilities to implicitly materialize native `forSome` contexts.
   */
  object Implicits {
    implicit def materialize[F[_]](implicit F: Exists[F]): F[A] forSome { type A } = F()
  }
}
