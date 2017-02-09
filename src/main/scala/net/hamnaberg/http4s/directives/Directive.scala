package net.hamnaberg.http4s.directives

import org.http4s.Request

import scala.language.reflectiveCalls

import scalaz._
import scalaz.concurrent.Task

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
    def bind[A, B](fa: Directive[L, A])(f: (A) => Directive[L, B]) = fa flatMap f
    def point[A](a: => A) = Directive[L, A](_ => Task.delay(Result.Success(a)))
  }

  def point[A](a: => A) = monad[Nothing].point(a)

  def result[L, R](result: => Result[L, R]) = Directive[L, R](_ => Task.delay(result))

  def success[R](success: => R) = result[Nothing, R](Result.Success(success))
  def failure[L](failure: => L) = result[L, Nothing](Result.Failure(failure))
  def error[L](error: => L) = result[L, Nothing](Result.Error(error))

  object commit {
    def flatMap[R, A](f:Unit => Directive[R, A]): Directive[R, A] =
      commit(f(()))

    def apply[R, A](d: Directive[R, A]) = Directive[R, A]{ r => d.run(r).map{
      case Result.Failure(response) => Result.Error[R](response)
      case result                   => result
    }}
  }

  case class Filter[+L](result:Boolean, failure: () => L)
}

