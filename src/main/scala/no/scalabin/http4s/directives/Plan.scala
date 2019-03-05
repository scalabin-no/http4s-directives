package no.scalabin.http4s.directives

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

import scala.language.higherKinds

object Plan {
  def routes[F[_]: Sync](pf: PartialFunction[Request[F], Directive[F, Response[F]]]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
    }
  }

  def default[F[_]: Sync] = apply(PartialFunction.empty)
}

case class Plan[F[_]](onFail: PartialFunction[Throwable, F[Response[F]]])(implicit M: Sync[F]) {
  def toRoutes(pf: PartialFunction[Request[F], Directive[F, Response[F]]]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req if pf.isDefinedAt(req) =>
      pf(req)
        .run(req)
        .map(_.response)
        .attempt
        .flatMap(
          _.fold({
            case t if onFail.isDefinedAt(t) => onFail(t)
            case t                          => M.pure(Response(Status.InternalServerError))
          }, M.pure)
        )
  }

  case class Mapping[X](from: Request[F] => X) {
    def apply(intent: PartialFunction[X, Directive[F, Response[F]]]): HttpRoutes[F] = toRoutes {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  lazy val PathMapping: Mapping[Uri.Path] = Mapping[Uri.Path](r => r.uri.path)
}
