package no.scalabin.http4s.directives

import java.time.LocalDateTime

import org.http4s._
import org.http4s.dsl.io._
import cats.effect.IO
import fs2.StreamApp
import org.http4s.dsl.impl.Root
import org.http4s.server.blaze.BlazeBuilder

object Main extends StreamApp[IO] {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    implicit val Direct: Directives[IO] = Directives[IO]

    import Direct._
    import ops._

    val Mapping = Plan[IO]().Mapping(req => Path(req.uri.path))

    val lm = LocalDateTime.now()

    val service = HttpService[IO] {
      Mapping {
        case Root / "hello" => {
          for {
            _   <- Method.GET
            res <- Conditional.ifModifiedSince(lm, Ok("Hello World"))
            //res <- Ok("Hello world")
          } yield res
        }
      }
    }

    BlazeBuilder[IO].bindHttp(8080, "localhost").mountService(service, "/").serve
  }
}
