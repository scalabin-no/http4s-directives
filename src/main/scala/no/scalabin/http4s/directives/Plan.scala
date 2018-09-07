package no.scalabin.http4s.directives

import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

import scala.language.higherKinds

object Plan {
  def apply[F[_]]()(implicit M: MonadError[F, Throwable]): Plan[F] = Plan(PartialFunction.empty)
}

case class Plan[F[_]](onFail: PartialFunction[Throwable, F[Response[F]]])(implicit M: MonadError[F, Throwable]) {
  type Intent = PartialFunction[Request[F], F[Response[F]]]

  def task(pf: PartialFunction[Request[F], Directive[F, Response[F]]]): Intent = {
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
    def apply(intent: PartialFunction[X, Directive[F, Response[F]]]): Intent = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  lazy val PathMapping: Mapping[Uri.Path] = Mapping[Uri.Path](r => r.uri.path)
}
