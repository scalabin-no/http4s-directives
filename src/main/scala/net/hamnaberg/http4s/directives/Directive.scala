package no.scalabin.http4s.directives

import cats.Monad
import cats.effect.Sync
import org.http4s._
import cats.syntax.flatMap._
import cats.syntax.functor._

import scala.language.higherKinds

case class Directive[F[+_]: Sync, +L, +R](run: Request[F] => F[Result[L, R]]){
  def flatMap[LL >: L, B](f: R => Directive[F, LL, B]) =
    Directive[F, LL, B](req => run(req).flatMap{
      case Result.Success(value) => f(value).run(req)
      case Result.Failure(value) => Sync[F].delay(Result.Failure(value))
      case Result.Error(value)   => Sync[F].delay(Result.Error(value))
    })

  def map[B](f: R => B): Directive[F, L, B] = Directive[F, L, B](req => run(req).map(_.map(f)))

  def filter[LL >: L](f: R => Directive.Filter[LL]): Directive[F, LL, R] =
    flatMap{ r =>
      val result = f(r)
      if(result.result)
        Directive.success[F, R](r)
      else
        Directive.failure[F, LL](result.failure())
    }

  def withFilter[LL >: L](f: R => Directive.Filter[LL]) = filter(f)

  def orElse[LL >: L, RR >: R](next: Directive[F, LL, RR]) =
    Directive[F, LL, RR](req => run(req).flatMap{
      case Result.Success(value) => Sync[F].delay(Result.Success(value))
      case Result.Failure(_)     => next.run(req)
      case Result.Error(value)   => Sync[F].delay(Result.Error(value))
    })

  def | [LL >: L, RR >: R](next: Directive[F, LL, RR]) = orElse(next)
}

object Directive {

  implicit def monad[F[+ _] : Sync, L] = new Monad[({type X[A] = Directive[F, L, A]})#X] {
    override def flatMap[A, B](fa: Directive[F, L, A])(f: (A) => Directive[F, L, B]) = fa flatMap f

    override def pure[A](a: A) = Directive[F, L, A](_ => Sync[F].delay(Result.Success(a)))

    override def tailRecM[A, B](a: A)(f: (A) => Directive[F, L, Either[A, B]]) =
      tailRecM(a)(a0 => Directive(f(a0).run))
  }

  def pure[F[+ _] : Sync, A](a: => A): Directive[F, Nothing, A] = monad[F, Nothing].pure(a)

  def result[F[+ _] : Sync, L, R](result: => Result[L, R]) = Directive[F, L, R](_ => Sync[F].delay(result))

  def success[F[+ _] : Sync, R](success: => R): Directive[F, Nothing, R] = result[F, Nothing, R](Result.Success(success))

  def failure[F[+ _] : Sync, L](failure: => L): Directive[F, L, Nothing] = result[F, L, Nothing](Result.Failure(failure))

  def error[F[+ _] : Sync, L](error: => L): Directive[F, L, Nothing] = result[F, L, Nothing](Result.Error(error))


  case class Filter[+L](result: Boolean, failure: () => L)


  object commit {
    def flatMap[F[+ _] : Sync, R, A](f: Unit => Directive[F, R, A]): Directive[F, R, A] =
      commit(f(()))

    def apply[F[+ _] : Sync, R, A](d: Directive[F, R, A]) = Directive[F, R, A] { r =>
      d.run(r).map {
        case Result.Failure(response) => Result.Error[R](response)
        case result => result
      }
    }
  }
}
