package jsonschema.excel

import jsonschema.*
import munit.FunSuite

class ExcelFunctionBuilderTest extends FunSuite:

  // ── Builder tests ──────────────────────────────────────────────────────────

  test("all required fields → correct types, optional = false"):
    case class Req(name: String, count: Int, score: Double, flag: Boolean) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Req]("REQ", "desc")
    assertEquals(fn.parameters.map(_.name), List("name", "count", "score", "flag"))
    assertEquals(
      fn.parameters.map(_.`type`),
      List(
        ExcelParameterType.StringType,
        ExcelParameterType.NumberType,
        ExcelParameterType.NumberType,
        ExcelParameterType.BooleanType
      )
    )
    assert(fn.parameters.forall(!_.optional))

  test("Option[T] fields → optional = true, inner type resolved correctly"):
    case class Opt(name: String, count: Option[Int]) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Opt]("OPT", "desc")
    val countParam = fn.parameters.find(_.name == "count").get
    assertEquals(countParam.optional, true)
    assertEquals(countParam.`type`, ExcelParameterType.NumberType)

  test("Int/Long → number, Double/Float → number"):
    case class Nums(i: Int, l: Long, d: Double, f: Float) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Nums]("NUMS", "desc")
    assert(fn.parameters.forall(_.`type` == ExcelParameterType.NumberType))

  test("Boolean → boolean"):
    case class Bools(b: Boolean) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Bools]("BOOLS", "desc")
    assertEquals(fn.parameters.head.`type`, ExcelParameterType.BooleanType)

  test("List, Map, nested case class → any; non-parameterized enum → string"):
    case class Inner(x: Int) derives JsonSchema
    enum Color derives JsonSchema:
      case Red, Green, Blue
    case class Complex(lst: List[String], mp: Map[String, Int], inner: Inner, color: Color)
        derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Complex]("COMPLEX", "desc")
    val anyParams = fn.parameters.filter(_.`type` == ExcelParameterType.AnyType)
    assertEquals(anyParams.map(_.name).toSet, Set("lst", "mp", "inner"))
    val colorParam = fn.parameters.find(_.name == "color").get
    assertEquals(colorParam.`type`, ExcelParameterType.StringType)

  test("@Description on field → description populated"):
    case class Described(@Description("The user name") name: String) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Described]("DESC", "desc")
    assertEquals(fn.parameters.head.description, "The user name")

  test("@Title with no @Description → description falls back to title"):
    case class Titled(@Title("User Name") name: String) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Titled]("TITLE", "desc")
    assertEquals(fn.parameters.head.description, "User Name")

  test("no annotation → description = empty string"):
    case class NoAnno(name: String) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[NoAnno]("NOANNO", "desc")
    assertEquals(fn.parameters.head.description, "")

  test("required fields appear before optional in parameter list"):
    case class Mixed(id: Int, name: String, note: Option[String], extra: Option[Int])
        derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Mixed]("MIXED", "desc")
    val names = fn.parameters.map(_.name)
    val reqIdx = names.indexOf("id").max(names.indexOf("name"))
    val optIdx = names.indexOf("note").min(names.indexOf("extra"))
    assert(reqIdx < optIdx)

  test("non-ObjectSchema input → IllegalArgumentException"):
    @annotation.nowarn("msg=unused local definition")
    case class NotObject(value: String) derives JsonSchema
    // Use an enum schema (non-object) directly
    enum Color derives JsonSchema:
      case Red, Green, Blue
    intercept[IllegalArgumentException]:
      ExcelFunctionBuilder.from[Color]("COLOR", "desc")

  // ── Manifest test ──────────────────────────────────────────────────────────

  test("ExcelFunctionsManifest.toJson wraps functions in {\"functions\": [...]}"):
    case class Simple(x: String) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Simple]("SIMPLE", "desc")
    val manifest = ExcelFunctionsManifest(List(fn))
    val json = manifest.toJson
    assert(json.hcursor.downField("functions").focus.exists(_.isArray))
    assertEquals(json.hcursor.downField("functions").as[List[io.circe.Json]].map(_.size), Right(1))

  // ── JS generator tests ─────────────────────────────────────────────────────

  test("ExcelJsGenerator.generate output contains const CENTRAL_URL header"):
    val js = ExcelJsGenerator.generate(Nil, "https://example.com/route")
    assert(js.contains("""const CENTRAL_URL = "https://example.com/route";"""))

  test("generated JS contains CustomFunctions.associate call"):
    case class Req2(ticker: String) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Req2]("MY.FUNC", "desc")
    val js = ExcelJsGenerator.generate(List(fn), "https://example.com")
    assert(js.contains("CustomFunctions.associate("))
    assert(js.contains(""""MY.FUNC""""))
    assert(js.contains("async function(ticker)"))

  test("request body shape: { functionId: ..., params: { field: field } }"):
    case class Req3(ticker: String, amount: Double) derives JsonSchema
    val fn = ExcelFunctionBuilder.from[Req3]("MY.FUNC2", "desc")
    val js = ExcelJsGenerator.generate(List(fn), "https://example.com")
    assert(js.contains("""functionId: "MY.FUNC2""""))
    assert(js.contains("ticker: ticker"))
    assert(js.contains("amount: amount"))

  test("full round-trip: two functions, both CustomFunctions.associate calls present"):
    case class PricingRequest(ticker: String, date: Option[String]) derives JsonSchema
    case class RiskRequest(portfolio: String, confidence: Double) derives JsonSchema
    val fns = List(
      ExcelFunctionBuilder.from[PricingRequest]("PRICING.GET_PRICE", "Gets price"),
      ExcelFunctionBuilder.from[RiskRequest]("RISK.COMPUTE_VAR", "Computes VaR")
    )
    val output = ExcelOutput.generate(fns, "https://central-api.example.com/route")
    assert(output.js.contains(""""PRICING.GET_PRICE""""))
    assert(output.js.contains(""""RISK.COMPUTE_VAR""""))
    val manifestJson = output.manifest.toJson
    assertEquals(
      manifestJson.hcursor.downField("functions").as[List[io.circe.Json]].map(_.size),
      Right(2)
    )
