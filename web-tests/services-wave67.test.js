import test from 'node:test';
import assert from 'node:assert/strict';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { CombatActionService } from '../app/src/main/assets/www/core/combat-service.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';

function makeState({campaignId='c70',seed='seed70',withWeapon=false,withFactions=false}={}){
 const s=makeEmptyCampaignState({campaignId,displayName:'70%',masterSeed:seed});
 s.characterState={characterId:'iria',name:'IRIA',level:1,baseAttackBonus:0,attributes:{Strength:7,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9},skills:{Survive:1,Notice:1,Sneak:0,Exert:0}};
 if(withWeapon)s.characterState.inventory=[{itemId:'test-spear',name:'Fixture de arma',weaponContract:{combatSkill:'Stab',attribute:'Strength',damage:'1d6',melee:true,shock:{damage:1,ac:13}}}];
 s.npcState={g1:{npcId:'g1',name:'Alvo de teste',hp:8,meleeAC:10,rangedAC:10,shockImmune:false,publicFacts:[],gmSecrets:[],memories:[]}};
 s.worldState={time:null,position:{q:17,r:22,label:'Ruínas da Vigília'},weather:null,expedition:{supplies:{foodUnits:5,waterUnits:5}}};
 if(withFactions)s.factionState={fA:{force:3,cunning:2,wealth:5,treasure:1},fB:{force:2,cunning:4,wealth:3,treasure:0}};
 return s;
}
function setup(opts={}){const state=makeState(opts);const p=new MemoryPersistenceAdapter();p.createCampaign({campaignId:state.campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'s0'});return{state,p,campaignId:state.campaignId};}

test('combat start commits deterministic side initiative and reopens without reroll',()=>{
 const {p,campaignId}=setup(); const svc=new CombatActionService({persistence:p});
 const r=svc.startCombat({campaignId,enemyDexterityModifier:0}); assert.equal(r.status,'OK'); assert.equal(r.receipt.newRevision,1);
 const reopen=p.loadCampaign(campaignId);const s=JSON.parse(reopen.stateJson);assert.equal(s.encounterState.combat.active,true);assert.equal(s.encounterState.combat.round,1);assert.equal(reopen.revision,1);
});

test('save action commits exactly once and uses IRIA canonical target',()=>{
 const {p,campaignId}=setup();const svc=new CombatActionService({persistence:p}); const r=svc.rollSave({campaignId,saveType:'MENTAL'});
 assert.equal(r.status,'OK');assert.equal(r.resolution.target,14);assert.equal(p.loadCampaign(campaignId).revision,1);assert.equal(p.listEvents(campaignId).events.length,1);
});

test('attack refuses to invent inventory when canonical inventory is absent',()=>{
 const {p,campaignId}=setup();const svc=new CombatActionService({persistence:p});const before=p.loadCampaign(campaignId);const r=svc.attackNpc({campaignId,npcId:'g1',weaponId:'missing'});const after=p.loadCampaign(campaignId);
 assert.equal(r.status,'MISSING_EVIDENCE_INVENTORY');assert.equal(after.revision,before.revision);assert.equal(after.stateJson,before.stateJson);
});

test('attack with explicit fixture weapon commits damage and RNG atomically',()=>{
 const {p,campaignId}=setup({withWeapon:true});const svc=new CombatActionService({persistence:p});const r=svc.attackNpc({campaignId,npcId:'g1',weaponId:'test-spear'});assert.equal(r.status,'OK');
 const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.npcState.g1.hp,r.targetHpAfter);assert.equal(s.revision,1);assert.equal(p.listEvents(campaignId).events[0].eventKind,'PLAYER_ATTACK');
});

test('travel day requires GM contract and does zero mutation when absent',()=>{
 const {p,campaignId}=setup();const svc=new WorldActionService({persistence:p});const before=p.loadCampaign(campaignId);const r=svc.travelDay({campaignId,contract:null});const after=p.loadCampaign(campaignId);
 assert.equal(r.status,'MISSING_GM_TRAVEL_CONTRACT');assert.equal(after.stateJson,before.stateJson);
});

test('travel day commits time, encounter RNG, supplies and GM-provided position atomically',()=>{
 const {p,campaignId}=setup();const svc=new WorldActionService({persistence:p});const r=svc.travelDay({campaignId,contract:{terrain:'PLAINS',hours:8,dayEncounterDie:6,nightEncounterDie:8,people:1,positionAfter:{q:18,r:22,label:'Hex vizinho'}}});
 assert.equal(r.status,'OK');const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.worldState.elapsedDays,1);assert.deepEqual(s.worldState.position,{q:18,r:22,label:'Hex vizinho'});assert.deepEqual(s.worldState.expedition.supplies,{foodUnits:4,waterUnits:4});
});

test('faction turn start is zero mutation with no factions',()=>{
 const {p,campaignId}=setup();const svc=new WorldActionService({persistence:p});const before=p.loadCampaign(campaignId);const r=svc.startFactionTurn({campaignId});assert.equal(r.status,'NO_FACTIONS_NO_MUTATION');assert.equal(p.loadCampaign(campaignId).stateJson,before.stateJson);
});

test('faction turn start rolls initiative, grants income only to first faction, then stops at upkeep gate',()=>{
 const {p,campaignId}=setup({withFactions:true});const svc=new WorldActionService({persistence:p});const r=svc.startFactionTurn({campaignId});assert.equal(r.status,'UPKEEP_REQUIRED');
 const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.worldState.factionTurn.phase,'UPKEEP_REQUIRED');assert.equal(s.worldState.factionTurn.initiativeOrder.length,2);
 const first=s.worldState.factionTurn.currentFactionId;const other=first==='fA'?'fB':'fA';
 const expectedIncome=first==='fA'?4:3;const initialTreasure=first==='fA'?1:0;const otherInitial=other==='fA'?1:0;
 assert.equal(s.factionState[first].treasure,initialTreasure+expectedIncome);assert.equal(s.factionState[other].treasure,otherInitial);
});

test('faction initiative tie requires GM order and performs zero mutation until supplied',()=>{
 let tiedSeed=null;
 for(let i=0;i<500;i+=1){
   const seed=`faction-tie-${i}`;
   const {p,campaignId}=setup({withFactions:true,seed});
   const svc=new WorldActionService({persistence:p});
   const before=p.loadCampaign(campaignId);
   const r=svc.startFactionTurn({campaignId});
   if(r.status==='GM_TIEBREAK_REQUIRED_NO_MUTATION'){
     tiedSeed=seed;
     const after=p.loadCampaign(campaignId);
     assert.equal(after.revision,before.revision);
     assert.equal(after.stateJson,before.stateJson);
     const ids=r.tiedGroups.flatMap(g=>g.factionIds);
     const resolved=svc.startFactionTurn({campaignId,tieBreakOrder:[...ids].reverse()});
     assert.equal(resolved.status,'UPKEEP_REQUIRED');
     assert.equal(p.loadCampaign(campaignId).revision,1);
     break;
   }
 }
 assert.ok(tiedSeed,'expected to find a deterministic d8 initiative tie');
});

test('forage service commits attempt but does not invent allocation; allocation is a separate atomic action',()=>{
 const {p,campaignId}=setup({seed:'forage-service'});const svc=new WorldActionService({persistence:p});
 let r=null;
 for(let i=0;i<20;i+=1){
   const attempt=svc.forage({campaignId,contract:{terrain:'WOODLANDS_OR_HEAVY_VEGETATION',wisdomScore:14,bestSurviveLevel:1,groupSurviveLevels:[1],encounterDie:8,fullDay:true,successiveDaysInSameHex:i}});
   if(attempt.status==='ALLOCATION_REQUIRED'){r=attempt;break;}
   if(attempt.status!=='OK')assert.fail(`unexpected forage status ${attempt.status}`);
 }
 assert.ok(r,'expected a successful deterministic forage attempt');
 const beforeAllocation=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(beforeAllocation.worldState.pendingForage.allocationRequired,true);
 const units=r.result.unitsFound;const alloc=svc.allocateForage({campaignId,foodUnits:units,waterUnits:0,firewoodUnits:0});assert.equal(alloc.status,'OK');
 const after=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(after.worldState.pendingForage,null);assert.equal(after.worldState.expedition.supplies.foodUnits,5+units);
});

test('forage allocation cannot exceed found units and does zero mutation on rejection',()=>{
 const {p,campaignId}=setup({seed:'forage-overalloc'});const svc=new WorldActionService({persistence:p});
 let r=null;
 for(let i=0;i<20;i+=1){const a=svc.forage({campaignId,contract:{terrain:'WOODLANDS_OR_HEAVY_VEGETATION',wisdomScore:18,bestSurviveLevel:1,groupSurviveLevels:[1],encounterDie:8,fullDay:true,successiveDaysInSameHex:i}});if(a.status==='ALLOCATION_REQUIRED'){r=a;break;}}
 assert.ok(r);const before=p.loadCampaign(campaignId);const bad=svc.allocateForage({campaignId,foodUnits:r.result.unitsFound+1});const after=p.loadCampaign(campaignId);assert.equal(bad.status,'FORAGE_ALLOCATION_EXCEEDS_FOUND_NO_MUTATION');assert.equal(after.stateJson,before.stateJson);
});
