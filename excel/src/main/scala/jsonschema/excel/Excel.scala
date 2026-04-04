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

  def `functions.json`(): Json = ExcelFunctionsManifest(functions).toJson

  def `functions.html`(): String =
    s"""|<!DOCTYPE html>
        |<html>
        |  <head>
        |    <meta charset="UTF-8" />
        |    <script src="https://appsforoffice.microsoft.com/lib/1/hosted/office.js"></script>
        |  </head>
        |  <body>
        |    <script src="/functions.js"></script>
        |  </body>
        |</html>
        |""".stripMargin
