package no.scalabin.http4s.directives

import cats.Monad

import scala.language.higherKinds

trait DirectiveOps[F[+_]] {
  implicit class FilterSyntax(b:Boolean) {
    def | [L](failure: => L) = Directive.Filter(b, () => failure)
  }

  implicit class MonadDecorator[+X](f: F[X])(implicit sync: Monad[F]) {
    def successValue: Directive[F, Nothing, X] = Directive.successF(f)
    def failureValue: Directive[F, X, Nothing] = Directive.failureF(f)
    def errorValue: Directive[F, X, Nothing] = Directive.errorF(f)
  }
}
