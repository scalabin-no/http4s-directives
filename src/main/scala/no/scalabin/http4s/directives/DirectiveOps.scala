package no.scalabin.http4s.directives

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.flatMap._
import org.http4s.Response

trait DirectiveOps[F[_]] {

  implicit class DirectiveResponseOps(dir: Directive[F, Response[F]])(implicit F: Monad[F]) {
    def failure[A]: Directive[F, A] = dir.flatMap(res => Directive.failure(res))
    def error[A]: Directive[F, A]   = dir.flatMap(res => Directive.error(res))
  }

  implicit class FilterSyntax(b: Boolean) {
    def orF(failureF: F[Response[F]])(implicit M: Monad[F]): Directive.Filter[F]  = or(Directive.successF(failureF))
    def orRes(failure: => Response[F])(implicit M: Monad[F]): Directive.Filter[F] = or(Directive.pure(failure))
    def or(failure: Directive[F, Response[F]]): Directive.Filter[F]               = Directive.Filter(b, failure)
  }

  implicit class MonadDecorator[X](f: F[X])(implicit sync: Monad[F]) {

    def successF: Directive[F, X]                                          = Directive.successF(f)
    def failureF[C](implicit ev: F[X] =:= F[Response[F]]): Directive[F, C] = Directive.failureF(ev(f))
    def errorF[C](implicit ev: F[X] =:= F[Response[F]]): Directive[F, C]   = Directive.errorF(ev(f))
    def liftF: Directive[F, X]                                             = Directive.liftF(f)
  }

  //TODO: Consider having these as documentation and not as actual code in the library
  implicit class OptionDirectives[A](opt: Option[A])(implicit S: Monad[F]) {
    def toSuccess(failure: Directive[F, A]): Directive[F, A] = {
      opt match {
        case Some(a) => Directive.success(a)
        case None    => failure
      }
    }
  }

  implicit class EitherDirectives[E, A](either: Either[E, A])(implicit S: Monad[F]) {
    def toSuccess(failure: E => Directive[F, A]): Directive[F, A] = {
      either match {
        case Right(a)   => Directive.success(a)
        case Left(left) => failure(left)
      }
    }
  }

  implicit class EitherTDirectives[E, A](monad: EitherT[F, E, A])(implicit S: Monad[F]) {
    def toSuccess(failure: E => Directive[F, A]): Directive[F, A] =
      Directive(req => monad.fold(failure, a => Directive.success[F, A](a)).flatMap(d => d.run(req)))
  }

  implicit class OptionTDirectives[A](monad: OptionT[F, A])(implicit S: Monad[F]) {
    def toSuccess(failure: Directive[F, A]): Directive[F, A] =
      Directive(req => monad.fold(failure)(a => Directive.success[F, A](a)).flatMap(d => d.run(req)))
  }
}
