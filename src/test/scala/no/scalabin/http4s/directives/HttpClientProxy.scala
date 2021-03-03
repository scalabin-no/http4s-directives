package no.scalabin.http4s
package directives

import cats.Monad
import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.blaze._
import org.http4s.implicits._
import org.http4s.server.blaze._

import scala.concurrent.ExecutionContext
import scala.language.higherKinds

object HttpClientProxy extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val dsl: DirectivesDsl[IO] = new DirectivesDsl[IO] with DirectiveDslOps[IO] {}

    def service(client: Client[IO]) =
      new Routes(client).httpRoutes.orNotFound

    val resources = for {
      client <- BlazeClientBuilder[IO](executionContext = ExecutionContext.global).resource
      _      <-
        BlazeServerBuilder[IO](executionContext = ExecutionContext.global).bindLocal(8080).withHttpApp(service(client)).resource
    } yield ()

    resources
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  class Routes[F[_]: Sync](httpClient: Client[F]) extends DirectivesDsl[F] with DirectiveDslOps[F] {

    def getExample: F[Response[F]] =
      httpClient.get("https://example.org/")(r => Sync[F].delay(r))

    def directiveFor(fRes: F[Response[F]]): ResponseDirective = {
      for {
        _   <- Method.GET
        res <- fRes.toDirective
      } yield { res }
    }

    def directiveflatMap(res: F[Response[F]]): ResponseDirective = {
      Method.GET.flatMap(_ => res.toDirective)
    }

    def httpRoutes =
      DirectiveRoutes[F] {
        case _ -> Root / "flatMap" => directiveflatMap(getExample)
        case _ -> Root             => directiveFor(getExample)
      }
  }
}
