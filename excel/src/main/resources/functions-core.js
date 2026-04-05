let registeredIds = new Set();

async function loadAndRegister() {
  const data = await fetch("/functions.json").then(r => r.json());
  for (const fn of (data.functions || [])) {
    if (registeredIds.has(fn.id)) continue;
    registeredIds.add(fn.id);
    CustomFunctions.associate(fn.id, async function(...args) {
      const params = {};
      fn.parameters.forEach((p, i) => { params[p.name] = args[i]; });
      const resp = await axios.post(CENTRAL_URL, { functionId: fn.id, params });
      return resp.data;
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
