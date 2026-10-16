import { validateState } from './state.mjs';

export class LocalSnapshotRepository {
  constructor(storage=globalThis.localStorage, key='braseiro.starwars.edge.snapshots.v1', max=10){ this.storage=storage; this.key=key; this.max=max; }
  list(){ const raw=this.storage.getItem(this.key); if(!raw) return []; const arr=JSON.parse(raw); return Array.isArray(arr)?arr:[]; }
  create(state,label='snapshot'){
    validateState(state);
    const item={id:`snap_${Date.now()}_${Math.random().toString(16).slice(2)}`,label,createdAt:new Date().toISOString(),state:structuredClone(state)};
    const next=[item,...this.list()].slice(0,this.max); this.storage.setItem(this.key,JSON.stringify(next)); return item;
  }
  remove(id){ const key=String(id??''); const current=this.list(); const next=current.filter(x=>x.id!==key); if(next.length!==current.length) this.storage.setItem(this.key,JSON.stringify(next)); return next.length!==current.length; }
  restore(id){ const item=this.list().find(x=>x.id===id); if(!item) throw new Error('Snapshot não encontrado.'); validateState(item.state); return structuredClone(item.state); }
}
