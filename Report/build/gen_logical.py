"""ER Diagram – Logical Schema (relational tables with PK/FK), generated from ProjectSA_SQL.sql."""
import os
from drawio import Diagram, table, TITLE, NOTE, REL
from sqlparse import schema

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(os.path.dirname(HERE), "ER Diagram", "ER Logical Schema.drawio.xml")

W = 270
POS = {"SKILL": (40, 100), "ENGINEER_SKILL": (420, 100), "EMPLOYEE": (800, 100), "PROJECT_ASSIGNMENT": (1250, 100),
       "CUSTOMER": (420, 480), "PROJECT": (800, 480), "NOTIFICATION": (1250, 480),
       "CUSTOMER_REQUIREMENT": (420, 940), "TEST_SCHEDULE": (800, 940), "PROJECT_DOCUMENT": (1250, 940),
       "REQUIREMENT_ATTACHMENT": (40, 1150)}
# (child, FK column, exit (x,y), entry (x,y), way points) – hand-routed so no line crosses a table
ROUTES = [
    ("ENGINEER_SKILL", "EmployeeID", (1, .5), (0, .16), []),
    ("ENGINEER_SKILL", "SkillID", (0, .5), (1, .38), []),
    ("PROJECT_ASSIGNMENT", "EngineerID", (0, .5), (1, .2), []),
    ("PROJECT_ASSIGNMENT", "ProjectID", (.2, 1), (1, .055), [(1304, 240), (1120, 240), (1120, 500)]),
    ("PROJECT", "SaleID", (.3, 0), (.3, 1), []),
    ("PROJECT", "PresaleID", (.5, 0), (.5, 1), []),
    ("PROJECT", "PMID", (.7, 0), (.7, 1), []),
    ("PROJECT", "CustomerID", (0, .14), (1, .5), []),
    ("NOTIFICATION", "ProjectID", (0, .3), (1, .16), []),
    ("NOTIFICATION", "RecipientID", (.5, 0), (1, .81), [(1385, 300)]),
    ("PROJECT_DOCUMENT", "ProjectID", (.2, 0), (.889, 1), [(1304, 890), (1040, 890)]),
    ("TEST_SCHEDULE", "ProjectID", (.3, 0), (.3, 1), []),
    ("TEST_SCHEDULE", "CreatedByID", (1, .1), (1, .894), [(1180, 969), (1180, 320)]),
    ("CUSTOMER_REQUIREMENT", "ProjectID", (.8, 0), (.15, 1), [(636, 900), (840, 900)]),
    ("CUSTOMER_REQUIREMENT", "CreatedByID", (0, .3), (0, .61), [(380, 1043), (380, 250)]),
    ("CUSTOMER_REQUIREMENT", "ReviewedByID", (0, .4), (0, .73), [(360, 1077), (360, 280)]),
    ("REQUIREMENT_ATTACHMENT", "RequirementID", (1, .5), (0, .73), []),
]


def key_label(c):
    k = "PK, FK" if c["pk"] and c["fk"] else "PK" if c["pk"] else "FK" if c["fk"] else ""
    name = f"<u>{c['name']}</u>" if c["pk"] else c["name"]
    return f"<b>{k}</b>&nbsp;&nbsp;{name} : {c['type']}" if k else f"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{name} : {c['type']}"


def main():
    sch = schema()
    d = Diagram("ER Logical Schema", "erLogicalPage01", "erL")
    d.vertex("ER Diagram – Logical Schema", 40, 20, 700, 40, TITLE)
    tid = {}
    for t, (x, y) in POS.items():
        tid[t], _ = table(d, t, [(key_label(c), c["name"]) for c in sch[t]], x, y, w=W, cid=t)
    fks = {(t, c["name"]): c["fk"].split("(")[0] for t, cols in sch.items() for c in cols if c["fk"]}
    assert set(fks) == {(r[0], r[1]) for r in ROUTES}, set(fks) ^ {(r[0], r[1]) for r in ROUTES}
    for child, col, ex, en, pts in ROUTES:
        st = REL.replace("edgeStyle=orthogonalEdgeStyle;", "") +             f"exitX={ex[0]};exitY={ex[1]};exitDx=0;exitDy=0;entryX={en[0]};entryY={en[1]};entryDx=0;entryDy=0;"
        d.edge(tid[child], tid[fks[(child, col)]], st, points=pts)
    d.save(OUT)
    print(OUT)


if __name__ == "__main__":
    main()
