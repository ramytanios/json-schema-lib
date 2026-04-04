package jsonschema.excel

object ExcelJsGenerator:

  private object Axios:

    private val version = "1.14.0"
    private val cdnUrl = s"https://cdn.jsdelivr.net/npm/axios@$version/dist/axios.min.js"
    def jsSource(): String =
      val conn = java.net.URI.create(cdnUrl).toURL.openConnection()
      conn.setConnectTimeout(10_000)
      conn.setReadTimeout(10_000)
      scala.io.Source.fromInputStream(conn.getInputStream).mkString

  private def renderFunction(fn: ExcelFunctionDef): String =
    val paramNames = fn.parameters.map(_.name).mkString(", ")
    val maxKeyLen = fn.parameters.map(_.name.length).maxOption.getOrElse(0)
    val paramLines = fn.parameters
      .map { p =>
        val pad = " " * (maxKeyLen - p.name.length)
        s"        ${p.name}$pad: ${p.name},"
      }
      .mkString("\n")
    s"""|CustomFunctions.associate(
        |  "${fn.id}",
        |  async function($paramNames) {
        |    const response = await axios.post(CENTRAL_URL, {
        |      functionId: "${fn.id}",
        |      params: {
        |         $paramLines
        |      },
        |    });
        |    return response.data;
        |  }
        |);""".stripMargin

  def generate(
      functions: List[ExcelFunctionDef],
      centralUrl: String,
      selfContained: Boolean = false
  ): String =
    val bodies = functions.map(renderFunction).mkString("\n\n")
    val core =
      s"""|const CENTRAL_URL = "$centralUrl";
          |
          |$bodies
          |""".stripMargin
    if selfContained then s"${Axios.jsSource()}\n\n$core"
    else core
