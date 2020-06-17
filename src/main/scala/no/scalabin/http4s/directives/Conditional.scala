package no.scalabin.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}

import cats.Monad
import org.http4s._
import org.http4s.headers._

object Conditional {
  def apply[F[_]]: Conditional[F] = new Conditional[F] {}
}

trait Conditional[F[_]] extends RequestDirectives[F] {
  type ResponseDirective = Directive[F, Response[F]]

  def ifModifiedSince(lm: LocalDateTime, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifModifiedSinceF(lm, M.pure(orElse))
  }

  def ifModifiedSinceF(lm: LocalDateTime, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    ifModifiedSinceDir(lm, Directive.successF[F, Response[F]](orElse))
  }

  def ifModifiedSinceDir(lm: LocalDateTime, orElse: ResponseDirective)(implicit M: Monad[F]): ResponseDirective = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(`If-Modified-Since`)
      res <- mod
               .filter(_.date == date)
               .fold(orElse)(_ => Directive.failure[F, Response[F]](Response[F](Status.NotModified)))
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifUnmodifiedSince(lm: LocalDateTime, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifUnmodifiedSinceF(lm, M.pure(orElse))
  }

  def ifUnmodifiedSinceF(lm: LocalDateTime, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    ifUnmodifiedSinceDir(lm, Directive.successF(orElse))
  }

  def ifUnmodifiedSinceDir(lm: LocalDateTime, orElse: ResponseDirective)(implicit M: Monad[F]): ResponseDirective = {
    val date = HttpDate.unsafeFromInstant(lm.toInstant(ZoneOffset.UTC))
    for {
      mod <- request.header(`If-Unmodified-Since`)
      res <- mod
               .filter(_.date == date)
               .fold(Directive.failure[F, Response[F]](Response[F](Status.NotModified)))(_ => orElse)
    } yield res.putHeaders(`Last-Modified`(date))
  }

  def ifNoneMatch(tag: ETag.EntityTag, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifNoneMatchF(tag, Monad[F].pure(orElse))
  }

  def ifNoneMatchF(tag: ETag.EntityTag, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    ifNoneMatchDir(tag, Directive.successF(orElse))
  }

  def ifNoneMatchDir(tag: ETag.EntityTag, orElse: ResponseDirective)(implicit M: Monad[F]): ResponseDirective = {
    for {
      mod <- request.header(`If-None-Match`)
      res <- mod
               .filter(_.tags.exists(t => t.exists(_ == tag)))
               .fold(orElse)(_ => Directive.failure(Response[F](Status.NotModified)))
    } yield res.putHeaders(ETag(tag))
  }

  def ifMatch(tag: ETag.EntityTag, orElse: => Response[F])(implicit M: Monad[F]): ResponseDirective = {
    ifMatchF(tag, M.pure(orElse))
  }

  def ifMatchF(tag: ETag.EntityTag, orElse: F[Response[F]])(implicit M: Monad[F]): ResponseDirective = {
    ifMatchDir(tag, Directive.successF[F, Response[F]](orElse))
  }

  def ifMatchDir(tag: ETag.EntityTag, orElse: ResponseDirective)(implicit M: Monad[F]): ResponseDirective = {
    for {
      mod <- request.header(`If-Match`)
      res <- mod
               .filter(_.tags.exists(t => t.exists(_ == tag)))
               .fold(Directive.failure[F, Response[F]](Response[F](Status.NotModified)))(_ => orElse)
    } yield res.putHeaders(ETag(tag))
  }
}
