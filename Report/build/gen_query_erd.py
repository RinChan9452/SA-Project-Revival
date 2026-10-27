"""ERD with query – one page per use case. Tables show only PK / FK (plus the attributes an UPDATE
writes, marked (U)); the query numbers of the use case are listed under every table they touch.
Generated from ProjectSA_SQL.sql (the same SQL as the Use-Case Narrative With Query page)."""
import os
from drawio import Diagram, table, TITLE, NOTE, REL
from sqlparse import schema, queries

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(os.path.dirname(HERE), "ER Diagram", "ERD with Query")

# grid slot (col, row) for each table – same neighbourhood as the logical schema
SLOT = {"SKILL": (0, 0), "ENGINEER_SKILL": (1, 0), "EMPLOYEE": (2, 0), "PROJECT_ASSIGNMENT": (3, 0),
        "CUSTOMER": (1, 1), "PROJECT": (2, 1), "NOTIFICATION": (3, 1),
        "REQUIREMENT_ATTACHMENT": (0, 2), "CUSTOMER_REQUIREMENT": (1, 2), "TEST_SCHEDULE": (2, 2),
        "PROJECT_DOCUMENT": (3, 2)}
COLW, ROWH, W = 420, 480, 300
OPNAME = {"C": "INSERT", "R": "SELECT", "U": "UPDATE", "D": "DELETE"}
QROW = "fillColor=#FFF2CC;whiteSpace=wrap;"


def _center(r):
    return r[0] + r[2] / 2, r[1] + r[3] / 2


def _hits(a, b, r, pad=8):
    """does segment a-b pass through rectangle r?"""
    x0, y0, x1, y1 = r[0] - pad, r[1] - pad, r[0] + r[2] + pad, r[1] + r[3] + pad
    for i in range(1, 100):
        x, y = a[0] + (b[0] - a[0]) * i / 100, a[1] + (b[1] - a[1]) * i / 100
        if x0 < x < x1 and y0 < y < y1:
            return True
    return False


def route(d, tid, rect, child, parent, chan):
    """Straight FK line, or – when that would cross another table – a dog-leg through the column gap."""
    rc, rp = rect[child], rect[parent]
    a, b = _center(rc), _center(rp)
    st = REL.replace("edgeStyle=orthogonalEdgeStyle;", "")
    if not any(_hits(a, b, rect[o]) for o in rect if o not in (child, parent)):
        d.edge(tid[child], tid[parent], st)
        return
    right = b[0] >= a[0]
    gap_x = rc[0] + rc[2] + (COLW - W) / 2 if right else rc[0] - (COLW - W) / 2
    k = chan.get(round(gap_x), 0); chan[round(gap_x)] = k + 1
    gx = gap_x + (k * 14 if right else -k * 14)
    ey = rp[1] + 14 + 10 * k                       # enter the parent just under its header
    enter_right = gx > rp[0] + rp[2]
    st += (f"exitX={1 if right else 0};exitY=0.5;exitDx=0;exitDy=0;"
           f"entryX={1 if enter_right else 0};entryY={(ey - rp[1]) / rp[3]:.3f};entryDx=0;entryDy=0;")
    d.edge(tid[child], tid[parent], st, points=[(gx, rc[1] + rc[3] / 2), (gx, ey)])


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    sch, qs = schema(), queries()
    for uc in sorted({q["uc"] for q in qs}):
        uq = [q for q in qs if q["uc"] == uc and q["tables"]]
        used = [t for t in SLOT if any(t in q["tables"] for q in uq)]
        upd = {}
        for q in uq:
            for t, cols in q["set_cols"].items():
                upd.setdefault(t, [])
                upd[t] += [c for c in cols if c not in upd[t]]
        n = int(uc[2:])
        d = Diagram(f"ERD with Query {uc}", f"erdQuery{uc}", f"eq{n}")
        d.vertex(f"{uc} {uq[0]['ucname']} – ERD with Query", 40, 10, 1200, 40, TITLE)
        # only the grid columns / rows actually used, so each page stays compact
        cols = sorted({SLOT[t][0] for t in used}); rows = sorted({SLOT[t][1] for t in used})
        tid, rect = {}, {}
        for t in used:
            c, r = SLOT[t]
            x, y = 40 + cols.index(c) * COLW, 80 + rows.index(r) * ROWH
            body = []
            for col in sch[t]:
                if col["pk"] or col["fk"]:
                    k = "PK, FK" if col["pk"] and col["fk"] else "PK" if col["pk"] else "FK"
                    name = f"<u>{col['name']}</u>" if col["pk"] else col["name"]
                    body.append((f"<b>{k}</b>&nbsp;&nbsp;{name}", col["name"]))
            for col in upd.get(t, []):
                if not any(col == b[1] for b in body):
                    body.append((f"<b>U</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>{col}</i>", col))
            tags = []
            for q in uq:
                if t in q["tables"]:
                    ops = "".join(o for o in "CRUD" if o in q["ops"][t])
                    tags.append((f"<b>Q{q['qid']}</b> {'/'.join(OPNAME[o] for o in ops)}", f"q{q['qid']}", QROW))
            tid[t], _ = table(d, t, body + tags, x, y, w=W, cid=t)
            rect[t] = (x, y, W, 30 + 24 * len(body + tags))
        pairs, chan = [], {}
        for t in used:
            for col in sch[t]:
                p = col["fk"].split("(")[0] if col["fk"] else None
                if p in tid and p != t and (t, p) not in pairs:
                    pairs.append((t, p))
                    route(d, tid, rect, t, p, chan)
        path = os.path.join(OUT_DIR, f"ERDQ{n:02d}.drawio.xml")
        d.save(path)
        print(path, used)


if __name__ == "__main__":
    main()
