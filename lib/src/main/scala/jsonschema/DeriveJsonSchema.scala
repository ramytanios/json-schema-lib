package jsonschema

import scala.quoted.*

/**
 * Macro-based derivation for JsonSchema
 */
object DeriveJsonSchema:

  /**
   * Derive a JsonSchema for a product type (case class)
   * This function will be called by the macro and will generate the appropriate schema
   * based on the case class's fields and their annotations.
   */
  inline def derived[A]: JsonSchema[A] = ${ derivedImpl[A] }

  /**
   * Implementation of the macro that generates a JsonSchema instance for a given type A.
   */
  private def derivedImpl[A: Type](using Quotes): Expr[JsonSchema[A]] =
    import quotes.reflect.*

    val tpe = TypeRepr.of[A]
    val typeSymbol = tpe.typeSymbol
    val annotations = typeSymbol.annotations

    // Extract title and description from annotations for the case class itself
    val title = extractAnnotation[Title, String](annotations)
    val description = extractAnnotation[Description, String](annotations)

    // Check if it's an enum (sealed type with case objects)
    if typeSymbol.flags.is(Flags.Enum) || (typeSymbol.flags.is(Flags.Sealed) && isEnumLike(
        typeSymbol
      ))
    then deriveEnumSchema[A](typeSymbol)
    else
      // Check if it's a case class
      if !typeSymbol.flags.is(Flags.Case) then
        report.errorAndAbort(s"${typeSymbol.name} is not a case class or enum")

      // Get the params of the constructor
      val params = typeSymbol.primaryConstructor.paramSymss.flatten.filter(_.isValDef)

      if params.isEmpty then
        report.errorAndAbort(s"${typeSymbol.name} has no parameters")

      // Generate schema for each field
      val fieldSchemas = params.map: param =>
        val fieldName = param.name
        val fieldType = tpe.memberType(param)
        val annotations = param.annotations

        // Extract schema and constraints from annotations for each field of the case class
        val (schemaExpr, isRequired) = generateFieldSchema(fieldType, annotations, fieldName)

        (Expr(fieldName), schemaExpr, Expr(isRequired))

      // Build the properties map
      val propertiesExprs = fieldSchemas.map:
        case (nameExpr, schemaExpr, _) => '{ ($nameExpr, $schemaExpr) }

      val propertiesMapExpr = Expr.ofList(propertiesExprs)

      // Build the required fields list
      val requiredExprs = fieldSchemas.collect:
        case (nameExpr, _, Expr(true)) => nameExpr

      val requiredListExpr = Expr.ofList(requiredExprs)

      '{
        JsonSchema.instance[A](
          Schema.ObjectSchema(
            properties = $propertiesMapExpr.toMap,
            required = $requiredListExpr
          )
            .withTitle(${ Expr(title) })
            .withDescription(${ Expr(description) })
        )
      }

  /**
   * Generate a field schema based on the field type and its annotations.
   */
  private def generateFieldSchema(using
      Quotes
  )(
      fieldType: quotes.reflect.TypeRepr,
      annotations: List[quotes.reflect.Term],
      fieldName: String
  ): (Expr[Schema], Boolean) =
    import quotes.reflect.*

    // Check if the field type is Option[T]
    val (actualType, isRequired) = fieldType match
      case AppliedType(tycon, List(innerType))
          if tycon.typeSymbol == TypeRepr.of[Option[?]].typeSymbol => (innerType, false)
      case _ => (fieldType, true)

    // Extract title and description
    val title = extractAnnotation[Title, String](annotations)
    val description = extractAnnotation[Description, String](annotations)

    // Extract constraint values from annotations
    val minLength = extractAnnotation[MinLength, Int](annotations)
    val maxLength = extractAnnotation[MaxLength, Int](annotations)
    val pattern = extractAnnotation[Pattern, String](annotations)
    val minimumInt = extractAnnotation[MinimumInt, Int](annotations)
    val maximumInt = extractAnnotation[MaximumInt, Int](annotations)
    val minimum = extractAnnotation[Minimum, Double](annotations)
    val maximum = extractAnnotation[Maximum, Double](annotations)
    val exclusiveMinimumInt = extractAnnotation[ExclusiveMinimumInt, Int](annotations)
    val exclusiveMaximumInt = extractAnnotation[ExclusiveMaximumInt, Int](annotations)
    val exclusiveMinimum = extractAnnotation[ExclusiveMinimum, Double](annotations)
    val exclusiveMaximum = extractAnnotation[ExclusiveMaximum, Double](annotations)
    val minItems = extractAnnotation[MinItems, Int](annotations)
    val maxItems = extractAnnotation[MaxItems, Int](annotations)
    val uniqueItems = extractAnnotation[UniqueItems, Boolean](annotations)

    val schemaExpr: Expr[Schema] =
      actualType.asType match
        case '[String] =>
          '{
            Schema.StringSchema(
              minLength = ${ Expr(minLength) },
              maxLength = ${ Expr(maxLength) },
              pattern = ${ Expr(pattern) }
            )
          }

        case '[Int] =>
          '{
            Schema.IntegerSchema(
              minimum = ${ Expr(minimumInt) },
              maximum = ${ Expr(maximumInt) },
              exclusiveMinimum = ${ Expr(exclusiveMinimumInt) },
              exclusiveMaximum = ${ Expr(exclusiveMaximumInt) }
            )
          }

        case '[Long] =>
          '{
            Schema.IntegerSchema(
              minimum = ${ Expr(minimumInt) },
              maximum = ${ Expr(maximumInt) },
              exclusiveMinimum = ${ Expr(exclusiveMinimumInt) },
              exclusiveMaximum = ${ Expr(exclusiveMaximumInt) }
            )
          }

        case '[Double] =>
          '{
            Schema.NumberSchema(
              minimum = ${ Expr(minimum) },
              maximum = ${ Expr(maximum) },
              exclusiveMinimum = ${ Expr(exclusiveMinimum) },
              exclusiveMaximum = ${ Expr(exclusiveMaximum) }
            )
          }

        case '[Float] =>
          '{
            Schema.NumberSchema(
              minimum = ${ Expr(minimum) },
              maximum = ${ Expr(maximum) },
              exclusiveMinimum = ${ Expr(exclusiveMinimum) },
              exclusiveMaximum = ${ Expr(exclusiveMaximum) }
            )
          }

        case '[Boolean] =>
          '{ Schema.BooleanSchema() }

        case '[java.time.LocalDate] =>
          '{ Schema.StringSchema(format = Some("date")) }

        case '[java.time.Instant] =>
          '{ Schema.StringSchema(format = Some("date-time")) }

        case '[java.util.UUID] =>
          '{ Schema.StringSchema(format = Some("uuid")) }

        case '[scala.collection.Seq[t]] =>
          val (elementSchemaExpr, _) =
            generateFieldSchema(TypeRepr.of[t], Nil, s"$fieldName.items")
          '{
            Schema.ArraySchema(
              items = $elementSchemaExpr,
              minItems = ${ Expr(minItems) },
              maxItems = ${ Expr(maxItems) },
              uniqueItems = ${ Expr(uniqueItems) }
            )
          }

        case '[Set[t]] =>
          Expr.summon[JsonSchema[t]] match
            case Some(elemJs) =>
              '{
                Schema.ArraySchema(
                  items = $elemJs.schema,
                  minItems = ${ Expr(minItems) },
                  maxItems = ${ Expr(maxItems) },
                  uniqueItems = Some(true)
                )
              }
            case None =>
              report.errorAndAbort(s"No JsonSchema instance for element type $fieldName")

        case '[t] =>
          Expr.summon[JsonSchema[t]] match
            case Some(jsExpr) =>
              '{ $jsExpr.schema }
            case None =>
              val sym = actualType.typeSymbol
              if sym.flags.is(Flags.Enum) || (sym.flags.is(Flags.Sealed) && isEnumLike(sym)) then
                val enumValues = sym.children.map(_.name)
                val valuesExpr = Expr.ofList(enumValues.map(Expr(_)))
                '{ Schema.EnumSchema(values = $valuesExpr) }
              else if sym.flags.is(Flags.Case) then
                '{ ${ derivedImpl[t] }.schema }
              else
                report.errorAndAbort(
                  s"Unsupported field type: ${actualType.show} for field $fieldName. " +
                    s"Provide an explicit JsonSchema instance or use a case class."
                )

    (
      '{ $schemaExpr.withTitle(${ Expr(title) }).withDescription(${ Expr(description) }) },
      isRequired
    )

  private def extractAnnotation[A: Type, R](using
      Quotes
  )(annotations: List[quotes.reflect.Term]): Option[R] =
    import quotes.reflect.*

    annotations.collectFirst:
      case Apply(Select(New(tpt), _), List(Literal(constant)))
          if tpt.tpe <:< TypeRepr.of[A] =>
        constant.value.asInstanceOf[R]

  private def isEnumLike(using Quotes)(symbol: quotes.reflect.Symbol): Boolean =
    import quotes.reflect.*

    if !symbol.flags.is(Flags.Sealed) then return false

    // Get all children of the sealed type
    val children = symbol.children
    if children.isEmpty then return false

    // Check if all children are modules (case objects)
    children.forall(child => child.flags.is(Flags.Module) && child.flags.is(Flags.Case))

  private def deriveEnumSchema[A: Type](using
      Quotes
  )(symbol: quotes.reflect.Symbol): Expr[JsonSchema[A]] =
    import quotes.reflect.*

    // Get all enum values (case objects)
    val enumValues = symbol.children.map(_.name)

    if enumValues.isEmpty then
      report.errorAndAbort(s"${symbol.name} has no enum values")

    val valuesExpr = Expr.ofList(enumValues.map(Expr(_)))

    '{
      JsonSchema.instance[A](
        Schema.EnumSchema(values = $valuesExpr)
      )
    }
