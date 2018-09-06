package no.scalabin.http4s
package directives

import cats.Monad
import cats.effect.IO
import fs2.StreamApp
import org.http4s._
import org.http4s.server.blaze._
import org.http4s.client.blaze._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.higherKinds

object HttpClientProxy extends StreamApp[IO] {
  val httpClient = Http1Client[IO]().unsafeRunSync()

  override def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    implicit val directives: Directives[IO] = Directives[IO]

    val pathMapping = Plan[IO].PathMapping

    val service = HttpService[IO] {
      pathMapping {
        case "/flatMap" => directiveflatMap(getExample())
        case "/"        => directiveFor(getExample())
      }
    }

    BlazeBuilder[IO].bindLocal(8080).mountService(service).serve
  }

  def getExample(): IO[Response[IO]] = {
    httpClient.get("https://example.org/")(r => IO(r))
  }

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
