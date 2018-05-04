package no.scalabin.http4s.directives

import cats.effect.Sync
import cats.syntax.functor._

import scala.language.higherKinds

trait DirectiveOps[F[+_]] {
  implicit class FilterSyntax(b:Boolean) {
    def | [L](failure: => L) = Directive.Filter(b, () => failure)
  }

  implicit class MonadDecorator[+X](f: F[X])(implicit sync: Sync[F]) {
    def successValue: Directive[F, Nothing, X] = Directive[F, Nothing, X](_ => f.map(Result.Success(_)))
    def failureValue: Directive[F, X, Nothing] = Directive[F, X, Nothing](_ => f.map(Result.Failure(_)))
    def errorValue: Directive[F, X, Nothing] = Directive[F, X, Nothing](_ => f.map(Result.Error(_)))
  }
}
