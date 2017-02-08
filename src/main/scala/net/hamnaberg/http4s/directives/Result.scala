package net.hamnaberg.http4s.directives

import scalaz.{Monad, Applicative, Traverse}

import scala.language.higherKinds

object Result {

  def merge[A](result:Result[A, A]) = result match {
    case Success(value) => value
    case Failure(value) => value
    case Error(value) => value

  }

  case class Success[+A](value:A) extends Result[Nothing, A]
  case class Failure[+A](value:A) extends Result[A, Nothing]
  case class Error[+A](value:A) extends Result[A, Nothing]

  implicit def monad[L] = new Monad[({type X[A] = Result[L, A]})#X]{
    def bind[A, B](fa: Result[L, A])(f: (A) => Result[L, B]) = fa flatMap f
    def point[A](a: => A) = Success(a)
  }

  implicit def traverse[L] = new Traverse[({type X[A] = Result[L, A]})#X]{
    def traverseImpl[G[_], A, B](fa: Result[L, A])(f: (A) => G[B])(implicit G: Applicative[G]) =
      fa match {
        case Result.Success(value) => G.map(f(value))(Result.Success(_))
        case Result.Failure(value) => G.point(Result.Failure(value))
        case Result.Error(value)   => G.point(Result.Error(value))
      }
  }
}

sealed trait Result[+L, +R] {
  def flatMap[LL >: L, B](f:R => Result[LL, B]):Result[LL, B] = this match {
    case Result.Success(value) => f(value)
    case Result.Failure(value) => Result.Failure(value)
    case Result.Error(value)   => Result.Error(value)
  }

  def orElse[LL >: L, RR >: R](next:Result[LL, RR]):Result[LL, RR] = this match {
    case Result.Success(value) => Result.Success(value)
    case Result.Failure(_)     => next
    case Result.Error(value)   => Result.Error(value)
  }

  def map[B](f:R => B) = flatMap(r => Result.Success(f(r)))
}
