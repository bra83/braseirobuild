/** U25: fail-closed regression for the actual U24/F01-F03 remote integration.
 * Tests production follower and receipt engines; NOT a browser or Android proof.
 */
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {OfficialEncounterFollowerEngine as Followers} from '../src/services/OfficialEncounterFollowerEngine.ts';
import {OfficialEncounterReceiptService as Receipts} from '../src/services/OfficialEncounterReceiptService.ts';
let checks=0;
const eq=(actual,expected,label)=>{assert.deepEqual(actual,expected,label);checks++};
const ok=(value,label)=>{assert.ok(value,label);checks++};
const journey=readFileSync(new URL('../src/services/JourneyService.ts',import.meta.url),'utf8');
const travel=readFileSync(new URL('../src/services/TravelCommitService.ts',import.meta.url),'utf8');
for(const [name,source] of [['JourneyService',journey],['TravelCommitService',travel]]){
  eq(source.split('OfficialEncounterFollowerEngine.advanceForTravel(').length-1,1,name+' one real movement call');
  ok(!source.includes('OfficialEncounterFollowerEngine.advanceWaterStowaways('),name+' no water-only shortcut');
  ok(!source.includes('OfficialEncounterFollowerEngine.advance('),name+' no land-only shortcut');
  ok(source.includes('await ')&&source.includes('.put(follower)'),name+' persists NPCs in transaction');
}
ok(/bondedCharacterId:characterId,officialFollowParty:true,officialFollowEventId:eventId/.test(journey),
  '#36 bond stores encounter identity, not just a follow flag');
const npc=(type,event,extra={})=>({id:'official-npc:A:'+event+':'+type,campaignId:'A',status:'Alive',
  currentLocationId:'h1',officialFollowParty:true,officialFollowEventId:event,memories:[],...extra});
const grelf=npc('grelf','e10'),linn=npc('linn','e27');
const hatchling=npc('black-dragon-hatchling','e36',{officialHatchling:true,bondedCharacterId:'hero',
 officialHatchlingState:{bondedCharacterId:'hero',strength:4,armor:2}});
const meroggha=npc('meroggha','e39',{firstSeenSceneId:'e39',hiddenFromParty:true,
 officialStowaway:true,officialFollowParty:false});
const all=[grelf,linn,hatchling,meroggha];
for(const [water,expected] of [[false,3],[true,4]]){
 const result=Followers.advanceForTravel(all,'A','h1','h2',100,water);
 eq(result.length,expected,water?'water transports all four':'land transports only companions');
 eq(new Set(result.map(x=>x.id)).size,expected,'no duplicated follower');
 ok(result.every(x=>x.currentLocationId==='h2'&&x.officialFollowLastMovedAt===100),'persistable destination');
 ok(result.every(x=>x.memories.length===1),'one follow memory');
 eq(result.find(x=>x.id===hatchling.id).officialHatchlingState.bondedCharacterId,'hero','bond identity preserved');
}
eq(Followers.advanceForTravel(all,'A','h1','h1',100,true).length,0,'no same-hex teleport');
eq(Followers.advanceForTravel([{...grelf,campaignId:'B'}],'A','h1','h2',100,false).length,0,'isolate campaigns');
eq(Followers.advanceForTravel([{...hatchling,officialFollowEventId:'fake'}],'A','h1','h2',100,false).length,0,'reject spoofed follow event');
eq(Followers.advanceForTravel([{...hatchling,officialHatchlingState:{bondedCharacterId:'other'}}],'A','h1','h2',100,false).length,0,'reject mismatched dragon bond');
const receipt=(choice,stage,decision={},status='resolved')=>Receipts.fromEvent({
 id:'e35',status,result:{mechanic:{kind:'official_random_encounter',encounterId:35},
 officialEncounterVisitNumber:1,officialSceneDecision:{choiceId:choice,stage,...decision}}});
for(const [choice,kind] of [['offer_adventurer','adventurer'],['offer_horse','horse']]){
 const sacrifice={kind,targetId:'victim-id',targetName:'Vítima',adjudicatedAt:123,
  evidence:'A Mestra confirmou o destino e a remoção.'};
 const valid={kargSacrifice:sacrifice,kargOfferAccepted:true,adjudicationMode:'official_karg_sacrifice',
  resolvedAt:123,pending:null};
 const positive=receipt(choice,'karg_sacrifice_adjudicated',valid);
 eq(positive.resourceTransferCommitted,true,kind+' valid sacrifice committed');
 eq(positive.followUpPending,false,kind+' valid sacrifice closed');
 ok(positive.label.includes('Sacrifício adjudicado'),kind+' sacrifice not mislabeled as gift');
 const mutations=[
  ['still-open',valid,'open','karg_sacrifice_adjudicated'],
  ['wrong-kind',{...valid,kargSacrifice:{...sacrifice,kind:kind==='horse'?'adventurer':'horse'}},'resolved','karg_sacrifice_adjudicated'],
  ['short-evidence',{...valid,kargSacrifice:{...sacrifice,evidence:'fake'}},'resolved','karg_sacrifice_adjudicated'],
  ['missing-victim',{...valid,kargSacrifice:{...sacrifice,targetId:''}},'resolved','karg_sacrifice_adjudicated'],
  ['mismatched-date',{...valid,resolvedAt:124},'resolved','karg_sacrifice_adjudicated'],
  ['missing-mode',{...valid,adjudicationMode:undefined},'resolved','karg_sacrifice_adjudicated'],
  ['unfinished-pending',{...valid,pending:'follow_up'},'resolved','karg_sacrifice_adjudicated'],
  ['unadjudicated-object',valid,'open','karg_sacrifice_pending'],
 ];
 for(const [name,data,status,stage] of mutations){
  const projected=receipt(choice,stage,data,status);
  eq(projected.resourceTransferCommitted,false,kind+' rejects '+name);
  eq(projected.followUpPending,true,kind+' still pending '+name);
 }
}
const gift=receipt('offer_gift','karg_gift_transferred',{kargGift:{transferredAt:100}});
eq(gift.resourceTransferCommitted,true,'legacy actual gift still recognized');
ok(gift.label.includes('Presente aceito'),'gift label remains separate from sacrifice');
console.log('U25_F01_F02_F03_REGRESSION PASS '+checks+'/'+checks+' (Node production + remote wiring contract; NOT Chromium/Android)');
