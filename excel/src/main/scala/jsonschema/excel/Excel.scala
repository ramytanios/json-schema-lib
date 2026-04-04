package jsonschema.excel

import io.circe.Json

/**
 * Represents an Excel add-in with a list of functions and a central URL for the add-in.
 *
 * @param functions   A list of Excel function definitions that the add-in provides.
 * @param centralUrl  The central URL for the add-in, which is used in the generated JavaScript and JSON outputs.
 */
class Excel(functions: List[ExcelFunctionDef], centralUrl: String):

  def `functions.js`(): String =
    ExcelJsGenerator.generate(functions, centralUrl, selfContained = true)

  def `functions.json`(): Json =
    ExcelOutput.generate(functions, centralUrl).manifest.toJson
