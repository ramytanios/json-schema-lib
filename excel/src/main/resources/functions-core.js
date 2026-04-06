function toExcelCell(value) {
  if (value === null || value === undefined) return "";
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") return value;
  return JSON.stringify(value);
}

// Always returns a 2D array so functions can be uniformly declared as dimensionality: "matrix".
// - scalar → [[value]]
// - array of arrays → each row mapped through toExcelCell
// - flat array → single-column matrix
// - object → two-column key/value matrix
function toExcelGrid(value) {
  if (value === null || value === undefined) return [[""]];
  if (typeof value === "string" || typeof value === "number" || typeof value === "boolean") return [[value]];
  if (Array.isArray(value)) {
    if (value.length === 0) return [[""]];
    if (Array.isArray(value[0])) return value.map(row => row.map(toExcelCell));
    return value.map(v => [toExcelCell(v)]);
  }
  return Object.entries(value).map(([k, v]) => [k, toExcelCell(v)]);
}

async function loadAndRegister() {
  const data = await fetch("/functions.json").then(r => r.json());
  for (const fn of (data.functions ?? [])) {
    CustomFunctions.associate(fn.id, async function(...args) {
      try {
        const params = {};
        fn.parameters.forEach((p, i) => { params[p.name] = args[i]; });
        const resp = await axios.post(CENTRAL_URL, { functionId: fn.id, params });
        return toExcelGrid(resp.data);
      } catch (e) {
        const msg = e?.response?.data?.message ?? e?.message ?? "Unknown error";
        throw new CustomFunctions.Error(CustomFunctions.ErrorCode.notAvailable, msg);
      }
    });
  }
}

loadAndRegister();

setInterval(async () => {
  const sig = await OfficeRuntime.storage.getItem("cf-reload-signal");
  if (sig) {
    await OfficeRuntime.storage.removeItem("cf-reload-signal");
    await loadAndRegister();
  }
}, 2000);
