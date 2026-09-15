import test from 'node:test';
import assert from 'node:assert/strict';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';
import { FactionTurnService } from '../app/src/main/assets/www/core/faction-turn-service.js';

function faction(overrides={}){return{force:3,cunning:3,wealth:3,magic:'LOW',treasure:20,hp:12,maxHp:12,goal:{id:'g0'},assets:{},...overrides};}
function setup(seed='contest-1'){
 const s=makeEmptyCampaignState({campaignId:'contest',displayName:'contest',masterSeed:seed});
 s.factionState={
   a:faction({force:1,cunning:1,wealth:3,hp:12,maxHp:12,assets:{x:{assetId:'x',attribute:'FORCE',location:'L',hp:4,maxHp:4,counterattackContract:{damage:'1d2',defenseDice:1}}}}),
   b:faction({force:8,cunning:8,wealth:3,assets:{y:{assetId:'y',attribute:'FORCE',location:'L',hp:6,maxHp:6,attackContract:{attackAttribute:'FORCE',defendAttribute:'FORCE',damage:'1d4',dice:1}}}}),
 };
 const p=new MemoryPersistenceAdapter();p.createCampaign({campaignId:s.campaignId,displayName:s.displayName,stateJson:serializeCampaignState(s),masterSeed:s.rngState.masterSeed,rngAlgorithm:s.rngState.algorithm,initialSnapshotId:'s0'});
 const w=new WorldActionService({persistence:p});const start=w.startFactionTurn({campaignId:s.campaignId});assert.equal(start.status,'UPKEEP_REQUIRED');assert.equal(start.currentFactionId,'a');
 const svc=new FactionTurnService({persistence:p});assert.equal(svc.resolveUpkeep({campaignId:s.campaignId}).status,'ACTION_REQUIRED');return{p,svc,campaignId:s.campaignId};
}

test('contested Expand Influence remains zero-mutation without explicit immediate-attack decisions',()=>{const {p,svc,campaignId}=setup();const before=p.loadCampaign(campaignId);const r=svc.takeAction({campaignId,action:{type:'EXPAND_INFLUENCE',location:'L',existingOwnAssetIds:['x'],baseHp:5,rivalFactionIds:['b']}});assert.equal(r.status,'EXPAND_CONTEST_CONTRACT_REQUIRED_NO_MUTATION');assert.equal(p.loadCampaign(campaignId).stateJson,before.stateJson);});

test('winning rival can explicitly decline its allowed immediate Attack',()=>{const {p,svc,campaignId}=setup();const r=svc.takeAction({campaignId,action:{type:'EXPAND_INFLUENCE',location:'L',existingOwnAssetIds:['x'],baseHp:5,rivalFactionIds:['b'],contestDecisions:{b:{decline:true}}}});assert.equal(r.status,'GOAL_REVIEW_REQUIRED');assert.equal(r.resolution.contested,true);assert.deepEqual(r.resolution.winningRivals,['b']);assert.equal(r.resolution.immediateAttacks[0].declined,true);const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.factionState.a.basesOfInfluence[0].hp,5);});

test('winning rival immediate Attack can target the new Base with a source-bound Asset contract',()=>{const {p,svc,campaignId}=setup();const before=JSON.parse(p.loadCampaign(campaignId).stateJson);const r=svc.takeAction({campaignId,action:{type:'EXPAND_INFLUENCE',location:'L',existingOwnAssetIds:['x'],baseHp:5,rivalFactionIds:['b'],contestDecisions:{b:{attacks:[{attackerAssetId:'y'}]}}}});assert.equal(r.status,'GOAL_REVIEW_REQUIRED');assert.equal(r.resolution.contested,true);assert.equal(r.resolution.immediateAttacks.length,1);assert.equal(r.resolution.immediateAttacks[0].targetBase,true);assert.equal(r.resolution.immediateAttacks[0].result.outcome,'ATTACK_SUCCESS');const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.factionState.a.basesOfInfluence[0].hp,4);assert.equal(s.factionState.a.hp,before.factionState.a.hp-1);});

test('creator can explicitly block an immediate Attack with another present Asset',()=>{const {p,svc,campaignId}=setup();const r=svc.takeAction({campaignId,action:{type:'EXPAND_INFLUENCE',location:'L',existingOwnAssetIds:['x'],baseHp:5,rivalFactionIds:['b'],contestDecisions:{b:{attacks:[{attackerAssetId:'y',defenderAssetId:'x'}]}}}});assert.equal(r.status,'GOAL_REVIEW_REQUIRED');assert.equal(r.resolution.immediateAttacks[0].targetBase,false);const s=JSON.parse(p.loadCampaign(campaignId).stateJson);assert.equal(s.factionState.a.basesOfInfluence[0].hp,5);assert.ok(s.factionState.a.assets.x.hp<=4);});
