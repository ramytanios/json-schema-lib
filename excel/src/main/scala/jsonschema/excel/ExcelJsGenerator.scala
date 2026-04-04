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

  private val dynamicCore: String =
    """|let registeredIds = new Set();
       |
       |async function loadAndRegister() {
       |  const data = await fetch("/functions.json").then(r => r.json());
       |  for (const fn of (data.functions || [])) {
       |    if (registeredIds.has(fn.id)) continue;
       |    registeredIds.add(fn.id);
       |    CustomFunctions.associate(fn.id, async function(...args) {
       |      const params = {};
       |      fn.parameters.forEach((p, i) => { params[p.name] = args[i]; });
       |      const resp = await axios.post(CENTRAL_URL, { functionId: fn.id, params });
       |      return resp.data;
       |    });
       |  }
       |}
       |
       |loadAndRegister();
       |
       |setInterval(async () => {
       |  const sig = await OfficeRuntime.storage.getItem("cf-reload-signal");
       |  if (sig) {
       |    await OfficeRuntime.storage.removeItem("cf-reload-signal");
       |    await loadAndRegister();
       |  }
       |}, 2000);
       |""".stripMargin

  def generate(
      centralUrl: String,
      selfContained: Boolean = false
  ): String =
    val core =
      s"""|const CENTRAL_URL = "$centralUrl";
          |
          |$dynamicCore
          |""".stripMargin
    if selfContained then s"${Axios.jsSource()}\n\n$core"
    else core
