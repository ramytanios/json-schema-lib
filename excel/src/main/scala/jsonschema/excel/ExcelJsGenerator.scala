package jsonschema.excel

object ExcelJsGenerator:

  private val axiosVersion = "1.7.9"
  private val axiosCdnUrl =
    s"https://cdn.jsdelivr.net/npm/axios@$axiosVersion/dist/axios.min.js"

  private def fetchAxiosSource(): String =
    val conn = java.net.URI.create(axiosCdnUrl).toURL.openConnection()
    conn.setConnectTimeout(10_000)
    conn.setReadTimeout(10_000)
    scala.io.Source.fromInputStream(conn.getInputStream).mkString

  def generate(
      functions: List[ExcelFunctionDef],
      centralUrl: String,
      selfContained: Boolean = false
  ): String =
    val header = s"""const CENTRAL_URL = "$centralUrl";"""
    val bodies = functions
      .map { fn =>
        val paramNames = fn.parameters.map(_.name).mkString(", ")
        val paramsEntries = fn.parameters.map(p => s"      ${p.name}: ${p.name}").mkString(",\n")
        s"""CustomFunctions.associate(
  "${fn.id}",
  async function($paramNames) {
    const response = await axios.post(CENTRAL_URL, {
      functionId: "${fn.id}",
      params: {
$paramsEntries
      }
    });
    return response.data;
  }
);"""
      }
      .mkString("\n\n")
    val core = s"$header\n\n$bodies\n"
    if selfContained then s"${fetchAxiosSource()}\n\n$core"
    else core
