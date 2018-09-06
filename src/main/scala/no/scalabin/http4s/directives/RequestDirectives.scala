package no.scalabin.http4s.directives

import cats.{Eq, Monad}
import cats.data.NonEmptyList
import cats.syntax.functor._
import org.http4s.dsl.impl.MethodConcat
import org.http4s.util.CaseInsensitiveString
import org.http4s._
import org.http4s.headers.Allow

import scala.language.{higherKinds, implicitConversions}

trait RequestDirectives[F[_]] {

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Monad[F]): Directive[F, Method] =
    when[F, Method] { case req if eq.eqv(M, req.method) => M } orElse sync.pure(Response[F](Status.MethodNotAllowed))

  implicit def MethodsDirective(M: MethodConcat)(implicit sync: Monad[F]): Directive[F, Method] =
    when[F, Method] { case req if M.methods(req.method) => req.method } orElse sync.pure(
      Response[F](Status.MethodNotAllowed).putHeaders({
        val methods = M.methods.toList.sortBy(_.name)
        Allow(NonEmptyList(methods.head, methods.tail))
      }))

  implicit def liftHeaderDirective[KEY <: HeaderKey](K: KEY)(implicit sync: Monad[F]): Directive[F, Option[K.HeaderT]] =
    request.header(K)

  implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY)(implicit sync: Monad[F]) {
    def directive: Directive[F, Option[key.HeaderT]] = liftHeaderDirective(key)
  }

  object request extends RequestOps
}

trait RequestOps {

  def headers[F[_]: Monad]: Directive[F, Headers] = Directive.request.map(_.headers)
  def header[F[_]: Monad, KEY <: HeaderKey](key: KEY): Directive[F, Option[key.HeaderT]] =
    headers.map(_.get(key.name)).map(_.flatMap(h => key.matchHeader(h)))
  def headerOrElse[F[_]: Monad, KEY <: HeaderKey](key: KEY, f: => F[Response[F]]): Directive[F, key.HeaderT] =
    header(key).flatMap(opt => Directive.getOrElse(opt, f))
  def header[F[_]: Monad](key: String): Directive[F, Option[Header]] = headers.map(_.get(CaseInsensitiveString(key)))
  def headerOrElse[F[_]: Monad](key: String, f: => F[Response[F]]): Directive[F, Header] =
    header(key).flatMap(opt => Directive.getOrElse(opt, f))

  def cookies[F[_]: Monad]: Directive[F, Option[NonEmptyList[Cookie]]] =
    header(org.http4s.headers.Cookie).map(_.map(_.values))

  def cookiesOrElse[F[_]: Monad](f: => F[Response[F]]): Directive[F, NonEmptyList[Cookie]] =
    cookies.flatMap(opt => Directive.getOrElse(opt, f))

  def cookie[F[_]: Monad](name: String): Directive[F, Option[Cookie]] =
    cookies.map(_.flatMap(_.find(c => c.name == name)))

  def uri[F[_]: Monad]: Directive[F, Uri]       = Directive.request.map(_.uri)
  def path[F[_]: Monad]: Directive[F, Uri.Path] = uri.map(_.path)
  def query[F[_]: Monad]: Directive[F, Query]   = uri.map(_.query)

  def queryParams[F[_]: Monad](name: String): Directive[F, Seq[String]]   = query.map(_.multiParams.getOrElse(name, Nil))
  def queryParam[F[_]: Monad](name: String): Directive[F, Option[String]] = query.map(_.params.get(name))
  def queryParamOrElse[F[_]: Monad](name: String, f: => F[Response[F]]): Directive[F, String] = queryParam(name).flatMap(
    opt => Directive.getOrElse(opt, f)
  )

  def bodyAs[F[_]: Monad, A](implicit dec: EntityDecoder[F, A]): Directive[F, A] =
    Directive(req => req.as[A].map(Result.success))
}
