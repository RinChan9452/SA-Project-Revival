"""Class diagrams. Every object on Sequence UC01–UC08 has a class here, with the same name.
  CD01 Entity classes (domain)            – generalization, composition / aggregation, association
  CD02 Boundary / Controller / API UC01–04 – generalization, association, dependency
  CD03 Boundary / Controller / API UC05–08
Attributes / methods follow the app (projectsa-app) and the sequence messages."""
import os
from html import escape
from drawio import Diagram, TITLE, NOTE
from sqlparse import schema

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(os.path.dirname(HERE), "Class Diagram")

CLS = "verticalAlign=top;align=left;overflow=fill;html=1;whiteSpace=wrap;fontSize=12;spacingLeft=4;"
LH = 15.5


def body(stereo, name, attrs, methods, abstract=False):
    nm = f"<i>{escape(name)}</i>" if abstract else escape(name)
    head = (f'<p style="margin:0px;margin-top:4px;text-align:center;">&lt;&lt;{escape(stereo)}&gt;&gt;'
            f'{"<br>{abstract}" if abstract else ""}<br><b>{nm}</b></p>')
    a = "<br>".join(escape(x) for x in attrs) or "&nbsp;"
    m = "<br>".join(escape(x) for x in methods) or "&nbsp;"
    return (head + '<hr size="1" style="border-style:solid;">'
            f'<p style="margin:0px;margin-left:4px;">{a}</p><hr size="1" style="border-style:solid;">'
            f'<p style="margin:0px;margin-left:4px;">{m}</p>')


def height(attrs, methods, abstract=False):
    return 44 + (16 if abstract else 0) + LH * (max(1, len(attrs)) + max(1, len(methods))) + 18


GEN = "endArrow=block;endFill=0;endSize=14;html=1;rounded=0;"
ASSOC = "endArrow=open;endFill=0;endSize=10;html=1;rounded=0;fontSize=12;"
LINE = "endArrow=none;html=1;rounded=0;fontSize=12;"
DEP = "endArrow=open;endFill=0;endSize=10;dashed=1;html=1;rounded=0;fontSize=11;"
AGG = "startArrow=diamondThin;startFill=0;startSize=16;endArrow=none;html=1;rounded=0;fontSize=12;"
COMP = "startArrow=diamondThin;startFill=1;startSize=16;endArrow=none;html=1;rounded=0;fontSize=12;"


def place(d, ids, rect, key, stereo, name, attrs, methods, x, y, w=260, abstract=False):
    h = height(attrs, methods, abstract)
    ids[key] = d.vertex(body(stereo, name, attrs, methods, abstract), x, y, w, h, CLS, cid=key)
    rect[key] = (x, y, w, h)
    return h


def ports(st, ex=None, en=None):
    s = st
    if ex: s += f"exitX={ex[0]};exitY={ex[1]};exitDx=0;exitDy=0;"
    if en: s += f"entryX={en[0]};entryY={en[1]};entryDx=0;entryDy=0;"
    return s


# ---------------------------------------------------------------- CD01 entity classes
ENTITY_METHODS = {
    "EMPLOYEE": ["+findById(id): EMPLOYEE", "+findByRole(role): List", "+register(): int", "+updateProfile()"],
    "SALE": ["+createProject(): PROJECT", "+approveRequirement(reqId, decision)"],
    "PRESALE": ["+storeSolution(projectId)"],
    "PM": ["+assignEngineer(projectId, engineerIds)", "+addRequirement(projectId)", "+closeProject(projectId)"],
    "TECH_ENGINEER": ["+setTestSchedule(projectId)", "+skills(): List<SKILL>"],
    "SKILL": ["+findAll(): List", "+findByName(name): SKILL", "+create(name): int"],
    "ENGINEER_SKILL": ["+add(employeeId, skillId)", "+replaceAll(employeeId, skillIds)"],
    "CUSTOMER": ["+findAll(): List", "+create(): int"],
    "PROJECT": ["+create(): int", "+updateSolution()", "+updateStatus(status)", "+setPM(pmId)",
                "+findByRole(role, loginId): List", "+canAccess(loginId): boolean"],
    "PROJECT_ASSIGNMENT": ["+assign(projectId, engineerId)", "+findTeam(projectId): List"],
    "TEST_SCHEDULE": ["+create(): int", "+cancelActive(projectId)", "+markDone(projectId)", "+latest(projectId)"],
    "CUSTOMER_REQUIREMENT": ["+create(): int", "+decide(status, reviewer, reason)", "+countPending(projectId): int",
                             "+countApprovedThisRound(projectId): int"],
    "REQUIREMENT_ATTACHMENT": ["+add(requirementId, file)", "+findByRequirement(id): List"],
    "PROJECT_DOCUMENT": ["+add(projectId, docType, file)", "+findByProject(id): List"],
    "NOTIFICATION": ["+send(recipientId, projectId, type, msg, sendAt)", "+unread(recipientId): int", "+markRead(id)"],
}
JTYPE = {"INT": "int", "BIT": "boolean", "DATE": "Date", "DATETIME": "DateTime", "TIME": "Time"}


def jtype(t):
    base = t.split("(")[0]
    return JTYPE.get(base, "decimal" if base == "DECIMAL" else "String")


def cd01():
    sch = schema()
    d = Diagram("Class Diagram – Entity", "classEntityPage1", "cdE")
    d.vertex("Class Diagram – Entity Classes", 40, 0, 800, 40, TITLE)
    ids, rect = {}, {}
    attrs = lambda t: [f"-{c['name']}: {jtype(c['type'])}" for c in sch[t]]
    sub_attr = {"SALE": ["-role: String = 'Sale'"], "PRESALE": ["-role: String = 'Presale'"],
                "PM": ["-role: String = 'PM'"], "TECH_ENGINEER": ["-role: String = 'TechEngineer'"]}
    place(d, ids, rect, "EMPLOYEE", "Entity", "EMPLOYEE", attrs("EMPLOYEE"), ENTITY_METHODS["EMPLOYEE"], 760, 70, 280)
    for i, s in enumerate(["SALE", "PRESALE", "PM", "TECH_ENGINEER"]):
        place(d, ids, rect, s, "Entity", s, sub_attr[s], ENTITY_METHODS[s], 140 + i * 400, 500, 300)
    place(d, ids, rect, "NOTIFICATION", "Entity", "NOTIFICATION", attrs("NOTIFICATION"), ENTITY_METHODS["NOTIFICATION"], 1300, 70, 330)
    place(d, ids, rect, "ENGINEER_SKILL", "Entity", "ENGINEER_SKILL", attrs("ENGINEER_SKILL"), ENTITY_METHODS["ENGINEER_SKILL"], 1780, 500, 280)
    place(d, ids, rect, "SKILL", "Entity", "SKILL", attrs("SKILL"), ENTITY_METHODS["SKILL"], 1780, 760, 280)
    place(d, ids, rect, "CUSTOMER", "Entity", "CUSTOMER", attrs("CUSTOMER"), ENTITY_METHODS["CUSTOMER"], 60, 900, 260)
    place(d, ids, rect, "PROJECT", "Entity", "PROJECT", attrs("PROJECT"), ENTITY_METHODS["PROJECT"], 560, 900, 300)
    place(d, ids, rect, "PROJECT_ASSIGNMENT", "Entity", "PROJECT_ASSIGNMENT", attrs("PROJECT_ASSIGNMENT"),
          ENTITY_METHODS["PROJECT_ASSIGNMENT"], 1300, 900, 300)
    place(d, ids, rect, "PROJECT_DOCUMENT", "Entity", "PROJECT_DOCUMENT", attrs("PROJECT_DOCUMENT"), ENTITY_METHODS["PROJECT_DOCUMENT"], 60, 1420, 300)
    place(d, ids, rect, "TEST_SCHEDULE", "Entity", "TEST_SCHEDULE", attrs("TEST_SCHEDULE"), ENTITY_METHODS["TEST_SCHEDULE"], 560, 1420, 300)
    place(d, ids, rect, "CUSTOMER_REQUIREMENT", "Entity", "CUSTOMER_REQUIREMENT", attrs("CUSTOMER_REQUIREMENT"),
          ENTITY_METHODS["CUSTOMER_REQUIREMENT"], 1060, 1420, 320)
    place(d, ids, rect, "REQUIREMENT_ATTACHMENT", "Entity", "REQUIREMENT_ATTACHMENT", attrs("REQUIREMENT_ATTACHMENT"),
          ENTITY_METHODS["REQUIREMENT_ATTACHMENT"], 1560, 1420, 320)

    # generalization: subclasses -> EMPLOYEE (tree through y = 450)
    e = rect["EMPLOYEE"]
    for s in ["SALE", "PRESALE", "PM", "TECH_ENGINEER"]:
        r = rect[s]
        d.edge(ids[s], ids["EMPLOYEE"], ports(GEN, (0.5, 0), (0.5, 1)),
               points=[(r[0] + r[2] / 2, 450), (e[0] + e[2] / 2, 450)])
    d.vertex("{disjoint, complete}", e[0] + e[2] / 2 + 10, 420, 160, 24, "text;html=1;fontSize=12;fontStyle=2;")
    E = d.edge
    def lab(edge_style, a, b, la, lb, ex=None, en=None, pts=None, name=""):
        st = ports(edge_style, ex, en)
        i = E(ids[a], ids[b], st, value=name, points=pts)
        # multiplicities as child labels of the edge
        d.cells.append(f'        <mxCell id="{i}-s" connectable="0" parent="{i}" style="edgeLabel;html=1;fontSize=12;" value="{la}" vertex="1">\n'
                       f'          <mxGeometry x="-0.85" y="-12" relative="1" as="geometry"><mxPoint as="offset" /></mxGeometry>\n        </mxCell>\n')
        d.cells.append(f'        <mxCell id="{i}-t" connectable="0" parent="{i}" style="edgeLabel;html=1;fontSize=12;" value="{lb}" vertex="1">\n'
                       f'          <mxGeometry x="0.85" y="-12" relative="1" as="geometry"><mxPoint as="offset" /></mxGeometry>\n        </mxCell>\n')
    lab(LINE, "SALE", "PROJECT", "1", "0..*", (0.5, 1), (0, 0.1), name="creates")
    lab(LINE, "PRESALE", "PROJECT", "1", "0..*", (0.5, 1), (0.3, 0), name="designs")
    lab(LINE, "PM", "PROJECT", "0..1", "0..*", (0.3, 1), (0.8, 0), name="manages")
    lab(LINE, "TECH_ENGINEER", "PROJECT_ASSIGNMENT", "1", "0..*", (0.5, 1), (0.5, 0))
    lab(LINE, "PROJECT_ASSIGNMENT", "PROJECT", "0..*", "1", (0, 0.3), (1, 0.12))
    lab(LINE, "TECH_ENGINEER", "ENGINEER_SKILL", "1", "1..*", (1, 0.4), (0, 0.4))
    lab(LINE, "ENGINEER_SKILL", "SKILL", "0..*", "1", (0.5, 1), (0.5, 0))
    lab(LINE, "CUSTOMER", "PROJECT", "1", "1..*", (1, 0.3), (0, 0.3), name="owns")
    lab(COMP, "PROJECT", "PROJECT_DOCUMENT", "1", "0..*", (0.15, 1), (0.5, 0))
    lab(AGG, "PROJECT", "TEST_SCHEDULE", "1", "0..*", (0.5, 1), (0.5, 0))
    lab(AGG, "PROJECT", "CUSTOMER_REQUIREMENT", "1", "0..*", (0.85, 1), (0.3, 0))
    lab(COMP, "CUSTOMER_REQUIREMENT", "REQUIREMENT_ATTACHMENT", "1", "0..*", (1, 0.3), (0, 0.3))
    lab(LINE, "PROJECT", "NOTIFICATION", "1", "0..*", (1, 0.05), (0, 0.85), pts=[(1285, 918), (1285, 250)], name="triggers")
    lab(LINE, "EMPLOYEE", "NOTIFICATION", "1", "0..*", (1, 0.3), (0, 0.3), name="receives")
    lab(LINE, "TECH_ENGINEER", "TEST_SCHEDULE", "1", "0..*", (0, 0.9), (1, 0.08), pts=[(1255, 590), (1255, 1300), (900, 1300), (900, 1446)], name="sets")
    lab(LINE, "PM", "CUSTOMER_REQUIREMENT", "1", "0..*", (0.7, 1), (0.6, 0), pts=[(1050, 870), (1250, 870)], name="records")
    lab(LINE, "SALE", "CUSTOMER_REQUIREMENT", "0..1", "0..*", (0.1, 1), (0.1, 0), pts=[(170, 860), (30, 860), (30, 1380), (1092, 1380)], name="approves")
    d.save(os.path.join(OUT_DIR, "CD01 Entity.drawio.xml"))


# ---------------------------------------------------------------- CD02 / CD03 layer classes
B, C, V, A = "Boundary", "Controller", "Controller", "API"
SUPERS = {
    "AppPage": (B, "AppPage", ["#loginUser: SessionUser", "#unreadCount: int"],
                ["+header()", "+showErrors(form, errors)", "+toast(message)", "+formData(form): Map"], True),
    "BaseController": (C, "BaseController", ["#api: String"],
                       ["+loadProjectList(): List", "+selectProject(ProjectID)"], True),
    "DetectBlank": (V, "DetectBlank", ["#errors: Map<String,String>"], ["+check(formData): Map", "#required(field): boolean"], True),
    "BaseAPI": (A, "BaseAPI", ["#sql: Sql", "#auth: Auth"],
                ["+requireRole(session, role): SessionUser", "+listProjects(filter): List  «GET /api/projects»"], True),
}
CLASSES = {
    # key: (stereo, name, attrs, methods, super)
    "ProjectListForm": (B, "ProjectList Form", ["-projects: List", "-keyword: String"],
                        ["+show(projects)", "+search(keyword)", "+select(ProjectID)"], "AppPage"),
    "CreateProjectButton": (B, "CreateProject Button", ["-label: String"], ["+onClick()"], "AppPage"),
    "ProjectDetailForm": (B, "ProjectDetail Form", ["-projectName: String", "-customerId: int", "-customerName: String",
                          "-customerEmail: String", "-presaleId: int", "-projectSite: String"],
                          ["+show(formData)", "+submit()", "+cancel()"], "AppPage"),
    "ProjectController": (C, "Project Controller", ["-form: ProjectDetail Form"],
                          ["+openCreateForm()", "+submitProject(formData)", "+downloadDocument(DocumentID)"], "BaseController"),
    "ProjectDetectBlank": (V, "Project DetectBlank", ["-emailPattern: Regex"],
                           ["+check(formData): Map  «4.1–4.5»"], "DetectBlank"),
    "ProjectAPI": (A, "Project API", ["-service: ProjectService", "-checkService: CheckProjectService"], ["+getCreateForm()  «GET /api/projects/create-form»",
                   "+createProject(body)  «POST /api/projects»", "+getDetail(id)  «GET /api/projects/{id}»",
                   "+getDocument(id, docId)  «GET …/documents/{docId}»", "+getAttachment(id, reqId, file)"], "BaseAPI"),
    "SolutionDetailForm": (B, "SolutionDetail Form", ["-startDate: Date", "-endDate: Date", "-objective: String",
                           "-estimatedBudget: decimal", "-solutionDetail: String", "-files: List<File>"],
                           ["+show(project, documents)", "+submit()"], "AppPage"),
    "SolutionController": (C, "Solution Controller", ["-selectedProjectId: int"], ["+validateSolution(formData)", "+saveSolution(formData, files)"], "BaseController"),
    "SolutionDetectBlank": (V, "Solution DetectBlank", ["-minBudget: decimal = 0"], ["+check(formData): Map  «5.1–5.3»"], "DetectBlank"),
    "FileCheck": (V, "File Check", ["-allowedTypes: List<String>", "-maxSize: long = 50 MB"],
                  ["+check(files, rule): List  «UC02 7.1–7.3, UC06 7»", "+extension(name): String"], None),
    "SolutionAPI": (A, "Solution API", ["-service: SolutionService", "-files: FileStore"], ["+getSolution(id)  «GET /api/projects/{id}/solution»",
                    "+saveSolution(id, form, files)  «POST …/solution»"], "BaseAPI"),
    "ProjectDetailPage": (B, "ProjectDetail Page", ["-project: Map", "-team: List", "-solution: Map", "-latestTest: Map",
                          "-requirements: List", "-documents: List"], ["+show(detail)", "+download(DocumentID)"], "AppPage"),
    "AssignEngineerForm": (B, "AssignEngineer Form", ["-engineers: List", "-skills: List", "-selected: List<int>"],
                           ["+show(form)", "+filter(SkillID)", "+toggle(EngineerID)", "+submit()"], "AppPage"),
    "AssignController": (C, "Assign Controller", ["-selectedProjectId: int", "-selectedEngineers: List<int>"], ["+filterEngineer(SkillID)", "+assignEngineer(ProjectID, EngineerIDs)"], "BaseController"),
    "AssignDetectBlank": (V, "Assign DetectBlank", ["-minEngineers: int = 1"], ["+check(engineerIds): Map  «7»"], "DetectBlank"),
    "AssignAPI": (A, "Assign API", ["-service: AssignService"], ["+getAssignForm(id)  «GET …/assign-form»", "+filter(skill)  «GET /api/engineers»",
                  "+assign(id, engineerIds)  «POST …/assignments»"], "BaseAPI"),
    "TestScheduleForm": (B, "TestSchedule Form", ["-testDate: Date", "-startTime: Time", "-endTime: Time", "-location: String",
                         "-testDetail: String", "-remindBefore: int"], ["+show(form)", "+submit()"], "AppPage"),
    "ScheduleController": (C, "Schedule Controller", ["-selectedProjectId: int"], ["+saveSchedule(formData)"], "BaseController"),
    "ScheduleDetectBlank": (V, "Schedule DetectBlank", ["-remindOptions: List<int>"], ["+check(formData): Map  «5.1–5.4»"], "DetectBlank"),
    "ScheduleAPI": (A, "Schedule API", ["-service: ScheduleService", "-email: EmailStub"], ["+getScheduleForm(id)  «GET …/schedule-form»",
                    "+saveSchedule(id, form)  «POST …/schedules»"], "BaseAPI"),
    "NewRequirementForm": (B, "NewRequirement Form", ["-title: String", "-detail: String", "-priority: String", "-dueDate: Date",
                           "-relatedFeature: String", "-attachments: List<File>"], ["+show(project)", "+submit()"], "AppPage"),
    "RequirementController": (C, "Requirement Controller", ["-selectedProjectId: int"], ["+validateRequirement(formData)",
                              "+submitRequirement(formData, files)"], "BaseController"),
    "RequirementDetectBlank": (V, "Requirement DetectBlank", ["-priorities: List<String>"], ["+check(formData): Map  «5.1–5.3»"], "DetectBlank"),
    "RequirementAPI": (A, "Requirement API", ["-service: RequirementService", "-email: EmailStub"], ["+getRequirementForm(id)  «GET …/requirement-form»",
                       "+addRequirement(id, form, files)  «POST …/requirements»"], "BaseAPI"),
    "RequirementListForm": (B, "RequirementList Form", ["-requirements: List"], ["+show(requirements)", "+select(RequirementID)"], "AppPage"),
    "ApproveForm": (B, "Approve Form", ["-requirement: Map", "-attachments: List", "-decision: String", "-rejectReason: String"],
                    ["+show(requirement)", "+approve()", "+reject()"], "AppPage"),
    "ApproveController": (C, "Approve Controller", ["-selectedProjectId: int", "-selectedRequirementId: int"], ["+selectRequirement(RequirementID)", "+submitDecision(RequirementID, decision)"], "BaseController"),
    "ApproveDetectBlank": (V, "Approve DetectBlank", ["-decisions: List<String>"], ["+check(decision, reason): Map  «8, alt 8a»"], "DetectBlank"),
    "ApproveAPI": (A, "Approve API", ["-service: ApproveService"], ["+pending(id)  «GET …/requirements?status=Pending»",
                   "+getRequirement(reqId, project)  «GET /api/requirements/{reqId}»",
                   "+decide(reqId, body)  «PUT …/decision»"], "BaseAPI"),
    "CloseProjectForm": (B, "CloseProject Form", ["-project: Map", "-engineers: List", "-latestTest: Map"],
                         ["+show(form)", "+confirmClose()", "+cancel()"], "AppPage"),
    "CloseController": (C, "Close Controller", ["-selectedProjectId: int"], ["+closeProject(ProjectID)"], "BaseController"),
    "CloseAPI": (A, "Close API", ["-service: CloseService"], ["+getCloseForm(id)  «GET …/close-form»", "+close(id)  «POST …/close»"], "BaseAPI"),
}
# rows: (UC label, boundaries, controller, validators, api, entities)
ROWS = {
    "UC01": (["CreateProjectButton", "ProjectDetailForm"], "ProjectController", ["ProjectDetectBlank"], "ProjectAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "NOTIFICATION"]),
    "UC02": (["SolutionDetailForm"], "SolutionController", ["SolutionDetectBlank", "FileCheck"], "SolutionAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "PROJECT_DOCUMENT", "NOTIFICATION"]),
    "UC03": (["ProjectDetailPage"], "ProjectController", [], "ProjectAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT", "PROJECT_DOCUMENT", "TEST_SCHEDULE",
              "CUSTOMER_REQUIREMENT", "REQUIREMENT_ATTACHMENT"]),
    "UC04": (["AssignEngineerForm"], "AssignController", ["AssignDetectBlank"], "AssignAPI",
             ["EMPLOYEE", "SKILL", "ENGINEER_SKILL", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT", "NOTIFICATION"]),
    "UC05": (["TestScheduleForm"], "ScheduleController", ["ScheduleDetectBlank"], "ScheduleAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT", "TEST_SCHEDULE", "NOTIFICATION"]),
    "UC06": (["NewRequirementForm"], "RequirementController", ["RequirementDetectBlank", "FileCheck"], "RequirementAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT", "CUSTOMER_REQUIREMENT", "REQUIREMENT_ATTACHMENT", "NOTIFICATION"]),
    "UC07": (["RequirementListForm", "ApproveForm"], "ApproveController", ["ApproveDetectBlank"], "ApproveAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "TEST_SCHEDULE", "CUSTOMER_REQUIREMENT", "REQUIREMENT_ATTACHMENT", "NOTIFICATION"]),
    "UC08": (["CloseProjectForm"], "CloseController", [], "CloseAPI",
             ["EMPLOYEE", "CUSTOMER", "PROJECT", "PROJECT_ASSIGNMENT", "TEST_SCHEDULE", "CUSTOMER_REQUIREMENT", "NOTIFICATION"]),
}
COLX = {"B": 40, "C": 470, "V": 900, "A": 1330, "E": 1960}
CW = 300


def layer_diagram(fname, title, ucs, page_id, prefix):
    d = Diagram(title, page_id, prefix)
    d.vertex(title, 40, 0, 1200, 40, TITLE)
    ids, rect = {}, {}
    y0 = 70
    # superclasses in the top row
    hs = []
    for col, key in (("B", "AppPage"), ("C", "BaseController"), ("V", "DetectBlank"), ("A", "BaseAPI")):
        st, nm, at, me, ab = SUPERS[key]
        hs.append(place(d, ids, rect, key, st, nm, at, me, COLX[col], y0, 420 if col == "A" else CW, ab))
    y = y0 + max(hs) + 90
    # ProjectList Form is shared by every UC except UC01 – drawn once, under AppPage
    if any(u != "UC01" for u in ucs):
        st, nm, at, me, _ = CLASSES["ProjectListForm"]
        y += place(d, ids, rect, "ProjectListForm", st, nm, at, me, COLX["B"], y, CW) + 50
    ents_needed = []
    for u in ucs:
        for e in ROWS[u][4]:
            if e not in ents_needed:
                ents_needed.append(e)
    deps = []
    for u in ucs:
        bnds, ctl, vals, api, ents = ROWS[u]
        d.vertex(f"<b>{u}</b>", COLX["B"] - 36, y, 40, 24, "text;html=1;fontSize=13;rotation=-90;")
        row_top = y
        by = y
        for b in bnds:
            if b not in ids:
                st, nm, at, me, _ = CLASSES[b]
                by += place(d, ids, rect, b, st, nm, at, me, COLX["B"], by, CW) + 30
        hc = 0
        if ctl not in ids:
            st, nm, at, me, _ = CLASSES[ctl]
            hc = place(d, ids, rect, ctl, st, nm, at, me, COLX["C"], y, CW)
        vy = y + 70
        for v in vals:
            if v not in ids:
                st, nm, at, me, _ = CLASSES[v]
                vy += place(d, ids, rect, v, st, nm, at, me, COLX["V"], vy, CW) + 30
        ha = 0
        if api not in ids:
            st, nm, at, me, _ = CLASSES[api]
            ha = place(d, ids, rect, api, st, nm, at, me, COLX["A"], y, 420)
        # associations
        for b in bnds:
            br, cr0 = rect[b], rect[ctl]
            if cr0[1] + cr0[3] < br[1]:   # controller drawn in an earlier row (UC03 reuses Project Controller)
                d.edge(ids[b], ids[ctl], ports(ASSOC, (1, 0.5), (0, 0.9)),
                       points=[(420, br[1] + br[3] / 2), (420, cr0[1] + 0.9 * cr0[3])])
            else:
                d.edge(ids[b], ids[ctl], ports(ASSOC, (1, 0.5), None))
        if "ProjectListForm" in ids and u != "UC01":
            pl = rect["ProjectListForm"]; cr = rect[ctl]
            d.edge(ids["ProjectListForm"], ids[ctl], ports(ASSOC, (1, 0.5), (0, 0.15)),
                   points=[(pl[0] + pl[2] + 40 + 8 * ucs.index(u), pl[1] + pl[3] / 2),
                           (pl[0] + pl[2] + 40 + 8 * ucs.index(u), cr[1] + 0.15 * cr[3])])
        cr, ar = rect[ctl], rect[api]
        line_y = max(cr[1], ar[1]) + 22
        if ctl != "ProjectController" or u == "UC01":
            d.edge(ids[ctl], ids[api], ports(ASSOC, (1, (line_y - cr[1]) / cr[3]), (0, (line_y - ar[1]) / ar[3])), value="calls")
        for v in vals:
            d.edge(ids[ctl], ids[v], ports(DEP, (1, 0.75), (0, 0.5)), value="«use»")
        deps.append((api, ents))
        y = max(by, vy, y + max(hc, ha)) + 70
    # generalization trees (channel left of each column)
    for key, (st, nm, at, me, sup) in CLASSES.items():
        if key in ids and sup:
            r, s = rect[key], rect[sup]
            cx = r[0] - 22
            d.edge(ids[key], ids[sup], ports(GEN, (0, 0.5), (0.12, 1)),
                   points=[(cx, r[1] + r[3] / 2), (cx, s[1] + s[3] + 30), (s[0] + 0.12 * s[2], s[1] + s[3] + 30)])
    # entity classes (detail in CD01) and API dependencies
    ey = y0
    for e in ents_needed:
        ids[e] = d.vertex(body("Entity", e, ["(attributes: Class Diagram – Entity)"], ["(methods: Class Diagram – Entity)"]),
                          COLX["E"], ey, 280, 90, CLS, cid=e)
        rect[e] = (COLX["E"], ey, 280, 90)
        ey += 120
    done = set()
    for k, (api, ents) in enumerate(deps):
        for e in ents:
            if (api, e) in done:
                continue
            done.add((api, e))
            ar = rect[api]
            d.edge(ids[api], ids[e], ports(DEP, (1, 0.3 + 0.05 * (k % 3)), (0, 0.5)))
    d.save(os.path.join(OUT_DIR, fname))


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    cd01()
    layer_diagram("CD02 Layers UC01-UC04.drawio.xml", "Class Diagram – Boundary / Controller / API (UC01–UC04)",
                  ["UC01", "UC02", "UC03", "UC04"], "classLayerPage1", "cdL1")
    layer_diagram("CD03 Layers UC05-UC08.drawio.xml", "Class Diagram – Boundary / Controller / API (UC05–UC08)",
                  ["UC05", "UC06", "UC07", "UC08"], "classLayerPage2", "cdL2")
    print("ok")


if __name__ == "__main__":
    main()
