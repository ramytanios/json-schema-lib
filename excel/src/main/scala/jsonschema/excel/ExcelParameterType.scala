package jsonschema.excel

enum ExcelParameterType:
  case StringType, NumberType, BooleanType, AnyType

object ExcelParameterType:
  given io.circe.Encoder[ExcelParameterType] =
    io.circe.Encoder.encodeString.contramap:
      case StringType  => "string"
      case NumberType  => "number"
      case BooleanType => "boolean"
      case AnyType     => "any"
