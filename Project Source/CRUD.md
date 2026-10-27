# ProjectSA – CRUD Matrix & Traceability

Source: `ProjectSA_Revised.drawio.xml` (29 pages) and `ProjectSA_SQL.sql`.
Updated 2026-10-25:
- added **UC08 Close Project**;
- added the features outside the use cases: **F1 Register, F2 Login, F3 Profile, F4 Notification Bell**;
- added `EMPLOYEE.ProfilePicture`.

Updated 2026-10-26: Tech Engineers **type their own skills** on F1 Register and F3 Profile instead of ticking a fixed list. A typed name is matched to an existing SKILL ignoring case (R.5), or added as a new SKILL row (R.6), so F1 and F3 now **create** SKILL.
The same matrix is drawn on the **CRUD Table** page of the drawio file. The old picture is kept below it for reference.

**How to read this file**
- **C / R / U / D** = INSERT / SELECT (FROM or JOIN) / UPDATE / DELETE.
- A number such as `5.2` is **Query 5.2** of that use case. The same number appears in three places:
  - the **Use-Case Narrative With Query** page;
  - the message `5.2 …` on the **Sequence UC0N** page;
  - the link label on the **Collaboration UC0N** page.
- `N.` (no second number) is narrative **Step N**.
- The matrix was built from the SQL itself (every INSERT, UPDATE, FROM and JOIN), not typed by hand.

---

## 1. CRUD matrix (Use Case × Table)

| UC | EMPLOYEE | SKILL | ENGINEER_SKILL | CUSTOMER | PROJECT | PROJECT_ASSIGNMENT | PROJECT_DOCUMENT | TEST_SCHEDULE | CUSTOMER_REQUIREMENT | REQUIREMENT_ATTACHMENT | NOTIFICATION |
|---|---|---|---|---|---|---|---|---|---|---|---|
| UC01 Create Project Profile | R |  |  | CR | C |  |  |  |  |  | C |
| UC02 Store Project Solution Details | R |  |  | R | RU |  | CR |  |  |  | C |
| UC03 Check Project Details | R |  |  | R | R | R | R | R | R | R |  |
| UC04 Assign Engineer to Project | R | R | R | R | RU | CR |  |  |  |  | C |
| UC05 Set Project Test Calendar Notification | R |  |  | R | RU | R |  | CRU |  |  | C |
| UC06 Add Customer New Requirement | R |  |  | R | RU | R |  |  | C | C | C |
| UC07 Approve New Project Requirement | R |  |  | R | RU |  |  | R | RU | R | C |
| UC08 Close Project | R |  |  | R | RU | R |  | RU | R |  | C |
| *F1 Register (outside the UCs)* | CR | CR | C |  |  |  |  |  |  |  |  |
| *F2 Login (outside the UCs)* | R |  |  |  |  |  |  |  |  |  |  |
| *F3 Profile (outside the UCs)* | RU | CR | CRD |  |  |  |  |  |  |  |  |
| *F4 Notification Bell (outside the UCs)* |  |  |  |  | R |  |  |  |  |  | RU |

No use case deletes data. Old data is kept by changing its status instead:
- a replaced test schedule becomes `Cancelled` (UC05 Query 6.1), and a finished one becomes `Done` (UC08 Query 7.1);
- a rejected requirement becomes `Rejected` (UC07 Query 9.1).

The only **D** is F3 Profile. When a Tech Engineer edits their skills, the old ENGINEER_SKILL rows are deleted (P.7) and the new set is inserted with R.4, in one transaction.

---

## 2. Each table: where it comes from and who uses it

| Table | ER Diagram (Revised) | C | R | U |
|---|---|---|---|---|
| EMPLOYEE | EMPLOYEE (T,D) → SALE / PRESALE / PM / TECH_ENGINEER; `Role` column; `ProfilePicture` (2026-10-25) | F1 (R.3) | UC01 (2.1, 2.3); UC02 (10.1); UC03 (4.1, 4.2); UC04 (3.2, 5.1); UC05 (3.2); UC06 (3.1, 3.2); UC07 (3.1); UC08 (3.1, 3.2); F1 (R.2); F2 (L.1); F3 (P.1, P.3) | F3 (P.4, P.5, P.6) |
| SKILL | SKILL | seed data (CCIE, CCNA, Java, Linux, Unix); F1 (R.6); F3 (R.6) when a Tech Engineer types a new skill | UC04 (3.2, 3.3, 5.1); F1 (R.1, R.5); F3 (P.2, R.1, R.5) | — |
| ENGINEER_SKILL | **Possesses** TECH_ENGINEER–SKILL (M:N) | F1 (R.4); F3 (R.4 again after P.7) | UC04 (3.2, 5.1); F3 (P.2) | — (**D**: F3 P.7) |
| CUSTOMER | CUSTOMER, **Owns** (1:M) | UC01 (5.1) | UC01 (2.2); UC02 (1.1, 3.1); UC03 (1.1, 4.1); UC04 (1.1); UC05 (1.1, 3.1); UC06 (1.1, 3.1); UC07 (1.1); UC08 (1.1, 3.1) | — |
| PROJECT | PROJECT; **Creates**, **Designs**, **Manages** (FKs SaleID, PresaleID, PMID) | UC01 (5.2, 6.1) | UC02 (1.1, 3.1); UC03 (1.1, 3.1, 4.1); UC04 (1.1, 3.1, 3.2, 5.1, 10.1); UC05 (1.1, 3.1, 8.1, 9.1); UC06 (1.1, 3.1, 10.1); UC07 (1.1, 11.3); UC08 (1.1, 3.1, 5.1, 8.1); F4 (N.2) | UC02 (8.1, 9.1); UC04 (8.2, 9.1); UC05 (7.1); UC06 (9.1); UC07 (11.3, 11.4); UC08 (6.1) |
| PROJECT_ASSIGNMENT | **AssignedTo** TECH_ENGINEER–PROJECT (M:N, AssignedDate) | UC04 (8.1) | UC03 (1.1, 3.1, 4.2); UC04 (3.2, 5.1, 8.1, 10.1); UC05 (1.1, 3.2, 8.1, 9.1); UC06 (3.2); UC08 (3.2, 8.1) | — |
| PROJECT_DOCUMENT | PROJECT_DOCUMENT, **Stores** (1:M) | UC02 (8.2) | UC02 (3.2); UC03 (4.5, 6.1) | — |
| TEST_SCHEDULE | TEST_SCHEDULE, **Has** (PROJECT 1:M), **Sets** (TECH_ENGINEER 1:M) | UC05 (6.2) | UC03 (4.3); UC05 (3.3); UC07 (11.2); UC08 (3.3) | UC05 (6.1); UC08 (7.1) |
| CUSTOMER_REQUIREMENT | CUSTOMER_REQUIREMENT, **Contains**, **Records** (PM), **Approves** (Sale) | UC06 (8.1) | UC03 (4.4, 4.6, 6.2); UC07 (1.1, 3.1, 5.1, 6.1, 10.1, 11.1, 11.2); UC08 (5.2) | UC07 (9.1) |
| REQUIREMENT_ATTACHMENT | multivalued attribute `AttachmentFile` of CUSTOMER_REQUIREMENT | UC06 (8.2) | UC03 (4.6, 6.2); UC07 (6.2) | — |
| NOTIFICATION | NOTIFICATION, **Triggers** (PROJECT), **Receives** (EMPLOYEE) | UC01 (7.1); UC02 (10.1); UC04 (10.1); UC05 (8.1, 9.1); UC06 (10.1); UC07 (10.1, 11.3); UC08 (8.1) | F4 (N.1, N.2) | F4 (N.3, N.4) |

**Checks**
- Every table in the schema appears in the matrix, and every table has a matching entity, relationship or multivalued attribute in the ER.
- There is no admin screen.
  - EMPLOYEE rows are created by **F1 Register**. Each account has exactly one Role, fixed by `CHECK (Role IN …)`, and F3 Profile can't change it.
  - ENGINEER_SKILL rows come from the skills a Tech Engineer types on Register and on F3 Profile (R.5 / R.6, then R.4).
  - SKILL is the only seed data. It grows when a Tech Engineer types a skill that isn't there yet (R.6); the new skill then appears in the UC04 filter and in the suggestions (R.1).
- NOTIFICATION is now read by **F4 Notification Bell**. N.1/N.2 only show rows whose `SendAt` has passed, so UC05 reminders appear on time. N.3/N.4 set `IsRead`.
- The CRUD Table page's UC01–UC07 cells were checked automatically against the SQL (77 cells) before the new rows were added.

---

## 3. Each use case across all pages

| UC | Use-Case page (actor) | To-Be (Revised) block / lane | Pain point (To-Be legend) | Narrative steps | Sequence & Collaboration objects | ER relationships written | State Diagram transition |
|---|---|---|---|---|---|---|---|
| UC01 | Sale | สร้าง Project Profile (UC01) / Sale | 2 (Project Site) | 1–7 | CreateProject Button, ProjectDetail Form, Project Controller, Project DetectBlank, Project API | Creates, Owns, Triggers, Receives | ● → **NewProject** (CreateProjectProfile) |
| UC02 | Presale | บันทึก Solution + เอกสาร ลงระบบ (UC02) / Presale | 5 | 1–10 | ProjectList Form, SolutionDetail Form, Solution Controller, Solution DetectBlank, File Check, Solution API | Designs*, Stores, Triggers, Receives | NewProject → **WaitingForAssignEngineer** (StoreProjectSolution) |
| UC03 | Sale, Presale, PM, Tech Engineer | Check Project Detail (UC03) ×4 / Presale (after Ⓐ), PM, Tech, Sale | 5 | 1–6 | ProjectList Form, ProjectDetail Page, Project Controller, Project API | — (read only) | no change |
| UC04 | PM | มอบหมาย Engineer ตาม Skill (UC04) / PM | 4 | 1–10 | ProjectList Form, AssignEngineer Form, Assign Controller, Assign DetectBlank, Assign API | AssignedTo, Manages, Triggers, Receives (reads Possesses) | WaitingForAssignEngineer → **Working** (AssignEngineer) |
| UC05 | Tech Engineer | ตั้งนัดทดสอบกับลูกค้า + แจ้งเตือน (UC05) / Tech | 1, 2 (Location) | 1–9 | ProjectList Form, TestSchedule Form, Schedule Controller, Schedule DetectBlank, Schedule API | Sets, Has, Triggers, Receives | Working → **Testing** (SetProjectCalendarTestDate); Testing → Testing (new test date) |
| UC06 | PM | บันทึก Requirement ใหม่ สถานะรออนุมัติ (UC06) / PM | 3 | 1–10 | ProjectList Form, NewRequirement Form, Requirement Controller, Requirement DetectBlank, File Check, Requirement API | Records, Contains, Triggers, Receives | Testing → **WaitingForApprove** (AddCustomerRequirement); WaitingForApprove → WaitingForApprove (another requirement) |
| UC07 | Sale | อนุมัติ Requirement ใหม่? (UC07) / Sale | 3 | 1–11 | ProjectList Form, RequirementList Form, Approve Form, Approve Controller, Approve DetectBlank, Approve API | Approves, Triggers, Receives | WaitingForApprove → **NewProject** (ApproveNewRequirement) or → **Finish** (NotApproveNewRequirement) |
| UC08 | PM | ปิด Project + แจ้งเตือนผู้รับผิดชอบทุกคน (UC08) / PM, between "ผ่านตามความต้องการลูกค้า? Yes" and "จัดทำเอกสาร UAT และ Handover" | — (closes the life cycle) | 1–8 | ProjectList Form, CloseProject Form, Close Controller, Close API | Triggers, Receives (updates PROJECT.Status and TEST_SCHEDULE.Status) | Testing → **Finish** (CloseProject (UC08)) |

\* `PresaleID` is chosen in UC01 (Query 5.2). UC02 fills in the Solution attributes of the project that Presale **Designs**.

**Notifications by use case** (NOTIFICATION.Type → who receives it)

| UC | Type | Recipient |
|---|---|---|
| UC01 | NewProject | Presale in Charge |
| UC02 | WaitingAssign | every PM |
| UC04 | Assigned | each assigned Tech Engineer |
| UC05 | TestSchedule, TestReminder | PM and the project's engineers (the customer gets an email at CUSTOMER.CustomerEmail, which is not stored) |
| UC06 | RequirementPending | Sale in Charge |
| UC07 | RequirementResult | PM who recorded the requirement, plus Presale when at least 1 requirement is approved |
| UC08 | ProjectClosed | everyone in charge of the project: Sale in Charge, Presale in Charge and every assigned Tech Engineer (the PM closes it, so isn't notified) |

All of these are shown by **F4 Notification Bell**: the number of unread notifications on the bell, the latest 20 in a list, and clicking one marks it read and opens UC03 for that project.

---

## 4. How the 29 pages relate

| Page | Role in the chain |
|---|---|
| As-If, Fixed As-If V.2 | The current manual process and the 5 pain points. Each pain point is mapped to a UC in the To-Be (Revised) legend. |
| As-If (Revised) | The same As-Is flow in the To-Be (Revised) layout; red boxes mark where pain points 1–5 happen. |
| To-Be, Clean To-Be | Original To-Be drafts, superseded by To-Be (Revised). |
| To-Be (Revised) | Each purple block is one UC (table §3, column 3). |
| Use-Case | The actors and their associations (§3, column 2). |
| Use-Case Narrative Without Query (Revised) | Steps 1…N for each UC. |
| Use-Case Narrative With Query | The same steps, with Query N.M under each system step. This page is the source of every number in §1–§2. |
| Sequence UC01–UC08 | One message per narrative step / query, in order. Only the main path is shown. |
| Collaboration UC01–UC08 | The same objects and the same numbered messages as the sequence page, grouped by link. They were rebuilt from the sequence pages. |
| ER Diagram(Don't Use This) | Old ER, kept for reference only. |
| ER Diagram (Revised) | The entities and relationships in §2. |
| State Diagram | The `PROJECT.Status` values that the **U** cells on PROJECT write (§3, last column). |
| CRUD Table | §1 drawn as a table, with the query numbers in each cell. |

**Queries that appear only on the With Query page** (alternative paths; each sequence page shows the main path only):
- UC01 6.1: status saved inside Query 5.2.
- UC03 6.2: download a requirement attachment.
- UC07 11.4: every requirement rejected → Finish.

---

## 5. Open item: resolved 2026-10-25

**Testing → Finish now has a use case: UC08 Close Project.** The PM presses "ปิด Project" when the customer has accepted the system with no new requirements. Before closing, the system checks that:
- the project is still Testing;
- this PM manages it;
- no requirement is Pending.

It then sets `Finish`, marks the latest test `Done`, and notifies Sale, Presale and every assigned engineer.

A knock-on fix: **UC03 Query 4.3** now reads `Status IN ('Scheduled', 'Done')`. Otherwise "นัดหมายทดสอบล่าสุด" would go blank once a project is closed. This was changed in the SQL file, the With Query page and Sequence UC03.
