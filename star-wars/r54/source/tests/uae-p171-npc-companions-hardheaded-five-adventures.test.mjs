/** P171: NPC initiative targeting, protected companions, next-turn recovery and published UAE saves. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {AOR_TALENT_TREES} from '../app/src/main/assets/vtt/js/expansion/aorTalentTreesData.mjs';
import {publishedAdventureCatalog} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const clone=structuredClone;
const nodes=Object.values(AOR_TALENT_TREES).flatMap(ss=>Object.values(ss).flatMap(t=>t.nodes));
const owned=nodes.filter(n=>['TENACIDAD','TENACIDAD MEJORADA','GUARDAESPALDAS'].includes(n.originalName));
const fixed=()=>.5;
class Repo {
 constructor(){this.saved=createCampaign('P171');this.fail=false;this.writes=0}
 load(){return clone(this.saved)}
 save(s){if(this.fail)throw new Error('disk failure P171');this.saved=clone(s);this.writes++}
}
function game({talents=owned}={}){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');
 const c=clone(g.state.player.character);c.creation={...c.creation,rulesProfile:'AOR_CORE'};
 c.talentPurchases=talents.map(n=>({nodeId:n.id,name:n.name,originalName:n.originalName,source:n.source}));
 c.talents=talents.map(n=>n.name);g.setCharacter(c);g.setCampaignMode('AOR');
 return {g,repo};
}
function enemy({stun=false}={}){
 const npc=createAdversary('imperial_officer',{range:'Curto'});
 if(stun){npc.weapons[0].damageType='strain';npc.weapons[0].damage=40}
 return npc;
}
function engage(g,{stun=false}={}){
 const r=g.beginEncounter({adversaries:[enemy({stun})],rng:fixed});
 assert.equal(g.state.combat.active,true);
 return r;
}
test('P171: targeting requires a real initiative encounter and a real conscious companion',()=>{
 const {g}=game();const ally=g.addCompanion({name:'Soldado'});
 assert.throws(()=>g.setNpcCompanionTarget({companionId:ally.id}),/iniciativa ativo/);
 engage(g);assert.throws(()=>g.setNpcCompanionTarget({companionId:'forged'}),/não pertence/);
 assert.throws(()=>g.setNpcCompanionTarget({companionId:7}),/inválido/);
 assert.equal(g.setNpcCompanionTarget({companionId:ally.id}).companionId,ally.id);
 assert.equal(g.setNpcCompanionTarget({companionId:null}).companionId,null);
});
test('P171: real NPC slot targets companion; damage, log, UI and SQLite-style save are atomic',()=>{
 const {g,repo}=game();const ally=g.addCompanion({name:'Batedora'});engage(g);
 g.setNpcCompanionTarget({companionId:ally.id});const before=clone(g.state);
 repo.fail=true;
 assert.throws(()=>g.finishPlayerInitiativeTurn({rng:fixed}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
 repo.fail=false;
 const result=g.finishPlayerInitiativeTurn({rng:fixed});
 const attacks=result.combat.log.filter(e=>e.type==='TURNO_NPC_COMPANHEIRO');
 assert.equal(attacks.length,1);assert.equal(attacks[0].companionId,ally.id);
 assert.equal(attacks[0].bodyGuardUpgrades,0);
 assert.ok(g.state.group.companions[0].character.derived.wounds.current>0);
 assert.ok(result.npcTurns.some(x=>x.text.includes('Batedora')));
 assert.equal(validateState(g.state),true);
 assert.deepEqual(new GameController(repo).state,g.state);
});
test('P171: GM targeting honors Body Guard purchased rank and expires at following player turn',()=>{
 const {g}=game();const ally=g.addCompanion({name:'Guardado'});engage(g);
 g.useBodyGuard({companionId:ally.id,strain:2,confirmedEngaged:true});
 g.setNpcCompanionTarget({companionId:ally.id});
 const r=g.finishPlayerInitiativeTurn({rng:fixed});
 const log=r.combat.log.find(e=>e.type==='TURNO_NPC_COMPANHEIRO');
 assert.equal(log.bodyGuardUpgrades,2);assert.equal(log.check.pool.challenge,1);assert.equal(log.check.pool.difficulty,1);
 assert.equal(g.state.combat.bodyGuard,undefined);
});
test('P171: companion ranged defense adds setback to the NPC initiative pool',()=>{
 const {g}=game();const ally=g.addCompanion({name:'Defensora'});engage(g);
 const group=clone(g.state.group);group.companions[0].character.derived.defense.ranged=2;
 g.commit('GROUP_UPDATED',{group});g.setNpcCompanionTarget({companionId:ally.id});
 const r=g.finishPlayerInitiativeTurn({rng:fixed});
 const attack=r.combat.log.find(e=>e.type==='TURNO_NPC_COMPANHEIRO');
 assert.ok(attack.check.pool.setback>=2);
 assert.equal(validateState(g.state),true);
});
test('P171: source-backed Body Guard cannot be forged via a copied display label',()=>{
 const {g}=game({talents:[]});const ally=g.addCompanion({name:'Recruta'});
 const c=clone(g.state.player.character);c.talents=['Guarda-costas'];g.setCharacter(c);engage(g);
 assert.throws(()=>g.useBodyGuard({companionId:ally.id,strain:1,confirmedEngaged:true}),/excede|adquirido/);
 g.setNpcCompanionTarget({companionId:ally.id});g.finishPlayerInitiativeTurn({rng:fixed});
 assert.equal(g.state.combat.log.find(e=>e.type==='TURNO_NPC_COMPANHEIRO').bodyGuardUpgrades,0);
});
test('P171: GM target automatically clears when companion falls incapacitated',()=>{
 const {g}=game();const ally=g.addCompanion({name:'Alvo'});engage(g);
 const c=clone(g.state.group);c.companions[0].character.derived.wounds.current=c.companions[0].character.derived.wounds.threshold;
 g.commit('GROUP_UPDATED',{group:c});g.setNpcCompanionTarget({companionId:ally.id});
 g.finishPlayerInitiativeTurn({rng:fixed});
 assert.equal(g.state.combat.npcCompanionTargetId,undefined);
 assert.equal(validateState(g.state),true);
});
test('P171: no display-name spoofing creates recovery opportunity',()=>{
 const {g}=game({talents:[]});const c=clone(g.state.player.character);
 c.talents=['Tenacidade aprimorada'];g.setCharacter(c);
 engage(g,{stun:true});
 assert.equal(g.state.combat.encounter.outcome,'player_incapacitated');
 assert.equal(g.state.combat.hardHeadedRecovery,undefined);
 assert.throws(()=>g.useHardHeaded({improved:true,rng:fixed}),/não foi adquirida|posição de iniciativa/);
});
test('P171: improved Hard Headed grants one next-player-slot window and clears on success',()=>{
 const {g,repo}=game();engage(g,{stun:true});
 assert.equal(g.state.combat.hardHeadedRecovery.ready,true);
 assert.equal(g.state.combat.encounter.waitingForPlayer,true);
 assert.equal(g.state.combat.encounter.outcome,undefined);
 const saved=clone(repo.saved);assert.equal(validateState(saved),true);
 const reopened=new GameController(repo);assert.deepEqual(reopened.state,saved);
 const recovered=reopened.useHardHeaded({improved:true,rng:()=>.49});
 assert.equal(recovered.success,true);assert.equal(reopened.state.combat.hardHeadedRecovery,undefined);
 assert.ok(reopened.state.player.character.derived.strain.current<=reopened.state.player.character.derived.strain.threshold);
 assert.equal(validateState(reopened.state),true);
});
test('P171: recovery cannot be used before the following slot or twice',()=>{
 const {g}=game();engage(g,{stun:true});
 const c=clone(g.state.combat);c.hardHeadedRecovery.ready=false;
 g.commit('COMBAT_UPDATED',{combat:c});
 assert.throws(()=>g.useHardHeaded({improved:true,rng:fixed}),/turno seguinte/);
 c.hardHeadedRecovery.ready=true;g.commit('COMBAT_UPDATED',{combat:c});
 g.useHardHeaded({improved:true,rng:()=>.49});
 assert.throws(()=>g.useHardHeaded({improved:true,rng:fixed}),/incapacitação|turno seguinte/);
});
test('P171: failed recovery resolves encounter defeat and survives 3 restores',()=>{
 const {g,repo}=game();engage(g,{stun:true});
 const fail=g.useHardHeaded({improved:true,rng:()=>.9999});
 assert.equal(fail.success,false);
 assert.equal(g.state.combat.encounter.outcome,'player_incapacitated');
 assert.equal(g.state.combat.hardHeadedRecovery.attempted,true);
 assert.throws(()=>g.useHardHeaded({improved:true,rng:()=>.49}),/posição de iniciativa|turno seguinte/);
 const state=clone(g.state);
 for(let i=0;i<3;i++){const opened=new GameController(repo);assert.equal(validateState(opened.state),true);assert.deepEqual(opened.state,state)}
});
test('P171: recovery check is transactional and does not consume action on failed disk write',()=>{
 const {g,repo}=game();engage(g,{stun:true});const before=clone(g.state);
 repo.fail=true;assert.throws(()=>g.useHardHeaded({improved:true,rng:fixed}),/disk failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,before);
});
test('P171: reject forged NPC target and invalid recovery marker in imported checkpoints',()=>{
 const {g}=game();engage(g);
 const bad=clone(g.state);bad.combat.npcCompanionTargetId='forged';
 assert.throws(()=>validateState(bad),/Alvo NPC/);
 const bad2=clone(g.state);bad2.combat.hardHeadedRecovery={ready:'yes',attempted:false,incapacitatedRound:1};
 assert.throws(()=>validateState(bad2),/Tenacidade aprimorada inválida/);
});
test('P171: bridge, shell, canonical bundle and Android 1.0.171 expose real GM target selection',()=>{
 const {repo,g}=game();const ally=g.addCompanion({name:'Escolta'});engage(g);
 const bridge=new BraseiroUiBridge({repository:repo,storage:null});
 assert.equal(bridge.setNpcCompanionTarget({companionId:ally.id}).ok,true);
 assert.equal(bridge.aorP170Status().data.npcCompanionTargetId,ally.id);
 const ui=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 assert.ok(ui.includes('setNpcCompanionTarget'));
 const shell=readFileSync(new URL('../app/src/main/assets/vtt/r46-shell.html',import.meta.url),'utf8');
 assert.ok(Number(shell.match(/data-version="1\.0\.(\d+)"/)?.[1])>=171);
});
for(const entry of publishedAdventureCatalog()){
 const id=entry.id;
 test(`P171 published adventure ${id}: GM target, NPC slot, save and three restores`,()=>{
  const {g,repo}=game();g.setCampaignMode('MIXED');
  g.executePublishedAdventureCommand({actionId:`p171:${id}:start`,kind:'start',adventureId:id});
  const ally=g.addCompanion({name:'Rebelde de apoio'});engage(g);
  g.setNpcCompanionTarget({companionId:ally.id});
  g.finishPlayerInitiativeTurn({rng:fixed});
  const stable=clone(repo.saved);
  assert.equal(validateState(stable),true);
  assert.ok(stable.combat.log.some(e=>e.type==='TURNO_NPC_COMPANHEIRO'));
  for(let i=0;i<3;i++){const opened=new GameController(repo);assert.equal(validateState(opened.state),true);assert.deepEqual(opened.state,stable)}
 });
}
