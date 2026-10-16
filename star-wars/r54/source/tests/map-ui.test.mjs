import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const html=fs.readFileSync(path.join(root,'app/src/main/assets/vtt/index.html'),'utf8');
const app=fs.readFileSync(path.join(root,'app/src/main/assets/vtt/js/ui/app.mjs'),'utf8');

test('Mapa Galáctico é destino próprio e usa asset local offline',()=>{
  assert.match(html,/data-view="mapView">Mapa</);
  assert.match(html,/id="galaxyMapViewport"/);
  assert.match(html,/assets\/maps\/galaxy_map\.jpg/);
  const stat=fs.statSync(path.join(root,'app/src/main/assets/vtt/assets/maps/galaxy_map.jpg'));
  assert.ok(stat.size>6_000_000);
});

test('mapa permite zoom e arraste mas viagem vira intenção do jogador',()=>{
  assert.match(app,/pointerdown/); assert.match(app,/zoomGalaxyMap/);
  assert.match(app,/game\.submitIntent\(`Quero viajar para/);
  assert.match(html,/não move o grupo/i);
  assert.match(html,/não teleporta/i);
});
