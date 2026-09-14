#!/usr/bin/env python3
from __future__ import annotations
import runpy
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: block01_repair_safe.py <android-project-root>')
root = Path(sys.argv[1]).resolve()
runpy.run_path(str(Path(__file__).with_name('block01_repair.py')), run_name='__main__')
p = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
s = p.read_text(encoding='utf-8')
repairs = 0
for raw, escaped in [("join('\n\n')", "join('\\n\\n')"), ("join('\n')", "join('\\n')")]:
    n = s.count(raw)
    if n:
        s = s.replace(raw, escaped)
        repairs += n
if repairs < 2:
    raise SystemExit(f'expected at least two malformed JS join delimiters, repaired {repairs}')
p.write_text(s, encoding='utf-8')
print(f'BLOCK01_ESCAPE_SAFE_OK {repairs}')
