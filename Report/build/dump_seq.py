import xml.etree.ElementTree as ET, re, html, glob, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
def txt(v): return re.sub(r"\s+"," ",re.sub("<[^>]+>"," ",html.unescape(html.unescape(v or "")))).strip()
for f in sorted(glob.glob("Sequence Diagram/SQ0*.xml")):
    print("=====",f)
    rows=[]
    for c in ET.parse(f).getroot().iter("mxCell"):
        if c.get("edge")=="1":
            g=c.find("mxGeometry"); sp=g.find("mxPoint[@as='sourcePoint']") if g is not None else None
            y=float(sp.get("y")) if sp is not None else -1
            dashed="dashed=1" in (c.get("style") or "")
            if "endArrow=none" in (c.get("style") or ""): continue
            rows.append((y,"RET" if dashed else "CALL",c.get("id"),txt(c.get("value"))[:70]))
    for r in sorted(rows): print(*r)
