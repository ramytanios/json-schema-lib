package jsonschema.excel

import jsonschema.*
import munit.FunSuite

class ExcelFunctionBuilderTest extends FunSuite:

  // ── Builder tests ──────────────────────────────────────────────────────────

  test("all required fields → correct types, optional = false"):
    @Description("req desc")
    case class Req(name: String, count: Int, score: Double, flag: Boolean) derives JsonSchema
    val fn = ExcelFunction.from[Req]("REQ")
    assertEquals(fn.parameters.map(_.name).toSet, Set("name", "count", "score", "flag"))
    assertEquals(
      fn.parameters.map(_.`type`).toSet,
      Set(
        ExcelFunction.ParameterType.StringType,
        ExcelFunction.ParameterType.NumberType,
        ExcelFunction.ParameterType.NumberType,
        ExcelFunction.ParameterType.BooleanType
      )
    )
    assert(fn.parameters.forall(!_.optional))

  test("Option[T] fields → optional = true, inner type resolved correctly"):
    @Description("opt desc")
    case class Opt(name: String, count: Option[Int]) derives JsonSchema
    val fn = ExcelFunction.from[Opt]("OPT")
    val countParam = fn.parameters.find(_.name == "count").get
    assertEquals(countParam.optional, true)
    assertEquals(countParam.`type`, ExcelFunction.ParameterType.NumberType)

  test("Int/Long → number, Double/Float → number"):
    @Description("nums desc")
    case class Nums(i: Int, l: Long, d: Double, f: Float) derives JsonSchema
    val fn = ExcelFunction.from[Nums]("NUMS")
    assert(fn.parameters.forall(_.`type` == ExcelFunction.ParameterType.NumberType))

  test("Boolean → boolean"):
    @Description("bools desc")
    case class Bools(b: Boolean) derives JsonSchema
    val fn = ExcelFunction.from[Bools]("BOOLS")
    assertEquals(fn.parameters.head.`type`, ExcelFunction.ParameterType.BooleanType)

  test("List, Map, nested case class → any; non-parameterized enum → string"):
    case class Inner(x: Int) derives JsonSchema
    enum Color derives JsonSchema:
      case Red, Green, Blue
    @Description("complex desc")
    case class Complex(lst: List[String], mp: Map[String, Int], inner: Inner, color: Color)
        derives JsonSchema
    val fn = ExcelFunction.from[Complex]("COMPLEX")
    val anyParams = fn.parameters.filter(_.`type` == ExcelFunction.ParameterType.AnyType)
    assertEquals(anyParams.map(_.name).toSet, Set("lst", "mp", "inner"))
    val colorParam = fn.parameters.find(_.name == "color").get
    assertEquals(colorParam.`type`, ExcelFunction.ParameterType.StringType)

  test("@Description on field → description populated"):
    @Description("described desc")
    case class Described(@Description("The user name") name: String) derives JsonSchema
    val fn = ExcelFunction.from[Described]("DESC")
    assertEquals(fn.parameters.head.description, "The user name")

  test("@Title with no @Description → description falls back to title"):
    @Description("titled desc")
    case class Titled(@Title("User Name") name: String) derives JsonSchema
    val fn = ExcelFunction.from[Titled]("TITLE")
    assertEquals(fn.parameters.head.description, "User Name")

  test("no annotation → description = empty string"):
    @Description("noanno desc")
    case class NoAnno(name: String) derives JsonSchema
    val fn = ExcelFunction.from[NoAnno]("NOANNO")
    assertEquals(fn.parameters.head.description, "")

  test("required fields appear before optional in parameter list"):
    @Description("mixed desc")
    case class Mixed(id: Int, name: String, note: Option[String], extra: Option[Int])
        derives JsonSchema
    val fn = ExcelFunction.from[Mixed]("MIXED")
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
      ExcelFunction.from[Color]("COLOR")

  test("missing description in schema → IllegalArgumentException"):
    case class NoDesc(x: String) derives JsonSchema
    intercept[IllegalArgumentException]:
      ExcelFunction.from[NoDesc]("NODESC")

  // ── Manifest test ──────────────────────────────────────────────────────────

  test("ExcelFunctionsManifest.toJson wraps functions in {\"functions\": [...]}"):
    @Description("simple desc")
    case class Simple(x: String) derives JsonSchema
    val fn = ExcelFunction.from[Simple]("SIMPLE")
    val manifest = ExcelFunctionsManifest(List(fn))
    val json = manifest.toJson
    assert(json.hcursor.downField("functions").focus.exists(_.isArray))
    assertEquals(json.hcursor.downField("functions").as[List[io.circe.Json]].map(_.size), Right(1))

  // ── JS generator tests ─────────────────────────────────────────────────────

  test("ExcelJsGenerator.generate output contains const CENTRAL_URL header"):
    val js = ExcelJsGenerator.generate("https://example.com/route")
    assert(js.contains("""const CENTRAL_URL = "https://example.com/route";"""))

  test("generated JS dynamically fetches functions.json and uses CustomFunctions.associate"):
    val js = ExcelJsGenerator.generate("https://example.com")
    assert(js.contains("fetch(\"/functions.json\")"))
    assert(js.contains("CustomFunctions.associate(fn.id"))
    assert(js.contains("axios.post(CENTRAL_URL"))
    assert(js.contains("resp.data"))

  test("generated JS maps parameter names from function metadata"):
    val js = ExcelJsGenerator.generate("https://example.com")
    assert(js.contains("params[p.name] = args[i]"))
    assert(js.contains("fn.parameters.forEach"))

  test("generated JS polls OfficeRuntime.storage for reload signal"):
    val js = ExcelJsGenerator.generate("https://example.com")
    assert(js.contains("OfficeRuntime.storage.getItem(\"cf-reload-signal\")"))
    assert(js.contains("loadAndRegister()"))

  test("selfContained = false does not include axios source inline"):
    val js = ExcelJsGenerator.generate("https://example.com", selfContained = false)
    assert(!js.contains("axios/lib") && !js.contains("var axios"))

  test("full round-trip: JS contains CENTRAL_URL and dynamic registration"):
    @Description("Gets price")
    case class PricingRequest(ticker: String, date: Option[String]) derives JsonSchema
    @Description("Computes VaR")
    case class RiskRequest(portfolio: String, confidence: Double) derives JsonSchema
    val fns = List(
      ExcelFunction.from[PricingRequest]("PRICING.GET_PRICE"),
      ExcelFunction.from[RiskRequest]("RISK.COMPUTE_VAR")
    )
    val excel = new Excel(fns, "https://central-api.example.com/route")
    val js = excel.`functionsJs`()
    assert(js.contains("""const CENTRAL_URL = "https://central-api.example.com/route";"""))
    assert(js.contains("loadAndRegister()"))
    val manifestJson = excel.functionsJson()
    assertEquals(
      manifestJson.hcursor.downField("functions").as[List[io.circe.Json]].map(_.size),
      Right(2)
    )
