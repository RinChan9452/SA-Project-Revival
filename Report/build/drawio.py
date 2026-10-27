"""Tiny helper to write draw.io (.drawio.xml) files in the same plain format as the other Report diagrams."""
from xml.sax.saxutils import escape, quoteattr


class Diagram:
    def __init__(self, name, page_id, prefix):
        self.name, self.page_id, self.p = name, page_id, prefix
        self.cells = []
        self.n = 0

    def _id(self, cid=None):
        if cid:
            return f"{self.p}-{cid}"
        self.n += 1
        return f"{self.p}-a{self.n}"

    def vertex(self, value, x, y, w, h, style, cid=None, parent=None):
        i = self._id(cid)
        self.cells.append(f'        <mxCell id="{i}" parent="{parent or self.p + "-1"}" style={quoteattr(style)} '
                          f'value={quoteattr(value)} vertex="1">\n'
                          f'          <mxGeometry x="{x:g}" y="{y:g}" width="{w:g}" height="{h:g}" as="geometry" />\n'
                          f'        </mxCell>\n')
        return i

    def edge(self, src, tgt, style, value="", cid=None, points=None, label_xy=None):
        i = self._id(cid)
        pts = ""
        if points:
            pts = "            <Array as=\"points\">\n" + "".join(
                f'              <mxPoint x="{x:g}" y="{y:g}" />\n' for x, y in points) + "            </Array>\n"
        geo_attr = f' x="{label_xy[0]:g}" y="{label_xy[1]:g}"' if label_xy else ""
        self.cells.append(f'        <mxCell id="{i}" edge="1" parent="{self.p}-1" source="{src}" target="{tgt}" '
                          f'style={quoteattr(style)} value={quoteattr(value)}>\n'
                          f'          <mxGeometry relative="1"{geo_attr} as="geometry">\n{pts}          </mxGeometry>\n'
                          f'        </mxCell>\n')
        return i

    def save(self, path):
        body = (f'<?xml version="1.0" encoding="UTF-8"?>\n<mxfile host="app.diagrams.net">\n'
                f'  <diagram name={quoteattr(self.name)} id="{self.page_id}">\n'
                f'    <mxGraphModel dx="1600" dy="1000" grid="0" gridSize="10" guides="1" tooltips="1" connect="1" '
                f'arrows="1" fold="1" page="0" pageScale="1" pageWidth="1169" pageHeight="827" math="0" shadow="0">\n'
                f'      <root>\n        <mxCell id="{self.p}-0" />\n        <mxCell id="{self.p}-1" parent="{self.p}-0" />\n'
                + "".join(self.cells) +
                '      </root>\n    </mxGraphModel>\n  </diagram>\n</mxfile>\n')
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(body)


TITLE = "text;html=1;fontSize=24;fontStyle=1;align=left;verticalAlign=middle;"
NOTE = "rounded=0;whiteSpace=wrap;html=1;fontSize=13;align=left;verticalAlign=top;spacing=10;"
TABLE = ("swimlane;fontStyle=1;childLayout=stackLayout;horizontal=1;startSize=30;horizontalStack=0;resizeParent=1;"
         "resizeParentMax=0;resizeLast=0;collapsible=0;marginBottom=0;html=1;fontSize=13;fillColor=#F5F5F5;")
ROW = "text;strokeColor=none;fillColor=none;align=left;verticalAlign=middle;spacingLeft=8;html=1;fontSize=12;"
REL = "edgeStyle=orthogonalEdgeStyle;rounded=0;html=1;endArrow=ERmandOne;startArrow=ERmany;endFill=0;startFill=0;endSize=10;startSize=10;"


def table(d, name, rows, x, y, w=250, row_h=24, cid=None):
    """rows: [(label_html, key)] or [(label_html, key, extra_style)] -> returns (table id, {key: row id})"""
    t = d.vertex(name, x, y, w, 30 + row_h * len(rows), TABLE, cid=cid)
    ids = {}
    for k, r in enumerate(rows):
        label, key, extra = (r + ("",))[:3]
        ids[key] = d.vertex(label, 0, 30 + row_h * k, w, row_h, ROW + extra, cid=f"{cid}-r{k}" if cid else None, parent=t)
    return t, ids
