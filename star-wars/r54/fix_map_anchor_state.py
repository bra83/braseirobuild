#!/usr/bin/env python3
from __future__ import annotations
import sys
from pathlib import Path

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_map_anchor_state.py <android-project-root>')

root = Path(sys.argv[1]).resolve()
vtt = root / 'app/src/main/assets/vtt'
files = [vtt / 'js/core/controller.mjs', vtt / 'js/core/uiBridge.bundle.js']
old_sig = "initializeLivingSandbox({ startingPoint = 'Porto de Fronteira', regionLabel = 'Região de Fronteira' } = {}) {"
new_sig = "initializeLivingSandbox({ startingPoint = 'Porto de Fronteira', regionLabel = 'Região de Fronteira', startMapPoint = null } = {}) {"
old_seed = "const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });"
new_seed = """if (startMapPoint !== null && (!Number.isFinite(startMapPoint.x) || !Number.isFinite(startMapPoint.y) || startMapPoint.x < 0 || startMapPoint.x > 100 || startMapPoint.y < 0 || startMapPoint.y > 100)) throw new Error('Ponto do mapa inválido.');
        const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });
        if (result.created && startMapPoint !== null) result.world.sandbox.startMapPoint = { x: startMapPoint.x, y: startMapPoint.y, mapId: 'local.map.galaxy', source: 'player_selected' };"""

for path in files:
    if not path.is_file():
        raise SystemExit(f'missing map engine file: {path}')
    text = path.read_text(encoding='utf-8')
    if new_sig not in text:
        if text.count(old_sig) != 1:
            raise SystemExit(f'{path}: map signature anchor count={text.count(old_sig)}')
        text = text.replace(old_sig, new_sig, 1)
    if "result.world.sandbox.startMapPoint" not in text:
        if text.count(old_seed) != 1:
            raise SystemExit(f'{path}: sandbox seed anchor count={text.count(old_seed)}')
        text = text.replace(old_seed, new_seed, 1)
    path.write_text(text, encoding='utf-8')
    verify = path.read_text(encoding='utf-8')
    assert new_sig in verify
    assert "result.world.sandbox.startMapPoint" in verify
    assert "source: 'player_selected'" in verify

runtime = vtt / 'r46/final-runtime.js'
rt = runtime.read_text(encoding='utf-8')
if "startMapPoint:{x:chosen.x,y:chosen.y}" not in rt:
    raise SystemExit('R54 UI does not forward chosen map coordinates')

print('R54 MAP ANCHOR STATE PATCH PASS')
