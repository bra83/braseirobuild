import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalStorageRepository, exportStateJson, importStateJson, R21_STATE_CHANGED_EVENT } from '../app/src/main/assets/vtt/js/core/storage.mjs';
import { LocalSnapshotRepository } from '../app/src/main/assets/vtt/js/core/snapshots.mjs';

class MemoryStorage {
  constructor(){ this.map = new Map(); }
  getItem(k){ return this.map.has(k) ? this.map.get(k) : null; }
  setItem(k,v){ this.map.set(k,String(v)); }
  removeItem(k){ this.map.delete(k); }
}
class FakeCustomEvent {
  constructor(type, options={}){ this.type = type; this.detail = options.detail; }
}
class FakeEventTarget {
  constructor(){ this.CustomEvent = FakeCustomEvent; this.events=[]; }
  dispatchEvent(event){ this.events.push(event); return true; }
}

function stateNamed(name){
  const s=createCampaign(name);
  s.scene.narration=`Narração ${name}`;
  return s;
}

test('backup -> mutação -> import restaura estado e emite refresh na mesma WebView', () => {
  const storage=new MemoryStorage();
  const target=new FakeEventTarget();
  const repo=new LocalStorageRepository(storage,'state-key',target);
  const snaps=new LocalSnapshotRepository(storage,'snap-key',10);

  const a=stateNamed('Campanha A');
  repo.save(a);
  target.events.length=0;
  const backup=exportStateJson(repo.load());

  const b=stateNamed('Campanha B');
  repo.save(b);
  const beforeRestore=repo.load();
  assert.equal(beforeRestore.name,'Campanha B');
  const snap=snaps.create(beforeRestore,'Antes de restaurar backup');

  target.events.length=0;
  const imported=importStateJson(backup);
  repo.save(imported);
  assert.equal(repo.load().name,'Campanha A');
  assert.deepEqual(target.events.map(e=>[e.type,e.detail]), [[R21_STATE_CHANGED_EVENT,{key:'state-key',operation:'save'}]]);

  target.events.length=0;
  const restored=snaps.restore(snap.id);
  repo.save(restored);
  assert.equal(repo.load().name,'Campanha B');
  assert.deepEqual(target.events.map(e=>[e.type,e.detail]), [[R21_STATE_CHANGED_EVENT,{key:'state-key',operation:'save'}]]);
});

test('clear remove campanha e emite refresh de clear', () => {
  const storage=new MemoryStorage();
  const target=new FakeEventTarget();
  const repo=new LocalStorageRepository(storage,'state-key',target);
  repo.save(stateNamed('A'));
  target.events.length=0;
  repo.clear();
  assert.equal(repo.load(),null);
  assert.deepEqual(target.events.map(e=>[e.type,e.detail]), [[R21_STATE_CHANGED_EVENT,{key:'state-key',operation:'clear'}]]);
});

test('uiBridge mantém import de backup e restore de snapshot passando por repository.save', () => {
  const text=fs.readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
  const backup=/importCampaignBackup\([^]*?this\.repository\.save\(imported\)[^]*?return ok\(\{ state: clone\(imported\), snapshotId \}\)/.test(text);
  const restore=/restoreSnapshot\([^]*?this\.snapshots\.restore\([^]*?this\.repository\.save\(restored\)[^]*?return ok\(\{ state: clone\(restored\) \}\)/.test(text);
  assert.equal(backup,true,'importCampaignBackup deve persistir via repository.save para emitir refresh');
  assert.equal(restore,true,'restoreSnapshot deve persistir via repository.save para emitir refresh');
});
