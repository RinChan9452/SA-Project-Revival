"""Number every unnumbered call message on SQ0N and the matching label on CL0N.

Rule: an unnumbered call becomes 'N<letter>' where N is the step of the next numbered
message (step 'N.' or query 'N.M'), e.g. onClick() before Query 2.1 -> '2a'.
Edits the files in place, keeping the rest of the text byte for byte."""
import xml.etree.ElementTree as ET, re, html, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
NUM = re.compile(r"^(\d+)(\.\d*)?\s")
def plain(v): return re.sub(r"\s+", " ", re.sub("<[^>]+>", " ", html.unescape(v or ""))).strip()

def plan(sq_path):
    rows = []
    for c in ET.parse(sq_path).getroot().iter("mxCell"):
        st = c.get("style") or ""
        if c.get("edge") != "1" or "endArrow=none" in st: continue
        sp = c.find("mxGeometry/mxPoint[@as='sourcePoint']")
        rows.append((float(sp.get("y")), "dashed=1" in st, c.get("id"), c.get("value") or ""))
    rows.sort()
    out, letters = [], {}
    for i, (y, dashed, cid, val) in enumerate(rows):
        if dashed or NUM.match(plain(val)) or not plain(val): continue
        nxt = next((NUM.match(plain(v)).group(1) for _, d, _, v in rows[i+1:] if NUM.match(plain(v))), None)
        letters[nxt] = letters.get(nxt, 0) + 1
        out.append((cid, val, f"{nxt}{'abcdefghij'[letters[nxt]-1]}"))
    return out

for n in range(1, 9):
    sq = f"Sequence Diagram/SQ0{n}.drawio.xml"; cl = f"Collaboration Diagram/CL0{n}.drawio.xml"
    msgs = plan(sq)
    s = open(sq, encoding="utf-8").read(); c = open(cl, encoding="utf-8").read()
    for cid, val, num in msgs:
        attr_old = f'id="{cid}"'
        i = s.index(attr_old); j = s.index('value="', i) if s.index('value="', i) < s.index(">", i) else None
        # value attribute of this cell (attributes may come in any order inside the tag)
        tag_end = s.index(">", i); tag_start = s.rindex("<mxCell", 0, i)
        tag = s[tag_start:tag_end]
        m = re.search(r'value="([^"]*)"', tag)
        newtag = tag[:m.start(1)] + f"{num} " + tag[m.start(1):]
        s = s[:tag_start] + newtag + s[tag_end:]
        # collaboration label: escaped text after an arrow, first one not yet numbered
        esc = m.group(1)
        pat = re.compile(r"([→←↑↓↗↘↙↖] )" + re.escape(esc))
        c, k = pat.subn(lambda mm: mm.group(1) + f"{num} " + esc, c, count=1)
        print(f"UC0{n} {num:4} {plain(val)[:60]}" + ("" if k else "   !!! NOT FOUND IN CL"))
    open(sq, "w", encoding="utf-8", newline="").write(s)
    open(cl, "w", encoding="utf-8", newline="").write(c)
