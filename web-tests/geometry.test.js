import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { validateGeometry } from '../app/src/main/assets/www/core/visual-geometry.js';
import { AssetResolver } from '../app/src/main/assets/www/core/asset-resolver.js';

const geometry=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/scr015.geometry.json',import.meta.url)));
const derived=JSON.parse(fs.readFileSync(new URL('../app/src/main/assets/www/data/scr015.derived-assets.json',import.meta.url)));

test('SCR-015 geometry validates exact 415x915 and all 27 layers',()=>{
  const result=validateGeometry(geometry);
  assert.equal(result.ok,true,result.errors.join('; '));
  assert.equal(result.layerCount,27);
});

test('only EXPLORAR is active in geometry fixture',()=>{
  assert.equal(geometry.deterministic_state.bottom_nav_active,'explore');
  assert.deepEqual(geometry.deterministic_state.bottom_nav_inactive,['home','sessions','favorites','settings']);
});

test('derived icon resolver rejects unknown assets and resolves approved derived assets',()=>{
  const resolver=new AssetResolver(derived);
  assert.match(resolver.resolve('top_search_icon').path,/top_search_icon/);
  assert.throws(()=>resolver.resolve('missing'));
});
