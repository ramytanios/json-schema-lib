package jsonschema.excel

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject
import jsonschema.JsonSchema

object ExcelFunction:

  enum ParameterType:
    case StringType, NumberType, BooleanType, AnyType

  object ParameterType:
    given Encoder[ParameterType] = Encoder.encodeString.contramap:
      case StringType  => "string"
      case NumberType  => "number"
      case BooleanType => "boolean"
      case AnyType     => "any"

  case class Parameter(name: String, description: String, `type`: ParameterType, optional: Boolean)

  object Parameter:
    given Encoder[Parameter] = Encoder.instance: p =>
      Json.fromJsonObject(
        JsonObject(
          "name" -> Json.fromString(p.name),
          "description" -> Json.fromString(p.description),
          "type" -> Encoder[ParameterType].apply(p.`type`),
          "optional" -> Json.fromBoolean(p.optional)
        )
      )

  case class Def(id: String, name: String, description: String, parameters: List[Parameter])

  object Def:
    given Encoder[Def] = Encoder.instance: fn =>
      Json.fromJsonObject(
        JsonObject(
          "id" -> Json.fromString(fn.id),
          "name" -> Json.fromString(fn.name),
          "description" -> Json.fromString(fn.description),
          "parameters" -> Encoder[List[Parameter]].apply(fn.parameters),
          "result" -> Json.fromJsonObject(
            JsonObject(
              "type" -> Json.fromString("any"),
              "dimensionality" -> Json.fromString("matrix")
            )
          ),
          "options" -> Json.fromJsonObject(
            JsonObject(
              "stream" -> Json.fromBoolean(false),
              "cancelable" -> Json.fromBoolean(false)
            )
          )
        )
      )

  def from[A](id: String)(using js: JsonSchema[A]): Def =
    val cursor = js.schema.toJson.hcursor
    if cursor.get[String]("type").getOrElse("") != "object" then
      throw IllegalArgumentException("ExcelFunction.from requires a case class JsonSchema")
    val description = cursor
      .get[String]("description")
      .getOrElse(throw IllegalArgumentException(
        "ExcelFunction.from requires a description in the JsonSchema"
      ))
    val required = cursor.downField("required").as[List[String]].getOrElse(Nil).toSet
    val properties = cursor.downField("properties").as[Map[String, Json]].getOrElse(Map.empty)
    val params =
      properties.toList.sortBy((name, _) => (!required(name), name)).map: (name, fieldJson) =>
        val fc = fieldJson.hcursor
        val paramType = fc.get[String]("type").toOption match
          case Some("string")  => ParameterType.StringType
          case Some("integer") => ParameterType.NumberType
          case Some("number")  => ParameterType.NumberType
          case Some("boolean") => ParameterType.BooleanType
          case _               => ParameterType.AnyType
        val desc = fc
          .get[String]("description")
          .toOption
          .orElse(fc.get[String]("title").toOption)
          .getOrElse("")
        Parameter(name, desc, paramType, !required(name))

    Def(id, id, description, params)
