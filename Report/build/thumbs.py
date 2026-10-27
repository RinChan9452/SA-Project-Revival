"""Render Report.pdf into contact sheets (20 pages each) for a visual check: python thumbs.py OUT_DIR"""
import os, sys, pymupdf as fitz
from PIL import Image
out = sys.argv[1]
doc = fitz.open(os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "Report.pdf"))
th = []
for p in doc:
    pix = p.get_pixmap(dpi=40)
    th.append(Image.frombytes("RGB", (pix.width, pix.height), pix.samples))
for k in range(0, len(th), 20):
    sh = Image.new("RGB", (5 * 340, 4 * 340), "#888")
    for j, im in enumerate(th[k:k + 20]):
        im = im.copy(); im.thumbnail((330, 330)); sh.paste(im, ((j % 5) * 340 + 5, (j // 5) * 340 + 5))
    sh.save(f"{out}/sheet_{k // 20:02d}.png")
print(len(th))
