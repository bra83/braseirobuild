import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelArrow7Chase,ARROW7_CHASE_RANGES} from '../app/src/main/assets/vtt/js/adventure/jewelArrow7Chase.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P206');this.saves=0;}load(){return clone(this.state)}save(s){this.state=clone(s);this.saves++;}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'round-'+i});}g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');return {g,repo};}
function skill(g,s){const c=clone(g.state.player.character);const map={mecanica:'intelecto',frialdad:'voluntad',atletismo:'fortaleza'};c.characteristics[map[s]]=7;c.skills[s]=7;g.commit('CHARACTER_UPDATED',{character:c});}
function ready(){const {g,repo}=game();skill(g,'mecanica');g.resolveJewelEscapeBomb({actionId:'det',step:'detect',skill:'mecanica',rng:()=>.28});skill(g,'frialdad');g.resolveJewelEscapeBomb({actionId:'dis',step:'disarm',skill:'frialdad',rng:()=>.99});g.resolveJewelEscapeImpact({actionId:'blast',mode:'blast',rng:()=>.28});skill(g,'atletismo');g.resolveJewelEscapeImpact({actionId:'fall',mode:'fall',fallRange:'short',skill:'atletismo',gmConfirmed:true,rng:()=>.28});g.beginPublishedAdventureEncounter('arend-shen-four-goons',{rng:()=>.5});return {g,repo};}
function opts(extra={}){return {actionId:'p206chase',playerRole:'evader',initialRange:'Médio',arrow7Pilot:{id:'pilot-sheet-shen-1',agility:0,rank:0},terrainDifficulty:0,terrainSetback:0,gmBothVehiclesConfirmed:true,gmTerrainConfirmed:true,gmStartingRangeConfirmed:true,gmNpcPilotSheetConfirmed:true,gmSpeedsVerified:false,rng:()=>.28,...extra};}
function atomic(g,fn,re){const state=clone(g.state),saves=g.repository.saves;assert.throws(fn,re);assert.deepEqual(g.state,state);assert.equal(g.repository.saves,saves);}
test('P206 official vehicle chase range is planetary and personal turret stays distinct',()=>{assert.deepEqual(ARROW7_CHASE_RANGES,['Bocajarro','Curto','Médio','Longo','Extremo']);const {g}=ready();const c=clone(g.state.player.character);c.characteristics.agilidad=7;c.skills.pilotar_vehiculo_planetario=7;g.commit('CHARACTER_UPDATED',{character:c});const r=g.resolveJewelArrow7Chase(opts());assert.equal(r.adjudication.source.scale,'planetary');assert.equal(r.adjudication.winner,'player');assert.equal(r.adjudication.bandShift,1);assert.equal(r.adjudication.newRange,'Longo');assert.equal(g.state.combat.adversaries[0].range,'Médio');});
test('P206 requires active official encounter and actual pilot sheets',()=>{const {g}=game();atomic(g,()=>g.resolveJewelArrow7Chase(opts()),/encontro oficial/);const {g:h}=ready();atomic(h,()=>h.resolveJewelArrow7Chase(opts({gmNpcPilotSheetConfirmed:false})),/ficha confirmada/);atomic(h,()=>h.resolveJewelArrow7Chase(opts({gmBothVehiclesConfirmed:false})),/dois veículos/);atomic(h,()=>h.resolveJewelArrow7Chase(opts({arrow7Pilot:{id:'npc',agility:null,rank:3}})),/ficha confirmada/);});
test('P206 two real rolls, optional speed bonus only when both verified, with persistence x3',()=>{const {g,repo}=ready();const c=clone(g.state.player.character);c.characteristics.agilidad=7;c.skills.pilotar_vehiculo_planetario=7;g.commit('CHARACTER_UPDATED',{character:c});const r=g.resolveJewelArrow7Chase(opts({gmSpeedsVerified:true,playerSpeed:3,arrow7Speed:1}));assert.equal(r.adjudication.bandShift,3);assert.equal(r.adjudication.status,'ESCAPE_PENDING_GM');assert.equal(r.adjudication.checks.player.roll.faces.length,7);assert.equal(r.adjudication.checks.arrow7.roll.faces.length,0);assert.equal(validateJewelArrow7Chase(g.state.world.publishedAdventure).history.length,1);for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(reopened.state.combat.arrow7Chase.range,'Extremo');assert.equal(validateJewelArrow7Chase(reopened.state.world.publishedAdventure).status,'ESCAPE_PENDING_GM');}});
test('P206 does not invent speeds or accept unverified bonuses',()=>{const {g}=ready();atomic(g,()=>g.resolveJewelArrow7Chase(opts({gmSpeedsVerified:true,playerSpeed:null,arrow7Speed:5})),/Velocidades atuais/);const r=g.resolveJewelArrow7Chase(opts({playerSpeed:6,arrow7Speed:0}));assert.equal(r.adjudication.playerSpeed,null);assert.equal(r.adjudication.arrow7Speed,null);assert.ok(r.adjudication.bandShift<=1);});
test('P206 repeated chase in same round and after acted actor are atomic errors',()=>{const {g}=ready();g.resolveJewelArrow7Chase(opts());atomic(g,()=>g.resolveJewelArrow7Chase(opts({actionId:'second'})),/já resolvida/);atomic(g,()=>g.resolveJewelArrow7Chase(opts()),/duplicado/);});
test('P206 anti-fraud checks face, pool, outcome, pilot provenance, duplicate receipt and order',()=>{const {g}=ready();g.resolveJewelArrow7Chase(opts());const mutate=[p=>p.operations.receipts.p206chase.checks.player.roll.faces[0].index=7,p=>p.operations.receipts.p206chase.checks.arrow7.pool.difficulty=5,p=>p.operations.receipts.p206chase.newRange='Extremo',p=>p.operations.receipts.p206chase.arrow7Pilot.provenance='OFFICIAL',p=>p.operations.receipts.fake={...p.operations.receipts.p206chase,id:'fake'}];for(const f of mutate){const w=clone(g.state.world);f(w.publishedAdventure);assert.throws(()=>getPublishedAdventureAdapter('jewel-of-yavin').status(w),/adulterad|diverge|Sequência|Face|Pool|Resultado|perseguição/);}});
test('P206 Android UI, bridge, offline bundle, version and source remain integrated',()=>{const root=new URL('../app/src/main/assets/vtt/',import.meta.url);for(const [f,t] of [['r46/final-runtime.js','data-jewel-chase-roll'],['js/core/uiBridge.mjs','JEWEL_ARROW7_CHASE_FAILED'],['js/core/uiBridge.bundle.js','jewelArrow7Chase.mjs'],['js/adventure/jewelOfYavin.mjs','validateJewelArrow7Chase']])assert.ok(readFileSync(new URL(f,root),'utf8').includes(t),f);assert.ok(Number((readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode = (\d+)/)||[])[1])>=10208);});
test('P206 a second round compares actual successes; tied checks preserve planetary distance',async()=>{
 const {resolveJewelArrow7Chase}=await import('../app/src/main/assets/vtt/js/adventure/jewelArrow7Chase.mjs');
 const {g}=ready();const first=g.resolveJewelArrow7Chase(opts());assert.equal(first.chase.status,'ACTIVE');
 const c=clone(g.state.combat);c.round++;if(c.encounter)c.encounter.actedActorIds=[];
 const second=resolveJewelArrow7Chase(g.state.world,c,g.state.player.character,opts({actionId:'round-two',rng:()=>0}));
 assert.equal(second.adjudication.winner,'tie');assert.equal(second.adjudication.bandShift,0);assert.equal(second.chase.range,first.chase.range);
 assert.equal(validateJewelArrow7Chase(second.world.publishedAdventure).history.length,2);
});
test('P206 forbids moving chase range after any actor has taken a turn',async()=>{
 const {resolveJewelArrow7Chase}=await import('../app/src/main/assets/vtt/js/adventure/jewelArrow7Chase.mjs');
 const {g}=ready(),c=clone(g.state.combat);c.encounter??={};c.encounter.actedActorIds=['pc'];
 assert.throws(()=>resolveJewelArrow7Chase(g.state.world,c,g.state.player.character,opts()),/início da rodada/);
});
test('P206 pending GM escape cannot silently trigger more pursuit rolls',async()=>{
 const {resolveJewelArrow7Chase}=await import('../app/src/main/assets/vtt/js/adventure/jewelArrow7Chase.mjs');
 const {g}=ready(),character=clone(g.state.player.character);character.characteristics.agilidad=7;character.skills.pilotar_vehiculo_planetario=7;
 const first=resolveJewelArrow7Chase(g.state.world,g.state.combat,character,opts({gmSpeedsVerified:true,playerSpeed:4,arrow7Speed:1}));
 assert.equal(first.chase.status,'ESCAPE_PENDING_GM');const c=clone(first.combat);c.round++;
 assert.throws(()=>resolveJewelArrow7Chase(first.world,c,character,opts({actionId:'next',gmSpeedsVerified:true,playerSpeed:4,arrow7Speed:1})),/aguarda decisão/);
});
test('P206 GM explicitly confirms ESCAPED; end receipt replays and persists',()=>{
 const {g,repo}=ready();const c=clone(g.state.player.character);c.characteristics.agilidad=7;c.skills.pilotar_vehiculo_planetario=7;g.commit('CHARACTER_UPDATED',{character:c});
 g.resolveJewelArrow7Chase(opts({gmSpeedsVerified:true,playerSpeed:4,arrow7Speed:1}));
 atomic(g,()=>g.confirmJewelArrow7ChaseOutcome({actionId:'end1',outcome:'ESCAPED',gmConfirmed:false}),/confirmação expressa/);
 atomic(g,()=>g.confirmJewelArrow7ChaseOutcome({actionId:'end1',outcome:'CONTACT',gmConfirmed:true}),/contato\/fuga real/);
 const end=g.confirmJewelArrow7ChaseOutcome({actionId:'end1',outcome:'ESCAPED',gmConfirmed:true});
 assert.equal(end.chase.status,'ESCAPED');assert.equal(end.adjudication.kind,'JEWEL_ARROW7_CHASE_END');
 for(let i=0;i<3;i++){const re=new GameController(repo);assert.equal(re.state.combat.arrow7Chase.status,'ESCAPED');assert.equal(validateJewelArrow7Chase(re.state.world.publishedAdventure).endReceipt.outcome,'ESCAPED');}
 atomic(g,()=>g.confirmJewelArrow7ChaseOutcome({actionId:'end2',outcome:'ESCAPED',gmConfirmed:true}),/contato\/fuga real/);
});
test('P206 chase end receipt rejects fake range, source and outcome',()=>{
 const {g}=ready();const c=clone(g.state.player.character);c.characteristics.agilidad=7;c.skills.pilotar_vehiculo_planetario=7;g.commit('CHARACTER_UPDATED',{character:c});
 g.resolveJewelArrow7Chase(opts({gmSpeedsVerified:true,playerSpeed:4,arrow7Speed:1}));g.confirmJewelArrow7ChaseOutcome({actionId:'end1',outcome:'ESCAPED',gmConfirmed:true});
 for(const mutate of [p=>p.operations.receipts.end1.range='Longo',p=>p.operations.receipts.end1.outcome='CONTACT',p=>p.operations.receipts.end1.gmConfirmed=false]){
  const w=clone(g.state.world);mutate(w.publishedAdventure);assert.throws(()=>getPublishedAdventureAdapter('jewel-of-yavin').status(w),/adulterad|diverge/);
 }
});
