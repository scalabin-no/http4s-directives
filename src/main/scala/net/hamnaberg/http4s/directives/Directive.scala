package net.hamnaberg.http4s.directives

//import unfiltered.request.HttpRequest
import org.http4s.Request
import scalaz._
import syntax.monad._
import syntax.std.option._

import scala.language.{higherKinds, implicitConversions, reflectiveCalls}

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

case class Directive[F[+_], +L, +R](run: Request => F[Result[L, R]]){
  def flatMap[LL >: L, B](f:R => Directive[F, LL, B])(implicit F:Monad[F]) =
    Directive[F, LL, B](req => run(req).flatMap{
      case Result.Success(value) => f(value).run(req)
      case Result.Failure(value) => F.point(Result.Failure(value))
      case Result.Error(value)   => F.point(Result.Error(value))
    })

  def map[B](f:R => B)(implicit F:Functor[F]) = Directive[F, L, B](req => run(req).map(_.map(f)))

  def filter[LL >: L](f:R => Directive.Filter[LL])(implicit F: Monad[F]): Directive[F, LL, R] =
    flatMap{ r =>
      val result = f(r)
      if(result.result)
        Directive.success[F, R](r)
      else
        Directive.failure[F, LL](result.failure())
    }

  def withFilter[LL >: L](f:R => Directive.Filter[LL])(implicit F:Monad[F]) =
    filter(f)

  def orElse[LL >: L, RR >: R](next:Directive[F, LL, RR])(implicit F:Monad[F]) =
    Directive[F, LL, RR](req => run(req).flatMap{
      case Result.Success(value) => F.point(Result.Success(value))
      case Result.Failure(_)     => next.run(req)
      case Result.Error(value)   => F.point(Result.Error(value))
    })

  def | [LL >: L, RR >: R](next: Directive[F, LL, RR])(implicit F:Monad[F]) = orElse(next)
}

/*object Directives {
  def apply[F[+_]](implicit M: Monad[F]): Directives[F] = new Directives[F]{
    implicit val F: Monad[F] = M
  }
}*/

/*trait Directives[F[+_]] {
  implicit val F:Monad[F]

  type Result[+L, +R] = d2.Result[L, R]
  val Result          = d2.Result

  type Directive[-T, +L, +R] = d2.Directive[T, F, L, R]

  object Directive {
    def apply[T, L, R](run:HttpRequest[T] => F[Result[L, R]]):Directive[T, L, R] = d2.Directive[T, F, L, R](run)
  }

  def result[L, R](result: Result[L, R]) = d2.Directive.result[F, L, R](result)
  def success[R](success: R) = d2.Directive.success[F, R](success)
  def failure[L](failure: L) = d2.Directive.failure[F, L](failure)
  def error[L](error: L)     = d2.Directive.error[F, L](error)

  def getOrElseF[L, R](opt:F[Option[R]], orElse: => L) = d2.Directive[Any, F, L, R] { _ =>
    opt.map(_.cata(Result.Success(_), Result.Failure(orElse)))
  }

  def getOrElse[A, L](opt:Option[A], orElse: => L) = opt.cata(success, failure(orElse))

  type Filter[+L] = d2.Directive.Filter[L]
  val Filter      = d2.Directive.Filter

  val commit = d2.Directive.commit

  def value[L, R](f: F[Result[L, R]]) = Directive[Any, L, R](_ => f)

  implicit def DirectiveMonad[T, L] = d2.Directive.monad[T, F, L]


  case class when[R](f:PartialFunction[HttpRequest[Any], R]){
    def orElse[L](fail: => L) =
      request[Any].flatMap(r => f.lift(r).cata(success, failure(fail)))
  }

  object ops {
    import unfiltered.request._

    implicit class FilterSyntax(b:Boolean) {
      def | [L](failure: => L) = Filter(b, () => failure)
    }

    implicit def MethodDirective(M:unfiltered.request.Method) = when{ case M(_) => M } orElse unfiltered.response.MethodNotAllowed

    implicit class MonadDecorator[+X](f: F[X]) {
      def successValue = d2.Directive[Any, F, Nothing, X](_ => f.map(Result.Success(_)))
      def failureValue = d2.Directive[Any, F, X, Nothing](_ => f.map(Result.Failure(_)))
      def errorValue   = d2.Directive[Any, F, X, Nothing](_ => f.map(Result.Error(_)))
    }

    implicit def queryParamsDirective[A, L](t: QueryParams.type): d2.Directive[A, F, L, Map[String, Seq[String]]] = {
      request[A].map{case QueryParams(qp) => qp}
    }

    implicit def stringHeaderDirective[A, L](Header: StringHeader): d2.Directive[A, F, L, Option[String]] = {
      request[A].map{
        case Header(v) => Some(v)
        case _ => None
      }
    }
  }

  object implicits {
    implicit def wrapSuccess[S](f: F[S]): d2.Directive[Any, F, Nothing, S] = ops.MonadDecorator(f).successValue
  }

  object request {
    def apply[T] = Directive[T, Nothing, HttpRequest[T]](req => F.point(Result.Success(req)))

    def underlying[T] = apply[T].map(_.underlying)
  }
}*/
