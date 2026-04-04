package jsonschema.excel

import io.circe.Encoder

enum ExcelParameterType:
  case StringType, NumberType, BooleanType, AnyType

object ExcelParameterType:

  given Encoder[ExcelParameterType] = Encoder.encodeString.contramap:
    case StringType  => "string"
    case NumberType  => "number"
    case BooleanType => "boolean"
    case AnyType     => "any"
