package jsonschema.excel

object ExcelJsGenerator:

  def generate(functions: List[ExcelFunctionDef], centralUrl: String): String =
    val header = s"""const CENTRAL_URL = "$centralUrl";"""
    val bodies = functions
      .map { fn =>
        val paramNames = fn.parameters.map(_.name).mkString(", ")
        val paramsEntries = fn.parameters.map(p => s"          ${p.name}: ${p.name}").mkString(",\n")
        s"""CustomFunctions.associate(
  "${fn.id}",
  async function($paramNames) {
    const res = await fetch(CENTRAL_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        functionId: "${fn.id}",
        params: {
$paramsEntries
        }
      })
    });
    return res.json();
  }
);"""
      }
      .mkString("\n\n")
    s"$header\n\n$bodies\n"
