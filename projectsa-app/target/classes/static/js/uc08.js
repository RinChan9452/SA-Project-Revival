// UC08 Close Project – <<Controller>> Close Controller (Sequence UC08)
"use strict";

const $ = (id) => document.getElementById(id);
let current = null;

// Step 1: loadProjectList()  →  GET /api/projects?status=Testing&pm=me (Query 1.1)
function loadProjectList() {
  $("closeProjectForm").classList.add("hidden");
  return App.projectList($("projectList"), "/api/projects?status=Testing&pm=me", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
  ], selectProject, "ไม่มี Project ที่คุณดูแลในสถานะ Testing");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/close-form (Query 3.1, 3.2, 3.3)
async function selectProject(row) {
  const d = await App.get(`/api/projects/${row.ProjectID}/close-form`);
  current = row;
  const p = d.project, t = d.latestTest;
  $("title").textContent = `ปิด Project – ${p.ProjectName}`;
  $("info").innerHTML = `<dt>Customer</dt><dd>${App.esc(p.CustomerName)}</dd>
    <dt>Sale in Charge</dt><dd>${App.esc(p.SaleInCharge)}</dd><dt>Presale Engineer in Charge</dt><dd>${App.esc(p.PresaleInCharge)}</dd>
    <dt>Tech Engineer</dt><dd>${d.engineers.map((e) => App.esc(e.FirstName + " " + e.LastName)).join(", ") || "-"}</dd>
    <dt>นัดทดสอบล่าสุด</dt><dd>${t ? `${App.date(t.TestDate)} ${App.time(t.StartTime)}–${App.time(t.EndTime)} ที่ ${App.esc(t.Location)}` : "ยังไม่มีข้อมูล"}</dd>`;
  $("closeProjectForm").classList.remove("hidden");
}

// Step 4 → 5–8: closeProject(ProjectID)  →  POST /api/projects/{id}/close (Query 5.1, 5.2, 6.1, 7.1, 8.1)
async function closeProject() {
  if (!confirm(`ยืนยันปิด Project "${current.ProjectName}"?\nระบบจะเปลี่ยนสถานะเป็น Finish และแจ้งเตือนผู้รับผิดชอบทุกคน`)) return; // Alt 4a
  try {
    await App.post(`/api/projects/${current.ProjectID}/close`);
    App.toast("ปิด Project สำเร็จ – สถานะ Finish และแจ้งเตือน Sale, Presale และ Tech Engineer แล้ว", "ok");
  } catch (err) {
    App.toast(err.error, "bad");                                                  // Alt 5a
  }
  loadProjectList();
}

(async () => {
  if (!(await App.page(["PM"]))) return;
  $("closeButton").onclick = closeProject;
  loadProjectList();
})();
