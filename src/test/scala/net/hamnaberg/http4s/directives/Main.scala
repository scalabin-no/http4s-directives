package net.hamnaberg.http4s.directives


import java.time.LocalDateTime

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze.BlazeBuilder

import scalaz.concurrent.Task

object Main extends ServerApp {
  override def server(args: List[String]): Task[Server] = {
    import Directives._
    import ops._
    import conditional.get._

    val Mapping = Plan().PathMapping

    val lm = LocalDateTime.now()

    val service = HttpService(
      Mapping{
        case Root / "hello" => {
          for {
            _ <- Method.GET | Method.HEAD
            res <- ifUnmodifiedSince(lm, Ok("Hello World"))
          } yield {
            res
          }
        }
      }
    )

    BlazeBuilder.bindHttp(8080, "localhost").mountService(service, "/").start
  }
}
