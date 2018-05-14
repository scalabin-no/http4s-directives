package no.scalabin.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}

import cats.data.{NonEmptyList, OptionT}
import cats.effect.Sync
import org.http4s._
import org.http4s.headers._
import org.http4s.parser.HttpHeaderParser
import org.http4s.util.CaseInsensitiveString

import scala.language.higherKinds


object Conditional {
  type ResponseDirective[F[+_]] = Directive[F, Response[F], Response[F]]

  private object request extends RequestOps

  def ifModifiedSince[F[+_]: Sync](lm: LocalDateTime, orElse: => F[Response[F]]): ResponseDirective[F] = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(`If-Modified-Since`)
      res <- OptionT.fromOption(mod).filter(_.date == date).cata(Directive.successF(orElse), _ => Directive.failure(Response[F](Status.NotModified)))
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifUnmodifiedSince[F[+_]: Sync](lm: LocalDateTime, orElse: => F[Response[F]]): ResponseDirective[F] = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(IfUnmodifiedSince)
      res <- OptionT.fromOption(mod).filter(_.date == date).cata(Directive.failure(Response[F](Status.NotModified)), _ => Directive.successF(orElse))
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifNoneMatch[F[+_]: Sync](tag: ETag.EntityTag, orElse: => F[Response[F]]): ResponseDirective[F] = {
    for {
      mod <- request.header(`If-None-Match`)
      res <- OptionT.fromOption(mod).filter(_.tags.exists(_.exists(a => a == tag))).cata(Directive.successF(orElse), _ => Directive.failure(Response[F](Status.NotModified)))
    } yield res.putHeaders(ETag(tag))
  }

  def ifMatch[F[+_]: Sync](tag: ETag.EntityTag, orElse: => F[Response[F]]): ResponseDirective[F] = {
    for {
      mod <- request.header(IfMatch)
      res <- OptionT.fromOption(mod).filter(_.tags.exists(_.exists(a => a == tag))).cata(Directive.failure(Response[F](Status.NotModified)), _ => Directive.successF(orElse))
    } yield res.putHeaders(ETag(tag))
  }
}

object IfMatch extends HeaderKey.Singleton {

  override type HeaderT = `If-None-Match`.HeaderT

  override val name: CaseInsensitiveString = CaseInsensitiveString("If-Match")

  override def matchHeader(header: Header): Option[HeaderT] = if (header.name == name) parse(header.value).toOption else None

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

  override def matchHeader(header: Header): Option[HeaderT] = if (header.name == name) parse(header.value).toOption else None

  override def parse(s: String): ParseResult[`If-Modified-Since`] =
    HttpHeaderParser.IF_MODIFIED_SINCE(s)
}
