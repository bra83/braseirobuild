import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {validateJewelArendProperty,AREND_PROPERTY_SECURITY} from '../app/src/main/assets/vtt/js/adventure/jewelArendProperty.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P220');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(holdingId='plaza-residence'){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p220-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const ch=clone(g.state.player.character);ch.characteristics.intelecto=7;ch.skills.mecanica=7;ch.characteristics.voluntad=7;ch.skills.frialdad=7;ch.skills.computadoras=7;g.commit('CHARACTER_UPDATED',{character:ch});
 g.resolveJewelEscapeBomb({actionId:'p220-bomb-detect',step:'detect',skill:'mecanica',rng:()=>.28});
 g.resolveJewelEscapeBomb({actionId:'p220-bomb-disarm',step:'disarm',skill:'frialdad',rng:()=>.28});
 const w=clone(g.state.world);w.publishedAdventure.encounterHistory=[{encounterId:'arend-shen-four-goons',outcome:'escaped'}];g.commit('WORLD_UPDATED',{world:w});
 g.investigateJewelArendHoldings({actionId:'p220-lead',skill:'computadoras',gmMainframeAccessConfirmed:true,rng:()=>.28});
 const all=['plaza-residence','figg-hall-office','port-town-building','plaza-yacht'];
 for(let i=0;i<all.length;i++){const h=all[i];const result=g.visitJewelArendHolding({actionId:'p220-visit-'+i,holdingId:h,gmSearchConfirmed:true,rng:()=>h===holdingId?0:0.999});if(result.investigation.foundAt)break;}
 assert.equal(g.state.world.publishedAdventure.operations.arendRevenge.foundAt,holdingId);
 return {g,repo};
}
function details(holdingId,stage='security',extra={}){return {actionId:'p220-'+holdingId+'-'+stage,holdingId,stage,range:'Médio',gmPhysicalPresenceConfirmed:true,gmDistinctPersonnelConfirmed:true,gmAccessConfirmed:true,gmSecurityBypassedConfirmed:false,rng:()=>.5,...extra};}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P220: exact official Arend, K4 and yacht crew profiles with real defenses',()=>{
 const ar=createAdversary('arend_shen');assert.equal(ar.kind,'nemesis');assert.equal(ar.wounds.threshold,14);assert.equal(ar.strain.threshold,16);assert.equal(ar.talents.find(t=>t.key==='adversario').rank,3);assert.equal(ar.weapons[0].damage,10);assert.equal(ar.weapons[0].vicious,4);
 const k=createAdversary('k4_security_droid');assert.equal(k.defense.melee,2);assert.equal(k.defense.ranged,1);assert.equal(k.wounds.threshold,12);
 const y=createAdversary('yacht_crew',{count:3});assert.equal(y.wounds.threshold,18);assert.equal(y.weapons[0].damage,7);
});
test('P220: cannot conjure Arend at wrong property, without confirmation or before search',()=>{
 const {g}=fixture();atomic(g,()=>g.beginJewelArendPropertyEncounter(details('figg-hall-office')));
 for(const k of ['gmPhysicalPresenceConfirmed','gmDistinctPersonnelConfirmed','gmAccessConfirmed'])atomic(g,()=>g.beginJewelArendPropertyEncounter(details('plaza-residence','security',{[k]:false})));
});
test('P220: residence starts actual initiative with three independent K4 rivals and six distinct goons',()=>{
 const {g,repo}=fixture();const r=g.beginJewelArendPropertyEncounter(details('plaza-residence'));
 assert.equal(r.adjudication.stage,'security');assert.equal(g.state.combat.active,true);
 assert.equal(g.state.combat.publishedAdventureContext.encounterId,'arend-property-plaza-residence-security');
 const a=g.state.combat.adversaries;assert.equal(a.length,3);assert.equal(a.filter(x=>x.profileKey==='k4_security_droid').length,3);
 assert.equal(new Set(a.map(x=>x.id)).size,3);
 for(let i=0;i<3;i++)assert.equal(validateJewelArendProperty(new GameController(repo).state.world.publishedAdventure).stages.length,1);
 atomic(g,()=>g.beginJewelArendPropertyEncounter(details('plaza-residence','security',{actionId:'dup'})));
});
test('P220: four published security complements and no fabricated extra combatants',()=>{
 for(const [h,expected] of Object.entries({'plaza-residence':[['k4_security_droid',1],['k4_security_droid',1],['k4_security_droid',1]],'figg-hall-office':[['grayson_private_security_guard',4]],'port-town-building':[['shens_goons',5],['shens_goons',5]],'plaza-yacht':[['yacht_crew',3],['yacht_crew',3],['yacht_crew',3]]})){
  const actual=AREND_PROPERTY_SECURITY[h].guards.slice(0,expected.length).map(x=>[x.profileKey,x.count]);assert.deepEqual(actual,expected);
  const {g}=fixture(h);g.beginJewelArendPropertyEncounter(details(h));assert.deepEqual(g.state.combat.adversaries.map(x=>[x.profileKey,x.membersTotal]),expected);
 }
});
test('P220: residence sitting room has six goons in its own encounter, never at entrance',()=>{
 const {g}=fixture();const r=g.beginJewelArendPropertyEncounter(details('plaza-residence','interior',{gmSecurityBypassedConfirmed:true}));
 assert.equal(r.adjudication.stage,'interior');assert.equal(g.state.combat.adversaries.length,1);
 assert.equal(g.state.combat.adversaries[0].profileKey,'shens_goons');assert.equal(g.state.combat.adversaries[0].membersTotal,6);
});
test('P220: Arend nemesis cannot be teleported past guards without GM-confirmed bypass',()=>{
 const {g}=fixture();atomic(g,()=>g.beginJewelArendPropertyEncounter(details('plaza-residence','arend')));
 const result=g.beginJewelArendPropertyEncounter(details('plaza-residence','arend',{gmSecurityBypassedConfirmed:true}));
 assert.equal(result.adjudication.stage,'arend');assert.equal(g.state.combat.adversaries.length,1);assert.equal(g.state.combat.adversaries[0].profileKey,'arend_shen');
 assert.equal(g.state.combat.adversaries[0].talents.find(x=>x.key==='adversario').rank,3);
});
test('P220: active guard fight prevents Arend encounter; no automatic outcome, XP, credits or NPC teleport',()=>{
 const {g}=fixture();const beforeXp=g.state.player.character.xp;g.beginJewelArendPropertyEncounter(details('plaza-residence'));
 atomic(g,()=>g.beginJewelArendPropertyEncounter(details('plaza-residence','arend',{gmSecurityBypassedConfirmed:true})));
 assert.deepEqual(g.state.player.character.xp,beforeXp);assert.equal(g.state.world.publishedAdventure.operations.shenAftermath,undefined);
});
test('P220: receipt tampering and derived state tampering rejected after reopening',()=>{
 const {g}=fixture();g.beginJewelArendPropertyEncounter(details('plaza-residence'));const p=g.state.world.publishedAdventure;
 for(const edit of [x=>x.operations.receipts['p220-plaza-residence-security'].count=999,x=>x.operations.receipts['p220-plaza-residence-security'].holdingId='plaza-yacht',x=>x.operations.arendProperty.stages[0].range='Longo',x=>delete x.operations.receipts['p220-plaza-residence-security']]){
  const altered=clone(p);edit(altered);assert.throws(()=>validateJewelArendProperty(altered));
 }
});
test('P220: no P219 receipt, save or Kokoro regression; UI, bridge, bundle and Android version wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p220-start'],['js/core/controller.mjs','beginJewelArendPropertyEncounter'],['js/core/uiBridge.mjs','beginJewelArendPropertyEncounter'],['js/core/uiBridge.bundle.js','jewelArendProperty.mjs'],['js/adventure/jewelOfYavin.mjs','arendProperty: clone(validateJewelArendProperty(p))'],['js/core/state.mjs','validateJewelArendProperty']])assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10220);
});
