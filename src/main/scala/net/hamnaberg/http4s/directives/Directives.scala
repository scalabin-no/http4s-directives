package net.hamnaberg.http4s.directives

import org.http4s.Request

import scalaz.{Equal, Monad}
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scala.language.{higherKinds, implicitConversions}

object Directives {
  def apply[F[+_]](implicit M: Monad[F]): Directives[F] = new Directives[F]{
    implicit val F: Monad[F] = M
  }
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
    import org.http4s._

    case class when[R](f: PartialFunction[Request, R]){
      def orElse[L](fail: => L) =
        request.flatMap(r => f.lift(r).cata(success, failure(fail)))
    }

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Filter(b, () => failure)
    }

    implicit def MethodDirective(M: Method)(implicit eq: Equal[Method]): Directive[Status, Method] = when { case req if eq.equal(M, req.method) => M } orElse Status.MethodNotAllowed

    implicit def HeadersDirective(headers: Headers.type): Directive[Nothing, Headers] = request.map(_.headers)

    implicit def HeaderDirective(K: HeaderKey.Extractable): Directive[Status, Option[HeaderKey.Extractable#HeaderT]] =
      HeadersDirective(Headers).map(_.get(K))

    implicit class MonadDecorator[+X](f: F[X]) {
      def successValue = Directive[Nothing, X](_ => f.map(Result.Success(_)))
      def failureValue = Directive[X, Nothing](_ => f.map(Result.Failure(_)))
      def errorValue   = Directive[X, Nothing](_ => f.map(Result.Error(_)))
    }

    /*implicit def queryParamsDirective[L](t: QueryParams.type): d2.Directive[A, F, L, Map[String, Seq[String]]] = {
      request[A].map{case QueryParams(qp) => qp}
    }*/
  }

  /*object request {
    def apply = Directive[Nothing, Request](req => F.point(Result.Success(req)))
  }*/
}
