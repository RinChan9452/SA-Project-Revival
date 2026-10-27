"""Prefix each process box of a BP diagram with its process number, in flow order
(the order is listed below by cell id). Edits the file in place; other bytes are untouched."""
import re, sys
ORDER = {
    "As-If/As-If.drawio.xml": "S1 P1 P2 P3 M1 M2 M3 P4 S5 K2 K3 S6 S7 T1 T2 T4 T5 T9 T7",
    "To-Be/To-Be.drawio.xml": "S1 UC01 P0 P1 P2 P3 M1 M2 M3 M4 P4 S5 K2 S6 T1 T2 T3 T5 T6 M5 S7 D3 UC08 T10",
}
PREFIX = {"As-If/As-If.drawio.xml": "aiR-", "To-Be/To-Be.drawio.xml": "tbR-"}
for f, order in ORDER.items():
    src = open(f, encoding="utf-8").read()
    for n, short in enumerate(order.split(), 1):
        i = src.index(f'id="{PREFIX[f]}{short}"'); ts = src.rindex("<mxCell", 0, i); te = src.index(">", i)
        tag = src[ts:te]; m = re.search(r'value="([^"]*)"', tag)
        if re.match(r"\d+\.\s", m.group(1)): continue
        src = src[:ts] + tag[:m.start(1)] + f"{n}. " + tag[m.start(1):] + src[te:]
    open(f, "w", encoding="utf-8", newline="").write(src)
    print(f, "numbered", len(order.split()))
