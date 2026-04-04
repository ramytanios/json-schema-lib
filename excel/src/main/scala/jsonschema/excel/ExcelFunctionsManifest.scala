package jsonschema.excel

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject

case class ExcelFunctionsManifest(functions: List[ExcelFunction.Def]):
  def toJson: Json =
    Json.fromJsonObject(
      JsonObject("functions" -> Encoder[List[ExcelFunction.Def]].apply(functions))
    )
