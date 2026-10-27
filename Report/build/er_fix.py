"""ER Diagram (Report copy): add FK attributes for every 1:M relationship and draw total
participation as a double line. Run once on the original (idempotent check on erC-fk-)."""
import re, sys
f = "ER Diagram/ER Diagram.xml"
s = open(f, encoding="utf-8").read()
if "erC-fk-" in s: sys.exit("already done")
TOTAL = "e5 e9 e13 e16 e18 e19 e21 e23 e24 e27 e28 e29 e31".split()
for e in TOTAL:
    s = re.sub(rf'(<mxCell id="erC-{e}"[^>]*style=")', r"\1shape=link;", s, count=1)
FK = [  # id, entity, label, x, y
    ("prjCus", "PRJ", "CustomerID (FK)", 1545, 790), ("prjSale", "PRJ", "SaleID (FK)", 1515, 860),
    ("prjPre", "PRJ", "PresaleID (FK)", 1485, 930), ("prjPM", "PRJ", "PMID (FK)", 1455, 1000),
    ("reqRev", "REQ", "ReviewedByID (FK)", 700, 540), ("reqBy", "REQ", "CreatedByID (FK)", 690, 610),
    ("reqPrj", "REQ", "ProjectID (FK)", 690, 680),
    ("schPrj", "SCH", "ProjectID (FK)", 1440, 1580), ("schBy", "SCH", "CreatedByID (FK)", 1700, 1560),
    ("docPrj", "DOC", "ProjectID (FK)", 2215, 635),
    ("notPrj", "NOT", "ProjectID (FK)", 1900, 1200), ("notRec", "NOT", "RecipientID (FK)", 1925, 1420),
]
add = []
for i, ent, lab, x, y in FK:
    add.append(f'        <mxCell id="erC-fk-{i}" parent="erC-1" style="ellipse;whiteSpace=wrap;html=1;shapeInside=1;dashed=1;fontStyle=2;" value="{lab}" vertex="1">\n'
               f'          <mxGeometry height="42" width="124" x="{x}" y="{y}" as="geometry" />\n        </mxCell>\n')
    add.append(f'        <mxCell id="erC-fke-{i}" edge="1" parent="erC-1" source="erC-{ent}" target="erC-fk-{i}" style="rounded=0;orthogonalLoop=1;jettySize=auto;html=1;endArrow=none;endFill=0;" value="">\n'
               f'          <mxGeometry relative="1" as="geometry" />\n        </mxCell>\n')
legend = ('        <mxCell id="erC-legend" parent="erC-1" style="rounded=0;whiteSpace=wrap;html=1;fontSize=13;align=left;verticalAlign=top;spacing=10;" '
          'value="&lt;b&gt;สัญลักษณ์&lt;/b&gt;&lt;br&gt;เส้นคู่ = Total participation (ทุก instance ต้องมีความสัมพันธ์)&lt;br&gt;เส้นเดี่ยว = Partial participation&lt;br&gt;'
          'วงรีขีดเส้นใต้ = Primary Key (PK)&lt;br&gt;วงรีเส้นประ (FK) = Foreign Key ของความสัมพันธ์ 1:M&lt;br&gt;วงรีเส้นคู่ = Multivalued attribute&lt;br&gt;'
          '(T,D) = Total, Disjoint specialization" vertex="1">\n'
          '          <mxGeometry height="150" width="430" x="-600" y="20" as="geometry" />\n        </mxCell>\n')
i = s.rindex("      </root>"); s = s[:i] + "".join(add) + legend + s[i:]
open(f, "w", encoding="utf-8", newline="").write(s)
print("ok")
