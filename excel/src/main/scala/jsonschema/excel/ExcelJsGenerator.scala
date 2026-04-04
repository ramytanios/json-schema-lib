package jsonschema.excel

object ExcelJsGenerator:

  private val axiosVersion = "1.7.9"
  private val axiosCdnUrl = s"https://cdn.jsdelivr.net/npm/axios@$axiosVersion/dist/axios.min.js"

  private def fetchAxiosSource(): String =
    val conn = java.net.URI.create(axiosCdnUrl).toURL.openConnection()
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
        |$paramLines
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
    if selfContained then s"${fetchAxiosSource()}\n\n$core"
    else core
