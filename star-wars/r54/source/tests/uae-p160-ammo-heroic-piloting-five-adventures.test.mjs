/** P160 — integrated AoR source-backed ammunition, Heroic Fortitude, pilot skill and UAE. */
import test from 'node:test';import assert from 'node:assert/strict';import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {aorCanonicalTalentName} from '../app/src/main/assets/vtt/js/expansion/aorTalentRuntimeAliases.mjs';
import {talentRankByOriginal,talentSkillPoolModifiers,sourceBackedTalentRank} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {criticalCheckModifiers} from '../app/src/main/assets/vtt/js/rules/critical.mjs';
import {AdventureRuntime} from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import {validateAdventureCheckpoint} from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
const copy=structuredClone,nodes=Object.values(AOR_TALENT_TREES).flatMap(x=>Object.values(x).flatMap(t=>t.nodes));
class Repo{constructor(){this.saved=createCampaign('P160');this.writes=0;this.fail=false}load(){return copy(this.saved)}save(s){if(this.fail)throw Error('P160 disk failure');this.saved=copy(s);this.writes++}}
function game(purchased=[],{light=2}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};c.talents=[];c.talentPurchases=[];
 for(const [name,rank] of purchased){const found=nodes.filter(n=>n.originalName===name).slice(0,rank);assert.equal(found.length,rank,name);
  for(const n of found){c.talents.push(n.name);c.talentPurchases.push({nodeId:n.id,originalName:name,name:n.name,source:n.source});}}
 g.setCharacter(c);const group=copy(g.state.group);group.destiny={light,dark:1};g.commit('GROUP_UPDATED',{group});return {g,repo};
}
function attackWithDespair(x,{limited=false}={}){
 const c=copy(x.g.state.player.character);
 if(limited)c.weapons[0].qualities=['Munição limitada 2'];
 x.g.setCharacter(c);x.g.beginCombat();
 const result=x.g.resolveWeaponAttack({weaponIndex:0,challenge:1,difficulty:0,rng:()=>0.999});
 assert.ok(result.check.despair>=1);assert.ok(result.attackId);assert.equal(result.weaponIndex,0);
 return result;
}
test('P160: five source-backed AoR positions gain consumers, 275/360 total',()=>{
 assert.ok(aorTalentCoverage().mechanicalPositions>=275);
 for(const [name,canonical,slots] of [['MUNICIÓN DE RESERVA','SPARECL',1],['PILOTO EXPERTO','SKILLJOCK',2],['MADERA DE HÉROE','HERFORT',2]]){
  assert.equal(aorCanonicalTalentName(name),canonical);
  assert.equal(aorTalentCatalog().filter(n=>n.originalName===name&&n.mechanicallyActive).length,slots);
  assert.equal(talentRankByOriginal(game([[name,slots]]).g.state.player.character,canonical),slots);
 }
 for(const name of ['GUARDAESPALDAS','MEJORAS CASERAS'])
  assert.equal(aorTalentCatalog().filter(n=>n.originalName===name&&n.mechanicallyActive).length,2); // P170
});
for(const rank of [0,1,2])for(const skill of ['pilotar_vehiculo_planetario','pilotar_vehiculo_espacial'])
 test(`P160: Skilled Jockey rank ${rank} removes only actual piloting setback for ${skill}`,()=>{
  const c=game(rank?[['PILOTO EXPERTO',rank]]:[]).g.state.player.character;
  assert.equal(talentSkillPoolModifiers(c,skill,{setback:3}).setback,3-rank);
  assert.equal(talentSkillPoolModifiers(c,'mecanica',{setback:3}).setback,3);
 });
test('P160: pilot modifiers actually affect controller dice and survive three reopens',()=>{
 const {g,repo}=game([['PILOTO EXPERTO',2]]);const result=g.resolveSkillCheck({skillKey:'pilotar_vehiculo_espacial',setback:3,rng:()=>0.5});
 assert.equal(result.pool.setback,1);for(let i=0;i<3;i++){const reload=new GameController(repo);assert.equal(validateState(reload.state),true);assert.equal(talentRankByOriginal(reload.state.player.character,'SKILLJOCK'),2);}
});
for(const rank of [0,1,2])test(`P160: Heroic Fortitude rank ${rank} Destiny and repeat gate`,()=>{
 const {g,repo}=game(rank?[['MADERA DE HÉROE',rank]]:[]);g.beginCombat();
 if(!rank){assert.throws(()=>g.activateHeroicFortitude(),/não foi adquirida/);return;}
 const before=copy(g.state);const result=g.activateHeroicFortitude();assert.equal(result.active,true);
 assert.equal(g.state.group.destiny.light,before.group.destiny.light-1);
 assert.equal(g.state.player.character.combatEffects.heroicFortitude,true);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.equal(reopened.state.player.character.combatEffects.heroicFortitude,true);assert.throws(()=>reopened.activateHeroicFortitude(),/já está ativa/);}
});
test('P160: Heroic Fortitude ignores check penalties on Brawn/Agility but not intellect or non-check effects',()=>{
 const c=copy(game([['MADERA DE HÉROE',1]]).g.state.player.character);
 c.criticalInjuries=[
  {id:'a',active:true,effect:{setback:1,duration:'until_healed'}},
  {id:'b',active:true,effect:{difficultyIncrease:1,difficultyCharacteristics:['fortaleza','agilidad'],duration:'until_healed'}},
  {id:'c',active:true,effect:{difficultyIncrease:1,difficultyCharacteristics:['intelecto'],duration:'until_healed'}},
  {id:'d',active:true,effect:{cannotVoluntarilySufferStrain:true,maxManeuvers:1,duration:'until_healed'}}
 ];
 assert.equal(criticalCheckModifiers(c,{skillKey:'atletismo',characteristicKey:'fortaleza'}).setback,1);
 assert.equal(criticalCheckModifiers(c,{skillKey:'atletismo',characteristicKey:'fortaleza'}).difficultyIncrease,1);
 c.combatEffects={heroicFortitude:true};
 for(const characteristicKey of ['fortaleza','agilidad']){
  const result=criticalCheckModifiers(c,{skillKey:'atletismo',characteristicKey});
  assert.equal(result.setback,0);assert.equal(result.difficultyIncrease,0);
  assert.equal(result.cannotVoluntarilySufferStrain,true);assert.equal(result.maxManeuvers,1);
 }
 assert.equal(criticalCheckModifiers(c,{skillKey:'mecanica',characteristicKey:'intelecto'}).setback,1);
 assert.equal(criticalCheckModifiers(c,{skillKey:'mecanica',characteristicKey:'intelecto'}).difficultyIncrease,1);
 const forged=copy(c);forged.talentPurchases=[];
 assert.equal(criticalCheckModifiers(forged,{skillKey:'atletismo',characteristicKey:'fortaleza'}).setback,1);
});
test('P160: suppressed next-check critical remains pending, is consumed on later unprotected check',()=>{
 const c=copy(game([['MADERA DE HÉROE',1]]).g.state.player.character);
 c.criticalInjuries=[{id:'next',active:true,pending:true,effect:{setback:1,duration:'next_check'}}];c.combatEffects={heroicFortitude:true};
 const protectedCheck=criticalCheckModifiers(c,{skillKey:'atletismo',characteristicKey:'fortaleza',consume:true});
 assert.equal(protectedCheck.setback,0);assert.equal(protectedCheck.character.criticalInjuries[0].pending,true);
 const later=criticalCheckModifiers(protectedCheck.character,{skillKey:'medicina',characteristicKey:'intelecto',consume:true});
 assert.equal(later.setback,1);assert.equal(later.character.criticalInjuries[0].pending,false);
});
test('P160: Heroic Fortitude fails without Destiny or encounter, never partially commits',()=>{
 const {g,repo}=game([['MADERA DE HÉROE',1]],{light:0});const saved=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.activateHeroicFortitude(),/encontro ativo/);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
 g.beginCombat();const before=copy(repo.saved),n=repo.writes;
 assert.throws(()=>g.activateHeroicFortitude(),/Destino luminoso/);assert.deepEqual(repo.saved,before);assert.equal(repo.writes,n);
});
test('P160: Heroic Fortitude disk failure rolls back character, Destiny and combat log',()=>{
 const {g,repo}=game([['MADERA DE HÉROE',1]]);g.beginCombat();const before=copy(g.state);repo.fail=true;
 assert.throws(()=>g.activateHeroicFortitude(),/disk failure/);assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 repo.fail=false;assert.equal(g.activateHeroicFortitude().active,true);
});
test('P160: starting new combat clears stale Heroic Fortitude from previous encounter',()=>{
 const {g}=game([['MADERA DE HÉROE',1]]);g.beginCombat();g.activateHeroicFortitude();g.beginCombat();
 assert.notEqual(g.state.player.character.combatEffects?.heroicFortitude,true);
});
test('P160: Spare Clip prevents GM Despair ammunition loss and writes an audit receipt',()=>{
 const x=game([['MUNICIÓN DE RESERVA',1]]);attackWithDespair(x);const before=copy(x.g.state.player.character);
 const result=x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0});assert.equal(result.prevented,true);
 assert.deepEqual(x.g.state.player.character.weapons[0],before.weapons[0]);
 assert.equal(x.g.state.combat.ammoDespairAppliedAttackId,x.g.state.combat.lastAttack.attackId);
 for(let i=0;i<3;i++){const again=new GameController(x.repo);assert.equal(validateState(again.state),true);assert.throws(()=>again.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/já foi adjudicado/);}
});
test('P160: without Spare Clip GM spends Despair, ammo blocks next attack, reload consumes gear',()=>{
 const x=game();const c=copy(x.g.state.player.character);c.gear.push({id:'power_pack_or_ammo_reload',name:'Recarga adicional',quantity:2});x.g.setCharacter(c);
 attackWithDespair(x);assert.equal(x.g.state.player.character.weapons[0].runtimeState?.outOfAmmo,undefined);
 const r=x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0});assert.equal(r.ammoLost,true);
 assert.equal(x.g.state.player.character.weapons[0].runtimeState.outOfAmmo,true);
 const reload=new GameController(x.repo);assert.throws(()=>reload.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/já foi adjudicado/);
 assert.throws(()=>reload.resolveWeaponAttack({weaponIndex:0,rng:()=>0.5}),/sem munição/);
 const restored=reload.reloadLimitedAmmoWeapon('Pistola blaster');assert.equal(restored.outOfAmmo,false);assert.equal(restored.reloadConsumed,true);
 assert.equal(reload.state.player.character.gear.find(i=>i.id==='power_pack_or_ammo_reload').quantity,1);
 for(let i=0;i<3;i++)assert.equal(new GameController(x.repo).state.player.character.weapons[0].runtimeState.outOfAmmo,false);
});
test('P160: GM cannot fabricate Despair, weapon identity or replay loss',()=>{
 const x=game();attackWithDespair(x);const saved=copy(x.repo.saved),writes=x.repo.writes;
 for(const index of [-1,1,100,'oops'])assert.throws(()=>x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:index}));
 assert.deepEqual(x.repo.saved,saved);assert.equal(x.repo.writes,writes);
});
test('P160: ammo Despair disk failure rolls back both character and combat receipt',()=>{
 const x=game();attackWithDespair(x);const before=copy(x.g.state);x.repo.fail=true;
 assert.throws(()=>x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/disk failure/);assert.deepEqual(x.g.state,before);
 x.repo.fail=false;assert.equal(x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0}).ammoLost,true);
});
test('P160: limited ammo remains limited even when Spare Clip is acquired',()=>{
 const x=game([['MUNICIÓN DE RESERVA',1]]);attackWithDespair(x,{limited:true});
 assert.equal(x.g.state.player.character.weapons[0].runtimeState.limitedAmmoRemaining,1);
 assert.throws(()=>x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/Munição Limitada/);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P160 ${id}: five-adventure UAE cascade, clocks, objectives, consequences and three restores`,()=>{
  const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
  module.initialObjectives['p160-obj']={title:'Objetivo integrado P160',type:'SECONDARY',target:1};
  module.initialClocks['p160-clock']={value:0,max:1,status:'ACTIVE'};
  module.triggers.push({id:'p160-trigger',event:'P160_SIGNAL',maxFires:1,effects:[
   {type:'advance-clock',clockId:'p160-clock',delta:1},{type:'progress-objective',objectiveId:'p160-obj',delta:1},
   {type:'set-flag',flag:'p160-flag',value:true}],consequence:{id:'p160-consequence',description:'Efeito de aventura P160'}});
  const runtime=new AdventureRuntime(module),key=`p160:${id}:once`,commands=[{type:'event',eventType:'P160_SIGNAL'}];
  assert.equal(runtime.commitAction(key,commands).applied,true);
  const saved=runtime.getState();assert.equal(saved.objectives['p160-obj'].status,'COMPLETED');
  assert.equal(saved.clocks['p160-clock'].status,'COMPLETED');assert.equal(saved.consequences.length,1);
  assert.equal(validateAdventureCheckpoint(module,saved),true);
  for(let i=0;i<3;i++){const again=AdventureRuntime.restore(module,runtime.serialize());assert.equal(again.commitAction(key,commands).applied,false);assert.deepEqual(again.getState(),saved);}
  const before=runtime.getState();assert.throws(()=>runtime.commitAction(`p160:${id}:fail`,[{type:'event',eventType:'P160_SIGNAL'},{type:'complete-objective',objectiveId:'forged'}]));assert.deepEqual(runtime.getState(),before);
 });
}
test('P160: Android 1.0.160, UI actions, 79-module bundle and eight Java sources present',()=>{
 const gradle=readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]||0)>=10160);assert.ok(bundle.includes(`FINAL ${gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1]} offline engine bundle`));
 for(const token of ['MUNICIÓN DE RESERVA','PILOTO EXPERTO','MADERA DE HÉROE','adjudicateAttackDespairAmmoLoss','activateHeroicFortitude'])assert.ok(bundle.includes(token),token);assert.ok(Number(bundle.match(/modules:(\d+)/)?.[1]??0)>=80,'O bundle não pode perder módulos da base P172');
 for(const token of ['data-p160-panel','data-p160-heroic','data-p160-despair','data-p160-reload'])assert.ok(ui.includes(token),token);
});
test('P160: forged display labels and duplicated source node cannot multiply piloting, ammo or critical effects',()=>{
 const {g}=game([['PILOTO EXPERTO',1],['MADERA DE HÉROE',1]]);
 const c=copy(g.state.player.character);c.talents.push('Munição reserva','Piloto especialista','Fibra de herói');
 c.talentPurchases.push(copy(c.talentPurchases[0]));
 assert.equal(talentSkillPoolModifiers(c,'pilotar_vehiculo_espacial',{setback:3}).setback,2);
 assert.equal(talentRankByOriginal(c,'SKILLJOCK')>=1,true);
 assert.equal(sourceBackedTalentRank(c,'SKILLJOCK'),1);
 assert.equal(sourceBackedTalentRank(c,'HERFORT'),1);
 assert.equal(sourceBackedTalentRank(c,'SPARECL'),0);
});
test('P160: ammo Despair cannot be adjudicated after combat has ended',()=>{
 const x=game();attackWithDespair(x);x.g.finishCombat({rng:()=>0.5});
 const before=copy(x.repo.saved),writes=x.repo.writes;
 assert.throws(()=>x.g.adjudicateAttackDespairAmmoLoss({weaponIndex:0}),/encontro ativo/);
 assert.deepEqual(x.repo.saved,before);assert.equal(x.repo.writes,writes);
});
test('P160: Android bridge actually dispatches Heroic Fortitude and Despair receipts',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const x=game([['MADERA DE HÉROE',1],['MUNICIÓN DE RESERVA',1]]);attackWithDespair(x);
 const bridge=new BraseiroUiBridge({repository:x.repo,storage:null});
 const activated=bridge.activateHeroicFortitude();assert.equal(activated.ok,true);assert.equal(activated.data.active,true);
 const result=bridge.adjudicateAttackDespairAmmoLoss({weaponIndex:0});assert.equal(result.ok,true);assert.equal(result.data.prevented,true);
 const duplicate=bridge.adjudicateAttackDespairAmmoLoss({weaponIndex:0});assert.equal(duplicate.ok,false);
 assert.equal(validateState(x.repo.saved),true);
});
