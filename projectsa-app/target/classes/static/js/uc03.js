// UC03 Check Project Details – <<Controller>> Project Controller (Sequence UC03). Read only.
"use strict";

const NONE = `<div class="empty">ยังไม่มีข้อมูล</div>`;          // Alt 4a
const $ = (id) => document.getElementById(id);

// Step 1 / 2: loadProjectList()  →  GET /api/projects[?keyword=] (Query 1.1)
function loadProjectList(keyword = "") {
  return App.projectList($("projectList"), "/api/projects" + (keyword ? "?keyword=" + encodeURIComponent(keyword) : ""), [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
    ["Status", (r) => App.status(r.Status)],
  ], (r) => selectProject(r.ProjectID), keyword ? "ไม่พบ Project" : "ยังไม่มี Project ที่คุณเกี่ยวข้อง");
}

// Step 3 → 4: selectProject(ProjectID)  →  GET /api/projects/{id} (Query 3.1, 4.1–4.6)
async function selectProject(id) {
  let d;
  try {
    d = await App.get(`/api/projects/${id}`);
  } catch (e) {
    $("projectDetailPage").classList.add("hidden");
    return App.toast(e.error || "ไม่พบ Project", "bad");               // Alt 3a
  }
  const p = d.project;
  $("title").innerHTML = `${App.esc(p.ProjectName)} ${App.status(p.Status)}`;
  $("basic").innerHTML = `<dt>Project ID</dt><dd>${p.ProjectID}</dd><dt>Project Site</dt><dd>${App.esc(p.ProjectSite)}</dd>
    <dt>Customer</dt><dd>${App.esc(p.CustomerName)}</dd><dt>Customer Email</dt><dd>${App.esc(p.CustomerEmail)}</dd>`;
  $("people").innerHTML = `<dt>Sale in Charge</dt><dd>${App.esc(p.SaleInCharge)}</dd>
    <dt>Presale Engineer in Charge</dt><dd>${App.esc(p.PresaleInCharge)}</dd>
    <dt>PM</dt><dd>${p.PMName ? App.esc(p.PMName) : "ยังไม่มีข้อมูล"}</dd>
    <dt>Tech Engineer</dt><dd>${d.engineers.length ? d.engineers.map((e) => App.esc(e.FirstName + " " + e.LastName)).join(", ") : "ยังไม่มีข้อมูล"}</dd>`;
  $("solution").innerHTML = p.Objective ? `<dl class="info">
    <dt>Start / End Date</dt><dd>${App.date(p.StartDate)} – ${App.date(p.EndDate)}</dd>
    <dt>Project Objective</dt><dd>${App.esc(p.Objective)}</dd>
    <dt>Estimated Budget</dt><dd>${App.money(p.EstimatedBudget)}</dd>
    <dt>รายละเอียด Solution</dt><dd style="white-space:pre-wrap">${App.esc(p.SolutionDetail || "-")}</dd></dl>` : NONE;
  const t = d.latestTest;
  $("test").innerHTML = t ? `<dl class="info"><dt>วันที่</dt><dd>${App.date(t.TestDate)}</dd>
    <dt>เวลา</dt><dd>${App.time(t.StartTime)} – ${App.time(t.EndTime)}</dd><dt>สถานที่</dt><dd>${App.esc(t.Location)}</dd>
    <dt>รายละเอียด</dt><dd>${App.esc(t.TestDetail || "-")}</dd></dl>` : NONE;
  $("requirements").innerHTML = d.requirements.length ? `<table><tr><th>ID</th><th>หัวข้อ</th><th>ความสำคัญ</th><th>วันที่ต้องการ</th><th>สถานะ</th></tr>${
    d.requirements.map((r) => `<tr><td>${r.RequirementID}</td><td>${App.esc(r.Title)}</td><td>${App.esc(r.Priority)}</td>
      <td>${App.date(r.DueDate)}</td><td>${App.status(r.Status)}</td></tr>`).join("")}</table>` : NONE;
  const docs = d.documents.map((x) => `<a href="/api/projects/${id}/documents/${x.DocumentID}" onclick="return downloadDocument(this)">
      ${App.esc(x.DocType)}: ${App.esc(x.FileName)}</a> <small class="muted">${App.dateTime(x.UploadedDate)}</small>`)
    .concat(d.attachments.map((a) => `<a href="/api/projects/${id}/requirements/${a.RequirementID}/attachment?file=${encodeURIComponent(a.AttachmentFile)}"
      onclick="return downloadDocument(this)">New Requirement (${App.esc(a.Title)}): ${App.esc(a.FileName)}</a>`));
  $("documents").innerHTML = docs.length ? docs.join("<br>") : NONE;
  $("projectDetailPage").classList.remove("hidden");
  $("projectDetailPage").scrollIntoView({ behavior: "smooth" });
}

// Step 5 → 6: downloadDocument(DocumentID)  →  GET …/documents/{id} (Query 3.1 + 6.1) or …/attachment (6.2)
function downloadDocument(a) {
  return true;   // the browser follows the link; the server checks access (3.1) before sending the file
}

(async () => {
  if (!(await App.page())) return;
  $("search").onsubmit = (ev) => { ev.preventDefault(); loadProjectList(ev.target.keyword.value.trim()); };
  await loadProjectList();
  const fromBell = new URLSearchParams(location.search).get("project");      // F4: notification → this project
  if (fromBell) selectProject(Number(fromBell));
})();
