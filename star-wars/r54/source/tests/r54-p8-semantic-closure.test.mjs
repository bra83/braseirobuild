import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {equipmentCatalog} from '../app/src/main/assets/vtt/js/expansion/equipmentCatalog.mjs';
import {EOTE_ACTIVE_CATALOG_VEHICLE_KEYS,STARTER_VEHICLE_PROFILES} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {GALACTIC_WORLD_CATALOG} from '../app/src/main/assets/vtt/js/expansion/galacticWorldCatalogWave6.mjs';
import {ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
const root=process.cwd();
const idx=JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/vtt/image-library/local/r54/p6-visual-index.json'),'utf8'));
const runtime=fs.readFileSync(path.join(root,'app/src/main/assets/vtt/r54-device-p6.js'),'utf8');
const sandbox=fs.readFileSync(path.join(root,'app/src/main/assets/vtt/js/world/sandbox.mjs'),'utf8');
const norm=v=>String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();
const exists=rel=>fs.existsSync(path.join(root,'app/src/main/assets/vtt',rel));

test('P8 exact visual coverage closes full active equipment catalog',()=>{
 const c=equipmentCatalog();
 for(const x of [...c.weapons,...c.armor,...c.generalGear]){
   const r=idx.exactByName[norm(x.name)];
   assert.ok(r,`missing exact visual: ${x.name}`);
   assert.ok(exists(r.path),`missing file for ${x.name}: ${r.path}`);
 }
});

test('P8 exact visual coverage closes active vehicle catalog',()=>{
 for(const id of EOTE_ACTIVE_CATALOG_VEHICLE_KEYS){
   const v=STARTER_VEHICLE_PROFILES[id];
   assert.ok(v,`vehicle profile missing ${id}`);
   const r=idx.exactByName[norm(v.name)] || idx.exactByName[norm(v.shortName)];
   assert.ok(r,`missing exact vehicle visual: ${v.name}`);
   assert.ok(exists(r.path),`missing vehicle file: ${v.name}`);
 }
});

test('P8 known adversaries have deterministic exact identity visuals',()=>{
 for(const [id,v] of Object.entries(ADVERSARY_PROFILES)){
   const r=idx.exactByName[norm(v.name)];
   assert.ok(r,`missing NPC visual: ${v.name}`);
   assert.ok(exists(r.path),`missing NPC file: ${v.name}`);
 }
});

test('P12 all 82 catalog worlds have honest playable location visuals',()=>{
 assert.equal(GALACTIC_WORLD_CATALOG.length,82);
 for(const w of GALACTIC_WORLD_CATALOG){
   const r=idx.locations[norm(w.name)];
   assert.ok(r,`missing location binding: ${w.name}`);
   assert.ok(['local_map','generated_play_map'].includes(r.strategy),`invalid location visual: ${w.name}`); if(r.provenance==='generated_from_location_profile') assert.equal(r.strategy,'generated_play_map',`generated visual mislabeled: ${w.name}`);
   assert.ok(exists(r.path),`missing map file: ${w.name}`);
 }
});

test('P8 no fuzzy visual resolver and no rejected technical RAG copy',()=>{
 assert.doesNotMatch(runtime,/bestScore|score\s*>|fuzzy/i);
 assert.doesNotMatch(sandbox,/detalhes factuais adicionais devem ser estabelecidos pelo Mestre com apoio do RAG/i);
});
