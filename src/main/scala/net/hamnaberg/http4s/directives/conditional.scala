package net.hamnaberg.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.util.Locale

import org.http4s._
import org.http4s.headers.{`If-None-Match`, _}

import scalaz.concurrent.Task
import Directives._
import ops._
import org.http4s.parser.HttpHeaderParser
import org.http4s.util.{CaseInsensitiveString, NonEmptyList}

import scala.util.Try

object conditional {

  object get {

    def ifModifiedSince(lm: LocalDateTime, orElse: => Task[Response]): Directive[Response, Response] = {
      val date = lm.withNano(0).toInstant(ZoneOffset.UTC)
      for {
        mod <- `If-Modified-Since`
        res <- mod.filter(_.date == date).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(`Last-Modified`(date))
    }

    def ifUnmodifiedSince(lm: LocalDateTime, orElse: => Task[Response]): Directive[Response, Response] = {
      val rfcFormatter = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
      val date = lm.withNano(0)
      for {
        mod <- request.header(`If-Unmodified-Since`)
        parsedDate = mod.map(_.value).flatMap(s => Try { LocalDateTime.parse(s, rfcFormatter)}.toOption)
        res <- parsedDate.filter(_ == date).map(_ => orElse).getOrElse(Task.delay(Response(Status.NotModified)))
      } yield res.putHeaders(`Last-Modified`(date.toInstant(ZoneOffset.UTC)))
    }

    def ifNoneMatch(tag: ETag.EntityTag, orElse: => Task[Response]): Directive[Response, Response] = {
      for {
        mod <- `If-None-Match`
        res <- mod.filter(_.tags.exists(_.contains(tag))).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(ETag(tag))
    }

    def ifMatch(tag: ETag.EntityTag, orElse: => Task[Response]): Directive[Response, Response] = {
      for {
        mod <- IfMatch
        res <- mod.filter(_.tags.exists(_.contains(tag))).map(_ => orElse).getOrElse(Task.delay(Response(Status.NotModified)))
      } yield res.putHeaders(ETag(tag))
    }

    object IfMatch extends HeaderKey.Singleton {

      override type HeaderT = `If-None-Match`.HeaderT

      override val name: CaseInsensitiveString = CaseInsensitiveString("If-Match")

      override def matchHeader(header: Header): Option[HeaderT] = if (header.name == name) parse(header.value).toOption else None

      /** Match any existing entity */
      val `*` = `If-None-Match`(None)

      def apply(first: ETag.EntityTag, rest: ETag.EntityTag*): `If-None-Match` = {
        `If-None-Match`(Some(NonEmptyList(first, rest:_*)))
      }

      override def parse(s: String): ParseResult[HeaderT] =
        HttpHeaderParser.IF_NONE_MATCH(s)
    }

  }

}
