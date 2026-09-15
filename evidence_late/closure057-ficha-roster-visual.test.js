import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const root=new URL('../app/src/main/assets/www/',import.meta.url);
const js=fs.readFileSync(new URL('ficha.js',root),'utf8');
const css=fs.readFileSync(new URL('ficha.css',root),'utf8');
const geom=JSON.parse(fs.readFileSync(new URL('data/ficha.iria.geometry.json',root),'utf8'));

test('Closure 057 preserves the approved FICHA geometry contract at 152 elements',()=>{
  assert.equal(geom.length,152);
  assert.equal(new Set(geom.map(x=>x.id)).size,152);
});

test('FICHA roster drawer is an overlay and closed state is non-rendering',()=>{
  assert.match(css,/\.character-drawer\[hidden\]\{display:none\}/);
  assert.match(css,/z-index:980/);
  assert.equal(js.includes("geom(drawer"),false);
});

test('CAMADAS opens roster management and GERAR HERÓI delegates through runtime adapter',()=>{
  assert.match(js,/btn_header_camadas/);
  assert.match(js,/openCharacterDrawer\(\)/);
  assert.match(js,/GERAR HERÓI/);
  assert.match(js,/BRASEIRO_CHARACTER_UI_ADAPTER/);
  assert.match(js,/characterUi\.generateRandom/);
  assert.match(js,/characterUi\.activate/);
});

test('FICHA projects generated characters without UI-side random rules',()=>{
  assert.match(js,/projectCharacterForSheet/);
  assert.equal(js.includes('Math.random'),false);
  assert.equal(js.includes('localStorage'),false);
});
