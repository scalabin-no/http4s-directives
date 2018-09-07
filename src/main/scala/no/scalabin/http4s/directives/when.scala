package no.scalabin.http4s.directives

import cats.Monad
import org.http4s.{Request, Response}

import scala.language.higherKinds

case class when[F[_]: Monad, A](f: PartialFunction[Request[F], A]) {
  def orElse[L](fail: => F[Response[F]]): Directive[F, A] =
    Directive.request.flatMap(req =>
      f.lift(req) match {
        case Some(r) => Directive.success(r)
        case None    => Directive.failureF(fail)
    })
}
