# Skolems [![Build Status](https://travis-ci.com/djspiewak/skolems.svg?branch=master)](https://travis-ci.com/djspiewak/skolems)

This library simply contains a few different encodings of universal and existential quantification of types. Whereas in Haskell one might write something like this (with appropriate extensions):

```haskell
type Uni f g = forall a . f a -> g a
type Exi b = exists a . (b, b -> a)
```

Scala isn't quite so simple. Unfortunately, Scala has *several* encodings of universal and existential types, all of which represent different tradeoffs, and using any of them effectively usually necessitates some understanding of how the compiler works. There's... not a lot that can be done to rectify that situation, but at least we can throw all the encoding machinery into one place so we don't have to keep copy/pasting it into every project ever.

You'll note that `Forall` is basically the same encoding as `scalaz.Forall`, and this is not a coincidence. It's a nice encoding. As I recall, the double-negation encoding (used in the `Forall` convenience constructor) was devised by [Miles Sabin](https://github.com/milessabin). The `Exists` encoding is very similar to the *old* `scalaz.Forsome` (I think?), which was removed quite a while ago. The `τ` trick was (to my knowledge) originally devised by [Ed Kmett](https://github.com/kmett). None of this is my work, originally, I'm just aggregating it because I'm sick of copy/paste.

## Usage

```sbt
libraryDependencies += "com.codecommit" %% "skolems" % "<version>"
```

Published for Scala 2.13, 2.12.

```scala
import skolems._
```
