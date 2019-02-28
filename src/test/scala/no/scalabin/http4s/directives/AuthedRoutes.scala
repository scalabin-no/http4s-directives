package no.scalabin.http4s.directives

import cats.data.{Kleisli, OptionT}
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.http4s.server.blaze.BlazeServerBuilder

import cats.syntax.functor._

object AuthedRoutes extends IOApp with RequestDirectives[IO] {
  case class User(username: String)

  private val pathMapping = AuthedPlan[IO, User]().PathMapping
  private val service = pathMapping{
    case (user, "/hello") => Directive.successF(Ok("Hello"))
  }

  def getUser = Kleisli[({type X[A] = OptionT[IO, A]})#X, Request[IO], User] { req =>
    OptionT.fromOption(req.headers.get(Authorization).collect{
      case Authorization(BasicCredentials(u, p)) if u == "username" && p == "password" => User(u)
    })
  }


  override def run(args: List[String]): IO[ExitCode] = {
    val middleware = AuthMiddleware[IO, User](getUser)

    val resource = BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpApp(middleware(service).orNotFound)
      .resource

    resource.use(_ => IO.never).as(ExitCode.Success)
  }
}
