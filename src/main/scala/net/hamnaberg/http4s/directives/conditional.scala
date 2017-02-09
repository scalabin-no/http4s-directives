package net.hamnaberg.http4s.directives

import java.time.temporal.ChronoField

import org.http4s.{Response, Status}
import org.http4s.headers._

import scalaz.concurrent.Task
import Directives._, ops._

object conditional {

  object get {
    import java.time.Instant


    def ifModifiedSince(lm: Instant, orElse: => Task[Response]): Directive[Task, Response, Response] = {
      val date = lm.`with`(ChronoField.MILLI_OF_SECOND, 0L)
      for {
        mod <- `If-Modified-Since`
        res <- mod.filter(_.date == date).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(`Last-Modified`(date))
    }

    /*def ifUnmodifiedSince(lm: Instant, orElse: => F[Response]): Directive[Response, Response] = {
      val date = lm.`with`(ChronoField.MILLI_OF_SECOND, 0L)
      for {
        mod <- `If-Unmodified-Since`
        res <- mod.filter(_.date == date).map(_ => orElse).getOrElse(F.point(Response(Status.NotModified)))
      } yield res
    }*/

    def ifNoneMatch(tag: ETag.EntityTag, orElse: => Task[Response]): Directive[Task, Response, Response] = {
      for {
        mod <- `If-None-Match`
        res <- mod.filter(_.tags.exists(_.contains(tag))).map(_ => Task.delay(Response(Status.NotModified))).getOrElse(orElse)
      } yield res.putHeaders(ETag(tag))
    }

    /*def ifMatch(toMatch: Option[NonEmptyList[ETag.EntityTag]], orElse: => F[Response]): Directive[Response, Response] = {
      for {
        mod <- `If-Match`
        res <- mod.filter(_.tags == toMatch).map(_ => orElse).getOrElse(F.point(Response(Status.NotModified)))
      } yield res
    }*/

  }

}
