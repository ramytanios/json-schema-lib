package jsonschema.excel

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject

case class ExcelParameter(
    name: String,
    description: String,
    `type`: ExcelParameterType,
    optional: Boolean
)

object ExcelParameter:
  given Encoder[ExcelParameter] = Encoder.instance { p =>
    Json.fromJsonObject(
      JsonObject(
        "name" -> Json.fromString(p.name),
        "description" -> Json.fromString(p.description),
        "type" -> Encoder[ExcelParameterType].apply(p.`type`),
        "optional" -> Json.fromBoolean(p.optional)
      )
    )
  }
