package net.hamnaberg.http4s.directives

import java.time.temporal.ChronoField

import org.http4s._
import org.http4s.dsl.MethodConcat
import org.http4s.util.NonEmptyList

import scalaz.{Equal, Monad}
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scala.language.{higherKinds, implicitConversions}

object Directives {
  def apply[F[+_]](implicit M: Monad[F]): Directives[F] = new Directives[F]{
    implicit val F: Monad[F] = M
  }

  //def task: Directives[Task] = apply[Task]
}

trait Directives[F[+_]] {
  import net.hamnaberg.http4s.directives
  import directives.Directive.Filter

  implicit val F: Monad[F]

  type Directive[+L, +R] = directives.Directive[F, L, R]

  object Directive {
    def apply[L, R](run: Request => F[Result[L, R]]): Directive[L, R] = directives.Directive[F, L, R](run)
  }

  def result[L, R](result: Result[L, R]) = directives.Directive.result[F, L, R](result)
  def success[R](success: R) = directives.Directive.success[F, R](success)
  def failure[L](failure: L) = directives.Directive.failure[F, L](failure)
  def error[L](error: L)     = directives.Directive.error[F, L](error)

  def getOrElseF[L, R](opt: F[Option[R]], orElse: => L) = directives.Directive[F, L, R] { _ =>
    opt.map(_.cata(Result.Success(_), Result.Failure(orElse)))
  }

  def getOrElse[A, L](opt:Option[A], orElse: => L) = opt.cata(success, failure(orElse))

  val commit = directives.Directive.commit

  def value[L, R](f: F[Result[L, R]]) = Directive[L, R](_ => f)

  implicit def DirectiveMonad[L] = directives.Directive.monad[F, L]

  def request: Directive[Nothing, Request] = Directive[Nothing, Request](req => F.point(Result.Success(req)))

  object ops {

    case class when[R](f: PartialFunction[Request, R]){
      def orElse[L](fail: => L) =
        request.flatMap(r => f.lift(r).cata(success, failure(fail)))
    }

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Filter(b, () => failure)
    }

    implicit def MethodDirective(M: Method)(implicit eq: Equal[Method]): Directive[Response, Method] = when { case req if eq.equal(M, req.method) => M } orElse Response(Status.MethodNotAllowed)

    implicit def MethodsDirective(M: MethodConcat): Directive[Response, Method] = when { case req if M.methods(req.method) => req.method } orElse Response(Status.MethodNotAllowed)

    implicit def HeadersDirective(headers: Headers.type): Directive[Nothing, Headers] = request.map(_.headers)

    implicit def UriDirective(uri: Uri.type): Directive[Nothing, Uri] = request.map(_.uri)

    implicit def QueryDirective(query: Query.type): Directive[Nothing, Query] = Uri.map(_.query)

    implicit def HeaderDirective[KEY <: HeaderKey](K: KEY): Directive[Nothing, Option[K.HeaderT]] =
      HeadersDirective(Headers).map(_.get(K.name).flatMap(K.unapply(_)))

    implicit class MonadDecorator[+X](f: F[X]) {
      def successValue = Directive[Nothing, X](_ => f.map(Result.Success(_)))
      def failureValue = Directive[X, Nothing](_ => f.map(Result.Failure(_)))
      def errorValue   = Directive[X, Nothing](_ => f.map(Result.Error(_)))
    }

    implicit def responseDirective(rf: F[Response]): Directive[Nothing, Response] = rf.successValue
  }

  object conditionalGET {
    import java.time.Instant
    import headers._
    import ops._

    def ifModifiedSince(lm: Instant, orElse: => F[Response]): Directive[Response, Response] = {
      val date = lm.`with`(ChronoField.MILLI_OF_SECOND, 0L)
      for {
        mod <- `If-Modified-Since`
        res <- mod.filter(_.date == date).map(_ => F.point(Response(Status.NotModified))).getOrElse(orElse)
      } yield res
    }

    /*def ifUnmodifiedSince(lm: Instant, orElse: => F[Response]): Directive[Response, Response] = {
      val date = lm.`with`(ChronoField.MILLI_OF_SECOND, 0L)
      for {
        mod <- `If-Unmodified-Since`
        res <- mod.filter(_.date == date).map(_ => orElse).getOrElse(F.point(Response(Status.NotModified)))
      } yield res
    }*/

    def ifNoneMatch(toMatch: Option[NonEmptyList[ETag.EntityTag]], orElse: => F[Response]): Directive[Response, Response] = {
      for {
        mod <- `If-None-Match`
        res <- mod.filter(_.tags == toMatch).map(_ => F.point(Response(Status.NotModified))).getOrElse(orElse)
      } yield res
    }

    /*def ifMatch(toMatch: Option[NonEmptyList[ETag.EntityTag]], orElse: => F[Response]): Directive[Response, Response] = {
      for {
        mod <- `If-Match`
        res <- mod.filter(_.tags == toMatch).map(_ => orElse).getOrElse(F.point(Response(Status.NotModified)))
      } yield res
    }*/

  }
}
