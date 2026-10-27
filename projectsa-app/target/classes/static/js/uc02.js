// UC02 Store Project Solution Details – <<Controller>> Solution Controller, Solution DetectBlank, File Check
"use strict";

const form = document.getElementById("form");
const panel = document.getElementById("solutionDetailForm");
let current = null;

// Step 1: loadProjectList()  →  GET /api/projects?status=NewProject (Query 1.1)
function loadProjectList() {
  panel.classList.add("hidden");
  return App.projectList(document.getElementById("projectList"), "/api/projects?status=NewProject", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
  ], selectProject, "ไม่มี Project สถานะ NewProject ที่คุณรับผิดชอบ");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/solution (Query 3.1, 3.2)
async function selectProject(row) {
  const d = await App.get(`/api/projects/${row.ProjectID}/solution`);
  current = d.project;
  document.getElementById("formTitle").textContent = `รายละเอียด Solution – ${current.ProjectName}`;
  document.getElementById("projectInfo").innerHTML =
    `<dt>Customer</dt><dd>${App.esc(current.CustomerName)}</dd><dt>Project Site</dt><dd>${App.esc(current.ProjectSite)}</dd>`;
  form.reset();
  App.clearErrors(form);
  form.startDate.value = current.StartDate ? App.date(current.StartDate) : "";   // existing solution can be edited
  form.endDate.value = current.EndDate ? App.date(current.EndDate) : "";
  form.objective.value = current.Objective || "";
  form.estimatedBudget.value = current.EstimatedBudget ?? "";
  form.solutionDetail.value = current.SolutionDetail || "";
  document.getElementById("documents").innerHTML = d.documents.length
    ? d.documents.map((x) => `${App.esc(x.DocType)}: ${App.esc(x.FileName)} (${App.dateTime(x.UploadedDate)})`).join("<br>")
    : "ยังไม่มี";
  panel.dataset.hasSolution = d.documents.some((x) => x.DocType === "Solution") ? "1" : "";
  panel.classList.remove("hidden");
}

// Step 4 → 5: validateSolution(formData)  –  Solution DetectBlank 5.1–5.3 (same rules as SolutionDetectBlank.java)
function validateSolution(d) {
  const e = {};
  if (!d.startDate) e.startDate = "กรุณากรอก Start Date";
  if (!d.endDate) e.endDate = "กรุณากรอก End Date";
  if (d.startDate && d.endDate && d.endDate < d.startDate) e.endDate = "End Date ต้องไม่ก่อน Start Date";
  if (!d.objective.trim()) e.objective = "กรุณากรอก Project Objective";
  const b = Number(String(d.estimatedBudget).replace(/,/g, ""));
  if (!String(d.estimatedBudget).trim()) e.estimatedBudget = "กรุณากรอก Estimated Budget";
  else if (!(b > 0)) e.estimatedBudget = "Estimated Budget ต้องเป็นตัวเลขที่มากกว่า 0";
  return e;
}

// Step 7: File Check 7.1–7.3 (PDF, ≤ 50 MB, at least 1 Solution file)
function fileCheck() {
  const sol = [...form.solutionFiles.files], req = [...form.requirementFiles.files];
  const bad = [...sol, ...req].filter((f) => !/\.pdf$/i.test(f.name) || f.size > 50 * 1024 * 1024);
  if (bad.length) return bad.map((f) => `${f.name}: ต้องเป็นไฟล์ PDF และขนาดไม่เกิน 50 MB`).join(" | ");
  if (!sol.length && !panel.dataset.hasSolution) return "ต้องมีเอกสาร Solution อย่างน้อย 1 ไฟล์";
  return null;
}

// Step 6 → 7–10: saveSolution(formData, files)  →  POST /api/projects/{id}/solution (Query 8.1, 8.2, 9.1, 10.1)
async function saveSolution() {
  const d = App.formData(form);
  const e = validateSolution(d);                                   // Alt 5a
  const fileProblem = fileCheck();                                 // Alt 7a
  if (fileProblem) e.files = fileProblem;
  if (Object.keys(e).length) return App.showErrors(form, { fields: e });
  const fd = new FormData(form);
  try {
    await App.post(`/api/projects/${current.ProjectID}/solution`, fd);
    App.toast("บันทึก Solution สำเร็จ – สถานะ WaitingForAssignEngineer และแจ้งเตือน PM แล้ว", "ok");
    loadProjectList();
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["Presale"]))) return;
  form.onsubmit = (ev) => { ev.preventDefault(); saveSolution(); };
  loadProjectList();
})();
