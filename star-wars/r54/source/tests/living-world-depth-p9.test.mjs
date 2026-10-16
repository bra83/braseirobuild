import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorld, createLocation, upsertLocation, createFaction, upsertFaction, createNpc, upsertNpc } from '../app/src/main/assets/vtt/js/world/world.mjs';
import { connectLocations, advanceLivingWorld, normalizeSandboxWorld } from '../app/src/main/assets/vtt/js/world/sandbox.mjs';
import { livingWorldSnapshot, livingWorldSummary } from '../app/src/main/assets/vtt/js/world/livingWorldDepth.mjs';
function base(){let w=createWorld();const a=createLocation({name:'Porto',knownToPlayer:true,tags:['porto']});const b=createLocation({name:'Deserto',knownToPlayer:true,tags:['deserto']});w=upsertLocation(w,a);w=upsertLocation(w,b);w=connectLocations(w,{fromLocationId:a.id,toLocationId:b.id,ticks:1});const f=createFaction({name:'Sindicato',knownToPlayer:true});w=upsertFaction(w,f);for(let i=0;i<3;i++)w=upsertNpc(w,createNpc({name:'NPC '+i,locationId:a.id,factionId:f.id,knownToPlayer:true,tags:i===0?['comerciante']:[]}));w=normalizeSandboxWorld(w);w.sandbox.initialized=true;w.sandbox.startLocationId=a.id;return {w,a,b,f};}
test('mundo vivo mantém memória, riqueza, atividade, clima e tempo persistentes',()=>{let {w,a}=base();let r=advanceLivingWorld(w,{ticks:8});w=r.world;const s=livingWorldSummary(w),v=livingWorldSnapshot(w,{locationId:a.id});assert.equal(s.npcCount,3);assert.ok(s.time.day>=3);assert.ok(['madrugada','manhã','tarde','noite'].includes(s.time.period));assert.ok(v.weather);assert.ok(v.market);for(const n of Object.values(w.livingDepth.npcs)){assert.ok(n.wealthTier);assert.ok(n.currentActivity);assert.ok(Array.isArray(n.memory));assert.ok(['alive','dead','missing'].includes(n.lifeState));}});
test('simulação avança estado econômico sem alterar créditos do jogador',()=>{let {w}=base();const before=JSON.stringify(w.player||null);w=advanceLivingWorld(w,{ticks:40}).world;assert.equal(JSON.stringify(w.player||null),before);assert.equal(w.livingDepth.lastTick,w.clock.tick);assert.ok(Object.keys(w.livingDepth.locations).length>=2);});

import { readFileSync } from 'node:fs';
import { join } from 'node:path';
test('P9 porta memória social, compromissos e diplomacia persistentes do desenho DnD',()=>{
  const src=readFileSync(join(process.cwd(),'app/src/main/assets/vtt/js/world/livingWorldDepth.mjs'),'utf8');
  assert.match(src,/relationshipDelta/); assert.match(src,/commitments/); assert.match(src,/consequencePending/);
  assert.match(src,/applyFactionDiplomacy/); assert.match(src,/treaties/); assert.match(src,/wars/);
});
