import test from 'node:test';
import assert from 'node:assert/strict';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
const a=getPublishedAdventureAdapter('long-arm-of-the-hutt');
const start=()=>a.start({}).world;
const step=(w,id)=>a.advance(w,id).world;
const arrive=()=>step(step(step(step(start(),'dock-ryloth'),'head-to-geonosis'),'enter-canyon'),'prepare-tatooine-return');
const goto=(w,ids)=>ids.reduce(step,w);
const withParty=(w,n)=>{w=structuredClone(w);w.publishedAdventure.flags.confirmedPartySize=n;w.publishedAdventure.flags.palaceAdditionalHenchmenCount=2;w.publishedAdventure.flags.gmHuttProfileConfirmed=true;return w};
const combatProof=(w)=>a.encounterSpec(w,'teemo-palace-guards').adversaries.map((x,i)=>({id:`combat-enemy-${i}`,profileKey:x.profileKey,status:'incapacitated',wounds:999}));
const check=(w,index,success=true,netAdvantage=0)=>a.adjudicateCheck(w,{checkIndex:index,result:{success,netSuccess:success?1:0,netAdvantage}}).world;

test('SWE02 printed p41: elite Gamorrean guards do not inherit generic inaccurate profile',()=>{
 const p=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.laoth_elite_gamorrean_guard;
 assert.equal(p.kind,'minion');assert.deepEqual(p.characteristics,{fortaleza:4,agilidad:3,intelecto:1,astucia:1,voluntad:1,presencia:1});
 assert.equal(p.soak,4);assert.equal(p.woundsThreshold,8);assert.equal(p.weapons[0].damage,7);assert.equal(p.weapons[0].pierce,2);
 assert.equal(p.source.printedPage,41);assert.equal(p.source.authority,'OFFICIAL_PDF_TEXT_VERIFIED');
});
test('SWE02 p41: frontal stealth not offered; deception gate only opens with successful actual check',()=>{
 let w=step(arrive(),'approach-fortified-gate');
 assert.equal(a.status(w).node.id,'palace-front-gate');
 assert.equal(a.status(w).node.choices.some(x=>x.id==='palace-gate-debt-bluff'),false);
 assert.equal(a.currentChecks(w)[0].pool.difficulty,2);
 assert.equal(a.currentChecks(w)[0].skillKey,'engano');
 assert.throws(()=>step(w,'palace-gate-debt-bluff'),/inválida/);
 w=check(w,0,false);assert.throws(()=>step(w,'palace-gate-debt-bluff'),/inválida/);
 w=check(w,0,true);w=step(w,'palace-gate-debt-bluff');assert.equal(a.status(w).node.id,'palace-reception');
});
test('SWE02 p41: disguise requires Hard Deception and separate Average Charm',()=>{
 let w=step(arrive(),'approach-fortified-gate');
 assert.equal(a.currentChecks(w)[1].pool.difficulty,3);
 w=check(w,1,true);w=step(w,'palace-gate-disguise');
 assert.equal(a.currentChecks(w)[0].pool.difficulty,2);
 assert.throws(()=>step(w,'palace-disguise-approved'),/inválida/);
 w=check(w,0,true);w=step(w,'palace-disguise-approved');assert.equal(a.status(w).node.id,'palace-reception');
});
test('SWE02 pp44-45: workshop B1 evidence is acquired explicitly and persists three reopens',()=>{
 let w=goto(arrive(),['approach-fortified-gate']);w=check(w,0,true);w=goto(w,['palace-gate-debt-bluff','palace-enter-workshop']);
 assert.equal(w.publishedAdventure.flags.b1ReverseEngineeringEvidence,undefined);
 w=step(w,'palace-record-b1-worklog');assert.equal(w.publishedAdventure.flags.b1ReverseEngineeringEvidence,true);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(a.status(w).node.id,'palace-workshop-evidence');assert.equal(w.publishedAdventure.flags.b1ReverseEngineeringEvidence,true)}
});
test('SWE02 p44: ryll kitchen evidence requires successful Hard Perception, not merely entering room',()=>{
 let w=step(arrive(),'approach-fortified-gate');w=check(w,0,true);w=goto(w,['palace-gate-debt-bluff','palace-enter-kitchen']);
 assert.equal(a.currentChecks(w)[0].pool.difficulty,3);
 assert.throws(()=>step(w,'palace-kitchen-reveal-ryll'),/inválida/);
 w=check(w,0,false);assert.throws(()=>step(w,'palace-kitchen-reveal-ryll'),/inválida/);
 w=check(w,0,true);w=step(w,'palace-kitchen-reveal-ryll');assert.equal(w.publishedAdventure.flags.ryllTradeEvidence,true);
});
test('SWE02 p45: communications five official pools, setback, evidence, two-threat lockout',()=>{
 let w=step(arrive(),'approach-fortified-gate');w=check(w,0,true);w=goto(w,['palace-gate-debt-bluff','palace-enter-comms']);
 const specs=a.currentChecks(w);
 assert.deepEqual(specs.map(x=>x.pool.difficulty),[1,2,3,3,3]);
 assert.deepEqual(specs.map(x=>x.pool.setback),[1,1,1,1,1]);
 assert.ok(specs.every(x=>x.skillKey==='computadoras'&&x.source.printedPage===45));
 w=check(w,2,true,1);assert.equal(w.publishedAdventure.flags.ryllFinancialEvidence,true);
 w=check(w,3,true,0);assert.equal(w.publishedAdventure.flags.b1ReverseEngineeringEvidence,true);
 w=check(w,4,true,-2);assert.equal(w.publishedAdventure.flags.bithSpyEvidence,true);
 assert.equal(w.publishedAdventure.flags.palaceComputersLocked,true);
 assert.equal(a.currentChecks(w).length,0);
 assert.throws(()=>check(w,0,true),/Teste inválido/);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(a.currentChecks(w).length,0)}
});
test('SWE02 p45: throne encounter party count, elite guards and no fake victory without proof',()=>{
 let w=step(arrive(),'confront-teemo');
 assert.equal(a.encounterSpec(w,'teemo-palace-guards').requiresPartySizeConfirmation,true);
 w=withParty(w,3);
 const spec=a.encounterSpec(w,'teemo-palace-guards');
 assert.equal(spec.requiresPartySizeConfirmation,false);
 assert.equal(spec.requiresGMAdditionalGroupReview,false);assert.equal(spec.requiresOfficialHuttProfileReview,false);
 assert.deepEqual(spec.adversaries.map(x=>[x.profileKey,x.count]),[['laoth_elite_gamorrean_guard',2],['laoth_drombb_thug',3],['laoth_drombb_thug',2],['teemo_the_hutt',undefined]]);
 const active=a.markEncounterActive(w,'teemo-palace-guards');
 assert.throws(()=>step(active,'withdraw-from-palace'),/Encerre o combate/);
 assert.throws(()=>a.recordEncounterOutcome(active,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated'}),/grupos reais/);
 assert.throws(()=>a.recordEncounterOutcome(active,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated',adversaries:combatProof(active).map((x,i)=>i===2?{...x,status:'active'}:x)}),/não foi derrotado/);
 assert.equal(active.publishedAdventure.flags.teemoDefeated,undefined);
});
test('SWE02 pp45-46: Teemo defeated only by real group proof, rollback on tamper, no fabricated title',()=>{
 let w=withParty(step(arrive(),'confront-teemo'),4);
 w=a.markEncounterActive(w,'teemo-palace-guards');
 w=a.recordEncounterOutcome(w,{encounterId:'teemo-palace-guards',outcome:'adversaries_defeated',adversaries:combatProof(w)}).world;
 assert.equal(w.publishedAdventure.flags.teemoDefeated,true);
 w=step(w,'teemo-resolved');assert.equal(w.publishedAdventure.status,'COMPLETED');
 assert.match(a.status(w).node.summary,/Jabba/);
 for(let i=0;i<3;i++){w=JSON.parse(JSON.stringify(w));assert.equal(a.status(w).node.id,'laoth-epilogue')}
 const forged=structuredClone(w);forged.publishedAdventure.encounterHistory=[];
 assert.throws(()=>a.status(forged),/recibo íntegro/);
 assert.equal(a.status(w).node.id,'laoth-epilogue');
});
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
class P236Repo {constructor(){this.state=createCampaign('SWE02 P236 Teemo');this.saves=0}load(){return structuredClone(this.state)}save(s){this.state=structuredClone(s);this.saves++}}
function p236Controller(){
 const repo=new P236Repo(),g=new GameController(repo);
 g.loadStarterCharacter('cael');g.startSession({playerCount:3,rng:()=>.9});
 g.startPublishedAdventureSession('long-arm-of-the-hutt',{rng:()=>.9});
 for(const id of ['dock-ryloth','head-to-geonosis','enter-canyon','prepare-tatooine-return'])g.advancePublishedAdventure(id);
 return {g,repo};
}
test('P236 actual controller: palace checks use deterministic dice, persist and do not grant unearned access',()=>{
 const {g,repo}=p236Controller();g.advancePublishedAdventure('approach-fortified-gate');
 assert.equal(g.publishedAdventureStatus('long-arm-of-the-hutt').node.id,'palace-front-gate');
 const before=repo.saves;
 const r=g.resolvePublishedAdventureCheck(0,{rng:()=>.9});
 assert.equal(r.adjudication.source.printedPage,41);
 assert.equal(r.check.success,r.adjudication.success);
 assert.ok(repo.saves>before);
 for(let i=0;i<3;i++){
   const reopened=new GameController(repo);
   assert.equal(reopened.state.world.publishedAdventure.checkHistory.at(-1).success,r.check.success);
   assert.equal(reopened.publishedAdventureStatus('long-arm-of-the-hutt').node.choices.some(x=>x.id==='palace-gate-debt-bluff'),r.check.success);
 }
});
test('P236 actual controller: Teemo combat fails closed without reviewed extra group and Hutt source',()=>{
 const {g,repo}=p236Controller();g.advancePublishedAdventure('confront-teemo');
 assert.throws(()=>g.beginPublishedAdventureEncounter('teemo-palace-guards',{rng:()=>.9}),/confirmação do segundo grupo/);
 assert.equal(g.state.combat.active,false);
 const r=g.beginPublishedAdventureEncounter('teemo-palace-guards',{rng:()=>.9,palaceAdditionalHenchmenCount:2,gmHuttProfileConfirmed:true});
 assert.deepEqual(r.encounter.adversaries.map(x=>x.count),[2,3,2,undefined]);
 assert.deepEqual(g.state.combat.adversaries.map(x=>x.profileKey),['laoth_elite_gamorrean_guard','laoth_drombb_thug','laoth_drombb_thug','teemo_the_hutt']);
 assert.equal(g.state.world.publishedAdventure.flags.confirmedPartySize,3);
 for(let i=0;i<3;i++){
   const reopened=new GameController(repo);
   assert.equal(reopened.state.combat.publishedAdventureContext.encounterId,'teemo-palace-guards');
   assert.equal(reopened.state.combat.adversaries.length,4);
 }
});
