"""Render draw.io XML files to PNG with headless Chrome + the draw.io viewer.

usage: python render.py OUT_DIR file1.xml [file2.xml ...]
Each page (<diagram>) of a file becomes OUT_DIR/<file stem>[_<n>].png
"""
import base64, html, json, os, subprocess, sys, time, re
import xml.etree.ElementTree as ET
import requests, websocket

HERE = os.path.dirname(os.path.abspath(__file__))
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
PORT = 9333
SCALE = 2


def start_chrome():
    prof = os.path.join(HERE, ".cache", "chrome-profile")
    p = subprocess.Popen([CHROME, "--headless=new", f"--remote-debugging-port={PORT}",
                          f"--user-data-dir={prof}", "--allow-file-access-from-files",
                          "--disable-gpu", "--hide-scrollbars", "--window-size=1600,1200",
                          "about:blank"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    for _ in range(50):
        try:
            tabs = requests.get(f"http://127.0.0.1:{PORT}/json").json()
            page = [t for t in tabs if t["type"] == "page"][0]
            return p, page["webSocketDebuggerUrl"]
        except Exception:
            time.sleep(0.2)
    raise SystemExit("chrome did not start")


class CDP:
    def __init__(self, url):
        self.ws = websocket.create_connection(url, timeout=120, suppress_origin=True)
        self.i = 0

    def call(self, method, **params):
        self.i += 1
        self.ws.send(json.dumps({"id": self.i, "method": method, "params": params}))
        while True:
            m = json.loads(self.ws.recv())
            if m.get("id") == self.i:
                if "error" in m:
                    raise RuntimeError(m["error"])
                return m.get("result", {})

    def js(self, expr):
        r = self.call("Runtime.evaluate", expression=expr, returnByValue=True, awaitPromise=True)
        return r.get("result", {}).get("value")


PAGE = """<!doctype html><html><head><meta charset="utf-8">
<style>html,body{margin:0;background:#fff}#c{display:inline-block;padding:16px;background:#fff}</style>
</head><body><div id="c"></div>
<script src="viewer-static.min.js"></script><script>
try{var d=mxUtils.parseXml(%s);
new GraphViewer(document.getElementById('c'),d.documentElement,{highlight:'none',nav:false,toolbar:null,border:0});
}catch(e){document.title="ERR "+e}</script></body></html>"""


def render(cdp, xml_text, out_png):
    page = os.path.join(HERE, ".cache", "page.html")
    with open(page, "w", encoding="utf-8") as f:
        f.write(PAGE % json.dumps(xml_text).replace("</", "<\\/"))
    cdp.call("Emulation.setDeviceMetricsOverride", width=8000, height=8000, deviceScaleFactor=1, mobile=False)
    cdp.call("Page.navigate", url="file:///" + page.replace("\\", "/"))
    for _ in range(200):
        time.sleep(0.15)
        ok = cdp.js("!!document.querySelector('#c svg') && document.readyState=='complete'")
        if ok:
            break
    time.sleep(1.0)  # let fonts settle
    box = cdp.js("(()=>{const r=document.getElementById('c').getBoundingClientRect();return [r.x,r.y,r.width,r.height]})()")
    x, y, w, h = box
    shot = cdp.call("Page.captureScreenshot", format="png", captureBeyondViewport=True,
                    clip={"x": x, "y": y, "width": w, "height": h, "scale": SCALE})
    with open(out_png, "wb") as f:
        f.write(base64.b64decode(shot["data"]))
    return int(w), int(h)


def main():
    out_dir, files = sys.argv[1], sys.argv[2:]
    os.makedirs(out_dir, exist_ok=True)
    proc, url = start_chrome()
    try:
        cdp = CDP(url)
        cdp.call("Page.enable")
        for fpath in files:
            raw = open(fpath, encoding="utf-8").read()
            root = ET.fromstring(raw.encode("utf-8"))
            diagrams = root.findall("diagram")
            stem = os.path.splitext(os.path.basename(fpath))[0].replace(".drawio", "")
            for n, d in enumerate(diagrams):
                single = ET.Element("mxfile")
                single.append(d)
                name = stem if len(diagrams) == 1 else f"{stem}_{n + 1}"
                out = os.path.join(out_dir, re.sub(r"[^\w\-]+", "_", name) + ".png")
                w, h = render(cdp, ET.tostring(single, encoding="unicode"), out)
                print(f"{out}  {w}x{h}")
    finally:
        proc.terminate()


if __name__ == "__main__":
    main()
