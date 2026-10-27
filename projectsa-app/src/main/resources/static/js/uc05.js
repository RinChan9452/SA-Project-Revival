// UC05 Set Project Test Calendar Notification – <<Controller>> Schedule Controller + Schedule DetectBlank
"use strict";

const $ = (id) => document.getElementById(id);
const form = $("form");
let current = null;

// Step 1: loadProjectList()  →  GET /api/projects?assigned=me (Query 1.1)
function loadProjectList() {
  $("testScheduleForm").classList.add("hidden");
  return App.projectList($("projectList"), "/api/projects?assigned=me", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
    ["Status", (r) => App.status(r.Status)],
  ], selectProject, "ไม่มี Project ที่คุณถูกมอบหมายในสถานะ Working / Testing");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/schedule-form (Query 3.1, 3.2, 3.3)
async function selectProject(row) {
  const d = await App.get(`/api/projects/${row.ProjectID}/schedule-form`);
  current = { ...row, ...d.project };
  $("title").textContent = `นัดทดสอบ – ${row.ProjectName}`;
  $("info").innerHTML = `<dt>Project Site</dt><dd>${App.esc(d.project.ProjectSite)}</dd>
    <dt>ลูกค้า</dt><dd>${App.esc(d.project.CustomerName)} (${App.esc(d.project.CustomerEmail)})</dd>
    <dt>Engineer ในทีม</dt><dd>${d.team.map((e) => App.esc(e.FirstName + " " + e.LastName)).join(", ")}</dd>`;
  const c = d.current;
  $("current").classList.toggle("hidden", !c);
  if (c) $("current").textContent = `นัดเดิม: ${App.date(c.TestDate)} ${App.time(c.StartTime)}–${App.time(c.EndTime)} ที่ ${c.Location} (จะถูกยกเลิกเมื่อบันทึกนัดใหม่)`;
  form.reset();
  App.clearErrors(form);
  form.location.value = d.project.ProjectSite;            // default = Project Site
  $("testScheduleForm").classList.remove("hidden");
}

// Step 5: Schedule DetectBlank 5.1–5.4 (same rules as ScheduleDetectBlank.java)
function detectBlank(d) {
  const e = {};
  const today = new Date().toLocaleDateString("sv-SE");          // local YYYY-MM-DD (not UTC)
  if (!d.testDate) e.testDate = "กรุณากรอกวันที่ทดสอบ";
  else if (d.testDate < today) e.testDate = "วันที่ทดสอบต้องไม่เป็นวันที่ผ่านมาแล้ว";
  if (!d.startTime) e.startTime = "กรุณากรอกเวลาเริ่ม";
  if (!d.endTime) e.endTime = "กรุณากรอกเวลาสิ้นสุด";
  if (d.startTime && d.endTime && d.endTime <= d.startTime) e.endTime = "เวลาสิ้นสุดต้องอยู่หลังเวลาเริ่ม";
  if (!d.location.trim()) e.location = "กรุณากรอกสถานที่ทดสอบ";
  if (!d.remindBefore) e.remindBefore = "กรุณาเลือกเวลาแจ้งเตือนล่วงหน้า";
  return e;
}

// Step 4 → 5–9: saveSchedule(formData)  →  POST /api/projects/{id}/schedules (Query 6.1, 6.2, 7.1, 8.1, 9.1)
async function saveSchedule() {
  const d = App.formData(form);
  const e = detectBlank(d);
  if (Object.keys(e).length) return App.showErrors(form, { fields: e });   // Alt 5a
  try {
    await App.post(`/api/projects/${current.ProjectID}/schedules`, { ...d, remindBefore: Number(d.remindBefore) });
    App.toast("บันทึกนัดทดสอบสำเร็จ – สถานะ Testing แจ้งเตือน PM และทีมแล้ว", "ok");
    loadProjectList();
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["TechEngineer"]))) return;
  form.onsubmit = (ev) => { ev.preventDefault(); saveSchedule(); };
  loadProjectList();
})();
