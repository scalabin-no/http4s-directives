package no.scalabin.http4s.directives

import cats.Monad

import scala.language.higherKinds

trait DirectiveOps[F[+_]] {
  implicit class FilterSyntax(b:Boolean) {
    def | [L](failure: => L) = Directive.Filter(b, () => failure)
  }

  implicit class MonadDecorator[+X](f: F[X])(implicit sync: Monad[F]) {

    def successF: Directive[F, Nothing, X] = Directive.successF(f)
    def failureF: Directive[F, X, Nothing] = Directive.failureF(f)
    def errorF: Directive[F, X, Nothing] = Directive.errorF(f)

    @deprecated("use successF instead", "0.3.0")
    def successValue: Directive[F, Nothing, X] = successF
    @deprecated("use failureF instead", "0.3.0")
    def failureValue: Directive[F, X, Nothing] = failureF
    @deprecated("use errorF instead", "0.3.0")
    def errorValue: Directive[F, X, Nothing] = errorF
  }
}
