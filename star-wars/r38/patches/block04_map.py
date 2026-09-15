#!/usr/bin/env python3
from pathlib import Path
import sys
root=Path(sys.argv[1]);vtt=root/'app/src/main/assets/vtt'
def replace(p,a,b):
 s=p.read_text();n=s.count(a)
 if n!=1:raise SystemExit(f'{p}: expected one map anchor, got {n}: {a[:90]}')
 p.write_text(s.replace(a,b))
# Persist the user-selected map reference through the canonical world snapshot.
for p in [vtt/'js/core/controller.mjs',vtt/'js/core/uiBridge.bundle.js']:
 replace(p,"initializeLivingSandbox({ startingPoint = 'Porto de Fronteira', regionLabel = 'Região de Fronteira' } = {}) {", "initializeLivingSandbox({ startingPoint = 'Porto de Fronteira', regionLabel = 'Região de Fronteira', startMapPoint = null } = {}) {")
 replace(p,"const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });", """if (startMapPoint !== null && (!Number.isFinite(startMapPoint.x) || !Number.isFinite(startMapPoint.y) || startMapPoint.x < 0 || startMapPoint.x > 100 || startMapPoint.y < 0 || startMapPoint.y > 100)) throw new Error('Ponto do mapa inválido.');
        const result = sandbox_mjs_1.seedLivingRegion(this.state.world, { startingPoint, regionLabel });
        if (result.created && startMapPoint !== null) result.world.sandbox.startMapPoint = { x: startMapPoint.x, y: startMapPoint.y, mapId: 'local.map.galaxy', source: 'player_selected' };""")
p=vtt/'r46/final-runtime.js'
replace(p,"select.value=String(modeResult.data?.campaignMode||'EOTE');let pointIndex=0,mode='list';", "select.value=String(modeResult.data?.campaignMode||'EOTE');let pointIndex=0,mode='list',selectedStart={...START_POINTS[0]};")
replace(p,"selected.textContent=START_POINTS[pointIndex].name;caption.textContent=START_POINTS[pointIndex].name", "selectedStart={...START_POINTS[pointIndex]};selected.textContent=selectedStart.name;caption.textContent=selectedStart.name;host.querySelector('#sandbox-custom-start')&&(host.querySelector('#sandbox-custom-start').value=selectedStart.name)")
replace(p,"button.onclick=()=>{const point=START_POINTS[pointIndex].name;", """window.BraseiroMapUI?.setup(host,selectedStart,(point)=>{selectedStart=point;selected.textContent=point.name;caption.textContent=point.name;host.querySelectorAll('[data-point]').forEach(x=>x.classList.remove('is-selected'))});
 button.onclick=()=>{const point=selectedStart.name.trim();if(!point){status.textContent='Informe o nome do ponto inicial.';return;}""")
replace(p,"{startingPoint:point,regionLabel:`Região de ${point}`}", "{startingPoint:point,regionLabel:`Região de ${point}`,startMapPoint:{x:selectedStart.x,y:selectedStart.y}}")
# Load before the existing runtime so setup can register its callback.
p=vtt/'r46-shell.html';s=p.read_text();needle='<script src="r46/final-runtime.js"'
if needle not in s:raise SystemExit('runtime script tag not found')
s=s.replace(needle,'<script src="r46/map-interaction.js"></script>\n'+needle);p.write_text(s)
(vtt/'r46/map-interaction.js').write_text(Path(__file__).with_name('map-interaction.js').read_text())
print('MAP_INTERACTION_PATCH_APPLIED')
