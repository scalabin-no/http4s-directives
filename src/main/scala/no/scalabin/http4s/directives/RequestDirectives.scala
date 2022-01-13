package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.{data, Eq, Monad, MonadError}
import cats.data.NonEmptyList
import org.http4s._
import org.http4s.headers.`Content-Type`
import org.typelevel.ci.CIString

import scala.language.implicitConversions

trait RequestDirectives[F[_]] extends WhenOps[F] {

  implicit def MethodDirective(M: Method)(implicit eq: Eq[Method], sync: Monad[F]): Directive[F, Method] =
    when[Method] { case req if eq.eqv(M, req.method) => M } orElseRes Response[F](Status.MethodNotAllowed)

  object request extends RequestOps[F]
}

trait RequestOps[F[_]] {
  def apply()(implicit monad: Monad[F]) = Directive.request[F]

  def headers(implicit M: Monad[F]): Directive[F, Headers] = apply().map(_.headers)

  def header[KEY](implicit H: Header.Select[KEY], M: Monad[F]): Directive[F, Option[H.F[KEY]]] =
    headers.map(_.get[KEY])

  def headerOrElse[KEY](orElse: => Response[F])(implicit H: Header.Select[KEY], M: Monad[F]): Directive[F, H.F[KEY]] =
    headerOrElseF(Monad[F].pure(orElse))

  def headerOrElseF[KEY](orElse: F[Response[F]])(implicit H: Header.Select[KEY], M: Monad[F]): Directive[F, H.F[KEY]] =
    header.flatMap(opt => Directive.getOrElseF(opt, orElse))

  def headerOrElse[KEY](orElse: Directive[F, Response[F]])(implicit
      H: Header.Select[KEY],
      M: Monad[F]
  ): Directive[F, H.F[KEY]] =
    header.flatMap(opt => Directive.getOrElse(opt, orElse))

  def header(key: CIString)(implicit M: Monad[F]): Directive[F, Option[data.NonEmptyList[Header.Raw]]] = headers.map(_.get(key))

  def headerOrElse(key: CIString, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, data.NonEmptyList[Header.Raw]] =
    headerOrElseF(key, M.pure(orElse))

  def headerOrElse(key: CIString, orElse: Directive[F, Response[F]])(implicit
      M: Monad[F]
  ): Directive[F, data.NonEmptyList[Header.Raw]] =
    header(key).flatMap(opt => Directive.getOrElse(opt, orElse))

  def headerOrElseF(key: CIString, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, data.NonEmptyList[Header.Raw]] =
    header(key).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def expectMediaType(first: MediaType, rest: MediaType*)(
      validate: (MediaType, MediaType) => Boolean = _.satisfiedBy(_)
  )(implicit M: Monad[F]) = {
    val nel = first :: rest.toList
    for {
      ct <- headerOrElse[`Content-Type`](Response[F](Status.UnprocessableEntity))
      if Directive.Filter(nel.exists(m => validate(m, ct.mediaType)), Directive.failure(Response[F](Status.UnsupportedMediaType)))
    } yield ct.mediaType
  }

  def expectMediaRange(
      range: Set[MediaRange]
  )(validate: (MediaRange, MediaRange) => Boolean = _.satisfiedBy(_))(implicit M: Monad[F]) = {
    for {
      ct <- headerOrElse[`Content-Type`](Response[F](Status.UnprocessableEntity))
      if Directive.Filter(
        range.exists(m => validate(m, ct.mediaType)),
        Directive.failure(Response[F](Status.UnsupportedMediaType))
      )
    } yield ct.mediaType
  }

  def cookies(implicit M: Monad[F]): Directive[F, Option[NonEmptyList[RequestCookie]]] =
    header[org.http4s.headers.Cookie].map(_.map(_.values))

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

  def queryParams(name: String)(implicit M: Monad[F]): Directive[F, Seq[String]]                         = query.map(_.multiParams.getOrElse(name, Nil))
  def queryParam(name: String)(implicit M: Monad[F]): Directive[F, Option[String]]                       = query.map(_.params.get(name))
  def queryParamOrElse(name: String, orElse: => Response[F])(implicit M: Monad[F]): Directive[F, String] =
    queryParamOrElseF(name, M.pure(orElse))

  def queryParamOrElse(name: String, orElse: Directive[F, Response[F]])(implicit M: Monad[F]): Directive[F, String] =
    queryParam(name).flatMap(opt => Directive.getOrElse(opt, orElse))

  def queryParamOrElseF(name: String, orElse: F[Response[F]])(implicit M: Monad[F]): Directive[F, String] =
    queryParam(name).flatMap(opt => Directive.getOrElseF(opt, orElse))

  def as[A](implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] =
    Directive(_.as[A].map(Result.success))

  def asExpected[A](implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] = {
    for {
      _      <- expectMediaRange(dec.consumes)()
      result <- as[A]
    } yield result
  }

  def bodyAs[A](implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] =
    bodyAs(_ => Response[F](Status.InternalServerError))

  def bodyAs[A](
      onError: DecodeFailure => Response[F]
  )(implicit dec: EntityDecoder[F, A], M: MonadError[F, Throwable]): Directive[F, A] = {
    Directive(req =>
      req
        .attemptAs[A]
        .fold(
          e => Result.failure(onError(e)),
          Result.success
        )
    )
  }
}
