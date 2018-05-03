package no.scalabin.http4s.directives

import cats.effect.Sync
import cats.syntax.functor._

import org.http4s.util.CaseInsensitiveString
import org.http4s.{EntityDecoder, Header, HeaderKey, Headers, Query, Request, Uri}

import scala.language.higherKinds

trait RequestDirectives[F[+_]] {
  def request(implicit sync: Sync[F]): Directive[F, Nothing, Request[F]] = Directive[F, Nothing, Request[F]](req => sync.pure(Result.Success(req)))

  def headers(implicit sync: Sync[F]): Directive[F, Nothing, Headers] = request.map(_.headers)
  def header(key: HeaderKey)(implicit sync: Sync[F]): Directive[F, Nothing, Option[Header]] = headers.map(_.get(key.name))
  def header(key: String)(implicit sync: Sync[F]): Directive[F, Nothing, Option[Header]] = headers.map(_.get(CaseInsensitiveString(key)))

  def uri(implicit sync: Sync[F]): Directive[F, Nothing, Uri] = request.map(_.uri)
  def path(implicit sync: Sync[F]): Directive[F, Nothing, Uri.Path] = uri.map(_.path)
  def query(implicit sync: Sync[F]): Directive[F, Nothing, Query] = uri.map(_.query)

  def bodyAs[A](implicit dec: EntityDecoder[F, A], sync: Sync[F]): Directive[F, Nothing, A] = Directive(req => req.as[A].map(Result.Success(_)))

}
