import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const vtt=path.join(root,'app/src/main/assets/vtt');
const norm=v=>String(v??'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,' ').trim();

test('P7: catálogo pessoal core inteiro tem binding exato e arquivo local',async()=>{
 const m=await import('../app/src/main/assets/vtt/js/expansion/equipmentCatalog.mjs');
 const c=m.equipmentCatalog(); const idx=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/local/r54/p6-visual-index.json'),'utf8'));
 const all=[...c.weapons.slice(0,31),...c.armor,...c.generalGear];
 for(const item of all){const ex=idx.exactByName[norm(item.name)];assert.ok(ex,`sem binding: ${item.name}`);assert.ok(fs.existsSync(path.join(vtt,ex.path)),`${item.name}: ${ex.path}`);}
});

test('P12: 82 locais têm visual de localização honesto e zero path quebrado',()=>{
 const idx=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/local/r54/p6-visual-index.json'),'utf8'));
 assert.equal(Object.keys(idx.locations).length,82); for(const [k,x] of Object.entries(idx.locations)){assert.ok(x.path,k);assert.ok(['local_map','generated_play_map','galactic_locator_map'].includes(x.strategy),`${k}: ${x.strategy}`);assert.ok(fs.existsSync(path.join(vtt,x.path)),`${k}: ${x.path}`);if(x.provenance==='generated_from_location_profile')assert.equal(x.strategy,'generated_play_map',k);}
});

test('P7: seed não fixa Voss e remove texto técnico RAG',()=>{
 const s=fs.readFileSync(path.join(vtt,'js/world/sandbox.mjs'),'utf8');
 assert.doesNotMatch(s,/Nera Voss|Kerr Voss/); assert.doesNotMatch(s,/detalhes factuais adicionais devem ser estabelecidos/i); assert.match(s,/seedEntropy/); assert.match(s,/rotated\(NPC_NAMES/); assert.match(s,/rotated\(FACTION_TEMPLATES/);
});

test('P7: renderer visual não usa scoring fuzzy',()=>{
 const s=fs.readFileSync(path.join(vtt,'r54-device-p6.js'),'utf8'); assert.doesNotMatch(s,/inter\*20|score|bs=-1/); assert.match(s,/labels\.some\(function\(l\)\{return l===key;\}\)/);
});
