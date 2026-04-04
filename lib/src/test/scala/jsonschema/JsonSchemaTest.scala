package jsonschema

import io.circe.parser.*
import munit.FunSuite

class JsonSchemaTest extends FunSuite:

  test("String schema generates correct JSON"):
    val schema = JsonSchema[String].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Int schema generates correct JSON"):
    val schema = JsonSchema[Int].schema
    val json = schema.toJson
    val expected = parse("""{"type": "integer"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Double schema generates correct JSON"):
    val schema = JsonSchema[Double].schema
    val json = schema.toJson
    val expected = parse("""{"type": "number"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Long schema generates correct JSON"):
    val schema = JsonSchema[Long].schema
    val json = schema.toJson
    val expected = parse("""{"type": "integer"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Float schema generates correct JSON"):
    val schema = JsonSchema[Float].schema
    val json = schema.toJson
    val expected = parse("""{"type": "number"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Boolean schema generates correct JSON"):
    val schema = JsonSchema[Boolean].schema
    val json = schema.toJson
    val expected = parse("""{"type": "boolean"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("UUID schema generates correct JSON"):
    val schema = JsonSchema[java.util.UUID].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string", "format": "uuid"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("LocalDate schema generates correct JSON"):
    val schema = JsonSchema[java.time.LocalDate].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string", "format": "date"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("LocalTime schema generates correct JSON"):
    val schema = JsonSchema[java.time.LocalTime].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string", "format": "time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("OffsetTime schema generates correct JSON"):
    val schema = JsonSchema[java.time.OffsetTime].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string", "format": "time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Instant schema generates correct JSON"):
    val schema = JsonSchema[java.time.Instant].schema
    val json = schema.toJson
    val expected =
      parse("""{"type": "string", "format": "date-time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("LocalDateTime schema generates correct JSON"):
    val schema = JsonSchema[java.time.LocalDateTime].schema
    val json = schema.toJson
    val expected =
      parse("""{"type": "string", "format": "date-time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("OffsetDateTime schema generates correct JSON"):
    val schema = JsonSchema[java.time.OffsetDateTime].schema
    val json = schema.toJson
    val expected =
      parse("""{"type": "string", "format": "date-time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("ZonedDateTime schema generates correct JSON"):
    val schema = JsonSchema[java.time.ZonedDateTime].schema
    val json = schema.toJson
    val expected =
      parse("""{"type": "string", "format": "date-time"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("java.time.Duration schema generates correct JSON"):
    val schema = JsonSchema[java.time.Duration].schema
    val json = schema.toJson
    val expected =
      parse("""{"type": "string", "format": "duration"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("scala Duration schema generates correct JSON"):
    val schema = JsonSchema[scala.concurrent.duration.Duration].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("FiniteDuration schema generates correct JSON"):
    val schema = JsonSchema[scala.concurrent.duration.FiniteDuration].schema
    val json = schema.toJson
    val expected = parse("""{"type": "string"}""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("String schema with constraints"):
    val schema = Schema.StringSchema(
      minLength = Some(5),
      maxLength = Some(50),
      pattern = Some("^[a-z]+$")
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "string",
      "minLength": 5,
      "maxLength": 50,
      "pattern": "^[a-z]+$"
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Integer schema with constraints"):
    val schema = Schema.IntegerSchema(
      minimum = Some(0),
      maximum = Some(100)
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "integer",
      "minimum": 0,
      "maximum": 100
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Number schema with constraints"):
    val schema = Schema.NumberSchema(
      minimum = Some(0.0),
      maximum = Some(1.0)
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "number",
      "minimum": 0.0,
      "maximum": 1.0
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Object schema with properties"):
    val schema = Schema.ObjectSchema(
      properties = Map(
        "name" -> Schema.StringSchema(),
        "age" -> Schema.IntegerSchema()
      ),
      required = List("name", "age")
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "age": {"type": "integer"}
      },
      "required": ["name", "age"]
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Enum schema with values"):
    val schema = Schema.EnumSchema(
      values = List("red", "green", "blue")
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "string",
      "enum": ["red", "green", "blue"]
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Array schema with items"):
    val schema = Schema.ArraySchema(
      items = Schema.StringSchema()
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "array",
      "items": {"type": "string"}
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Map schema with string values"):
    val schema = Schema.MapSchema(additionalProperties = Schema.StringSchema())
    val json = schema.toJson
    val expected = parse("""{
      "type": "object",
      "additionalProperties": {"type": "string"}
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Map schema with integer values"):
    val schema = Schema.MapSchema(additionalProperties = Schema.IntegerSchema())
    val json = schema.toJson
    val expected = parse("""{
      "type": "object",
      "additionalProperties": {"type": "integer"}
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("Array schema with enum items"):
    val schema = Schema.ArraySchema(
      items = Schema.EnumSchema(List("red", "green", "blue"))
    )
    val json = schema.toJson
    val expected = parse("""{
      "type": "array",
      "items": {
        "type": "string",
        "enum": ["red", "green", "blue"]
      }
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)

  test("withSchemaVersion adds $schema as the first field (Draft-07)"):
    val schema = JsonSchema[String].schema.withSchemaVersion(JsonSchemaVersion.Draft07)
    val json = schema.toJson
    val expected =
      parse("""{"$schema": "http://json-schema.org/draft-07/schema#", "type": "string"}""")
        .getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)
    assertEquals(json.hcursor.keys.flatMap(_.headOption), Some("$schema"))

  test("withSchemaVersion adds $schema as the first field (Draft 2019-09)"):
    val schema = JsonSchema[Int].schema.withSchemaVersion(JsonSchemaVersion.Draft201909)
    val json = schema.toJson
    val expected =
      parse("""{"$schema": "https://json-schema.org/draft/2019-09/schema", "type": "integer"}""")
        .getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)
    assertEquals(json.hcursor.keys.flatMap(_.headOption), Some("$schema"))

  test("withSchemaVersion adds $schema as the first field (Draft 2020-12)"):
    val schema = JsonSchema[Boolean].schema.withSchemaVersion(JsonSchemaVersion.Draft202012)
    val json = schema.toJson
    val expected =
      parse("""{"$schema": "https://json-schema.org/draft/2020-12/schema", "type": "boolean"}""")
        .getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)
    assertEquals(json.hcursor.keys.flatMap(_.headOption), Some("$schema"))

  test("withSchemaVersion composes with withTitle and withDescription"):
    val schema = JsonSchema[String].schema
      .withTitle(Some("My Schema"))
      .withDescription(Some("A test schema"))
      .withSchemaVersion(JsonSchemaVersion.Draft202012)
    val json = schema.toJson
    val expected = parse("""{
      "$schema": "https://json-schema.org/draft/2020-12/schema",
      "type": "string",
      "title": "My Schema",
      "description": "A test schema"
    }""").getOrElse(io.circe.Json.Null)
    assertEquals(json, expected)
    assertEquals(json.hcursor.keys.flatMap(_.headOption), Some("$schema"))
