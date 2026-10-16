import test from 'node:test';
import assert from 'node:assert/strict';
import {startBeyondTheRim,advanceBeyondTheRim,beyondTheRimStatus} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';

test('P64 completion reports completed and persists aftermath without spoilers before completion',()=>{
 let {world}=startBeyondTheRim({clock:{tick:42},locations:{},npcs:{},factions:{}});
 assert.equal(beyondTheRimStatus(world).knownConsequences?.length??0,0);
 world.publishedAdventure.currentNodeId='flying-junk';world.publishedAdventure.visitedNodeIds.push('flying-junk');world.publishedAdventure.flags.finalEscapeSucceeded=true;world.publishedAdventure.flags.harsolTrust=2;world.publishedAdventure.flags.survivorTrust=2;world.publishedAdventure.flags.yiyarEscalated=true;world.publishedAdventure.systems.imperialExposure=4;
 const r=advanceBeyondTheRim(world,'complete-beyond-rim');
 assert.equal(r.completed,true);assert.equal(r.world.publishedAdventure.status,'completed');assert.equal(r.world.publishedAdventure.consequencesApplied,true);
 assert.equal(r.world.publishedAdventureConsequences['beyond-the-rim'].livingWorldApplied,true);
 assert.equal(r.world.publishedAdventureConsequences['beyond-the-rim'].imperialExposure,4);
 assert.ok(r.world.publishedAdventureWorldPlans['beyond-the-rim'].some(x=>x.id==='isb-followup'));
 assert.ok(r.world.publishedAdventureWorldPlans['beyond-the-rim'].some(x=>x.id==='yiyar-retaliation'));
 assert.equal(r.world.npcs['btr.npc.rel_harsol'].relation,'friendly');
 assert.equal(beyondTheRimStatus(r.world).knownConsequences.length,1);
});

test('P64 aftermath application is idempotent',()=>{
 let {world}=startBeyondTheRim({clock:{tick:7},locations:{},npcs:{},factions:{}});world.publishedAdventure.currentNodeId='flying-junk';world.publishedAdventure.flags.finalEscapeSucceeded=true;
 const once=advanceBeyondTheRim(world,'complete-beyond-rim').world;const facts=[...(once.factions['btr.fac.isotech'].facts||[])];
 assert.throws(()=>advanceBeyondTheRim(once,'complete-beyond-rim'),/não está ativa/);assert.deepEqual(once.factions['btr.fac.isotech'].facts,facts);
});
