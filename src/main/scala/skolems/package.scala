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

package object skolems {
  type ∀[+F[_]] = Forall[F]
  val ∀ = Forall

  type ∃[+F[_]] = Exists[F]
  val ∃ = Exists

  type Tau
  type τ = Tau

  type Not[A] = A => Nothing
  type ¬[A] = Not[A]

  /**
   * Note that this is part of the implicit scope when the compiler attempts to
   * resolve something like `implicitly[Foo[τ]]`, because the fully-expanded version
   * of that expression is `implicitly[Foo[skolems.Tau]]`, and thus `skolems` is part
   * of the type, and its companion is itself.
   *
   * This trick will break in Dotty.
   */
  implicit def forallToTau[F[_]](implicit F: Forall[F]): F[τ] = F[τ]
}
