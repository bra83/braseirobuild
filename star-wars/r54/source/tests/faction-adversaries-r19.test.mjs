import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { buildMasterContext } from '../app/src/main/assets/vtt/js/master/context.mjs';
class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
function make(key){ const g=new GameController(new LocalStorageRepository(new MemoryStorage(),key)); g.loadStarterCharacter('cael'); return g; }

test('adversário não persistente pode carregar facção explícita validada',()=>{
  const g=make('r19-fac-adv'); const f=g.addWorldFaction({name:'Guarnição'}); g.prepareMasterContext('Patrulha aparece.'); g.applyMasterProposal({encounterDirective:{adversaries:[{profileKey:'stormtrooper',count:2,factionId:f.id}]}}); g.beginPendingMasterEncounter({rng:()=>0});
  assert.equal(g.state.combat.adversaries[0].factionId,f.id); const c=buildMasterContext(g.state,{intentText:'Quem está lutando?'}); assert.equal(c.combat.adversaries[0].factionId,f.id);
});

test('PNPC persistente herda facção e tentativa de troca silenciosa é rejeitada',()=>{
  const g=make('r19-pnpc-fac'); const a=g.addWorldFaction({name:'Aurora'}), b=g.addWorldFaction({name:'Império'}); const n=g.addWorldNpc({name:'Kara',factionId:a.id}); g.prepareMasterContext('Kara aparece.');
  assert.throws(()=>g.applyMasterProposal({encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:n.id,factionId:b.id}]}}),/facção do PNPC persistente/i);
  g.prepareMasterContext('Kara aparece.'); g.applyMasterProposal({encounterDirective:{adversaries:[{profileKey:'imperial_officer',worldNpcId:n.id}]}}); g.beginPendingMasterEncounter({rng:()=>0}); assert.equal(g.state.combat.adversaries[0].factionId,a.id); assert.equal(g.state.combat.adversaries[0].worldNpcId,n.id);
});

test('consequência de facção pode inferir afiliação dos adversários do encontro',()=>{
  const g=make('r19-hook-fac'); const loc=g.addWorldLocation({name:'Hangar'}); const f=g.addWorldFaction({name:'Império'}); g.addWorldFactionPlan(f.id,{title:'Tomar hangar',steps:[{text:'Entrar',locationId:loc.id}]}); const plan=g.state.world.factions[f.id].plans[0];
  g.scheduleWorldEvent({type:'advance_faction_plan',dueTick:1,payload:{factionId:f.id,planId:plan.id}}); g.advanceWorldTime({ticks:1}); g.prepareMasterContext('Entro no hangar.'); g.applyMasterProposal({sceneLocationId:loc.id}); const hook=g.causalEncounterCandidates()[0];
  g.prepareMasterContext('Avanço.'); g.applyMasterProposal({encounterDirective:{sourceHookId:hook.hookId,adversaries:[{profileKey:'stormtrooper',count:1}]}}); g.beginPendingMasterEncounter({rng:()=>0}); assert.equal(g.state.combat.adversaries[0].factionId,f.id);
});
