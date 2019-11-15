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

trait Forall[+F[_]] { outer =>
  def apply[A]: F[A]

  def raise[G[_], B](implicit ev: F[τ] <:< (G[τ] => B)): ∃[G] => B =
    Forall.raise(new Forall[λ[α => G[α] => B]] {
      // we do this in two stages because it's actually unsound to do it in one
      def apply[A]: G[A] => B =
        Forall.of[λ[α => G[α] => B]](
          Forall.of[λ[α => F[α] <:< (G[τ] => B)]](ev)[A](outer[A]))[A]
    })
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

  def raise[F[_], B](f: Forall[λ[α => F[α] => B]]): ∃[F] => B =
    ef => f[ef.A](ef())

  def lower[F[_], B](f: ∃[F] => B): Forall[λ[α => F[α] => B]] =
    Forall[λ[α => F[α] => B]](_(fa => f(∃[F](fa))))

  def lowerA[F[_], B]: PartiallyAppliedLowerA[F, B] =
    new PartiallyAppliedLowerA[F, B]

  final class PartiallyAppliedLowerA[F[_], B] {
    def apply[A](f: ∃[F] => B): F[A] => B =
      lower[F, B](f)[A]
  }

  /**
   * Be very careful with this! It's relatively easy to do things that are
   * entirely unsound. For example: `Bar[τ] => Baz[String, τ]`.
   * It is *tempting* to read this type as being equivalent to
   * `∀[α => Bar[α] => Baz[String, α]]`. However, that is not the case! τ always
   * binds to the innermost scope, similar to the `?` operator in kind-projector.
   * So the type is *actually* `∀[Bar] => ∀[Baz[String, ?]]`. The problem is that
   * `Forall.of` will not prevent you from taking a value of this type and
   * coercing it incorrectly. So as a general rule, just don't use `of` with
   * non-simple type constructors. The instantiation of τ within the type constructor
   * passed to `of` should always be outer-most. You're responsible for getting your
   * ranks right, and if you don't, you'll see `ClassCastException`s.
   *
   * In let-bound terms, this case can arise from something like the following:
   *
   * def foo[A, B](f: F[A]): G[B] = ???
   * def bar[A](f: F[A]): G[A] = ???
   *
   * If you represent these functions explicitly using `Forall`, you'll get
   * the following:
   *
   * val foo: ∀[λ[α => ∀[λ[β => F[α] => G[β]]]]]
   * val bar: ∀[λ[α => F[α] => G[α]]]
   *
   * However, if you represent them naively using τ, you'll see something more
   * like this:
   *
   * val foo: F[τ] => G[τ]
   * val bar: F[τ] => G[τ]
   *
   * And, unfortunately, the compiler will not prevent you from doing this! Neither
   * interpretation is particularly *wrong* (τ as an always-fresh type variable, vs
   * τ as being the same type in each case), but the interpretation can be very tricky.
   * The flaws in this interpretation appear when you try to use `of`, since if you
   * aren't careful, you can coerce things into the wrong shape.
   *
   * You can also get into trouble with rank. For example:
   *
   * def foo[A](f: F[A]): B = ???
   * def bar(f: ∀[F]): B = ???
   *
   * These are two different types! This is made clear if we use `Forall` to represent
   * them as vals:
   *
   * val foo: ∀[λ[α => F[α] => B]]
   * val bar: ∀[F] => B
   *
   * However, if represented with τ, the ambiguities once again emerge:
   *
   * val foo: F[τ] => B
   * val bar: F[τ] => B
   *
   * Just keep track of your actual quantifiers and remember that τ is not a mechanical
   * representational transformation in all cases.
   */
  implicit def of[F[_]](implicit F: F[τ]): Forall[F] =
    Forall[F](_(F.asInstanceOf))    // this is sillyness, but it works (albeit at the cost of a confused compiler)

  // non-implicit parameter version designed to provide nicer syntax
  implicit def ofDirect[F[_]](F: F[τ]): Forall[F] = of(F)

  /**
   * Utilities to implicitly materialize let-bound polymorphic contexts.
   */
  object Implicits {
    implicit def materialize[F[_], A](implicit F: Forall[F]): F[A] = F[A]
  }
}
