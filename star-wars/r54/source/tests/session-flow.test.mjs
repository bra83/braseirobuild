import test from 'node:test';
import assert from 'node:assert/strict';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createSessionRuntime, startSessionRuntime, setSessionPhase, noteSessionCheck, noteSessionCombatStarted, noteSessionCombatEnded, closeSessionRuntime } from '../app/src/main/assets/vtt/js/master/sessionFlow.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('fluxo de sessão local percorre fases sem se apresentar como regra do RPG',()=>{
  let s=createSessionRuntime(1);
  s=startSessionRuntime(s,{at:'2026-09-05T00:00:00Z'});
  assert.equal(s.status,'ativa'); assert.equal(s.phase,'abertura');
  s=setSessionPhase(s,'decisao');
  s=noteSessionCheck(s); assert.equal(s.phase,'consequencia'); assert.equal(s.checksResolved,1);
  s=noteSessionCombatStarted(s); assert.equal(s.phase,'conflito'); assert.equal(s.combatEncounters,1);
  s=noteSessionCombatEnded(s); assert.equal(s.phase,'recuperacao');
  s=closeSessionRuntime(s,{at:'2026-09-05T01:00:00Z',summary:'Teste concluído.'});
  assert.equal(s.status,'encerrada'); assert.equal(s.phase,'encerrada');
});

test('sessão completa persiste abertura, teste, combate, recuperação e encerramento',()=>{
  const storage=new MemoryStorage();
  const game=new GameController(new LocalStorageRepository(storage,'session'));
  game.loadStarterCharacter('arkhan');
  game.startSession();
  assert.equal(game.state.session.status,'ativa');
  game.submitIntent('Eu procuro sinais de soldados imperiais.');
  game.resolveSkillCheck({skillKey:'percepcion',difficulty:0,rng:()=>0.2});
  assert.equal(game.state.session.checksResolved,1);
  game.beginCombat();
  assert.equal(game.state.session.phase,'conflito');
  game.finishCombat();
  assert.equal(game.state.session.phase,'recuperacao');
  const closed=game.closeSession({summary:'Patrulha evitada e posição preservada.',xpAward:10});
  assert.equal(closed.session.status,'encerrada');
  assert.equal(closed.character.xp.available,10);
  assert.equal(game.state.sessionHistory.length,1);
  const reopened=new GameController(new LocalStorageRepository(storage,'session'));
  assert.equal(reopened.state.session.status,'encerrada');
  assert.equal(reopened.state.sessionHistory[0].summary,'Patrulha evitada e posição preservada.');
  const next=reopened.startSession();
  assert.equal(next.session.number,2);
});

test('encerramento não inventa PE quando nenhum valor é informado',()=>{
  const game=new GameController(new LocalStorageRepository(new MemoryStorage(),'no-xp'));
  game.loadStarterCharacter('jin-rio'); game.startSession();
  const before=game.state.player.character.xp.available;
  const result=game.closeSession();
  assert.equal(result.session.xpAwarded,0);
  assert.equal(result.character.xp.available,before);
});
