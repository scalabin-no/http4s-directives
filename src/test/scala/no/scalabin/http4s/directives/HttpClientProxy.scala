package no.scalabin.http4s
package directives

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
    val directives = Directives[IO]
    import directives._
    import ops._

    val pathMapping = Plan[IO].PathMapping

    val service = HttpService[IO] {
      pathMapping {
        case "/flatMap" => directiveflatMap(getExample())(directives)
        case "/" => directiveFor(getExample())(directives)
      }
    }

    BlazeBuilder[IO].bindLocal(8080).mountService(service).serve
  }

  def getExample(): IO[Response[IO]] = {
    httpClient.get("https://example.org/")(r => IO(r))
  }

  type ValueDirective[F[+_], A] = Directive[F, Response[F], A]
  type ResponseDirective[F[+_]] = ValueDirective[F, Response[F]]

  def directiveFor[F[+_]](fRes: F[Response[F]])(implicit d: Directives[F]): ResponseDirective[F] = {
    import d._
    import ops._
    for {
      _ <- Method.GET
      res <- fRes.successValue
    } yield { res }
  }

  def directiveflatMap[F[+_]](res: F[Response[F]])(implicit d: Directives[F]): ResponseDirective[F] = {
    import d._
    import ops._

    Method.GET.flatMap(_ => successF(res))

  }
}
