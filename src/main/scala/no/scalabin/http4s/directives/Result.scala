package no.scalabin.http4s.directives

import cats.{Applicative, Eval, Monad, Traverse}

import scala.language.higherKinds

object Result {

  def merge[A](result: Result[A, A]): A = result match {
    case Success(value) => value
    case Failure(value) => value
    case Error(value)   => value
  }

  def success[A](a: A): Result[Nothing, A] = Success(a)
  def failure[A](a: A): Result[A, Nothing] = Failure(a)
  def error[A](a: A): Result[A, Nothing]   = Error(a)

  case class Success[+A](value: A) extends Result[Nothing, A]

  case class Failure[+A](value: A) extends Result[A, Nothing]

  case class Error[+A](value: A) extends Result[A, Nothing]

  implicit def monad[L] = new Monad[({ type X[A] = Result[L, A] })#X] {

    override def pure[A](x: A) = Success(x)

    override def flatMap[A, B](fa: Result[L, A])(f: (A) => Result[L, B]) = fa flatMap f

    override def tailRecM[A, B](a: A)(f: (A) => Result[L, Either[A, B]]) = {
      f(a) match {
        case Success(Left(v))  => tailRecM(v)(f)
        case Success(Right(v)) => Success(v)
        case Failure(fail)     => Failure(fail)
        case Error(err)        => Error(err)
      }
    }
  }

  implicit def traverse[L] = new Traverse[({ type X[A] = Result[L, A] })#X] {
    override def traverse[G[_], A, B](fa: Result[L, A])(f: (A) => G[B])(implicit G: Applicative[G]) =
      fa match {
        case Result.Success(value) => G.map(f(value))(Result.Success(_))
        case Result.Failure(value) => G.pure(Result.Failure(value))
        case Result.Error(value)   => G.pure(Result.Error(value))
      }

    override def foldLeft[A, B](fa: Result[L, A], b: B)(f: (B, A) => B) = fa match {
      case Result.Success(value) => f(b, value)
      case _                     => b
    }

    override def foldRight[A, B](fa: Result[L, A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]) = fa match {
      case Result.Success(value) => f(value, lb)
      case _                     => lb
    }
  }
}

sealed trait Result[+L, +R] extends Product with Serializable {
  def flatMap[LL >: L, B](f: R => Result[LL, B]): Result[LL, B] = this match {
    case Result.Success(value) => f(value)
    case Result.Failure(value) => Result.Failure(value)
    case Result.Error(value)   => Result.Error(value)
  }

  def orElse[LL >: L, RR >: R](next: Result[LL, RR]): Result[LL, RR] = this match {
    case Result.Success(value) => Result.Success(value)
    case Result.Failure(_)     => next
    case Result.Error(value)   => Result.Error(value)
  }

  def map[B](f: R => B) = flatMap(r => Result.Success(f(r)))
}
