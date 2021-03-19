package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.Monad
import org.http4s.{ContextRequest, ContextRoutes, HttpRoutes, Request, Response}

object DirectiveRoutes {
  def apply[F[_]: Monad](pf: PartialFunction[Request[F], Directive[F, Response[F]]]): HttpRoutes[F] =
    HttpRoutes.of[F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
    }
}

object DirectiveContextRoutes {
  def apply[A, F[_]: Monad](
      pf: PartialFunction[ContextRequest[F, A], Directive[F, Response[F]]]
  ): ContextRoutes[A, F] =
    ContextRoutes.of[A, F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req.req).map(_.response)
    }
}
