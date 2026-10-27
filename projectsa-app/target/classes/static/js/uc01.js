// UC01 Create Project Profile – <<Controller>> Project Controller + Project DetectBlank (Sequence UC01)
"use strict";

const form = document.getElementById("form");
const panel = document.getElementById("projectDetailForm");

// Step 1 → 2: onClick() : openCreateForm()  →  GET /api/projects/create-form (Query 2.1–2.3)
async function openCreateForm() {
  const d = await App.get("/api/projects/create-form");
  document.getElementById("saleInCharge").value = `${d.sale.FirstName} ${d.sale.LastName}`;
  document.getElementById("presale").innerHTML = `<option value="">– เลือก Presale –</option>` +
    d.presales.map((p) => `<option value="${p.EmployeeID}">${App.esc(p.FirstName + " " + p.LastName)}</option>`).join("");
  document.getElementById("customer").innerHTML = `<option value="">– เลือกลูกค้าเดิม –</option>` +
    d.customers.map((c) => `<option value="${c.CustomerID}">${App.esc(c.CustomerName)} (${App.esc(c.CustomerEmail)})</option>`).join("");
  form.reset();
  App.clearErrors(form);
  setCustomerMode("old");
  panel.classList.remove("hidden");
}

function setCustomerMode(mode) {
  form.customerMode.value = mode;
  document.getElementById("customer").classList.toggle("hidden", mode !== "old");
  document.querySelectorAll(".newCust").forEach((el) => el.classList.toggle("hidden", mode !== "new"));
}

// Step 4: Project DetectBlank (4.1–4.5) – same rules as uc01/ProjectDetectBlank.java
function detectBlank(d) {
  const e = {};
  if (!d.projectName.trim()) e.projectName = "กรุณากรอก Project Name";
  if (d.customerMode === "old") {
    if (!d.customerId) e.customerId = "กรุณาเลือกลูกค้า";
  } else {
    if (!d.newCustomerName.trim()) e.newCustomerName = "กรุณากรอก Customer Name";
    if (!d.newCustomerEmail.trim()) e.newCustomerEmail = "กรุณากรอก Customer Email";
    else if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(d.newCustomerEmail.trim())) e.newCustomerEmail = "รูปแบบอีเมลไม่ถูกต้อง";
  }
  if (!d.presaleId) e.presaleId = "กรุณาเลือก Presale Engineer in Charge";
  if (!d.projectSite.trim()) e.projectSite = "กรุณากรอก Project Site";
  return e;
}

// Step 3 → 4–7: submitProject(formData)  →  POST /api/projects (Query 5.1, 5.2, 7.1)
async function submitProject(d) {
  const e = detectBlank(d);
  if (Object.keys(e).length) return App.showErrors(form, { fields: e });   // Alt 4a
  const body = {
    projectName: d.projectName, projectSite: d.projectSite, presaleId: Number(d.presaleId),
    customerId: d.customerMode === "old" ? Number(d.customerId) : null,
    newCustomerName: d.customerMode === "new" ? d.newCustomerName : null,
    newCustomerEmail: d.customerMode === "new" ? d.newCustomerEmail : null,
  };
  try {
    const r = await App.post("/api/projects", body);
    App.toast(`สร้าง Project #${r.ProjectID} สำเร็จ (สถานะ NewProject) และแจ้งเตือน Presale แล้ว`, "ok");
    panel.classList.add("hidden");
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["Sale"]))) return;
  document.getElementById("createProjectButton").onclick = openCreateForm;
  form.querySelectorAll("input[name=customerMode]").forEach((r) => (r.onchange = () => setCustomerMode(r.value)));
  form.onsubmit = (ev) => { ev.preventDefault(); submitProject(App.formData(form)); };
  document.getElementById("cancel").onclick = () => panel.classList.add("hidden");   // Alt 3a: nothing saved
})();
