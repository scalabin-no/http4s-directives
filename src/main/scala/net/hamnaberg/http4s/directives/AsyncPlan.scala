package net.hamnaberg.http4s.directives

import cats.effect.IO
import org.http4s.dsl.impl.Path
import org.http4s._


object AsyncPlan {
  def apply(): AsyncPlan = AsyncPlan(PartialFunction.empty)
}

case class AsyncPlan(onFail: PartialFunction[Throwable, IO[Response[IO]]]) {
  type Intent = PartialFunction[Request[IO], IO[Response[IO]]]

  def task(pf: PartialFunction[Request[IO], Directive[IO, Response[IO], Response[IO]]]): Intent = {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(Result.merge).attempt.flatMap(
      _.fold({
        case t if onFail.isDefinedAt(t) => onFail(t)
        case t => IO.pure(Response(Status.InternalServerError))
      }, IO.pure)
    )
  }

  case class Mapping[X](from: Request[IO] => X) {
    def apply(intent: PartialFunction[X, Directive[IO, Response[IO], Response[IO]]]): Intent = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  val PathMapping : Mapping[Path] = Mapping[Path](r => Path(r.pathInfo))
}

