package no.scalabin.http4s.directives

import cats.effect.IO
import fs2.StreamApp
import org.http4s._
import org.http4s.server.blaze._
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.Implicits.global

object HttpClientProxy extends StreamApp[IO] {
  val httpClient = Http1Client[IO]().unsafeRunSync()

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    val directives = Directives[IO]
    import directives._
    import ops._

    val pathMapping = Plan[IO].PathMapping

    val service = HttpService[IO] {
      pathMapping {
        case _ => for {
          _ <- Method.GET
          res <- getExample().successValue
        } yield {
          res
        }
      }
    }

    BlazeBuilder[IO].bindLocal(8080).mountService(service).serve
  }

  def getExample(): IO[Response[IO]] = {
    httpClient.get("https://example.org/")(r => IO(r))
  }
}
