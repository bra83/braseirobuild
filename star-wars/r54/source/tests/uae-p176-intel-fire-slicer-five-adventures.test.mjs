import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {sourceBackedTalentRank,masterSkillDifficultyOption} from '../app/src/main/assets/vtt/js/rules/talents.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const copy=structuredClone,all=aorTalentCatalog(),byName=name=>all.find(n=>n.originalName===name);
const names=['FUEGO CONTROLADO','DISEÑO FAMILIAR','COMPRAR INFORMACIÓN','PIRATA INFORMÁTICO CONSUMADO','PIRATA INFORMÁTICO EXPERTO','SALIDA TÉRMICA','IMPACTO DIRECTO','IMPACTO DIRECTO MEJORADO'];
class Repo{constructor(){this.saved=createCampaign('P176');this.fail=false;}load(){return copy(this.saved);}save(x){if(this.fail)throw Error('P176 falha de armazenamento');this.saved=copy(x);}}
function fixture(talents=names,{combat=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=copy(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=talents.map(name=>{const n=byName(name);assert.ok(n,name);return {nodeId:n.id,originalName:n.originalName,name:n.name};});
 c.talents=talents.map(name=>byName(name).name);c.credits=5000;
 c.characteristics.intelecto=4;c.skills.educacion=3;c.skills.computadoras=4;
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.4});
 if(combat){const state=copy(g.state.combat);state.active=true;state.round=1;g.commit('COMBAT_UPDATED',{combat:state});}
 return {g,repo};
}
const known=(g,actionId='schematic')=>g.useAoRKnownSchematic({actionId,subjectId:'hangar_alpha',approved:true,rng:()=>0.4});
const bought=(g,actionId='intel')=>g.useAoRBoughtInfo({actionId,subjectId:'intel_imperial',skillKey:'educacion',difficulty:3,approved:true});
test('P176 source-backed four new AoR slots; separate Skilled and Master Slicer',()=>{
 for(const n of names)assert.equal(byName(n).mechanicallyActive,true,n);
 assert.ok(aorTalentCoverage().mechanicalPositions>=342); // P176 minimum; newer checkpoints extend AoR.
 const {g}=fixture(['PIRATA INFORMÁTICO CONSUMADO']);
 assert.equal(sourceBackedTalentRank(g.state.player.character,'SKILSLI'),1);
 assert.equal(masterSkillDifficultyOption(g.state.player.character,'slicer').available,false);
 const {g:expert}=fixture(['PIRATA INFORMÁTICO EXPERTO']);
 assert.equal(masterSkillDifficultyOption(expert.state.player.character,'slicer').available,true);
});
test('P176 Bought Info charges 150 credits, automatic one success, GM revelation pending, causal receipt',()=>{
 const {g,repo}=fixture();const credits=g.state.player.character.credits;
 const result=bought(g);assert.equal(result.netSuccess,1);assert.equal(result.creditsRemaining,credits-150);
 assert.equal(result.gmRevelationRequired,true);assert.equal(validateState(g.state),true);
 assert.equal(g.state.master.aorIntelActions.length,1);assert.equal(g.state.events.filter(e=>e.type==='AOR_INTEL_ACTION').length,1);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.equal(reopened.state.player.character.credits,credits-150);}
 const stable=copy(g.state);assert.equal(bought(g).duplicate,true);assert.deepEqual(g.state,stable);
 assert.throws(()=>g.useAoRBoughtInfo({actionId:'intel',subjectId:'different',difficulty:3,approved:true}),/divergentes/);
});
test('P176 Bought Info rejects unowned talent, missing approval, invalid subject and insufficient funds',()=>{
 for(const variant of ['talent','approval','subject','funds']){
  const {g}=fixture();let opts={actionId:'safe',subjectId:'intel_imperial',skillKey:'educacion',difficulty:3,approved:true};
  if(variant==='talent'){const c=copy(g.state.player.character);c.talentPurchases=[];g.setCharacter(c);}
  if(variant==='approval')opts.approved=false;
  if(variant==='subject')opts.subjectId='bad id';
  if(variant==='funds'){const c=copy(g.state.player.character);c.credits=1;g.setCharacter(c);}
  const before=copy(g.state);assert.throws(()=>g.useAoRBoughtInfo(opts),/adquirido|aprovação|identificadores|Créditos/);assert.deepEqual(g.state,before);
 }
});
test('P176 Known Schematic Hard Education maneuver, one/session and no invented maps',()=>{
 const {g,repo}=fixture(names,{combat:true});const before=copy(g.state.combat.playerTurn);
 const result=known(g);assert.equal(result.receipt.difficulty,3);assert.equal(result.receipt.creditsSpent,0);
 assert.equal(g.state.combat.playerTurn.maneuversUsed,before.maneuversUsed+1);
 assert.equal(g.state.combat.playerTurn.actionUsed,before.actionUsed);
 assert.equal(result.receipt.gmRevelationRequired,result.check.success);
 assert.equal(validateState(g.state),true);
 assert.throws(()=>known(g,'second'),/já utilizado/);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);}
});
test('P176 Known Schematic failure still consumes the single session use',()=>{
 const {g}=fixture();const result=g.useAoRKnownSchematic({actionId:'failed',subjectId:'hangar_alpha',approved:true,rng:()=>0});
 assert.equal(result.success,false);assert.equal(result.gmRevelationRequired,false);
 assert.throws(()=>known(g,'retry'),/já utilizado/);assert.equal(validateState(g.state),true);
});
test('P176 Fire Control gives +1 target silhouette on actual ship gunnery and expires',()=>{
 const {g,repo}=fixture();g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Bocajarro'});
 const fire=g.useAoRFireControl();assert.equal(fire.bonusTargetSilhouette,1);
 assert.equal(g.state.vehicles.combat.maneuversUsed,1);
 assert.throws(()=>g.useAoRFireControl(),/não acumula/);
 const attack=g.resolveVehicleAttack({rng:()=>0.4});assert.equal(attack.controlledFireBonus,1);
 assert.equal(validateState(g.state),true);
 const restored=new GameController(repo);assert.equal(restored.state.vehicles.combat.aorFireControl.round,1);
 g.nextVehicleRound();assert.equal(g.state.vehicles.combat.aorFireControl,null);
 assert.equal(g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId].roundEffects.controlledFire,false);
});
test('P176 Fire Control second maneuver pays vehicle strain and cannot bypass action budget',()=>{
 const {g}=fixture();g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 g.useVehiclePilotManeuver('accelerate');
 const before=g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId].systemStrain;
 const r=g.useAoRFireControl({method:'strain'});assert.equal(r.bonusTargetSilhouette,1);
 assert.equal(g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId].systemStrain,before+2);
 assert.equal(validateState(g.state),true);
});
test('P176 Skilled Slicer Triumph grants only same-system Computers maneuver; Master Slicer separate',()=>{
 const {g,repo}=fixture(names,{combat:true});
 const check=g.resolveSkillCheck({skillKey:'computadoras',difficulty:1,slicingSystemId:'console_1',confirmedSlicingSystem:true,spendSkilledSlicerTriumph:true,rng:()=>0.999});
 assert.equal(check.skilledSlicer.triumphSpent,1);assert.equal(check.remainingTriumph,check.triumph-1);
 assert.equal(g.state.combat.aorSkilledSlicer.systemId,'console_1');
 assert.throws(()=>g.resolveSkillCheck({skillKey:'computadoras',difficulty:1,slicingSystemId:'console_2',useSkilledSlicerManeuver:true,rng:()=>0.4}),/mesmo sistema/);
 const result=g.resolveSkillCheck({skillKey:'computadoras',difficulty:1,slicingSystemId:'console_1',useSkilledSlicerManeuver:true,rng:()=>0.4});
 assert.equal(result.skilledSlicerManeuver,true);assert.equal(validateState(g.state),true);
 for(let i=0;i<3;i++){const restored=new GameController(repo);assert.equal(restored.state.combat.aorSkilledSlicer.systemId,'console_1');}
});
test('P176 Skilled Slicer cannot activate from a display label or without Triumph',()=>{
 const {g}=fixture([],{combat:true});const c=copy(g.state.player.character);c.talents=['Hacker consumado'];g.setCharacter(c);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'computadoras',slicingSystemId:'sys',confirmedSlicingSystem:true,spendSkilledSlicerTriumph:true,rng:()=>0.999}),/talento comprado/);
 const {g:earned}=fixture(['PIRATA INFORMÁTICO CONSUMADO'],{combat:true});
 const r=earned.resolveSkillCheck({skillKey:'computadoras',slicingSystemId:'sys',confirmedSlicingSystem:true,spendSkilledSlicerTriumph:true,rng:()=>0});
 assert.equal(r.skilledSlicer.triumphSpent,0);assert.equal(earned.state.combat.aorSkilledSlicer,undefined);
});
test('P176 rollback on repository failure covers credits, events, maneuvers, source receipts',()=>{
 for(const action of [g=>bought(g),g=>known(g),g=>{g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});return g.useAoRFireControl();}]){
  const {g,repo}=fixture();if(action.toString().includes('addVehicle')){g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});}
  const before=copy(g.state);repo.fail=true;
  if(action.toString().includes('addVehicle'))assert.throws(()=>g.useAoRFireControl(),/armazenamento/);
  else assert.throws(()=>action(g),/armazenamento/);
  assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 }
});
test('P176 tampered receipts, missing causal events and forged fire control are rejected',()=>{
 const {g}=fixture();bought(g);
 for(const modify of [s=>s.master.aorIntelActions[0].creditsSpent=0,
   s=>s.master.aorIntelActions[0].netSuccess=2,
   s=>s.events=s.events.filter(e=>e.type!=='AOR_INTEL_ACTION'),
   s=>s.master.aorIntelActions.push(copy(s.master.aorIntelActions[0]))]){
  const state=copy(g.state);modify(state);assert.throws(()=>validateState(state),/Recibo|recibo|informação|adulterado/);
 }
 const {g:ship}=fixture();ship.addVehicle('xwing_t65b');ship.beginVehicleCombat({targetProfileKey:'tie_ln'});ship.useAoRFireControl();
 const forged=copy(ship.state);forged.vehicles.combat.aorFireControl.round=999;
 assert.throws(()=>validateState(forged),/Fogo controlado/);
});
test('P176 Android bridge, UI and old P175 checkpoint migration are compatible',()=>{
 const {repo,g}=fixture();const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.aorP176Status().ok,true);assert.equal(bridge.useAoRBoughtInfo({actionId:'android',subjectId:'base',approved:true,difficulty:1}).ok,true);
 const old=copy(g.state);delete old.master.aorIntelActions;const migrated=migrateState(old);
 assert.deepEqual(migrated.master.aorIntelActions,[]);assert.equal(validateState(migrated),true);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const s of ['useAoRKnownSchematic','useAoRBoughtInfo','useAoRFireControl','aorP176Status'])assert.ok(bundle.includes(s),s);
 for(const s of ['PROJETO CONHECIDO','COMPRAR INFORMAÇÃO','FOGO CONTROLADO','SLICER CONSUMADO'])assert.ok(ui.includes(s),s);
});
for(const {id} of publishedAdventureCatalog()){
 test(`P176 ${id}: Bought Info does not overwrite UAE, three reopenings`,()=>{
  const {g,repo}=fixture();g.setCampaignMode('MIXED');g.startPublishedAdventureSession(id,{rng:()=>0.5});
  const before=copy(g.state.world.publishedAdventureRuntime);
  bought(g,'intel_'+id);assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
  for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.deepEqual(reopened.state.world.publishedAdventureRuntime,before);}
 });
}
function shipFixture(talents=names,{massive=2}={}){
 const {g,repo}=fixture(talents);g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Bocajarro'});
 const vehicles=copy(g.state.vehicles);vehicles.vehicles[vehicles.combat.targetVehicleId].massive=massive;g.commit('VEHICLES_UPDATED',{vehicles});
 return {g,repo};
}
test('P176 Massive 2 raises actual critical cost by 2; Exhaust Port spends Destiny to ignore it',()=>{
 const {g}=shipFixture();const c=copy(g.state.player.character);c.characteristics.agilidad=4;g.setCharacter(c);
 const destiny=copy(g.state.group);destiny.destiny.light=3;destiny.destiny.dark=0;g.commit('GROUP_UPDATED',{group:destiny});
 const before=g.state.group.destiny.light;
 const attack=g.resolveVehicleAttack({useExhaustPort:true,rng:()=>0.4});
 assert.equal(attack.massiveRating,2);assert.equal(attack.massiveIgnored,2);
 assert.equal(attack.criticalCost,Number(attack.weapon.crit));
 assert.equal(attack.exhaustPortUsed,true);assert.equal(g.state.group.destiny.light,before-1);
 assert.equal(validateState(g.state),true);
});
test('P176 Massive without Exhaust Port increases critical cost and never consumes Destiny',()=>{
 const {g}=shipFixture();const before=g.state.group.destiny.light;
 const attack=g.resolveVehicleAttack({rng:()=>0.4});
 assert.equal(attack.massiveRating,2);assert.equal(attack.massiveIgnored,0);
 assert.equal(attack.criticalCost,Number(attack.weapon.crit)+2);
 assert.equal(g.state.group.destiny.light,before);
});
test('P176 Dead to Rights applies Agility-based vehicle damage only on hit and flips Destiny',()=>{
 for(const [namesOwned,expected] of [[['IMPACTO DIRECTO'],2],[['IMPACTO DIRECTO','IMPACTO DIRECTO MEJORADO'],4]]){
  const {g}=shipFixture(namesOwned,{massive:0});const c=copy(g.state.player.character);c.characteristics.agilidad=4;g.setCharacter(c);
  const destiny=copy(g.state.group);destiny.destiny.light=3;destiny.destiny.dark=0;g.commit('GROUP_UPDATED',{group:destiny});
  const attack=g.resolveVehicleAttack({useDeadToRights:true,rng:()=>0.4});
  assert.equal(attack.deadToRightsUsed,attack.hit);
  assert.equal(attack.deadToRightsBonus,attack.hit?expected:0);
  assert.equal(g.state.group.destiny.light,3-Number(attack.hit));
  assert.equal(validateState(g.state),true);
 }
});
test('P176 Exhaust Port and Dead to Rights reject unearned talents and insufficient Destiny atomically',()=>{
 for(const mode of ['no-talent','no-destiny','no-massive']){
  const {g}=shipFixture(mode==='no-talent'?[]:names,{massive:mode==='no-massive'?0:2});
  if(mode==='no-destiny'){const group=copy(g.state.group);group.destiny.light=0;g.commit('GROUP_UPDATED',{group});}
  const before=copy(g.state);
  assert.throws(()=>g.resolveVehicleAttack({useExhaustPort:true,useDeadToRights:true,rng:()=>0.4}),/adquirida|insuficientes|Massive/);
  assert.deepEqual(g.state,before);
 }
});
test('P176 Dead to Rights miss does not consume light Destiny',()=>{
 const {g}=shipFixture(names,{massive:0});const group=copy(g.state.group);group.destiny.light=2;g.commit('GROUP_UPDATED',{group});
 const attack=g.resolveVehicleAttack({useDeadToRights:true,rng:()=>0});
 assert.equal(attack.hit,false);assert.equal(attack.deadToRightsUsed,false);assert.equal(attack.deadToRightsBonus,0);
 assert.equal(g.state.group.destiny.light,2);
});
test('P176 Fire Control expires on original ship even when active ship is switched',()=>{
 const {g}=shipFixture();const oldId=g.state.vehicles.activeVehicleId;
 g.useAoRFireControl();g.addVehicle('yt1300');
 assert.equal(validateState(g.state),true);
 g.nextVehicleRound();
 assert.equal(g.state.vehicles.vehicles[oldId].roundEffects.controlledFire,false);
 assert.equal(validateState(g.state),true);
});
