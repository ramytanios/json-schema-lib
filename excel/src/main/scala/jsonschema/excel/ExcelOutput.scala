package jsonschema.excel

case class ExcelOutput(manifest: ExcelFunctionsManifest, js: String)

object ExcelOutput:
  def generate(functions: List[ExcelFunctionDef], centralUrl: String): ExcelOutput =
    ExcelOutput(
      ExcelFunctionsManifest(functions),
      ExcelJsGenerator.generate(functions, centralUrl)
    )
