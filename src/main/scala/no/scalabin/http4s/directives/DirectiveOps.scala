package no.scalabin.http4s.directives

import cats.Eq
import cats.effect.Sync
import cats.syntax.functor._
import org.http4s.dsl.impl.MethodConcat
import org.http4s.{HeaderKey, Method, Response, Status}

import scala.language.higherKinds
import scala.language.implicitConversions


trait DirectiveOps[F[+_]] extends RequestDirectives[F] {
  implicit class FilterSyntax(b:Boolean) {
    def | [L](failure: => L) = Directive.Filter(b, () => failure)
  }

  implicit class MonadDecorator[+X](f: F[X])(implicit sync: Sync[F]) {
    def successValue = Directive[F, Nothing, X](_ => f.map(Result.Success(_)))
    def failureValue = Directive[F, X, Nothing](_ => f.map(Result.Failure(_)))
    def errorValue   = Directive[F, X, Nothing](_ => f.map(Result.Error(_)))
  }

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Sync[F]): Directive[F, Response[F], Method] = when[F, Method] { case req if eq.eqv(M, req.method) => M } orElse Response[F](Status.MethodNotAllowed)

  implicit def MethodsDirective(M: MethodConcat)(implicit sync: Sync[F]): Directive[F, Response[F], Method] = when[F, Method] { case req if M.methods(req.method) => req.method } orElse Response[F](Status.MethodNotAllowed)

  implicit def liftHeaderDirective[KEY <: HeaderKey](K: KEY)(implicit sync: Sync[F]): Directive[F, Nothing, Option[K.HeaderT]] =
    headers.map(_.get(K.name).flatMap(K.unapply(_)))

  implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY)(implicit sync: Sync[F]) {
    def directive: Directive[F, Nothing, Option[key.HeaderT]] = liftHeaderDirective(key)
  }

}
