package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.effect.Sync
import org.http4s.{AuthedRequest, AuthedRoutes, HttpRoutes, Request, Response}

object DirectiveRoutes {
  def apply[F[_]: Sync](pf: PartialFunction[Request[F], ResponseDirective[F]]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
  }
}

object AuthedDirectiveRoutes {
  def apply[A, F[_]: Sync](pf: PartialFunction[AuthedRequest[F, A], ContextResponseDirective[F, A]]): AuthedRoutes[A, F] =
    AuthedRoutes.of[A, F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
    }
}
