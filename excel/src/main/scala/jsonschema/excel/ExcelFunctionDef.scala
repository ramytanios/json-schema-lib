package jsonschema.excel

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject

case class ExcelFunctionDef(
    id: String,
    name: String,
    description: String,
    parameters: List[ExcelParameter]
)

object ExcelFunctionDef:
  given Encoder[ExcelFunctionDef] = Encoder.instance { fn =>
    Json.fromJsonObject(
      JsonObject(
        "id" -> Json.fromString(fn.id),
        "name" -> Json.fromString(fn.name),
        "description" -> Json.fromString(fn.description),
        "parameters" -> Encoder[List[ExcelParameter]].apply(fn.parameters),
        "result" -> Json.fromJsonObject(JsonObject("type" -> Json.fromString("any"))),
        "options" -> Json.fromJsonObject(
          JsonObject(
            "stream" -> Json.fromBoolean(false),
            "cancelable" -> Json.fromBoolean(false)
          )
        )
      )
    )
  }
