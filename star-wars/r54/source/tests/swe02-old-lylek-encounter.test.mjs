import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';

const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo { constructor(){this.state=createCampaign('SWE02 Old Lylek');this.saves=0;} load(){return structuredClone(this.state);} save(s){this.state=structuredClone(s);this.saves++;} }
function fixture(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.5});
 for(const choice of ['dock-ryloth','help-new-meen','travel-lylek-den'])g.advancePublishedAdventure(choice);
 return {repo,g};
}
function reopenThree(repo,assertState){let last;for(let n=0;n<3;n++){last=new GameController(repo);assertState(last.state);}return last;}
function reachHuntersReady(g,{slowDown=true}={}){
 g.chooseSWE02OldLylekRoute({slowDown});
 if(slowDown)g.approachSWE02OldLylekOnFoot();
 else {for(let n=0;n<2;n++)g.resolvePublishedAdventureCheck(0,{adventureId:'long-arm-of-the-hutt',rng:()=>.5});g.proceedSWE02OldLylekBluff();}
 assert.equal(g.state.world.publishedAdventure.oldLylek.phase,'combat-ready');
}

test('Old Lylek foot approach is available without a Perception declaration; hunter loot, warning and optional verified lylek attack survive three reopenings',()=>{
 const {repo,g}=fixture();
 assert.throws(()=>g.advancePublishedAdventure('reach-new-meen'),/Escolha inválida/);
 assert.throws(()=>g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5}),/pp.15–17/);
 reachHuntersReady(g);
 g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
 const hunters=structuredClone(g.state.combat);hunters.encounter.outcome='adversaries_defeated';
 for(const enemy of hunters.adversaries)enemy.status='incapacitated';
 hunters.swe02OldLylekAdvantageTriggered=true;
 g.commit('COMBAT_ACTION',{combat:hunters});repo.save(g.state);g.finishCombat({rng:()=>.5});
 g.inspectSWE02OldLylekSpeeder();
 let reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-hunters');assert.equal(old.lylekTriggered,true);assert.equal(old.lylekKilledHunter,true);
   assert.equal(old.hunterLoot.status,'found');assert.equal(old.hunterLoot.inspected,true);
   assert.deepEqual(old.hunterLoot.items,[{name:'stimpack',quantity:2},{name:'medpac',quantity:1},{name:'emergency repair kit',quantity:1}]);
   assert.equal(s.world.publishedAdventure.flags.lylekHuntersResolved,true);
 });
 reopened.insistSWE02OldLylek();
 assert.deepEqual(reopened.state.scene.visibleNpcIds,['laoth.npc.bura_ban']);
 assert.match(reopened.state.scene.narration,/carne fresca|presa/);
 reopened=reopenThree(repo,s=>assert.equal(s.world.publishedAdventure.oldLylek.phase,'lylek-warned'));
 const lylekProof=reopened.persistSWE02OldLylek();
 assert.equal(lylekProof.profileKey,'swe02_captive_rancor');
 assert.equal(lylekProof.source.profileSourceVerified,true);
 reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'lylek-ready');assert.equal(old.lylekEncounter.source.referencePrintedPage,47);
 });
 const profile=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.swe02_captive_rancor;
 assert.equal(profile.source.productCode,'SWE01');assert.equal(profile.source.printedPage,47);
 assert.deepEqual(profile.characteristics,{fortaleza:6,agilidad:2,intelecto:1,astucia:3,voluntad:3,presencia:1});
 assert.equal(profile.soak,12);assert.equal(profile.woundsThreshold,40);assert.equal(profile.strainThreshold,15);
 assert.equal(profile.skills.pelea,2);assert.equal(profile.weapons[0].damage,20);assert.equal(profile.weapons[0].crit,3);
 reopened.beginPublishedAdventureEncounter('lylek-den-lylek',{rng:()=>.5});
 assert.equal(reopened.state.combat.adversaries[0].profileKey,'swe02_captive_rancor');
 const lylek=reopened.state.combat.adversaries[0];
 const npc=reopened._resolveNpcTurn(structuredClone(reopened.state.combat),structuredClone(reopened.state.player.character),lylek.id,()=>.5,reopened.state.group);
 assert.equal(npc.attack.weapon.name,'Garras enormes');
 const ended=structuredClone(npc.combat);ended.encounter.outcome='ended';
 reopened.commit('COMBAT_ACTION',{combat:ended,character:npc.character,group:npc.group??reopened.state.group});repo.save(reopened.state);
 reopened.finishCombat({rng:()=>.5});
 reopened=reopenThree(repo,s=>{
   const old=s.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-lylek');assert.equal(old.lylekResolution.outcome,'ended');
   assert.equal(old.lylekResolution.beastProfileVerified,true);
   assert.equal(old.lylekResolution.profileKey,'swe02_captive_rancor');
 });
 reopened.advancePublishedAdventure('reach-new-meen');
 assert.equal(reopened.state.world.publishedAdventure.currentNodeId,'new-meen-miners');
});

test('failed fast-route Pilot checks keep the party-wide strain consequence visibly pending',()=>{
 const {repo,g}=fixture();g.chooseSWE02OldLylekRoute({slowDown:false});
 const failed=adapter.adjudicateCheck(g.state.world,{checkIndex:0,result:{success:false,netSuccess:0,netAdvantage:0}});
 const receipt=failed.world.publishedAdventure.oldLylek.pilotChecks[0];
 assert.equal(receipt.collision,true);assert.equal(receipt.onboardPcStrain.perPc,1);
 assert.equal(receipt.onboardPcStrain.status,'PENDENTE');
 assert.match(receipt.onboardPcStrain.resolution,/cada PJ a bordo/);
 assert.equal(failed.adjudication.playerStrainDelta,0);
 assert.equal(g.state.player.character.derived.strain.current,0);
});

test('retreat or incapacitation settles the hunter branch while preserving the lylek trigger and its p.18 loot rule',()=>{
 for(const outcome of ['escaped','player_incapacitated']){
   const {g}=fixture();reachHuntersReady(g);g.beginPublishedAdventureEncounter('lylek-den-bounty-hunters',{rng:()=>.5});
   const combat=structuredClone(g.state.combat);combat.encounter.outcome=outcome;
   for(const enemy of combat.adversaries)enemy.status='active';combat.swe02OldLylekAdvantageTriggered=true;
   g.commit('COMBAT_ACTION',{combat});g.finishCombat({rng:()=>.5});
   const old=g.state.world.publishedAdventure.oldLylek;
   assert.equal(old.phase,'post-hunters');assert.equal(old.lylekTriggered,true);
   assert.equal(old.huntersOutcome,outcome==='player_incapacitated'?'party-incapacitated':'retreated');
   assert.equal(old.hunterLoot,null);
 }
});

