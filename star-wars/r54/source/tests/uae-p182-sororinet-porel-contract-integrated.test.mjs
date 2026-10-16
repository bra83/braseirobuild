/** P182: source-checked SWE13 pp. 17-28, 90-95; not full-book certification. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {MASK_OF_THE_PIRATE_QUEEN as book} from '../app/src/main/assets/vtt/js/adventure/maskOfThePirateQueen.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
const a=getPublishedAdventureAdapter(book.id);
const copy=structuredClone;
const go=(w,choice)=>a.advance(w,choice).world;
const through=(w,choices)=>choices.reduce(go,w);
const toLeads=()=>through(a.start({}).world,['accept-venlana','meet-at-paradise','follow-saleucami-leads']);
const toShowdown=()=>through(toLeads(),[
 'question-krezo','trace-blackwind','enter-the-vault','confront-decoy','deliver-revah',
 'travel-ord-mantell','meet-jander','visit-vandin','fix-the-fight','collect-vandin-favor-fight',
 'enter-queen-palace','face-ryale','pursue-renegades-blood','approach-flagship',
 'board-the-ship','reach-queen-bridge'
]);
class Repo {constructor(){this.state=createCampaign('P182');}load(){return copy(this.state);}save(s){this.state=copy(s);}}
function makeController(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession(book.id,{rng:()=>.5});return {g,repo};}

test('P182: semantic preflight, source-specific nodes, Porel and Lathe persistent NPCs',()=>{
 assert.ok(Object.keys(book.nodes).length>=27);
 assert.ok(validateAdventureSemantics(publishedAdventureRuntimeModule(book)).triggers>0);
 const w=a.start({}).world;
 assert.equal(w.npcs['mpq.npc.porel_vakra'].name,'Porel Vakra');
 assert.equal(w.npcs['mpq.npc.lathe'].name,'Lathe');
 assert.equal(w.npcs['mpq.npc.graf'].name,'Graf Lind');
 assert.ok(w.locations['mpq.loc.sororinet_node']);
});
test('P182: third lead SororiNet is playable only after successful checks',()=>{
 let w=go(toLeads(),'investigate-sororinet');
 assert.equal(a.status(w).node.id,'sororinet-entry');
 assert.deepEqual(a.status(w).node.choices.map(c=>c.id),['withdraw-sororinet']);
 assert.throws(()=>go(w,'probe-sororinet'),/inválida/);
 w=a.adjudicateCheck(w,{checkIndex:0,result:{success:true}}).world;
 w=go(w,'probe-sororinet');
 assert.equal(a.status(w).node.id,'sororinet-secondary');
 assert.throws(()=>go(w,'breach-final-gateway'),/inválida/);
 w=a.adjudicateCheck(w,{checkIndex:0,result:{success:true}}).world;
 w=go(w,'breach-final-gateway');
 assert.equal(a.status(w).node.id,'sororinet-final');
 assert.throws(()=>go(w,'extract-blackwind'),/inválida/);
 w=a.adjudicateCheck(w,{checkIndex:0,result:{success:true}}).world;
 w=go(w,'extract-blackwind');
 assert.equal(a.status(w).node.id,'scrap-zone-investigation');
 assert.equal(w.publishedAdventure.flags.sororinetCoordinates,true);
 assert.ok(w.publishedAdventureRuntime.visitedNodeIds.includes('sororinet-final'));
 for(let i=0;i<3;i++){
   w=JSON.parse(JSON.stringify(w));
   assert.equal(a.status(w).node.id,'scrap-zone-investigation');
   assert.deepEqual(AdventureRuntime.restore(publishedAdventureRuntimeModule(book),w.publishedAdventureRuntime).getState(),w.publishedAdventureRuntime);
 }
});
test('P182: SororiNet failure records detection and alert but not fabricated coordinates',()=>{
 let w=go(toLeads(),'investigate-sororinet');
 const before=copy(w);
 assert.throws(()=>a.adjudicateCheck(w,{checkIndex:0,result:{success:'yes'}}),/sucesso ou falha/);
 assert.deepEqual(w,before);
 w=a.adjudicateCheck(w,{checkIndex:0,result:{success:false,netAdvantage:-1}}).world;
 assert.equal(w.publishedAdventure.clocks.pirateAlert.value,1);
 assert.equal(w.publishedAdventure.flags.sororinetIntrusionDetected,true);
 assert.equal(w.publishedAdventure.flags.sororinetCoordinates,undefined);
 assert.throws(()=>go(w,'probe-sororinet'),/inválida/);
 w=go(w,'withdraw-sororinet');
 assert.equal(a.status(w).node.id,'saleucami-leads');
 assert.ok(a.status(w).node.choices.some(c=>c.id==='question-krezo'));
});
test('P182: final capture and alliance cannot bypass encounter adjudication',()=>{
 const w=toShowdown();
 assert.throws(()=>go(w,'capture-noira'),/inválida/);
 assert.throws(()=>go(w,'join-sorority'),/inválida/);
 const active=a.markEncounterActive(w,'pirate-queen-duel');
 assert.throws(()=>go(active,'capture-noira'),/encontro ativo/);
 const res=a.recordEncounterOutcome(active,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world;
 assert.ok(a.status(res).node.choices.some(c=>c.id==='capture-noira'));
 assert.ok(a.status(res).node.choices.some(c=>c.id==='join-sorority'));
});
test('P182: Noira capture leads to Paradise confrontation, not automatic payout',()=>{
 let w=toShowdown();
 w=a.recordEncounterOutcome(w,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world;
 w=go(w,'capture-noira');
 assert.equal(a.status(w).node.id,'paradise-final-handover');
 assert.equal(w.publishedAdventure.status,'ACTIVE');
 assert.equal(w.publishedAdventureRewards?.[book.id],undefined);
 assert.ok(a.status(w).node.visibleNpcIds.includes('mpq.npc.porel_vakra'));
 w=go(w,'side-porel');
 assert.equal(w.publishedAdventure.status,'COMPLETED');
 assert.equal(w.publishedAdventure.flags.venlanaExposed,true);
 assert.equal(w.npcs['mpq.npc.venlana_sipal'].status,'captured');
 assert.equal(w.publishedAdventureConsequences[book.id].resolutionMode,'porel');
 assert.equal(w.publishedAdventureConsequences[book.id].editoriallyCertified,false);
 for(let i=0;i<3;i++){
   w=JSON.parse(JSON.stringify(w));
   assert.equal(a.status(w).completed,true);
   assert.equal(w.publishedAdventureConsequences[book.id].resolutionMode,'porel');
 }
});
test('P182: siding with Venlana records Consortium retaliation; no Porel bonus',()=>{
 let w=toShowdown();
 w=a.recordEncounterOutcome(w,{encounterId:'pirate-queen-duel',outcome:'adversaries_defeated'}).world;
 w=go(w,'capture-noira');w=go(w,'side-venlana');
 assert.equal(w.publishedAdventure.flags.porelBetrayed,true);
 assert.equal(w.npcs['mpq.npc.porel_vakra'].status,'fugitive');
 assert.equal(w.publishedAdventureConsequences[book.id].consortiumRetaliation,true);
});
test('P182: no reward without completed mission; no duplicate award',()=>{
 let w=toShowdown();
 assert.throws(()=>a.markRewardsApplied(w,{credits:11000}),/exige conclusão/);
 w=a.recordEncounterOutcome(w,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world;
 w=go(go(w,'capture-noira'),'side-porel');
 const marked=a.markRewardsApplied(w,{credits:10500});
 assert.equal(marked.publishedAdventureRewards[book.id].credits,10500);
 assert.throws(()=>a.markRewardsApplied(marked,{credits:10500}),/já aplicada/);
});
test('P182: retainer 500 once, Porel source bounty 10000+1000, no unsupported Obligation reduction',()=>{
 const {g,repo}=makeController();
 const initial=g.state.player.character.credits;
 g.advancePublishedAdventure('accept-venlana');
 assert.equal(g.state.player.character.credits,initial+500);
 assert.equal(g.state.world.publishedAdventure.flags.contractRetainerPaid,true);
 assert.throws(()=>g.advancePublishedAdventure('accept-venlana'),/inválida/);
 assert.equal(g.state.player.character.credits,initial+500);
 const path=['meet-at-paradise','follow-saleucami-leads','question-krezo','trace-blackwind','enter-the-vault','confront-decoy','deliver-revah','travel-ord-mantell','meet-jander','visit-vandin','fix-the-fight','collect-vandin-favor-fight','enter-queen-palace','face-ryale','pursue-renegades-blood','approach-flagship','board-the-ship','reach-queen-bridge'];
 for(const c of path) g.advancePublishedAdventure(c);
 g.commit('WORLD_UPDATED',{world:a.recordEncounterOutcome(g.state.world,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world});
 g.advancePublishedAdventure('capture-noira');
 assert.equal(g.state.player.character.credits,initial+500);
 const result=g.advancePublishedAdventure('side-porel');
 assert.equal(result.rewards.credits,10500);
 assert.equal(result.rewards.obligationReduced,0);
 assert.equal(g.state.player.character.credits,initial+11000);
 for(let i=0;i<3;i++){
   const restored=new GameController(repo);
   assert.equal(restored.state.player.character.credits,initial+11000);
   assert.equal(restored.state.world.publishedAdventureRewards[book.id].credits,10500);
 }
});
test('P182: Venlana doubles source bounty, alliance forfeits payment',()=>{
 for(const mode of ['venlana','alliance','independent']){
   let w=toShowdown();
   w=a.recordEncounterOutcome(w,{encounterId:'pirate-queen-duel',outcome:'negotiated'}).world;
   if(mode==='alliance') w=go(w,'join-sorority');
   else w=go(go(w,'capture-noira'),mode==='venlana'?'side-venlana':'reject-both');
   const policy=a.rewardPolicyForWorld(w);
   assert.equal(policy.credits,mode==='venlana'?20000:0);
   assert.equal(policy.obligationReduction,0);
 }
});
test('P182: bridge uses two groups of three source-checked guards and Noira stat block',async()=>{
 const {createAdversary}=await import('../app/src/main/assets/vtt/js/rules/adversaries.mjs');
 let w=toShowdown();
 const spec=a.encounterSpec(w,'pirate-queen-duel');
 assert.deepEqual(spec.adversaries.map(x=>x.profileKey),['mpq_bridge_guard','mpq_bridge_guard','mpq_noira']);
 assert.deepEqual(spec.adversaries.map(x=>x.count??1),[3,3,1]);
 const groups=spec.adversaries.map(x=>createAdversary(x.profileKey,{count:x.count||1,range:x.range}));
 assert.deepEqual(groups.map(x=>x.wounds.threshold),[15,15,16]);
 assert.equal(groups[0].soak,6);
 assert.equal(groups[2].skills.liderazgo,5);
 assert.equal(groups[2].source.authority,'SOURCE_CHECKED_PROFILE');
 assert.equal(groups[2].weapons[0].pierce,5);
 w=a.recordEncounterOutcome(w,{encounterId:spec.id,outcome:'negotiated'}).world;
 w=go(w,'capture-noira');
 const porel=a.encounterSpec(w);
 assert.equal(porel.adversaries[0].profileKey,'mpq_porel');
 const npc=createAdversary(porel.adversaries[0].profileKey);
 assert.equal(npc.skills.frialdad,3);
 assert.equal(npc.talents.find(t=>t.key==='adversario').rank,1);
});
test('P182: unknown conditions fail closed and no active encounter can be skipped',()=>{
 let w=toShowdown();
 const active=a.markEncounterActive(w,'pirate-queen-duel');
 const before=copy(active);
 assert.throws(()=>go(active,'capture-noira'),/encontro ativo/);
 assert.deepEqual(active,before);
 assert.throws(()=>a.recordEncounterOutcome(active,{encounterId:'pirate-queen-duel',outcome:'forged'}),/inválido/);
 assert.deepEqual(active,before);
});
