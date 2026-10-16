import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {arriveAtNabat,advanceNabatMorning,resolveOldLylekRetreat} from './swe02-act1-fixture.mjs';

const adapter=getPublishedAdventureAdapter('long-arm-of-the-hutt');
class Repo{constructor(){this.state=createCampaign('P19-23');this.saves=0;}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++;}}
function setup(){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 let w=adapter.start(g.state.world).world;
 w=arriveAtNabat(adapter,w);
 w=advanceNabatMorning(adapter,w);
 w=resolveOldLylekRetreat(adapter,w,3);
 for(const id of ['reach-new-meen','confront-drombb-directly'])w=adapter.advance(w,id).world;
 w.publishedAdventure.flags.confirmedPartySize=3;g.state.world=w;repo.save(g.state);
 return {g,repo};
}
function reopen(repo){for(let i=0;i<3;i++){const g=new GameController(repo);assert.equal(adapter.status(g.state.world).active,true);}}
test('pp.19–23 controller imports actual bribe and safe receipts into durable lifecycle events',()=>{
 const {g,repo}=setup();
 const funded=structuredClone(g.state.player.character);funded.credits=5000;g.commit('CHARACTER_UPDATED',{character:funded});
 g.recordSWE02NewMeenLifecycle('compound_approach',{precautions:true});
 g.advancePublishedAdventure('new-meen-enter-cantina');
 const bribe=g.bribeSWE02NewMeenThug({credits:500,rng:()=>.4});
 assert.equal(g.state.world.publishedAdventure.newMeenLifecycle.cantina.bribeReceipts.length,1);
 assert.equal(g.state.world.publishedAdventure.newMeenLifecycle.events.at(-1).type,'THUG_BRIBE_IMPORTED');
 g.beginPublishedAdventureEncounter('laoth-drombb-cantina',{rng:()=>.9});
 const combat=structuredClone(g.state.combat);
 for(const a of combat.adversaries){a.wounds.current=a.wounds.threshold+1;a.status='incapacitated';}
 combat.encounter.outcome='adversaries_defeated';g.commit('COMBAT_ACTION',{combat});g.finishCombat({rng:()=>.9});
 assert.equal(g.state.world.publishedAdventure.newMeenLifecycle.stage,'VICTORY');
 const before=g.state.player.character.credits;
 const safe=g.openSWE02DrombbSafe({method:'blast',gmConfirmedQuick:true});
 assert.equal(safe.creditsGranted,2000);assert.equal(g.state.player.character.credits,before+2000);
 assert.equal(g.state.world.publishedAdventure.newMeenLifecycle.safe.creditsGranted,2000);
 reopen(repo);
 assert.equal(bribe.receipt.source.printedPage,23);
});
