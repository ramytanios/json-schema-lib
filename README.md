# json-schema-lib

A Scala 3 library that leverages type classes and macros to derive JSON schemas from case classes at compile time.

## ⚠️ Project Status

This project is purely experimental and was created for me to experiment with [Claude Code](https://claude.com/claude-code) and explore vibe-based coding workflows. It's a learning exercise and playground for testing AI-assisted development patterns. Use at your own risk!

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.ramytanios" %% "json-schema-lib" % "<version>"
```


## Features

- **Compile-time schema generation** - Zero runtime overhead with macro-based derivation
- **Primitive type support** - String, Int, Long, Float, Double, Boolean
- **`java.time` support** - `LocalDate` maps to `{ "type": "string", "format": "date" }`, `Instant` maps to `{ "type": "string", "format": "date-time" }`
- **Enum support** - Scala 3 enums automatically map to JSON Schema enums
- **Nested case class support** - Case class fields are recursively inlined into the schema
- **Seq support** - Mutable and immutable sequences
- **Option support** - Optional fields automatically excluded from required list
- **Circe integration** - Built-in JSON encoding for schemas

## Example

```scala
import jsonschema.*

enum Role:
  case Admin, User, Guest

case class Address(street: String, city: String)

@Title("User profile")
case class Profile(
  @MinLength(3) @MaxLength(50) username: String,
  @MinimumInt(18) @MaximumInt(120) age: Int,
  active: Boolean,
  role: Role,
  address: Address,
  @MinItems(1) tags: List[String],
  bio: Option[String]
)

object Profile:
  given JsonSchema[Profile] = DeriveJsonSchema.derived

val json = JsonSchema[Profile].schema.toJson
```

**Generated JSON Schema:**

```json
{
  "type": "object",
  "title": "User profile",
  "properties": {
    "username": { "type": "string", "minLength": 3, "maxLength": 50 },
    "age": { "type": "integer", "minimum": 18, "maximum": 120 },
    "active": { "type": "boolean" },
    "role": { "type": "string", "enum": ["Admin", "User", "Guest"] },
    "address": {
      "type": "object",
      "properties": {
        "street": { "type": "string" },
        "city": { "type": "string" }
      },
      "required": ["street", "city"]
    },
    "tags": { "type": "array", "items": { "type": "string" }, "minItems": 1 },
    "bio": { "type": "string" }
  },
  "required": ["username", "age", "active", "role", "address", "tags"]
}
```

Nested case classes are inlined (no `$ref`) and work to arbitrary depth.

> **Limitation:** Mutually recursive case classes (e.g. `A` contains `B`, `B` contains `A`) will cause a compile-time stack overflow. Workaround: provide an explicit `given JsonSchema[B]` before deriving `A`.
