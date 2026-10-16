import test from 'node:test';
import assert from 'node:assert/strict';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState,migrateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {aorTalentCatalog,aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const clone=structuredClone, nodes=aorTalentCatalog();
const talent=name=>nodes.find(x=>x.originalName===name);
const commandNames=['COORDINACIÓN TÁCTICA','COORDINACIÓN TÁCTICA MEJORADA','LÍDER EXPERTO','ATAQUE COORDINADO'];
class Repo {constructor(){this.saved=createCampaign('P175');this.fail=false}load(){return clone(this.saved)}save(x){if(this.fail)throw Error('falha de disco P175');this.saved=clone(x)}}
function fixture(names=commandNames){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=clone(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=names.map(name=>{const n=talent(name);assert.ok(n);return {nodeId:n.id,originalName:n.originalName,name:n.name}});
 c.talents=names.map(name=>talent(name).name);c.characteristics.presencia=3;c.skills.liderazgo=3;
 g.setCharacter(c);g.setCampaignMode('AOR');g.startSession({rng:()=>0.4});
 const ally1=g.addCompanion({name:'Asha'}),ally2=g.addCompanion({name:'Dorn'});
 const combat=clone(g.state.combat);combat.active=true;combat.round=1;g.commit('COMBAT_UPDATED',{combat});
 return {repo,g,ids:[ally1.id??g.state.group.companions[0].id,ally2.id??g.state.group.companions[1].id]};
}
const success=()=>0.4;
test('P175 AoR command source purchase activation and no display-only privilege',()=>{
 for(const name of commandNames)assert.equal(talent(name).mechanicallyActive,true);
 assert.ok(aorTalentCoverage().mechanicalPositions>=335);
 const {g}=fixture([]);const c=clone(g.state.player.character);c.talents=['Coordenação tática','Líder especialista'];g.setCharacter(c);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'missing',companionIds:[g.state.group.companions[0].id],confirmedMedium:true}),/comprado/);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'liderazgo',useMasterLeader:true,rng:success}),/comprado/);
});
test('P175 Field Commander success: real Leadership check, ally strain, free maneuver, real cover setback',()=>{
 const {g,ids}=fixture();const before=g.state.group.companions[0].character.derived.strain.current;
 const result=g.useAoRFieldCommander({actionId:'p175-command',companionIds:ids,confirmedMedium:true,rng:success});
 assert.equal(result.duplicate,false);assert.equal(result.check.success,true);
 assert.equal(result.tokens.length,2);assert.equal(result.receipt.round,1);
 assert.equal(g.state.group.companions[0].character.derived.strain.current,before+1);
 assert.equal(validateState(g.state),true);
 const replay=g.useAoRFieldCommander({actionId:'p175-command',companionIds:ids,confirmedMedium:true,rng:success});
 assert.equal(replay.duplicate,true);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'p175-other',companionIds:ids,confirmedMedium:true,rng:success}),/já usada/);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'p175-command',companionIds:ids.slice(0,1),confirmedMedium:true}),/divergentes/);
 const cover=g.useAoRCommandedCover({companionId:ids[0]});assert.equal(cover.coverSetback,1);
 assert.throws(()=>g.useAoRCommandedCover({companionId:ids[0]}),/Nenhuma manobra/);
 const attack=g.resolveIncomingCompanionAttack({companionId:ids[0],rng:success});
 assert.equal(attack.coverSetback,1);assert.equal(validateState(g.state),true);
});
test('P175 Field Commander: no invented free action without Improved and Triumph',()=>{
 const {g,ids}=fixture(['COORDINACIÓN TÁCTICA']);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'forged',companionIds:ids,confirmedMedium:true,useImproved:true,rng:success}),/aprimorada não adquirida/);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'forged',companionIds:ids,confirmedMedium:true,grantFreeAction:true,rng:success}),/exige Coordenação/);
 assert.equal(g.state.combat.aorFieldCommandHistory,undefined);
});
test('P175 Master Leader spends 2 strain and reduces only Leadership, once per round',()=>{
 const {g}=fixture(['LÍDER EXPERTO']);const before=g.state.player.character.derived.strain.current;
 const check=g.resolveSkillCheck({skillKey:'liderazgo',difficulty:3,useMasterLeader:true,rng:success});
 assert.equal(check.masterLeader.used,true);assert.equal(check.masterLeader.strainCost,2);
 assert.equal(check.pool.difficulty,2);assert.equal(g.state.player.character.derived.strain.current,before+2);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'liderazgo',difficulty:3,useMasterLeader:true,rng:success}),/já usado/);
 assert.equal(validateState(g.state),true);
});
test('P175 Coordinated Assault: maneuver grants one automatic Advantage to engaged companion combat check',()=>{
 const {g,ids}=fixture(['ATAQUE COORDINADO']);
 const order=g.useAoRCoordinatedAssault({companionIds:ids,confirmedEngaged:true});
 assert.equal(order.companionIds.length,2);
 const first=g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'armas_distancia_ligeras',difficulty:1,rng:success});
 assert.equal(first.coordinatedAssaultAdvantage,1);
 const second=g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'armas_distancia_ligeras',difficulty:1,rng:success});
 assert.equal(second.coordinatedAssaultAdvantage,0);
 const other=g.resolveCompanionSkillCheck({companionId:ids[1],skillKey:'percepcion',difficulty:1,rng:success});
 assert.equal(other.coordinatedAssaultAdvantage,0);
 assert.equal(validateState(g.state),true);
});
test('P175 command transaction rollback on storage failure',()=>{
 const {g,repo,ids}=fixture();const stable=clone(g.state);repo.fail=true;
 assert.throws(()=>g.useAoRFieldCommander({actionId:'disk',companionIds:ids,confirmedMedium:true,rng:success}),/disco/);
 assert.deepEqual(g.state,stable);assert.deepEqual(repo.saved,stable);
});
test('P175 rejects tampered command history and unearned tokens after restore',()=>{
 const {g,ids}=fixture();g.useAoRFieldCommander({actionId:'p175-clean',companionIds:ids,confirmedMedium:true,rng:success});
 for(const mutate of [s=>s.combat.aorFieldCommandHistory[0].success=false,
    s=>s.combat.aorCommandTokens.push({companionId:'foreign',round:1,kind:'maneuver',used:false}),
    s=>s.combat.aorFieldCommandHistory[0].companionIds.push(ids[0])]){
  const forged=clone(g.state);mutate(forged);assert.throws(()=>validateState(forged),/comando|Manobra|Recibo/);
 }
});
test('P175 Android bridge has real command actions and restore path',()=>{
 const {repo,g,ids}=fixture();const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.aorCommandStatus().ok,true);
 const r=bridge.useAoRFieldCommander({actionId:'p175-bridge',companionIds:ids,confirmedMedium:true,rng:success});
 assert.equal(r.ok,true);assert.equal(r.data.receipt.success,true);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.equal(reopened.state.combat.aorFieldCommandHistory.length,1)}
});
for(const {id} of publishedAdventureCatalog()){
 test(`P175 ${id}: command action and three saves preserve published UAE`,()=>{
  const {g,repo,ids}=fixture();g.setCampaignMode('MIXED');g.startPublishedAdventureSession(id,{rng:()=>0.5});
  const before=clone(g.state.world.publishedAdventureRuntime);
  g.useAoRFieldCommander({actionId:'p175-'+id,companionIds:ids,confirmedMedium:true,rng:success});
  assert.deepEqual(g.state.world.publishedAdventureRuntime,before);
  for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true);assert.deepEqual(reopened.state.world.publishedAdventureRuntime,before)}
 });
}

test('P175 Brilliant Evasion: real opposed Piloting, once per encounter, enemy fire blocked for Agility rounds',()=>{
 const {g,repo}=fixture(['EVASIÓN IMPECABLE']);
 const c=clone(g.state.player.character);c.characteristics.agilidad=4;c.skills.pilotar_vehiculo_espacial=4;g.setCharacter(c);
 g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln',range:'Bocajarro'});
 const before=clone(g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId]);
 const result=g.useAoRBrilliantEvasion({npcAgility:2,npcPiloting:1,rng:success});
 assert.equal(result.success,true);assert.equal(result.untilRound,4);
 assert.throws(()=>g.useAoRBrilliantEvasion({rng:success}),/já tentada/);
 for(let round=1;round<=4;round++){
  assert.equal(g.state.vehicles.combat.round,round);
  const blocked=g.resolveIncomingVehicleAttack({rng:success});assert.equal(blocked.blocked,true);
  assert.deepEqual(g.state.vehicles.vehicles[g.state.vehicles.activeVehicleId],before);
  assert.equal(validateState(g.state),true);
  const reopened=new GameController(repo);assert.equal(reopened.state.vehicles.combat.brilliantEvasion.untilRound,4);
  if(round<4)g.nextVehicleRound();
 }
 g.nextVehicleRound();const attack=g.resolveIncomingVehicleAttack({rng:success});assert.notEqual(attack.blocked,true);
});
test('P175 Brilliant Evasion: failure consumes action/attempt and save failure rolls back',()=>{
 const {g,repo}=fixture(['EVASIÓN IMPECABLE']);g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 const before=clone(g.state);repo.fail=true;
 assert.throws(()=>g.useAoRBrilliantEvasion({rng:success}),/disco/);assert.deepEqual(g.state,before);
 repo.fail=false;const miss=g.useAoRBrilliantEvasion({npcAgility:6,npcPiloting:5,rng:()=>0});
 assert.equal(miss.success,false);assert.equal(g.state.vehicles.combat.brilliantEvasion,null);
 assert.throws(()=>g.useAoRBrilliantEvasion({rng:success}),/já tentada/);
});

test('P175 Situational Awareness: real allied Perception/Vigilance Boost near owned vehicle only',()=>{
 const {g,ids}=fixture(['CONOCIMIENTO DEL ENTORNO']);
 assert.throws(()=>g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'percepcion',situationalRange:'close',confirmedNearVehicle:true,rng:success}),/veículo aliado/);
 g.addVehicle('xwing_t65b');
 const near=g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'percepcion',situationalRange:'short',confirmedNearVehicle:true,rng:success});
 assert.equal(near.situationalAwarenessBoost,1);assert.equal(near.pool.boost,1);
 const close=g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'alerta',situationalRange:'close',confirmedNearVehicle:true,rng:success});
 assert.equal(close.situationalAwarenessBoost,2);assert.equal(close.pool.boost,2);
 assert.throws(()=>g.resolveCompanionSkillCheck({companionId:ids[0],skillKey:'mecanica',situationalRange:'close',confirmedNearVehicle:true,rng:success}),/Conhecimento do ambiente/);
 assert.equal(validateState(g.state),true);
});
test('P175 Clever Solution: one Cunning-based skill check per session, no display-only unlock, three restores',()=>{
 const {g,repo}=fixture(['SOLUCIÓN INGENIOSA']);
 const c=clone(g.state.player.character);c.characteristics.astucia=5;c.characteristics.intelecto=2;g.setCharacter(c);
 const check=g.resolveSkillCheck({skillKey:'mecanica',difficulty:1,useCleverSolution:true,rng:success});
 assert.equal(check.cleverSolution.characteristicKey,'astucia');assert.equal(check.pool.proficiency,Number(c.skills.mecanica));
 assert.equal(check.pool.ability+check.pool.proficiency,5);
 assert.throws(()=>g.resolveSkillCheck({skillKey:'percepcion',useCleverSolution:true,rng:success}),/uma vez/);
 for(let i=0;i<3;i++){const reopened=new GameController(repo);assert.equal(validateState(reopened.state),true)}
});

test('P175 no free Triumph reroll: failed Triumph spend still consumes Leadership action',()=>{
 const {g,ids}=fixture();const result=g.useAoRFieldCommander({actionId:'p175-no-triumph',companionIds:ids,
  confirmedMedium:true,useImproved:true,grantFreeAction:true,rng:success});
 assert.equal(result.check.triumph,0);assert.equal(result.receipt.freeActionGranted,false);
 assert.ok(result.tokens.every(t=>t.kind==='maneuver'));
 assert.equal(g.state.combat.playerTurn.actionUsed,true);
 assert.throws(()=>g.useAoRFieldCommander({actionId:'reroll',companionIds:ids,confirmedMedium:true,rng:success}),/já usada/);
 assert.equal(validateState(g.state),true);
});
test('P175 Brilliant Evasion: forged duration, foreign target, missing attempt rejected',()=>{
 const {g}=fixture(['EVASIÓN IMPECABLE']);g.addVehicle('xwing_t65b');g.beginVehicleCombat({targetProfileKey:'tie_ln'});
 g.useAoRBrilliantEvasion({npcAgility:1,npcPiloting:0,rng:success});
 for(const tamper of [s=>s.vehicles.combat.brilliantEvasion.untilRound=9999,
  s=>s.vehicles.combat.brilliantEvasion.targetVehicleId='forged',
  s=>s.vehicles.combat.brilliantEvasionAttempted=false]){
  const state=clone(g.state);tamper(state);assert.throws(()=>validateState(state),/Evasão impecável/);
 }
});

test('P175 offline Android bridge and PT-BR UI expose command, evasion and Cunning actions',async()=>{
 const {readFileSync}=await import('node:fs');
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bundle=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js',import.meta.url),'utf8');
 for(const key of ['aorCommandStatus','useAoRFieldCommander','useAoRCommandedCover',
  'useAoRCoordinatedAssault','useAoRBrilliantEvasion','useMasterLeader','useCleverSolution',
  'situationalAwarenessBoost'])assert.ok(bundle.includes(key),key);
 for(const key of ['P175 · COMANDO REBELDE AoR','COORDENAÇÃO TÁTICA','ATAQUE COORDENADO',
  'SOLUÇÃO ENGENHOSA','EVASÃO IMPECÁVEL','CONHECIMENTO DO AMBIENTE'])assert.ok(ui.includes(key),key);
 assert.match(ui,/R54 P\d+ · Android 1\.0\.\d+/);
});
