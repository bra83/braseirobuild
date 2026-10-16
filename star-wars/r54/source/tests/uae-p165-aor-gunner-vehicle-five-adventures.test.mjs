/** R54 P165: source-backed Gunner talents, persistent Advantage spends and UAE campaigns. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {validateVehicle,vehicleStatus} from '../app/src/main/assets/vtt/js/rules/vehicles.mjs';
import {sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
class Repo {constructor(){this.saved=createCampaign('P165');this.fail=false;this.writes=0;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P165 disk failure');this.saved=copy(s);this.writes++;}}
function game({overwhelm=0,debilitating=0,skill=0,target='xwing_t65b'}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];c.characteristics.agilidad=2;c.skills.artilleria=skill;
 for(const [name,count] of [['ABRUMAR DEFENSAS',overwhelm],['DISPARO DEBILITANTE',debilitating]]){
  const selected=nodes.filter(n=>n.originalName===name).slice(0,count);assert.equal(selected.length,count);
  for(const n of selected){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:n.originalName,name:n.name,source:n.source});}
 }
 g.setCharacter(c);g.setCampaignMode('AOR');g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:target,range:'Bocajarro'});
 return {g,repo};
}
function miss(){let i=0;return ()=>[.999,.999,0,0][i++%4];}
function hit(){let i=0;return ()=>[.999,.999,0,0,0][i++%5];}
function target(g){return g.state.vehicles.vehicles[g.state.vehicles.combat.targetVehicleId];}
function restore(repo,check){for(let i=0;i<3;i++){const g=new GameController(repo);assert.equal(validateState(g.state),true);check(g);}}
test('P165: three AoR Gunner positions newly executable; full 360 tree still available',()=>{
 assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=298);
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='ABRUMAR DEFENSAS'&&n.mechanicallyActive).length,2);
 assert.equal(aorTalentCatalog().filter(n=>n.originalName==='DISPARO DEBILITANTE'&&n.mechanicallyActive).length,1);
});
test('P165: failed attack offers Overwhelm ranks, costs actual Advantage, lowers zone defense',()=>{
 const {g,repo}=game({overwhelm:2});const a=g.resolveVehicleAttack({rng:miss()});assert.equal(a.hit,false);assert.ok(a.check.netAdvantage>=2);
 assert.equal(a.pendingSpend.qualities.find(x=>x.key==='aor_overwhelm').rank,2);
 const result=g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'});assert.equal(result.effect.zone,'proa');assert.equal(result.effect.defenseAfter,0);assert.equal(result.remainingAdvantage,a.check.netAdvantage-2);
 assert.equal(vehicleStatus(target(g),{currentRound:1}).defense.proa,0);
 restore(repo,x=>assert.equal(vehicleStatus(target(x),{currentRound:1}).defense.proa,0));
 assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'}),/zerada|insuficientes/);
});
test('P165: Overwhelm is per-zone and persists across rounds, not after combat',()=>{
 const {g}=game({overwhelm:2,target:'vt49_decimator'});g.resolveVehicleAttack({rng:miss()});g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'});
 assert.equal(vehicleStatus(target(g),{currentRound:1}).defense.proa,1);assert.equal(vehicleStatus(target(g),{currentRound:1}).defense.popa,2);
 g.nextVehicleRound();assert.equal(vehicleStatus(target(g),{currentRound:2}).defense.proa,1);
 const targetId=g.state.vehicles.combat.targetVehicleId;g.endVehicleCombat();assert.equal(vehicleStatus(g.state.vehicles.vehicles[targetId]).defense.proa,2);
});
test('P165: Overwhelm cannot spend more ranks, or reuse Advantages on another effect',()=>{
 const {g}=game({overwhelm:1,target:'vt49_decimator'});g.resolveVehicleAttack({rng:miss()});g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'});
 assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'}),/graduações/);
});
test('P165: Debilitating Shot requires a hit and 2 real Advantage, reduces max speed through next round',()=>{
 const {g,repo}=game({debilitating:1,skill:1});const v=copy(g.state.vehicles);v.vehicles[v.combat.targetVehicleId].speed=5;g.commit('VEHICLES_UPDATED',{vehicles:v});
 const a=g.resolveVehicleAttack({rng:hit()});assert.equal(a.hit,true);assert.ok(a.check.netAdvantage>=2);
 const result=g.activatePendingVehicleQuality({qualityKey:'aor_debilitating'});assert.equal(result.effect.untilRound,2);
 assert.equal(vehicleStatus(target(g),{currentRound:1}).effectiveSpeedMax,4);assert.equal(target(g).speed,4);
 restore(repo,x=>assert.equal(vehicleStatus(target(x),{currentRound:1}).effectiveSpeedMax,4));
 g.nextVehicleRound();assert.equal(vehicleStatus(target(g),{currentRound:2}).effectiveSpeedMax,4);
 g.nextVehicleRound();assert.equal(vehicleStatus(target(g),{currentRound:3}).effectiveSpeedMax,5);
});
test('P165: speed reduction never reduces a speed-1 target below one',()=>{
 const {g}=game({debilitating:1,skill:1,target:'digger_crawler'});const a=g.resolveVehicleAttack({rng:hit()});assert.equal(a.hit,true);g.activatePendingVehicleQuality({qualityKey:'aor_debilitating'});
 assert.equal(vehicleStatus(target(g),{currentRound:1}).effectiveSpeedMax,1);
});
test('P165: display-only or forged AoR talent cannot authorize vehicle spend',()=>{
 const {g}=game();const c=copy(g.state.player.character);c.talents=['Sobrecarregar defesas','Disparo debilitante'];c.talentPurchases=[{nodeId:'ace:pilot:r1c4',originalName:'ABRUMAR DEFENSAS'}];g.setCharacter(c);
 assert.equal(sourceBackedTalentRank(g.state.player.character,'OVERWHELMDEF'),0);
 assert.equal(sourceBackedTalentRank(g.state.player.character,'DEBILSHOT'),0);
 const a=g.resolveVehicleAttack({rng:miss()});assert.ok(!a.pendingSpend.qualities.some(x=>x.key.startsWith('aor_')));
 assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'}),/indisponível/);
});
test('P165: removing purchased talent after roll prevents pending spend',()=>{
 const {g}=game({overwhelm:1});g.resolveVehicleAttack({rng:miss()});const c=copy(g.state.player.character);c.talentPurchases=[];g.setCharacter(c);
 const before=copy(g.state);assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'}),/não autenticado/);assert.deepEqual(g.state,before);
});
test('P165: transaction rollback leaves target and Advantage untouched on save failure',()=>{
 const {g,repo}=game({overwhelm:1});g.resolveVehicleAttack({rng:miss()});const before=copy(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.activatePendingVehicleQuality({qualityKey:'aor_overwhelm'}),/disk failure/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P165: invalid imported AoR effects rejected, without sanitizing malicious values',()=>{
 const {g}=game();for(const effects of [{aorOverwhelmedDefense:{proa:-1}},{aorOverwhelmedDefense:{constructor:2}},{aorDebilitatedSpeed:{untilRound:Infinity}},{aorDebilitatedSpeed:{untilRound:-1}}]){
  const v=copy(target(g));v.roundEffects={...v.roundEffects,...effects};assert.throws(()=>validateVehicle(v),/AoR inválid/);
 }
});
test('P165: vehicle attack itself is atomic when disk save fails',()=>{
 const {g,repo}=game({overwhelm:2});const before=copy(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.resolveVehicleAttack({rng:miss()}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P165: Android bridge exposes effective speed and defense after a persisted Gunner spend',()=>{
 const {g,repo}=game({debilitating:1,skill:1});g.resolveVehicleAttack({rng:hit()});g.activatePendingVehicleQuality({qualityKey:'aor_debilitating'});
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});const response=bridge.vehicleRuntimeStatus();
 assert.equal(response.ok,true);assert.equal(response.data.targetStatus.effectiveSpeedMax,4);
 assert.equal(response.data.targetStatus.aorDebilitatedSpeedActive,true);
});
test('P165: Android dynamic quality buttons and offline bundle include integrated spend paths',()=>{
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const token of ['P165 · QUALIDADES','aor_overwhelm','aor_debilitating','data-p98-quality'])assert.ok(ui.includes(token)||bundle.includes(token),token);
 assert.ok(bundle.includes('OVERWHELMDEF'));assert.ok(bundle.includes('DEBILSHOT'));assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=80,'O bundle não pode perder módulos da base P172');
});
for(const {id} of publishedAdventureCatalog())test(`P165: ${id} UAE trigger, consequences, clocks, objectives, rollback, 3 restores`,()=>{
 const mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 mod.initialObjectives.p165={title:'P165 patrulha interceptada',type:'SECONDARY',target:2};
 mod.initialClocks.p165={value:0,max:2,status:'ACTIVE'};
 mod.triggers.push({id:'p165-vehicle-intercept',event:'P165_VEHICLE_EVENT',maxFires:2,effects:[{type:'progress-objective',objectiveId:'p165',delta:1},{type:'advance-clock',clockId:'p165',delta:1}],consequence:{id:'p165-vehicle-outcome',description:'Intercepção encerrada'}});
 const runtime=new AdventureRuntime(mod);
 for(let i=0;i<2;i++)runtime.commitAction(`${id}:p165:${i}`,[{type:'event',eventType:'P165_VEHICLE_EVENT'}]);
 const before=runtime.getState();assert.equal(before.objectives.p165.status,'COMPLETED');assert.equal(before.clocks.p165.status,'COMPLETED');assert.equal(validateAdventureCheckpoint(mod,before),true);
 assert.throws(()=>runtime.commitAction(`${id}:p165:bad`,[{type:'progress-objective',objectiveId:'not-in-book',delta:1}]));assert.deepEqual(runtime.getState(),before);
 for(let i=0;i<3;i++){const loaded=AdventureRuntime.restore(mod,runtime.serialize());assert.equal(loaded.commitAction(`${id}:p165:1`,[{type:'event',eventType:'P165_VEHICLE_EVENT'}]).applied,false);assert.deepEqual(loaded.getState(),before);}
});
