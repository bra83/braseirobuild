#!/usr/bin/env python3
from __future__ import annotations
import os
import runpy
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: block01_repair_safe.py <android-project-root>')
root = Path(sys.argv[1]).resolve()
# Execute the audited Block01 patch unchanged first.
runpy.run_path(str(Path(__file__).with_name('block01_repair.py')), run_name='__main__')
# Python re.sub replacement processing in the original patch turns JS '\\n'
# escapes inside single-quoted join delimiters into literal newlines. Repair all
# such delimiters after generation, without touching valid multiline template text.
p = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
s = p.read_text(encoding='utf-8')
broken = "join('\n')"  # contains an actual newline in Python
count = s.count(broken)
if count < 1:
    raise SystemExit('expected at least one malformed JS join delimiter')
s = s.replace(broken, "join('\\n')")
p.write_text(s, encoding='utf-8')
print(f'BLOCK01_ESCAPE_SAFE_OK {count}')
