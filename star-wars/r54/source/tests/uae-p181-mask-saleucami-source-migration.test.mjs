/** P181: SWE13 PDF chapter and named-character crosscheck; save migration and game mechanics. */
import test from 'node:test';
import assert from 'node:assert/strict';
import { MASK_OF_THE_PIRATE_QUEEN as book, migrateMaskOfThePirateQueenWorld } from '../app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs';
import { getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { validatePublishedAdventureGraph } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
const adapter = getPublishedAdventureAdapter(book.id);
const copy = structuredClone;
const advance = (w,id) => adapter.advance(w,id).world;
const route = [
 'accept-venlana','meet-at-paradise','follow-saleucami-leads','question-graf',
 'visit-krezo-after-graf','trace-blackwind','enter-the-vault','confront-decoy',
 'deliver-revah','travel-ord-mantell','meet-jander','visit-vandin',
 'fix-the-fight','collect-vandin-favor-fight','enter-queen-palace','face-ryale',
 'pursue-renegades-blood','approach-flagship','board-the-ship','reach-queen-bridge','capture-noira','side-porel'
];
test('P181: official chapter sequence Saleucami -> Worlport -> Renegade’s Blood',()=>{
 assert.equal(book.startNodeId,'saleucami-contract-offer');
 assert.equal(book.schemaVersion,3);
 assert.ok(Object.keys(book.nodes).length >= 23);
 assert.deepEqual([...new Set(Object.values(book.nodes).map(x=>x.episode))],[1,2,3]);
 for(const n of Object.values(book.nodes)) assert.ok(n.sourceSection?.includes('Episode'),n.title);
 assert.ok(validatePublishedAdventureGraph(book).structurallyReachable);
 assert.ok(validateAdventureSemantics(publishedAdventureRuntimeModule(book)).triggers>0);
 let s=adapter.start({});
 assert.equal(s.node.title,'Contrato da Irmandade Oculta');
 assert.ok(s.world.npcs['mpq.npc.venlana_sipal']);
 assert.ok(s.world.npcs['mpq.npc.revah']);
 assert.ok(s.world.npcs['mpq.npc.noira']);
 assert.ok(s.world.locations['mpq.loc.taleucema']);
 for(const choice of route) {
   if(choice==='capture-noira') s={...s,world:adapter.recordEncounterOutcome(s.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world};
   s=adapter.advance(s.world,choice);
 }
 assert.equal(s.completed,true);
 assert.equal(s.progress.flags.decoyExposed,true);
 assert.equal(s.progress.flags.trueQueenIdentified,true);
 assert.equal(s.progress.flags.resolutionMode,'porel');
 assert.equal(s.progress.visitedNodeIds.length,route.length+1);
 const episodes=s.progress.visitedNodeIds.map(id=>book.nodes[id].episode);
 assert.deepEqual([...new Set(episodes)],[1,2,3]);
 assert.equal(s.world.publishedAdventureRuntime.status,'COMPLETED');
 const archived=adapter.archive(s.world);
 assert.equal(adapter.status(archived).archived,true);
});
test('P181: two Vandin alternatives and the Noira alliance are playable',()=>{
 let s=adapter.start({});
 for(const c of ['accept-venlana','meet-at-paradise','follow-saleucami-leads','question-krezo','trace-blackwind','enter-the-vault','confront-decoy','deliver-revah','travel-ord-mantell','meet-jander','visit-vandin','reach-rusty','collect-vandin-favor-reach','enter-queen-palace','face-ryale','pursue-renegades-blood','approach-flagship','board-the-ship','reach-queen-bridge','join-sorority']) {
  if(c==='join-sorority') s={...s,world:adapter.recordEncounterOutcome(s.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world};
  s=adapter.advance(s.world,c);
 }
 assert.equal(s.completed,true);
 assert.equal(s.progress.flags.vandinFavor,'rusty-reach');
 assert.equal(s.progress.flags.queenAlliance,true);
 assert.equal(s.progress.flags.queenCaptured,undefined);
});
test('P181: actual NPC encounter only in authored scene, replay guarded, outcomes persistent',()=>{
 let s=adapter.start({});
 assert.throws(()=>adapter.encounterSpec(s.world,'pirate-queen-duel'),/indisponível/);
 for(const c of ['accept-venlana','meet-at-paradise','follow-saleucami-leads','question-krezo','trace-blackwind','enter-the-vault']) s=adapter.advance(s.world,c);
 assert.equal(s.progress.currentNodeId,'veiled-stronghold');
 const spec=adapter.encounterSpec(s.world);
 assert.equal(spec.id,'vault-sorority-guards');
 assert.equal(spec.adversaries[0].profileKey,'veiled_sorority_corsair');
 assert.throws(()=>adapter.markEncounterActive(s.world,'pirate-queen-duel'),/não pode/);
 const active=adapter.markEncounterActive(s.world,spec.id);
 assert.equal(active.publishedAdventure.activeEncounterId,spec.id);
 assert.throws(()=>adapter.recordEncounterOutcome(active,{encounterId:spec.id,outcome:'forged'}),/inválido/);
 const res=adapter.recordEncounterOutcome(active,{encounterId:spec.id,outcome:'adversaries_defeated'});
 assert.equal(res.world.publishedAdventure.encounterHistory.length,1);
 assert.equal(res.world.publishedAdventure.flags[`encounter:${spec.id}`],'adversaries_defeated');
 assert.throws(()=>adapter.recordEncounterOutcome(res.world,{encounterId:spec.id,outcome:'adversaries_defeated'}),/incompatível/);
 let json=JSON.stringify(res.world);
 for(let i=0;i<3;i++) {
   const restored=JSON.parse(json);
   assert.equal(restored.publishedAdventure.encounterHistory.length,1);
   assert.equal(adapter.status(restored).active,true);
   json=JSON.stringify(restored);
 }
});
test('P181: check failure raises persistent alert, success never fabricates a win',()=>{
 let s=adapter.start({});
 for(const c of ['accept-venlana','meet-at-paradise','follow-saleucami-leads','question-krezo','trace-blackwind']) s=adapter.advance(s.world,c);
 const before=copy(s.world);
 assert.throws(()=>adapter.adjudicateCheck(s.world,{checkIndex:0,result:{}}),/sucesso ou falha/);
 assert.deepEqual(s.world,before);
 const failed=adapter.adjudicateCheck(s.world,{checkIndex:0,result:{success:false,netAdvantage:-2}});
 assert.equal(failed.world.publishedAdventure.clocks.pirateAlert.value,1);
 assert.equal(failed.world.publishedAdventure.checkHistory.at(-1).success,false);
 assert.equal(failed.world.publishedAdventure.flags['check:scrap-zone-investigation:0'],false);
 const passed=adapter.adjudicateCheck(failed.world,{checkIndex:0,result:{success:true}});
 assert.equal(passed.world.publishedAdventure.clocks.pirateAlert.value,1);
 assert.equal(passed.world.publishedAdventure.flags['check:scrap-zone-investigation:0'],true);
});
test('P181: v1 Ord Mantell start migrates to Saleucami without changing event receipts',()=>{
 let s=adapter.start({});
 const legacy=copy(s.world);
 legacy.publishedAdventure.schemaVersion=1;
 legacy.publishedAdventure.currentNodeId='ord-mantell-arrival';
 legacy.publishedAdventure.visitedNodeIds=['ord-mantell-arrival'];
 legacy.publishedAdventureRuntime.currentNodeId='ord-mantell-arrival';
 legacy.publishedAdventureRuntime.visitedNodeIds=['ord-mantell-arrival'];
 const events=copy(legacy.publishedAdventureRuntime.canonicalEvents);
 const migrated=migrateMaskOfThePirateQueenWorld(legacy);
 assert.equal(migrated.publishedAdventure.currentNodeId,book.startNodeId);
 assert.equal(migrated.publishedAdventureRuntime.currentNodeId,book.startNodeId);
 assert.deepEqual(migrated.publishedAdventureRuntime.canonicalEvents,events);
 assert.equal(legacy.publishedAdventure.currentNodeId,'ord-mantell-arrival');
 assert.deepEqual(migrateMaskOfThePirateQueenWorld(migrated),migrated);
 assert.equal(adapter.status(legacy).node.id,book.startNodeId);
 s=adapter.advance(legacy,'accept-venlana');
 assert.equal(s.world.publishedAdventure.schemaVersion,2);
 assert.equal(s.world.publishedAdventure.migration.previousNodeId,'ord-mantell-arrival');
 assert.equal(s.world.publishedAdventureRuntime.currentNodeId,'taleucema-arrival');
 const persisted=JSON.stringify(s.world);
 for(let i=0;i<3;i++) {
   const reopened=JSON.parse(persisted);
   assert.equal(adapter.status(reopened).node.id,'taleucema-arrival');
   assert.equal(reopened.publishedAdventure.migration.previousNodeId,'ord-mantell-arrival');
   assert.deepEqual(AdventureRuntime.restore(publishedAdventureRuntimeModule(book),reopened.publishedAdventureRuntime).getState(),reopened.publishedAdventureRuntime);
 }
});

test('P181: controller credits only the Consortium contract outcome, not an alliance', async()=>{
 const {GameController}=await import('../app/src/main/assets/vtt/js/core/controller.mjs');
 const {createCampaign}=await import('../app/src/main/assets/vtt/js/core/state.mjs');
 class MemoryRepository { constructor(){this.state=createCampaign('P181');} load(){return copy(this.state);} save(s){this.state=copy(s);} }
 for(const alliance of [false,true]) {
   const repo=new MemoryRepository(),game=new GameController(repo);
   game.loadStarterCharacter('cael');
   const before=game.state.player.character.credits;
   game.startPublishedAdventureSession(book.id,{rng:()=>.5});
   const path=[...route];
   if(alliance) path.splice(path.length-2,2,'join-sorority');
   let result;
   for(const choice of path) {
     if(choice==='capture-noira'||choice==='join-sorority') {
       const resolved=adapter.recordEncounterOutcome(game.state.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'});
       game.commit('WORLD_UPDATED',{world:resolved.world});
     }
     result=game.advancePublishedAdventure(choice);
   }
   assert.equal(result.adventure.completed,true);
   assert.equal(result.rewards.credits,alliance?0:10500);
   assert.equal(game.state.player.character.credits,before+500+(alliance?0:10500));
   assert.equal(game.state.world.publishedAdventureRewards[book.id].credits,alliance?0:10500);
   const recovered=new GameController(repo);
   assert.equal(recovered.state.world.publishedAdventureRewards[book.id].credits,alliance?0:10500);
 }
});
