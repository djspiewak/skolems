# Skolems [![Build Status](https://travis-ci.com/djspiewak/skolems.svg?branch=master)](https://travis-ci.com/djspiewak/skolems)

This library simply contains a few different encodings of universal and existential quantification of types. Whereas in Haskell one might write something like this (with appropriate extensions):

```haskell
type Uni f g = forall a . f a -> g a
type Exi a = exists b . (b, b -> a)
```

Scala isn't quite so simple. Unfortunately, Scala has *several* encodings of universal and existential types, all of which represent different tradeoffs, and using any of them effectively usually necessitates some understanding of how the compiler works. There's... not a lot that can be done to rectify that situation, but at least we can throw all the encoding machinery into one place so we don't have to keep copy/pasting it into every project ever.

## Usage

```sbt
libraryDependencies += "com.codecommit" %% "skolems" % "<version>"
```

Published for Scala 2.13, 2.12.

```scala
import skolems._
```
