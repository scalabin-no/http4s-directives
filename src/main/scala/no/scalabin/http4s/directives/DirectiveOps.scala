package no.scalabin.http4s.directives

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s.{HttpRoutes, Request, Response}

import scala.language.higherKinds

trait DirectiveOps[F[_]] {
  implicit class DirectiveKleisli[A](dir: Directive[F, A])(implicit F: Sync[F]) {
    def kleisli: Kleisli[F, Request[F], Result[F, A]] = Kleisli(dir.run)

    def toHttpRoutes(implicit ev: A =:= Response[F]): HttpRoutes[F] =
      HttpRoutes(req => OptionT.liftF(dir.run(req).map(_.response)))
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
