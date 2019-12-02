package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.{Eq, Monad, MonadError}
import cats.data.NonEmptyList
import cats.effect.Sync
import org.http4s.util.CaseInsensitiveString
import org.http4s._

import scala.language.implicitConversions

trait RequestDirectives[F[_]] extends WhenOps[F] {

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Monad[F]): Directive[F, Method] =
    when[Method] { case req if eq.eqv(M, req.method) => M } orElseRes Response[F](Status.MethodNotAllowed)

  implicit def liftHeaderDirective[KEY <: HeaderKey](K: KEY)(implicit sync: Monad[F]): Directive[F, Option[K.HeaderT]] =
    request.header(K)

  implicit class HeaderDirective[KEY <: HeaderKey](val key: KEY)(implicit sync: Monad[F]) {
    def directive: Directive[F, Option[key.HeaderT]] = liftHeaderDirective(key)
  }

  object request extends RequestOps[F]
}

trait RequestOps[F[_]] {

  def headers(implicit M: Monad[F]): Directive[F, Headers] = Directive.request.map(_.headers)

  def header[KEY <: HeaderKey](key: KEY)(implicit M: Monad[F]): Directive[F, Option[key.HeaderT]] =
    headers.map(_.get(key.name)).map(_.flatMap(h => key.matchHeader(h)))

  def headerOrElse[KEY <: HeaderKey](key: KEY, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, key.HeaderT] =
    headerOrElseF(key, Monad[F].pure(orElse))

  def headerOrElseF[KEY <: HeaderKey](key: KEY, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, key.HeaderT] =
    header(key).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def headerOrElse[KEY <: HeaderKey](key: KEY, orElse: Directive[F, Response[F]])(
      implicit M: Monad[F]
  ): Directive[F, key.HeaderT] =
    header(key).flatMap(opt => Directive.getOrElse(opt, orElse))

  def header(key: String)(implicit M: Monad[F]): Directive[F, Option[Header]] = headers.map(_.get(CaseInsensitiveString(key)))

  def headerOrElse(key: String, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, Header] =
    headerOrElseF(key, M.pure(orElse))

  def headerOrElse(key: String, orElse: Directive[F, Response[F]])(implicit M: Monad[F]): Directive[F, Header] =
    header(key).flatMap(opt => Directive.getOrElse(opt, orElse))

  def headerOrElseF(key: String, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, Header] =
    header(key).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def cookies(implicit M: Monad[F]): Directive[F, Option[NonEmptyList[RequestCookie]]] =
    header(org.http4s.headers.Cookie).map(_.map(_.values))

  def cookiesAsList(implicit M: Monad[F]): Directive[F, List[RequestCookie]] =
    cookies.map(_.toList.flatMap(_.toList))

  def cookiesOrElse(orElse: => Response[F])(implicit M: Monad[F]): Directive[F, NonEmptyList[RequestCookie]] =
    cookiesOrElseF(M.pure(orElse))

  def cookiesOrElse(orElse: Directive[F, Response[F]])(implicit M: Monad[F]): Directive[F, NonEmptyList[RequestCookie]] =
    cookies.flatMap(opt => Directive.getOrElse(opt, orElse))

  def cookiesOrElseF(orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, NonEmptyList[RequestCookie]] =
    cookies.flatMap(opt => Directive.getOrElseF(opt, orElse))

  def cookie(name: String)(implicit M: Monad[F]): Directive[F, Option[RequestCookie]] =
    cookies.map(_.flatMap(_.find(c => c.name == name)))

  def cookieOrElse(name: String, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, RequestCookie] =
    cookie(name).flatMap(opt => Directive.getOrElse(opt, orElse))

  def cookieOrElse(name: String, orElse: Directive[F, Response[F]])(implicit M: Monad[F]): Directive[F, RequestCookie] =
    cookie(name).flatMap(opt => Directive.getOrElse(opt, orElse))

  def cookieOrElseF(name: String, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, RequestCookie] =
    cookie(name).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def uri(implicit M: Monad[F]): Directive[F, Uri]       = Directive.request.map(_.uri)
  def path(implicit M: Monad[F]): Directive[F, Uri.Path] = uri.map(_.path)
  def query(implicit M: Monad[F]): Directive[F, Query]   = uri.map(_.query)

  def queryParams(name: String)(implicit M: Monad[F]): Directive[F, Seq[String]]   = query.map(_.multiParams.getOrElse(name, Nil))
  def queryParam(name: String)(implicit M: Monad[F]): Directive[F, Option[String]] = query.map(_.params.get(name))
  def queryParamOrElse(name: String, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, String] =
    queryParamOrElseF(name, M.pure(orElse))

  def queryParamOrElse(name: String, orElse: Directive[F, Response[F]])(implicit M: Monad[F]): Directive[F, String] =
    queryParam(name).flatMap(opt => Directive.getOrElse(opt, orElse))

  def queryParamOrElseF(name: String, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, String] =
    queryParam(name).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def as[A](implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] =
    Directive(_.as[A].map(Result.success))

  def bodyAs[A](implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] =
    bodyAs(_ => Response[F](Status.InternalServerError))

  def bodyAs[A](
      onError: DecodeFailure => Response[F]
  )(implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] = {
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
