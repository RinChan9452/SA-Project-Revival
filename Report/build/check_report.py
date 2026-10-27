"""Run tools/check_docs.py against the Report/ diagram copies, plus Report-only checks:
- every numbered message on SQ0N appears on CL0N and vice versa;
- the CRUD Table page agrees with the SQL (C/R/U per UC x table)."""
import glob, html, os, re, sys, tempfile
import xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding="utf-8")
HERE = os.path.dirname(os.path.abspath(__file__)); REPORT = os.path.dirname(HERE); ROOT = os.path.dirname(REPORT)
sys.path.insert(0, HERE)
from sqlparse import queries, TABLE_ORDER

files = [f for f in glob.glob(os.path.join(REPORT, "**", "*.xml"), recursive=True) if os.sep + "build" + os.sep not in f]
mx = ET.Element("mxfile", {"host": "app.diagrams.net"})
for f in files:
    for d in ET.parse(f).getroot().findall("diagram"):
        mx.append(d)
mx.set("pages", str(len(mx)))
tmp = os.path.join(tempfile.gettempdir(), "report_all.drawio.xml")
ET.ElementTree(mx).write(tmp, encoding="utf-8", xml_declaration=True)
src = open(os.path.join(ROOT, "tools", "check_docs.py"), encoding="utf-8").read()
src = src.replace('DRAW = os.path.join(ROOT, "ProjectSA_Revised.drawio.xml")', f'DRAW = {tmp!r}')
src = src.replace('ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Project Source")',
                  f'ROOT = {os.path.join(ROOT, "Project Source")!r}')
print("== tools/check_docs.py on Report copies")
try:
    exec(compile(src, "check_docs.py", "exec"), {"__name__": "__main__"})
except SystemExit as e:
    print("check_docs exit:", e.code)

def labels(path):
    out = []
    for c in ET.parse(path).getroot().iter("mxCell"):
        v = html.unescape(html.unescape(c.get("value") or ""))
        for line in re.split(r"<br\s*/?>|\n", v):
            t = re.sub("<[^>]+>", "", line).strip().lstrip("→←↑↓↗↘↙↖ ").strip()
            m = re.match(r"^(\d+[a-j]|\d+\.\d+|\d+\.)\s", t)
            if m:
                out.append(m.group(1))
    return out

bad = 0
print("== SQ vs CL numbered messages")
for n in range(1, 9):
    sq = set(labels(os.path.join(REPORT, "Sequence Diagram", f"SQ0{n}.drawio.xml")))
    cl = set(labels(os.path.join(REPORT, "Collaboration Diagram", f"CL0{n}.drawio.xml")))
    if sq != cl:
        bad += 1
        print(f"UC0{n}: only on SQ {sorted(sq - cl)}  only on CL {sorted(cl - sq)}")
    else:
        print(f"UC0{n}: {len(sq)} numbers match")

print("== CRUD Table vs SQL")
crud = {}
root = ET.parse(os.path.join(REPORT, "CRUD", "CRUD Table.drawio.xml")).getroot()
cells = {c.get("id"): c for c in root.iter("mxCell")}
for cid, c in cells.items():
    m = re.match(r"crud-c(\d+)-(\d+)$", cid)
    if m:
        crud[(int(m.group(1)), int(m.group(2)))] = re.sub("<[^>]+>", "", html.unescape(c.get("value") or "")).strip()
heads = {int(re.match(r"crud-h(\d+)", k).group(1)): re.sub(r"<[^>]+>|\s", "", html.unescape(c.get("value") or ""))
         for k, c in cells.items() if re.match(r"crud-h\d+$", k)}
qs = queries()
for r in range(8):
    uc = f"UC0{r + 1}"
    for col, t in heads.items():
        if t not in TABLE_ORDER:
            continue
        want = "".join(o for o in "CRUD" if any(o in q["ops"].get(t, ()) for q in qs if q["uc"] == uc))
        have = crud.get((r, col), "")
        if want != have:
            bad += 1
            print(f"{uc} {t}: CRUD page '{have}' but SQL gives '{want}'")
print(f"CRUD cells compared: {sum(1 for (r, c) in crud if r < 8 and heads.get(c) in TABLE_ORDER)}"); print("ALL OK" if not bad else f"{bad} problem(s)")
