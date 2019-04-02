package no.scalabin.http4s.directives

import java.time.{LocalDateTime, ZoneOffset}

import cats.effect._
import org.http4s._
import org.http4s.implicits._
import org.http4s.dsl.io._
import org.http4s.headers.`If-Modified-Since`
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext

class DirectivesSpec extends FlatSpec with Matchers {
  private val lastModifiedTime = LocalDateTime.now()

  implicit def contextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  it should "respond with a ok response" in {
    val response = createService().orNotFound
      .run(Request(method = Method.GET, uri = Uri.uri("/hello?foo=1")))
      .unsafeRunSync()

    check(response = response, expectedHttpCode = Ok, expectedBody = Some("Hello World"))
  }

  it should "respond with not modified response" in {
    val modified = `If-Modified-Since`(HttpDate.unsafeFromInstant(lastModifiedTime.toInstant(ZoneOffset.UTC)))
    val response = createService().orNotFound
      .run(Request(method = Method.GET, uri = Uri.uri("/hello?foo=1"), headers = Headers(modified)))
      .unsafeRunSync()

    check(response = response, expectedHttpCode = NotModified)
  }

  it should "respond with a  bad request response" in {
    val response = createService().orNotFound
      .run(Request(method = Method.GET, uri = Uri.uri("/hello")))
      .unsafeRunSync()

    check(response = response, expectedHttpCode = BadRequest, expectedBody = Some("You didn't provide a foo, you fool!"))
  }

  it should "fallback to not found when not matching anything" in {
    val response = createService().orNotFound
      .run(Request(method = Method.GET, uri = Uri.uri("/foobar")))
      .unsafeRunSync()

    check(response = response, expectedHttpCode = NotFound)
  }

  private def check(response: Response[IO], expectedHttpCode: Status, expectedBody: Option[String] = None) = {
    response.status shouldBe expectedHttpCode
    if (expectedBody.nonEmpty) {
      val body = response.bodyAsText.compile.last.unsafeRunSync()
      body shouldBe expectedBody
    }
  }

  private def createService(): HttpRoutes[IO] = {
    val dsl = new DirectivesDsl[IO] with DirectiveDslOps[IO]
    import dsl._

    val Mapping = Plan.default[IO].Mapping(req => Path(req.uri.path))

    Mapping {
      case Root / "hello" =>
        for {
          _   <- Method.GET
          res <- ifModifiedSinceDir(lastModifiedTime, Ok("Hello World"))
          foo <- request.queryParam("foo")
          if foo.isDefined orDir BadRequest("You didn't provide a foo, you fool!")
        } yield res
    }
  }

}
