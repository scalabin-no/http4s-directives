package no.scalabin.http4s.directives

import java.util.concurrent.Executors

import cats.effect.IO
import fs2.StreamApp.ExitCode
import cats.implicits._
import fs2._
import fs2.text._
import org.http4s._
import org.http4s.client.blaze.Http1Client

import scala.concurrent.ExecutionContext

object StreamingClient extends StreamApp[IO] {
  implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    def apiRequest(): Request[IO] = {
      Request[IO](
        Method.GET,
        Uri.unsafeFromString("http://localhost:8080/event")
      )
    }

    val clientStream = for {
      client <- Http1Client.stream[IO]()
      s <- client.streaming(apiRequest()) { resp =>
        resp.body.through(utf8Decode)
      }
    } yield s
    clientStream.to(Sink.showLinesStdOut).drain ++ Stream.emit(ExitCode.Success)
  }
}