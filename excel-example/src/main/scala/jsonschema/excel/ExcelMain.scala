package jsonschema.excel

import cats.data.OptionT
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.syntax.semigroupk.*
import com.comcast.ip4s.*
import io.circe.Json
import io.circe.syntax.*
import jsonschema.Description
import jsonschema.JsonSchema
import org.http4s.HttpRoutes
import org.http4s.Uri
import org.http4s.circe.*
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder

/**
 * Example server for manual HTTP testing.
 *
 * Start with: sbt "excel-example/runMain jsonschema.excel.ExcelMain"
 *
 * Example invoke calls:
 *   # pure computation
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"ADD","params":{"x":3,"y":4}}'
 *
 *   # calls jsonplaceholder.typicode.com
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"FETCH_POST","params":{"id":1}}'
 *
 *   # calls api.open-meteo.com (lat/lon for Paris)
 *   curl -s -X POST http://localhost:7777/invoke \
 *     -H 'Content-Type: application/json' \
 *     -d '{"functionId":"CURRENT_TEMP","params":{"lat":48.85,"lon":2.35}}'
 */
object ExcelMain extends IOApp.Simple:

  // ── Function input schemas ─────────────────────────────────────────────────

  @Description("Add two numbers")
  case class Add(
      @Description("first operand") x: Double,
      @Description("second operand") y: Double
  ) derives JsonSchema

  @Description("Repeat a string N times")
  case class Repeat(
      @Description("text to repeat") text: String,
      @Description("number of repetitions") times: Int,
      @Description("separator between repetitions (default: space)") sep: Option[String]
  ) derives JsonSchema

  @Description("Fetch the title of a JSONPlaceholder post")
  case class FetchPost(
      @Description("post ID between 1 and 100") id: Int
  ) derives JsonSchema

  @Description("Current temperature (°C) from Open-Meteo")
  case class CurrentTemp(
      @Description("latitude  (e.g. 48.85 for Paris, 40.71 for New York)") lat: Double,
      @Description("longitude (e.g.  2.35 for Paris, -74.0 for New York)") lon: Double
  ) derives JsonSchema

  // ── Excel instance ─────────────────────────────────────────────────────────

  private val excel = Excel(
    functions = List(
      ExcelFunctionBuilder.from[Add]("ADD"),
      ExcelFunctionBuilder.from[Repeat]("REPEAT"),
      ExcelFunctionBuilder.from[FetchPost]("FETCH_POST"),
      ExcelFunctionBuilder.from[CurrentTemp]("CURRENT_TEMP")
    ),
    centralUrl = "http://localhost:7777/invoke",
    namespace = "EXMAIN"
  )

  // ── Invoke routes ──────────────────────────────────────────────────────────

  private def invokeRoutes(client: Client[IO]): HttpRoutes[IO] =
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

            case "FETCH_POST" =>
              params
                .get[Int]("id")
                .fold(
                  e => BadRequest(e.message),
                  id =>
                    client
                      .expect[Json](
                        Uri.unsafeFromString(s"https://jsonplaceholder.typicode.com/posts/$id")
                      )
                      .flatMap: json =>
                        Ok(json.hcursor.get[String]("title").getOrElse("(no title)").asJson)
                )

            case "CURRENT_TEMP" =>
              (for
                lat <- params.get[Double]("lat")
                lon <- params.get[Double]("lon")
              yield
                val uri = Uri
                  .unsafeFromString("https://api.open-meteo.com/v1/forecast")
                  .withQueryParam("latitude", lat)
                  .withQueryParam("longitude", lon)
                  .withQueryParam("current_weather", "true")
                client
                  .expect[Json](uri)
                  .flatMap: json =>
                    Ok(json.hcursor.downField(
                      "current_weather"
                    ).get[Double]("temperature").getOrElse(Double.NaN).asJson)
              ).fold(e => BadRequest(e.message), identity)

            case other =>
              NotFound(s"unknown function: $other")

  // ── Entry point ────────────────────────────────────────────────────────────

  private def logged(routes: HttpRoutes[IO]): HttpRoutes[IO] =
    HttpRoutes[IO](req => OptionT(IO.println(s"${req.method} ${req.uri}") *> routes(req).value))

  def run: IO[Unit] =
    EmberClientBuilder.default[IO].build.use: client =>
      (Resource.eval(ExcelRoutes.routes(excel)).flatMap: addinRoutes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"0.0.0.0")
          .withPort(port"7777")
          .withHttpApp((logged(addinRoutes) <+> logged(invokeRoutes(client))).orNotFound)
          .build).use: srv =>
        IO.println(s"Server running at http://${srv.address}") *> IO.never
