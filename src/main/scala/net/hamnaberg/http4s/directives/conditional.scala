package net.hamnaberg.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.Locale

import org.http4s._
import org.http4s.headers.{`If-None-Match`, _}
import fs2.Task
import Directive._
import cats.data.NonEmptyList
import ops._
import org.http4s.parser.HttpHeaderParser
import org.http4s.util.CaseInsensitiveString


object conditional {

  object get {

    def ifModifiedSince(lm: LocalDateTime, orElse: => Task[Response]): Directive[Response, Response] = {
      val date = HttpDate.unsafeFromInstant(lm.withNano(0).toInstant(ZoneOffset.UTC))
      for {
        mod <- `If-Modified-Since`
        res <- mod.filter(_.date == date).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(`Last-Modified`(date))
    }

    def ifUnmodifiedSince(lm: LocalDateTime, orElse: => Task[Response]): Directive[Response, Response] = {
      val date = HttpDate.unsafeFromInstant(lm.withNano(0).toInstant(ZoneOffset.UTC))
      for {
        mod <- IfUnmodifiedSince
        res <- mod.filter(_.date == date).map(_ => orElse).getOrElse(Task.delay(Response(Status.NotModified)))
      } yield res.putHeaders(`Last-Modified`(date))
    }

    def ifNoneMatch(tag: ETag.EntityTag, orElse: => Task[Response]): Directive[Response, Response] = {
      for {
        mod <- `If-None-Match`
        res <- mod.filter(_.tags.exists(_.exists(a => a == tag))).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(ETag(tag))
    }

    def ifMatch(tag: ETag.EntityTag, orElse: => Task[Response]): Directive[Response, Response] = {
      for {
        mod <- IfMatch
        res <- mod.filter(_.tags.exists(_.exists(a => a == tag))).map(_ => orElse).getOrElse(Task.delay(Response(Status.NotModified)))
      } yield res.putHeaders(ETag(tag))
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


  }

}
