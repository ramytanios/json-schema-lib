package jsonschema.excel

import cats.effect.IO
import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject
import io.circe.syntax.*
import org.http4s.Response
import org.http4s.circe.*
import org.http4s.dsl.io.*

/**
 * Structured error response for Excel invoke endpoints.
 *
 * The generated JavaScript runtime extracts `response.data.message` from failed
 * requests and surfaces it in Excel via `CustomFunctions.Error`, so the trader
 * sees a meaningful message in the error card rather than a generic HTTP status.
 *
 * Usage:
 * {{{
 *   ExcelError.badRequest("Strike must be positive")
 *   ExcelError.badRequest("Rate out of bounds", code = Some("RATE_OOB"))
 * }}}
 */
case class ExcelError(message: String, code: Option[String] = None)

object ExcelError:

  given Encoder[ExcelError] = Encoder.instance: e =>
    val base = JsonObject("message" -> Json.fromString(e.message))
    Json.fromJsonObject(e.code.fold(base)(c => base.add("code", Json.fromString(c))))

  /** 400 response with a structured JSON error body. */
  def badRequest(message: String, code: Option[String] = None): IO[Response[IO]] =
    BadRequest(ExcelError(message, code).asJson)

  /** 422 response for semantic validation errors (bad parameter values). */
  def unprocessable(message: String, code: Option[String] = None): IO[Response[IO]] =
    UnprocessableEntity(ExcelError(message, code).asJson)
