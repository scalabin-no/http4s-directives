package no.shiplog.http4s.directives

import cats.Eq
import cats.effect.Sync
import org.http4s.{EntityDecoder, _}
import cats.syntax.functor._
import org.http4s.dsl.impl.MethodConcat
import scala.language.higherKinds
import scala.language.implicitConversions

import no.shiplog.http4s.directives


object Directives {
  def apply[F[+_]](implicit M: Sync[F]):Directives[F] = new Directives[F]{
    implicit val F: Sync[F] = M
  }
}

trait Directives[F[+_]] {
  implicit val F: Sync[F]

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

  def getOrElseF[L, R](opt:F[Option[R]], orElse: => L) = directives.Directive[F, L, R] { _ =>
    opt.map(_.fold[Result[L, R]](Result.Failure(orElse))(Result.Success(_)))
  }

  def getOrElse[A, L](opt:Option[A], orElse: => L) = opt match {
    case Some(r) => success(r)
    case None => failure(orElse)
  }

  type Filter[+L] = directives.Directive.Filter[L]
  val Filter      = directives.Directive.Filter

  val commit = directives.Directive.commit

  def value[L, R](f: F[Result[L, R]]) = Directive[L, R](_ => f)

  implicit def DirectiveMonad[L] = directives.Directive.monad[F, L]

  case class when[R](f:PartialFunction[Request[F], R]){
    def orElse[L](fail: => L) =
      request.apply.flatMap(req => f.lift(req) match {
        case Some(r) => success(r)
        case None => failure(fail)
      })
  }

  object ops {

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Filter(b, () => failure)
    }

    implicit class MonadDecorator[+X](f: F[X]) {
      def successValue = directives.Directive[F, Nothing, X](_ => f.map(Result.Success(_)))
      def failureValue = directives.Directive[F, X, Nothing](_ => f.map(Result.Failure(_)))
      def errorValue   = directives.Directive[F, X, Nothing](_ => f.map(Result.Error(_)))
    }

    implicit def MethodDirective(M: Method)(implicit eq: Eq[Method]): Directive[Response[F], Method] = when { case req if eq.eqv(M, req.method) => M } orElse Response[F](Status.MethodNotAllowed)

    implicit def MethodsDirective(M: MethodConcat): Directive[Response[F], Method] = when { case req if M.methods(req.method) => req.method } orElse Response[F](Status.MethodNotAllowed)

    implicit def liftHeaderDirective[KEY <: HeaderKey](K: KEY): Directive[Nothing, Option[K.HeaderT]] =
      request.headers.map(_.get(K.name).flatMap(K.unapply(_)))

    implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY) {
      def directive: Directive[Nothing, Option[key.HeaderT]] = liftHeaderDirective(key)
    }
  }

  object implicits {
    implicit def wrapSuccess[S](f: F[S]): directives.Directive[F, Nothing, S] = ops.MonadDecorator(f).successValue
  }

  object request {
    def apply = Directive[Nothing, Request[F]](req => F.pure(Result.Success(req)))

    def headers: Directive[Nothing, Headers] = apply.map(_.headers)
    def header(key: HeaderKey): Directive[Nothing, Option[Header]] = headers.map(_.get(key.name))

    def uri: Directive[Nothing, Uri] = apply.map(_.uri)
    def path: Directive[Nothing, Uri.Path] = uri.map(_.path)
    def query: Directive[Nothing, Query] = uri.map(_.query)

    def bodyAs[A](implicit dec: EntityDecoder[F, A]): Directive[Nothing, A] = Directive(req => req.as[A].map(Result.Success(_)))

  }
}

