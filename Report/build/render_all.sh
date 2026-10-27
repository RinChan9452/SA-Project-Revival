#!/bin/sh
# Render every Report diagram (except the narrative pages) to Report/Images/*.png
cd "$(dirname "$0")/.." || exit 1
PY="/c/Program Files/Python311/python"
find . -name "*.xml" -not -path "./build/*" -not -path "*/Narrative/*" -print0 | xargs -0 "$PY" build/render.py Images
