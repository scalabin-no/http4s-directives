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
    request.header(K)

  implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY)(implicit sync: Sync[F]) {
    def directive: Directive[F, Nothing, Option[key.HeaderT]] = liftHeaderDirective(key)
  }

  object request extends RequestOps
}

trait RequestOps {

  def headers[F[+_]: Sync]: Directive[F, Nothing, Headers] = Directive.request.map(_.headers)
  def header[F[+_]: Sync, KEY <: HeaderKey](key: KEY): Directive[F, Nothing, Option[key.HeaderT]] = headers.map(_.get(key.name)).map(_.flatMap(h => key.matchHeader(h)))
  def headerOrElse[F[+_]: Sync, KEY <: HeaderKey, L](key: KEY, f: => F[L]): Directive[F, L, key.HeaderT] = header(key).flatMap(opt => Directive.getOrElse(opt, f))
  def header[F[+_]: Sync](key: String): Directive[F, Nothing, Option[Header]] = headers.map(_.get(CaseInsensitiveString(key)))
  def headerOrElse[F[+_]: Sync, L](key: String, f: => F[L]): Directive[F, L, Header] = header(key).flatMap(opt => Directive.getOrElse(opt, f))

  def cookies[F[+_]: Sync]: Directive[F, Nothing, Option[NonEmptyList[Cookie]]] = header(org.http4s.headers.Cookie).map(_.map(_.values))

  def cookiesOrElse[F[+_]: Sync, L](f: => F[L]): Directive[F, L, NonEmptyList[Cookie]] =
    cookies.flatMap(opt => Directive.getOrElse(opt, f))

  def cookie[F[+_]: Sync](name: String): Directive[F, Nothing, Option[Cookie]] = cookies.map(_.flatMap(_.find(c => c.name == name)))

  def uri[F[+_]: Sync]: Directive[F, Nothing, Uri] = Directive.request.map(_.uri)
  def path[F[+_]: Sync]: Directive[F, Nothing, Uri.Path] = uri.map(_.path)
  def query[F[+_]: Sync]: Directive[F, Nothing, Query] = uri.map(_.query)

  def queryParams[F[+_]: Sync](name: String): Directive[F, Nothing, Seq[String]] = query.map(_.multiParams.getOrElse(name, Nil))
  def queryParam[F[+_]: Sync](name: String): Directive[F, Nothing, Option[String]] = query.map(_.params.get(name))
  def queryParamOrElse[F[+_]: Sync, L](name: String, f: => F[L]): Directive[F, L, String] = queryParam(name).flatMap(
    opt => Directive.getOrElse(opt, f)
  )

  def bodyAs[F[+_]: Sync, A](implicit dec: EntityDecoder[F, A]): Directive[F, Nothing, A] = Directive(req => req.as[A].map(Result.success(_)))
}
