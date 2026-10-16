import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateJewelEpisode3Rewards,JEWEL_EP3_REWARDS} from '../app/src/main/assets/vtt/js/adventure/jewelEpisode3Rewards.mjs';
import {validateJewelOperations} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavinOperations.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P222');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p222-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 const w=clone(g.state.world);w.publishedAdventure.operations.escapeBomb={status:'DISARMED'};w.publishedAdventure.encounterHistory=[{encounterId:'arend-shen-four-goons',outcome:'escaped'}];g.commit('WORLD_UPDATED',{world:w});
 return {g,repo};
}
const alliance={actionId:'p222-aris-alliance',gmArisMetInPerson:true,gmArisAgreedToAlliance:true,gmPartyAcceptedAlliance:true,gmArisNoLongerHostile:true};
const award=(milestone,actionId='p222-xp')=>({milestone,actionId,gmAwardConfirmed:true});
function atomic(g,fn){const before=clone(g.state),n=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,n);}
test('P222 official Episode III p.95 rewards are 10/5/15, no speculative cloud car or escape reward',()=>{
 assert.deepEqual(Object.fromEntries(Object.entries(JEWEL_EP3_REWARDS).map(([k,v])=>[k,v.xp])),{HOLO_PARK_ESCAPE:10,ARIS_ALLIANCE:5,AREND_DEFEATED:15,CLOUD_CAR_PATROLS:5,BESPIN_ESCAPE:5});
 assert.equal(JEWEL_EP3_REWARDS.CLOUD_CARS,undefined);
});
test('P222 Aris cannot grant PE before a real alliance or without GM approval',()=>{
 const {g}=fixture();atomic(g,()=>g.grantJewelEpisode3Reward(award('ARIS_ALLIANCE')));
 g.recordJewelArisAlliance(alliance);atomic(g,()=>g.grantJewelEpisode3Reward({...award('ARIS_ALLIANCE'),gmAwardConfirmed:false}));
 atomic(g,()=>g.grantJewelEpisode3Reward(award('CLOUD_CARS')));
});
test('P222 real Aris milestone grants exactly 5 to actual PC, persists across 3 reopens and blocks double awards',()=>{
 const {g,repo}=fixture();g.recordJewelArisAlliance(alliance);
 const before=clone(g.state.player.character.xp),result=g.grantJewelEpisode3Reward(award('ARIS_ALLIANCE'));
 assert.equal(result.adjudication.xp,5);assert.equal(g.state.player.character.xp.total,before.total+5);
 assert.equal(g.state.player.character.xp.available,before.available+5);
 assert.equal(result.adjudication.characterId,'player-slot');
 assert.equal(validateJewelEpisode3Rewards(g.state.world.publishedAdventure).length,1);
 assert.doesNotThrow(()=>validateJewelOperations(g.state.world));
 for(let i=0;i<3;i++){const opened=new GameController(repo);assert.equal(opened.state.player.character.xp.total,before.total+5);assert.equal(validateJewelEpisode3Rewards(opened.state.world.publishedAdventure).length,1);}
 atomic(g,()=>g.grantJewelEpisode3Reward(award('ARIS_ALLIANCE','new-id')));
 atomic(g,()=>g.grantJewelEpisode3Reward(award('ARIS_ALLIANCE')));
});
test('P222 tampered XP amount, proof, PC, faces of receipt and derived state rejected',()=>{
 const {g}=fixture();g.recordJewelArisAlliance(alliance);g.grantJewelEpisode3Reward(award('ARIS_ALLIANCE'));
 for(const edit of [p=>p.operations.receipts['p222-xp'].xp=15,p=>p.operations.receipts['p222-xp'].proofReceiptId='fake',
  p=>p.operations.receipts['p222-xp'].characterId='__proto__',p=>p.operations.receipts['p222-xp'].afterAvailable+=5,
  p=>p.operations.episode3Rewards[0].xp=999,p=>delete p.operations.receipts['p222-xp']]){
  const p=clone(g.state.world.publishedAdventure);edit(p);assert.throws(()=>validateJewelEpisode3Rewards(p));
 }
});
test('P222 Arend XP cannot be granted merely because a GM recorded his fate; requires defeated real encounter',()=>{
 const {g}=fixture();atomic(g,()=>g.grantJewelEpisode3Reward(award('AREND_DEFEATED')));
 atomic(g,()=>g.grantJewelEpisode3Reward(award('HOLO_PARK_ESCAPE')));
});
test('P222 bridge, controller, Android UI, offline bundle, save validator and version are connected',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p222-xp-grant'],['js/core/controller.mjs','grantJewelEpisode3Reward'],
  ['js/core/uiBridge.mjs','grantJewelEpisode3Reward'],['js/core/uiBridge.bundle.js','jewelEpisode3Rewards.mjs'],
  ['js/core/state.mjs','validateJewelEpisode3Rewards'],['js/adventure/jewelOfYavin.mjs','episode3Rewards: clone(validateJewelEpisode3Rewards(p))']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 assert.ok(Number(readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8').match(/versionCode\s*=\s*(\d+)/)?.[1])>=10222);
});
