import {assertLifeStore} from './life-engine.mjs';
/** Browser/WebView IndexedDB. CAS check and write share one native transaction. */
export async function openIndexedDB(name='braseiro-life-v2',indexedDB=globalThis.indexedDB) {
 if(!indexedDB)throw Error('IndexedDB indisponível');
 const db=await new Promise((resolve,reject)=>{const r=indexedDB.open(name,1);r.onupgradeneeded=()=>r.result.createObjectStore('states');r.onsuccess=()=>resolve(r.result);r.onerror=()=>reject(r.error);});
 return {
  async load(key){return new Promise((resolve,reject)=>{const t=db.transaction('states','readonly'),r=t.objectStore('states').get(key);r.onsuccess=()=>{try{resolve(r.result?assertLifeStore(r.result):null);}catch(e){reject(e);}};r.onerror=()=>reject(r.error);});},
  async save(key,state,expectedRevision=null){assertLifeStore(state);return new Promise((resolve,reject)=>{const t=db.transaction('states','readwrite'),o=t.objectStore('states'),r=o.get(key);let failure;
   r.onsuccess=()=>{if((r.result?.revision??null)!==expectedRevision){failure=Error('Conflito de revisão persistida');t.abort();return;}o.put(state,key);};
   t.oncomplete=()=>resolve();t.onabort=()=>reject(failure??t.error);t.onerror=()=>reject(t.error);});},
  close(){db.close();}
 };
}
export function exportBackup(state){return JSON.stringify({format:'braseiro.life.backup/v2',state:assertLifeStore(state)});}
export function restoreBackup(json){const b=JSON.parse(json);if(b.format!=='braseiro.life.backup/v2')throw Error('Backup incompatível');return assertLifeStore(b.state);}
