let fns = [];

function sig(f) {
  const ps = f.parameters.map(p => p.optional ? "[" + p.name + "]" : p.name).join(", ");
  return "=" + NS_PREFIX + f.name + "(" + ps + ")";
}

function card(f) {
  const div = document.createElement("div");
  div.className = "fn-card";
  const ps = f.parameters.map(p => {
    const opt = p.optional ? ' <span class="popt">(optional)</span>' : "";
    const desc = p.description ? " — " + p.description : "";
    return '<li><span class="pname">' + p.name + "</span>" + opt + desc + "</li>";
  }).join("");
  div.innerHTML =
    '<div class="fn-sig">' + sig(f) + "</div>" +
    '<div class="fn-desc">' + f.description + "</div>" +
    (ps ? '<ul class="params">' + ps + "</ul>" : "");
  return div;
}

function render(list) {
  const el = document.getElementById("list");
  el.innerHTML = "";
  list.forEach(f => el.appendChild(card(f)));
  document.getElementById("empty").style.display = list.length ? "none" : "block";
  document.getElementById("count").textContent =
    list.length + " function" + (list.length === 1 ? "" : "s");
}

function doFilter() {
  const q = document.getElementById("q").value.toLowerCase();
  render(q
    ? fns.filter(f => f.name.toLowerCase().includes(q) || f.description.toLowerCase().includes(q))
    : fns);
}

fetch("/functions.json").then(r => r.json()).then(data => {
  fns = data.functions || [];
  render(fns);
});

async function reload() {
  const btn = document.getElementById("reload-btn");
  btn.disabled = true;
  btn.textContent = "Reloading…";
  try {
    await OfficeRuntime.storage.setItem("cf-reload-signal", "1");
    const data = await fetch("/functions.json").then(r => r.json());
    fns = data.functions || [];
    doFilter();
    btn.textContent = "Reload Functions";
  } catch(e) {
    btn.textContent = "Error — retry";
  } finally {
    btn.disabled = false;
  }
}
