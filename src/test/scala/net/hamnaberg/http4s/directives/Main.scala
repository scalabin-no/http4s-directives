package net.hamnaberg.http4s.directives


import java.time.Instant

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze.BlazeBuilder


import scalaz.concurrent.Task

object Main extends ServerApp {
  override def server(args: List[String]): Task[Server] = {
    import Directives._
    import ops._
    import conditionalGET._

    val Mapping = AsyncTask().PathMapping

    val lm = Instant.now()

    val service = HttpService(
      Mapping{
        case Root / "hello" => {
          for {
            _ <- Method.GET | Method.HEAD
            res <- ifModifiedSince(lm, Ok("Hello World"))
          } yield {
            res
          }
        }
      }
    )

    BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").start
  }
}
