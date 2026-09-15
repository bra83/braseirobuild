import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { TERRAIN_MPH, exploreHexDays, travelRateMph } from '../runtime/core/wwn-travel-core.js';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../runtime');
const html=fs.readFileSync(path.join(root,'scr014.html'),'utf8');
const css=fs.readFileSync(path.join(root,'scr014.css'),'utf8');
const js=fs.readFileSync(path.join(root,'scr014.js'),'utf8');

const token=path.join(root,'assets/atlas_repo/group_token_adventurers.png');
const terrain=path.join(root,'assets/atlas_repo/terrain_current_ruins.jpg');

test('Atlas real has a terrain panel below the map with current canonical fields',()=>{
  for(const id of ['terrainInfo','terrainTitle','terrainDescription','terrainType','terrainMove','terrainState','placeName','openParty']) assert.match(html,new RegExp(`id="${id}"`));
  assert.match(html,/RUÍNAS ROCHOSAS/);
  assert.match(html,/COLINAS \/ RUÍNAS/);
  assert.match(html,/DESCOBERTO/);
});

test('Terrain panel does not invent legacy visibility or risk mechanics',()=>{
  const panel=html.match(/<section class=\"terrain-info\"[\s\S]*?<\/section>/)?.[0]??'';
  assert.doesNotMatch(panel,/VISIBILIDADE/i);
  assert.doesNotMatch(panel,/RISCO|PERIGO|MODERADO/i);
  assert.doesNotMatch(js,/encounterDie|visibility|danger/i);
});

test('Rugged hills movement comes from current Rules Engine, not UI hardcode',()=>{
  assert.equal(TERRAIN_MPH.DENSE_FOREST_OR_RUGGED_HILLS,1.5);
  assert.equal(travelRateMph({terrain:'DENSE_FOREST_OR_RUGGED_HILLS'}),1.5);
  assert.match(js,/import \{ TERRAIN_MPH \} from '.\/core\/wwn-travel-core\.js'/);
  assert.match(js,/TERRAIN_MPH\.DENSE_FOREST_OR_RUGGED_HILLS/);
});

test('WWN wilderness projection retains standard six-mile hex scale',()=>{
  assert.equal(exploreHexDays({concealmentMultiplier:1}).standardHexMiles,6);
  assert.match(html,/1 HEX = 6 MILHAS/);
});

test('Current occupied hex is known and actually receives terrain art',()=>{
  assert.match(js,/currentHexRecord\(q,r,current\)/);
  assert.match(js,/known:true,terrain:'COLINAS \/ RUÍNAS'/);
  assert.match(js,/--terrain-image/);
  assert.match(js,/terrain_current_ruins\.jpg/);
  assert.ok(fs.statSync(terrain).size>10000);
});

test('Unknown hex selection remains hidden and never borrows current terrain art',()=>{
  assert.match(js,/\{known:false\}/);
  assert.match(js,/NÃO EXPLORADO/);
  assert.match(js,/Selecioná-lo não revela terreno nem conteúdo oculto/);
  assert.match(js,/data:image\/gif;base64/);
  assert.match(css,/\.hex\.unknown::after\{content:'\?'/);
});

test('User attachment crop is the real group token and opens the existing party UI',()=>{
  assert.ok(fs.statSync(token).size>100000);
  assert.match(js,/group_token_adventurers\.png/);
  assert.match(js,/openParty\(group\.groupId\)/);
  assert.match(css,/\.group-token img\{display:block;width:48px;height:48px/);
});

test('Hex borders are true six-sided fills instead of clipped rectangular borders',()=>{
  assert.match(css,/\.hex::before\{content:'';position:absolute;inset:2px;clip-path:polygon\(50% 0,100% 25%,100% 75%,50% 100%,0 75%,0 25%\)/);
  assert.match(css,/\.hex\{[^}]*border:0/);
});

test('Terrain title and explanatory text share one aligned copy block',()=>{
  assert.match(html,/<div class="terrain-copy">[\s\S]*id="terrainTitle"[\s\S]*id="terrainDescription"[\s\S]*<\/div>/);
  assert.match(css,/\.terrain-copy\{align-self:center/);
});
