package no.scalabin.http4s.directives

import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

import scala.language.higherKinds

object Plan {
  type Intent[F[_]] = PartialFunction[Request[F], Directive[F, Response[F]]]

  def routes[F[_]: Sync](pf: Intent[F]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
  }

  def default[F[_]: Sync] = apply(PartialFunction.empty)

  case class Mapping[F[_]: Sync, X](from: Request[F] => X)(toRoutes: Intent[F] => HttpRoutes[F]) {
    def apply(intent: PartialFunction[X, Directive[F, Response[F]]]): HttpRoutes[F] = toRoutes {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  def PathMapping[F[_]: Sync]: Mapping[F, Uri.Path] = Mapping[F, Uri.Path](r => r.uri.path)(routes)
}

case class Plan[F[_]](onFail: PartialFunction[Throwable, F[Response[F]]])(implicit M: Sync[F]) {
  def toRoutes(pf: Plan.Intent[F]): HttpRoutes[F] = HttpRoutes.of[F] {
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

  def Mapping[X](from: Request[F] => X): Plan.Mapping[F, X] = Plan.Mapping[F, X](from)(toRoutes)

  lazy val PathMapping: Plan.Mapping[F, Uri.Path] = Mapping(_.uri.path)
}
