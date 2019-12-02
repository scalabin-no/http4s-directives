package no.scalabin.http4s.directives

import cats.Monad
import org.http4s.{Request, Response}

import scala.language.higherKinds

case class when[F[_]: Monad, A](pf: PartialFunction[Request[F], A]) {
  def orElse(fail: Directive[F, Response[F]]): Directive[F, A] =
    Directive.request.flatMap(
      req =>
        pf.lift(req) match {
          case Some(r) => Directive.pure(r)
          case None    => fail.flatMap(r => Directive.failure(r))
        }
    )

  def orElseRes(fail: => Response[F]): Directive[F, A] =
    orElseF(Monad[F].pure(fail))

  def orElseF(fail: F[Response[F]]): Directive[F, A] =
    orElse(Directive.successF(fail))
}

trait WhenOps[F[_]] {
  type When[A] = when[F, A]
  def when[A](pf: PartialFunction[Request[F], A])(implicit F: Monad[F]): When[A] =
    no.scalabin.http4s.directives.when[F, A](pf)
}
