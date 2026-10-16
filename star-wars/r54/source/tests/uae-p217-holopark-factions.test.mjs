import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {validateJewelHoloPark} from '../app/src/main/assets/vtt/js/adventure/jewelHoloPark.mjs';
const clone=structuredClone, adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo {constructor(){this.state=createCampaign('P209');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function game(){
 const repo=new Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const w=clone(g.state.world),p=w.publishedAdventure;
 // Generate real P201 bomb receipts instead of a fabricated status.
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=6;ch.skills.mecanica=5;ch.characteristics.voluntad=6;ch.skills.frialdad=5;
 g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const valid=clone(g.state.world);p.operations=valid.publishedAdventure.operations;
 p.encounterHistory=[...(p.encounterHistory??[]),{encounterId:'arend-shen-four-goons',outcome:'adversaries_defeated',nodeId:p.currentNodeId}];
 p.activeEncounterId=null;
 g.commit('WORLD_UPDATED',{world:w});return {g,repo};
}
const args={actionId:'wing-holo-start',gmParkArrivalConfirmed:true,gmPursuersConfirmed:true,rng:()=>.5};
function atomic(g,fn){const s=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,s);assert.equal(g.repository.saves,n);}

const arend={actionId:'p217-arend-1',faction:'arend_goons',count:2,range:'Médio',gmParkPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmWingGuardNotEngagedConfirmed:true,rng:()=>.5};
const grayson={actionId:'p217-grayson-1',faction:'grayson_security',count:3,range:'Curto',gmParkPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmWingGuardNotEngagedConfirmed:true,rng:()=>.5};
function afterWing(){const {g,repo}=game();g.beginJewelHoloParkWingGuard(args);g.finishCombat({rng:()=>.5});const ch=clone(g.state.player.character);ch.derived.wounds.current=0;ch.derived.strain.current=0;g.commit('CHARACTER_UPDATED',{character:ch});return {g,repo};}
test('P217 SWE09 p78 Grayson security uses published NPC card, not generic Wing Guard',async()=>{
 const {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES,createAdversary}=await import('../app/src/main/assets/vtt/js/rules/adversaries.mjs');
 const p=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.grayson_private_security_guard;
 assert.equal(p.source.printedPage,78);assert.equal(p.source.crossCheck,'JoY:78; private_security_guard_75');
 assert.equal(p.soak,4);assert.equal(p.woundsThreshold,5);assert.equal(p.defense.ranged,1);
 assert.deepEqual(p.weapons.map(w=>[w.damage,w.crit]),[[7,3],[4,2]]);
 assert.equal(createAdversary(p.key,{count:3}).membersTotal,3);
 assert.equal(createAdversary(p.key,{count:3}).defense.ranged,1);
});
test('P217 cannot teleport faction or invent count and cannot join ongoing Wing Guard',()=>{
 const {g}=game();atomic(g,()=>g.beginJewelHoloParkFaction(arend));
 g.beginJewelHoloParkWingGuard(args);atomic(g,()=>g.beginJewelHoloParkFaction(arend));
 g.finishCombat({rng:()=>.5});
 atomic(g,()=>g.beginJewelHoloParkFaction({...arend,gmParkPresenceConfirmed:false}));
 atomic(g,()=>g.beginJewelHoloParkFaction({...arend,gmDistinctPersonnelConfirmed:false}));
 atomic(g,()=>g.beginJewelHoloParkFaction({...arend,gmWingGuardNotEngagedConfirmed:false}));
 atomic(g,()=>g.beginJewelHoloParkFaction({...arend,count:5}));
 atomic(g,()=>g.beginJewelHoloParkFaction({...grayson,count:7}));
 atomic(g,()=>g.beginJewelHoloParkFaction({...grayson,range:'Teleporte'}));
});
test('P217 Arend group starts actual initiative atomically, is not old Arrow-7 goon group',()=>{
 const {g,repo}=afterWing(),n=repo.saves;const out=g.beginJewelHoloParkFaction(arend);
 assert.equal(repo.saves,n+1);assert.equal(out.combat.active,true);
 assert.equal(out.combat.publishedAdventureContext.encounterId,'holo-park-arend-goons');
 assert.equal(out.combat.adversaries[0].profileKey,'shens_goons');
 assert.equal(out.combat.adversaries[0].membersTotal,2);
 assert.equal(out.combat.adversaries[0].range,'Médio');
 assert.equal(g.state.world.publishedAdventure.operations.holoFactions.arend_goons.count,2);
 atomic(g,()=>g.beginJewelHoloParkFaction({...arend,actionId:'p217-arend-repeat'}));
});
test('P217 Grayson profile starts combat and persists across three reopenings',()=>{
 const {g,repo}=afterWing(),n=repo.saves;g.beginJewelHoloParkFaction(grayson);
 assert.equal(repo.saves,n+1);
 assert.equal(g.state.combat.adversaries[0].profileKey,'grayson_private_security_guard');
 assert.equal(g.state.combat.adversaries[0].membersTotal,3);
 assert.equal(g.state.combat.adversaries[0].range,'Curto');
 for(let i=0;i<3;i++){const x=new GameController(repo);assert.equal(x.state.combat.adversaries[0].membersTotal,3);
  assert.equal(x.state.world.publishedAdventure.operations.holoFactions.grayson_security.count,3);}
});
test('P217 Grayson outcome recorded once and faction cannot respawn',()=>{
 const {g}=afterWing();g.beginJewelHoloParkFaction(grayson);
 const result=g.finishCombat({rng:()=>.5});
 assert.equal(result.publishedAdventureOutcome.encounterId,'holo-park-grayson-security');
 assert.equal(g.state.world.publishedAdventure.encounterHistory.filter(x=>x.encounterId==='holo-park-grayson-security').length,1);
 atomic(g,()=>g.beginJewelHoloParkFaction({...grayson,actionId:'p217-grayson-again'}));
});
test('P217 replay detects tampered faction count and identity',async()=>{
 const {g}=afterWing();g.beginJewelHoloParkFaction(grayson);
 const {validateJewelHoloFactions}=await import('../app/src/main/assets/vtt/js/adventure/jewelHoloFactions.mjs');
 const p=clone(g.state.world.publishedAdventure);p.operations.holoFactions.grayson_security.count=6;
 assert.throws(()=>validateJewelHoloFactions(p));
 p.operations.holoFactions=clone(g.state.world.publishedAdventure.operations.holoFactions);
 p.operations.receipts['p217-grayson-1'].count=6;assert.throws(()=>validateJewelHoloFactions(p));
});

test('P217 Android bridge and PT-BR UI wired to real controller',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [path,token] of [['r46/final-runtime.js','data-p217-faction-start'],['js/core/uiBridge.mjs','beginJewelHoloParkFaction'],['js/core/controller.mjs','beginJewelHoloParkFaction'],['js/adventure/jewelOfYavin.mjs','jewelHoloFactionEncounter']])
  assert.ok(readFileSync(new URL(path,root),'utf8').includes(token),path);
});
test('P217 preserves Holo-Park clock across distinct combat encounters without fictitious rounds',()=>{
 const {g}=game();g.beginJewelHoloParkWingGuard(args);
 const a=g.recordJewelHoloParkRound({actionId:'p217-wing-round',gmDistanceConfirmed:true,range:'Médio',gmPursuersNeutralizedConfirmed:false});
 assert.equal(a.adjudication.round,1);
 g.finishCombat({rng:()=>.5});const ch=clone(g.state.player.character);ch.derived.wounds.current=0;ch.derived.strain.current=0;g.commit('CHARACTER_UPDATED',{character:ch});
 g.beginJewelHoloParkFaction(grayson);
 assert.equal(g.state.combat.round,1);
 assert.equal(g.state.combat.holoParkRoundOffset,1);
 const b=g.recordJewelHoloParkRound({actionId:'p217-grayson-round',gmDistanceConfirmed:true,range:'Médio',gmPursuersNeutralizedConfirmed:false});
 assert.equal(b.adjudication.round,2);
 assert.equal(b.holoPark.roundCount,2);
});
test('P217 both source-backed factions can enter sequentially, never together or respawn',()=>{
 const {g}=afterWing();g.beginJewelHoloParkFaction(arend);
 atomic(g,()=>g.beginJewelHoloParkFaction({...grayson,actionId:'grayson-too-early'}));
 g.finishCombat({rng:()=>.5});
 const ch=clone(g.state.player.character);ch.derived.wounds.current=0;ch.derived.strain.current=0;g.commit('CHARACTER_UPDATED',{character:ch});
 const result=g.beginJewelHoloParkFaction(grayson);
 assert.equal(result.combat.adversaries.length,1);
 assert.equal(result.combat.adversaries[0].profileKey,'grayson_private_security_guard');
 assert.equal(Object.keys(g.state.world.publishedAdventure.operations.holoFactions).length,2);
});
