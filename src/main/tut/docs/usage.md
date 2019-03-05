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

```tut
implicit val Direct: Directives[IO] = Directives[IO]

import Direct._
import ops._

val Mapping = Plan.default[IO].Mapping(req => Path(req.uri.path))

val service = Mapping {
  case Root / "hello" => 
    for {
      _ <- Method.GET
      r <- Ok("Hello World").successF
    } yield r
}
```

### Parsing body
```tut
val bodyService = Mapping {
  case Root / "hello" => 
    for {
      _    <- Method.POST
      body <- request.bodyAs[String]
      r    <- Ok(s"echo $body").successF
    } yield r
}
```

### Query parameters
```tut
val queryParamService = Mapping {
  case Root / "hello" => 
    for {
      _         <- Method.POST
      name      <- request.queryParam("name")
      items     <- request.queryParams("items")
      nickname  <- request.queryParamOrElseF("nickname", BadRequest("missing nickname"))
      r         <- Ok(s"Hello $name($nickname): ${items.mkString(",")}").successF
    } yield r
}
```
