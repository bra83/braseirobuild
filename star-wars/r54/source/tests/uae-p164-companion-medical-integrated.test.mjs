/** P164 — companion medical persistence, UI bridge, and five UAE adventures. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone;
class Repo {constructor(){this.saved=createCampaign('P164');this.fail=false;this.writes=0;}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('falha de armazenamento P164');this.saved=copy(s);this.writes++;}}
const nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
function game(names=[]){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];c.skills.medicina=5;c.characteristics.intelecto=5;c.gear=[{id:'medpac',quantity:1},{id:'stimpack_injector',quantity:2}];for(const name of names){const node=nodes.find(n=>n.originalName===name);assert.ok(node,name);c.talents.push(node.name);c.talentPurchases.push({nodeId:node.id,originalName:node.originalName,name:node.name,source:node.source});}g.setCharacter(c);return {g,repo};}
function reopened(repo,check){for(let i=0;i<3;i++){const g=new GameController(repo);assert.equal(validateState(g.state),true);check(g);}}
test('P164: player-authored companion has explicit non-official provenance and persisted health',()=>{
 const {g,repo}=game();const m=g.addCompanion({name:'Piloto Rebelde',woundsThreshold:15,strainThreshold:12});assert.equal(m.character.source.official,false);assert.equal(m.character.derived.wounds.threshold,15);assert.ok(m.id);
 reopened(repo,x=>assert.deepEqual(x.companionRoster(),[m]));
});
test('P164: companion roster bounds, name, thresholds, ID and duplicate validation',()=>{
 const {g}=game();for(const opts of [{name:''},{name:'x',woundsThreshold:0},{name:'x',strainThreshold:NaN},{name:'x',woundsThreshold:1.5}])assert.throws(()=>g.addCompanion(opts));
 for(let i=0;i<8;i++)g.addCompanion({name:`Aliado ${i}`});assert.throws(()=>g.addCompanion({name:'Nono'}),/oito/);
 const invalid=copy(g.state);invalid.group.companions[1].id=invalid.group.companions[0].id;assert.throws(()=>validateState(invalid),/duplicado/);
});
test('P164: damage to a companion is real, saved, never applied to player',()=>{
 const {g,repo}=game(),m=g.addCompanion({name:'Técnico'});const before=copy(g.state.player.character);const r=g.applyCompanionDamage({companionId:m.id,damage:4});assert.equal(r.companionId,m.id);assert.equal(g.companionRoster()[0].character.derived.wounds.current,4);assert.deepEqual(g.state.player.character,before);
 reopened(repo,x=>assert.equal(x.companionRoster()[0].character.derived.wounds.current,4));
});
test('P164: foreign, null and unsafe target IDs cannot heal arbitrary actors',()=>{
 const {g}=game();assert.throws(()=>g.useMedicine({targetCompanionId:'unknown'}),/não encontrado/);assert.throws(()=>g.recoverInBacta({targetCompanionId:'__proto__',facilityConfirmed:true,hours:2}),/não encontrado/);
 assert.throws(()=>g.applyCompanionDamage({companionId:null,damage:4}),/Selecione/);
});
test('P164: actual Medicine dice heal companion, self treatment penalties do not apply',()=>{
 const {g,repo}=game();const m=g.addCompanion({name:'Batedora'});g.applyCompanionDamage({companionId:m.id,damage:7});const playerBefore=copy(g.state.player.character);
 const result=g.useMedicine({targetCompanionId:m.id,rng:()=>0.999});assert.equal(result.selfTreatment,false);assert.equal(result.targetCompanionId,m.id);assert.equal(result.check.success,true);assert.ok(result.healed>0);assert.equal(g.companionRoster()[0].character.derived.wounds.current,7-result.healed);assert.deepEqual(g.state.player.character,playerBefore);
 reopened(repo,x=>assert.equal(x.companionRoster()[0].character.derived.wounds.current,7-result.healed));
});
test('P164: Master Doctor costs healer strain, not patient strain',()=>{
 const {g,repo}=game(['MÉDICO EXPERTO']);const m=g.addCompanion({name:'Guarda'});g.applyCompanionDamage({companionId:m.id,damage:5});const r=g.useMedicine({targetCompanionId:m.id,useMasterDoctor:true,rng:()=>0.999});assert.equal(r.masterDoctorUsed,true);assert.equal(g.state.player.character.derived.strain.current,2);assert.equal(g.companionRoster()[0].character.derived.strain.current,0);
 reopened(repo,x=>assert.equal(x.state.player.character.derived.strain.current,2));
});
test('P164: Medicine action on companion consumes combat action and prevents repeat',()=>{
 const {g}=game(),m=g.addCompanion({name:'Artilheira'});g.applyCompanionDamage({companionId:m.id,damage:5});g.beginCombat();g.useMedicine({targetCompanionId:m.id,rng:()=>0.999});assert.equal(g.state.combat.playerTurn.actionUsed,true);assert.throws(()=>g.useMedicine({targetCompanionId:m.id}),/usada|ação/i);
});
test('P164: Stim Application boosts companion only, then expires on encounter closure',()=>{
 const {g,repo}=game(['APLICACIÓN DE ESTIMULANTES']);const m=g.addCompanion({name:'Comando'});g.beginCombat();const original=g.companionRoster()[0].character.characteristics.agilidad;
 const result=g.applyStimApplication({targetCompanionId:m.id,characteristicKey:'agilidad',rng:()=>0.999});assert.equal(result.check.success,true);assert.equal(g.companionRoster()[0].character.characteristics.agilidad,original+1);assert.equal(g.state.player.character.characteristics.agilidad,2);
 reopened(repo,x=>assert.equal(x.companionRoster()[0].character.characteristics.agilidad,original+1));
 g.finishCombat({rng:()=>0});assert.equal(g.companionRoster()[0].character.characteristics.agilidad,original);
 reopened(repo,x=>assert.equal(x.companionRoster()[0].character.characteristics.agilidad,original));
});
test('P164: failed stim check changes neither patient nor healer, but spends action',()=>{
 const {g}=game(['APLICACIÓN DE ESTIMULANTES']),m=g.addCompanion({name:'Escoteira'});g.beginCombat();const before=copy(g.companionRoster());const r=g.applyStimApplication({targetCompanionId:m.id,characteristicKey:'agilidad',rng:()=>0});assert.equal(r.check.success,false);assert.deepEqual(g.companionRoster(),before);assert.equal(g.state.combat.playerTurn.actionUsed,true);
});
test('P164: companion Bacta heals target with source-backed player supervision',()=>{
 const {g,repo}=game(['ESPECIALISTA EN BACTA']),m=g.addCompanion({name:'Explorador'});g.applyCompanionDamage({companionId:m.id,damage:8});const playerBefore=copy(g.state.player.character);
 const r=g.recoverInBacta({hours:2,facilityConfirmed:true,targetCompanionId:m.id});assert.equal(r.woundsRecovered,2);assert.equal(r.specialistBonus,1);assert.equal(g.companionRoster()[0].character.derived.wounds.current,6);assert.deepEqual(g.state.player.character,playerBefore);
 reopened(repo,x=>assert.equal(x.companionRoster()[0].character.derived.wounds.current,6));
});
test('P164: Bacta requires explicit facility and is prohibited during combat',()=>{
 const {g}=game(),m=g.addCompanion({name:'Piloto'});assert.throws(()=>g.recoverInBacta({hours:2,targetCompanionId:m.id}),/Confirme/);g.beginCombat();assert.throws(()=>g.recoverInBacta({hours:2,targetCompanionId:m.id,facilityConfirmed:true}),/Encerre/);
});
test('P164: companion registration and medical actions are rollback-safe',()=>{
 const {g,repo}=game(['MÉDICO EXPERTO','ESPECIALISTA EN BACTA']);let before=copy(g.state);repo.fail=true;assert.throws(()=>g.addCompanion({name:'Falha'}),/armazenamento/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);repo.fail=false;
 const m=g.addCompanion({name:'Real'});g.applyCompanionDamage({companionId:m.id,damage:7});before=copy(g.state);repo.fail=true;
 for(const fn of [()=>g.useMedicine({targetCompanionId:m.id,useMasterDoctor:true,rng:()=>0.999}),()=>g.recoverInBacta({targetCompanionId:m.id,hours:2,facilityConfirmed:true}),()=>g.applyCompanionDamage({companionId:m.id,damage:2})]){
  assert.throws(fn,/armazenamento/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 }
});
test('P164: failed companion stim rolls back both action and target',()=>{
 const {g,repo}=game(['APLICACIÓN DE ESTIMULANTES']),m=g.addCompanion({name:'Alvo'});g.beginCombat();const before=copy(g.state);repo.fail=true;assert.throws(()=>g.applyStimApplication({targetCompanionId:m.id,characteristicKey:'agilidad',rng:()=>0.999}),/armazenamento/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P164: removal is durable and forbidden during active encounter',()=>{
 const {g,repo}=game(),m=g.addCompanion({name:'Temporário'});g.beginCombat();assert.throws(()=>g.removeCompanion(m.id),/Encerre/);g.finishCombat({rng:()=>0});g.removeCompanion(m.id);reopened(repo,x=>assert.deepEqual(x.companionRoster(),[]));
});
test('P164: bridge and offline UI expose companions and target-aware treatment',()=>{
 const {repo}=game(),bridge=new BraseiroUiBridge({repository:repo,storage:null});assert.equal(bridge.addCompanion({name:'Nova Aliada'}).ok,true);const status=bridge.recoveryRuntimeStatus();assert.equal(status.ok,true);assert.equal(status.data.companions.length,1);assert.equal(bridge.companionRoster().ok,true);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');for(const token of ['data-p164-target','data-p164-add','data-p164-remove','data-p164-injure','targetCompanionId:targetCompanionId()'])assert.ok(ui.includes(token),token);for(const token of ['companionRoster','applyCompanionDamage','addCompanion'])assert.ok(bundle.includes(token),token);assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=80,'O bundle não pode perder módulos da base P172');
});
for(const {id} of publishedAdventureCatalog())test(`P164: ${id} adventure trigger/clock/objective/consequence and companion health survive 3 reloads`,()=>{
 const mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);mod.initialObjectives.p164={title:'Evacuar companheiro',type:'SECONDARY',target:2};mod.initialClocks.p164={value:0,max:2,status:'ACTIVE'};
 mod.triggers.push({id:'p164-evacuation',event:'P164_FIELD_EVACUATION',maxFires:2,effects:[{type:'progress-objective',objectiveId:'p164',delta:1},{type:'advance-clock',clockId:'p164',delta:1}],consequence:{id:'p164-evacuation-consequence',description:'Aliado resgatado'}});
 const runtime=new AdventureRuntime(mod);for(let i=0;i<2;i++)runtime.commitAction(`${id}:p164:${i}`,[{type:'event',eventType:'P164_FIELD_EVACUATION',payload:{patient:'custom-companion'}}]);
 const before=runtime.getState();assert.equal(before.objectives.p164.status,'COMPLETED');assert.equal(before.clocks.p164.status,'COMPLETED');assert.ok(before.consequences.some(x=>x.id==='p164-evacuation-consequence'));assert.equal(validateAdventureCheckpoint(mod,before),true);
 const {g,repo}=game();const member=g.addCompanion({name:`Aliado ${id}`});g.applyCompanionDamage({companionId:member.id,damage:3});const beforeGame=copy(g.state);
 assert.throws(()=>runtime.commitAction(`${id}:p164:bad`,[{type:'progress-objective',objectiveId:'nonexistent',delta:1}]));assert.deepEqual(runtime.getState(),before);
 for(let i=0;i<3;i++){const restored=AdventureRuntime.restore(mod,runtime.serialize());assert.equal(restored.commitAction(`${id}:p164:1`,[{type:'event',eventType:'P164_FIELD_EVACUATION',payload:{patient:'custom-companion'}}]).applied,false);assert.deepEqual(restored.getState(),before);const next=new GameController(repo);assert.deepEqual(next.state,beforeGame);}
});
test('P164: imported roster with wrong shape is rejected, never silently erased',()=>{
 const {g}=game(),m=g.addCompanion({name:'Tripulante'});
 for(const bad of [null,{},'fake']){const state=copy(g.state);state.group.companions=bad;assert.throws(()=>validateState(state),/companheiros/);}
 const invalid=copy(g.state);invalid.group.companions[0].character.derived.wounds.current=-1;assert.throws(()=>validateState(invalid),/Saúde/);
 assert.ok(m.id);
});
test('P164: companion treatment cannot forge a medical kit or use equipment not owned',()=>{
 const {g}=game(),m=g.addCompanion({name:'Batedor'});g.applyCompanionDamage({companionId:m.id,damage:4});const c=copy(g.state.player.character);c.gear=[];g.setCharacter(c);const before=copy(g.state);
 assert.throws(()=>g.useMedicine({targetCompanionId:m.id,medicalSupplies:true,rng:()=>0.999}),/suprimentos médicos/);assert.deepEqual(g.state,before);
 assert.throws(()=>g.useMedicine({targetCompanionId:m.id,medicalSupplies:false,medicalItemId:'medpac',rng:()=>0.999}),/inventário/);assert.deepEqual(g.state,before);
 const r=g.useMedicine({targetCompanionId:m.id,medicalSupplies:false,rng:()=>0.999});assert.equal(r.medicalSupplies,false);
});
test('P164: failed encounter close cannot strip companion buffs from persisted state',()=>{
 const {g,repo}=game(['APLICACIÓN DE ESTIMULANTES']),m=g.addCompanion({name:'Aliado'});g.beginCombat();g.applyStimApplication({targetCompanionId:m.id,characteristicKey:'agilidad',rng:()=>0.999});const before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.finishCombat({rng:()=>0}),/armazenamento/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P164: custom companions are independent from published adventure mode EotE/AoR',()=>{
 for(const mode of ['EOTE','AOR','MIXED']){const {g,repo}=game();g.setCampaignMode(mode);const m=g.addCompanion({name:`Companheiro ${mode}`});g.applyCompanionDamage({companionId:m.id,damage:2});reopened(repo,x=>{assert.equal(x.state.campaignMode,mode);assert.equal(x.companionRoster()[0].character.derived.wounds.current,2);});}
});
test('P164: UAE predicate engine rejects pathological recursion and object coercion',()=>{
 const {id}=publishedAdventureCatalog()[0],mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta),runtime=new AdventureRuntime(mod);
 let nested='flag:ready';for(let i=0;i<25;i++)nested=[nested];
 assert.equal(runtime.isConditionMet(nested),false);
 assert.equal(runtime.isConditionMet(Array(65).fill('!flag:absent')),false);
 assert.equal(runtime.isConditionMet(`all:${Array(33).fill('!flag:absent').join('|')}`),false);
 assert.equal(runtime.isConditionMet('payload:thing:[object Object]',{thing:{}}),false);
 assert.equal(runtime.isConditionMet('payload:thing:a,b',{thing:['a','b']}),false);
 assert.equal(runtime.isConditionMet('payload:thing:ok',{thing:'ok'}),true);
 assert.equal(runtime.isConditionMet('payload:thing:true',{thing:true}),true);
});
for(const {id} of publishedAdventureCatalog())test(`P164: ${id} trigger payload provenance cannot be forged by JS object stringification`,()=>{
 const mod=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 mod.initialObjectives.p164Payload={title:'Autenticar resposta',target:1,type:'SECONDARY'};
 mod.triggers.push({id:'p164-payload-gate',event:'P164_AUTH',maxFires:1,condition:'payload:result:[object Object]',effects:[{type:'progress-objective',objectiveId:'p164Payload',delta:1}]});
 const runtime=new AdventureRuntime(mod);runtime.commitAction(`${id}:p164:obj`,[{type:'event',eventType:'P164_AUTH',payload:{result:{malicious:true}}}]);
 assert.equal(runtime.getState().objectives.p164Payload.status,'ACTIVE');assert.equal(runtime.getState().triggersFired['p164-payload-gate']??0,0);
 for(let i=0;i<3;i++){const restored=AdventureRuntime.restore(mod,runtime.serialize());assert.equal(restored.getState().objectives.p164Payload.progress,0);assert.equal(validateAdventureCheckpoint(mod,restored.getState()),true);}
});
for(const {id} of publishedAdventureCatalog())test(`P164: ${id} controller keeps published UAE checkpoint intact while treating companion`,()=>{
 const {g,repo}=game();g.startPublishedAdventureSession(id,{rng:()=>0.5});const beforeUae=copy(g.state.world.publishedAdventureRuntime);
 const m=g.addCompanion({name:`Socorrista ${id}`});g.applyCompanionDamage({companionId:m.id,damage:4});const r=g.useMedicine({targetCompanionId:m.id,rng:()=>0.999});assert.ok(r.healed>0);
 assert.deepEqual(g.state.world.publishedAdventureRuntime,beforeUae);
 reopened(repo,x=>{assert.equal(x.state.world.publishedAdventureRuntime.adventureId,id);assert.deepEqual(x.state.world.publishedAdventureRuntime,beforeUae);assert.equal(x.companionRoster()[0].character.derived.wounds.current,4-r.healed);});
});
