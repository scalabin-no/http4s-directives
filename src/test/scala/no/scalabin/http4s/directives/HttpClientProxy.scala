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
    implicit val directives: Directives[IO] = Directives[IO]

    val pathMapping = Plan[IO].PathMapping

    def service(client: Client[IO]) =
      HttpRoutes.of {
        pathMapping {
          case "/flatMap" => directiveflatMap(getExample(client))
          case "/"        => directiveFor(getExample(client))
        }
      }.orNotFound

    val resources = for {
      client <- BlazeClientBuilder[IO](executionContext = ExecutionContext.global).resource
      _      <- BlazeServerBuilder[IO].bindLocal(8080).withHttpApp(service(client)).resource
    } yield ()

    resources
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }

  def getExample(httpClient: Client[IO]): IO[Response[IO]] =
    httpClient.get("https://example.org/")(r => IO(r))

  type ValueDirective[F[_], A] = Directive[F, A]
  type ResponseDirective[F[_]] = ValueDirective[F, Response[F]]

  def directiveFor[F[_]: Monad](fRes: F[Response[F]])(implicit d: Directives[F]): ResponseDirective[F] = {
    import d.ops._
    for {
      _   <- Method.GET
      res <- fRes.successF
    } yield { res }
  }

  def directiveflatMap[F[_]: Monad](res: F[Response[F]])(implicit d: Directives[F]): ResponseDirective[F] = {
    import d.ops._

    Method.GET.flatMap(_ => res.successF)
  }
}
