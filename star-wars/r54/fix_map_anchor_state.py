#!/usr/bin/env python3
from __future__ import annotations
import subprocess
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
legacy_patched_seed = """if (startMapPoint !== null && (!Number.isFinite(startMapPoint.x) || !Number.isFinite(startMapPoint.y) || startMapPoint.x < 0 || startMapPoint.x > 100 || startMapPoint.y < 0 || startMapPoint.y > 100)) throw new Error('Ponto do mapa inválido.');
        const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });
        if (result.created && startMapPoint !== null) result.world.sandbox.startMapPoint = { x: startMapPoint.x, y: startMapPoint.y, mapId: 'local.map.galaxy', source: 'player_selected' };"""
new_seed = """if (startMapPoint !== null && (!Number.isFinite(startMapPoint.x) || !Number.isFinite(startMapPoint.y) || startMapPoint.x < 0 || startMapPoint.x > 100 || startMapPoint.y < 0 || startMapPoint.y > 100)) throw new Error('Ponto do mapa inválido.');
        const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });
        if (result.created) {
            result.world.sandbox.startLocationId = result.startLocationId;
            if (startMapPoint !== null) result.world.sandbox.startMapPoint = { x: startMapPoint.x, y: startMapPoint.y, mapId: 'local.map.galaxy', source: 'player_selected' };
        }"""

for path in files:
    if not path.is_file():
        raise SystemExit(f'missing map engine file: {path}')
    text = path.read_text(encoding='utf-8')
    if new_sig not in text:
        if text.count(old_sig) != 1:
            raise SystemExit(f'{path}: map signature anchor count={text.count(old_sig)}')
        text = text.replace(old_sig, new_sig, 1)
    if "result.world.sandbox.startLocationId = result.startLocationId" not in text:
        if legacy_patched_seed in text:
            text = text.replace(legacy_patched_seed, new_seed, 1)
        elif text.count(old_seed) == 1:
            text = text.replace(old_seed, new_seed, 1)
        else:
            raise SystemExit(f'{path}: sandbox seed upgrade anchor unavailable')
    path.write_text(text, encoding='utf-8')
    verify = path.read_text(encoding='utf-8')
    assert new_sig in verify
    assert "result.world.sandbox.startLocationId = result.startLocationId" in verify
    assert "result.world.sandbox.startMapPoint" in verify
    assert "source: 'player_selected'" in verify

runtime = vtt / 'r46/final-runtime.js'
rt = runtime.read_text(encoding='utf-8')
if "startMapPoint:{x:chosen.x,y:chosen.y}" not in rt:
    raise SystemExit('R54 UI does not forward chosen map coordinates')

vehicle_patch = Path(__file__).with_name('fix_vehicle_bridge_runtime.py')
if not vehicle_patch.is_file():
    raise SystemExit(f'missing NAVE bridge patch: {vehicle_patch}')
subprocess.run([sys.executable, str(vehicle_patch), str(root)], check=True)

print('R54 MAP ANCHOR STATE PATCH PASS startLocationId=canonical startMapPoint=canonical vehicleBridge=canonical')
