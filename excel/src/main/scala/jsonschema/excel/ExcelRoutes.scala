package jsonschema.excel

import cats.effect.IO
import org.http4s.HttpRoutes
import org.http4s.MediaType
import org.http4s.dsl.io.*
import org.http4s.headers.`Content-Type`

object ExcelRoutes:

  /** Builds the static-file routes for an [[Excel]] add-in, pre-computing all assets. */
  def routes(excel: Excel): IO[HttpRoutes[IO]] =
    for
      js <- IO.blocking(excel.functionsJs())
      json <- IO.pure(excel.functionsJson().noSpaces)
      html <- IO.pure(excel.functionsHtml())
      taskpane <- IO.pure(excel.taskpaneHtml())
      css <- IO.pure(excel.taskpaneCss())
      taskpaneJs <- IO.pure(excel.taskpaneJs())
    yield HttpRoutes.of[IO]:
      case GET -> Root / "functions.js" =>
        Ok(js).map(_.withContentType(`Content-Type`(MediaType.text.javascript)))
      case GET -> Root / "functions.json" =>
        Ok(json).map(_.withContentType(`Content-Type`(MediaType.application.json)))
      case GET -> Root / "functions.html" =>
        Ok(html).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      case GET -> Root / "taskpane.html" =>
        Ok(taskpane).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      case GET -> Root / "taskpane.css" =>
        Ok(css).map(_.withContentType(`Content-Type`(MediaType.text.css)))
      case GET -> Root / "taskpane.js" =>
        Ok(taskpaneJs).map(_.withContentType(`Content-Type`(MediaType.text.javascript)))
      case GET -> Root / "icon.svg" =>
        Ok(excel.iconSvg).map(_.withContentType(`Content-Type`(MediaType.image.`svg+xml`)))
