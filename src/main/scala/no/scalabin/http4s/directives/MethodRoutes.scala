package no.scalabin.http4s.directives

import cats.{Defer, Monad}
import org.http4s.headers.Allow
import org.http4s._

object MethodRoutes {
  object fromMap {
    def apply[F[_]: Monad](map: Map[Method, Directive[F, Response[F]]]): Directive[F, Response[F]] =
      Directive((req: Request[F]) =>
        map
          .getOrElse(req.method, Directive.error[F, Response[F]](Response(Status.MethodNotAllowed).putHeaders(Allow(map.keySet))))
          .run(req)
      )
    def httpRoutes[F[_]: Monad: Defer](map: Map[Method, Directive[F, Response[F]]]): HttpRoutes[F] = fromMap(map).toHttpRoutes

    def contextRoutes[F[_]: Monad: Defer, A](map: Map[Method, Directive[F, Response[F]]]): ContextRoutes[A, F] =
      ContextRoutes((req: ContextRequest[F, A]) => httpRoutes(map).apply(req.req))
  }

  def apply[F[_]: Monad](map: (Method, Directive[F, Response[F]])*): Directive[F, Response[F]] =
    fromMap(map.toMap)

  def httpRoutes[F[_]: Monad: Defer](map: (Method, Directive[F, Response[F]])*): HttpRoutes[F] = fromMap.httpRoutes(map.toMap)

  def contextRoutes[F[_]: Monad: Defer, A](map: (Method, Directive[F, Response[F]])*): ContextRoutes[A, F] =
    fromMap.contextRoutes(map.toMap)
}
