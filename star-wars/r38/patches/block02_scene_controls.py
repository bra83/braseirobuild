#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: block02_scene_controls.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
runtime = root / 'app/src/main/assets/vtt/r46/final-runtime.js'
css = root / 'app/src/main/assets/vtt/r46/final-runtime.css'
if not runtime.is_file() or not css.is_file():
    raise SystemExit('missing R46 runtime files')

s = runtime.read_text(encoding='utf-8')
old = " syncRecoveredSceneVisual(root,view);\n"
new = " syncRecoveredSceneVisual(root,view);\n enhanceSceneGeminiButton();\n"
if new not in s:
    count = s.count(old)
    if count != 1:
        raise SystemExit(f'expected exactly one recovered-scene mount point, found {count}')
    s = s.replace(old, new, 1)

if s.count('function enhanceSceneGeminiButton(){') != 1:
    raise SystemExit('unexpected enhanceSceneGeminiButton definition count')
if s.count('enhanceSceneGeminiButton();') < 1:
    raise SystemExit('scene Gemini controls are not mounted')
runtime.write_text(s, encoding='utf-8')

# The approved media frame is 150px tall with overflow:hidden and the recovered
# image occupies 100% of that height. The controls therefore must be positioned
# inside that existing frame rather than flowing after the image and being clipped.
c = css.read_text(encoding='utf-8')
old_css = '.final-scene-gemini{align-items:flex-end;flex-wrap:wrap}'
new_css = '.final-scene-gemini{position:absolute;left:8px;right:8px;bottom:8px;z-index:5;display:flex;align-items:flex-end;justify-content:space-between;gap:6px;flex-wrap:wrap;pointer-events:auto}\n.final-scene-button-row button{min-height:44px}'
if new_css not in c:
    count = c.count(old_css)
    if count != 1:
        raise SystemExit(f'expected exactly one scene controls CSS anchor, found {count}')
    c = c.replace(old_css, new_css, 1)
css.write_text(c, encoding='utf-8')

if 'position:absolute;left:8px;right:8px;bottom:8px;z-index:5' not in c:
    raise SystemExit('scene controls positioning not applied')
if '.final-scene-button-row button{min-height:44px}' not in c:
    raise SystemExit('scene controls tap target hardening not applied')

print('BLOCK02_SCENE_CONTROLS_MOUNT_OK')
print('BLOCK02_SCENE_CONTROLS_VISIBILITY_OK')
