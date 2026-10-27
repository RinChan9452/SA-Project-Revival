// UC07 Approve New Project Requirement – <<Controller>> Approve Controller + Approve DetectBlank (Sequence UC07)
"use strict";

const $ = (id) => document.getElementById(id);
const form = $("form");
let project = null, requirement = null;

// Step 1: loadProjectList()  →  GET /api/projects?status=WaitingForApprove (Query 1.1)
function loadProjectList() {
  $("requirementListForm").classList.add("hidden");
  $("approveForm").classList.add("hidden");
  return App.projectList($("projectList"), "/api/projects?status=WaitingForApprove", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
    ["รออนุมัติ", (r) => `${r.PendingCount} ข้อ`],
  ], selectProject, "ไม่มี Project ที่รออนุมัติ Requirement");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/requirements?status=Pending (Query 3.1)
async function selectProject(row) {
  project = row;
  $("approveForm").classList.add("hidden");
  $("reqTitle").textContent = `Requirement ที่รออนุมัติ – ${row.ProjectName}`;
  await App.projectList($("requirementList"), `/api/projects/${row.ProjectID}/requirements?status=Pending`, [
    ["ID", (r) => r.RequirementID],
    ["หัวข้อ", (r) => App.esc(r.Title)],
    ["ความสำคัญ", (r) => App.esc(r.Priority)],
    ["วันที่ต้องการ", (r) => App.date(r.DueDate)],
    ["ผู้บันทึก", (r) => App.esc(r.CreatedBy)],
  ], selectRequirement, "ไม่มี Requirement ที่รออนุมัติ");
  $("requirementListForm").classList.remove("hidden");
}

// Step 4 → 5–6: selectRequirement(RequirementID)  →  GET /api/requirements/{id}?project= (Query 5.1, 6.1, 6.2)
async function selectRequirement(row) {
  let d;
  try {
    d = await App.get(`/api/requirements/${row.RequirementID}?project=${project.ProjectID}`);
  } catch (e) {
    App.toast(e.error, "bad");                                            // Alt 5a
    return selectProject(project);
  }
  requirement = row;
  const r = d.requirement;
  $("title").textContent = `พิจารณา: ${r.Title}`;
  $("detail").innerHTML = `<dt>รายละเอียด</dt><dd style="white-space:pre-wrap">${App.esc(r.Detail)}</dd>
    <dt>ความสำคัญ</dt><dd>${App.esc(r.Priority)}</dd><dt>วันที่ต้องการ</dt><dd>${App.date(r.DueDate)}</dd>
    <dt>ส่วนงาน / ฟีเจอร์</dt><dd>${App.esc(r.RelatedFeature || "-")}</dd><dt>บันทึกเมื่อ</dt><dd>${App.dateTime(r.CreatedDate)}</dd>
    <dt>เอกสารประกอบ</dt><dd>${d.attachments.length ? d.attachments.map((a) =>
      `<a href="/api/projects/${project.ProjectID}/requirements/${row.RequirementID}/attachment?file=${encodeURIComponent(a.AttachmentFile)}">${App.esc(a.FileName)}</a>`).join("<br>") : "-"}</dd>`;
  form.reset();
  App.clearErrors(form);
  $("approveForm").classList.remove("hidden");
}

// Step 8: Approve DetectBlank – a decision, and a reason when rejecting (Alt 8a)
function detectBlank(d) {
  const e = {};
  if (!d.decision) e.decision = "กรุณาเลือกอนุมัติ หรือ ปฏิเสธ";
  if (d.decision === "Rejected" && !d.rejectReason.trim()) e.rejectReason = "กรุณาระบุเหตุผลที่ปฏิเสธ";
  return e;
}

// Step 7 → 8–11: submitDecision(RequirementID, decision)  →  PUT /api/requirements/{id}/decision
async function submitDecision() {
  const d = App.formData(form);
  const e = detectBlank(d);
  if (Object.keys(e).length) return App.showErrors(form, { fields: e });
  try {
    const r = await App.put(`/api/requirements/${requirement.RequirementID}/decision`,
      { projectId: project.ProjectID, decision: d.decision, rejectReason: d.rejectReason });
    const msg = { WaitingForApprove: "ยังมี Requirement รออนุมัติอีก", NewProject: "พิจารณาครบ – Project กลับเป็น NewProject และแจ้ง Presale ปรับ Solution",
      Finish: "พิจารณาครบ – ปฏิเสธทั้งหมด Project เป็น Finish" }[r.ProjectStatus];
    App.toast(`บันทึกผลแล้ว และแจ้งเตือน PM – ${msg}`, "ok");
    if (r.ProjectStatus === "WaitingForApprove") selectProject(project);  // Alt 11a: back to Step 3
    else loadProjectList();
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["Sale"]))) return;
  form.onsubmit = (ev) => { ev.preventDefault(); submitDecision(); };
  loadProjectList();
})();
