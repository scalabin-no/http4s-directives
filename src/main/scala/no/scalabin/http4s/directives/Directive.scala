package no.scalabin.http4s.directives

import cats.data.OptionT
import cats.Monad
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

import scala.language.{higherKinds, reflectiveCalls}

case class Directive[F[_]: Monad, A](run: Request[F] => F[Result[F, A]]) {
  def flatMap[B](f: A => Directive[F, B]): Directive[F, B] =
    Directive[F, B](req =>
      run(req).flatMap {
        case Result.Success(value) => f(value).run(req)
        case Result.Failure(value) => Monad[F].pure(Result.failure(value))
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def map[B](f: A => B): Directive[F, B] = Directive[F, B](req => run(req).map(_.map(f)))

  def filter(f: A => Directive.Filter[F]): Directive[F, A] =
    flatMap { r =>
      val result = f(r)
      if (result.result)
        Directive.success[F, A](r)
      else
        Directive.failureF[F, A](result.failure)
    }

  def withFilter(f: A => Directive.Filter[F]): Directive[F, A] = filter(f)

  def orElse(next: Directive[F, A]): Directive[F, A] =
    Directive[F, A](req =>
      run(req).flatMap {
        case Result.Success(value) => Monad[F].pure(Result.success(value))
        case Result.Failure(_)     => next.run(req)
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def |(next: Directive[F, A]): Directive[F, A] = orElse(next)

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

  def result[F[_]: Monad, A](result: => Result[F, A]): Directive[F, A] = Directive[F, A](_ => Monad[F].pure(result))

  def success[F[_]: Monad, A](success: => A): Directive[F, A] = pure(success)

  def failure[F[_]: Monad, A](failure: => Response[F]): Directive[F, A] = result[F, A](Result.failure[F, A](failure))

  def error[F[_]: Monad, A](error: => Response[F]): Directive[F, A] = result[F, A](Result.error[F, A](error))

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

  def getOrElseF[F[_]: Monad, A](opt: F[Option[A]], orElse: => F[Response[F]]): Directive[F, A] =
    Directive(_ => OptionT(opt).cata(orElse.map(Result.failure[F, A]), v => Monad[F].pure(Result.success[F, A](v))).flatten)

  def getOrElse[F[_]: Monad, A](opt: Option[A], orElse: => F[Response[F]]): Directive[F, A] = opt match {
    case Some(r) => success(r)
    case None    => failureF(orElse)
  }
}
