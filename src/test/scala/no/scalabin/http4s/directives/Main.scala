package no.scalabin.http4s.directives

import java.time.LocalDateTime

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.implicits._
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]) = {
    val dsl = new DirectivesDsl[IO] with DirectiveDslOps[IO]
    import dsl._

    val lm = LocalDateTime.now()

    val service =
      DirectiveRoutes[IO] {
        case _ -> Root / "hello" => {
          for {
            _   <- Method.GET
            res <- ifModifiedSinceDir(lm, Ok("Hello World"))
            foo <- request.queryParam("foo")
            if foo.isDefined or BadRequest("You didn't provide a foo, you fool!")
            //res <- Ok("Hello world")
          } yield res
        }
      }.orNotFound

    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(service)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}
