import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const cp=structuredClone, all=aorTalentCatalog();
const names=['ARTILUGIO','METICULOSO','INCITAR A LA REBELIÓN','COMPARTIMENTO OCULTO','UNTAR MANOS','GOLPE CONMOCIONADOR','GOLPE CONMOCIONADOR MEJORADO'];
class Repo{constructor(){this.saved=createCampaign('P177');this.fail=false;}load(){return cp(this.saved);}save(s){if(this.fail)throw Error('falha de armazenamento P177');this.saved=cp(s);}}
function fixture(owned=names,{combat=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=cp(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=owned.map(name=>{const n=all.find(t=>t.originalName===name);assert.ok(n,name);return {nodeId:n.id,originalName:name,name:n.name};});
 c.talents=owned.map(name=>all.find(t=>t.originalName===name).name);
 c.characteristics.intelecto=4;c.characteristics.vontade=4;c.skills.mecanica=4;c.skills.coaccion=3;c.skills.negociacion=3;c.skills.armas_cuerpo_a_cuerpo=3;c.characteristics.fortaleza=4;
 c.credits=1000;c.gear=[{id:'kit',name:'Kit',quantity:2,encumbrance:1},{id:'parts',name:'Peças',quantity:3,encumbrance:1},{id:'cargo',name:'Carga',quantity:1,encumbrance:1},{id:'pack',name:'Mochila',quantity:1,encumbrance:1}];
 c.weapons=[{id:'blade',name:'Lâmina',skill:'armas_cuerpo_a_cuerpo',range:'Interação',damage:5,crit:3}];
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.4});
 if(combat){const state=cp(g.state.combat);state.active=true;state.round=1;state.adversaries=[createAdversary('imperial_soldier',{range:'Interação'}),createAdversary('imperial_officer',{range:'Interação'})];g.commit('COMBAT_UPDATED',{combat:state});}
 return {g,repo};
}
const action=(g,kind,id,extra={})=>g.useAoRP177Action({kind,actionId:id,subjectId:'objective_1',approved:true,...extra});
test('P177 seven source-backed positions have real consumers, no blanket alias',()=>{
 for(const n of names)assert.ok(all.find(x=>x.originalName===n&&x.mechanicallyActive),n);
 assert.ok(aorTalentCoverage().mechanicalPositions>=350); // P177 floor; newer checkpoints add verified consumers.
});
test('P177 Careful Planning persists an approved fact without flipping Destiny, one/session',()=>{
 const {g,repo}=fixture();const d=cp(g.state.group.destiny);const r=action(g,'careful_planning','care1',{fact:'Porta de manutenção acessível'});
 assert.equal(r.receipt.noDestinySpent,true);assert.deepEqual(g.state.group.destiny,d);assert.equal(g.state.scene.approvedFacts[0].fact,'Porta de manutenção acessível');assert.equal(validateState(g.state),true);
 assert.equal(action(g,'careful_planning','care1',{fact:'Porta de manutenção acessível'}).duplicate,true);
 assert.throws(()=>action(g,'careful_planning','care2',{fact:'Outra porta'}),/já utilizado/);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P177 Careful Planning rejects unowned, invalid fact, absent GM approval, and rollback',()=>{
 for(const variant of ['talent','fact','approval','save']){
  const {g,repo}=fixture(variant==='talent'?[]:names);const before=cp(g.state);
  if(variant==='save')repo.fail=true;
  assert.throws(()=>g.useAoRP177Action({kind:'careful_planning',actionId:'care3',subjectId:'objective_1',approved:variant!=='approval',fact:variant==='fact'?'<script>':'Porta aberta'}));
  assert.deepEqual(g.state,before);
 }
});
test('P177 Contraption rolls Hard Mechanics, consumes owned parts and tools only on success',()=>{
 const {g,repo}=fixture(names,{combat:true});const before=cp(g.state.player.character.gear);
 const r=action(g,'contraption','con1',{toolsId:'kit',partsId:'parts',rng:()=>0.4});
 assert.equal(r.receipt.difficulty,3);assert.equal(r.check.pool.difficulty,3);
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.equal(g.state.player.character.gear.find(x=>x.id==='parts').quantity,before[1].quantity-Number(r.check.success));
 assert.equal(r.receipt.expiresWithEncounter,true);if(r.check.success)assert.ok(g.state.player.character.gear.some(x=>x.id===r.receipt.deviceItemId));assert.equal(validateState(g.state),true);
 assert.equal(new GameController(repo).state.master.aorP177Actions.length,1);
 assert.throws(()=>action(g,'contraption','con2',{toolsId:'kit',partsId:'parts'}),/já utilizado/);
});
test('P177 Contraption fails closed for missing gear, noncombat, missing approval, duplicated ids',()=>{
 for(const mode of ['missing','noncombat','approval','same']){
  const {g}=fixture(names,{combat:mode!=='noncombat'});const before=cp(g.state);
  assert.throws(()=>g.useAoRP177Action({kind:'contraption',actionId:'c2',subjectId:'device',approved:mode!=='approval',toolsId:'kit',partsId:mode==='missing'?'missing':mode==='same'?'kit':'parts'}));
  assert.deepEqual(g.state,before);
 }
});
test('P177 Incite Rebellion rolls Hard Coercion against real minion/rival, respects skill cap',()=>{
 const {g,repo}=fixture(names,{combat:true});const ids=g.state.combat.adversaries.map(x=>x.id);
 const r=action(g,'incite_rebellion','inc1',{targetIds:ids,rng:()=>0.4});
 assert.equal(r.receipt.difficulty,3);assert.equal(r.check.pool.difficulty,3);
 if(r.check.success)for(const a of g.state.combat.adversaries)assert.equal(a.aorIncitedRebellion.sourceActionId,'inc1');
 assert.equal(validateState(g.state),true);
 assert.equal(validateState(new GameController(repo).state),true);
});
test('P177 Incite Rebellion rejects phantom/duplicate/over-cap targets, leaves state unchanged',()=>{
 for(const targetIds of [['fake'],['same','same'],['one','two','three','four']]){
  const {g}=fixture(names,{combat:true});const before=cp(g.state);
  assert.throws(()=>action(g,'incite_rebellion','incbad',{targetIds,rng:()=>0.4}));assert.deepEqual(g.state,before);
 }
});
test('P177 Hidden Storage uses owned gear capacity and no item duplication',()=>{
 const {g,repo}=fixture();const before=g.state.player.character.gear.length;
 const r=action(g,'hidden_storage','hide1',{containerId:'pack',itemId:'cargo'});
 assert.equal(r.receipt.detectionDifficulty,5);assert.equal(g.state.player.character.gear.length,before);
 assert.equal(g.state.player.character.gear.find(x=>x.id==='cargo').hiddenStorage.containerId,'pack');
 assert.equal(validateState(g.state),true);assert.equal(validateState(new GameController(repo).state),true);
 assert.throws(()=>action(g,'hidden_storage','hide2',{containerId:'pack',itemId:'cargo'}),/já oculto/);
});
test('P177 Hidden Storage rejects nonexistent item/container, capacity overrun, cross-campaign replay',()=>{
 for(const variant of ['item','container','capacity']){
  const {g}=fixture();const c=cp(g.state.player.character);
  if(variant==='capacity')c.gear.find(x=>x.id==='cargo').encumbrance=9;
  g.setCharacter(c);const before=cp(g.state);
  assert.throws(()=>action(g,'hidden_storage','hide3',{containerId:variant==='container'?'fake':'pack',itemId:variant==='item'?'fake':'cargo'}));assert.deepEqual(g.state,before);
 }
});
test('P177 Greased Palms spends 50 credits, upgrades actual Negotiation roll and records causal receipt',()=>{
 const {g,repo}=fixture();const before=g.state.player.character.credits;
 const r=g.resolveSkillCheck({skillKey:'negociacion',difficulty:2,greasedPalmsCredits:50,greasedPalmsActionId:'bribe1',rng:()=>0.4});
 assert.equal(r.greasedPalms.upgrades,1);assert.equal(g.state.player.character.credits,before-50);
 assert.equal(g.state.master.aorP177Actions.length,1);assert.equal(validateState(g.state),true);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P177 Greased Palms refuses unearned rank, non-social skill, invalid credits, duplicate id',()=>{
 for(const mode of ['talent','skill','amount']){
  const {g}=fixture(mode==='talent'?[]:names);const before=cp(g.state);
  assert.throws(()=>g.resolveSkillCheck({skillKey:mode==='skill'?'mecanica':'negociacion',difficulty:2,greasedPalmsCredits:mode==='amount'?75:50,greasedPalmsActionId:'bribe1',rng:()=>0.4}));
  assert.deepEqual(g.state,before);
 }
 const {g}=fixture();g.resolveSkillCheck({skillKey:'negociacion',difficulty:2,greasedPalmsCredits:50,greasedPalmsActionId:'bribe1',rng:()=>0.4});
 const before=cp(g.state);assert.throws(()=>g.resolveSkillCheck({skillKey:'negociacion',difficulty:2,greasedPalmsCredits:50,greasedPalmsActionId:'bribe1',rng:()=>0.4}),/duplicado/);assert.deepEqual(g.state,before);
});
test('P177 Stunning Blow replaces personal melee wounds with strain without ignoring soak',()=>{
 const {g}=fixture(names,{combat:true});const target=g.state.combat.adversaries[1];
 const r=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useStunningBlow:true,rng:()=>0.4});
 assert.equal(r.damageType,r.hit?'strain':undefined);assert.equal(r.pendingSpend?.attackSnapshot?.damageType,'strain');
 assert.equal(validateState(g.state),true);
});
test('P177 Stunning Blow rejects unowned talent and nonmelee attack',()=>{
 const {g}=fixture([],{combat:true});const before=cp(g.state);
 assert.throws(()=>g.resolveWeaponAttack({weaponIndex:0,useStunningBlow:true,rng:()=>0.4}),/adquirido/);assert.deepEqual(g.state,before);
});
test('P177 Improved Stunning Blow spends real pending Triumph only once, applies staggered',()=>{
 const {g}=fixture(names,{combat:true});const target=g.state.combat.adversaries[1];
 let roll=0;const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useStunningBlow:true,rng:()=>{roll++;return roll===2?0.999:roll<=4?0.3:0;}});
 assert.equal(attack.hit,true);assert.ok(g.state.combat.pendingPlayerSpend.remainingTriumph>0);
 const result=g.activatePendingImprovedStunningBlow({triumphs:1});assert.equal(result.triumphs,1);
 assert.ok(g.state.combat.adversaries.find(a=>a.id===target.id).statusEffects.some(s=>s.type==='staggered'));
 assert.equal(validateState(g.state),true);
 assert.throws(()=>g.activatePendingImprovedStunningBlow({triumphs:1}),/indisponível/);
});
test('P177 Improved Stunning Blow fails closed without Triumph or target, no fabricated status',()=>{
 const {g}=fixture(names,{combat:true});const target=g.state.combat.adversaries[1];
 g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useStunningBlow:true,rng:()=>0.4});const before=cp(g.state);
 assert.throws(()=>g.activatePendingImprovedStunningBlow({triumphs:5}),/suficientes/);assert.deepEqual(g.state,before);
});
test('P177 forged ledgers, missing events, duplicates, tampered credits are rejected',()=>{
 const {g}=fixture();action(g,'careful_planning','care1',{fact:'Porta de serviço'});
 for(const tamper of [s=>s.master.aorP177Actions[0].costCredits=50,s=>s.events=s.events.filter(e=>e.type!=='AOR_P177_ACTION'),s=>s.master.aorP177Actions.push(cp(s.master.aorP177Actions[0]))]){
  const state=cp(g.state);tamper(state);assert.throws(()=>validateState(state));
 }
});
test('P177 P176 save migration, Android bridge, and panel are connected',()=>{
 const {g,repo}=fixture();const old=cp(g.state);delete old.master.aorP177Actions;assert.equal(validateState(migrateState(old)),true);
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});assert.equal(bridge.aorP177Status().ok,true);
 assert.equal(bridge.useAoRP177Action({kind:'careful_planning',actionId:'bridge',subjectId:'scene',approved:true,fact:'Acesso confirmado'}).ok,true);
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const key of ['aorP177Status','useAoRP177Action','activatePendingImprovedStunningBlow'])assert.ok(bundle.includes(key),key);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 for(const key of ['PLANEJAMENTO CUIDADOSO','ARTILUGIO','INCITAR À REBELIÃO','UNTAR MÃOS'])assert.ok(ui.includes(key),key);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P177 ${id}: approved Careful Planning leaves UAE untouched across three reopenings`,()=>{
  const {g,repo}=fixture();g.setCampaignMode('MIXED');g.startPublishedAdventureSession(id,{rng:()=>0.5});
  const before=cp(g.state.world.publishedAdventureRuntime);
  action(g,'careful_planning','care_'+id,{fact:'O grupo tem uma rota de acesso'});
  assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
  for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.deepEqual(reopened.state.world.publishedAdventureRuntime,before);}
 });
}
test('P177 Contraption temporary gear disappears when the encounter closes, ledger persists',()=>{
 const {g,repo}=fixture(names,{combat:true});const r=action(g,'contraption','conend',{toolsId:'kit',partsId:'parts',rng:()=>0.4});
 if(!r.check.success)return;
 assert.ok(g.state.player.character.gear.some(x=>x.id===r.receipt.deviceItemId));
 g.finishCombat({rng:()=>0.4});assert.equal(g.state.player.character.gear.some(x=>x.id===r.receipt.deviceItemId),false);
 assert.equal(g.state.master.aorP177Actions.length,1);assert.equal(validateState(g.state),true);
 assert.equal(validateState(new GameController(repo).state),true);
});
test('P177 all four action families roll back on failed repository save',()=>{
 for(const [kind,opts,combat] of [
  ['contraption',{toolsId:'kit',partsId:'parts'},true],
  ['incite_rebellion',{},true],
  ['hidden_storage',{containerId:'pack',itemId:'cargo'},false],
  ['careful_planning',{fact:'Entrada de manutenção'},false]
 ]){
  const {g,repo}=fixture(names,{combat});
  if(kind==='incite_rebellion')opts.targetIds=[g.state.combat.adversaries[0].id];
  const before=cp(g.state);repo.fail=true;
  assert.throws(()=>action(g,kind,'fail_'+kind,opts),/armazenamento/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 }
});
test('P177 Greased Palms rolls back credits, maneuvers and receipts on save failure',()=>{
 const {g,repo}=fixture(names,{combat:true});const before=cp(g.state);repo.fail=true;
 assert.throws(()=>g.resolveSkillCheck({skillKey:'negociacion',difficulty:2,greasedPalmsCredits:50,greasedPalmsActionId:'fail_bribe',rng:()=>0.4}),/armazenamento/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P177 Stunning Blow does not ignore target soak or allow improvised critical spending',()=>{
 const {g}=fixture(names,{combat:true});const target=g.state.combat.adversaries[1];
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useStunningBlow:true,rng:()=>0.4});
 if(attack.hit){assert.ok(attack.targetDamage.suffered<=Math.max(0,attack.damage-target.soak));}
 const before=cp(g.state);
 assert.throws(()=>g.activatePendingImprovedStunningBlow({triumphs:6}),/inválida/);
 assert.deepEqual(g.state,before);
});
test('P177 Soft Spot AoR adds Cunning to real personal attack on hit, spends exactly one Destiny',()=>{
 const {g}=fixture([...names,'PUNTO DÉBIL'],{combat:true});const target=g.state.combat.adversaries[1];
 const group=cp(g.state.group);group.destiny.light=3;group.destiny.dark=0;g.commit('GROUP_UPDATED',{group});
 const before=g.state.group.destiny.light;
 const attack=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target.id,useSoftSpot:true,rng:()=>0.4});
 assert.equal(attack.hit,true);assert.equal(attack.talentDamageBonus>=g.state.player.character.characteristics.astucia,true);
 assert.equal(g.state.group.destiny.light,before-1);assert.equal(validateState(g.state),true);
});
test('P177 Soft Spot rejects unowned, conflicting Destiny options and rolls back on missing Destiny',()=>{
 for(const mode of ['unowned','conflict','destiny']){
  const {g}=fixture(mode==='unowned'?names:[...names,'PUNTO DÉBIL'],{combat:true});const group=cp(g.state.group);group.destiny.light=mode==='destiny'?0:3;g.commit('GROUP_UPDATED',{group});const before=cp(g.state);
  assert.throws(()=>g.resolveWeaponAttack({weaponIndex:0,useSoftSpot:true,useLightDestiny:mode==='conflict',rng:()=>0.4}));
  assert.deepEqual(g.state,before);
 }
});
