package no.scalabin.http4s.directives

import cats.{Applicative, Eval, Monad, Traverse}
import org.http4s.Response

import scala.language.{higherKinds, reflectiveCalls}

object Result {
  def success[F[_], A](a: A): Result[F, A]           = Success(a)
  def failure[F[_], A](a: Response[F]): Result[F, A] = Failure(a)
  def error[F[_], A](a: Response[F]): Result[F, A]   = Error(a)

  case class Success[F[_], A](value: A) extends Result[F, A]

  case class Failure[F[_], A](value: Response[F]) extends Result[F, A]

  case class Error[F[_], A](value: Response[F]) extends Result[F, A]

  implicit def monad[F[_]]: Monad[Result[F, ?]] = new Monad[Result[F, ?]] {

    override def pure[A](x: A): Result[F, A] = Success(x)

    override def flatMap[A, B](fa: Result[F, A])(f: A => Result[F, B]): Result[F, B] = fa flatMap f

    override def tailRecM[A, B](a: A)(f: A => Result[F, Either[A, B]]): Result[F, B] = {
      f(a) match {
        case Success(Left(v))  => tailRecM(v)(f)
        case Success(Right(v)) => Success(v)
        case Failure(fail)     => Failure(fail)
        case Error(err)        => Error(err)
      }
    }
  }

  implicit def traverse[F[_]]: Traverse[Result[F, ?]] = new Traverse[Result[F, ?]] {
    override def traverse[G[_], A, B](fa: Result[F, A])(f: A => G[B])(implicit G: Applicative[G]) =
      fa match {
        case Result.Success(value) => G.map(f(value))(Result.Success(_))
        case Result.Failure(value) => G.pure(Result.Failure(value))
        case Result.Error(value)   => G.pure(Result.Error(value))
      }

    override def foldLeft[A, B](fa: Result[F, A], b: B)(f: (B, A) => B) = fa match {
      case Result.Success(value) => f(b, value)
      case _                     => b
    }

    override def foldRight[A, B](fa: Result[F, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]) = fa match {
      case Result.Success(value) => f(value, lb)
      case _                     => lb
    }
  }
}

sealed trait Result[F[_], A] extends Product with Serializable {
  def flatMap[B](f: A => Result[F, B]): Result[F, B] = this match {
    case Result.Success(value) => f(value)
    case Result.Failure(value) => Result.Failure(value)
    case Result.Error(value)   => Result.Error(value)
  }

  def orElse(next: Result[F, A]): Result[F, A] = this match {
    case Result.Success(value) => Result.Success(value)
    case Result.Failure(_)     => next
    case Result.Error(value)   => Result.Error(value)
  }

  def map[B](f: A => B): Result[F, B] = flatMap(r => Result.Success[F, B](f(r)))

  def response(implicit ev: A =:= Response[F]): Response[F] = this match {
    case Result.Success(response) => response
    case Result.Failure(response) => response
    case Result.Error(response)   => response
  }
}
