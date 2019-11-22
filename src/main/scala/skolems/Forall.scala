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

import scala.annotation.unchecked.uncheckedVariance

private[skolems] sealed trait Parent {
  private[skolems] type Apply[A]
}

trait Forall[+F[_]] extends Parent { outer =>
  private[skolems] type Apply[A] = F[A] @uncheckedVariance
  def apply[A]: F[A]
}

object Forall {
  // cannot be referenced outside of Forall
  private[Forall] type τ

  /**
   * This function is intended to be invoked explicitly, the implicit
   * modifiers are simply because the compiler can infer this in many
   * cases. For example:
   *
   * implicit def eitherMonad[A]: Monad[Either[A, ?]] = ???
   *
   * implicitly[∀[α => Monad[Either[α, ?]]]]
   *
   * The above will work.
   */
  def apply[F[_]](ft: F[τ]): Forall[F] =
    new Forall[F] {
      def apply[A] = ft.asInstanceOf[F[A]]
    }

  /**
   * This is the implicit version of apply, but restructured and encoded
   * such that the F is unconstrained in in arity or fixity.
   */
  implicit def materialize[T <: Parent](implicit ft: T#Apply[τ]): T =
    apply(ft).asInstanceOf[T]

  def raise[F[_], B](f: Forall[λ[α => F[α] => B]]): ∃[F] => B =
    ef => f[ef.A](ef.value)

  def lower[F[_], B](f: ∃[F] => B): Forall[λ[α => F[α] => B]] =
    Forall[λ[α => F[α] => B]](fa => f(∃[F](fa)))

  def lowerA[F[_], B]: PartiallyAppliedLowerA[F, B] =
    new PartiallyAppliedLowerA[F, B]

  final class PartiallyAppliedLowerA[F[_], B] {
    def apply[A](f: ∃[F] => B): F[A] => B =
      lower[F, B](f)[A]
  }

  /**
   * Utilities to implicitly materialize let-bound polymorphic contexts.
   */
  /*object Implicits {
    implicit def materializeUnification[P <: Parent, A, E](
        implicit P: P,
        ev: P#Apply[E] <:< A): A =
      ev(P.asInstanceOf[Forall[P.Apply]][E])
  }*/
}
