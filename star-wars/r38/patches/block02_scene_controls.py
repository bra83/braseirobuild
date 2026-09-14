#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: block02_scene_controls.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
runtime = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
if not runtime.is_file():
    raise SystemExit(f'missing runtime: {runtime}')

s = runtime.read_text(encoding='utf-8')
old = " syncRecoveredSceneVisual(root,view);\n"
new = " syncRecoveredSceneVisual(root,view);\n enhanceSceneGeminiButton();\n"
count = s.count(old)
if count != 1:
    raise SystemExit(f'expected exactly one recovered-scene mount point, found {count}')
if ' syncRecoveredSceneVisual(root,view);\n enhanceSceneGeminiButton();\n' not in s:
    s = s.replace(old, new, 1)

# Fail closed: function must exist exactly once and the live scene enhancer must call it.
if s.count('function enhanceSceneGeminiButton(){') != 1:
    raise SystemExit('unexpected enhanceSceneGeminiButton definition count')
if s.count('enhanceSceneGeminiButton();') < 1:
    raise SystemExit('scene Gemini controls are not mounted')

runtime.write_text(s, encoding='utf-8')
print('BLOCK02_SCENE_CONTROLS_MOUNT_OK')
