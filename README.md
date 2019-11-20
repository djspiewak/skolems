# Skolems [![Build Status](https://travis-ci.com/djspiewak/skolems.svg?branch=master)](https://travis-ci.com/djspiewak/skolems)

This library simply contains a few different encodings of universal and existential quantification of types. Whereas in Haskell one might write something like this (with appropriate extensions):

```haskell
type Uni f g = forall a . f a -> g a
type Exi a = exists b . (b, b -> a)
```

Unfortunately, Scala does not directly provide support for universally quantified higher-rank types, and its support for existential quantification of higher-rank types is spotty and extremely buggy. This project provides better, more uniform alternatives with sane type inference and sound behavior.

As a note, in most of the documentation I will be using the symbols ∀ ("for all") and ∃ ("exists"). Skolems allows you to use these symbols exactly as written, or if you prefer Latin characters, you can use `Forall` and `Exists`. Similarly, kind-projector allows you to use `Lambda` rather than `λ` if you find that easier to read (or type). I also have a habit of using α (lowercase "alpha") as a variable in my type lambdas, but this is not in any way required.

## Usage

```sbt
libraryDependencies += "com.codecommit" %% "skolems" % "<version>"
```

Published for Scala 2.13, 2.12, and 2.11. This library does not have *any* upstream dependencies (though I would dearly love to add a Cats module at some point, since `Forall` and `Exists` both form useful classes).

## API

Just to conceptualize some of this, it's worth taking a moment to digress on the functionality available in this department with *vanilla* Scala 2.13 (note that Scala 3 improves on this in some important areas, and actually makes it worse in others). Also note that when I say "vanilla Scala", I actually mean "vanilla Scala with [kind-projector](https://github.com/typelevel/kind-projector)", since it's almost impossible to talk about any of this stuff *without* kind-projector.

In Scala *today*, what follows is the only way to express universal quantification:

```scala
def foo[A](a: A): List[A] = List(a)
```

This function is universally quantified over `A`. We often refer to this special case of quantification as *polymorphism*, or sometimes as "parametricity" or "generics". Whatever word you choose to use, it all comes down to the same thing. Unfortunately, this functionality in the language has a very important limitation: you cannot write it *as a value*. As an example:

```scala
def foo[A](a: A): List[A] = List(a)
val otherFoo = foo(_)    // ?????
```

That doesn't work. Or rather, it works in the sense that it compiles, but the resulting `otherFoo` will have the very unhelpful `Nothing => List[Nothing]` type signature, which is not at all the type that `foo` has. This is because the quantification of `foo` is lost when it is expressed as a free value, which is why this form of quantification (as present in Scala) is sometimes referred to as *let-bound polymorphism*, since the polymorphism is lost if you detach the value (which is to say, the function) from its declaration.

Now, let-bound polymorphism is quite useful, but it is simply insufficient for many cases. As a trivial example, imagine we wanted to write a function that takes two parameters, one `String` and one `Int`, and *also* takes a function with the same theoretical type signature as `foo` above, and then applies it to both. So in other words, callers of the function would pass their `String` and `Int` values, as well as `foo` (or something with the same type as `foo`), and this function would apply the `foo`-like thing to both types.

We can't write this today in Scala. Writing it in Haskell would look something like this:

```haskell
bippy :: String -> Int -> (forall a . a -> [a]) -> ([String], [Int])
bippy s i f = (f s, f i)
```

That's actually... really straightforward, but you notice the trick: Haskell is allowing us to write the type of a *value* which itself is universally quantified. In other words, in Haskell, `foo` isn't stuck being a `def`, it is actually a value just like anything else.

Skolems allows you to write this function in Scala. It looks like this:

```scala
import skolems.∀

def bippy(s: String, i: Int, f: ∀[λ[α => α => List[α]]]): (List[String], List[Int]) =
  (f[String](s), f[Int](i))
```

Or, if you prefer the Latin character version:

```scala
import skolems.Forall

def bippy(
    s: String,
    i: Int,
    f: Forall[Lambda[a => a => List[a]]])
    : (List[String], List[Int]) =
  (f[String](s), f[Int](i))
```

*Calling* this function is relatively straightforward as well:

```scala
def foo[A](a: A): List[A] = List(a)

bippy("hi", 42, ∀[λ[α => α => List[α]]](foo))   // => (List("hi"), List(42))
```

In some cases, it may be cleaner to use a named type alias rather than the type lambda:

```scala
def foo[A](a: A): List[A] = List(a)

type ListConstr[A] = A => List[A]
bippy("hi", 42, ∀[ListConstr](foo))   // => (List("hi"), List(42))
```

So far, we've only looked at universal quantification, but what about existential? Here, Scala has, on paper, slightly better support in the form of the `forSome` type operator:

```scala
def foo(unapplied: (A, A => String) forSome { type A }): String = {
  val (v, f) = unapplied
  f(v)
}

foo((42, (i: Int) => i.toString))
foo(("hi", (s: String) => s))
```

Or, more interestingly:

```scala
def foo(unapplied: List[(A, A => String) forSome { type A }]): List[String] =
  unapplied map {
    case (v, f) => f(v)
  }

foo(
  List(
    (42, (i: Int) => i.toString),
    ("hi", (s: String) => s)))
```

Unfortunately, `forSome` is extremely brittle. The compiler will often "lose" the quantifier as it passes through various transformations in more complex types. Additionally, there are certain cases where the compiler will do straight-up unsound things with the type. For example, I've seen scalac convert the following types into one another (or rather, more complex variants of these types):

```scala
type One = (F[A] forSome { type A }) => B
type Two = (F[A] => B) forSome { type A }
```

These are not the same type! Not even close. In fact, `One` and `Two` could *actually* be rewritten as the following:

```scala
type Neo = ∀[λ[α => F[α] => B]]
type Wot = ∀[F] => B
```

In other words, pretending that `One` and `Two` are the same type (which, as I said, I've seen the compiler do at times when using `forSome`) is not just limiting, it's actually wrong and can generate type unsoundness. tldr, `forSome` is buggy in Scala, and you should try not to use it.

But of course, if you *don't* use `forSome`, then your only option for encoding existential quantification is the ad-hoc approach using type members. For example:

```scala
trait Unapplied {
  type A
  val a: A
  def apply(v: A): String
}

def foo(unapplied: List[Unapplied]): List[String] =
  unapplied map { u =>
    u(u.v)
  }

foo(
  List(
    new Unapplied {
      type A = Int
      val a = 42
      def apply(i: Int): String = i.toString
    },
    new Unapplied {
      type A = String
      val a = "hi"
      def apply(s: String): String = s
    }))
```

This works a lot better than the `forSome` approach, and the Scala compiler is *much* better about not losing its mind when using this encoding, but it's a little awkward to create a new wrapper type every time you need an existential. Also it's unbelievably verbose.

For this reason, Skolems introduces the `Exists` (`∃`) type, which is analogous to `Forall` (`∀`) except for existential rather than universal types.

```scala
import skolems._

def foo(unapplied: List[∃[λ[α => (α, α => String)]]]): List[String] =
  unapplied map { u =>
    val (v, f) = u()
    f(v)
  }

foo(
  List(
    ∃[λ[α => (α, α => String)]]((42, (i: Int) => i.toString)),
    ∃[λ[α => (α, α => String)]](("hi", (s: String) => s))))
```

Basically, think of `Exists` as a "better `forSome`". It will type infer sanely and it's almost exactly as easy to use.

### Implicit Evidence

Another interesting use of higher-rank universal types is in defining polymorphic implicit values. As an example, [Cats](https://typelevel.org/cats) defines an instance of `Monad` for `Either[A, ?]`, for *any* type `A`. This is defined in the following way:

```scala
implicit def eitherMonad[A]: Monad[Either[A, ?]] = ???
```

Here again we see let-bound polymorphism rearing its ugly head. While it is possible to *define* such a value (a monad for `Either` for all type instantiations), it isn't possible to take it as a parameter. For example:

```scala
// we can write this function!
def foo[F[_], A](fa: F[A])(implicit F: Monad[F]) = ???

// but writing this function is awkward
def bar[F[_, _], A, B, C](
    one: F[A, C],
    two: F[B, C])(
    implicit F1: Monad[F[A, ?]],
    F2: Monad[F[B, ?]]) = ???
```

Notice how we had to take two `Monad` instances (`F1` and `F2`) *for the same type constructor*, which is more than a little weird when the instances are actually *the same* instance: `eitherMonad`. There are other cases where this problem can get even worse, or even intractable.

What we really want to write is this:

```scala
def bar[F[_, _], A, B, C](
    one: F[A, C],
    two: F[B, C])(
    implicit F: ∀[λ[α => Monad[F[α, ?]]]]) = ???
```

In other words, a *universally quantified* version of the `Monad` which has the same expressivity as the original definition, `eitherMonad`.

With Skolems, you can do exactly this. In fact, the above works exactly as written, and you can even call it in exactly the way you expect!

```scala
bar[Either, String, Boolean, Int](Right(42), Left(false))
```

And that's it. It just works. In fact, you can even omit the type parameters:

```scala
bar(Right(42), Left(false))
```

That works too.

The *reason* this works is Skolems is able to materialize an implicit `Forall` type *given* an implicit definition that uses let-bound polymorphism (such as `eitherMonad`). Unfortunately, it cannot *currently* go in the other direction. In other words:

```scala
def foo[F[_, _]](implicit F: ∀[λ[α => Monad[F[α, ?]]]]) = Monad[F[String, ?]]  // nope!
```

This doesn't work. **Yet.** The problem is that scalac lacks a particular form of symbolic unification in its type checker. This isn't really a *theoretical* problem, scalac's implementation just can't handle the kind of equation that is necessary to typecheck this. Skolems will add support for this using a macro in an upcoming release.

But until then... you can materialize the instance manually:

```scala
def foo[F[_, _]](implicit F: ∀[λ[α => Monad[F[α, ?]]]]) = {
  implicit val fs = F[String]
  implicit val fi = F[Int]

  Monad[F[String, ?]]  // no problem!
  Monad[F[Int, ?]]     // also cool
}
```

That works fine for a lot of cases, and we'll be making things even better soon!

As an aside, the same implicit materialization *should* work for existentially-quantified implicit declarations just the same as it works for universally-quantified ones, but I haven't been able to come up with a good motivating example for this.

### Identities

There are a number of identities which hold for rank-n quantification in first-order logic (which is to say, Scala's type system). These identities are very difficult to access and highly opaque when using let-bound polymorphism and `forSome`, but Skolems can make them very easy and direct. These identities are specifically as follows (with their corresponding implementation in the API):

- ![](https://i.imgur.com/MJvkK36.png)
  + Left-to-right: `Forall.raise`
  + Right-to-left: `Forall.lower`
    * Also `Forall.lowerA` if the let-bound polymorphic quantifier encoding is more useful than the `Forall`-based version
- ![](https://i.imgur.com/kvNiR39.png)
  + Left-to-right: `Exists.raise`
  + Right-to-left: `Exists.lower`
    * Also `Exists.lowerE` if the `forSome` quantifier encoding is more useful than the `Exists`-based version

As a note on the naming convention, you should think of `raise` as "raise the rank". In other words, you're going from a rank-1 type (with the quantifier on the outside) to a rank-2 type (with the quantifier on the inside). Obviously, `lower` is the inverse. These functions are relatively trivial to define (except for `Exists.lower`, which requires an `asInstanceOf` due to the fact that Scala only has local type inference), but it's nice to have them already available.

## Related Work

The encodings in this project occurred in their (to my knowledge) original forms in Scalaz. The use of the inexpressible sentinel type as a mechanism for inferring a rank-2 universal from a Scala expression has varied origins. Miles Sabin mentioned the trick to me a few years ago and had some (now discarded) prototype constructs in Shapeless which took advantage of it. Ed Kmett has also discussed it as an encoding which infers better, though I cannot currently find a link to his use of it, and based on my memory of what he was doing, it may have been unsound all along. Scalaz's `Forall` has a peculiar `apply` function which uses a doubly-negated existential type (expressed using `forSome` and `A => Nothing`) to infer a rank-2 universal. This trick was *also* devised originally by Miles Sabin (waaaaaay back in the mists of time), and if you look very very closely, it's really just yet another way of expressing the "inexpressible sentinel type" idea. As for the idea of using an abstract type member within an object to encode the sentinel, I first learned of this from Kai, though Alex Konovalov also uses this extensively in several of his projects and I honestly don't know where it originates.

Speaking of Alex, he has a really interesting library called [polymorphic](https://github.com/alexknvl/polymorphic) which may be a better use of your time than my failings here! Some differences between polymorphic and skolems:

- His `Forall` and `Exists` are entirely unboxed
- He provides a very clever `Instance` constructor for the common implicit case of existential pairs
- He also provides `Pi` and `Sigma` for dependent type quantification
- Polymorphic depends on cats-core, while Skolems has no dependencies (for better or worse)
- Skolems defines a mechanism for materializing rank-n implicit values, while this is not provided by polymorphic in any form except `Instance`
- The type inference seems to be nicer with polymorphic than with skolems
