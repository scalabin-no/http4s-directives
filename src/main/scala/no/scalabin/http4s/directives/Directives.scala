package no.scalabin.http4s
package directives

import cats.Monad
import org.http4s._

import scala.language.{higherKinds, implicitConversions}


object Directives {
  def apply[F[+_]](implicit M: Monad[F]):Directives[F] = new Directives[F]
}

class Directives[F[+_]: Monad] {

  type Result[+L, +R] = directives.Result[L, R]
  val Result          = directives.Result

  type Directive[+L, +R] = directives.Directive[F, L, R]

  object Directive {
    def apply[L, R](run: Request[F] => F[Result[L, R]]):Directive[L, R] = directives.Directive[F, L, R](run)
  }

  def result[L, R](result: Result[L, R]) = directives.Directive.result[F, L, R](result)
  def success[R](success: R) = directives.Directive.success[F, R](success)
  def failure[L](failure: L) = directives.Directive.failure[F, L](failure)
  def error[L](error: L)     = directives.Directive.error[F, L](error)

  def successF[R](success: F[R]) = directives.Directive.successF[F, R](success)
  def failureF[L](failure: F[L]) = directives.Directive.failureF[F, L](failure)
  def errorF[L](error: F[L])     = directives.Directive.errorF[F, L](error)

  def getOrElseF[L, R](opt:F[Option[R]], orElse: => F[L]) = directives.Directive.getOrElseF[F, L, R](opt, orElse)

  def getOrElse[A, L](opt:Option[A], orElse: => F[L]) = directives.Directive.getOrElse[F, L, A](opt, orElse)

  type Filter[+L] = directives.Directive.Filter[L]
  val Filter      = directives.Directive.Filter

  val commit = directives.Directive.commit

  def value[L, R](f: F[Result[L, R]]) = Directive[L, R](_ => f)

  implicit def DirectiveMonad[L] = directives.Directive.monad[F, L]


  type when[A] = directives.when[F, A]
  val when = directives.when

  object ops extends DirectiveOps[F] with RequestDirectives[F]

  object implicits {
    implicit def wrapSuccess[S](f: F[S]): directives.Directive[F, Nothing, S] = ops.MonadDecorator(f).successF
  }
}

