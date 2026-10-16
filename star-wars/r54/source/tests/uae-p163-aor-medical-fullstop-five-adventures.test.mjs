/** R54 P163 — AoR medical, full-stop, Well Rounded and five UAE campaigns. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {purchaseTalent,sourceBackedTalentRank,eligibleTalents} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {recoverInBactaTank,recoverUnderMedicalSupervision,resolveStimApplication,expireStimApplication,resolveMedicineCheck} from '../app/src/main/assets/vtt/js/rules/recovery.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const clone=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
class Repo{constructor(){this.saved=createCampaign('P163');this.fail=false;this.writes=0}load(){return clone(this.saved)}save(s){if(this.fail)throw Error('P163 storage failure');this.saved=clone(s);this.writes++}}
function game(bought=[]){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=clone(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];c.skills.medicina=4;c.characteristics.intelecto=4;
 c.gear=[{id:'medpac',name:'Medpac',quantity:1},{id:'stimpack_injector',name:'Injetor de estimulantes',quantity:2}];
 for(const [name,rank] of bought){const found=nodes.filter(n=>n.originalName===name).slice(0,rank);assert.equal(found.length,rank,name);for(const n of found){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source});}}
 g.setCharacter(c);return {g,repo};
}
function persisted(repo,fn){for(let i=0;i<3;i++){const g=new GameController(repo);assert.equal(validateState(g.state),true);fn(g);}}
const fullMedical=[['APLICACIÓN DE ESTIMULANTES',1],['APLICACIÓN DE ESTIMULANTES MEJORADA',1],['APLICACIÓN DE ESTIMULANTES SUPREMA',1]];
test('P163: 8 AoR positions wired without source/display forgery',()=>{
 assert.equal(aorTalentCoverage().positions,360);assert.ok(aorTalentCoverage().mechanicalPositions>=295);
 for(const [name,rank] of [['ESPECIALISTA EN BACTA',1],['MÉDICO EXPERTO',1],['POLIFACÉTICO',1],['PARADA EN SECO',2],...fullMedical]){
  assert.equal(aorTalentCatalog().filter(x=>x.originalName===name&&x.mechanicallyActive).length,rank,name);
 }
 const x=game();const c=clone(x.g.state.player.character);c.talents.push('Médico especialista','Polifacético');c.talentPurchases.push({nodeId:'ace:pilot:r1c4',originalName:'MÉDICO EXPERTO'});
 x.g.setCharacter(c);assert.equal(sourceBackedTalentRank(x.g.state.player.character,'MASDOC'),0);
});
test('P163: Well Rounded chooses two distinct, permanent career skills with purchase provenance',()=>{
 const x=game(),c=clone(x.g.state.player.character),node=nodes.find(n=>n.originalName==='POLIFACÉTICO');
 c.creation={...c.creation,careerKey:'soldier',specializationKey:'medic'};c.specializations=['medic'];c.xp.available=100;const prerequisite=nodes.find(n=>n.id==='soldier:medic:r2c1');c.talentPurchases.push({nodeId:prerequisite.id,originalName:prerequisite.originalName,name:prerequisite.name,source:prerequisite.source});
 const options={careerKey:'soldier',specializationKey:'medic'};
 assert.throws(()=>purchaseTalent(c,node.id,{...options,skillKeys:['medicina','medicina']}),/distintas/);
 assert.throws(()=>purchaseTalent(c,node.id,{...options,skillKeys:['hack','medicina']}),/válidas/);
 assert.throws(()=>purchaseTalent(c,node.id,{...options,skillKeys:['sigilo']}),/exatamente/);
 const eligible=eligibleTalents(c,options).find(n=>n.id===node.id);assert.equal(eligible.choiceType,'twoSkills');
 const r=purchaseTalent(c,node.id,{...options,skillKeys:['sigilo','computadoras']});
 assert.equal(r.character.careerSkills.includes('sigilo'),true);assert.equal(r.character.careerSkills.includes('computadoras'),true);
 assert.deepEqual(r.character.talentPurchases.at(-1).choice,['sigilo','computadoras']);assert.equal(r.character.xp.available,c.xp.available-node.cost);
});
test('P163: Bacta Specialist increases actual healing, not empty or unsupervised time',()=>{
 const x=game([['ESPECIALISTA EN BACTA',1]]),c=clone(x.g.state.player.character);c.derived.wounds.current=5;
 assert.equal(recoverInBactaTank(c,2).woundsRecovered,1);
 const healed=recoverInBactaTank(c,2,{supervisor:c});assert.equal(healed.woundsRecovered,2);assert.equal(healed.specialistBonus,1);assert.equal(c.derived.wounds.current,5);
 assert.equal(recoverInBactaTank(c,0,{supervisor:c}).woundsRecovered,0);
 const long=recoverUnderMedicalSupervision(c,{supervisor:c});assert.equal(long.woundsRecovered,2);
});
test('P163: Bacta recovery is explicit, persistent and rolled back on storage failure',()=>{
 const {g,repo}=game([['ESPECIALISTA EN BACTA',1]]);const c=clone(g.state.player.character);c.derived.wounds.current=6;g.setCharacter(c);
 assert.throws(()=>g.recoverInBacta({hours:2}),/Confirme/);
 const before=clone(g.state);repo.fail=true;assert.throws(()=>g.recoverInBacta({hours:2,facilityConfirmed:true}),/storage failure/);
 // P163 controller must not leave in-memory mutation on failed save.
 assert.deepEqual(repo.saved,before);repo.fail=false;
 const r=g.recoverInBacta({hours:2,facilityConfirmed:true});assert.equal(r.woundsRecovered,2);
 persisted(repo,x=>assert.equal(x.state.player.character.derived.wounds.current,4));
});
test('P163: base Stim Application spends an action, boosts exactly one stat and costs 4 strain',()=>{
 const {g,repo}=game([['APLICACIÓN DE ESTIMULANTES',1]]);g.beginCombat();
 const c=clone(g.state.player.character);const old=c.characteristics.agilidad,oldStrain=c.derived.strain.current;
 const r=g.applyStimApplication({characteristicKey:'agilidad',rng:()=>0.999});assert.equal(r.check.success,true);assert.equal(r.strainSuffered,4);
 assert.equal(g.state.player.character.characteristics.agilidad,old+1);assert.equal(g.state.player.character.derived.strain.current,oldStrain+4);
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 persisted(repo,x=>assert.equal(x.state.player.character.characteristics.agilidad,old+1));
 assert.throws(()=>g.applyStimApplication({characteristicKey:'agilidad',rng:()=>0.999}),/ação|Ação|já/i);
});
test('P163: Improved Stim raises difficulty and reduces strain to one',()=>{
 const {g}=game([...fullMedical.slice(0,2)]),c=g.state.player.character;
 const r=resolveStimApplication(c,c,{characteristicKey:'fortaleza',improved:true,rng:()=>0.999});
 assert.equal(r.difficulty,3);assert.equal(r.strainSuffered,1);assert.equal(r.character.characteristics.fortaleza,c.characteristics.fortaleza+1);
});
test('P163: Supreme Stim spends actual Triumph and applies a different extra stat',()=>{
 const {g}=game(fullMedical),c=g.state.player.character;
 const r=resolveStimApplication(c,c,{characteristicKey:'agilidad',additionalCharacteristics:['voluntad'],rng:()=>0.999});
 assert.ok(r.check.triumph>=1);assert.deepEqual(r.boosted,['agilidad','voluntad']);
 assert.equal(r.character.characteristics.voluntad,c.characteristics.voluntad+1);
 const failed=resolveStimApplication(c,c,{characteristicKey:'agilidad',additionalCharacteristics:['voluntad'],rng:()=>0});assert.deepEqual(failed.boosted,[]);assert.equal(failed.strainSuffered,0);
});
test('P163: Stim prevents repeat stat and equipment/alias forgery',()=>{
 const {g}=game(fullMedical),c=g.state.player.character;
 const one=resolveStimApplication(c,c,{characteristicKey:'agilidad',rng:()=>0.999}).character;
 assert.throws(()=>resolveStimApplication(one,one,{characteristicKey:'agilidad',rng:()=>0.999}),/já recebeu/);
 const none=clone(c);none.gear=[];assert.throws(()=>resolveStimApplication(none,none,{characteristicKey:'agilidad'}),/medpac/);
 const fake=clone(c);fake.talentPurchases=[];assert.throws(()=>resolveStimApplication(fake,fake,{characteristicKey:'agilidad'}),/não foi adquirida/);
});
test('P163: Stim failures never grant stat or strain, encounter end removes boosts',()=>{
 const {g}=game([['APLICACIÓN DE ESTIMULANTES',1]]),c=g.state.player.character;
 const failed=resolveStimApplication(c,c,{characteristicKey:'agilidad',rng:()=>0});assert.equal(failed.check.success,false);assert.deepEqual(failed.character,c);
 const boosted=resolveStimApplication(c,c,{characteristicKey:'agilidad',rng:()=>0.999}).character;
 assert.deepEqual(expireStimApplication(boosted).characteristics,c.characteristics);
 g.beginCombat();g.applyStimApplication({characteristicKey:'agilidad',rng:()=>0.999});g.finishCombat({rng:()=>0});
 assert.equal(g.state.player.character.characteristics.agilidad,c.characteristics.agilidad);
});
test('P163: Master Doctor applies real 2 strain and -1 Medicine difficulty',()=>{
 const {g,repo}=game([['MÉDICO EXPERTO',1]]);const c=clone(g.state.player.character);c.derived.wounds.current=4;g.setCharacter(c);
 const base=resolveMedicineCheck(c,c,{rng:()=>0.999});const r=g.useMedicine({useMasterDoctor:true,rng:()=>0.999});
 assert.equal(r.masterDoctorUsed,true);assert.equal(r.difficulty,base.difficulty-1);assert.equal(g.state.player.character.derived.strain.current,2);
 persisted(repo,x=>assert.equal(x.state.player.character.derived.strain.current,2));
});
test('P163: Master Doctor cannot be invoked by display label',()=>{
 const {g}=game();const c=clone(g.state.player.character);c.talents.push('Médico especialista');g.setCharacter(c);
 assert.throws(()=>g.useMedicine({useMasterDoctor:true}),/não foi adquirido/);
});
test('P163: AoR Full Stop gated by actual purchased node, costs speed in system strain',()=>{
 const {g,repo}=game([['PARADA EN SECO',1]]);g.setCampaignMode('AOR');g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const v=clone(g.state.vehicles);v.vehicles[v.activeVehicleId].speed=4;g.commit('VEHICLES_UPDATED',{vehicles:v});
 const r=g.useVehiclePilotManeuver('stop_short');assert.equal(r.vehicle.speed,0);assert.equal(r.vehicle.systemStrain,4);
 persisted(repo,x=>assert.equal(x.activeVehicle().speed,0));
});
test('P163: AoR Full Stop rejects unowned nodes before charging maneuver',()=>{
 const {g,repo}=game();g.setCampaignMode('AOR');g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const before=clone(g.state),writes=repo.writes;assert.throws(()=>g.useVehiclePilotManeuver('stop_short'),/exige talento AoR/);
 assert.deepEqual(g.state,before);assert.equal(repo.writes,writes);
});
test('P163: successful Supreme application refuses invented Triumph',()=>{
 const {g}=game(fullMedical),c=g.state.player.character;
 assert.throws(()=>resolveStimApplication(c,c,{characteristicKey:'agilidad',additionalCharacteristics:['voluntad'],rng:()=>0.4}),/Triunfo real/);
 assert.equal(c.characteristics.agilidad,2);
});
test('P163: Stim atomicity restores character and action after storage failure',()=>{
 const {g,repo}=game(fullMedical);g.beginCombat();const before=clone(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.applyStimApplication({characteristicKey:'agilidad',rng:()=>0.999}),/storage failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P163: Medicine atomicity restores health and strain after storage failure',()=>{
 const {g,repo}=game([['MÉDICO EXPERTO',1]]),c=clone(g.state.player.character);c.derived.wounds.current=5;g.setCharacter(c);
 const before=clone(g.state),writes=repo.writes;repo.fail=true;
 assert.throws(()=>g.useMedicine({useMasterDoctor:true,rng:()=>0.999}),/storage failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P163: Well Rounded persists after three reopenings and cannot be purchased twice',()=>{
 const {g,repo}=game(),c=clone(g.state.player.character),node=nodes.find(n=>n.originalName==='POLIFACÉTICO');
 c.creation={...c.creation,careerKey:'soldier',specializationKey:'medic'};c.specializations=['medic'];c.xp.available=100;
 const prereq=nodes.find(n=>n.id==='soldier:medic:r2c1');c.talentPurchases.push({nodeId:prereq.id,originalName:prereq.originalName,name:prereq.name,source:prereq.source});g.setCharacter(c);
 const options={careerKey:'soldier',specializationKey:'medic',skillKeys:['sigilo','computadoras']};g.buyTalent(node.id,options);
 persisted(repo,x=>{assert.ok(x.state.player.character.careerSkills.includes('computadoras'));assert.throws(()=>x.buyTalent(node.id,options),/já foi adquirido/)});
});
test('P163: Stim application does not consume access-only medical equipment',()=>{
 const {g}=game(fullMedical),c=g.state.player.character;
 const qty=c.gear.map(x=>x.quantity);
 const result=resolveStimApplication(c,c,{characteristicKey:'agilidad',rng:()=>0.999});
 assert.deepEqual(result.character.gear.map(x=>x.quantity),qty);
});
test('P163: Master Doctor also powers contextual Medicine check without forged purchase',()=>{
 const {g}=game([['MÉDICO EXPERTO',1]]);const result=g.resolveSkillCheck({skillKey:'medicina',difficulty:3,useMasterDoctor:true,rng:()=>0.999});
 assert.equal(result.activeSkillTalents?.some(x=>x.kind==='doctor'),true);
 assert.equal(g.state.player.character.derived.strain.current,2);
});
test('P163: Android bridge routes new medical actions and bundle contains new API',()=>{
 const {repo}=game([['ESPECIALISTA EN BACTA',1]]),bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.recoverInBacta({hours:2,facilityConfirmed:true}).ok,true);
 assert.equal(bridge.applyStimApplication({characteristicKey:'agilidad'}).ok,false);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const token of ['dataset.p163Medical','data-p163-apply','data-p163-bacta','data-p163-skill-a','skillKeys:sa&&sb'])assert.ok(ui.includes(token),token);
 for(const token of ['applyStimApplication','recoverInBacta'])assert.ok(bundle.includes(token),token);assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=80,'O bundle não pode perder módulos da base P172');
});
for(const {id} of publishedAdventureCatalog())test(`P163: ${id} UAE trigger/objective/clock/consequence, rollback and 3 reloads`,()=>{
 const mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 mod.initialObjectives.p163={title:'P163 hospital de campanha',type:'SECONDARY',target:2};mod.initialClocks.p163={value:0,max:2,status:'ACTIVE'};
 mod.triggers.push({id:'p163-medical-trigger',event:'P163_FIELD_MEDIC',maxFires:2,effects:[{type:'progress-objective',objectiveId:'p163',delta:1},{type:'advance-clock',clockId:'p163',delta:1}],consequence:{id:'p163-medical-outcome',description:'Evacuação estabilizada'}});
 const runtime=new AdventureRuntime(mod);for(let i=0;i<2;i++)runtime.commitAction(`${id}:p163:${i}`,[{type:'event',eventType:'P163_FIELD_MEDIC'}]);
 const before=runtime.getState();assert.equal(before.objectives.p163.status,'COMPLETED');assert.equal(before.clocks.p163.status,'COMPLETED');assert.ok(before.consequences.some(x=>x.id==='p163-medical-outcome'));
 assert.equal(validateAdventureCheckpoint(mod,before),true);
 assert.throws(()=>runtime.commitAction(`${id}:p163:invalid`,[{type:'progress-objective',objectiveId:'nonexistent',delta:1}]));assert.deepEqual(runtime.getState(),before);
 for(let i=0;i<3;i++){const loaded=AdventureRuntime.restore(mod,runtime.serialize());assert.equal(loaded.commitAction(`${id}:p163:1`,[{type:'event',eventType:'P163_FIELD_MEDIC'}]).applied,false);assert.deepEqual(loaded.getState(),before);}
});
