package no.scalabin.http4s.directives


import cats.effect.IO
import fs2._
import org.http4s._
import org.http4s.server.blaze._
import org.http4s.dsl.io._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object SSEApp extends StreamApp[IO] {
  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    val directives = Directives[IO]
    import directives._
    import ops._

    val pathMapping = Plan[IO].PathMapping

    val service = HttpService[IO] {
      pathMapping {
        case _ => for {
          _ <- Method.GET
          res <- Ok(events.map(e => ServerSentEvent(e.toString))).successF
        } yield {
          res
        }
      }
    }

    BlazeBuilder[IO].bindLocal(8080).mountService(service).serve
  }

  def events: Stream[IO, Event] = {
    val seconds = Scheduler[IO](2).flatMap(_.awakeEvery[IO](1.second))
    seconds.take(10).map(_ => Event("event"))
  }


  case class Event(name: String)
}
