package no.scalabin.http4s.directives

import cats.{~>, Monad}
import org.http4s.dsl.Http4sDsl2

abstract class DirectivesDsl[F[_]: Monad] extends Http4sDsl2[Directive[F, *], F] {
  override def liftG: F ~> Directive[F, *] = Directive.liftK[F]
}

trait DirectiveDslOps[F[_]] extends DirectiveOps[F] with Conditional[F]
