package no.scalabin.http4s.directives

import cats.effect._
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DirectiveHttp4sWrapperSpec extends AnyFlatSpec with Matchers {
  it should "respond with a ok response for /foo" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/foo"))

    check(responseIO = response, expectedHttpCode = Ok, expectedBody = Some("1"))
  }
  it should "respond with a ok response for /bar" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/bar"))

    check(responseIO = response, expectedHttpCode = Ok, expectedBody = Some("2"))
  }
  it should "respond with a ok response for /" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/"))

    check(responseIO = response, expectedHttpCode = Ok, expectedBody = Some("root"))
  }
  it should "respond with a ok response for /1" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/1"))

    check(responseIO = response, expectedHttpCode = Ok, expectedBody = Some("good"))
  }
  it should "respond with a internal server error response for /0" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/0"))

    check(responseIO = response, expectedHttpCode = InternalServerError, expectedBody = Some("No zeros!"))
  }
  it should "respond with a notFound response for /barfoo" in {
    val response = myService.run(Request(method = Method.GET, uri = uri"/barfoo"))

    check(responseIO = response, expectedHttpCode = NotFound, expectedBody = None)
  }

  private val ops = new DirectiveOps[IO] {}
  import ops._

  private val foobarService =
    DirectiveRoutes[IO] {
      case GET -> Root / "foo" => Ok("1").successF
      case GET -> Root / "bar" => Ok("2").successF
    }

  private val numberService = {
    val dsl = new DirectivesDsl[IO] with DirectiveDslOps[IO]
    import dsl._

    DirectiveRoutes[IO] {
      case GET -> Root                   => Ok("root")
      case GET -> Root / LongVar(number) => {
        if (number == 0)
          InternalServerError("No zeros!")
        else
          Ok("good")
      }
    }
  }

  private val myService = (foobarService <+> numberService).orNotFound

  private def check(responseIO: IO[Response[IO]], expectedHttpCode: Status, expectedBody: Option[String] = None) = {
    val response = responseIO.unsafeRunSync()
    response.status shouldBe expectedHttpCode
    if (expectedBody.nonEmpty) {
      val body = response.bodyAsText.compile.last.unsafeRunSync()
      body shouldBe expectedBody
    }
  }
}
