---
layout: docs
title: Usage
position: 2
---

# Usage

## Imports

```tut:silent
import no.scalabin.http4s.directives._
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.dsl.impl.Root
```

## Creating your first directive

```tut:silent
implicit val Direct: Directives[IO] = Directives[IO]

import Direct._
import ops._

val Mapping = Plan[IO]().Mapping(req => Path(req.uri.path))

val service = HttpService[IO] {
  Mapping {
    case Root / "hello" => 
      for {
        _ <- Method.GET
        r <- Ok("Hello World").successF
      } yield r
  }
}
```