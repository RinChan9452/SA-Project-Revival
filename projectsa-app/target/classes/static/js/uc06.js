// UC06 Add Customer New Requirement – <<Controller>> Requirement Controller, Requirement DetectBlank, File Check
"use strict";

const $ = (id) => document.getElementById(id);
const form = $("form");
let current = null;

// Step 1: loadProjectList()  →  GET /api/projects?status=Testing,WaitingForApprove (Query 1.1)
function loadProjectList() {
  $("newRequirementForm").classList.add("hidden");
  return App.projectList($("projectList"), "/api/projects?status=Testing,WaitingForApprove", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
    ["Status", (r) => App.status(r.Status)],
  ], selectProject, "ไม่มี Project ที่คุณดูแลในสถานะ Testing / WaitingForApprove");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/requirement-form (Query 3.1, 3.2)
async function selectProject(row) {
  const d = await App.get(`/api/projects/${row.ProjectID}/requirement-form`);
  current = row;
  const p = d.project;
  $("title").textContent = `Requirement ใหม่ – ${p.ProjectName}`;
  $("info").innerHTML = `<dt>Customer</dt><dd>${App.esc(p.CustomerName)}</dd><dt>PM</dt><dd>${App.esc(p.PMName)}</dd>
    <dt>Tech Engineer</dt><dd>${d.engineers.map((e) => App.esc(e.FirstName + " " + e.LastName)).join(", ") || "-"}</dd>
    <dt>สถานะปัจจุบัน</dt><dd>${App.status(p.Status)}</dd><dt>Start Date</dt><dd>${App.date(p.StartDate)}</dd>
    <dt>รายละเอียด Solution</dt><dd style="white-space:pre-wrap">${App.esc(p.SolutionDetail || "-")}</dd>`;
  form.reset();
  App.clearErrors(form);
  $("newRequirementForm").classList.remove("hidden");
}

// Step 4 → 5: validateRequirement(formData)  –  Requirement DetectBlank 5.1–5.3
function validateRequirement(d) {
  const e = {};
  if (!d.title.trim()) e.title = "กรุณากรอกหัวข้อ Requirement";
  if (!d.detail.trim()) e.detail = "กรุณากรอกรายละเอียดความต้องการ";
  if (!d.priority) e.priority = "กรุณาเลือกระดับความสำคัญ";
  if (!d.dueDate) e.dueDate = "กรุณากรอกวันที่ลูกค้าต้องการให้เสร็จ";
  else if (d.dueDate < new Date().toLocaleDateString("sv-SE")) e.dueDate = "วันที่ต้องการให้เสร็จต้องไม่เป็นวันที่ผ่านมาแล้ว";
  return e;
}

// Step 7: File Check (PDF / JPG / PNG ≤ 50 MB); no files → skip (Alt 6a)
function fileCheck() {
  const bad = [...form.attachments.files].filter((f) => !/\.(pdf|jpe?g|png)$/i.test(f.name) || f.size > 50 * 1024 * 1024);
  return bad.length ? bad.map((f) => `${f.name}: ต้องเป็นไฟล์ PDF / JPG / PNG และขนาดไม่เกิน 50 MB`).join(" | ") : null;
}

// Step 6 → 7–10: submitRequirement(formData, files)  →  POST /api/projects/{id}/requirements (8.1, 8.2, 9.1, 10.1)
async function submitRequirement() {
  const e = validateRequirement(App.formData(form));                       // Alt 5a
  const f = fileCheck();                                                   // Alt 7a
  if (f) e.files = f;
  if (Object.keys(e).length) return App.showErrors(form, { fields: e });
  try {
    await App.post(`/api/projects/${current.ProjectID}/requirements`, new FormData(form));
    App.toast("ส่งอนุมัติแล้ว – Requirement สถานะรออนุมัติ และแจ้งเตือน Sale แล้ว", "ok");
    loadProjectList();
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["PM"]))) return;
  form.onsubmit = (ev) => { ev.preventDefault(); submitRequirement(); };
  loadProjectList();
})();
