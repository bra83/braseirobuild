import test from 'node:test';
import {readFileSync} from 'node:fs';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {jewelWingGuardInvestigations} from '../app/src/main/assets/vtt/js/adventure/jewelOfYavin.mjs';
import {recordResolvedTaskTime} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
const clone=structuredClone,adapter=getPublishedAdventureAdapter('jewel-of-yavin');
class Repo{constructor(){this.state=createCampaign('P193');this.saves=0;}load(){return clone(this.state)}save(s){this.state=clone(s);this.saves++}}
function game(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});return {g,repo};}
function bank(g){for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=1;i<=10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:`round${i}`});}
 g.advancePublishedAdventure('bank-operation');
 g.resolveJewelBankStep({actionId:'acquire',step:'acquire',target:'industrial_docks',rng:()=>.49});
 g.resolveJewelBankStep({actionId:'disable',step:'disable',rng:()=>.49});}
function dispatch(g,choice={wingGuardTeams:1},suffix='1'){
 const id=`slice${suffix}`,sid=`spend${suffix}`;
 g.resolveJewelBankStep({actionId:id,step:'slice',rng:()=>.6});
 g.spendJewelBankSymbols({actionId:sid,bankStepId:id,choice});
 return jewelWingGuardInvestigations(g.state.world).at(-1);
}
function advanceTime(g,minutes,id='task_test_patrol_wait'){
 const timed=recordResolvedTaskTime(g.state.world,{checkId:id,skillKey:'computadoras',minutes,baseMinutes:minutes});
 g.commit('WORLD_UPDATED',{world:timed.world});}
test('P193 official JoY p12 Wing Guard officer and sergeant profiles are combat-capable',()=>{
 const officer=createAdversary('bespin_wing_guard_officer',{count:3});
 assert.equal(officer.membersTotal,3);assert.equal(officer.wounds.threshold,15);
 assert.equal(officer.weapons[0].pierce,2);assert.equal(officer.weapons[0].damage,6);
 assert.equal(officer.source.productCode,'SWE09');
 const sergeant=createAdversary('bespin_wing_guard_sergeant');
 assert.equal(sergeant.kind,'rival');assert.equal(sergeant.skills.liderazgo,2);
 assert.equal(sergeant.weapons[1].disorient,2);
});
test('P193 no Wing Guard encounter before ETA and no fabricated dispatch',()=>{
 const {g}=game();bank(g);
 assert.deepEqual(jewelWingGuardInvestigations(g.state.world),[]);
 assert.equal(adapter.status(g.state.world).encounter,null);
 const team=dispatch(g);
 assert.equal(team.status,'en_route');assert.equal(team.remainingMinutes,240);
 assert.equal(adapter.status(g.state.world).encounter,null);
 assert.throws(()=>adapter.encounterSpec(g.state.world,team.id),/Nenhum encontro|não chegou/);
});
test('P193 ETA activates actual encounter with exactly as many Wing Guards as PCs',()=>{
 const {g}=game();bank(g);const team=dispatch(g);
 advanceTime(g,239);
 assert.equal(adapter.status(g.state.world).encounter,null);
 advanceTime(g,1,'task_test_patrol_last_minute');
 const ready=adapter.status(g.state.world).encounter;
 assert.equal(ready.id,team.id);assert.equal(ready.status,'ready');
 const spec=adapter.encounterSpec(g.state.world,team.id);
 assert.deepEqual(spec.adversaries,[{profileKey:'bespin_wing_guard_officer',count:1,range:'Médio'}]);
 const combat=g.beginPublishedAdventureEncounter(team.id,{adventureId:'jewel-of-yavin',rng:()=>.5});
 assert.equal(g.state.combat.active,true);
 assert.equal(g.state.combat.adversaries[0].profileKey,'bespin_wing_guard_officer');
 assert.equal(g.state.combat.adversaries[0].membersTotal,1);
 assert.equal(g.state.world.publishedAdventure.activeEncounterId,team.id);
 assert.equal(adapter.status(g.state.world).encounter.status,'active');
});
test('P193 cannot skip Wing Guard already at the bank',()=>{
 const {g}=game();bank(g);dispatch(g);advanceTime(g,240);
 const before=clone(g.state);
 assert.throws(()=>g.advancePublishedAdventure('skip-bank-transfer'),/Wing Guard|combate/);
 assert.deepEqual(g.state,before);
});
test('P193 encounter cannot be forged, resolved before starting or started twice',()=>{
 const {g}=game();bank(g);const team=dispatch(g);advanceTime(g,240);
 assert.throws(()=>adapter.markEncounterActive(g.state.world,'jewel-wing-0-forged'),/incompatível/);
 assert.throws(()=>adapter.recordEncounterOutcome(g.state.world,{encounterId:team.id,outcome:'ended'}),/não iniciado/);
 const started=adapter.markEncounterActive(g.state.world,team.id);
 assert.throws(()=>adapter.markEncounterActive(started,team.id),/incompatível/);
 const finished=adapter.recordEncounterOutcome(started,{encounterId:team.id,outcome:'negotiated'});
 assert.equal(finished.record.outcome,'negotiated');
 assert.equal(adapter.status(finished.world).wingGuardInvestigations[0].status,'resolved');
 assert.throws(()=>adapter.recordEncounterOutcome(finished.world,{encounterId:team.id,outcome:'ended'}),/não iniciado|resolvido/);
});
test('P193 Wing Guard combat can be finished and persists after three reopenings',()=>{
 const {g,repo}=game();bank(g);const team=dispatch(g);advanceTime(g,240);
 g.beginPublishedAdventureEncounter(team.id,{adventureId:'jewel-of-yavin',rng:()=>.5});
 const ended=g.finishCombat({rng:()=>.5});
 assert.equal(g.state.combat.active,false);
 assert.equal(ended.publishedAdventureOutcome.encounterId,team.id);
 assert.equal(adapter.status(g.state.world).wingGuardInvestigations[0].status,'resolved');
 for(let i=0;i<3;i++){const reopened=new GameController(repo);
  assert.equal(adapter.status(reopened.state.world).wingGuardInvestigations[0].status,'resolved');
  assert.equal(adapter.status(reopened.state.world).encounter,null);}
});
test('P193 if party left bank, dispatched Wing Guard investigates location instead of teleporting',()=>{
 const {g}=game();bank(g);const team=dispatch(g);
 g.advancePublishedAdventure('skip-bank-transfer');
 advanceTime(g,240);
 const st=adapter.status(g.state.world);
 assert.equal(st.wingGuardInvestigations[0].status,'investigating');
 assert.equal(st.encounter,null);
 g.advancePublishedAdventure('retreat-without-jewel');
 assert.equal(adapter.status(g.state.world).encounter.id,team.id);
});
test('P193 invalid forged Wing Guard encounter history rejected',()=>{
 const {g}=game();bank(g);const team=dispatch(g);advanceTime(g,240);
 for(const mutate of [w=>w.publishedAdventure.encounterHistory=[{encounterId:'jewel-wing-0-fake',outcome:'ended'}],
  w=>w.publishedAdventure.encounterHistory=[{encounterId:team.id,outcome:'ended'}],
  w=>w.publishedAdventure.encounterHistory=[{encounterId:team.id,outcome:'ended',teamIndex:0,dispatchId:team.dispatchId,guardCount:9,arrivalElapsedMinutes:team.arrivalElapsedMinutes,resolvedElapsedMinutes:240}]]){
   const w=clone(g.state.world);mutate(w);
   assert.throws(()=>adapter.status(w),/Wing Guard|Patrulha/);
 }
});
test('P193 multiple alarms remain independent patrols, no phantom sergeants',()=>{
 const {g}=game();bank(g);dispatch(g,{wingGuardTeams:1},'1');
 dispatch(g,{wingGuardTeams:1},'2');advanceTime(g,240);
 const teams=adapter.status(g.state.world).wingGuardInvestigations;
 assert.equal(teams.length,2);assert.notEqual(teams[0].id,teams[1].id);
 const started=adapter.markEncounterActive(g.state.world,teams[0].id);
 const done=adapter.recordEncounterOutcome(started,{encounterId:teams[0].id,outcome:'escaped'});
 assert.equal(adapter.status(done.world).encounter.id,teams[1].id);
});
test('P193 legacy adventure with no Wing Guard receipts retains normal progression',()=>{
 const {g}=game();bank(g);
 assert.deepEqual(adapter.status(g.state.world).wingGuardInvestigations,[]);
 g.advancePublishedAdventure('skip-bank-transfer');
 assert.equal(adapter.status(g.state.world).node.id,'vault-infiltration');
});

test('P193 captured signature upgrades generic Cloud City slicing but not unrelated Computers checks',()=>{
 const {g}=game();bank(g);
 const baseline=g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,codebreaking:true,rng:()=>.5});
 g.resolveJewelBankStep({actionId:'sliceSignature',step:'slice',rng:()=>.92});
 g.spendJewelBankSymbols({actionId:'spendSignature',bankStepId:'sliceSignature',choice:{signatureDespair:1}});
 const sliced=g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,codebreaking:true,rng:()=>.5});
 assert.equal(sliced.jewelElectronicSignatureUpgrade,true);
 assert.equal(sliced.pool.challenge,baseline.pool.challenge+1);
 assert.equal(sliced.pool.difficulty,baseline.pool.difficulty-1);
 const unrelated=g.resolveSkillCheck({skillKey:'computadoras',difficulty:2,rng:()=>.5});
 assert.equal(unrelated.jewelElectronicSignatureUpgrade,false);
 assert.equal(unrelated.pool.challenge,baseline.pool.challenge);
});
test('P193 signature also applies to a confirmed named Cloud City slicing system',()=>{
 const {g}=game();bank(g);
 g.resolveJewelBankStep({actionId:'sliceSignature',step:'slice',rng:()=>.92});
 g.spendJewelBankSymbols({actionId:'spendSignature',bankStepId:'sliceSignature',choice:{signatureDespair:1}});
 const check=g.resolveSkillCheck({skillKey:'computadoras',difficulty:3,slicingSystemId:'cloud_city_public_terminal',confirmedSlicingSystem:true,rng:()=>.5});
 assert.equal(check.jewelElectronicSignatureUpgrade,true);
 assert.equal(check.pool.challenge,1);
});

test('P193 cannot forge an active patrol before arrival or unknown dispatch ID',()=>{
 const {g}=game();bank(g);const team=dispatch(g);
 const early=clone(g.state.world);early.publishedAdventure.activeEncounterId=team.id;
 assert.throws(()=>adapter.status(early),/antes de chegar/);
 const unknown=clone(g.state.world);unknown.publishedAdventure.activeEncounterId='jewel-wing-99-fake';
 assert.throws(()=>adapter.status(unknown),/sem despacho/);
});
test('P193 Android PT-BR presents patrol arrival and generic combat button',()=>{
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const text of ['GUARDA ALADA · RASTREAMENTO DO BANCO','CHEGOU — encontro disponível','data-published-encounter'])
   assert.ok(ui.includes(text),text);
});
