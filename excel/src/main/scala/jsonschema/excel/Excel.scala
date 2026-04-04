package jsonschema.excel

import io.circe.Json

/**
 * Represents an Excel add-in with a list of functions and a central URL for the add-in.
 *
 * @param functions
 *   A list of Excel function definitions that the add-in provides.
 * @param centralUrl
 *   The URL used in the generated JavaScript to dispatch function calls.
 * @param namespace
 *   The Excel namespace prefix used in formula syntax (e.g. "EXMAIN" → =EXMAIN.ADD(...)). Should
 *   match the Namespace resource in the add-in manifest.
 */
class Excel(functions: List[ExcelFunctionDef], centralUrl: String, namespace: String = ""):

  def `functions.js`(): String =
    ExcelJsGenerator.generate(functions, centralUrl, selfContained = true)
  def `functions.json`(): Json = ExcelFunctionsManifest(functions).toJson
  def `functions.html`(): String = ExcelHtml.functionsHtml
  def `taskpane.html`(): String = ExcelHtml.taskpaneHtml(namespace)
  def iconPng(size: Int): Array[Byte] = ExcelIcon.png(size)
