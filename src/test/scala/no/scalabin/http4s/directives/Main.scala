package no.scalabin.http4s.directives

import java.time.LocalDateTime

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.impl.Root
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeBuilder

object Main extends IOApp {

  override def run(args: List[String]) = {
    implicit val Direct: Directives[IO] = Directives[IO]

    import Direct._
    import ops._

    val Mapping = Plan[IO]().Mapping(req => Path(req.uri.path))

    val lm = LocalDateTime.now()

    val service = HttpRoutes.of {
      Mapping {
        case Root / "hello" => {
          for {
            _   <- Method.GET
            res <- Conditional.ifModifiedSince(lm, Ok("Hello World"))
            foo <- request.queryParam[IO]("foo")
            if foo.isDefined orF BadRequest("You didn't provide a foo, you fool!")
            //res <- Ok("Hello world")
          } yield res
        }
      }
    }

    BlazeBuilder[IO].bindHttp(8080, "localhost").mountService(service, "/").serve.compile.drain.as(ExitCode.Success)
  }
}
