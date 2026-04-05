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

  private def functionsCore(): String =
    scala.io.Source.fromInputStream(
      getClass.getResourceAsStream("/functions-core.js")
    ).mkString

  def generate(
      centralUrl: String,
      selfContained: Boolean = false
  ): String =
    val core =
      s"""|const CENTRAL_URL = "$centralUrl";
          |
          |${functionsCore()}
          |""".stripMargin
    if selfContained then s"${Axios.jsSource()}\n\n$core"
    else core
