# Report.docx – progress checklist

Resume here if the session runs out. Source of requirements: `Report/Req.md`.
Output: `Report/Report.docx` (python-docx build script: `Report/build/build_report.py`).
Diagram images are exported from the `.xml` files in `Report/` to `Report/Images/` (`Report/build/render.py`, headless Chrome + draw.io viewer).

## Decisions from the user (2026-10-27)
- Sequence / Collaboration: **keep 8** (UC01–UC08); no extra diagrams for F1–F4.
- No `<<DB Connect>>` lifeline; keep API → Entity as drawn (explain Sql/JdbcTemplate in the text).
- Output reports (#32): use existing app pages – Report 1 = UC03 Project Detail, Report 2 = UC04 engineer list (skills + workload).
- Fix diagrams by **editing the Report copies in place**; never touch `Project Source/`. New diagrams go in new Report subfolders.
- Font TH Sarabun New (installed), A4.

## Diagram work
- [x] As-If: add process numbers
- [x] To-Be: add process numbers
- [x] Use-Case diagram (revised): actor names = swim-lane names (Presale Engineer, Technical Engineer)
- [x] NEW Use-Case diagram first version (7 UC, from original team diagram) → `Use-Case/Diagram/Use-Case Diagram (First).drawio.xml`
- [x] NEW CRUD first version (UC01–UC07) → `CRUD/CRUD Table (First).drawio.xml`
- [x] NEW BP + Use Case (computer UCs + manual processes marked) → `BP-UseCase/`
- [x] ER (Chen): add FK attributes, total/partial participation
- [x] NEW ER logical schema (M:N present) → `ER Diagram/`
- [x] NEW ERD with query → `ER Diagram/`
- [x] Collaboration CL01–08 + Sequence SQ01–08: number every message (same numbers in both)
- [x] NEW Class diagram(s) → `Class Diagram/`
- [x] State diagram: class.state, [condition]event/action (action = UC), SQL update
- [x] NEW SW platform stack → `Platform/`
- [x] NEW Site map → `Site Map/`
- [x] Render all to `Report/Images/` (re-run build/render_all.sh after any diagram change)

## Report sections (Req #) – all written (2026-10-27), Report.docx + Report.pdf, 138 pages
- [x] 0 Cover (template)  - [x] 1 TOC  - [x] 2 List of figures  - [x] 3 List of tables
- [x] 4 Abstract TH/EN (1 page)  - [x] 5 Intro (template)  - [x] 6 Problems (template)
- [x] 7–13 BP As-is, description, problems, To-be, description, BP+UC, BP↔UC table
- [x] 14–18 UC diagram first, CRUD, CRUD description, UC revised, CRUD revised (+ F1–F4 CRUD table)
- [x] 19/24/25 per UC: description → sequence → collaboration (UC01…UC08)
- [x] 20–23 ER, logical schema, ERD with query, query table, data dictionary (11 tables × 5 sample rows)
- [x] 26 Class diagram  - [x] 27 State diagram
- [x] 29 Platform  - [x] 30 Site map  - [x] 31 UI design  - [x] 32 Output design  - [x] 33 SQL statements (tested)  - [x] 34 Test tables
- [x] Update fields (TOC/figures/tables) via Word COM, export PDF, visual check

## How to rebuild
1. (only if a diagram .xml changed) `sh build/render_all.sh` → Images/*.png, then `python build/check_report.py` must print ALL OK.
2. `"/c/Program Files/Python311/python" build/build_report.py` → Report.docx (text lives in build/report_text.py, build/report_data.py).
3. `powershell -File build/update_fields.ps1` (Word must be closed) → fills TOC / lists, saves, exports Report.pdf.
4. Visual check: `python build/thumbs.py <dir>` → page contact sheets.

## User feedback (2026-10-27, round 2)
- No description / legend boxes beside any diagram (removed from As-If, To-Be, BP-UseCase, ER, SQ subtitles, and from every generator). Keep that rule for new diagrams; put explanations in the report text instead.
- Every page portrait (A4) – no landscape sections; wide diagrams are fitted to the page width. Report now 134 pages.

## Cover (2026-10-27, round 3)
- Cover follows `Report/Cover template.docx` line for line (COVER_LINES in build_report.py, Angsana New 15, logo `Images/KU_logo.png` copied from the template). Verified side by side with the template PDF: same positions.
- Placeholders put on template blank lines: other members' name + ID, teacher name.

## Left for the team (highlighted yellow in the docx)
- Cover: course, members (photo / ID / name), teacher, term, faculty.
- 1.1 Intro: company name, year, staff count, projects per year.
- 1.2 / 2.3 problem numbers are **assumed** figures – replace with real data from the company.
- Req #24/#25 minimum is 10 SQ/CL; report has 8 (user decision). Req #35/#36 not written (by Req).

## Notes / findings
- Scripts in `build/`: number_messages.py (SQ/CL numbering, done – don't rerun), number_processes.py (BP numbers, done – idempotent), dump_cells.py / dump_seq.py (inspect).
- Message numbering rule: unnumbered call before step N's queries = `Na`, `Nb` … (same label on SQ and CL).
- To-Be got a legend note (purple = improved by system, pain point → process mapping).
- BP-UseCase: purple solid ellipse = computer UC, orange dashed = manual UC M1–M7.
- ER: FK attrs (dashed ellipse, italic) + total participation (shape=link double line) + legend; script build/er_fix.py (done, guarded).
- Logical schema: build/gen_logical.py → ER Diagram/ER Logical Schema.drawio.xml (hand-routed edges).
- ERD with query: build/gen_query_erd.py → ER Diagram/ERD with Query/ERDQ01..08 (one per UC, query rows in yellow).
- build/sqlparse.py parses schema + 69 UC queries from Project Source/ProjectSA_SQL.sql.
- State diagram regenerated by build/gen_state.py (overwrites State Diagram.drawio.xml; old copy not kept).
- Class diagrams: build/gen_class.py → Class Diagram/CD01 Entity, CD02 Layers UC01-UC04, CD03 Layers UC05-UC08.
- Platform + Site map: build/gen_platform_sitemap.py.
- Render all: build/render_all.sh → Report/Images/*.png
- FIX (consistency): To-Be + BP-UseCase: UC07 'No' branch now goes to 24 UAT (UC07 Q11.4 already sets Finish; UC08 needs Testing). Note added to To-Be legend.
- Checks: build/check_report.py (check_docs on Report copies + SQ/CL numbers + CRUD vs SQL) → ALL OK. tools/test_sql.py 87/87 (run with msys python: SQLite 3.46; Program Files python has SQLite 3.40 → CONCAT error). mvnw test exit 0 (2026-10-27).
- Sample data: app jar run on a COPY of projectsa-app/data in scratchpad, 4 projects added via real UC APIs (seed script in scratchpad), exported to build/data/*.csv. User DB untouched.
