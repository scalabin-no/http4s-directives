package no.scalabin.http4s.directives

import java.time.LocalDateTime

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.blaze._
import org.http4s.util.StreamApp


object Main extends StreamApp[IO] {

  override def stream(args: List[String], requestShutdown: IO[Unit]) = {
    implicit val Direct: Directives[IO] = Directives[IO]
    val conditional = Conditional[IO]

    import Direct._
    import ops._
    import implicits._

    val Mapping = Plan[IO]().PathMapping

    val lm = LocalDateTime.now()

    val service = HttpService[IO]{
      Mapping{
        case Root / "hello" => {
          for {
            _ <- Method.GET
            res <- conditional.ifModifiedSince(lm, Ok("Hello World"))
            //res <- Ok("Hello world")
          } yield res
        }
      }
    }

    BlazeBuilder[IO].bindHttp(8080, "localhost").mountService(service, "/").serve
  }
}

