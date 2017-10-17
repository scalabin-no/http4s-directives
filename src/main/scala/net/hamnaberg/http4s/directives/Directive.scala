package net.hamnaberg.http4s.directives

import cats.{Eq, Monad}
import org.http4s.dsl.MethodConcat
import org.http4s._

import fs2.Task

case class Directive[+L, +R](run: Request => Task[Result[L, R]]){
  def flatMap[LL >: L, B](f: R => Directive[LL, B]) =
    Directive[LL, B](req => run(req).flatMap{
      case Result.Success(value) => f(value).run(req)
      case Result.Failure(value) => Task.delay(Result.Failure(value))
      case Result.Error(value)   => Task.delay(Result.Error(value))
    })

  def map[B](f: R => B) = Directive[L, B](req => run(req).map(_.map(f)))

  def filter[LL >: L](f: R => Directive.Filter[LL]): Directive[LL, R] =
    flatMap{ r =>
      val result = f(r)
      if(result.result)
        Directive.success[R](r)
      else
        Directive.failure[LL](result.failure())
    }

  def withFilter[LL >: L](f: R => Directive.Filter[LL]) = filter(f)

  def orElse[LL >: L, RR >: R](next: Directive[LL, RR]) =
    Directive[LL, RR](req => run(req).flatMap{
      case Result.Success(value) => Task.delay(Result.Success(value))
      case Result.Failure(_)     => next.run(req)
      case Result.Error(value)   => Task.delay(Result.Error(value))
    })

  def | [LL >: L, RR >: R](next: Directive[LL, RR]) = orElse(next)
}

object Directive {

  implicit def monad[L] = new Monad[({type X[A] = Directive[L, A]})#X]{
    override def flatMap[A, B](fa: Directive[L, A])(f: (A) => Directive[L, B]) = fa flatMap f
    override def pure[A](a: A) = Directive[L, A](_ => Task.delay(Result.Success(a)))

    override def tailRecM[A, B](a: A)(f: (A) => Directive[L, Either[A, B]]) =
      tailRecM(a)(a0 => Directive(f(a0).run))
  }

  def pure[A](a: => A): Directive[Nothing, A] = monad[Nothing].pure(a)

  def result[L, R](result: => Result[L, R]) = Directive[L, R](_ => Task.delay(result))

  def success[R](success: => R): Directive[Nothing, R] = result[Nothing, R](Result.Success(success))
  def failure[L](failure: => L): Directive[L, Nothing] = result[L, Nothing](Result.Failure(failure))
  def error[L](error: => L): Directive[L, Nothing] = result[L, Nothing](Result.Error(error))

  object commit {
    def flatMap[R, A](f:Unit => Directive[R, A]): Directive[R, A] =
      commit(f(()))

    def apply[R, A](d: Directive[R, A]) = Directive[R, A]{ r => d.run(r).map{
      case Result.Failure(response) => Result.Error[R](response)
      case result                   => result
    }}
  }

  def getOrElseF[L, R](task: Task[Option[R]], orElse: => L): Directive[L, R] = Directive[L, R] { _ =>
    task.map(_.fold[Result[L, R]](Result.Failure(orElse))(Result.Success(_)))
  }

  def getOrElse[A, L](opt:Option[A], orElse: => L): Directive[L, A] = opt.fold[Directive[L, A]](failure(orElse))(success(_))


  case class Filter[+L](result:Boolean, failure: () => L)

  object ops {
    import scala.language.implicitConversions

    case class when[R](f: PartialFunction[Request, R]){
      def orElse[L](fail: => L): Directive[L, R] =
        request.apply.flatMap(r => f.lift(r).fold[Directive[L, R]](failure(fail))(success(_)))
    }

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Directive.Filter(b, () => failure)
    }

    implicit def MethodDirective(M: Method)(implicit eq: Eq[Method]): Directive[Response, Method] = when { case req if eq.eqv(M, req.method) => M } orElse Response(Status.MethodNotAllowed)

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

object request {
  def apply: Directive[Nothing, Request] = Directive[Nothing, Request](req => Task.now(Result.Success(req)))
  def headers: Directive[Nothing, Headers] = apply.map(_.headers)
  def header(key: HeaderKey): Directive[Nothing, Option[Header]] = headers.map(_.get(key.name))

  def uri: Directive[Nothing, Uri] = apply.map(_.uri)
  def path: Directive[Nothing, Uri.Path] = uri.map(_.path)
  def query: Directive[Nothing, Query] = uri.map(_.query)

  def bodyAs[A : EntityDecoder]: Directive[Nothing, A] = Directive(req => req.as[A].map(Result.Success(_)))
}

