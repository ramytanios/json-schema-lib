package jsonschema.excel

import cats.effect.IO
import cats.effect.IOApp
import io.circe.Json
import io.circe.syntax.*
import jsonschema.Description
import jsonschema.JsonSchema
import org.http4s.HttpRoutes
import org.http4s.circe.*
import org.http4s.dsl.io.*

/**
 * Example server for manual HTTP testing.
 *
 * Start with: sbt "excel/runMain jsonschema.excel.ExcelMain"
 *
 * Endpoints:
 *   - GET  http://localhost:7777/functions.html  — add-in host page
 *   - GET  http://localhost:7777/functions.js    — generated JS bundle
 *   - GET  http://localhost:7777/functions.json  — functions manifest
 *   - POST http://localhost:7777/invoke          — function dispatcher
 *
 * Example invoke calls:
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"ADD","params":{"x":3,"y":4}}'
 *
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"REPEAT","params":{"text":"hello","times":3}}'
 *
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"REPEAT","params":{"text":"hi","times":2,"sep":" | "}}'
 */
object ExcelMain extends IOApp.Simple:

  // ── Function input schemas ─────────────────────────────────────────────────

  case class Add(
      @Description("first operand") x: Double,
      @Description("second operand") y: Double
  ) derives JsonSchema

  case class Repeat(
      @Description("text to repeat") text: String,
      @Description("number of repetitions") times: Int,
      @Description("separator between repetitions (default: space)") sep: Option[String]
  ) derives JsonSchema

  // ── Excel instance ─────────────────────────────────────────────────────────

  private val centralUrl = "http://localhost:7777/invoke"

  private val excel = Excel(
    functions = List(
      ExcelFunctionBuilder.from[Add]("ADD", "Add two numbers"),
      ExcelFunctionBuilder.from[Repeat]("REPEAT", "Repeat a string N times")
    ),
    centralUrl = centralUrl
  )

  // ── Invoke route ───────────────────────────────────────────────────────────

  private val invokeRoutes: HttpRoutes[IO] =
    HttpRoutes.of[IO]:
      case req @ POST -> Root / "invoke" =>
        req.as[Json].flatMap: body =>
          val c = body.hcursor
          val fnId = c.get[String]("functionId").getOrElse("")
          val params = c.downField("params")
          fnId match
            case "ADD" =>
              (for
                x <- params.get[Double]("x")
                y <- params.get[Double]("y")
              yield Ok((x + y).asJson))
                .fold(e => BadRequest(e.message), identity)
            case "REPEAT" =>
              (for
                text <- params.get[String]("text")
                times <- params.get[Int]("times")
                sep = params.get[String]("sep").toOption.getOrElse(" ")
              yield Ok(List.fill(times)(text).mkString(sep).asJson))
                .fold(e => BadRequest(e.message), identity)
            case other =>
              NotFound(s"unknown function: $other")

  // ── Entry point ────────────────────────────────────────────────────────────

  def run: IO[Unit] =
    ExcelServer
      .server(excel, extraRoutes = invokeRoutes)
      .use: srv =>
        IO.println(s"Server running at http://${srv.address}") *> IO.never
