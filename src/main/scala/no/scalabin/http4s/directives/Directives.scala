package no.scalabin.http4s
package directives

import cats.Monad
import org.http4s._

import scala.language.{higherKinds, implicitConversions}

object Directives {
  def apply[F[_]](implicit M: Monad[F]): Directives[F] = new Directives[F]
}

class Directives[F[_]: Monad] {

  type Result[R] = directives.Result[F, R]
  val Result = directives.Result

  type Directive[R] = directives.Directive[F, R]

  object Directive {
    def apply[R](run: Request[F] => F[Result[R]]): Directive[R] = directives.Directive[F, R](run)
  }

  def result[R](result: Result[R]) = directives.Directive.result[F, R](result)

  def pure[R](success: R)    = directives.Directive.pure[F, R](success)
  def success[R](success: R) = pure(success)
  def failure(failure: Response[F]) = directives.Directive.failure[F](failure)
  def error(error: Response[F])     = directives.Directive.error[F](error)

  def liftF[R](success: F[R])    = directives.Directive.liftF[F, R](success)
  def successF[R](success: F[R]) = liftF(success)
  def failureF(failure: F[Response[F]]) = directives.Directive.failureF[F, Response[F]](failure)
  def errorF(error: F[Response[F]])     = directives.Directive.errorF[F, Response[F]](error)

  def getOrElseF[R](opt: F[Option[R]], orElse: => F[Response[F]]) = directives.Directive.getOrElseF[F, R](opt, orElse)

  def getOrElse[A](opt: Option[A], orElse: => F[Response[F]]) = directives.Directive.getOrElse[F, A](opt, orElse)

  type Filter = directives.Directive.Filter[F]
  val Filter = directives.Directive.Filter

  val commit = directives.Directive.commit

  def value[R](f: F[Result[R]]) = Directive[R](_ => f)

  implicit def DirectiveMonad[L] = directives.Directive.monad[F]

  type when[A] = directives.when[F, A]
  val when = directives.when

  object ops extends DirectiveOps[F] with RequestDirectives[F]

  object implicits {
    implicit def wrapSuccess[S](f: F[S]): directives.Directive[F, S] = ops.MonadDecorator(f).successF
  }
}
