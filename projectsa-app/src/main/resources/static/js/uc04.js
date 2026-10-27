// UC04 Assign Engineer to Project – <<Controller>> Assign Controller + Assign DetectBlank (Sequence UC04)
"use strict";

const $ = (id) => document.getElementById(id);
const form = $("form");
let current = null, all = [], selected = new Set();

// Step 1: loadProjectList()  →  GET /api/projects?status=WaitingForAssignEngineer (Query 1.1)
function loadProjectList() {
  $("assignEngineerForm").classList.add("hidden");
  return App.projectList($("projectList"), "/api/projects?status=WaitingForAssignEngineer", [
    ["Project ID", (r) => r.ProjectID],
    ["Project Name", (r) => App.esc(r.ProjectName)],
    ["Customer", (r) => App.esc(r.CustomerName)],
  ], selectProject, "ไม่มี Project ที่รอมอบหมาย Engineer");
}

// Step 2 → 3: selectProject(ProjectID)  →  GET /api/projects/{id}/assign-form (Query 3.1, 3.2, 3.3)
async function selectProject(row) {
  const d = await App.get(`/api/projects/${row.ProjectID}/assign-form`);
  current = d.project;
  all = d.engineers;
  selected = new Set(all.filter((e) => e.InThisProject === 1).map((e) => e.EmployeeID));  // already in project
  $("title").textContent = `มอบหมาย Tech Engineer – ${current.ProjectName}`;
  $("solution").innerHTML = `<dt>Start / End Date</dt><dd>${App.date(current.StartDate)} – ${App.date(current.EndDate)}</dd>
    <dt>Objective</dt><dd>${App.esc(current.Objective)}</dd>`;
  $("skill").innerHTML = `<option value="">– ทุก Skill –</option>` +
    d.skills.map((s) => `<option value="${s.SkillID}">${App.esc(s.SkillName)}</option>`).join("");
  App.clearErrors(form);
  showEngineers(all);
  $("assignEngineerForm").classList.remove("hidden");
}

function showEngineers(list) {
  $("engineers").innerHTML = list.length ? `<table><tr><th></th><th>Tech Engineer</th><th>Skill</th><th>Project ที่ทำอยู่</th></tr>${
    list.map((e) => `<tr><td><input type="checkbox" value="${e.EmployeeID}" ${selected.has(e.EmployeeID) ? "checked" : ""}></td>
      <td>${App.esc(e.FirstName + " " + e.LastName)}</td><td>${App.esc(e.Skills || "-")}</td><td>${e.ActiveProjects}</td></tr>`).join("")
  }</table>` : `<div class="empty">ไม่พบ Engineer</div>`;                    // Alt 5a
  $("engineers").querySelectorAll("input").forEach((cb) => (cb.onchange = () => {
    const id = Number(cb.value);
    cb.checked ? selected.add(id) : selected.delete(id);
  }));
}

// Step 4 → 5: filterEngineer(SkillID)  →  GET /api/engineers?skill={SkillID} (Query 5.1)
async function filterEngineer(skillId) {
  if (!skillId) return showEngineers(all);
  showEngineers(await App.get(`/api/engineers?skill=${skillId}`));
}

// Step 6 → 7–10: assignEngineer(ProjectID, EngineerIDs)  →  POST /api/projects/{id}/assignments
async function assignEngineer() {
  if (!selected.size) {                                                     // Assign DetectBlank (Alt 7a)
    return App.showErrors(form, { fields: { engineerIds: "กรุณาเลือก Tech Engineer อย่างน้อย 1 คน" } });
  }
  try {
    await App.post(`/api/projects/${current.ProjectID}/assignments`, { engineerIds: [...selected] });
    App.toast("มอบหมายสำเร็จ – สถานะ Working และแจ้งเตือน Engineer แล้ว", "ok");
    loadProjectList();
  } catch (err) {
    App.showErrors(form, err);
  }
}

(async () => {
  if (!(await App.page(["PM"]))) return;
  $("skill").onchange = (e) => filterEngineer(e.target.value);
  $("showAll").onclick = () => { $("skill").value = ""; showEngineers(all); };
  form.onsubmit = (ev) => { ev.preventDefault(); assignEngineer(); };
  loadProjectList();
})();
