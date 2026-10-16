import test from 'node:test'; import assert from 'node:assert/strict'; import fs from 'node:fs';
const src=fs.readFileSync('app/src/main/assets/vtt/r54-device-p6.js','utf8');
test('P13 mapas disponíveis abrem visualizador em tela cheia',()=>{assert.match(src,/function openMapViewer\(/);assert.match(src,/data-map-src/);assert.match(src,/r54-p6-map-viewer/);assert.match(src,/Toque para ampliar/);});
test('P13 visualizador preserva proveniência mapa dedicado esquema e galáctico',()=>{assert.match(src,/kind==='local_map'/);assert.match(src,/kind==='generated_play_map'/);assert.match(src,/Localização galáctica/);});
test('P13 mapa é acessível por teclado',()=>{assert.match(src,/tabindex="0" role="button"/);assert.match(src,/e\.key==='Enter'\|\|e\.key===' '/);});
