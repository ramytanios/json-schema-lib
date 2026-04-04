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
        |  <style>${css}</style>
        |</head>
        |<body>
        |  <div id="header">Function Reference</div>
        |  <div id="search-box">
        |    <input id="q" type="text" placeholder="Search functions…" oninput="doFilter()"/>
        |  </div>
        |  <div id="count"></div>
        |  <div id="list"></div>
        |  <div id="empty">No matching functions.</div>
        |  <script>${js(nsPrefix)}</script>
        |</body>
        |</html>
        |""".stripMargin

  // ── Styles ──────────────────────────────────────────────────────────────────

  private val css: String =
    """|
       |  * { box-sizing: border-box; }
       |
       |  body {
       |    margin: 0;
       |    font-family: 'Segoe UI', Tahoma, sans-serif;
       |    font-size: 13px;
       |    color: #323130;
       |    background: #fff;
       |  }
       |
       |  #header {
       |    background: #217346;
       |    color: #fff;
       |    padding: 12px 14px;
       |    font-size: 15px;
       |    font-weight: 600;
       |  }
       |
       |  #search-box {
       |    padding: 8px 10px;
       |    border-bottom: 1px solid #edebe9;
       |  }
       |
       |  #search-box input {
       |    width: 100%;
       |    padding: 5px 8px;
       |    border: 1px solid #c8c6c4;
       |    border-radius: 2px;
       |    font-size: 13px;
       |    outline: none;
       |  }
       |
       |  #search-box input:focus { border-color: #217346; }
       |
       |  #count {
       |    padding: 4px 14px;
       |    font-size: 11px;
       |    color: #a19f9d;
       |    background: #faf9f8;
       |    border-bottom: 1px solid #edebe9;
       |  }
       |
       |  .fn-card {
       |    padding: 10px 14px;
       |    border-bottom: 1px solid #edebe9;
       |  }
       |
       |  .fn-card:hover { background: #f3f2f1; }
       |
       |  .fn-sig {
       |    font-family: 'Cascadia Code', Consolas, monospace;
       |    color: #217346;
       |    font-weight: 600;
       |    font-size: 12px;
       |  }
       |
       |  .fn-desc { color: #605e5c; margin: 3px 0 5px; font-size: 12px; }
       |
       |  .params { list-style: none; padding: 0; margin: 0; }
       |
       |  .params li { font-size: 11px; color: #605e5c; padding: 1px 0; }
       |
       |  .pname { font-family: monospace; font-weight: 600; color: #323130; }
       |
       |  .popt { color: #a19f9d; font-style: italic; }
       |
       |  #empty { padding: 20px; text-align: center; color: #a19f9d; display: none; }
       |""".stripMargin

  // ── Client-side script ──────────────────────────────────────────────────────

  private def js(nsPrefix: String): String =
    s"""|
        |  const NS_PREFIX = "$nsPrefix";
        |  let fns = [];
        |
        |  function sig(f) {
        |    const ps = f.parameters.map(p => p.optional ? "[" + p.name + "]" : p.name).join(", ");
        |    return "=" + NS_PREFIX + f.name + "(" + ps + ")";
        |  }
        |
        |  function card(f) {
        |    const div = document.createElement("div");
        |    div.className = "fn-card";
        |    const ps = f.parameters.map(p => {
        |      const opt = p.optional ? ' <span class="popt">(optional)</span>' : "";
        |      const desc = p.description ? " — " + p.description : "";
        |      return '<li><span class="pname">' + p.name + "</span>" + opt + desc + "</li>";
        |    }).join("");
        |    div.innerHTML =
        |      '<div class="fn-sig">' + sig(f) + "</div>" +
        |      '<div class="fn-desc">' + f.description + "</div>" +
        |      (ps ? '<ul class="params">' + ps + "</ul>" : "");
        |    return div;
        |  }
        |
        |  function render(list) {
        |    const el = document.getElementById("list");
        |    el.innerHTML = "";
        |    list.forEach(f => el.appendChild(card(f)));
        |    document.getElementById("empty").style.display = list.length ? "none" : "block";
        |    document.getElementById("count").textContent =
        |      list.length + " function" + (list.length === 1 ? "" : "s");
        |  }
        |
        |  function doFilter() {
        |    const q = document.getElementById("q").value.toLowerCase();
        |    render(q
        |      ? fns.filter(f => f.name.toLowerCase().includes(q) || f.description.toLowerCase().includes(q))
        |      : fns);
        |  }
        |
        |  fetch("/functions.json").then(r => r.json()).then(data => {
        |    fns = data.functions || [];
        |    render(fns);
        |  });
        |""".stripMargin
