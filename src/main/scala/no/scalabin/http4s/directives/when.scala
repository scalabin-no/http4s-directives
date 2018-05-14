package no.scalabin.http4s.directives

import cats.effect.Sync
import org.http4s.Request

import scala.language.higherKinds

case class when[F[+_]: Sync, R](f:PartialFunction[Request[F], R]) extends RequestDirectives[F] {
  def orElse[L](fail: => L): Directive[F, L, R] =
    request.apply.flatMap(req => f.lift(req) match {
      case Some(r) => Directive.success(r)
      case None => Directive.failure(fail)
    })

  def orElseF[L](fail: => F[L]): Directive[F, L, R] =
    request.apply.flatMap(req => f.lift(req) match {
      case Some(r) => Directive.success(r)
      case None => Directive.failureF(fail)
    })
}
