package no.scalabin.http4s.directives

import cats.{Eq, Monad}
import cats.data.NonEmptyList
import cats.effect.Sync
import org.http4s.util.CaseInsensitiveString
import org.http4s._

import scala.language.{higherKinds, implicitConversions}

trait RequestDirectives[F[_]] {

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Monad[F]): Directive[F, Method] =
    when[F, Method] { case req if eq.eqv(M, req.method) => M } orElse sync.pure(Response[F](Status.MethodNotAllowed))

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

  def cookies[F[_]: Monad]: Directive[F, Option[NonEmptyList[RequestCookie]]] =
    header(org.http4s.headers.Cookie).map(_.map(_.values))

  def cookiesAsList[F[_]: Monad]: Directive[F, List[RequestCookie]] =
    cookies.map(_.toList.flatMap(_.toList))

  def cookiesOrElse[F[_]: Monad](f: => F[Response[F]]): Directive[F, NonEmptyList[RequestCookie]] =
    cookies.flatMap(opt => Directive.getOrElse(opt, f))

  def cookie[F[_]: Monad](name: String): Directive[F, Option[RequestCookie]] =
    cookies.map(_.flatMap(_.find(c => c.name == name)))

  def uri[F[_]: Monad]: Directive[F, Uri]       = Directive.request.map(_.uri)
  def path[F[_]: Monad]: Directive[F, Uri.Path] = uri.map(_.path)
  def query[F[_]: Monad]: Directive[F, Query]   = uri.map(_.query)

  def queryParams[F[_]: Monad](name: String): Directive[F, Seq[String]]   = query.map(_.multiParams.getOrElse(name, Nil))
  def queryParam[F[_]: Monad](name: String): Directive[F, Option[String]] = query.map(_.params.get(name))
  def queryParamOrElse[F[_]: Monad](name: String, f: => F[Response[F]]): Directive[F, String] = queryParam(name).flatMap(
    opt => Directive.getOrElse(opt, f)
  )

  def bodyAs[F[_]: Sync, A](implicit dec: EntityDecoder[F, A]): Directive[F, A] =
    bodyAs(_ => Response[F](Status.InternalServerError))

  def bodyAs[F[_]: Sync, A](onError: DecodeFailure => Response[F])(implicit dec: EntityDecoder[F, A]): Directive[F, A] = {
    Directive(
      req =>
        req
          .attemptAs[A]
          .fold(
            e => Result.failure(onError(e)),
            Result.success
        )
    )
  }
}
