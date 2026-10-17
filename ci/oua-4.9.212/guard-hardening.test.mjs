// Tests for the independent RulesGuard command-bus entry point.
import test from 'node:test';
import assert from 'node:assert/strict';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
if (!process.env.OUA_MODULE_ROOT) throw new Error('OUA_MODULE_ROOT missing');
const url=pathToFileURL(path.join(process.env.OUA_MODULE_ROOT,'src/application/Tor2eRulesGuard.mjs')).href;
const {Tor2eRulesGuard}=await import(url);
const orc={id:'orc',name:'Orc',enduranceCurrent:20,endurance:20,targetNumber:14,parry:0,armorRating:1};
const ctx=()=>({combatants:[{...orc}]});
test('OUA GUARD-01: no caller-provided dice, attacker, weapon, skillRank in production',()=>{
 const g=new Tor2eRulesGuard({environment:'production',authorizeCombatAction:()=>({
   attacker:{cultureId:'hobbits',strengthBonus:0},
   weapon:{group:'Espadas',damage:3,injury:16},skillRank:2
 })});
 for(const p of [
   {mockFeatRolls:[12]}, {mockSuccessRolls:[6]}, {weapon:{damage:999}},
   {attacker:{strengthBonus:999}},{skillRank:30}
 ]) {
   assert.throws(()=>g.dispatch({commandId:'reject-'+Object.keys(p)[0],kind:'PLAYER_ACTION',
     payload:{actionType:'ATTACK',defender:{id:'orc'},...p}},ctx()),/proibido|forjado/i);
 }
 const invalid=g.dispatch({commandId:'ghost-attack',kind:'PLAYER_ACTION',
   payload:{actionType:'ATTACK',defender:{id:'phantom'}}},ctx());
 assert.equal(invalid.status,'INVALID_TARGET');
});
test('OUA GUARD-02: unauthorized caller cannot claim GM role',()=>{
 const g=new Tor2eRulesGuard({environment:'production'});
 g.rag.query=({userRole})=>({seenRole:userRole});
 const out=g.dispatch({commandId:'help-1',kind:'GM_HELP',
   payload:{query:'hidden',userRole:'GM'}},{});
 assert.equal(out.status,'NO_WORLD_CHANGE');
 assert.equal(out.helpResponse.seenRole,'PLAYER');
});
test('OUA GUARD-03: duplicate action is rejected even after save and reopen',()=>{
 const command={commandId:'durable-guard-001',kind:'PLAYER_ACTION',
   payload:{actionType:'GENERIC_LOG',text:'test'}};
 const g=new Tor2eRulesGuard({environment:'production'});
 const first=g.dispatch(command,{});
 assert.equal(first.status,'APPLIED');
 assert.deepEqual(first.state.ouaExecutedCommandIds,['durable-guard-001']);
 const reopened=new Tor2eRulesGuard({environment:'production'});
 const second=reopened.dispatch(command,structuredClone(first.state));
 assert.equal(second.status,'DUPLICATE_REJECTED');
 assert.deepEqual(second.state,first.state);
});
test('OUA GUARD-04: single ingress dispatches adventure lifecycle with host-authorized sheet and atomic rollback on save failure',()=>{
 const hostSheet={skills:[{name:'Caçada',rank:3},{name:'Viagem',rank:3},{name:'Vigilância',rank:2},{name:'Furtividade',rank:2},{name:'Cortesia',rank:2},{name:'Enigma',rank:2},{name:'Persuasão',rank:2}]};
 const savedSnapshots=[];
 let failSave=false;
 const g=new Tor2eRulesGuard({
   environment:'production',
   authorizedCharacterSheet:hostSheet,
   persistenceStore:{
     save(state,meta){
       if(failSave==='sync') throw new Error('disk full');
       if(failSave==='async') return Promise.resolve();
       savedSnapshots.push({state:structuredClone(state),meta});
       return true;
     }
   }
 });
 const start=g.dispatch({commandId:'adv-start-1',kind:'PLAYER_ACTION',payload:{actionType:'START_ADVENTURE',adventureId:'tales-01-troll-hole'}},{character:{fatigue:0,shadow:0,hope:10}});
 assert.equal(start.status,'APPLIED');
 assert.equal(start.state.ouaAdventureSnapshot.activeAdventureId,'tales-01-troll-hole');
 assert.throws(()=>g.dispatch({commandId:'adv-forged-sheet',kind:'PLAYER_ACTION',payload:{actionType:'PERFORM_SCENE_CHECK',skill:'Caçada',authorizedCharacterSheet:{skills:[{name:'Caçada',rank:6}]}}},start.state),/autorizada|diverg/i);
 const checkId=start.state.ouaAdventureView.checks[0].checkId;
 const skill=start.state.ouaAdventureView.checks[0].skill;
 const check=g.dispatch({commandId:'adv-check-1',kind:'PLAYER_ACTION',payload:{actionType:'PERFORM_SCENE_CHECK',skill,checkId}},start.state);
 assert.equal(check.status,'APPLIED');
 assert.ok(check.state.ouaExecutedCommandIds.includes('adv-check-1'));
 const replay=new Tor2eRulesGuard({environment:'production',authorizedCharacterSheet:hostSheet}).dispatch({commandId:'adv-check-1',kind:'PLAYER_ACTION',payload:{actionType:'PERFORM_SCENE_CHECK',skill,checkId}},structuredClone(check.state));
 assert.equal(replay.status,'DUPLICATE_REJECTED');
 failSave='sync';
 assert.throws(()=>g.dispatch({commandId:'adv-fail-save',kind:'PLAYER_ACTION',payload:{actionType:'GENERIC_LOG',text:'boom'}},check.state),/rollback atômico/i);
 assert.equal(g.executedCommandIds.has('adv-fail-save'),false);
 failSave='async';
 assert.throws(()=>g.dispatch({commandId:'adv-async-save',kind:'PLAYER_ACTION',payload:{actionType:'GENERIC_LOG',text:'boom-async'}},check.state),/Promise não é persistência atômica síncrona/i);
 assert.equal(g.executedCommandIds.has('adv-async-save'),false);
});
