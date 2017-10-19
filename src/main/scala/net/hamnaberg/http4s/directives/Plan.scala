package net.hamnaberg.http4s.directives

import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import org.http4s.dsl.impl.Path
import org.http4s._
import cats.effect.Sync

import scala.language.higherKinds


object Plan {
  def apply[F[+_]: Sync](): Plan[F] = Plan(PartialFunction.empty)
}

case class Plan[F[+_]: Sync](onFail: PartialFunction[Throwable, F[Response[F]]]) {
  type Intent = PartialFunction[Request[F], F[Response[F]]]

  def task(pf: PartialFunction[Request[F], Directive[F, Response[F], Response[F]]]): Intent = {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(Result.merge).attempt.flatMap(
      _.fold({
        case t if onFail.isDefinedAt(t) => onFail(t)
        case t => Sync[F].pure(Response(Status.InternalServerError))
      }, Sync[F].pure)
    )
  }

  case class Mapping[X](from: Request[F] => X) {
    def apply(intent: PartialFunction[X, Directive[F, Response[F], Response[F]]]): Intent = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  val PathMapping : Mapping[Path] = Mapping[Path](r => Path(r.pathInfo))
}

