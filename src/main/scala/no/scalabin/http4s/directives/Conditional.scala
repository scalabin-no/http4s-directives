package no.scalabin.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}

import cats.Monad
import cats.data.{NonEmptyList, OptionT}
import org.http4s._
import org.http4s.headers._
import org.http4s.parser.HttpHeaderParser
import org.http4s.util.CaseInsensitiveString

import scala.language.higherKinds

object Conditional {
  def apply[F[_]]: Conditional[F] = new Conditional[F] {}
}

trait Conditional[F[_]] {
  type ResponseDirective = Directive[F, Response[F]]

  object request extends RequestOps[F]

  def ifModifiedSince(lm: LocalDateTime, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifModifiedSinceF(lm, M.pure(orElse))
  }

  def ifModifiedSinceF(lm: LocalDateTime, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(`If-Modified-Since`)
      res <- mod
              .filter(_.date == date)
              .fold(Directive.successF[F, Response[F]](orElse))(_ =>
                Directive.failure[F, Response[F]](Response[F](Status.NotModified)))
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifUnmodifiedSince(lm: LocalDateTime, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifUnmodifiedSinceF(lm, M.pure(orElse))
  }

  def ifUnmodifiedSinceF(lm: LocalDateTime, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(IfUnmodifiedSince)
      res <- mod
              .filter(_.date == date)
              .fold(Directive.failure[F, Response[F]](Response[F](Status.NotModified)))(_ =>
                Directive.successF[F, Response[F]](orElse))
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifNoneMatch(tag: ETag.EntityTag, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifNoneMatchF(tag, Monad[F].pure(orElse))
  }

  def ifNoneMatchF(tag: ETag.EntityTag, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    for {
      mod <- request.header(`If-None-Match`)
      res <- mod
              .filter(_.tags.exists(t => t.exists(_ == tag)))
              .fold(Directive.successF(orElse))(_ => Directive.failure(Response[F](Status.NotModified)))
    } yield res.putHeaders(ETag(tag))
  }

  def ifMatch(tag: ETag.EntityTag, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifMatchF(tag, M.pure(orElse))
  }

  def ifMatchF(tag: ETag.EntityTag, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    for {
      mod <- request.header(IfMatch)
      res <- mod
              .filter(_.tags.exists(t => t.exists(_ == tag)))
              .fold(Directive.failure[F, Response[F]](Response[F](Status.NotModified)))(_ =>
                Directive.successF[F, Response[F]](orElse))
    } yield res.putHeaders(ETag(tag))
  }
}

object IfMatch extends HeaderKey.Singleton {

  override type HeaderT = `If-None-Match`.HeaderT

  override val name: CaseInsensitiveString = CaseInsensitiveString("If-Match")

  override def matchHeader(header: Header): Option[HeaderT] =
    if (header.name == name) parse(header.value).right.toOption else None

  /** Match any existing entity */
  val `*` = `If-None-Match`(None)

  def apply(first: ETag.EntityTag, rest: ETag.EntityTag*): `If-None-Match` = {
    `If-None-Match`(Some(NonEmptyList(first, rest.toList)))
  }

  override def parse(s: String): ParseResult[HeaderT] =
    HttpHeaderParser.IF_NONE_MATCH(s)
}

object IfUnmodifiedSince extends HeaderKey.Singleton {

  override type HeaderT = `If-Modified-Since`.HeaderT

  override def name: CaseInsensitiveString = CaseInsensitiveString("If-Unmodified-Since")

  override def matchHeader(header: Header): Option[HeaderT] =
    if (header.name == name) parse(header.value).right.toOption else None

  override def parse(s: String): ParseResult[`If-Modified-Since`] =
    HttpHeaderParser.IF_MODIFIED_SINCE(s)
}
