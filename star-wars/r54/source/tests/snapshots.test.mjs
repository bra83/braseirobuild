import test from 'node:test';
import assert from 'node:assert/strict';
import { createCampaign } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { LocalSnapshotRepository } from '../app/src/main/assets/vtt/js/core/snapshots.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('snapshot restaura cópia íntegra do estado e mantém limite',()=>{
  const storage=new MemoryStorage(); const repo=new LocalSnapshotRepository(storage,'s',2); const state=createCampaign('Base');
  const a=repo.create(state,'A'); state.name='Mutado'; const restored=repo.restore(a.id); assert.equal(restored.name,'Base');
  repo.create(state,'B'); repo.create(state,'C'); assert.equal(repo.list().length,2);
});
