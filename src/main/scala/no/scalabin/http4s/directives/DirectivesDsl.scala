package no.scalabin.http4s.directives

import org.http4s.dsl.Http4sDsl2

trait DirectivesDsl[F[_]] extends Http4sDsl2[Directive[F, ?], F]

trait DirectiveDslOps[F[_]] extends DirectiveOps[F] with Conditional[F]
