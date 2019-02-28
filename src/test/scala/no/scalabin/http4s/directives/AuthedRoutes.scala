package no.scalabin.http4s.directives

import cats.effect._
import cats.syntax.functor._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.authentication.BasicAuth

object AuthedRoutes extends IOApp with RequestDirectives[IO] {
  case class User(username: String)

  private val pathMapping = AuthedPlan[IO, User]().PathMapping
  private val service = pathMapping {
    case (user, "/hello") => Directive.successF(Ok("Hello"))
  }

  private def getUser(up: BasicCredentials) =
    IO(up match {
      case BasicCredentials(u, p) if u == "username" && p == "password" => Some(User(u))
      case _                                                            => None
    })

  override def run(args: List[String]): IO[ExitCode] = {
    val middleware = BasicAuth[IO, User]("realm", getUser)

    val resource = BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(middleware(service).orNotFound)
      .resource

    resource.use(_ => IO.never).as(ExitCode.Success)
  }
}
