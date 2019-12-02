package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.{Defer, Applicative}
import org.http4s.{ContextRequest, AuthedRoutes, HttpRoutes, Request, Response}

object DirectiveRoutes {
  def apply[F[_]: Defer: Applicative](pf: PartialFunction[Request[F], Directive[F, Response[F]]]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
  }
}

object AuthedDirectiveRoutes {
  def apply[A, F[_]: Defer: Applicative](pf: PartialFunction[ContextRequest[F, A], Directive[F, Response[F]]]): AuthedRoutes[A, F] =
    AuthedRoutes.of[A, F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req.req).map(_.response)
    }
}
