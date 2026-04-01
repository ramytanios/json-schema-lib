package jsonschema

import io.circe.parser.*
import munit.FunSuite

import scala.collection.mutable.Buffer

class DerivedJsonSchemaTest extends FunSuite:

  test("Derive schema for simple case class"):
    case class Person(name: String, age: Int)

    object Person:
      given JsonSchema[Person] = DeriveJsonSchema.derived

    val schema = JsonSchema[Person].schema
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

  test("Derive schema for case class with all primitive types"):
    case class AllPrimitives(
        str: String,
        int: Int,
        dbl: Double,
        lng: Long,
        flt: Float,
        bool: Boolean
    )

    object AllPrimitives:
      given JsonSchema[AllPrimitives] = DeriveJsonSchema.derived

    val schema = JsonSchema[AllPrimitives].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "str": {"type": "string"},
        "int": {"type": "integer"},
        "dbl": {"type": "number"},
        "lng": {"type": "integer"},
        "flt": {"type": "number"},
        "bool": {"type": "boolean"}
      },
      "required": ["str", "int", "dbl", "lng", "flt", "bool"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with string constraints"):
    case class Username(@MinLength(3) @MaxLength(20) username: String)

    object Username:
      given JsonSchema[Username] = DeriveJsonSchema.derived

    val schema = JsonSchema[Username].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "username": {
          "type": "string",
          "minLength": 3,
          "maxLength": 20
        }
      },
      "required": ["username"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with pattern constraint"):
    case class Email(@Pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") email: String)

    object Email:
      given JsonSchema[Email] = DeriveJsonSchema.derived

    val schema = JsonSchema[Email].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "email": {
          "type": "string",
          "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        }
      },
      "required": ["email"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with integer constraints"):
    case class Age(@MinimumInt(0) @MaximumInt(150) age: Int)

    object Age:
      given JsonSchema[Age] = DeriveJsonSchema.derived

    val schema = JsonSchema[Age].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "age": {
          "type": "integer",
          "minimum": 0,
          "maximum": 150
        }
      },
      "required": ["age"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with double constraints"):
    case class Temperature(@Minimum(-273.15) @Maximum(1000.0) temp: Double)

    object Temperature:
      given JsonSchema[Temperature] = DeriveJsonSchema.derived

    val schema = JsonSchema[Temperature].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "temp": {
          "type": "number",
          "minimum": -273.15,
          "maximum": 1000.0
        }
      },
      "required": ["temp"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with multiple constraints on different fields"):
    case class User(
        @MinLength(3) @MaxLength(50) name: String,
        @MinimumInt(18) @MaximumInt(120) age: Int,
        @Pattern("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$") email: String
    )

    object User:
      given JsonSchema[User] = DeriveJsonSchema.derived

    val schema = JsonSchema[User].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "minLength": 3,
          "maxLength": 50
        },
        "age": {
          "type": "integer",
          "minimum": 18,
          "maximum": 120
        },
        "email": {
          "type": "string",
          "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        }
      },
      "required": ["name", "age", "email"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with exclusive minimum and maximum"):
    case class Score(
        @ExclusiveMinimumInt(0) @ExclusiveMaximumInt(100) score: Int
    )

    object Score:
      given JsonSchema[Score] = DeriveJsonSchema.derived

    val schema = JsonSchema[Score].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "score": {
          "type": "integer",
          "exclusiveMinimum": 0,
          "exclusiveMaximum": 100
        }
      },
      "required": ["score"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with all Option fields - no required fields"):
    case class OptionalUser(
        name: Option[String],
        age: Option[Int],
        email: Option[String]
    )

    object OptionalUser:
      given JsonSchema[OptionalUser] = DeriveJsonSchema.derived

    val schema = JsonSchema[OptionalUser].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "age": {"type": "integer"},
        "email": {"type": "string"}
      }
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with mixed required and optional fields"):
    case class MixedUser(
        id: Int,
        name: String,
        email: Option[String],
        age: Option[Int]
    )

    object MixedUser:
      given JsonSchema[MixedUser] = DeriveJsonSchema.derived

    val schema = JsonSchema[MixedUser].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "name": {"type": "string"},
        "email": {"type": "string"},
        "age": {"type": "integer"}
      },
      "required": ["id", "name"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with Option fields and constraints"):
    case class OptionalWithConstraints(
        @MinLength(3) @MaxLength(50) name: Option[String],
        @MinimumInt(0) @MaximumInt(150) age: Option[Int]
    )

    object OptionalWithConstraints:
      given JsonSchema[OptionalWithConstraints] = DeriveJsonSchema.derived

    val schema = JsonSchema[OptionalWithConstraints].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {
          "type": "string",
          "minLength": 3,
          "maxLength": 50
        },
        "age": {
          "type": "integer",
          "minimum": 0,
          "maximum": 150
        }
      }
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for simple enum"):
    enum Color:
      case Red, Green, Blue

    object Color:
      given JsonSchema[Color] = DeriveJsonSchema.derived

    val schema = JsonSchema[Color].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "string",
      "enum": ["Red", "Green", "Blue"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for enum with multiple values"):
    enum Status:
      case Pending, Approved, Rejected, InProgress

    object Status:
      given JsonSchema[Status] = DeriveJsonSchema.derived

    val schema = JsonSchema[Status].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "string",
      "enum": ["Pending", "Approved", "Rejected", "InProgress"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class containing enum field"):
    enum Priority:
      case Low, Medium, High

    case class Task(name: String, priority: Priority)
    object Task:
      given JsonSchema[Task] = DeriveJsonSchema.derived

    val schema = JsonSchema[Task].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "priority": {
          "type": "string",
          "enum": ["Low", "Medium", "High"]
        }
      },
      "required": ["name", "priority"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class containing sequences"):

    enum Color:
      case Red, Green, Blue

    case class Car(drivers: Buffer[String], color: Color)
    object Car:
      given JsonSchema[Car] = DeriveJsonSchema.derived

    val schema = JsonSchema[Car].schema
    val json = schema.toJson

    val expected = parse("""{
        "type": "object",
        "properties": {
          "drivers": {
            "type": "array",
            "items": {"type": "string"}
          },
          "color": {
            "type": "string",
            "enum": ["Red", "Green", "Blue"]
          }
        },
        "required": ["drivers", "color"]
      }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class containing sequences of objects"):

    enum Color:
      case Red, Green, Blue

    case class Car(name: String, availableColors: List[Color])
    object Car:
      given JsonSchema[Car] = DeriveJsonSchema.derived

    val schema = JsonSchema[Car].schema
    val json = schema.toJson

    val expected = parse("""{
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "availableColors": {
            "type": "array",
            "items": {
              "type": "string",
              "enum": ["Red", "Green", "Blue"]
            }
          }
        },
        "required": ["name", "availableColors"]
      }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class containing sets"):

    enum Color:
      case Red, Green, Blue

    case class Car(drivers: Set[String], color: Color)
    object Car:
      given JsonSchema[Car] = DeriveJsonSchema.derived

    val schema = JsonSchema[Car].schema
    val json = schema.toJson

    val expected = parse("""{
        "type": "object",
        "properties": {
          "drivers": {
            "type": "array",
            "items": {"type": "string"},
            "uniqueItems": true
          },
          "color": {
            "type": "string",
            "enum": ["Red", "Green", "Blue"]
          }
        },
        "required": ["drivers", "color"]
      }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for nested case class with explicit derives"):
    case class Address(street: String, city: String)
    object Address:
      given JsonSchema[Address] = DeriveJsonSchema.derived

    case class Person(name: String, address: Address)
    object Person:
      given JsonSchema[Person] = DeriveJsonSchema.derived

    val schema = JsonSchema[Person].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "address": {
          "type": "object",
          "properties": {
            "street": {"type": "string"},
            "city": {"type": "string"}
          },
          "required": ["street", "city"]
        }
      },
      "required": ["name", "address"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for nested case class without explicit derives"):
    @annotation.nowarn("msg=unused local definition")
    case class Coords(lat: Double, lon: Double)
    case class Location(name: String, coords: Coords)
    object Location:
      given JsonSchema[Location] = DeriveJsonSchema.derived

    val schema = JsonSchema[Location].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "coords": {
          "type": "object",
          "properties": {
            "lat": {"type": "number"},
            "lon": {"type": "number"}
          },
          "required": ["lat", "lon"]
        }
      },
      "required": ["name", "coords"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for Option of nested case class"):
    @annotation.nowarn("msg=unused local definition")
    case class Meta(key: String, value: String)
    case class Item(id: Int, meta: Option[Meta])
    object Item:
      given JsonSchema[Item] = DeriveJsonSchema.derived

    val schema = JsonSchema[Item].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "meta": {
          "type": "object",
          "properties": {
            "key": {"type": "string"},
            "value": {"type": "string"}
          },
          "required": ["key", "value"]
        }
      },
      "required": ["id"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for List of nested case class"):
    @annotation.nowarn("msg=unused local definition")
    case class Tag(name: String)
    case class Post(title: String, tags: List[Tag])
    object Post:
      given JsonSchema[Post] = DeriveJsonSchema.derived

    val schema = JsonSchema[Post].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "title": {"type": "string"},
        "tags": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "name": {"type": "string"}
            },
            "required": ["name"]
          }
        }
      },
      "required": ["title", "tags"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for three levels of nesting"):
    @annotation.nowarn("msg=unused local definition")
    case class City(name: String)
    @annotation.nowarn("msg=unused local definition")
    case class Address(street: String, city: City)
    case class Person(name: String, address: Address)
    object Person:
      given JsonSchema[Person] = DeriveJsonSchema.derived

    val schema = JsonSchema[Person].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "address": {
          "type": "object",
          "properties": {
            "street": {"type": "string"},
            "city": {
              "type": "object",
              "properties": {
                "name": {"type": "string"}
              },
              "required": ["name"]
            }
          },
          "required": ["street", "city"]
        }
      },
      "required": ["name", "address"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for nested case class field with title and description"):
    @annotation.nowarn("msg=unused local definition")
    case class Inner(value: Int)
    case class Outer(@Title("Inner field") @Description("The inner object") inner: Inner)
    object Outer:
      given JsonSchema[Outer] = DeriveJsonSchema.derived

    val schema = JsonSchema[Outer].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "inner": {
          "type": "object",
          "title": "Inner field",
          "description": "The inner object",
          "properties": {
            "value": {"type": "integer"}
          },
          "required": ["value"]
        }
      },
      "required": ["inner"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for UUID field"):
    case class User(id: java.util.UUID, name: String)
    object User:
      given JsonSchema[User] = DeriveJsonSchema.derived

    val schema = JsonSchema[User].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "id": {"type": "string", "format": "uuid"},
        "name": {"type": "string"}
      },
      "required": ["id", "name"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for LocalDate field"):
    case class Event(name: String, date: java.time.LocalDate)
    object Event:
      given JsonSchema[Event] = DeriveJsonSchema.derived

    val schema = JsonSchema[Event].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "date": {"type": "string", "format": "date"}
      },
      "required": ["name", "date"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for Instant field"):
    case class LogEntry(message: String, timestamp: java.time.Instant)
    object LogEntry:
      given JsonSchema[LogEntry] = DeriveJsonSchema.derived

    val schema = JsonSchema[LogEntry].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "message": {"type": "string"},
        "timestamp": {"type": "string", "format": "date-time"}
      },
      "required": ["message", "timestamp"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class with both LocalDate and Instant"):
    case class Record(
        id: Int,
        createdAt: java.time.Instant,
        date: java.time.LocalDate
    )
    object Record:
      given JsonSchema[Record] = DeriveJsonSchema.derived

    val schema = JsonSchema[Record].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "id": {"type": "integer"},
        "createdAt": {"type": "string", "format": "date-time"},
        "date": {"type": "string", "format": "date"}
      },
      "required": ["id", "createdAt", "date"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for Option[LocalDate]"):
    case class Task(name: String, dueDate: Option[java.time.LocalDate])
    object Task:
      given JsonSchema[Task] = DeriveJsonSchema.derived

    val schema = JsonSchema[Task].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "dueDate": {"type": "string", "format": "date"}
      },
      "required": ["name"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for LocalDateTime field"):
    case class Event(name: String, startsAt: java.time.LocalDateTime)
    object Event:
      given JsonSchema[Event] = DeriveJsonSchema.derived

    val schema = JsonSchema[Event].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "startsAt": {"type": "string", "format": "date-time"}
      },
      "required": ["name", "startsAt"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for OffsetDateTime field"):
    case class Event(name: String, startsAt: java.time.OffsetDateTime)
    object Event:
      given JsonSchema[Event] = DeriveJsonSchema.derived

    val schema = JsonSchema[Event].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "startsAt": {"type": "string", "format": "date-time"}
      },
      "required": ["name", "startsAt"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for ZonedDateTime field"):
    case class Event(name: String, startsAt: java.time.ZonedDateTime)
    object Event:
      given JsonSchema[Event] = DeriveJsonSchema.derived

    val schema = JsonSchema[Event].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "startsAt": {"type": "string", "format": "date-time"}
      },
      "required": ["name", "startsAt"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for LocalTime field"):
    case class Schedule(label: String, at: java.time.LocalTime)
    object Schedule:
      given JsonSchema[Schedule] = DeriveJsonSchema.derived

    val schema = JsonSchema[Schedule].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "label": {"type": "string"},
        "at": {"type": "string", "format": "time"}
      },
      "required": ["label", "at"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for OffsetTime field"):
    case class Schedule(label: String, at: java.time.OffsetTime)
    object Schedule:
      given JsonSchema[Schedule] = DeriveJsonSchema.derived

    val schema = JsonSchema[Schedule].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "label": {"type": "string"},
        "at": {"type": "string", "format": "time"}
      },
      "required": ["label", "at"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for Duration field"):
    case class Job(name: String, timeout: java.time.Duration)
    object Job:
      given JsonSchema[Job] = DeriveJsonSchema.derived

    val schema = JsonSchema[Job].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "timeout": {"type": "string", "format": "duration"}
      },
      "required": ["name", "timeout"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for scala.concurrent.duration.Duration field"):
    case class Job(name: String, timeout: scala.concurrent.duration.Duration)
    object Job:
      given JsonSchema[Job] = DeriveJsonSchema.derived

    val schema = JsonSchema[Job].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "timeout": {"type": "string"}
      },
      "required": ["name", "timeout"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for scala.concurrent.duration.FiniteDuration field"):
    case class Job(name: String, timeout: scala.concurrent.duration.FiniteDuration)
    object Job:
      given JsonSchema[Job] = DeriveJsonSchema.derived

    val schema = JsonSchema[Job].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "timeout": {"type": "string"}
      },
      "required": ["name", "timeout"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class with Map[String, String] field"):
    case class Config(settings: Map[String, String])
    object Config:
      given JsonSchema[Config] = DeriveJsonSchema.derived

    val schema = JsonSchema[Config].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "settings": {
          "type": "object",
          "additionalProperties": {"type": "string"}
        }
      },
      "required": ["settings"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class with Map[String, Int] field"):
    case class Scores(values: Map[String, Int])
    object Scores:
      given JsonSchema[Scores] = DeriveJsonSchema.derived

    val schema = JsonSchema[Scores].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "values": {
          "type": "object",
          "additionalProperties": {"type": "integer"}
        }
      },
      "required": ["values"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class with Option[Map[String, String]] field"):
    case class Profile(metadata: Option[Map[String, String]])
    object Profile:
      given JsonSchema[Profile] = DeriveJsonSchema.derived

    val schema = JsonSchema[Profile].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "metadata": {
          "type": "object",
          "additionalProperties": {"type": "string"}
        }
      }
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema for case class with Map[String, nested case class] field"):
    @annotation.nowarn("msg=unused local definition")
    case class Info(value: String)
    case class Registry(entries: Map[String, Info])
    object Registry:
      given JsonSchema[Registry] = DeriveJsonSchema.derived

    val schema = JsonSchema[Registry].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "properties": {
        "entries": {
          "type": "object",
          "additionalProperties": {
            "type": "object",
            "properties": {
              "value": {"type": "string"}
            },
            "required": ["value"]
          }
        }
      },
      "required": ["entries"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)

  test("Derive schema with title and description"):
    @Title("Product") @Description("A product in the catalog")
    @Description("This case class represents a product with a name and price")
    case class Product(
        @Title("Product Name") @Description("The name of the product") name: String,
        @Title("Product Price") @Description("The price of the product") price: Double
    )

    object Product:
      given JsonSchema[Product] = DeriveJsonSchema.derived

    val schema = JsonSchema[Product].schema
    val json = schema.toJson

    val expected = parse("""{
      "type": "object",
      "title": "Product",
      "description": "This case class represents a product with a name and price",
      "properties": {
        "name": {
          "type": "string",
          "title": "Product Name",
          "description": "The name of the product"
        },
        "price": {
          "type": "number",
          "title": "Product Price",
          "description": "The price of the product"
        }
      },
      "required": ["name", "price"]
    }""").getOrElse(io.circe.Json.Null)

    assertEquals(json, expected)
