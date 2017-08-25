package net.hamnaberg.http4s.directives

import org.http4s.dsl.Path
import org.http4s.{Request, Response, Status}

import fs2.Task

object Plan {
  def apply(): Plan = Plan(PartialFunction.empty)
}

case class Plan(onFail: PartialFunction[Throwable, Task[Response]]) {
  type Intent = PartialFunction[Request, Task[Response]]

  def task(pf: PartialFunction[Request, Directive[Response, Response]]): Intent = {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(Result.merge).handleWith{
      case t if onFail.isDefinedAt(t) => onFail(t)
      case t => Task.now(Response(Status.InternalServerError))
    }
  }

  case class Mapping[X](from: Request => X) {
    def apply(intent: PartialFunction[X, Directive[Response, Response]]): Intent = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }

  val PathMapping : Mapping[Path] = Mapping[Path](r => Path(r.pathInfo))
}
