import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {addStarterCritical} from '../app/src/main/assets/vtt/js/rules/critical.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const cp=structuredClone,all=aorTalentCatalog();
const names=['MANO DE SANTO','INGENIOSO','SOLUCIÓN IMPROVISADA','IMPARABLE','GOLPE INCAPACITANTE'];
class Repo{constructor(){this.saved=createCampaign('P178');this.fail=false;}load(){return cp(this.saved);}save(s){if(this.fail)throw Error('falha de armazenamento P178');this.saved=cp(s);}}
function fixture(owned=names,{combat=false}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=cp(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=owned.map(n=>{const node=all.find(t=>t.originalName===n);assert.ok(node,n);return {nodeId:node.id,originalName:n,name:node.name};});
 c.talents=owned.map(n=>all.find(t=>t.originalName===n).name);
 c.characteristics.intelecto=5;c.characteristics.presencia=4;c.characteristics.agilidad=3;c.characteristics.fortaleza=3;
 c.skills.mecanica=2;c.skills.negociacion=2;c.skills.atletismo=2;c.skills.armas_distancia_ligeras=3;
 c.weapons=[{id:'blaster',name:'Blaster',skill:'armas_distancia_ligeras',range:'Curto',damage:6,crit:3}];
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.4});
 if(combat){const state=cp(g.state.combat);state.active=true;state.round=1;state.adversaries=[createAdversary('imperial_soldier',{range:'Curto'})];g.commit('COMBAT_UPDATED',{combat:state});}
 return {g,repo};
}
const variants=[
 ['MANO DE SANTO','useWorksLikeACharm','WORKS_LIKE_A_CHARM','presencia','worksLikeACharm'],
 ['INGENIOSO','useStrokeOfGenius','STROKE_OF_GENIUS','intelecto','strokeOfGenius'],
 ['SOLUCIÓN IMPROVISADA','useQuickFix','QUICK_FIX','agilidad','quickFix']
];
test('P178 five distinct AoR source slots have actual consumers',()=>{
 for(const n of names)assert.ok(all.find(t=>t.originalName===n&&t.mechanicallyActive),n);
 assert.ok(aorTalentCoverage().mechanicalPositions>=356);
});
for(const [name,flag,canonical,attribute,sessionKey] of variants){
 test(`P178 ${name} substitutes the live dice pool, once/session, survives three reopens`,()=>{
  const {g,repo}=fixture();const r=g.resolveSkillCheck({skillKey:'atletismo',difficulty:2,[flag]:true,rng:()=>0.4});
  assert.equal(r.aorAttributeSubstitution.talent,canonical);assert.equal(r.aorAttributeSubstitution.characteristicKey,attribute);
  assert.equal(r.pool.ability+r.pool.proficiency,Math.max(g.state.player.character.characteristics[attribute],g.state.player.character.skills.atletismo));
  assert.equal(g.state.player.character.sessionEffects.talentUses[sessionKey],true);
  for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
  const before=cp(g.state);assert.throws(()=>g.resolveSkillCheck({skillKey:'atletismo',[flag]:true,rng:()=>0.4}),/uma vez/);assert.deepEqual(g.state,before);
 });
 test(`P178 ${name} refuses unowned talent and rolls back on failed save`,()=>{
  const {g}=fixture([]),before=cp(g.state);assert.throws(()=>g.resolveSkillCheck({skillKey:'atletismo',[flag]:true,rng:()=>0.4}),/não adquirido/);assert.deepEqual(g.state,before);
  const {g:owned,repo}=fixture();const old=cp(owned.state);repo.fail=true;
  assert.throws(()=>owned.resolveSkillCheck({skillKey:'atletismo',[flag]:true,rng:()=>0.4}),/armazenamento/);
  assert.deepEqual(owned.state,old);assert.deepEqual(repo.saved,old);
 });
}
test('P178 rejects multiple characteristic substitutions without charging actions',()=>{
 const {g}=fixture();const before=cp(g.state);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'atletismo',useWorksLikeACharm:true,useStrokeOfGenius:true,rng:()=>0.4}),/duas vezes/);
 assert.deepEqual(g.state,before);
});
test('P178 Unstoppable suppresses an actual critical total of 1, including immediate strain',()=>{
 const {g}=fixture();const c=cp(g.state.player.character),old=cp(c);
 const result=addStarterCritical(c,'Ataque de teste',{roll:1,rng:()=>0});
 assert.equal(result.prevented,true);assert.equal(result.added,false);assert.deepEqual(result.character,old);
 const without=cp(c);without.talentPurchases=without.talentPurchases.filter(p=>p.originalName!=='IMPARABLE');
 const unprotected=addStarterCritical(without,'Ataque de teste',{roll:1,rng:()=>0});
 assert.equal(unprotected.added,true);assert.equal(unprotected.injury.code,'minor_nick');
});
test('P178 Unstoppable does not suppress total above 1 or unowned',()=>{
 const {g}=fixture();const c=cp(g.state.player.character);
 assert.equal(addStarterCritical(c,'Teste',{roll:2,rng:()=>0}).added,true);
 c.criticalInjuries=[{id:'old',active:true}];
 assert.equal(addStarterCritical(c,'Teste',{roll:1,rng:()=>0}).added,true);
});
test('P178 Crippling Blow changes real attack difficulty and wounds real target',()=>{
 const {g,repo}=fixture(names,{combat:true});const id=g.state.combat.adversaries[0].id;
 const result=g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:id,useCripplingBlow:true,rng:()=>0.4});
 assert.equal(result.difficulty,2);assert.equal(result.pendingSpend.attackSnapshot.cripplingBlow,true);
 if(result.hit&&result.targetDamage.suffered>0){
  const npc=g.state.combat.adversaries[0];assert.equal(npc.aorCripplingBlow.strainPerMove,1);
  assert.equal(npc.aorCripplingBlow.sourceAttackId,result.attackId);
 }
 assert.equal(validateState(g.state),true);assert.equal(validateState(new GameController(repo).state),true);
});
test('P178 Crippling Blow rejects unowned and phantom targets atomically',()=>{
 for(const [owned,target] of [[[],null],[names,'phantom']]){
  const {g}=fixture(owned,{combat:true});const before=cp(g.state);
  assert.throws(()=>g.resolveWeaponAttack({weaponIndex:0,targetAdversaryId:target,useCripplingBlow:true,rng:()=>0.4}));
  assert.deepEqual(g.state,before);
 }
});
test('P178 P177 idempotency cannot silently accept a different GM-approved fact',()=>{
 const {g}=fixture(['METICULOSO']);g.useAoRP177Action({kind:'careful_planning',actionId:'care_178',subjectId:'scene',approved:true,fact:'A porta existe'});
 const before=cp(g.state);
 assert.throws(()=>g.useAoRP177Action({kind:'careful_planning',actionId:'care_178',subjectId:'scene',approved:true,fact:'A porta não existe'}),/divergentes/);
 assert.deepEqual(g.state,before);
});
test('P178 offline bridge includes canonical consumers',()=>{
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const s of ['useWorksLikeACharm','useStrokeOfGenius','useQuickFix','UNSTOPPABLE','CRIPPLING_BLOW'])assert.ok(bundle.includes(s),s);
});
for(const {id} of publishedAdventureCatalog())test(`P178 ${id} attribute use is persistent and adventure state unchanged after 3 reopens`,()=>{
 const {g,repo}=fixture();g.setCampaignMode('MIXED');g.startPublishedAdventureSession(id,{rng:()=>0.5});
 const before=cp(g.state.world.publishedAdventureRuntime);
 g.resolveSkillCheck({skillKey:'atletismo',difficulty:2,useStrokeOfGenius:true,rng:()=>0.4});
 assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.deepEqual(reopened.state.world.publishedAdventureRuntime,before);}
});
test('P178 Form on Me propagates real Gain the Advantage to a confirmed nearby allied ship',()=>{
 const {g,repo}=fixture([...names,'FORMACIÓN DE COMBATE']);
 const c=cp(g.state.player.character);c.skills.liderazgo=2;g.setCharacter(c);
 g.addVehicle('xwing_t65b',{id:'leader'});g.addVehicle('xwing_t65b',{id:'wingman',setActive:false});
 g.selectVehicle('leader');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const vehicles=cp(g.state.vehicles);vehicles.vehicles.leader.speed=4;vehicles.vehicles.wingman.speed=4;
 vehicles.vehicles.wingman.alliedToPlayer=true;vehicles.vehicles.wingman.closeToActive=true;
 g.commit('VEHICLES_UPDATED',{vehicles});
 const result=g.gainVehicleAdvantage({alliedVehicleIds:['wingman'],rng:()=>0.4});
 assert.equal(result.success,true);assert.deepEqual(result.formationAllies,['wingman']);
 const leader=g.state.vehicles.vehicles.leader,ally=g.state.vehicles.vehicles.wingman;
 assert.equal(ally.roundEffects.gainAdvantage.targetVehicleId,g.state.vehicles.combat.targetVehicleId);
 assert.equal(ally.roundEffects.gainAdvantage.formationLeaderVehicleId,'leader');
 assert.equal(leader.roundEffects.gainAdvantage.targetVehicleId,ally.roundEffects.gainAdvantage.targetVehicleId);
 assert.equal(validateState(g.state),true);
 for(let i=0;i<3;i++)assert.equal(validateState(new GameController(repo).state),true);
});
test('P178 Form on Me rejects phantom, unconfirmed and unowned allied ships atomically',()=>{
 for(const [owned,allyId] of [[names,'wingman'],[[...names,'FORMACIÓN DE COMBATE'],'fake'],[[...names,'FORMACIÓN DE COMBATE'],'wingman']]){
  const {g}=fixture(owned);const c=cp(g.state.player.character);c.skills.liderazgo=2;g.setCharacter(c);
  g.addVehicle('xwing_t65b',{id:'leader'});g.addVehicle('xwing_t65b',{id:'wingman',setActive:false});
  g.selectVehicle('leader');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
  const v=cp(g.state.vehicles);v.vehicles.leader.speed=4;v.vehicles.wingman.speed=4;
  v.vehicles.wingman.alliedToPlayer=owned===names;v.vehicles.wingman.closeToActive=true;
  g.commit('VEHICLES_UPDATED',{vehicles:v});const before=cp(g.state);
  assert.throws(()=>g.gainVehicleAdvantage({alliedVehicleIds:[allyId],rng:()=>0.4}));assert.deepEqual(g.state,before);
 }
});
test('P178 GM confirms only registered formation allies and rolls back on storage failure',()=>{
 const {g,repo}=fixture([...names,'FORMACIÓN DE COMBATE']);
 g.addVehicle('xwing_t65b',{id:'leader'});g.addVehicle('xwing_t65b',{id:'wingman',setActive:false});
 g.selectVehicle('leader');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const before=cp(g.state);
 assert.throws(()=>g.confirmAoRFormationAlly({vehicleId:'wingman',approved:false,closeToActive:true}),/Mestre/);
 assert.throws(()=>g.confirmAoRFormationAlly({vehicleId:'ghost',approved:true,closeToActive:true}),/registrado/);
 assert.deepEqual(g.state,before);
 repo.fail=true;assert.throws(()=>g.confirmAoRFormationAlly({vehicleId:'wingman',approved:true,closeToActive:true}),/armazenamento/);
 assert.deepEqual(g.state,before);repo.fail=false;
 const r=g.confirmAoRFormationAlly({vehicleId:'wingman',approved:true,closeToActive:true});
 assert.equal(r.alliedToPlayer,true);assert.equal(g.state.vehicles.vehicles.wingman.closeToActive,true);
 assert.equal(validateState(new GameController(repo).state),true);
});
test('P178 Crippling Blow inflicts actual NPC strain on movement and not on idle turns',()=>{
 const {g}=fixture(names,{combat:true});const npc=createAdversary('imperial_officer',{range:'Curto'});npc.weapons=[];
 npc.aorCripplingBlow={sourceAttackId:'attack_p178',encounterRound:1,strainPerMove:1};
 npc.statusEffects=[{type:'prone',duration:'rounds',rounds:2,source:'talent'}];
 const combat=cp(g.state.combat);combat.adversaries=[npc];
 const old=Number(npc.wounds.current);
 const moved=g._resolveNpcTurn(combat,g.state.player.character,npc.id,()=>0.4,g.state.group);
 const after=moved.combat.adversaries.find(x=>x.id===npc.id);
 assert.equal(after.wounds.current,old+1); // Rivals convert strain to wounds per FFG.
 assert.equal(after.aorCripplingBlow.strainPerMove,1);
});
test('P178 forged critical movement and formation references rejected on reload',()=>{
 const {g}=fixture(names,{combat:true});
 const forged=cp(g.state);forged.combat.adversaries[0].aorCripplingBlow={sourceAttackId:'fake',encounterRound:0,strainPerMove:900};
 assert.throws(()=>validateState(forged),/incapacitante/);assert.throws(()=>migrateState(forged),/incapacitante/);
 const invalid=cp(g.state);invalid.vehicles.vehicles.fake={id:'fake',name:'Fake',roundEffects:{gainAdvantage:{targetVehicleId:'target',formationLeaderVehicleId:'nonexistent'}},alliedToPlayer:true,closeToActive:true};
 assert.throws(()=>validateState(invalid),/Formação/);
});
test('P178 Unstoppable wound-threshold crossing does not fabricate criticalAdded',async()=>{
 const {applyDamage}=await import('../app/src/main/assets/vtt/js/rules/health.mjs');
 const {g}=fixture(['IMPARABLE']);const c=cp(g.state.player.character);
 c.derived.wounds.current=c.derived.wounds.threshold;
 const hit=applyDamage(c,{damage:20,pierce:10,criticalRng:()=>0});
 assert.equal(hit.criticalAdded,false);assert.equal(hit.criticalInjury,null);
 assert.equal(hit.character.criticalInjuries.length,c.criticalInjuries.length);
 assert.ok(hit.character.derived.wounds.current>c.derived.wounds.threshold);
});
