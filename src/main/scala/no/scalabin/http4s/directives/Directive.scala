package no.scalabin.http4s.directives

import cats.Monad
import cats.data.OptionT
import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.http4s._

case class Directive[F[_]: Monad, A, R](run: A => F[Result[F, R]]) {
  def flatMap[B](f: R => Directive[F, A, B]): Directive[F, A, B] =
    Directive[F, A, B](req =>
      run(req).flatMap {
        case Result.Success(value) => f(value).run(req)
        case Result.Failure(value) => Monad[F].pure(Result.failure(value))
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def map[B](f: R => B): Directive[F, A, B] = Directive[F, A, B](req => run(req).map(_.map(f)))

  def filter(f: R => Directive.Filter[F, A]): Directive[F, A, R] =
    flatMap { r =>
      val result = f(r)
      if (result.result)
        Directive.success[F, A, R](r)
      else
        result.failure.flatMap(r => Directive.failure[F, A, R](r))
    }

  def withFilter(f: R => Directive.Filter[F, A]): Directive[F, A, R] = filter(f)

  def orElse(next: Directive[F, A, R]): Directive[F, A, R] =
    Directive[F, A, R](req =>
      run(req).flatMap {
        case Result.Success(value) => Monad[F].pure(Result.success(value))
        case Result.Failure(_)     => next.run(req)
        case Result.Error(value)   => Monad[F].pure(Result.error(value))
    })

  def |(next: Directive[F, A, R]): Directive[F, A, R] = orElse(next)

  def semiFlatMap[B](f: R => F[B]): Directive[F, A, B] = flatMap[B](a => Directive.liftF(f(a)))

  def toHttpRoutes(implicit evA: Request[F] =:= A, evR: R =:= Response[F], S: Sync[F]): HttpRoutes[F] =
    HttpRoutes(req => OptionT.liftF(run(evA(req)).map(_.response)))
}

object Directive {

  implicit def monad[F[_]: Monad, A]: Monad[Directive[F, A, *]] =
    new Monad[Directive[F, A, *]] {
      override def flatMap[R, B](fa: Directive[F, A, R])(f: R => Directive[F, A, B]): Directive[F, A, B] = fa flatMap f
      override def pure[R](a: R): Directive[F, A, R] = Directive[F, A, R](_ => Monad[F].pure(Result.success(a)))
      override def tailRecM[R, B](a: R)(f: R => Directive[F, A, Either[R, B]]): Directive[F, A, B] =
        Directive(req => f(a).run(req).map(r => Result.monad[F].tailRecM(a)(_ => r)))
    }

  def request[F[_]: Monad]: RequestDirective[F, Request[F]] = Directive(req => Monad[F].pure(Result.success(req)))

  def pure[F[_]: Monad, A, R](a: => R): Directive[F, A, R] = monad[F, A].pure(a)

  def result[F[_]: Monad, A, R](result: => Result[F, R]): Directive[F, A, R] = resultF(Monad[F].pure(result))

  def resultF[F[_]: Monad, A, R](result: F[Result[F, R]]): Directive[F, A, R] = Directive[F, A, R](_ => result)

  def success[F[_]: Monad, A, R](success: => R): Directive[F, A, R] = pure(success)

  def failure[F[_]: Monad, A, R](failure: => Response[F]): Directive[F, A, R] = result[F, A, R](Result.failure[F, R](failure))

  def error[F[_]: Monad, A, R](error: => Response[F]): Directive[F, A, R] = result[F, A, R](Result.error[F, R](error))

  def liftF[F[_]: Monad, A, R](f: F[R]): Directive[F, A, R] = Directive[F, A, R](_ => f.map(Result.Success(_)))

  def successF[F[_]: Monad, A, R](f: F[R]): Directive[F, A, R] = liftF(f)

  def failureF[F[_]: Monad, A, R](f: F[Response[F]]): Directive[F, A, R] = Directive[F, A, R](_ => f.map(Result.Failure[F, R]))

  def errorF[F[_]: Monad, A, R](f: F[Response[F]]): Directive[F, A, R] = Directive[F, A, R](_ => f.map(Result.Error[F, R]))

  case class Filter[F[_], A](result: Boolean, failure: Directive[F, A, Response[F]])

  object commit {
    def flatMap[F[_]: Monad, A, R](f: Unit => Directive[F, A, R]): Directive[F, A, R] =
      commit(f(()))

    def apply[F[_]: Monad, A, R](d: Directive[F, A, R]): Directive[F, A, R] = Directive[F, A, R] { r =>
      d.run(r).map {
        case Result.Failure(response) => Result.error[F, R](response)
        case result                   => result
      }
    }
  }

  def getOrElseF[F[_]: Monad, A, R](opt: F[Option[R]], orElse: Directive[F, A, Response[F]]): Directive[F, A, R] =
    Directive(
      req =>
        OptionT(opt)
          .cata(orElse.flatMap(Directive.failure[F, A, R](_)).run(req), v => Monad[F].pure(Result.success[F, R](v)))
          .flatten)

  def getOrElseF[F[_]: Monad, A, R](opt: F[Option[R]], orElse: F[Response[F]]): Directive[F, A, R] =
    Directive(_ => OptionT(opt).cata(orElse.map(Result.failure[F, R]), v => Monad[F].pure(Result.success[F, R](v))).flatten)

  def getOrElseF[F[_]: Monad, A, R](opt: Option[R], orElse: F[Response[F]]): Directive[F, A, R] = opt match {
    case Some(r) => success(r)
    case None    => failureF(orElse)
  }

  def getOrElse[F[_]: Monad, A, R](opt: Option[R], orElse: Directive[F, A, Response[F]]): Directive[F, A, R] = opt match {
    case Some(r) => success(r)
    case None    => orElse.flatMap(res => Directive.failure(res))
  }

  def getOrElse[F[_]: Monad, A, R](opt: Option[R], orElse: => Response[F]): Directive[F, A, R] = opt match {
    case Some(r) => success(r)
    case None    => failure(orElse)
  }
}
