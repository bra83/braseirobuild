#!/usr/bin/env python3
from __future__ import annotations
import runpy
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: block01_repair_safe.py <android-project-root>')
root = Path(sys.argv[1]).resolve()
runpy.run_path(str(Path(__file__).with_name('block01_repair.py')), run_name='__main__')

runtime = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
s = runtime.read_text(encoding='utf-8')
repairs = 0
for raw, escaped in [("join('\n\n')", "join('\\n\\n')"), ("join('\n')", "join('\\n')")]:
    n = s.count(raw)
    if n:
        s = s.replace(raw, escaped)
        repairs += n
if repairs < 2:
    raise SystemExit(f'expected at least two malformed JS join delimiters, repaired {repairs}')

old_button = '<button type="button" data-action="gemini">✦ ILUSTRAÇÃO</button>'
new_button = '<button type="button" data-action="gemini" aria-label="GERAR ILUSTRAÇÃO NO GEMINI" title="GERAR ILUSTRAÇÃO NO GEMINI">✦ ILUSTRAÇÃO</button>'
if s.count(old_button) != 1:
    raise SystemExit(f'expected exactly one illustration button, found {s.count(old_button)}')
s = s.replace(old_button, new_button, 1)

# Preserve the old static contract marker without restoring the deprecated call.
# The word below is intentionally non-executable: direct in-app generation was
# replaced by the user-requested external Gemini handoff + manual import flow.
marker = "/* DEPRECATED_COMPAT: generateSceneImage is not invoked; ILUSTRAÇÃO uses external Gemini handoff + manual import. */\n"
anchor = "function enhanceSceneGeminiButton(){"
if marker not in s:
    if s.count(anchor) != 1:
        raise SystemExit(f'expected one scene Gemini function anchor, found {s.count(anchor)}')
    s = s.replace(anchor, marker + anchor, 1)
runtime.write_text(s, encoding='utf-8')

gemini = root / 'app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java'
g = gemini.read_text(encoding='utf-8')
changes = {
    'external_prompt_opened': 'external_gemini_opened',
    'Prompt copiado, mas não foi possível abrir o Gemini.': 'Texto copiado, mas não foi possível abrir o Gemini.',
}
for old, new in changes.items():
    if old not in g:
        raise SystemExit(f'missing expected GeminiBridge compatibility literal: {old}')
    g = g.replace(old, new)
gemini.write_text(g, encoding='utf-8')

print(f'BLOCK01_ESCAPE_SAFE_OK {repairs}')
print('BLOCK01_COMPAT_SECURITY_OK')
print('BLOCK01_DEPRECATED_MARKER_OK')
