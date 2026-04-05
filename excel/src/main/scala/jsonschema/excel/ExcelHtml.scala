package jsonschema.excel

object ExcelHtml:

  /** Minimal host page that bootstraps the custom-functions JS runtime. */
  val functionsHtml: String =
    """|<!DOCTYPE html>
       |<html>
       |  <head>
       |    <meta charset="UTF-8"/>
       |    <script src="https://appsforoffice.microsoft.com/lib/1/hosted/office.js"></script>
       |  </head>
       |  <body>
       |    <script src="/functions.js"></script>
       |  </body>
       |</html>
       |""".stripMargin

  /** Task-pane reference page listing all registered functions. */
  def taskpaneHtml(namespace: String): String =
    val nsPrefix = if namespace.nonEmpty then s"$namespace." else ""
    s"""|<!DOCTYPE html>
        |<html>
        |<head>
        |  <meta charset="UTF-8"/>
        |  <meta name="viewport" content="width=device-width, initial-scale=1"/>
        |  <script src="https://appsforoffice.microsoft.com/lib/1/hosted/office.js"></script>
        |  <link rel="stylesheet" href="/taskpane.css"/>
        |</head>
        |<body>
        |  <div id="header">Function Reference</div>
        |  <div id="search-box">
        |    <input id="q" type="text" placeholder="Search functions…" oninput="doFilter()"/>
        |  </div>
        |  <button id="reload-btn" onclick="reload()">Reload Functions</button>
        |  <div id="count"></div>
        |  <div id="list"></div>
        |  <div id="empty">No matching functions.</div>
        |  <script>const NS_PREFIX = "$nsPrefix";</script>
        |  <script src="/taskpane.js"></script>
        |</body>
        |</html>
        |""".stripMargin

  val taskpaneCss: String =
    scala.io.Source.fromInputStream(
      getClass.getResourceAsStream("/taskpane.css")
    ).mkString

  val taskpaneJs: String =
    scala.io.Source.fromInputStream(
      getClass.getResourceAsStream("/taskpane.js")
    ).mkString

  val iconSvg: String =
    """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 80 80">
      |  <rect width="80" height="80" rx="4" fill="#217346"/>
      |  <text x="40" y="56" font-family="Segoe UI,sans-serif" font-size="44"
      |        font-weight="700" fill="#fff" text-anchor="middle">f</text>
      |</svg>""".stripMargin
