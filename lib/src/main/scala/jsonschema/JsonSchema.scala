package jsonschema

import scala.collection.mutable.*

/**
 * Type class for generating JSON schemas at compile time
 */
trait JsonSchema[A]:
  def schema: Schema

object JsonSchema:

  /**
   * Summon a JsonSchema instance
   */
  def apply[A](using js: JsonSchema[A]): JsonSchema[A] = js

  /**
   * Create a JsonSchema instance from a schema value
   */
  def instance[A](s: Schema): JsonSchema[A] = new JsonSchema[A]:
    def schema: Schema = s

  /**
   * Given instance for String
   */
  given JsonSchema[String] = instance(Schema.StringSchema())

  /**
   * Given instance for Int
   */
  given JsonSchema[Int] = instance(Schema.IntegerSchema())

  /**
   * Given instance for Double
   */
  given JsonSchema[Double] = instance(Schema.NumberSchema())

  /**
   * Given instance for Long
   */
  given JsonSchema[Long] = instance(Schema.IntegerSchema())

  /**
   * Given instance for Float
   */
  given JsonSchema[Float] = instance(Schema.NumberSchema())

  /**
   * Given instance for Boolean
   */
  given JsonSchema[Boolean] = instance(Schema.BooleanSchema())

  /**
   * Given instance for java.util.UUID
   */
  given JsonSchema[java.util.UUID] = instance(Schema.StringSchema(format = Some("uuid")))

  /**
   * Given instance for java.time.LocalDate
   */
  given JsonSchema[java.time.LocalDate] = instance(Schema.StringSchema(format = Some("date")))

  /**
   * Given instance for java.time.LocalTime
   */
  given JsonSchema[java.time.LocalTime] = instance(Schema.StringSchema(format = Some("time")))

  /**
   * Given instance for java.time.OffsetTime
   */
  given JsonSchema[java.time.OffsetTime] = instance(Schema.StringSchema(format = Some("time")))

  /**
   * Given instance for java.time.Instant
   */
  given JsonSchema[java.time.Instant] = instance(Schema.StringSchema(format = Some("date-time")))

  /**
   * Given instance for java.time.LocalDateTime
   */
  given JsonSchema[java.time.LocalDateTime] =
    instance(Schema.StringSchema(format = Some("date-time")))

  /**
   * Given instance for java.time.OffsetDateTime
   */
  given JsonSchema[java.time.OffsetDateTime] =
    instance(Schema.StringSchema(format = Some("date-time")))

  /**
   * Given instance for java.time.ZonedDateTime
   */
  given JsonSchema[java.time.ZonedDateTime] =
    instance(Schema.StringSchema(format = Some("date-time")))

  /**
   * Given instance for java.time.Duration
   */
  given JsonSchema[java.time.Duration] = instance(Schema.StringSchema(format = Some("duration")))

  /**
   * Given instance for scala.concurrent.duration.Duration
   */
  @annotation.targetName("given_JsonSchema_ScalaDuration")
  given JsonSchema[scala.concurrent.duration.Duration] = instance(Schema.StringSchema())

  /**
   * Given instance for scala.concurrent.duration.FiniteDuration
   */
  @annotation.targetName("given_JsonSchema_FiniteDuration")
  given JsonSchema[scala.concurrent.duration.FiniteDuration] = instance(Schema.StringSchema())

  /**
   * Given instance for List
   */
  given [A](using js: JsonSchema[A]): JsonSchema[List[A]] = instance(Schema.ArraySchema(js.schema))

  /**
   *  Given instrance for Seq
   */
  given [A](using js: JsonSchema[A]): JsonSchema[Seq[A]] = instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance for Vector
   */
  given [A](using js: JsonSchema[A]): JsonSchema[Vector[A]] = instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance for Set
   */
  given [A](using js: JsonSchema[A]): JsonSchema[Set[A]] = instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance for Buffer
   */
  given [A](using js: JsonSchema[A]): JsonSchema[Buffer[A]] = instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance or ListBuffer
   */
  given [A](using js: JsonSchema[A]): JsonSchema[ListBuffer[A]] =
    instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance for ArrayBuffer
   */
  given [A](using js: JsonSchema[A]): JsonSchema[ArrayBuffer[A]] =
    instance(Schema.ArraySchema(js.schema))

  /**
   * Given instance for immutable Map (String keys only)
   */
  given [V](using js: JsonSchema[V]): JsonSchema[Map[String, V]] =
    instance(Schema.MapSchema(js.schema))

  /**
   * Given instance for mutable Map (String keys only)
   */
  @annotation.targetName("given_JsonSchema_MutableMap")
  given [V](using js: JsonSchema[V]): JsonSchema[scala.collection.mutable.Map[String, V]] =
    instance(Schema.MapSchema(js.schema))

  /**
   * Given instance for mutable HashMap (String keys only)
   */
  @annotation.targetName("given_JsonSchema_HashMap")
  given [V](using js: JsonSchema[V]): JsonSchema[HashMap[String, V]] =
    instance(Schema.MapSchema(js.schema))

  /**
   * Extension method to generate schema
   */
  extension [A](value: A)
    def jsonSchema(using js: JsonSchema[A]): Schema = js.schema

  /**
   * Extension on types to generate schema
   */
  inline def schemaFor[A](using js: JsonSchema[A]): Schema = js.schema
