import test from 'node:test';
import assert from 'node:assert/strict';
import { startUnderBlackSun, advanceUnderBlackSun, underBlackSunStatus } from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
import { createWorld } from '../app/src/main/assets/vtt/js/world/world.mjs';

const step=(w,id)=>advanceUnderBlackSun(w,id).world;
const reopen=w=>JSON.parse(JSON.stringify(w));

test('P75: rota social Zelcomm mantém objetivo de investigação em PT-BR após reabrir save',()=>{
 let w=startUnderBlackSun(createWorld()).world;
 for(const id of ['escape-clean','begin-investigation','go-zelcomm','zelcomm-social']) w=step(w,id);
 const s=underBlackSunStatus(reopen(w));
 assert.equal(s.progress.currentNodeId,'zelcomm-frontdesk');
 assert.match(s.tracker.objective,/Reunir pistas sobre Kaa’to/);
 assert.doesNotMatch(s.tracker.objective,/perseguição|dados roubados/i);
 assert.match(s.node.title,/Recepção da Zelcomm/);
});

test('P75: reabertura serializada preserva escolha e não expõe destinos internos nas escolhas públicas',()=>{
 let w=startUnderBlackSun(createWorld()).world;
 for(const id of ['escape-clean','begin-investigation','go-spyder','spyder-table']) w=step(w,id);
 const s=underBlackSunStatus(reopen(w));
 assert.equal(s.progress.currentNodeId,'spyder-sabacc');
 assert.ok(s.node.choices.length>=2);
 for(const c of s.node.choices){ assert.deepEqual(Object.keys(c).sort(),['id','label']); assert.equal('to' in c,false); assert.equal('effects' in c,false); }
});
