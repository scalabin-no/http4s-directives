package net.hamnaberg.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.language.higherKinds
import org.http4s._
import org.http4s.headers._
import org.http4s.parser.HttpHeaderParser
import org.http4s.util.CaseInsensitiveString

object Conditional {
  def apply[F[+_]] = new Conditional[F] {}
}

trait Conditional[F[+_]] {
  def ifModifiedSince(lm: LocalDateTime, orElse: => F[Response[F]])(implicit directives: Directives[F]): Directive[F, Response[F], Response[F]] = {
    import directives._
    import ops._
    import implicits._

    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- `If-Modified-Since`.directive
      res <- mod.filter(_.date == date).map(_ => F.delay(Response[F](Status.NotModified))).getOrElse(orElse).successValue
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifUnmodifiedSince(lm: LocalDateTime, orElse: => F[Response[F]])(implicit directives: Directives[F]): Directive[F, Response[F], Response[F]] = {
    import directives._
    import ops._

    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- IfUnmodifiedSince.directive
      res <- mod.filter(_.date == date).fold(F.delay(Response[F](Status.NotModified)))(_ => orElse).successValue
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifNoneMatch(tag: ETag.EntityTag, orElse: => F[Response[F]])(implicit directives: Directives[F]): Directive[F, Response[F], Response[F]] = {
    import directives._
    import ops._
    for {
      mod <- `If-None-Match`.directive
      res <- mod.filter(_.tags.exists(_.exists(a => a == tag))).map(_ => F.delay(Response[F](Status.NotModified))).getOrElse(orElse).successValue
    } yield res.putHeaders(ETag(tag))
  }

  def ifMatch(tag: ETag.EntityTag, orElse: => F[Response[F]])(implicit directives: Directives[F]): Directive[F, Response[F], Response[F]] = {
    import directives._
    import ops._

    for {
      mod <- IfMatch.directive
      res <- mod.filter(_.tags.exists(_.exists(a => a == tag))).map(_ => orElse).getOrElse(F.delay(Response[F](Status.NotModified))).successValue
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
