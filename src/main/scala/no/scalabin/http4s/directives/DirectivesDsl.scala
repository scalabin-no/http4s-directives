package no.scalabin.http4s.directives

import cats.{Monad, ~>}
import org.http4s.dsl.Http4sDsl2

abstract class DirectivesDsl[F[_]: Monad] extends Http4sDsl2[Directive[F, *], F] {
  override def liftG: F ~> Directive[F, *] = new ~>[F, Directive[F, *]] {
    override def apply[A](fa: F[A]): Directive[F, A] = Directive.liftF(fa)
  }
}

trait DirectiveDslOps[F[_]] extends DirectiveOps[F] with Conditional[F]
