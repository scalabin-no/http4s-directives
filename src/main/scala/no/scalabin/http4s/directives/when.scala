package no.scalabin.http4s.directives

import cats.effect.Sync
import org.http4s.Request

import scala.language.higherKinds

case class when[F[+_]: Sync, R](f:PartialFunction[Request[F], R]) extends RequestDirectives[F] {
  def orElse[L](fail: => L): Directive[F, L, R] =
    request.flatMap(req => f.lift(req) match {
      case Some(r) => Directive.success(r)
      case None => Directive.failure(fail)
    })
}
