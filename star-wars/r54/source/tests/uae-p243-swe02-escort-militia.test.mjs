import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo{constructor(){this.state=createCampaign('P243');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
function fixture({evacuate=false,scene='new-meen-miners'}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 let w=adapter.start(g.state.world).world;
 for(const c of ['dock-ryloth','help-new-meen','travel-lylek-den'])w=adapter.advance(w,c).world;w=resolveOldLylekRetreat(adapter,w,3);w=adapter.advance(w,'reach-new-meen').world;
 w.publishedAdventure.flags.confirmedPartySize=3;
 if(evacuate)w=adapter.advance(w,'escort-new-meen-evacuees').world;
 else if(scene==='drombb-cantina'){w=adapter.advance(w,'confront-drombb-directly').world;w=adapter.recordNewMeenLifecycle(w,'compound_approach',{precautions:true}).world;w=adapter.advance(w,'new-meen-enter-cantina').world;}
 else if(scene==='new-meen-night-raid')w=adapter.advance(w,'stay-new-meen-night').world;
 g.state.world=w;repo.save(g.state);return {repo,g};
}
function reopen(repo){for(let i=0;i<3;i++){const g=new GameController(repo);assert.ok(adapter.status(g.state.world).active);}}
test('P243 PDF p20: prepare tracks exactly 20 adults and unspecified children without false victory',()=>{
 const {repo,g}=fixture({evacuate:true});const r=g.prepareSWE02NewMeenEscort();assert.equal(r.adults,20);assert.equal(r.childrenCount,null);
 assert.equal(g.state.world.publishedAdventure.newMeenEscort.status,'ASSEMBLED');assert.equal(g.state.world.publishedAdventure.flags.newMeenOutcome,'EVACUATING');
 assert.equal(g.state.world.publishedAdventure.flags.newMeenEscortArrived,undefined);reopen(repo);
});
test('P243 PDF p20: approved travel ticks advance living world and record Bura favor but not liberation',()=>{
 const {repo,g}=fixture({evacuate:true});g.prepareSWE02NewMeenEscort();const start=g.state.world.clock.tick;
 const depart=g.departSWE02NewMeenEscort({travelTicks:3});assert.equal(depart.gmApprovedTravelTicks,3);
 const arrival=g.arriveSWE02NewMeenEscort();assert.equal(arrival.timeAdvancedTicks,3);
 assert.equal(g.state.world.clock.tick,start+3);assert.equal(g.state.world.publishedAdventure.newMeenEscort.status,'ARRIVED');
 assert.equal(g.state.world.publishedAdventure.newMeenEscort.significantFavor,true);
 assert.equal(g.state.world.publishedAdventure.flags.newMeenOutcome,'EVACUATING');
 assert.equal(g.state.world.publishedAdventure.flags.drombbThreatNeutralized,undefined);
 assert.equal(adapter.status(g.state.world).node.choices.some(x=>x.id==='ota-after-new-meen-escort'),true);
 g.advancePublishedAdventure('ota-after-new-meen-escort');assert.equal(g.state.world.publishedAdventure.currentNodeId,'ota-briefing');reopen(repo);
});
test('P243 elapsed world time counts toward escort and is not charged twice',()=>{
 const {repo,g}=fixture({evacuate:true});g.prepareSWE02NewMeenEscort();g.departSWE02NewMeenEscort({travelTicks:4});
 g.advanceWorldTime({ticks:2});const tick=g.state.world.clock.tick;const r=g.arriveSWE02NewMeenEscort();
 assert.equal(r.timeAdvancedTicks,2);assert.equal(g.state.world.clock.tick,tick+2);reopen(repo);
});
test('P243 cannot depart without assembly, arrive without departure or duplicate arrival',()=>{
 const {repo,g}=fixture({evacuate:true});let saves=repo.saves;
 assert.throws(()=>g.departSWE02NewMeenEscort({travelTicks:1}),/organize/);assert.equal(repo.saves,saves);
 g.prepareSWE02NewMeenEscort();saves=repo.saves;assert.throws(()=>g.arriveSWE02NewMeenEscort(),/não está em trânsito/);assert.equal(repo.saves,saves);
 g.departSWE02NewMeenEscort({travelTicks:1});g.arriveSWE02NewMeenEscort();saves=repo.saves;
 assert.throws(()=>g.arriveSWE02NewMeenEscort(),/não está em trânsito/);assert.equal(repo.saves,saves);reopen(repo);
});
test('P243 invalid GM ticks fail closed without advancing clock or saving',()=>{
 const {repo,g}=fixture({evacuate:true});g.prepareSWE02NewMeenEscort();const tick=g.state.world.clock.tick,saves=repo.saves;
 for(const v of [0,-1,1.5,97,undefined]){assert.throws(()=>g.departSWE02NewMeenEscort({travelTicks:v}),/Mestre deve aprovar/);assert.equal(repo.saves,saves);}
 assert.equal(g.state.world.clock.tick,tick);
});
test('P243 abandon has no Bura favor, no liberated settlement and survives three reopens',()=>{
 const {repo,g}=fixture({evacuate:true});g.prepareSWE02NewMeenEscort();g.abandonSWE02NewMeenEscort();
 assert.equal(g.state.world.publishedAdventure.newMeenEscort.significantFavor,false);
 assert.equal(g.state.world.publishedAdventure.flags.newMeenEscortArrived,undefined);
 g.advancePublishedAdventure('ota-after-new-meen-withdrawal');assert.equal(g.state.world.publishedAdventure.currentNodeId,'ota-briefing');reopen(repo);
});
test('P243 legacy direct Ota departure is explicitly abandonment, never credited escort',()=>{
 const {repo,g}=fixture({evacuate:true});g.advancePublishedAdventure('ota-after-new-meen-withdrawal');
 assert.equal(g.state.world.publishedAdventure.newMeenEscort.status,'ABANDONED');
 assert.equal(g.state.world.publishedAdventure.flags.newMeenEscortArrived,undefined);reopen(repo);
});
test('P243 tampered escort arrival, duration, or adults rejected by adapter status',()=>{
 const {g}=fixture({evacuate:true});g.prepareSWE02NewMeenEscort();g.departSWE02NewMeenEscort({travelTicks:2});g.arriveSWE02NewMeenEscort();
 for(const mutate of [p=>p.newMeenEscort.adults=22,p=>p.newMeenEscort.arrivalTick=0,p=>p.flags.newMeenEscortArrived=false]){
  const w=structuredClone(g.state.world);mutate(w.publishedAdventure);assert.throws(()=>adapter.status(w),/adulterado|sem viagem|sem chegada/);
 }
});
test('P243 militia takes a real blaster weapon out of inventory and tracks one adult',()=>{
 const {repo,g}=fixture();const before=g.state.player.character.weapons.length;
 const r=g.armSWE02NewMeenMilitia({weaponIndex:0});assert.equal(r.armedMembers,1);
 assert.equal(g.state.player.character.weapons.length,before-1);
 assert.equal(g.state.world.publishedAdventure.newMeenMilitia.loanedWeapons[0].weapon.name,'Fuzil blaster pesado');reopen(repo);
});
test('P243 rejects grenade, nonexistent weapon, wrong scene and no double spend',()=>{
 const {repo,g}=fixture();const saves=repo.saves;
 for(const index of [-1,2,999]){assert.throws(()=>g.armSWE02NewMeenMilitia({weaponIndex:index}),/blaster apropriada|selecione/);assert.equal(repo.saves,saves);}
 g.advancePublishedAdventure('escort-new-meen-evacuees');assert.throws(()=>g.armSWE02NewMeenMilitia({weaponIndex:0}),/cena de New Meen/);
});
test('P243 official optional abstract combat: one wound per armed ally and one allied loss, once per round',()=>{
 const {repo,g}=fixture({scene:'drombb-cantina'});g.armSWE02NewMeenMilitia({weaponIndex:0});
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});const target=g.state.combat.adversaries[0];const before=target.wounds.current;
 const r=g.applySWE02NewMeenMilitiaRound({targetGroupId:target.id});assert.equal(r.woundsInflicted,1);assert.equal(r.alliedLosses,1);
 assert.equal(g.state.combat.adversaries[0].wounds.current,before+1);
 assert.equal(g.state.world.publishedAdventure.newMeenMilitia.members,0);
 assert.throws(()=>g.applySWE02NewMeenMilitiaRound({targetGroupId:target.id}),/não há milicianos/);reopen(repo);
});
test('P243 militia never wounds Drombb, cannot attack outside combat and cannot act twice',()=>{
 const {repo,g}=fixture({scene:'drombb-cantina'});g.armSWE02NewMeenMilitia({weaponIndex:0});let saves=repo.saves;
 assert.throws(()=>g.applySWE02NewMeenMilitiaRound({targetGroupId:'fake'}),/combate publicado/);assert.equal(repo.saves,saves);
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});const boss=g.state.combat.adversaries.at(-1);
 saves=repo.saves;assert.throws(()=>g.applySWE02NewMeenMilitiaRound({targetGroupId:boss.id}),/grupo hostil ativo/);assert.equal(repo.saves,saves);
});
test('P243 invalid militia receipts and casualties rejected by adapter status',()=>{
 const {g}=fixture({scene:'drombb-cantina'});g.armSWE02NewMeenMilitia({weaponIndex:0});
 const w=structuredClone(g.state.world);w.publishedAdventure.newMeenMilitia.members=100;
 assert.throws(()=>adapter.status(w),/efetivo da milícia adulterado/);
});
test('P243 bridge and offline Android UI expose escort and militia actions',()=>{
 const bridge=new BraseiroUiBridge({repository:new Repo()});for(const name of ['prepareSWE02NewMeenEscort','departSWE02NewMeenEscort','arriveSWE02NewMeenEscort','abandonSWE02NewMeenEscort','armSWE02NewMeenMilitia','applySWE02NewMeenMilitiaRound'])assert.equal(typeof bridge[name],'function');
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 for(const id of ['swe02NewMeenEscort','swe02EscortPrepare','swe02EscortDepart','swe02EscortArrive','swe02EscortAbandon','swe02NewMeenMilitia','swe02MilitiaArm','swe02MilitiaRound']){assert.ok(html.includes(`id="${id}"`));assert.ok(ui.includes(id));}
});
test('P243 optional militia round cannot be duplicated or applied after combat outcome',()=>{
 const {g,repo}=fixture({scene:'drombb-cantina'});
 g.state.player.character.weapons.push({name:'Pistola blaster',skill:'armas_distancia_ligeras',range:'Médio',damage:6,crit:3});
 g.armSWE02NewMeenMilitia({weaponIndex:0});g.armSWE02NewMeenMilitia({weaponIndex:g.state.player.character.weapons.length-1});
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});const id=g.state.combat.adversaries[0].id;
 const r=g.applySWE02NewMeenMilitiaRound({targetGroupId:id});assert.equal(r.woundsInflicted,2);assert.equal(g.state.world.publishedAdventure.newMeenMilitia.members,1);
 const saves=repo.saves;assert.throws(()=>g.applySWE02NewMeenMilitiaRound({targetGroupId:id}),/já atuou/);assert.equal(repo.saves,saves);
 const c=structuredClone(g.state.combat);c.encounter.outcome='escaped';g.commit('COMBAT_ACTION',{combat:c});
 assert.throws(()=>g.applySWE02NewMeenMilitiaRound({targetGroupId:id}),/após desfecho/);reopen(repo);
});
test('P243 New Meen escort and militia panels remain visible without a vehicle',()=>{
 const html=fs.readFileSync(new URL('../app/src/main/assets/vtt/index.html',import.meta.url),'utf8');
 assert.ok(html.indexOf('id="swe02NewMeenEscort"')<html.indexOf('id="vehicleData"'));
 assert.ok(html.indexOf('id="swe02NewMeenMilitia"')<html.indexOf('id="vehicleData"'));
 const ui=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/ui/app.mjs',import.meta.url),'utf8');
 const render=ui.slice(ui.indexOf('function renderVehicle(state)'));
 assert.ok(render.indexOf("$('swe02NewMeenEscort').hidden")<render.indexOf('if(!vehicle) return;'));
});
