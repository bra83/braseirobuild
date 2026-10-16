import crypto from "node:crypto";
const c=x=>structuredClone(x); const hash=x=>crypto.createHash("sha256").update(JSON.stringify(x)).digest("hex");
export const EXPANSION_SCHEMA=26;
export function ensureInfra(state={}){
 const s=c(state);s.expansion=s.expansion||{};s.expansion.schema=Math.max(Number(s.expansion.schema)||0,EXPANSION_SCHEMA);
 s.expansion.infra=s.expansion.infra||{revision:0,journal:[],commits:{},snapshots:[],deadLetters:[],migrationHistory:[],lastIntegrity:null};
 return s;
}
export function migrateExpansion(state={}){
 let s=ensureInfra(state),i=s.expansion.infra;
 if(!i.migrationHistory.some(x=>x.to===26))i.migrationHistory.push({from:Number(state?.expansion?.schema)||0,to:26,id:"EXPANSION_INFRA_26"});
 return s;
}
export function beginAtomic(state,{id,type,payload={},expectedRevision=null,source="BARBARA"}={}){
 const s=migrateExpansion(state),i=s.expansion.infra;if(!id||!type)throw new Error("event");
 if(i.commits[id])return {state:s,transaction:c(i.commits[id]),duplicate:true};
 if(expectedRevision!==null&&expectedRevision!==i.revision)throw new Error("revision_conflict");
 const tx={id,type,payload:c(payload),source,baseRevision:i.revision,status:"PREPARED",payloadHash:hash(payload),createdSeq:i.journal.length};
 i.journal.push({id,type,status:"PREPARED",baseRevision:i.revision,payloadHash:tx.payloadHash});return {state:s,transaction:tx,duplicate:false};
}
export function approveAtomic(state,tx,{masterApproved=true}={}){
 const s=ensureInfra(state);if(!tx||tx.status!=="PREPARED")throw new Error("prepared");
 return {state:s,transaction:{...c(tx),status:masterApproved?"MASTER_APPROVED_CONTROLLER_PENDING":"MASTER_REJECTED"}};
}
export function commitAtomic(state,tx,{controllerId="controller",apply}={}){
 const s=ensureInfra(state),i=s.expansion.infra;if(i.commits[tx?.id])return {state:s,receipt:c(i.commits[tx.id]),duplicate:true};
 if(tx?.status!=="MASTER_APPROVED_CONTROLLER_PENDING")throw new Error("approval");
 if(tx.baseRevision!==i.revision)throw new Error("revision_conflict");
 const before=c(s);let next=c(s);
 try{if(apply)next=apply(next,c(tx.payload))||next;next=ensureInfra(next);const ni=next.expansion.infra;
  ni.revision=i.revision+1;const receipt={id:tx.id,type:tx.type,status:"COMMITTED",revision:ni.revision,controllerId,payloadHash:tx.payloadHash};
  ni.commits[tx.id]=receipt;ni.journal.push(receipt);return {state:next,receipt:c(receipt),duplicate:false};
 }catch(error){const bi=before.expansion.infra;bi.deadLetters.push({id:tx.id,type:tx.type,error:String(error?.message||error),payloadHash:tx.payloadHash});return {state:before,error:String(error?.message||error),rolledBack:true};}
}
export function snapshot(state,{reason="AUTO"}={}){
 const s=ensureInfra(state),i=s.expansion.infra,data=c({...s,expansion:{...s.expansion,infra:{...i,snapshots:[]}}});
 const snap={revision:i.revision,reason,digest:hash(data),data};i.snapshots.push(snap);if(i.snapshots.length>10)i.snapshots.shift();return {state:s,snapshot:c(snap)};
}
export function restoreSnapshot(state,snap){
 if(!snap?.data||hash(snap.data)!==snap.digest)throw new Error("snapshot_integrity");
 let s=ensureInfra(c(snap.data));s.expansion.infra.snapshots=ensureInfra(state).expansion.infra.snapshots;return s;
}
export function integrity(state){
 const s=ensureInfra(state),i=s.expansion.infra;const commitRows=i.journal.filter(x=>x.status==="COMMITTED");
 const ids=new Set(Object.keys(i.commits));const ok=commitRows.every(x=>ids.has(x.id))&&[...ids].every(id=>commitRows.some(x=>x.id===id));
 const out={ok,revision:i.revision,commits:ids.size,journal:i.journal.length,deadLetters:i.deadLetters.length};i.lastIntegrity=out;return {state:s,report:c(out)};
}
export function rehydrate(serialized){const raw=typeof serialized==="string"?JSON.parse(serialized):serialized;const s=migrateExpansion(raw||{});return integrity(s)}
export function exportExpansionBackup(state){const s=ensureInfra(state),payload=c(s);return JSON.stringify({format:"SW_EDGE_EXPANSION_BACKUP_V26",digest:hash(payload),payload})}
export function importExpansionBackup(text){const x=JSON.parse(text);if(x.format!=="SW_EDGE_EXPANSION_BACKUP_V26"||hash(x.payload)!==x.digest)throw new Error("backup_integrity");return rehydrate(x.payload)}
export function infrastructureContext(state){const {report}=integrity(state);return {...report,schema:EXPANSION_SCHEMA,atomicCommit:true,idempotency:true,rollback:true,snapshot:true,backup:true,rehydration:true}}
