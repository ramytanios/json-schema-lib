package jsonschema.excel

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject

case class ExcelFunctionsManifest(functions: List[ExcelFunctionDef]):
  def toJson: Json =
    Json.fromJsonObject(
      JsonObject("functions" -> Encoder[List[ExcelFunctionDef]].apply(functions))
    )
