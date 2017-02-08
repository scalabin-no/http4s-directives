package net.hamnaberg.http4s.directives

import org.http4s.{Request, Response, Status}

import scalaz.concurrent.Task

object AsyncTask {
  def apply(): AsyncTask = AsyncTask(PartialFunction.empty)
}

case class AsyncTask(onFail: PartialFunction[Throwable, Task[Response]]) {
  type Intent = PartialFunction[Request, Task[Response]]

  def task(pf: PartialFunction[Request, Directive[Task, Response, Response]]): Intent = {
    case req if pf.isDefinedAt(req) => pf(req).run(req).map(Result.merge).handleWith{
      case t if onFail.isDefinedAt(t) => onFail(t)
      case t => Task.now(Response(Status.InternalServerError))
    }
  }

  case class Mapping[X](from: Request => X) {
    def apply(intent: PartialFunction[X, Directive[Task, Response, Response]]): Intent = task {
      case req if intent.isDefinedAt(from(req)) => intent(from(req))
    }
  }
}
