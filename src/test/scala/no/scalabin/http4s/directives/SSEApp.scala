package no.scalabin.http4s.directives

import cats.effect._
import cats.implicits._
import fs2._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.concurrent.duration._

object SSEApp extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val directives = Directives[IO]
    import directives._
    import ops._

    val pathMapping = Plan[IO].PathMapping

    val service = HttpRoutes
      .of[IO] {
        pathMapping {
          case _ =>
            for {
              _   <- Method.GET
              res <- Ok(events.map(e => ServerSentEvent(e.toString))).successF
            } yield {
              res
            }
        }
      }
      .orNotFound

    BlazeServerBuilder[IO].bindLocal(8080).withHttpApp(service).resource.use(_ => IO.never).as(ExitCode.Success)
  }

  def events: Stream[IO, Event] = {
    val seconds = Stream.awakeEvery[IO](1.second)
    seconds.take(10).map(_ => Event("event"))
  }

  case class Event(name: String)
}
