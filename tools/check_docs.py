# -*- coding: utf-8 -*-
"""Consistency check for the design files (CLAUDE.md §8). Exit code 1 if anything is wrong.

1. ProjectSA_Revised.drawio.xml parses; no duplicate mxCell ids; every parent / source / target exists
   (pages copied unchanged from the team's original file are reported, not failed, if they had dangling edges).
2. Every use-case query in ProjectSA_SQL.sql appears with identical text on "Use-Case Narrative With Query".
3. Every SQL message on "Sequence UC0N" ("<b>N.M</b> SELECT ...") matches the SQL file
   (a message may carry one statement of a multi-statement query).

usage:  python tools/check_docs.py
"""
import html, os, re, sys
import xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding="utf-8")

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Project Source")
DRAW = os.path.join(ROOT, "ProjectSA_Revised.drawio.xml")
SQL = os.path.join(ROOT, "ProjectSA_SQL.sql")
problems = []


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


def text(v):
    return html.unescape(re.sub(r"<[^>]+>", "", re.sub(r"<br\s*/?>", "\n", v or ""))).replace("\xa0", " ")


# ---- SQL file
sections, cur = {}, None
src = open(SQL, encoding="utf-8-sig").read()
for blk in re.split(r"\n(?=-- (?:=+\n|Query ))", "\n" + src[src.index("-- ===="):]):
    blk = blk.strip("\n")
    m = re.match(r"-- =+\n-- ((?:UC\d\d|F\d))\b", blk)
    if m:
        cur = m.group(1)
        sections[cur] = {}
        continue
    m = re.match(r"-- Query ([\w.]+)(?:: [^\n]*)?\n(.*)", blk, re.S)
    if m and cur:
        body = m.group(2).strip()
        sections[cur][m.group(1)] = None if body == "-- (no statement)" else body

# ---- drawio structure
root = ET.parse(DRAW).getroot()
pages = {d.get("name"): d for d in root.findall("diagram")}
if root.get("pages") != str(len(pages)):
    problems.append(f'mxfile pages="{root.get("pages")}" but {len(pages)} <diagram> elements')
ORIGINAL = {"As-If", "Fixed As-If V.2", "To-Be", "Clean To-Be"}
for name, d in pages.items():
    cells = list(d.iter("mxCell"))
    ids = [c.get("id") for c in cells]
    known = set(ids)
    dup = len(ids) - len(known)
    broken = [c.get("id") for c in cells if (c.get("parent") and c.get("parent") not in known)
              or (c.get("edge") and any(c.get(k) and c.get(k) not in known for k in ("source", "target")))]
    if dup or broken:
        (print if name in ORIGINAL else problems.append)(f"page {name!r}: {dup} duplicate ids, broken refs {broken[:5]}")

# ---- With Query page == SQL file
d = pages["Use-Case Narrative With Query"]
cells = list(d.iter("mxCell"))
parent = {c.get("id"): c for c in cells}
page_q = {}
for c in cells:
    v = text(c.get("value"))
    if "Query " not in v:
        continue
    t = c
    while t is not None and not ("shape=table" in (t.get("style") or "") and text(t.get("value")).strip().startswith("UC")):
        t = parent.get(t.get("parent"))
    if t is None:
        continue
    uc = text(t.get("value")).split()[0]
    for m in re.finditer(r"Query (\d+\.\d+):[^\n]*\n(.*?)(?=\nQuery \d|\n\s*\nStep|\nStep \d|\Z)", v, re.S):
        page_q[(uc, m.group(1))] = norm(m.group(2))
n_uc = 0
for uc, qs in sections.items():
    if not uc.startswith("UC"):
        continue
    for qid, body in qs.items():
        if body is None:
            continue
        n_uc += 1
        if page_q.get((uc, qid)) != norm(body):
            problems.append(f"With Query page differs from SQL file: {uc} Query {qid}")

# ---- Sequence SQL messages == SQL file
n_msg = 0
for name, d in pages.items():
    m = re.match(r"Sequence (UC\d\d)$", name)
    if not m:
        continue
    uc = m.group(1)
    for c in d.iter("mxCell"):
        v = c.get("value") or ""
        mm = re.match(r"<b>(\d+\.\d+)</b>", v)
        if not (c.get("edge") and mm):
            continue
        n_msg += 1
        body = norm(text(re.sub(r"^<b>[\d.]+</b>(&nbsp;)?", "", v)))
        body = re.sub(r" -- \d+\. .*$", "", body)          # trailing explanatory comment on a message
        full = sections.get(uc, {}).get(mm.group(1))
        stmts = [norm(s) + ";" for s in (full or "").split(";") if s.strip()]
        if full is None or (body != norm(full) and body not in stmts):
            problems.append(f"{name}: message {mm.group(1)} differs from SQL file")

print(f"pages: {len(pages)} | use-case queries checked on With Query page: {n_uc} | sequence SQL messages: {n_msg}")
for p in problems:
    print("PROBLEM:", p)
print("OK" if not problems else f"{len(problems)} problem(s)")
sys.exit(1 if problems else 0)
