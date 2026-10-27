"""Read the Use-Case Narrative pages (draw.io tables) into rows of plain text."""
import html, os, re
import xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
REPORT = os.path.dirname(HERE)
PAGES = {"with": os.path.join(REPORT, "Use-Case", "Narrative", "Use-Case Narrative With Query.drawio.xml"),
         "without": os.path.join(REPORT, "Use-Case", "Narrative", "Use-Case Narrative Without Query.drawio.xml")}


def plain(v):
    v = html.unescape(v or "")
    v = re.sub(r"<br\s*/?>|</div>|</p>|</li>", "\n", v)
    v = re.sub(r"<[^>]+>", "", v)
    v = html.unescape(v).replace("\xa0", " ")
    lines = [re.sub(r"[ \t]+", " ", l).rstrip() for l in v.split("\n")]
    out = "\n".join(lines).strip("\n")
    return re.sub(r"\n{3,}", "\n\n", out)


def read(which="with"):
    """{UC id: {"title": str, "rows": [[cell text, ...], ...]}} in page order."""
    root = ET.parse(PAGES[which]).getroot()
    cells = {c.get("id"): c for c in root.iter("mxCell")}
    kids = {}
    for c in cells.values():
        kids.setdefault(c.get("parent"), []).append(c)
    geo = lambda c: c.find("mxGeometry")
    num = lambda c, k: float(geo(c).get(k, 0)) if geo(c) is not None else 0.0
    out = {}
    for t in cells.values():
        if "shape=table;" not in (t.get("style") or "") or t.get("vertex") != "1":
            continue
        title = plain(t.get("value"))
        m = re.match(r"(UC\d\d)", title)
        if not m:
            continue
        rows = []
        for r in sorted(kids.get(t.get("id"), []), key=lambda c: num(c, "y")):
            if "tableRow" not in (r.get("style") or ""):
                continue
            rcells = sorted(kids.get(r.get("id"), []), key=lambda c: num(c, "x"))
            vals = [plain(c.get("value")) for c in rcells if num(c, "width") > 0]
            rows.append(vals)
        out[m.group(1)] = {"title": title, "rows": rows}
    return dict(sorted(out.items()))


if __name__ == "__main__":
    import sys, io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    which = sys.argv[1] if len(sys.argv) > 1 else "with"
    for uc, d in read(which).items():
        print("=====", d["title"])
        for r in d["rows"]:
            print(" | ".join(x.replace("\n", " ⏎ ") for x in r))
