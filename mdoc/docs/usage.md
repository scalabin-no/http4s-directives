---
layout: docs
title: Usage
position: 2
---

# Usage

## Imports

```scala mdoc:silent
import no.scalabin.http4s.directives._
import cats.effect.IO
import org.http4s._
```

## Creating your first directive

```scala mdoc
implicit val dirDsl = new DirectivesDsl[IO] with DirectiveDslOps[IO]

import dirDsl._

val Mapping = Plan.default[IO].Mapping(req => Path(req.uri.path))

val service = Mapping {
  case Root / "hello" => 
    for {
      _ <- Method.GET
      r <- Ok("Hello World")
    } yield r
}
```

### Parsing body
```scala mdoc
val bodyService = Mapping {
  case Root / "hello" => 
    for {
      _    <- Method.POST
      body <- request.bodyAs[String]
      r    <- Ok(s"echo $body")
    } yield r
}
```

### Query parameters
```scala mdoc
val queryParamService = Mapping {
  case Root / "hello" => 
    for {
      _         <- Method.POST
      name      <- request.queryParam("name")
      items     <- request.queryParams("items")
      nickname  <- request.queryParamOrElse("nickname", BadRequest("missing nickname"))
      r         <- Ok(s"Hello $name($nickname): ${items.mkString(",")}")
    } yield r
}
```
