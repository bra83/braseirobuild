import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { LocalStorageRepository, R21_STATE_CHANGED_EVENT } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';

class MemoryStorage {
  constructor(){ this.m = new Map(); }
  getItem(k){ return this.m.has(k) ? this.m.get(k) : null; }
  setItem(k,v){ this.m.set(k,String(v)); }
  removeItem(k){ this.m.delete(k); }
}
class FakeCustomEvent {
  constructor(type, init={}) { this.type=type; this.detail=init.detail; }
}
class EventCollector {
  constructor(){ this.events=[]; this.CustomEvent=FakeCustomEvent; }
  dispatchEvent(event){ this.events.push(event); return true; }
}

test('repository emits the exact R46 state-changed event after save and clear',()=>{
  const storage=new MemoryStorage();
  const target=new EventCollector();
  const repo=new LocalStorageRepository(storage,'braseiro.starwars.edge.state.v1',target);
  repo.save(createCampaign('Teste'));
  repo.clear();
  assert.equal(R21_STATE_CHANGED_EVENT,'braseiro:r21-state-changed');
  assert.deepEqual(target.events.map(e=>[e.type,e.detail?.operation,e.detail?.key]),[
    [R21_STATE_CHANGED_EVENT,'save','braseiro.starwars.edge.state.v1'],
    [R21_STATE_CHANGED_EVENT,'clear','braseiro.starwars.edge.state.v1']
  ]);
});

test('a real GameController mutation emits same-window refresh events',()=>{
  const storage=new MemoryStorage();
  const target=new EventCollector();
  const repo=new LocalStorageRepository(storage,'state',target);
  const game=new GameController(repo);
  const afterConstructor=target.events.length;
  game.updateGroupBase({name:'Eco',location:'Dantooine'});
  assert.ok(afterConstructor>=1);
  assert.ok(target.events.length>afterConstructor);
  assert.equal(target.events.at(-1).type,R21_STATE_CHANGED_EVENT);
  assert.equal(target.events.at(-1).detail.operation,'save');
});

test('R46 listens to the same custom event used by the repository',()=>{
  const source=fs.readFileSync(new URL('../shell-r46-run87/VTTApp.tsx',import.meta.url),'utf8');
  const bundle=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/r46-shell.bundle.js',import.meta.url),'utf8');
  assert.match(source,/addEventListener\(R21_STATE_CHANGED_EVENT, onStateChanged\)/);
  assert.match(bundle,/braseiro:r21-state-changed/);
  assert.match(bundle,/addEventListener\(r21Projection_1\.R21_STATE_CHANGED_EVENT, onStateChanged\)/);
});
