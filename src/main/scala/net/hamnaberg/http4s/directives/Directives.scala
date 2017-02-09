package net.hamnaberg.http4s.directives

import org.http4s._
import org.http4s.dsl.MethodConcat

import scala.language.implicitConversions
import scalaz.Equal
import scalaz.concurrent.Task
import scalaz.syntax.std.option._

object Directives extends Directives

trait Directives {

  def result[L, R](result: Result[L, R]) = directives.Directive.result[L, R](result)
  def success[R](success: R) = directives.Directive.success[R](success)
  def failure[L](failure: L) = directives.Directive.failure[L](failure)
  def error[L](error: L)     = directives.Directive.error[L](error)

  def getOrElseF[L, R](opt: Task[Option[R]], orElse: => L) = Directive[L, R] { _ =>
    opt.map(_.cata(Result.Success(_), Result.Failure(orElse)))
  }

  def getOrElse[A, L](opt:Option[A], orElse: => L) = opt.cata(success, failure(orElse))

  val commit = directives.Directive.commit

  def value[L, R](f: Task[Result[L, R]]) = Directive[L, R](_ => f)

  object request {
    def apply: Directive[Nothing, Request] = Directive[Nothing, Request](req => Task.now(Result.Success(req)))
    def headers: Directive[Nothing, Headers] = apply.map(_.headers)
    def header(key: HeaderKey): Directive[Nothing, Option[Header]] = headers.map(_.get(key.name))

    def uri: Directive[Nothing, Uri] = apply.map(_.uri)
    def path: Directive[Nothing, Uri.Path] = uri.map(_.path)
    def query: Directive[Nothing, Query] = uri.map(_.query)

    def bodyAs[A : EntityDecoder]: Directive[Nothing, A] = Directive(req => req.as[A].map(Result.Success(_)))
  }

  object ops {

    case class when[R](f: PartialFunction[Request, R]){
      def orElse[L](fail: => L) =
        request.apply.flatMap(r => f.lift(r).cata(success, failure(fail)))
    }

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Directive.Filter(b, () => failure)
    }

    implicit def MethodDirective(M: Method)(implicit eq: Equal[Method]): Directive[Response, Method] = when { case req if eq.equal(M, req.method) => M } orElse Response(Status.MethodNotAllowed)

    implicit def MethodsDirective(M: MethodConcat): Directive[Response, Method] = when { case req if M.methods(req.method) => req.method } orElse Response(Status.MethodNotAllowed)

    implicit def HeaderDirective[KEY <: HeaderKey](K: KEY): Directive[Nothing, Option[K.HeaderT]] =
      request.headers.map(_.get(K.name).flatMap(K.unapply(_)))

    implicit class MonadDecorator[+X](f: Task[X]) {
      def successValue = Directive[Nothing, X](_ => f.map(Result.Success(_)))
      def failureValue = Directive[X, Nothing](_ => f.map(Result.Failure(_)))
      def errorValue   = Directive[X, Nothing](_ => f.map(Result.Error(_)))
    }

    implicit def responseDirective(rf: Task[Response]): Directive[Nothing, Response] = rf.successValue
  }
}
