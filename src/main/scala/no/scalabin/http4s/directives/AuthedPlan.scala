package no.scalabin.http4s.directives

import cats.MonadError
import cats.syntax.applicativeError._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.{AuthedService, _}

import scala.language.higherKinds

object AuthedPlan {
  def apply[F[_], A]()(implicit M: MonadError[F, Throwable]): AuthedPlan[F, A] = AuthedPlan(PartialFunction.empty)
}

case class AuthedPlan[F[_], A](onFail: PartialFunction[Throwable, F[Response[F]]])(implicit M: MonadError[F, Throwable]) {

  def task(pf: PartialFunction[AuthedRequest[F, A], Directive[F, Response[F]]]): AuthedService[A, F] = AuthedService[A, F] {
    case req if pf.isDefinedAt(req) =>
      pf(req)
        .run(req.req)
        .map(_.response)
        .attempt
        .flatMap(
          _.fold({
            case t if onFail.isDefinedAt(t) => onFail(t)
            case t                          => M.pure(Response(Status.InternalServerError))
          }, M.pure)
        )
  }

  case class Mapping[X](from: AuthedRequest[F, A] => (A, X)) {
    def apply(intent: PartialFunction[(A, X), Directive[F, Response[F]]]): AuthedService[A, F] = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  lazy val PathMapping: Mapping[Uri.Path] = Mapping[Uri.Path](r => r.authInfo -> r.req.uri.path)
}
