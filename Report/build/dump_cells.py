import xml.etree.ElementTree as ET,re,html,sys,io
sys.stdout=io.TextIOWrapper(sys.stdout.buffer,encoding="utf-8")
f=sys.argv[1]
cells={c.get("id"):c for c in ET.parse(f).getroot().iter("mxCell")}
def absxy(c):
    x=y=0
    while c is not None:
        g=c.find("mxGeometry")
        if g is not None and c.get("vertex")=="1": x+=float(g.get("x",0)); y+=float(g.get("y",0))
        c=cells.get(c.get("parent"))
    return x,y
rows=[]
for c in cells.values():
    if c.get("vertex")!="1": continue
    v=re.sub(r"\s+"," ",re.sub("<[^>]+>"," ",html.unescape(c.get("value") or ""))).strip()
    st=c.get("style") or ""; shape=("rhombus" if "rhombus" in st else "ellipse" if "ellipse" in st else "rounded" if "rounded=1" in st else st.split(";")[0][:25])
    x,y=absxy(c); rows.append((round(y),round(x),c.get("id"),shape,v[:60]))
for r in sorted(rows): print(*r)
