package no.scalabin.http4s
package directives

import cats.Monad
import org.http4s._

import scala.language.{higherKinds, implicitConversions}

object Directives {
  def apply[F[_]](implicit M: Monad[F]): Directives[F] = new Directives[F]
}

class Directives[F[_]: Monad] {

  type Result[A] = directives.Result[F, A]
  val Result = directives.Result

  type Directive[A] = directives.Directive[F, A]

  object Directive {
    def apply[A](run: Request[F] => F[Result[A]]): Directive[A] = directives.Directive(run)
  }

  def result[A](result: Result[A]) = directives.Directive.result(result)

  def pure[A](success: A)              = directives.Directive.pure(success)
  def success[A](success: A)           = pure(success)
  def failure[A](failure: Response[F]) = directives.Directive.failure[F, A](failure)
  def error[A](error: Response[F])     = directives.Directive.error[F, A](error)

  def liftF[A](success: F[A])           = directives.Directive.liftF[F, A](success)
  def successF[A](success: F[A])        = liftF(success)
  def failureF(failure: F[Response[F]]) = directives.Directive.failureF[F, Response[F]](failure)
  def errorF(error: F[Response[F]])     = directives.Directive.errorF[F, Response[F]](error)

  def getOrElseF[A](opt: F[Option[A]], orElse: => F[Response[F]]) = directives.Directive.getOrElseF(opt, orElse)

  def getOrElse[A](opt: Option[A], orElse: => F[Response[F]]) = directives.Directive.getOrElse(opt, orElse)

  type Filter = directives.Directive.Filter[F]
  val Filter = directives.Directive.Filter

  val commit = directives.Directive.commit

  def value[A](f: F[Result[A]]) = Directive[A](_ => f)

  implicit def DirectiveMonad = directives.Directive.monad[F]

  type when[A] = directives.when[F, A]
  val when = directives.when

  object ops extends DirectiveOps[F] with RequestDirectives[F]

  object implicits {
    implicit def wrapSuccess[S](f: F[S]): directives.Directive[F, S] = ops.MonadDecorator(f).successF
  }
}
