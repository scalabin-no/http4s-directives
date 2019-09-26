package no.scalabin.http4s

import org.http4s.{AuthedRequest, Request, Response}

package object directives {
  type ContextRequest[F[_], C]             = AuthedRequest[F, C]
  type RequestDirective[F[_], R]           = Directive[F, Request[F], R]
  type ContextRequestDirective[F[_], C, R] = Directive[F, ContextRequest[F, C], R]
  type ContextResponseDirective[F[_], C]   = Directive[F, ContextRequest[F, C], Response[F]]
  type ResponseDirective[F[_]]             = Directive[F, Request[F], Response[F]]
}
