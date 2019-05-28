package no.scalabin.http4s.directives

import cats.syntax.functor._
import cats.effect.Sync
import org.http4s.{AuthedRequest, AuthedService, HttpRoutes, Request, Response}

import scala.language.higherKinds

object DirectiveService {
  def apply[F[_]: Sync](pf: PartialFunction[Request[F], Directive[F, Response[F]]]): HttpRoutes[F] = HttpRoutes.of[F] {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(_.response)
  }
}

object AuthedDirectiveService {
  def apply[A, F[_]: Sync](pf: PartialFunction[AuthedRequest[F, A], Directive[F, Response[F]]]): AuthedService[A, F] =
    AuthedService[A, F] {
      case req if pf.isDefinedAt(req) => pf(req).run(req.req).map(_.response)
    }
}
