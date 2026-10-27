"""SW development platform (stack) and Site map / UI structure."""
import os
from drawio import Diagram, TITLE, NOTE

HERE = os.path.dirname(os.path.abspath(__file__))
REPORT = os.path.dirname(HERE)

LAYER = "rounded=0;whiteSpace=wrap;html=1;fontSize=15;align=center;verticalAlign=middle;strokeColor=#333333;"
TAGS = "rounded=1;whiteSpace=wrap;html=1;fontSize=13;strokeColor=#666666;fillColor=#FFFFFF;"


def platform():
    d = Diagram("SW Development Platform", "platformPage1", "pf")
    d.vertex("SW Development Platform – Project Management Program", 40, 0, 1100, 40, TITLE)
    layers = [
        ("Client", "#DAE8FC", ["Web Browser (Google Chrome / Microsoft Edge)", "HTML5 + CSS3", "Vanilla JavaScript (ไม่มี front-end build)",
                               "<<Boundary>> หน้าจอ static/*.html", "<<Controller>> js/ucNN.js, app.js, bell.js"]),
        ("Application", "#D5E8D4", ["Spring Boot 3.5.16 (embedded Tomcat, port 8080)", "Spring Web – REST <<API>> /api/…",
                                    "Spring Validation – DetectBlank", "HttpSession + BCrypt (spring-security-crypto)"]),
        ("Data Access", "#FFF2CC", ["Spring JDBC – NamedParameterJdbcTemplate", "QueryCatalog + Sql (รัน Query N.M ตาม id)",
                                    "ไม่มี JPA / Hibernate"]),
        ("Database / Storage", "#F8CECC", ["H2 Database (file mode, MODE=MSSQLServer)", "Schema + Query: ProjectSA_SQL.sql (T-SQL)",
                                           "ไฟล์แนบ: ./uploads (PDF / JPG / PNG)"]),
        ("Runtime / OS", "#E1D5E7", ["Java 21 (JDK 21.0.7)", "Maven 3.9.10 + Maven Wrapper (mvnw)", "Windows 11"]),
    ]
    y = 70
    for name, color, items in layers:
        cells, x, row_y = [], 260, y + 14
        for it in items:
            w = 14 + 7.6 * len(it)
            if x + w > 1125:
                x, row_y = 260, row_y + 44
            cells.append((it.replace("<<", "&lt;&lt;").replace(">>", "&gt;&gt;"), x, row_y, w))
            x += w + 12
        h = row_y - y + 34 + 14
        d.vertex(f"<b>{name}</b>", 40, y, 200, h, LAYER + f"fillColor={color};")
        d.vertex("", 240, y, 900, h, LAYER + f"fillColor={color};")
        for v, cx, cy, w in cells:
            d.vertex(v, cx, cy, w, 34, TAGS)
        y += h + 10
    d.vertex("<b>เครื่องมือพัฒนา / ทดสอบ</b>", 1180, 70, 330, 50, LAYER + "fillColor=#F5F5F5;")
    tools = ["Visual Studio Code / IntelliJ IDEA", "draw.io (BP, UML, ER Diagram)", "JUnit 5 + Spring Boot Test (MockMvc)",
             "H2 Console (/h2-console)", "Python 3 – tools/check_docs.py, tools/test_sql.py", "Claude Code (ผู้ช่วยเขียนโค้ด)"]
    for i, t in enumerate(tools):
        d.vertex(t, 1180, 130 + i * 50, 330, 40, TAGS)
    d.save(os.path.join(REPORT, "Platform", "SW Platform.drawio.xml"))


NODE = "rounded=1;whiteSpace=wrap;html=1;fontSize=13;"
ROOT = NODE + "fillColor=#000000;fontColor=#FFFFFF;fontStyle=1;"
ROLE = NODE + "fillColor=#DAE8FC;strokeColor=#6C8EBF;fontStyle=1;"
UCN = NODE + "fillColor=#E1D5E7;strokeColor=#9673A6;"
SUB = NODE + "fillColor=#FFFFFF;"
COMMON = NODE + "fillColor=#F5F5F5;strokeColor=#999999;"
TREE = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=none;"


def sitemap():
    d = Diagram("Site Map", "siteMapPage1", "sm")
    d.vertex("Site Map / UI Structure", 40, 0, 800, 40, TITLE)
    W, H = 220, 50
    login = d.vertex("Login<br>(login.html)", 40, 640, W, H, ROOT)
    reg = d.vertex("Register<br>(register.html)", 40, 760, W, H, COMMON)
    d.edge(login, reg, "html=1;endArrow=open;dashed=1;fontSize=12;", value="สมัครสมาชิก")
    roles = [
        ("Sale", [("UC01 Create Project Profile", ["ProjectDetail Form"]),
                  ("UC03 Check Project Details", ["ProjectList Form", "ProjectDetail Page"]),
                  ("UC07 Approve New Project Requirement", ["ProjectList Form", "RequirementList Form", "Approve Form"])]),
        ("Presale Engineer", [("UC02 Store Project Solution Details", ["ProjectList Form", "SolutionDetail Form"]),
                              ("UC03 Check Project Details", ["ProjectList Form", "ProjectDetail Page"])]),
        ("PM", [("UC03 Check Project Details", ["ProjectList Form", "ProjectDetail Page"]),
                ("UC04 Assign Engineer to Project", ["ProjectList Form", "AssignEngineer Form"]),
                ("UC06 Add Customer New Requirement", ["ProjectList Form", "NewRequirement Form"]),
                ("UC08 Close Project", ["ProjectList Form", "CloseProject Form"])]),
        ("Technical Engineer", [("UC03 Check Project Details", ["ProjectList Form", "ProjectDetail Page"]),
                                ("UC05 Set Project Test Calendar Notification", ["ProjectList Form", "TestSchedule Form"])]),
    ]
    RX, UX, SX = 340, 640, 940
    y = 170
    first = None
    for role, ucs in roles:
        rows = len(ucs) + 1
        top = y
        r = d.vertex(f"หน้าหลัก {role}<br>(index.html)", RX, top + (rows * 80 - 20) / 2 - H / 2, W, H, ROLE)
        d.edge(login, r, TREE + "exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;")
        for uc, screens in ucs:
            u = d.vertex(uc, UX, y, W + 40, 60, UCN)
            d.edge(r, u, TREE + "exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;")
            prev, sx = u, SX + 40
            for s_ in screens:
                v = d.vertex(s_, sx, y + 10, W - 20, 40, SUB)
                d.edge(prev, v, "html=1;endArrow=open;endFill=0;rounded=0;")
                prev, sx = v, sx + W + 20
            y += 80
        cm = d.vertex("โปรไฟล์ (profile.html) · 🔔 การแจ้งเตือน · ออกจากระบบ", UX, y, W + 40 + 3 * (W + 20), 50, COMMON)
        d.edge(r, cm, TREE + "exitX=1;exitY=0.5;exitDx=0;exitDy=0;entryX=0;entryY=0.5;entryDx=0;entryDy=0;")
        y += 90
    d.save(os.path.join(REPORT, "Site Map", "Site Map.drawio.xml"))


if __name__ == "__main__":
    os.makedirs(os.path.join(REPORT, "Platform"), exist_ok=True)
    os.makedirs(os.path.join(REPORT, "Site Map"), exist_ok=True)
    platform(); sitemap(); print("ok")
