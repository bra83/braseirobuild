import {open,readFile,rename,unlink,mkdir} from 'node:fs/promises';
import {dirname} from 'node:path';
import {exportBackup,restoreBackup} from './persistence.mjs';
export async function loadFile(path){try{return restoreBackup(await readFile(path,'utf8'));}catch(e){if(e.code==='ENOENT')return null;throw e;}}
/** Atomic rename, fsync and exclusive writer lock. Stale locks are surfaced, not stolen. */
export async function saveFile(path,state,expectedRevision=null){
 const body=exportBackup(state);await mkdir(dirname(path),{recursive:true});const lock=await open(`${path}.lock`,'wx');let temp;
 try{const previous=await loadFile(path);if((previous?.revision??null)!==expectedRevision)throw Error('Conflito de revisão persistida');
  temp=`${path}.${process.pid}.tmp`;const handle=await open(temp,'wx');try{await handle.writeFile(body);await handle.sync();}finally{await handle.close();}await rename(temp,path);temp=null;
 }finally{if(temp)await unlink(temp).catch(()=>{});await lock.close();await unlink(`${path}.lock`);}
}
