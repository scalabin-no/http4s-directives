package no.scalabin.http4s.directives

import cats.{Monad, ~>}
import org.http4s.dsl.Http4sDsl2

abstract class DirectivesDsl[F[_]: Monad] extends Http4sDsl2[RequestDirective[F, *], F] {
  override def liftG: F ~> RequestDirective[F, *] = new ~>[F, RequestDirective[F, *]] {
    override def apply[A](fa: F[A]): RequestDirective[F, A] = Directive.liftF(fa)
  }
}

trait DirectiveDslOps[F[_]] extends DirectiveOps[F] with Conditional[F]
