package jsonschema

import io.circe.Encoder
import io.circe.Json
import io.circe.JsonObject

/**
 * Represents a JSON Schema
 */
sealed trait Schema:
  def toJson: Json

object Schema:

  given Encoder[Schema] = Encoder.instance(_.toJson)

  extension (sch: Schema)
    private def withOptionalStringField(key: String, value: Option[String]): Schema =
      new Schema:
        def toJson: Json =
          val base = sch.toJson.asObject.getOrElse(JsonObject.empty)
          Json.fromJsonObject(value.fold(base)(v => base.add(key, Json.fromString(v))))

    /**
     * Add a title to the schema.
     * This will create a new schema with the title added to the JSON representation.
     */
    def withTitle(title: Option[String]): Schema =
      withOptionalStringField("title", title)

    /**
     * Add a description to the schema.
     * This will create a new schema with the description added to the JSON representation.
     */
    def withDescription(description: Option[String]): Schema =
      withOptionalStringField("description", description)

  private def buildNumericJson[N](typeName: String, encode: N => Json)(
      minimum: Option[N],
      maximum: Option[N],
      exclusiveMinimum: Option[N],
      exclusiveMaximum: Option[N]
  ): Json =
    val base = JsonObject("type" -> Json.fromString(typeName))
    val w1 = minimum.fold(base)(v => base.add("minimum", encode(v)))
    val w2 = maximum.fold(w1)(v => w1.add("maximum", encode(v)))
    val w3 = exclusiveMinimum.fold(w2)(v => w2.add("exclusiveMinimum", encode(v)))
    val w4 = exclusiveMaximum.fold(w3)(v => w3.add("exclusiveMaximum", encode(v)))
    Json.fromJsonObject(w4)

  /**
   * A string type schema
   */
  case class StringSchema(
      minLength: Option[Int] = None,
      maxLength: Option[Int] = None,
      pattern: Option[String] = None,
      format: Option[String] = None
  ) extends Schema:
    def toJson: Json =
      val base = JsonObject("type" -> Json.fromString("string"))
      val withMinLength = minLength.fold(base)(min => base.add("minLength", Json.fromInt(min)))
      val withMaxLength =
        maxLength.fold(withMinLength)(max => withMinLength.add("maxLength", Json.fromInt(max)))
      val withPattern =
        pattern.fold(withMaxLength)(p => withMaxLength.add("pattern", Json.fromString(p)))
      val withFormat = format.fold(withPattern)(f => withPattern.add("format", Json.fromString(f)))
      Json.fromJsonObject(withFormat)

  /**
   * An integer type schema
   */
  case class IntegerSchema(
      minimum: Option[Int] = None,
      maximum: Option[Int] = None,
      exclusiveMinimum: Option[Int] = None,
      exclusiveMaximum: Option[Int] = None
  ) extends Schema:
    def toJson: Json =
      Schema.buildNumericJson(
        "integer",
        Json.fromInt
      )(minimum, maximum, exclusiveMinimum, exclusiveMaximum)

  /**
   * A number type schema (for floating point numbers)
   */
  case class NumberSchema(
      minimum: Option[Double] = None,
      maximum: Option[Double] = None,
      exclusiveMinimum: Option[Double] = None,
      exclusiveMaximum: Option[Double] = None
  ) extends Schema:
    def toJson: Json =
      Schema.buildNumericJson[Double]("number", (d: Double) => Json.fromDoubleOrNull(d))(
        minimum,
        maximum,
        exclusiveMinimum,
        exclusiveMaximum
      )

  /**
   * A boolean type schema
   */
  case class BooleanSchema() extends Schema:
    def toJson: Json =
      Json.fromJsonObject(JsonObject("type" -> Json.fromString("boolean")))

  /**
   * An enum type schema (non-parameterized enum cases)
   */
  case class EnumSchema(values: List[String]) extends Schema:
    def toJson: Json =
      Json.fromJsonObject(
        JsonObject(
          "type" -> Json.fromString("string"),
          "enum" -> Json.fromValues(values.map(Json.fromString))
        )
      )

  /**
   * An array type schema
   */
  case class ArraySchema(
      items: Schema,
      minItems: Option[Int] = None,
      maxItems: Option[Int] = None,
      uniqueItems: Option[Boolean] = None
  ) extends Schema:
    def toJson: Json =
      val base = JsonObject(
        "type" -> Json.fromString("array"),
        "items" -> items.toJson
      )
      val withMinItems = minItems.fold(base)(min => base.add("minItems", Json.fromInt(min)))
      val withMaxItems =
        maxItems.fold(withMinItems)(max => withMinItems.add("maxItems", Json.fromInt(max)))
      val withUniqueItems = uniqueItems.fold(withMaxItems)(unique =>
        withMaxItems.add("uniqueItems", Json.fromBoolean(unique))
      )
      Json.fromJsonObject(withUniqueItems)

  /**
   * An object type schema
   */
  case class ObjectSchema(
      properties: Map[String, Schema],
      required: List[String] = Nil
  ) extends Schema:
    def toJson: Json =
      val propsJson = JsonObject.fromMap(properties.view.mapValues(_.toJson).toMap)
      val base = JsonObject(
        "type" -> Json.fromString("object"),
        "properties" -> Json.fromJsonObject(propsJson)
      )
      val withRequired =
        if required.isEmpty then base
        else base.add("required", Json.fromValues(required.map(Json.fromString)))
      Json.fromJsonObject(withRequired)

  /**
   * A oneOf type schema for ADTs (parameterized enum cases)
   */
  case class ParametrizedCaseEnumSchema(schemas: List[Schema]) extends Schema:
    def toJson: Json =
      Json.fromJsonObject(
        JsonObject("oneOf" -> Json.fromValues(schemas.map(_.toJson)))
      )

  /**
   * An object type schema for Map types, using additionalProperties
   */
  case class MapSchema(additionalProperties: Schema) extends Schema:
    def toJson: Json =
      Json.fromJsonObject(
        JsonObject(
          "type" -> Json.fromString("object"),
          "additionalProperties" -> additionalProperties.toJson
        )
      )
