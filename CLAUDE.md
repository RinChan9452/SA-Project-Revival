# CLAUDE.md – ProjectSA (Project Management Program)

This repo holds the system-analysis design for a **Project Management Program**, used by an IT-solution company that sells and installs systems for customers. The design is finished; there is no code yet. Read this file before you write any code. It tells you where the design lives, how the program is structured, and what each feature must do.

## 1. What the program does

Today Sale, Presale, PM and Technical Engineers pass project information by email and paper. The program replaces that with one shared project record. It solves 5 pain points (As-If page):

| # | Pain point | Fixed by |
|---|---|---|
| 1 | Meeting / test appointments with the customer get mixed up | UC05 |
| 2 | The work site doesn't match the appointment | UC01 (Project Site), UC05 (Location) |
| 3 | The project is delayed when the customer adds requirements | UC06, UC07 |
| 4 | The PM doesn't know each engineer's skills | UC04 (filter by Skill) |
| 5 | Project details get lost when work is handed over | UC02, UC03 |

**Build these 8 use cases plus 4 features outside the UCs** (§6.0: F1 Register, F2 Login, F3 Profile, F4 Notification Bell), and nothing else. UC08 and F1–F4 were added on 2026-10-25, and their design is complete (diagrams, SQL, CRUD).

| UC | Name | Actor |
|---|---|---|
| UC01 | Create Project Profile | Sale |
| UC02 | Store Project Solution Details | Presale Engineer |
| UC03 | Check Project Details | Sale, Presale, PM, Tech Engineer |
| UC04 | Assign Engineer to Project | PM |
| UC05 | Set Project Test Calendar Notification | Tech Engineer |
| UC06 | Add Customer New Requirement | PM |
| UC07 | Approve New Project Requirement | Sale |
| UC08 | Close Project | PM |

**Out of scope.** The To-Be keeps these steps manual:
- equipment and stock (แจ้งอุปกรณ์, Stock เช็คของ, จัดซื้อ);
- outsourcing;
- installing on site;
- writing the UAT / Handover documents.

## 2. Where the design lives (source of truth)

All files are in `Project Source/`.

| File / page | Use it for |
|---|---|
| `ProjectSA_Revised.drawio.xml` | **The design. Always use this file.** It has 29 pages; open them in draw.io. |
| · As-If (Revised) | The current manual process, in the To-Be (Revised) layout; the red boxes are where the 5 pain points happen. |
| · To-Be (Revised) | The business flow, and where each UC sits in it. |
| · Use-Case | Actors ↔ use cases. |
| · Use-Case Narrative Without Query (Revised) | Pre/post-conditions, Steps 1…N, alternative flows. **This is the behaviour spec.** |
| · Use-Case Narrative With Query | The same steps, with the SQL (`Query N.M`) under each system step. |
| · Sequence UC01–UC08 | The class/layer design: Boundary → Controller → API → Entity, method names and REST endpoints (§3). These pages show the **main path only**. |
| · Collaboration UC01–UC08 | The same messages as the sequence pages, grouped by link. |
| · ER Diagram (Revised) | Chen ER: the entities and relationships. |
| · State Diagram | The life cycle of `PROJECT.Status` (§4). |
| · CRUD Table | Use case × table matrix. |
| `ProjectSA_SQL.sql` | SQL Server (T-SQL) schema plus every query: UC01–UC08 (69, matching the With Query page exactly) and the features F1–F4 (18, at the end of the file: R.n, L.n, P.n, N.n). **Build the database from this file.** |
| `CRUD.md` | The CRUD matrix as text, plus a table tracing each UC across every page. |
| `../tools/check_docs.py` | Checks the design files agree: the drawio parses with no duplicate or broken ids; the With Query page has the same SQL as `ProjectSA_SQL.sql`; every SQL message on the sequence pages matches. Usage: `python tools/check_docs.py` (exit code 0 = OK). |
| `../tools/test_sql.py` | Runs every query in `ProjectSA_SQL.sql` through the whole life cycle on in-memory SQLite (it translates the T-SQL). It needs only Python 3.10+. Usage: `python tools/test_sql.py`. Exit code 0 means all 87 queries ran and every check passed. |
| `ProjectSA_Analysis.md` | Why the design is the way it is, and the problems that were fixed. |
| `ProjectSA.drawio.xml` | The team's original diagrams. **Read-only. Never edit it.** |
| `../projectsa-app/` | The program (Spring Boot). Layout, how to run it and the demo accounts are in §5. |

**Do not use:**
- the page "ER Diagram(Don't Use This)";
- the pages "To-Be" and "Clean To-Be" (replaced by To-Be (Revised));
- the original narratives in `ProjectSA.drawio.xml`.

**Numbering that every artifact shares:**
- `Step N` is narrative step N.
- `Query N.M` is the SQL for system step N.
- The sequence messages use the same `N.` / `N.M` numbers.

Use these numbers in code comments, commit messages and tests, e.g. `// UC04 Step 8 / Query 8.1`.

## 3. Program structure (from the sequence diagrams)

Every UC uses the same 4 layers:

```
Actor → <<Boundary>> Form / Page / Button     (UI: shows data, collects input)
      → <<Controller>> <X> Controller         (one per UC: calls validators, then the API)
           ├─ <<Controller>> <X> DetectBlank  (field validation = narrative "ตรวจสอบความถูกต้อง")
           └─ <<Controller>> File Check       (file type / size rules, UC02 + UC06)
      → <<API>> <X> API                       (REST endpoint, runs the Query N.M statements)
      → <<Entity>> tables                     (SQL Server, schema in ProjectSA_SQL.sql)
```

| UC | Boundary | Controller methods | API endpoints |
|---|---|---|---|
| UC01 | CreateProject Button, ProjectDetail Form | `openCreateForm()`, `submitProject(formData)` | `GET /api/projects/create-form`, `POST /api/projects` |
| UC02 | ProjectList Form, SolutionDetail Form | `loadProjectList()`, `selectProject(id)`, `validateSolution(formData)`, `saveSolution(formData, files)` | `GET /api/projects?status=NewProject`, `GET /api/projects/{id}/solution`, `POST /api/projects/{id}/solution` |
| UC03 | ProjectList Form, ProjectDetail Page | `loadProjectList()`, `selectProject(id)`, `downloadDocument(docId)` | `GET /api/projects[?keyword=]`, `GET /api/projects/{id}`, `GET /api/projects/{id}/documents/{docId}`, `GET /api/projects/{id}/requirements/{reqId}/attachment?file=` (Q6.2, alternative path) |
| UC04 | ProjectList Form, AssignEngineer Form | `loadProjectList()`, `selectProject(id)`, `filterEngineer(skillId)`, `assignEngineer(id, engineerIds)` | `GET /api/projects?status=WaitingForAssignEngineer`, `GET /api/projects/{id}/assign-form`, `GET /api/engineers?skill={skillId}`, `POST /api/projects/{id}/assignments` |
| UC05 | ProjectList Form, TestSchedule Form | `loadProjectList()`, `selectProject(id)`, `saveSchedule(formData)` | `GET /api/projects?assigned=me`, `GET /api/projects/{id}/schedule-form`, `POST /api/projects/{id}/schedules` |
| UC06 | ProjectList Form, NewRequirement Form | `loadProjectList()`, `selectProject(id)`, `validateRequirement(formData)`, `submitRequirement(formData, files)` | `GET /api/projects?status=Testing,WaitingForApprove`, `GET /api/projects/{id}/requirement-form`, `POST /api/projects/{id}/requirements` |
| UC07 | ProjectList Form, RequirementList Form, Approve Form | `loadProjectList()`, `selectProject(id)`, `selectRequirement(reqId)`, `submitDecision(reqId, decision)` | `GET /api/projects?status=WaitingForApprove`, `GET /api/projects/{id}/requirements?status=Pending`, `GET /api/requirements/{reqId}?project={id}`, `PUT /api/requirements/{reqId}/decision` (body `{projectId, decision, rejectReason}`) |

UC07's requirement endpoints carry the project ID. No design query maps a `RequirementID` to its project, so the server checks ownership with Query 1.1 (my projects) and Query 3.1 (that project's pending requirements). The Sequence and Collaboration UC07 labels show this (updated 2026-10-25).
| UC08 | ProjectList Form, CloseProject Form | `loadProjectList()`, `selectProject(id)`, `closeProject(id)` | `GET /api/projects?status=Testing&pm=me`, `GET /api/projects/{id}/close-form`, `POST /api/projects/{id}/close` |

UC08 has no DetectBlank: every check in Step 5 is a database check (Q5.1, Q5.2), done inside the POST.

The features outside the UCs (§6.0) have no diagrams. Their endpoints are implemented as listed here; CLAUDE.md is their only description:

| Feature | Screen | API endpoints | SQL |
|---|---|---|---|
| F1 Register | register.html | `GET /api/skills` (suggestions while typing), `POST /api/register` | R.1–R.6 |
| F2 Login | login.html | `POST /api/login`, `POST /api/logout` | L.1 |
| F3 Profile | profile.html | `GET /api/profile`, `PUT /api/profile`, `PUT /api/profile/password`, `POST /api/profile/picture`, `PUT /api/profile/skills` | P.1–P.7 (+ R.1, R.4–R.6) |
| F4 Notification Bell | 🔔 in the header of every page | `GET /api/notifications/unread-count`, `GET /api/notifications`, `PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all` | N.1–N.4 |

Keep these layer names and endpoints, so the code can be traced back to the diagrams.

### Tech stack (decided 2026-10-25: the user chose Java and asked Claude to pick the rest)

The program is a **local demo**: it runs on one machine, has no email, and doesn't need SQL Server installed.

| Layer | Choice | Why |
|---|---|---|
| Language / build | **Java 21 + Maven** (both already installed: JDK 21.0.7, Maven 3.9.10) | No extra installs. Add the Maven wrapper (`mvnw`) so the version is pinned. |
| Framework | **Spring Boot 3** (Web, JDBC, Validation) | One runnable jar, with REST controllers matching §3's endpoints. |
| Database | **H2, file mode, `MODE=MSSQLServer`** (e.g. `jdbc:h2:file:./data/projectsa;MODE=MSSQLServer`) | Embedded, so there's nothing to install. Data survives a restart. The H2 console (`/h2-console`) shows the tables during the demo. MSSQLServer mode accepts most of the T-SQL in `ProjectSA_SQL.sql` (see the compatibility rule below). |
| Data access | **`NamedParameterJdbcTemplate`**, with the narrative SQL written out by hand. **No JPA/Hibernate.** | Each repository method runs exactly one `Query N.M`, so the code reads like the With Query page. `@Param` in the docs becomes `:Param` in code, with parameters always bound, never joined into the string. |
| UI | **Static HTML + vanilla JavaScript** in `src/main/resources/static`, with no front-end build | One page per `<<Boundary>>`. The JS functions are the `<<Controller>>` methods (`openCreateForm()`, `submitProject()` …). The DetectBlank / FileCheck rules run in JS **and again** on the server. |
| Login | HttpSession plus BCrypt password hashing (`spring-security-crypto` only) | Enough for a demo. Every API checks the session's role (§4). |
| Files | Saved to the local `./uploads/` folder; the table stores the path | As in §4. |
| Tests | JUnit 5 + Spring Boot Test against a fresh H2 database | Port the life-cycle test in §6.8. |

**SQL compatibility (checked 2026-10-25).** All 87 queries now run on H2 MSSQLServer mode (`SqlCompatibilityTest`, `SqlLifecycleH2Test`).
- H2 accepts as written: `NVARCHAR(MAX)`, `STRING_AGG`, `TOP n`, `GETDATE()`, `N'…'`, `CONCAT`, `DATEADD`, `IDENTITY(1,1)`.
- Only UC05 8.1 / 9.1 had to change. `CONVERT(VARCHAR(5), @StartTime, 108)` became `LEFT(CONCAT(@StartTime, ''), 5)`, and `CAST(@TestDate AS DATETIME) + CAST(@StartTime AS DATETIME)` became `CAST(CONCAT(@TestDate, ' ', LEFT(CONCAT(@StartTime, ''), 8)) AS DATETIME)`. Both forms work on SQL Server too, and the change was made in the SQL file, the With Query page and Sequence UC05.
- `SET @x = SCOPE_IDENTITY()` is never sent to the database. `QueryCatalog` drops it, and `Sql.insert()` returns the identity through JDBC generated keys.

**How the app runs the design's SQL.** Maven copies `Project Source/ProjectSA_SQL.sql` into the jar (`classpath:sql/`). `common/QueryCatalog` parses the schema and every `Query N.M` at startup. `common/Sql` runs a query by its design ID, e.g. `sql.update("UC04", "8.1", params)`, with `@Param` bound as `:Param`. **The Java code contains no SQL of its own**, so editing the SQL file changes the app. Every query is logged as `Query UC04 8.1`. If a query fails, rewrite it in SQL that works on **both** H2 and SQL Server, then copy the new text into `ProjectSA_SQL.sql` and the With Query page, so the docs and the code always contain the same SQL (§8).

## 4. Data and business rules (apply to every UC)

**Tables** (`ProjectSA_SQL.sql`):
- EMPLOYEE (`Role` = Sale / Presale / PM / TechEngineer, **one role per account**; `ProfilePicture` = path, F3)
- SKILL, ENGINEER_SKILL
- CUSTOMER, PROJECT
- PROJECT_ASSIGNMENT, TEST_SCHEDULE
- CUSTOMER_REQUIREMENT, REQUIREMENT_ATTACHMENT
- PROJECT_DOCUMENT, NOTIFICATION

**PROJECT.Status** (State Diagram). Only these transitions are allowed:

| From | To | Set by |
|---|---|---|
| (new) | NewProject | UC01 Query 5.2 |
| NewProject | WaitingForAssignEngineer | UC02 Query 9.1 |
| WaitingForAssignEngineer | Working | UC04 Query 9.1 |
| Working | Testing | UC05 Query 7.1 |
| Testing | Testing | UC05 again (new test date after a fix) |
| Testing | WaitingForApprove | UC06 Query 9.1 |
| WaitingForApprove | WaitingForApprove | UC06 again (another requirement, same round) |
| WaitingForApprove | NewProject | UC07 Query 11.3 (at least 1 approved this round) |
| WaitingForApprove | Finish | UC07 Query 11.4 (all rejected) |
| Testing | Finish | UC08 Query 6.1 (the PM presses "ปิด Project"; the latest test becomes `Done` in Query 7.1) |

Other statuses:
- `CUSTOMER_REQUIREMENT.Status`: Pending (รออนุมัติ) / Approved / Rejected.
- `TEST_SCHEDULE.Status`: Scheduled / Cancelled / Done.

**Rules:**
- **No use case deletes data.** Old data is kept by changing its status:
  - a replaced test schedule becomes `Cancelled`, and a finished one becomes `Done` (UC08);
  - a rejected requirement becomes `Rejected`.

  The only DELETE is F3 Profile P.7: a Tech Engineer replacing their own skills. It deletes the old ENGINEER_SKILL rows and inserts the new ones in one transaction.
- **Each list shows only the projects the logged-in user is allowed to see:**
  - Sale / Presale: the projects they are in charge of;
  - PM: every project in UC03 and UC04; in UC06, the projects they manage;
  - Tech Engineer: the projects they are assigned to.
- **Every write endpoint checks the status again on the server.** Don't trust the list the user was shown. For example, the UC07 update uses `WHERE Status = 'Pending'`.
- **Each POST/PUT runs in one database transaction:** the data rows, the status update and the NOTIFICATION rows are saved together or not at all.
- **Files:**
  - UC02 accepts PDF only, at most 50 MB per file, with at least 1 Solution document.
  - UC06 accepts PDF / JPG / PNG, at most 50 MB per file, and attachments are optional.
  - The F3 profile picture is JPG / PNG, at most 5 MB, saved under `./uploads/profile/`.
  - Files are stored outside the database; the tables keep the path.
- **Notifications** (NOTIFICATION.Type → who receives it):

  | UC | Type | Recipient |
  |---|---|---|
  | UC01 | NewProject | Presale in Charge |
  | UC02 | WaitingAssign | every PM |
  | UC04 | Assigned | each assigned engineer |
  | UC05 | TestSchedule (now) and TestReminder (`SendAt` = test time − RemindBefore minutes) | PM and the project's engineers |
  | UC06 | RequirementPending | Sale in Charge |
  | UC07 | RequirementResult | the PM who recorded it, and Presale when ≥ 1 is approved |
  | UC08 | ProjectClosed | everyone in charge: Sale in Charge, Presale in Charge and every assigned Tech Engineer |

  Users see notifications through the **F4 bell** (§6.0). It shows only rows whose `SendAt` has passed, so UC05 reminders appear on time without a separate job.

  **The demo sends no email** (decided 2026-10-25). The narratives' email steps are the customer email in UC05 Step 8 and the Sale email in UC06 Step 10. Leave them out and write a log line instead (`[EMAIL SKIPPED] to=… subject=…`). In-app NOTIFICATION rows are still written.
- **The UC07 "round":** only requirements created after the latest `TEST_SCHEDULE.CreatedDate` count (Query 11.2). This way an approval from an earlier round doesn't reopen the project.
- **Things filled in automatically from the logged-in user:**
  - Sale in Charge (UC01);
  - PM of the project (UC04 Query 8.2);
  - the requirement's CreatedBy (UC06) and ReviewedBy (UC07).
- **Who creates employees and skills.** There is no admin screen (decided 2026-10-25):
  - EMPLOYEE rows come from **F1 Register**. The user picks **exactly one** role (radio buttons); the role can't be changed later.
  - ENGINEER_SKILL rows come from the skills a Tech Engineer **types** on Register (shown when the role is Tech Engineer), and later on F3 Profile (decided 2026-10-26).
  - SKILL starts as seed data (`data.sql`): CCIE, CCNA, Java, Linux, Unix. A typed skill is matched to an existing one ignoring case (R.5); if there is none it becomes a new SKILL row (R.6), which then shows in the UC04 filter and in the suggestions.

  The CRUD Table page and `CRUD.md` have rows for F1–F4.

## 5. Build order

1. [x] **Tech stack and design decisions** (§3, §7). Every question asked so far has been answered (2026-10-25).
2. [x] **Project skeleton + database** (done 2026-10-25):
   - [x] Spring Boot 3.5.16 project with the Maven wrapper in `projectsa-app/`;
   - [x] on first start, the schema is created from `ProjectSA_SQL.sql` and SKILL is seeded from `data.sql` (`common/DatabaseConfig`);
   - [x] **all 87 queries run on H2**: `SqlCompatibilityTest` prepares every statement, and `SqlLifecycleH2Test` (the Java port of `tools/test_sql.py`) runs the whole life cycle. UC05 8.1/9.1 were rewritten to work on both databases (§3).
3. [x] **F1 Register + F2 Login (§6.0)** (done 2026-10-25): `auth/` package plus `login.html` and `register.html`. Demo accounts `sale`, `presale`, `pm`, `tech1`, `tech2` (password `demo1234`) are created on first start when `app.demo-users=true`. Tested by `AuthApiTest`, `DemoUsersTest` and a smoke test of the packaged jar.
4. [x] **Shared pieces** (done 2026-10-25):
   - [x] the page header with the user's name, profile picture and the **F4 bell** (`App.header()` in `js/app.js`, `js/bell.js`, `notification/NotificationApi`);
   - [x] the project list component, used as each UC's ProjectList Form (`App.projectList()`; each UC's API does the status/role filtering with its Query 1.1);
   - [x] file upload to `./uploads/` plus File Check (`common/FileStore`, `common/FileCheck`). Only `/uploads/profile/**` is public; documents are served by UC03 after its access check;
   - [x] notification writer: not needed as a separate class. Every UC writes its notifications with its own design query (e.g. UC01 7.1, UC08 8.1);
   - [x] email stub that only logs (`common/EmailStub`).
5. [x] **Use cases, in life-cycle order:** UC01 → UC02 → UC03 → UC04 → UC05 → UC06 → UC07 → UC08 (done 2026-10-25). One package per UC (`ucNN/`: `<X>Api`, `<X>Service`, `<X>DetectBlank`) plus `static/ucNN.html` + `js/ucNN.js`. One MockMvc test per UC (`ucNN/*ApiTest`) covers the steps, the alternative flows and the 403 for a wrong role.
6. [x] **F3 Profile** (§6.0) (done 2026-10-25): `profile/` package plus `profile.html`; tested by `ProfileApiTest`.
7. [x] **The full life-cycle test (§6.8)** (done 2026-10-25): `LifecycleAcceptanceTest` drives the whole story through the HTTP API on a fresh database.
8. [x] **Docs stay in sync (§8)** (checked 2026-10-25): `tools/check_docs.py` OK and `tools/test_sql.py` 87/87. The UC05 SQL rewrite is in all three SQL copies, the UC07 endpoint labels are updated, and `ProjectSA_Analysis.md` §6 records the implementation. Keep this box true: re-run the three checks after every change.

**Project layout** (as built, 2026-10-25):
```
projectsa-app/                      ← Maven project, next to "Project Source/"
  pom.xml, mvnw, mvnw.cmd             pom copies ../Project Source/ProjectSA_SQL.sql into classpath:sql/
  src/main/java/com/projectsa/
      common/      QueryCatalog + Sql (run a design query by id), DatabaseConfig (schema + SKILL seed on first start),
                   Auth (session / 401 / 403), ProjectListApi (GET /api/projects → each UC's Query 1.1),
                   FileCheck, FileStore, EmailStub, ApiException / ApiErrors, Menu, Check
      auth/        F1 Register + F2 Login (+ DemoUsers)       profile/       F3 Profile
      notification/ F4 Notification Bell
      uc01/ … uc08/ one package per use case: <X>Api (<<API>>, REST), <X>Service (@Transactional, runs the
                    Query N.M ids in narrative order), <X>DetectBlank (field rules; UC03 / UC08 have none)
  src/main/resources/
      application.properties        H2 file DB ./data/projectsa (MSSQLServer mode), h2-console, ./uploads, demo users
      data.sql                      SKILL seed (the schema itself comes from ProjectSA_SQL.sql)
      static/                       login, register, index, profile, uc01 … uc08 .html; js/app.js (header, API,
                                    project list), js/bell.js (F4), js/ucNN.js = the <<Controller>> functions
  src/test/java/com/projectsa/      SqlCompatibilityTest, SqlLifecycleH2Test, one *ApiTest per UC / feature,
                                    LifecycleAcceptanceTest (§6.8)
```
**Run:** `cd projectsa-app` then `./mvnw spring-boot:run` (Windows: `mvnw.cmd spring-boot:run`), and open `http://localhost:8080`.
- Demo accounts: `sale`, `presale`, `pm`, `tech1`, `tech2`, all with password `demo1234`.
- H2 console: `http://localhost:8080/h2-console` (JDBC URL from `application.properties`, user `sa`, empty password).
- To reset the demo, stop the app and delete `projectsa-app/data/` and `projectsa-app/uploads/`.

**Tests:** `./mvnw test`.

## 6. Per–use-case checklist

For each UC: build the screens, the controller and DetectBlank, the API using exactly the listed queries, the status change, the notifications and **every alternative flow**. The UC is done when every box is ticked and its test passes.

### 6.0 Features outside the use cases (decided 2026-10-25; SQL in `ProjectSA_SQL.sql`, sections F1–F4)

**F1 Register**
- [x] Fields: First/Last Name, Email, Phone, Username, Password (saved as a BCrypt hash), and **Role**. The role is chosen with **radio buttons: exactly one of Sale / Presale / PM / TechEngineer**. One account = one role, and it can't be changed later.
- [x] Any role can be chosen freely, **including PM**, with no approval (demo decision, §7 #10).
- [x] Check that the Username and Email aren't already used (R.2), then save the employee (R.3).
- [x] When the role is **TechEngineer**, show a skill block where the engineer **types their own skills** (2026-10-26): Enter / comma / "เพิ่ม" makes a tag, × removes it, and existing skills (R.1) are suggested while typing. Require 1–20 skills of at most 50 characters; names are trimmed and duplicates (ignoring case) dropped. For each name: R.5 (existing, ignoring case) or R.6 (new SKILL row), then R.4.
- [x] R.3 and the R.5 / R.6 / R.4 statements run in one transaction.

**F2 Login**
- [x] Username + Password only. **There is no role choice at login**; the role comes from the account (L.1). Check the password against its BCrypt hash in Java.
- [x] The session stores EmployeeID (`@LoginID`) and Role (`@Role`). Every API returns 401 when nobody is logged in (`Auth.current`).
- [x] Every UC API returns 403 when the role is wrong for that UC (`Auth.require`). Tick this once each UC API has a 403 test.
- [x] The menu shows only that role's UCs (§1 table). Add Logout.

**F3 Profile** (header menu → "โปรไฟล์")
- [x] Show the user's own info, role and picture (P.1). For a Tech Engineer, also show their skills (P.2). **Role and Username are read-only.**
- [x] Edit First/Last Name, Email, Phone (P.4). The new email must not belong to someone else (P.3).
- [x] **Profile picture:** upload JPG/PNG ≤ 5 MB to `./uploads/profile/` and save the path (P.5). Show it in the header and on the profile page, with a default avatar when it's empty.
- [x] Change password: check the current one (L.1), then save the new hash (P.6).
- [x] Tech Engineer skills, typed the same way as on Register: P.7 (delete), then R.5 / R.6 + R.4 for each skill, in one transaction, keeping at least 1.

**F4 Notification Bell** (a simple bell in the header of every page)
- [x] The 🔔 shows a red badge with the unread count (N.1). Poll every 30 s. When the count goes up, the bell **rings**: a short shake animation in CSS, with no sound.
- [x] Clicking it opens a dropdown with the latest 20 (N.2): message, project name, time. Unread ones are shown in bold.
- [x] "อ่านทั้งหมด" marks every due notification read (N.4). Clicking a notification marks it read (N.3).
- [x] …and then opens UC03 for that project (`uc03.html?project=ID`). Tick when the UC03 page handles `?project=`.
- [x] Only rows whose `SendAt` has passed are shown, so UC05 reminders appear at the right time (`NotificationApiTest`).

### 6.1 UC01 Create Project Profile (Sale) → NewProject
- [x] The form fills in Sale in Charge from the login (Q2.1), loads existing customers (Q2.2) and Presale engineers (Q2.3).
- [x] Inputs: Project Name, existing **or** new customer (Name, Email), Presale in Charge, Project Site.
- [x] Validation 4.1–4.5: Project Name, Customer Name, Customer Email (format), Presale chosen, Project Site.
- [x] Save: new customer (Q5.1, only if new); project with Status `NewProject` (Q5.2); notify Presale (Q7.1).
- [x] Alt 4a: show the error next to the wrong field and stay on the form. Alt 3a: Cancel saves nothing.

### 6.2 UC02 Store Project Solution Details (Presale) → WaitingForAssignEngineer
- [x] List: the Presale's own `NewProject` projects (Q1.1). Detail shows the existing solution and documents (Q3.1, Q3.2).
- [x] Inputs: Start/End Date, Objective, Estimated Budget, Solution Detail, plus files typed Solution / ProductRequirement.
- [x] Validation 5.1–5.3: the dates are filled in and End ≥ Start; Objective is filled in; Budget > 0.
- [x] File Check 7.1–7.3: PDF, ≤ 50 MB each, at least 1 Solution file.
- [x] Save Q8.1, Q8.2 (once per file), then Q9.1 (status), then Q10.1 (notify every PM).
- [x] Alt 5a: field errors. Alt 7a: reject the bad file and keep the rest of the form.

### 6.3 UC03 Check Project Details (all roles, read-only)
- [x] List by role, plus search by name or ID (Q1.1, `@Keyword`). Access check (Q3.1); if it fails, alt 3a: "ไม่พบ Project".
- [x] Detail page sections 4.1–4.7: basics, customer, people in charge, solution, latest test, requirements, documents (Q4.1–Q4.6). The latest test reads `Scheduled` **or `Done`** (Q4.3), so it still shows after UC08.
- [x] An empty section shows "ยังไม่มีข้อมูล" (alt 4a).
- [x] Download a project document (Q6.1) or a requirement attachment (Q6.2). Run the access check before sending any file.
- [x] This UC must not write anything to the database.

### 6.4 UC04 Assign Engineer to Project (PM) → Working
- [x] List of `WaitingForAssignEngineer` projects (Q1.1). The form shows the solution (Q3.1), every engineer with their skills, how many projects they are on, and whether they are already on this one (Q3.2), plus the skill list (Q3.3).
- [x] Filter by skill (Q5.1). Alt 5a: "ไม่พบ Engineer".
- [x] Validation 7: at least 1 engineer. Alt 7a: show a warning.
- [x] Save: Q8.1 once per engineer (skip anyone already assigned); Q8.2 sets PMID = the login; Q9.1 (Working); Q10.1 (notify the engineers).

### 6.5 UC05 Set Project Test Calendar Notification (Tech Engineer) → Testing
- [x] List: projects the engineer is assigned to with status Working or Testing (Q1.1). The form shows the site, the customer, the team and the existing appointment (Q3.1–Q3.3).
- [x] Inputs: Test Date, Start/End Time, Location (defaults to Project Site), Detail, RemindBefore.
- [x] Validation 5.1–5.4: the date isn't in the past; End is after Start; Location is filled in; a reminder is chosen.
- [x] Save: cancel the old appointment (Q6.1), insert the new one (Q6.2), status Testing (Q7.1), notify now (Q8.1), schedule the reminder (Q9.1), and email the customer.

### 6.6 UC06 Add Customer New Requirement (PM) → WaitingForApprove
- [x] List: projects the PM manages with status Testing **or** WaitingForApprove (Q1.1), so the PM can add several requirements in one round. Detail: Q3.1, Q3.2.
- [x] Inputs: Title, Detail, Priority (Low/Medium/High), Due Date, Related Feature (optional), attachments (optional).
- [x] Validation 5.1–5.3: Title and Detail are filled in; Priority is chosen; the due date isn't in the past.
- [x] File Check 7: PDF/JPG/PNG, ≤ 50 MB. Alt 6a: if there are no files, skip the check.
- [x] Save: Q8.1 (Pending), Q8.2 (once per file), Q9.1 (WaitingForApprove), Q10.1 (notify Sale), and email Sale.

### 6.7 UC07 Approve New Project Requirement (Sale) → NewProject / Finish
- [x] List: the Sale's WaitingForApprove projects with a pending count (Q1.1). Pending requirements (Q3.1).
- [x] Re-check the requirement is still Pending (Q5.1). If not, alt 5a: warn and go back to the list. Then show details and attachments (Q6.1, Q6.2).
- [x] Decision: Approve or Reject. A reason is required to reject (alt 8a).
- [x] Save Q9.1 (with the `Status = 'Pending'` guard) and Q10.1 (notify the PM).
- [x] Step 11:
  - if requirements are still pending (Q11.1 > 0), the status stays WaitingForApprove (alt 11a);
  - otherwise, if Q11.2 > 0, run Q11.3 (NewProject + notify Presale);
  - otherwise run Q11.4 (Finish).

### 6.8 Life-cycle acceptance test (must pass before release)
- [x] UC01 → NewProject. UC02 → WaitingForAssignEngineer. UC04 → Working. UC05 → Testing.
- [x] Round 1: UC06 adds 2 requirements (the second is added while the project is already WaitingForApprove). UC07 approves 1: still WaitingForApprove. UC07 rejects 1: NewProject. Presale is notified.
- [x] Round 2: UC02 again, UC04 again (no duplicate assignment rows), UC05 again (the old appointment is Cancelled). UC06 adds 1, UC07 rejects it: **Finish**. The approval from round 1 must not count.
- [x] Check the notification recipients for each UC, the reminder `SendAt` (test time − RemindBefore), the rejection message including the reason, and that the approval message has no empty "(เหตุผล: )".
- [x] Role checks: a Tech Engineer who isn't assigned can't see or download a project. Write endpoints refuse a wrong status.
- [x] Round 3 (UC08), on a second project: after UC05 → Testing, the PM closes it → **Finish**. Check that:
  - the latest test is `Done` and UC03 still shows it;
  - Sale, Presale and both engineers are notified;
  - closing again is refused (status isn't Testing), and another PM can't close it.
- [x] F1–F4: you can't register the same username twice; a second role can't be stored; a profile edit keeps the role; the bell hides future reminders and marks notifications read.

### 6.9 UC08 Close Project (PM) → Finish
The design is complete (2026-10-25): To-Be (Revised), Use-Case, both narratives, Sequence UC08, Collaboration UC08, State Diagram, CRUD Table, SQL section UC08.
- [x] List: the PM's own Testing projects (Q1.1).
- [x] Detail: project, Sale/Presale (Q3.1), engineers (Q3.2), latest test (Q3.3), and the **"ปิด Project"** button with a confirmation dialog. Alt 4a: cancel changes nothing.
- [x] `POST /api/projects/{id}/close`, in one transaction:
  - re-check the status is Testing and this PM manages the project (Q5.1), and nothing is Pending (Q5.2). Otherwise alt 5a: "ไม่สามารถปิด Project ได้";
  - Q6.1 (Finish, with a `Status = 'Testing'` guard);
  - Q7.1 (latest test → Done);
  - Q8.1 (notify Sale, Presale and every assigned engineer, Type `ProjectClosed`).

## 7. Decisions and open questions

**Decided by the user (2026-10-25):**

| # | Question | Decision | Where it's applied |
|---|---|---|---|
| 1 | Tech stack | Java. Claude chose the rest: Spring Boot 3, H2 file DB in MSSQLServer mode, JdbcTemplate, static HTML/JS. A local demo. | §3 |
| 2 | Testing → Finish | The PM gets a "close project" button: **UC08 Close Project**, placed before "จัดทำเอกสาร UAT และ Handover", which notifies everyone in charge of the project. | §4, §6.9 |
| 4 | Admin screens | None. F1 Register lets the user choose **one** role, and a Tech Engineer also chooses skills (Java, Unix, CCNA, CCIE). | §4, §6.0 |
| 6 | Email / files | No email in the demo; email steps only write a log line. Files go to `./uploads/` (Claude's choice, which follows from "local demo"). | §3, §4 |
| 7 | Notification inbox | Yes: **a simple bell (F4)** with a count, a list and mark-as-read. | §6.0 |
| 8 | Profile | Yes: **F3 Profile**, where users change their information and profile picture. One person has one role, and it can't be changed. | §4, §6.0 |
| 9 | Claude's defaults for UC08 / F1–F4 | Accepted as proposed: the PM isn't notified of their own close; engineers can edit their skills on the profile page; the profile picture limit is 5 MB; the bell "rings" with a CSS shake, not a sound; the F1–F4 endpoints listed in §3. | §3, §4, §6.0 |
| 10 | Who can register as PM | **Anyone, for now** (demo). A real system would need an approval step; that's out of scope. | §6.0 |
| 11 | SQL placeholders in the docs | **Keep `@Param`** (the user asked for whatever is easiest to read). It names each value, is valid T-SQL, and is already on the With Query page and every sequence diagram. The With Query page explains it: "@ตัวแปร = ค่าที่รับจากหน้าจอ / ผู้ใช้ที่ login". **In code, `@Param` becomes a bound `:Param`.** Never build SQL by joining strings. | §3 |
| 12 | Tech Engineer skills on Register / Profile (2026-10-26) | **Typed by the engineer**, not ticked from a fixed list, because the seeded skills can't cover everyone. Claude's defaults: a typed name matches an existing skill ignoring case (R.5), otherwise it's added to SKILL (R.6); existing skills are suggested while typing; 1–20 skills, ≤ 50 characters each. | §4, §6.0 |

**Still open:** nothing at the moment. If a new question comes up, ask; don't guess.

## 8. Rules for working in this repo

- **Spec first.** Every behaviour must trace to a UC Step / Query. If the code needs something the design doesn't have, ask, then update the design. Don't invent behaviour silently.
- **When behaviour changes, update every artifact that describes it:**
  - both narrative pages;
  - `ProjectSA_SQL.sql` and the With Query page (they must stay identical). **After any SQL change, run `python tools/check_docs.py`, `python tools/test_sql.py` and `projectsa-app/mvnw test`; all must pass.** If you add a query, also add it to both life-cycle tests (`tools/test_sql.py` and `SqlLifecycleH2Test`), or their coverage checks fail;
  - the Sequence and Collaboration pages;
  - the CRUD Table page and `CRUD.md`;
  - the State Diagram if a status changes;
  - `ProjectSA_Analysis.md` (what changed and why).
- **Editing `ProjectSA_Revised.drawio.xml`:**
  - it is about 1 MB and someone may also be editing it in draw.io, so re-read it before you edit;
  - change only the pages you mean to change, and copy every other `<diagram>` block byte for byte;
  - afterwards, run `python tools/check_docs.py`. It checks the XML parses, there are no duplicate `mxCell` ids, every `parent`/`source`/`target` exists, and the SQL copies agree.
- **Never modify `ProjectSA.drawio.xml`** (the original).
- **Language:** UI text, messages and narratives are Thai, matching the diagrams. Identifiers, endpoints and SQL are English, exactly as in §3–§4.
