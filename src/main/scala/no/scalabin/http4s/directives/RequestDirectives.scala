package no.scalabin.http4s.directives

import cats.Eq
import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.functor._
import org.http4s.dsl.impl.MethodConcat
import org.http4s.util.CaseInsensitiveString
import org.http4s._
import org.http4s.headers.Allow

import scala.language.{higherKinds, implicitConversions}

trait RequestDirectives[F[+_]] {

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Sync[F]): Directive[F, Response[F], Method] = when[F, Method] { case req if eq.eqv(M, req.method) => M } orElse Response[F](Status.MethodNotAllowed)

  implicit def MethodsDirective(M: MethodConcat)(implicit sync: Sync[F]): Directive[F, Response[F], Method] = when[F, Method] { case req if M.methods(req.method) => req.method } orElse Response[F](Status.MethodNotAllowed).putHeaders({
    val methods = M.methods.toList.sortBy(_.name)
    Allow(NonEmptyList(methods.head, methods.tail))
  })

  implicit def liftHeaderDirective[KEY <: HeaderKey](K: KEY)(implicit sync: Sync[F]): Directive[F, Nothing, Option[K.HeaderT]] =
    headers.map(_.get(K.name).flatMap(K.unapply(_)))

  implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY)(implicit sync: Sync[F]) {
    def directive: Directive[F, Nothing, Option[key.HeaderT]] = liftHeaderDirective(key)
  }

  def request(implicit sync: Sync[F]): Directive[F, Nothing, Request[F]] = Directive[F, Nothing, Request[F]](req => sync.pure(Result.Success(req)))

  def headers(implicit sync: Sync[F]): Directive[F, Nothing, Headers] = request.map(_.headers)
  def header(key: HeaderKey)(implicit sync: Sync[F]): Directive[F, Nothing, Option[Header]] = headers.map(_.get(key.name))
  def header(key: String)(implicit sync: Sync[F]): Directive[F, Nothing, Option[Header]] = headers.map(_.get(CaseInsensitiveString(key)))

  def uri(implicit sync: Sync[F]): Directive[F, Nothing, Uri] = request.map(_.uri)
  def path(implicit sync: Sync[F]): Directive[F, Nothing, Uri.Path] = uri.map(_.path)
  def query(implicit sync: Sync[F]): Directive[F, Nothing, Query] = uri.map(_.query)

  def bodyAs[A](implicit dec: EntityDecoder[F, A], sync: Sync[F]): Directive[F, Nothing, A] = Directive(req => req.as[A].map(Result.Success(_)))

}
