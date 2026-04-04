package jsonschema.excel

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.server.Server

object ExcelServer:

  val defaultPort: Port = port"7777"

  def server(
      excel: Excel,
      extraRoutes: HttpRoutes[IO] = HttpRoutes.empty[IO],
      port: Port = defaultPort
  ): Resource[IO, Server] =
    for
      js <- Resource.eval(IO.blocking(excel.`functions.js`()))
      json <- Resource.eval(IO.pure(excel.`functions.json`().noSpaces))
      html <- Resource.eval(IO.pure(excel.`functions.html`()))
      srv <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port)
        .withHttpApp((staticRoutes(js, json, html) <+> extraRoutes).orNotFound)
        .build
    yield srv

  private def staticRoutes(js: String, json: String, html: String): HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case GET -> Root / "functions.js" =>
        Ok(js).map(_.withContentType(`Content-Type`(MediaType.unsafeParse("text/javascript"))))
      case GET -> Root / "functions.json" =>
        Ok(json).map(_.withContentType(`Content-Type`(MediaType.application.json)))
      case GET -> Root / "functions.html" =>
        Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))
