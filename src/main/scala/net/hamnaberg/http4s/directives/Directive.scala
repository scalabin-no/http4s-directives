package net.hamnaberg.http4s.directives

import org.http4s.Request

import scalaz._
import syntax.monad._
import scala.language.higherKinds

case class Directive[F[+_], +L, +R](run: Request => F[Result[L, R]]){
  def flatMap[LL >: L, B](f: R => Directive[F, LL, B])(implicit F:Monad[F]) =
    Directive[F, LL, B](req => run(req).flatMap{
      case Result.Success(value) => f(value).run(req)
      case Result.Failure(value) => F.point(Result.Failure(value))
      case Result.Error(value)   => F.point(Result.Error(value))
    })

  def map[B](f: R => B)(implicit F: Functor[F]) = Directive[F, L, B](req => run(req).map(_.map(f)))

  def filter[LL >: L](f: R => Directive.Filter[LL])(implicit F: Monad[F]): Directive[F, LL, R] =
    flatMap{ r =>
      val result = f(r)
      if(result.result)
        Directive.success[F, R](r)
      else
        Directive.failure[F, LL](result.failure())
    }

  def withFilter[LL >: L](f: R => Directive.Filter[LL])(implicit F: Monad[F]) =
    filter(f)

  def orElse[LL >: L, RR >: R](next: Directive[F, LL, RR])(implicit F: Monad[F]) =
    Directive[F, LL, RR](req => run(req).flatMap{
      case Result.Success(value) => F.point(Result.Success(value))
      case Result.Failure(_)     => next.run(req)
      case Result.Error(value)   => F.point(Result.Error(value))
    })

  def | [LL >: L, RR >: R](next: Directive[F, LL, RR])(implicit F: Monad[F]) = orElse(next)
}

object Directive {

  implicit def monad[F[+_] : Monad, L] = new Monad[({type X[A] = Directive[F, L, A]})#X]{
    def bind[A, B](fa: Directive[F, L, A])(f: (A) => Directive[F, L, B]) = fa flatMap f
    def point[A](a: => A) = Directive[F, L, A](_ => Monad[F].point(Result.Success(a)))
  }

  def point[F[+_] : Monad, A](a: => A) = monad[F, Nothing].point(a)

  def result[F[+_] : Monad, L, R](result: => Result[L, R]) = Directive[F, L, R](_ => Monad[F].point(result))

  def success[F[+_] : Monad, R](success: => R) = result[F, Nothing, R](Result.Success(success))
  def failure[F[+_] : Monad, L](failure: => L) = result[F, L, Nothing](Result.Failure(failure))
  def error[F[+_] : Monad, L](error: => L) = result[F, L, Nothing](Result.Error(error))

  object commit {
    def flatMap[F[+_]:Monad, R, A](f:Unit => Directive[F, R, A]): Directive[F, R, A] =
      commit(f(()))

    def apply[F[+_]:Monad, R, A](d: Directive[F, R, A]) = Directive[F, R, A]{ r => d.run(r).map{
      case Result.Failure(response) => Result.Error[R](response)
      case result                   => result
    }}
  }

  case class Filter[+L](result:Boolean, failure: () => L)
}

