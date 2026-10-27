# ProjectSA – Analysis & Revision Notes

Source: `ProjectSA.drawio.xml` (9 pages, unchanged).
Output: `ProjectSA_Revised.drawio.xml` (29 pages). The original pages keep their content; only the small consistency fixes in §4 were applied to them. UC08 Close Project and the features outside the use cases were added on 2026-10-25 (§5). New pages:

- **Sequence UC08** and **Collaboration UC08** (2026-10-25). They use the same style and object layers as the UC01–UC07 pages.

- **Collaboration UC01 – UC07**: one collaboration (communication) diagram per use case, generated from the same data as the sequence diagrams.
  - It uses the same objects as the sequence page: the actor, `<<Boundary>>` forms, `<<Controller>>` (main controller plus DetectBlank / FileCheck), `<<API>>` and `<<Entity>>` tables. Each pair of objects that exchange messages is joined by one link.
  - Each link lists its messages, with the same numbers as the sequence diagram (N. = step, N.M = query, no number = internal Boundary → Controller → API call) and an arrow showing direction.
  - Calls and numbered step messages are shown; plain return values are on the sequence pages.
  - The pages were rebuilt from the sequence pages, and a check confirms that every collaboration page has exactly the same numbered messages as its sequence page.

- **Sequence UC01 – UC07**: one sequence diagram per use case, in the team's hand-drawn UC01 style. Each has a stick-figure actor, stereotyped objects (`<<Boundary>>` Button / Form, `<<Controller>>` Controller / DetectBlank / FileCheck, `<<API>>`, and the `<<Entity>>` tables), dashed lifelines and grey activation bars. Messages are numbered: calls are solid lines with an open block arrow, and returns are dashed lines with an open arrow. Only returns that carry a processing result are shown (data, IDs, validation or save results). Display returns are left out: anything returned to the user, and "screen ready" returns between forms. A screen's activation bar ends when the user acts again. Messages are numbered by the use case:
- `N.` is narrative Step N.
- `N.M` is a database call and matches Query N.M on the With-Query page.
- Unnumbered messages are the user opening a screen, and returns.

A script checks that every narrative step and every query appears, in step order. The messages follow the revised narratives' steps, and the entity lifelines match the tables used by the queries.

- **As-If (Revised)** (2026-10-27): new page after "Fixed As-If V.2". It redraws the V.2 flow in the To-Be (Revised) layout (same lanes, shapes, fonts and Ⓐ connector), so the two pages can be compared side by side. The steps where the 5 pain points happen are red boxes labelled "(Pain Point N)", the same way To-Be (Revised) marks UCs in purple. The Pain Point list and the "ระบบเดิมทำงานโดย เมล และ เอกสาร" note sit to the right of the lanes; the "วิธีแก้ 3 / 4" notes were left out because they describe the To-Be.
- **To-Be (Revised)**: new page after "Clean To-Be". It fixes the To-Be issues listed below and uses a tighter layout.
- **Use-Case Narrative Without Query (Revised)**: replaces "Use-Case Narrative Without Query".
- **Use-Case Narrative With Query**: the same narratives with SQL Server (T-SQL) queries under each system step. The layout follows the team's UC01 example (title, Main Actor, Describe, Pre/Post, then one Actor Action | System row). The database schema is shown on the same page and also saved as `ProjectSA_SQL.sql`.
- **ER Diagram (Revised)**: new page, placed after the old ER page. It uses Chen notation, the same style as the original ER.

The old ER page ("Don't Use This") is still in the file for reference. The new ER was not built from it.

---

## 1. Page-by-page analysis

### As-If / Fixed As-If V.2
- 5 swimlanes: Sale, จัดซื้อ/Stock, Presale Engineer, PM, Technical Engineer.
- V.2 removes the Outsource branch and adds the Pain Point list and notes. The As-Is process runs on email and paper documents.
- Fixed As-If V.2 and To-Be list the same 5 pain points, and the "แก้ Pain point: N" boxes in To-Be use that numbering.

### To-Be / Clean To-Be
The circled blocks are the system use cases. They map one-to-one to the use-case diagram:

| To-Be block | Lane | Use case |
|---|---|---|
| สร้างไฟล์ Project ใหม่ | Sale | UC01 Create Project Profile |
| ส่ง Solution ที่ถูกจัดเก็บภายใน Project ลง App | Presale | UC02 Store Project Solution Details |
| Check Project Detail (×4) | PM, Tech, Sale | UC03 Check Project Details |
| กำหนดทีม Engineer เข้า Project | PM | UC04 Assign Engineer to Project |
| ตั้ง Calendar นัดหมายการเทสกับลูกค้า | Tech | UC05 Set Project Test Calendar Notification |
| บันทึกรายละเอียดเพิ่มของลูกค้า + ตั้งสถานะรออนุมัติ | PM | UC06 Add Customer New Requirement |
| Sale approve requirement ใหม่ | Sale | UC07 Approve New Project Requirement |

Equipment and stock steps (แจ้งอุปกรณ์, Stock เช็คของ, จัดซื้อสั่งของ) are **not** circled, so they stay manual and are left out of the ER.

Issues found. Items 1–5 are fixed on the **To-Be (Revised)** page; the original To-Be pages are left as they were:
1. **"Sale approve requirment ใหม่" has only one exit.** It goes to ออกแบบ Solution and has no Yes/No labels and no reject path. The State Diagram does have a reject path (NotApproveNewRequirement → Finish).
2. **"ระบบทำงานตรงตามความต้องการลูกค้าไหม" has an unlabeled edge back to "ตั้ง Calendar"**, in addition to Yes and No. The condition for it is unclear.
3. **The Note "วิธีแก้ 3" disagrees with the diagram.** The note says the *Engineer* records the extra requirement and sets the status. The To-Be lane and the use-case diagram both give this to the *PM*. The revised narrative follows the diagram (PM).
4. One edge into "Stock เช็คของภายใน บริษัท" has no source. It is not attached to "แจ้งจัดซื้อ / Stock".
5. The label "กำหนดทีม Engineer เข้า Project ผ่าน" is cut off: it doesn't say what the team is assigned *through*.
6. Notes exist only for วิธีแก้ 3 and 4. Pain points 1, 2 and 5 have no solution notes. The revised page's legend maps every pain point to the use case that fixes it.

**To-Be (Revised): what changed**
- Size: about 1880 × 2150 px, down from about 5100 × 2750. The 5 lanes are kept, and every block sits inside its own lane (the old Start shape was outside the lanes).
- Use-case blocks are filled purple instead of circled, and each is tagged with its UC number.
- Approve requirement (UC07) now has two exits. Yes → connector **A** → back to ออกแบบ Solution. No → UAT / Handover.
- The edge with no condition, from "ตรงตามความต้องการ?" back to the calendar step, is removed. After a customer test, the only paths are pass → UAT, or system problem / new requirement.
- "บันทึกรายละเอียดเพิ่ม" and "ตั้งสถานะรออนุมัติ" are merged into one PM step (UC06), matching the narrative.
- The equipment loop is connected end to end. Stock "ส่งข้อความแจ้งเตือน" becomes the "ครบ" edge back to Sale.
- A note on the page records the remaining gap: no use case sets the status to Finish when the customer has no new requirement.

### Use-Case diagram
- Actors and use cases are consistent with To-Be.
- **Presale → Check project detail is drawn, but the UC03 narrative left Presale out.** This is fixed in the revision.
- Spelling: "Approve_New_Project_Requirment" is now "Requirement" (fixed).

### Use-Case Narrative (original). Problems that were fixed

| UC | Problem in original |
|---|---|
| All | No alternative/exception flows (e.g. what happens when validation fails). Status values were made up per UC (Active/Inactive, "พร้อมทดสอบ", "ดำเนินงาน") and didn't match the State Diagram. |
| UC01 | Steps 1 and 2 were both actor steps, with no system response that shows the form. Sale had to type "Sale in Charge" (the system should fill in the logged-in user). No Project ID or initial status. Presale was never notified. |
| UC02 | Status set to "Active/Inactive". This should be WaitingForAssignEngineer. The pre-condition left out "customer confirmed" and the NewProject status. No date-range or budget checks. PM was never notified. |
| UC03 | **Two "Step 3"s.** Presale was missing as an actor. Only 4 fields were shown: PM, engineers, solution, test schedule and requirements were missing. The pre-condition required a finished Solution, which blocked Sale and Presale from viewing earlier. |
| UC04 | No status change to Working. No notification to the engineers. Engineer workload was not shown (this matters for Pain Point 4). |
| UC05 | **Steps 4–7 were missing** (it jumped from Step 3 to Step 8). Steps 2 and 3 were merged into one line. No location field (Pain Point 2). The customer was in the description but was never notified. The pre-condition status didn't match the State Diagram (it should be Working). |
| UC06 | The status "รออนุมัติ" was set twice (Steps 8 and 10). The project status never changed to WaitingForApprove. The pre-condition said "ดำเนินงาน" but should be Testing. Steps 2 and 4 ran together. |
| UC07 | **Wrong actor in the system steps:** "แสดงเอกสารให้ *PM* ตรวจ" and "ผลการอนุมัติของ *PM*" should both say Sale. Step 4 checked the *Project* status when it should check the *Requirement* status. Steps 2 and 3 were consecutive actor steps. Step 10 sent approved work to Engineer/PM planning, but To-Be and the State Diagram send it back to Presale (→ NewProject). |

### ER Diagram (Don't Use This)
This was ignored, as asked. Why it can't be used as a base:
- Use-case actions are modelled as relationships (Create, CheckProject, SetCalender…).
- There are no Customer, Requirement, Test Schedule or Document entities.
- Address and status are drawn as loose attributes.

### State Diagram
NewProject → WaitingForAssignEngineer → Working → Testing → WaitingForApprove → (NewProject | Finish), plus Testing → Finish.

The revised narratives use exactly these states. Gap: **no use case moves Testing → Finish when there are no new requirements** (the "NoCustomerRequirement" transition). No one is responsible for closing the project in the system. Possible fixes:
- add a small "Close Project / Confirm UAT" use case, or
- let UC05 or UC06 record "no new requirement".

### CRUD Table
The page held a pasted picture whose columns were actors (Sales, TechEngineer, PM, Presale) plus Project. It has been replaced by an editable Use Case × Table matrix built from the SQL, with the query numbers in each cell. The old picture is kept below it. See `CRUD.md` for the matrix and the traceability across all pages.

---

## 2. ER Diagram (Revised): design notes

Notation: **Chen**, in the same style as the original ER:
- rectangles are entities, diamonds are relationships, ellipses are attributes;
- PKs are underlined and lines are labelled 1 / M / N;
- the EMPLOYEE specialization uses open arrows.

**Entities**
- EMPLOYEE, a superclass with subclasses SALE, PRESALE, PM and TECH_ENGINEER.
- CUSTOMER, PROJECT, CUSTOMER_REQUIREMENT, TEST_SCHEDULE, PROJECT_DOCUMENT, NOTIFICATION and SKILL.

**Relationships and where they come from**

| Relationship | Cardinality | Comes from |
|---|---|---|
| SALE – Creates – PROJECT | 1 : M | UC01 |
| CUSTOMER – Owns – PROJECT | 1 : M | UC01 |
| PRESALE – Designs – PROJECT | 1 : M | UC02 |
| PROJECT – Stores – PROJECT_DOCUMENT | 1 : M | UC02, UC03 download |
| PM – Manages – PROJECT | 1 : M | UC04 (the assigning PM becomes PM of the project) |
| TECH_ENGINEER – AssignedTo – PROJECT (attr. AssignedDate) | M : N | UC04 |
| TECH_ENGINEER – Possesses – SKILL | M : N | UC04 filter; Note วิธีแก้ 4 |
| TECH_ENGINEER – Sets – TEST_SCHEDULE | 1 : M | UC05 |
| PROJECT – Has – TEST_SCHEDULE | 1 : M | UC05 |
| PM – Records – CUSTOMER_REQUIREMENT | 1 : M | UC06 |
| SALE – Approves – CUSTOMER_REQUIREMENT | 1 : M | UC07 |
| PROJECT – Contains – CUSTOMER_REQUIREMENT | 1 : M | UC06, UC07 |
| PROJECT – Triggers – NOTIFICATION | 1 : M | Notifications in UC01, 02, 04, 05, 06, 07 |
| EMPLOYEE – Receives – NOTIFICATION | 1 : M | Same notifications |

**Design decisions (differences from the old ER)**
- **Use cases are not relationships.** The old ER drew CheckProject, SetCalendar and similar as diamonds.
  - UC03 Check Project Details only reads data, so it has no relationship.
  - Each relationship above records a fact that a use case stores.
- **The specialization is (T,D), total and disjoint.** Every employee has exactly one role. The old ER marked it (P,O).
- **Composite attributes:**
  - Name = FirstName + LastName.
  - Period = StartDate + EndDate.
  - Solution = Objective + EstimatedBudget + SolutionDetail. These are the fields saved in UC02.
- **Multivalued attribute:** CUSTOMER_REQUIREMENT.AttachmentFile, for the UC06 attachments.
- **There is no "Uploads" relationship.** The uploader can be worked out from the data: Solution documents always come from the project's Presale, and requirement attachments come from the PM who recorded the requirement.
- **Equipment and stock are not included**, because the To-Be keeps that work outside the system.
- **Customer emails (UC05) are not stored.** They go to CUSTOMER.CustomerEmail; NOTIFICATION only holds in-app messages to employees.

---

## 3. SQL queries (With Query page and `ProjectSA_SQL.sql`)

**Schema mapping (ER → tables)**
- The EMPLOYEE (T,D) specialization becomes one EMPLOYEE table with a `Role` column.
- The M:N relationships become PROJECT_ASSIGNMENT and ENGINEER_SKILL.
- The multivalued AttachmentFile becomes REQUIREMENT_ATTACHMENT.
- The composites (Name, Period, Solution) are stored as flat columns.

**Conventions**
- `@Variables` are screen inputs or the logged-in user (`@LoginID`, `@Role`).
- `Query N.M` belongs to System Step N.

**How the queries were tested**
- All 60 queries were translated to SQLite and run through one complete project:
  - create, solution, assign and test;
  - round 1: 2 requirements, one approved and one rejected → NewProject;
  - redo solution, reassign, re-test;
  - round 2: rejected → Finish.
- The test checked every status change against the State Diagram, notification recipients, reminder time, no duplicate engineer assignment, and the old schedule being cancelled.

**Problems found by the test, and fixed**
- **UC06:** after the first new requirement the project becomes WaitingForApprove, so a second requirement from the same customer test was blocked. UC06 now also accepts WaitingForApprove (pre-condition, Step 1 and Query 1.1 updated). The State Diagram now shows this as a WaitingForApprove → WaitingForApprove self-loop.
- **UC03 Query 4.1:** before a PM is assigned, `CONCAT` returned `' '` instead of NULL for the PM name. It now returns NULL.
- **UC07 Step 11 ("which round?"):** only requirements recorded after the latest test schedule are counted. This way an approval from an earlier round doesn't send the project back to NewProject again.

---

## 4. Consistency fixes (2026-10-17)

A page-by-page check found the following problems. All were fixed in `ProjectSA_Revised.drawio.xml`.

| Page | Problem | Fix |
|---|---|---|
| Collaboration UC01–UC07 | Out of date after the sequence pages gained Controller and API objects. Object names and message targets differed. UC03 had 6.2 and UC07 had 11.4, which the sequence pages don't show. | Rebuilt from the sequence pages: same objects, same numbered messages. |
| Narrative Without Query, UC01 | Step 4.5 "เช็คว่ากรอก PM in Charge": there is no PM field in Step 3, and the PM is set in UC04. Step 4.6 was on the same line. | Removed the PM check, so Project Site is now 4.5. This matches the With Query page. |
| Sequence UC01 | "ตรวจสอบความถูกต้องของข้อมูล (4.1–4.6)" | Changed to (4.1–4.5). |
| Both narratives, UC02 Step 8 | Said the system saves "ผู้อัปโหลด" (the uploader), but PROJECT_DOCUMENT has no uploader column. | The text now says it saves the document type and upload time, which matches Query 8.2. |
| State Diagram | Missing Testing → Testing (UC05 new test date) and WaitingForApprove → WaitingForApprove (UC06). Misspelled "Calender" / "Requirment". | Added both self-loops and fixed the spelling. |
| To-Be (Revised) | No Presale "Check Project Detail (UC03)", although the Use-Case diagram and the UC03 narrative include Presale. | Added a Presale UC03 block between Ⓐ and "ออกแบบ Solution". |
| ER Diagram (Revised) | Two relationships were both named "Has". | TECH_ENGINEER–SKILL renamed to **Possesses**, and the legend updated. |
| As-If, Fixed As-If V.2, To-Be, Clean To-Be | One or two arrows on each page started on a box but weren't attached to it. | Attached them to "เริ่ม Project" / "แจ้งจัดซื้อ / Stock". |
| Use-Case | "Approve_New_Project_Requirment" | Changed to "Requirement". |
| CRUD Table | Columns were actors, not tables. It contradicted the SQL (CreateProject = U, CheckProject blank). | Replaced as described in §1 and in `CRUD.md`. |

This gap was closed on 2026-10-25 by UC08 (§5).

---

## 5. UC08 Close Project and the features outside the use cases (2026-10-25)

**The user's decisions**
- The PM gets a "close project" button. Closing notifies everyone in charge of the project.
- The app has a simple notification bell.
- The app has a profile page where users edit their information and profile picture.
- Each account has exactly one role.

**UC08 Close Project (PM): Testing → Finish**

| Page | Change |
|---|---|
| To-Be (Revised) | New PM block **"ปิด Project + แจ้งเตือนผู้รับผิดชอบทุกคน (UC08)"** between "ผ่านตามความต้องการลูกค้า? Yes" and "จัดทำเอกสาร UAT และ Handover". UAT/Handover and "ส่งมอบ Project" moved down one row, and the lanes grew by 110 px. UC07 "No" (every requirement rejected, so the project is already Finish) still goes straight to UAT, on its own path. The legend says UC01–UC08, and its old note about the missing close step now describes UC08. |
| Use-Case | New ellipse **Close Project**, labelled UC8, linked to PM. The system boundary was extended so it's inside. |
| Both narrative pages | UC08 table: Steps 1–8, alternative flows 4a (PM cancels the confirmation) and 5a (the project isn't Testing, or a requirement is still Pending). The With Query version has Queries 1.1, 3.1–3.3, 5.1, 5.2, 6.1, 7.1, 8.1. |
| Sequence UC08 (new) | PM → ProjectList Form / CloseProject Form → Close Controller → Close API (`GET /api/projects?status=Testing&pm=me`, `GET /api/projects/{id}/close-form`, `POST /api/projects/{id}/close`) → PROJECT, PROJECT_ASSIGNMENT, TEST_SCHEDULE, CUSTOMER_REQUIREMENT, NOTIFICATION. It has no DetectBlank, because every check in Step 5 is a database check (5.1, 5.2) made inside the POST. |
| Collaboration UC08 (new) | Same objects and messages as the sequence page (checked automatically). |
| State Diagram | The Testing → Finish label changed from "NoCustomerRequirement" to **"CloseProject (UC08)"**. |
| CRUD Table | UC08 row (PROJECT RU, TEST_SCHEDULE RU, CUSTOMER_REQUIREMENT R, NOTIFICATION C, …). |

The notification goes to Sale in Charge, Presale in Charge and every assigned Tech Engineer (`Type = 'ProjectClosed'`). The PM is the one closing the project, so isn't notified. The latest `Scheduled` test becomes `Done`; this status existed already but nothing used it before.

**Knock-on fix.** UC03 Query 4.3 ("นัดหมายทดสอบล่าสุด") read only `Scheduled` tests, so after UC08 the section would go blank. It now reads `Status IN ('Scheduled', 'Done')`, in the SQL file, the With Query page and Sequence UC03.

**Features outside the use cases** (SQL only; no narrative or sequence pages; see `CLAUDE.md` §6.0)

| Feature | Queries | Notes |
|---|---|---|
| F1 Register | R.1–R.6 | One account = one role (radio button; `CHECK (Role IN …)`). Username and email must be unique. A Tech Engineer **types** at least 1 skill (2026-10-26): R.5 matches an existing skill ignoring case, R.6 adds a new one, R.4 links it. |
| F2 Login | L.1 | No role choice at login: the role comes from the account. The password is checked against its BCrypt hash. |
| F3 Profile | P.1–P.7 | Edit name, email, phone, password and **profile picture** (JPG/PNG ≤ 5 MB → new column `EMPLOYEE.ProfilePicture`, also added to the ER). Tech Engineers can edit their skills, typed the same way as on Register (P.7, then R.5 / R.6 + R.4). Role and Username can't be changed. |
| F4 Notification Bell | N.1–N.4 | Unread count, latest 20, mark one / all as read. Only rows whose `SendAt` has passed are shown, so UC05 reminders appear on time. |

**Tested:** all 87 query entries ran through one life-cycle test on SQLite: 69 for the use cases (UC01 6.1 has no SQL of its own) and 18 for the features. The test covers:
- register → login → UC01–UC07 in 2 rounds;
- a second project closed by UC08;
- profile edits;
- the bell.

All checks passed. The SQL on the With Query page and on the sequence messages matches `ProjectSA_SQL.sql`.

---

## 6. Implementation (2026-10-25)

The program is in `projectsa-app/` (Java 21, Spring Boot 3.5.16, H2 in SQL Server mode). `CLAUDE.md` §3 and §5 describe its layout and how to run it.

**The program runs the design's SQL file directly.** Maven copies `ProjectSA_SQL.sql` into the jar. At startup the app creates the schema from it, and every use-case step runs its `Query N.M` by ID, so the code contains no SQL of its own. `@Param` is bound as `:Param`, and `SET @x = SCOPE_IDENTITY()` becomes a JDBC generated key.

**Design changes made while building**

| Change | Why | Where it was updated |
|---|---|---|
| UC05 Query 8.1 / 9.1 now use `LEFT(CONCAT(@StartTime, ''), 5)` and `CAST(CONCAT(@TestDate, ' ', LEFT(CONCAT(@StartTime, ''), 8)) AS DATETIME)`, replacing `CONVERT(…, 108)` and adding a date to a time. | H2 rejected the two T-SQL-only forms. The new text works on both H2 and SQL Server and gives the same values (message "10:00"; reminder = test time − RemindBefore). | SQL file, With Query page, Sequence UC05, `tools/test_sql.py` |
| UC07 `GET /api/requirements/{id}?project={ProjectID}`, and `projectId` in the decision body. | No design query maps a RequirementID to its project. The server checks ownership with Query 1.1 + 3.1 instead of new SQL. | Sequence UC07, Collaboration UC07, `CLAUDE.md` §3 |
| Tech Engineer skills are typed, not ticked (2026-10-26). New Query R.5 (find a skill by name, ignoring case) and R.6 (add a new SKILL row). F1 and F3 now create SKILL. | A fixed list of seeded skills can't cover every engineer's skills. Matching ignoring case keeps "java" and "Java" as one skill, so the UC04 filter stays clean; the page also suggests existing skills while typing. | SQL file (R.5, R.6), CRUD Table page, `CRUD.md`, `CLAUDE.md` §3–§6.0, both life-cycle tests |
| Requirement attachments are stored as `uuid-originalName`. | REQUIREMENT_ATTACHMENT has only the path, so this keeps the original file name for display. | Code only (`FileStore.saveNamed`) |

**Tests (all pass)**
- `tools/check_docs.py` and `tools/test_sql.py`.
- 20 JUnit tests:
  - all 87 queries prepared and run on H2;
  - one MockMvc test per use case and per feature, covering the alternative flows and the 403 for a wrong role;
  - `LifecycleAcceptanceTest`, which runs the whole story through the HTTP API.

