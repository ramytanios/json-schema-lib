package jsonschema.excel

import io.circe.Json
import jsonschema.JsonSchema

object ExcelFunctionBuilder:

  def from[A](id: String, description: String)(using js: JsonSchema[A]): ExcelFunctionDef =
    val cursor = js.schema.toJson.hcursor
    if cursor.get[String]("type").getOrElse("") != "object" then
      throw IllegalArgumentException("ExcelFunctionBuilder.from requires a case class JsonSchema")
    val required = cursor.downField("required").as[List[String]].getOrElse(Nil)
    val properties = cursor.downField("properties").as[Map[String, Json]].getOrElse(Map.empty)
    val reqPairs = required.flatMap(n => properties.get(n).map(n -> _))
    val optPairs = (properties -- required.toSet).toList.sortBy(_._1)
    val params = (reqPairs ++ optPairs).map { (name, fieldJson) =>
      val fc = fieldJson.hcursor
      val excelType = fc.get[String]("type").toOption match
        case Some("string")  => ExcelParameterType.StringType
        case Some("integer") => ExcelParameterType.NumberType
        case Some("number")  => ExcelParameterType.NumberType
        case Some("boolean") => ExcelParameterType.BooleanType
        case _               => ExcelParameterType.AnyType
      val desc = fc
        .get[String]("description")
        .toOption
        .orElse(fc.get[String]("title").toOption)
        .getOrElse("")
      ExcelParameter(name, desc, excelType, !required.contains(name))
    }
    ExcelFunctionDef(id, id, description, params)
