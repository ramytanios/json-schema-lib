# json-schema-lib

A Scala 3 library that leverages type classes and macros to derive JSON schemas from case classes at compile time.

## ⚠️ Project Status

This project is purely experimental and was created for me to experiment with [Claude Code](https://claude.com/claude-code) and explore vibe-based coding workflows. It's a learning exercise and playground for testing AI-assisted development patterns. Use at your own risk!

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.ramytanios" %% "json-schema-lib" % "<version>"
libraryDependencies += "io.github.ramytanios" %% "json-schema-lib-excel" % "<version>"
```


## Features

- **`derives` keyword support** - Idiomatic Scala 3 derivation at the definition site
- **Compile-time schema generation** - Zero runtime overhead with macro-based derivation
- **Primitive type support** - String, Int, Long, Float, Double, Boolean
- **`java.time` support** - `LocalDate` → `date`, `LocalTime`/`OffsetTime` → `time`, `Instant`/`LocalDateTime`/`OffsetDateTime`/`ZonedDateTime` → `date-time`, `Duration` → `duration`
- **Scala duration support** - `scala.concurrent.duration.Duration` and `FiniteDuration` map to `{ "type": "string" }` (no standard JSON Schema format exists for Scala durations)
- **`java.util.UUID` support** - `UUID` maps to `{ "type": "string", "format": "uuid" }`
- **Enum support** - Scala 3 enums automatically map to JSON Schema enums
- **Nested case class support** - Case class fields are recursively inlined into the schema
- **Seq support** - Mutable and immutable sequences
- **Map support** - `Map[String, V]` maps to `{ "type": "object", "additionalProperties": ... }`
- **Option support** - Optional fields automatically excluded from required list
- **Circe integration** - Built-in JSON encoding for schemas
- **`$schema` declaration** - Annotate the root schema with a JSON Schema draft URI via `withSchemaVersion`

## Example

```scala
import jsonschema.*

enum Role:
  case Admin, User, Guest

case class Address(street: String, city: String)

// Idiomatic `derives` syntax (recommended)
@Title("User profile")
case class Profile(
  @MinLength(3) @MaxLength(50) username: String,
  @MinimumInt(18) @MaximumInt(120) age: Int,
  active: Boolean,
  role: Role,
  address: Address,
  @MinItems(1) tags: List[String],
  bio: Option[String]
) derives JsonSchema

// Equivalent explicit form (also supported)
// object Profile:
//   given JsonSchema[Profile] = DeriveJsonSchema.derived

val json = JsonSchema[Profile].schema
  .withSchemaVersion(JsonSchemaVersion.Draft202012)
  .toJson
```

**Generated JSON Schema:**

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
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

## Excel

An optional module (`excel`) exposes Scala functions as Excel custom functions via an HTTP server.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Excel                                                                      │
│                                                                             │
│  ┌──────────────────────┐            ┌──────────────────────────────────┐   │
│  │   Functions runtime  │            │           Task pane              │   │
│  │   (functions.html)   │            │         (taskpane.html)          │   │
│  │                      │            │                                  │   │
│  │  on startup          │            │  on load / "Reload Functions"    │   │
│  │  ┌────────────────┐  │            │  ┌────────────────────────────┐  │   │
│  │  │loadAndRegister │──┼────────────┼─▶│  GET /functions.json       │  │   │
│  │  └────────────────┘  │            │  └────────────────────────────┘  │   │
│  │                      │            │       │ re-render list           │   │
│  │  poll every 2 s      │            │       ▼                          │   │
│  │  ┌────────────────┐  │  signal    │  OfficeRuntime.storage           │   │
│  │  │OfficeRuntime   │◀─┼────────────┼─  .setItem("cf-reload-signal")   │   │
│  │  │.storage.getItem│  │            │                                  │   │
│  │  └───────┬────────┘  │            └──────────────────────────────────┘   │
│  │          │ detected  │                                                   │
│  │          ▼           │                                                   │
│  │  ┌────────────────┐  │                                                   │
│  │  │loadAndRegister │  │  (only new IDs — tracked in registeredIds Set)    │
│  │  │  (additive)    │  │                                                   │
│  │  └───────┬────────┘  │                                                   │
│  │          │           │                                                   │
│  └──────────┼───────────┘                                                   │
└─────────────┼───────────────────────────────────────────────────────────────┘
              │ POST /invoke  {functionId, params}
              ▼
     ┌─────────────────┐
     │   Scala server  │
     │  (http4s/ember) │
     └─────────────────┘
```

- **`/functions.json`** — served at startup from the in-memory function list; always reflects the running server's functions.
- **`/functions.js`** — fetches `/functions.json` at runtime and calls `CustomFunctions.associate()` dynamically; polls `OfficeRuntime.storage` every 2 s for a reload signal.
- **Reload without add-in restart** — clicking "Reload Functions" signals the runtime to re-register new functions and refreshes the taskpane list. Formula-bar autocomplete for brand-new functions still requires a full add-in reload (Excel limitation).
