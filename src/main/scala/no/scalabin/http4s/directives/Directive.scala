package no.scalabin.http4s.directives

import cats.data.OptionT
import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

import scala.language.{higherKinds, reflectiveCalls}

case class Directive[F[_]: Monad, R](run: Request[F] => F[Result[F, R]]) {
  def flatMap[B](f: R => Directive[F, B]): Directive[F, B] =
    Directive[F, B](req =>
      run(req).flatMap {
        case Result.Success(value) => f(value).run(req)
        case Result.Failure(value) => Monad[F].pure(Result.failure(value))
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def map[B](f: R => B): Directive[F, B] = Directive[F, B](req => run(req).map(_.map(f)))

  def filter(f: R => Directive.Filter[F]): Directive[F, R] =
    flatMap { r =>
      val result = f(r)
      if (result.result)
        Directive.success[F, R](r)
      else
        Directive.failureF[F, R](result.failure)
    }

  def withFilter(f: R => Directive.Filter[F]): Directive[F, R] = filter(f)

  def orElse(next: Directive[F, R]): Directive[F, R] =
    Directive[F, R](req =>
      run(req).flatMap {
        case Result.Success(value) => Monad[F].pure(Result.success(value))
        case Result.Failure(_)     => next.run(req)
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def |(next: Directive[F, R]): Directive[F, R] = orElse(next)

}

object Directive {

  implicit def monad[F[_]: Monad]: Monad[({ type X[A] = Directive[F, A] })#X] =
    new Monad[({ type X[A] = Directive[F, A] })#X] {
      override def flatMap[A, B](fa: Directive[F, A])(f: A => Directive[F, B]): Directive[F, B] = fa flatMap f

      override def pure[A](a: A): Directive[F, A] = Directive[F, A](_ => Monad[F].pure(Result.success(a)))

      override def tailRecM[A, B](a: A)(f: A => Directive[F, Either[A, B]]): Directive[F, B] =
        tailRecM(a)(a0 => Directive(f(a0).run))
    }

  def request[F[_]: Monad]: Directive[F, Request[F]] = Directive(req => Monad[F].pure(Result.success(req)))

  def pure[F[_]: Monad, A](a: => A): Directive[F, A] = monad[F].pure(a)

  def result[F[_]: Monad, R](result: => Result[F, R]): Directive[F, R] = Directive[F, R](_ => Monad[F].pure(result))

  def success[F[_]: Monad, R](success: => R): Directive[F, R] = pure(success)

  def failure[F[_]: Monad, R](failure: => Response[F]): Directive[F, R] = result[F, R](Result.failure[F, R](failure))

  def error[F[_]: Monad, R](error: => Response[F]): Directive[F, R] = result[F, R](Result.error[F, R](error))

  def liftF[F[_]: Monad, X](f: F[X]): Directive[F, X] = Directive[F, X](_ => f.map(Result.Success(_)))

  def successF[F[_]: Monad, X](f: F[X]): Directive[F, X] = liftF(f)

  def failureF[F[_]: Monad, X](f: F[Response[F]]): Directive[F, X] = Directive[F, X](_ => f.map(Result.Failure[F, X]))

  def errorF[F[_]: Monad, X](f: F[Response[F]]): Directive[F, X] = Directive[F, X](_ => f.map(Result.Error[F, X]))

  case class Filter[F[_]](result: Boolean, failure: F[Response[F]])

  object commit {
    def flatMap[F[_]: Monad, A](f: Unit => Directive[F, A]): Directive[F, A] =
      commit(f(()))

    def apply[F[_]: Monad, A](d: Directive[F, A]): Directive[F, A] = Directive[F, A] { r =>
      d.run(r).map {
        case Result.Failure(response) => Result.error[F, A](response)
        case result                   => result
      }
    }
  }

  def getOrElseF[F[_]: Monad, R](opt: F[Option[R]], orElse: => F[Response[F]]): Directive[F, R] =
    Directive(_ => OptionT(opt).cata(orElse.map(Result.failure[F, R]), v => Monad[F].pure(Result.success[F, R](v))).flatten)

  def getOrElse[F[_]: Monad, A](opt: Option[A], orElse: => F[Response[F]]): Directive[F, A] = opt match {
    case Some(r) => success(r)
    case None    => failureF(orElse)
  }
}
