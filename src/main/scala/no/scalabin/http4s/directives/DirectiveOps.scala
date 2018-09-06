package no.scalabin.http4s.directives

import cats.Monad
import org.http4s.Response

import scala.language.higherKinds

trait DirectiveOps[F[_]] {
  implicit class FilterSyntax(b: Boolean) {
    def orF(failureF: F[Response[F]]) = Directive.Filter(b, failureF)
  }

  implicit class MonadDecorator[X](f: F[X])(implicit sync: Monad[F]) {

    def successF: Directive[F, X] = Directive.successF(f)
    def failureF[C]: Directive[F, C] = Directive.failureF(f.asInstanceOf[F[Response[F]]])
    def errorF[C]: Directive[F, C]   = Directive.errorF(f.asInstanceOf[F[Response[F]]])
  }
}
