import {ensureInfra,beginAtomic,approveAtomic,commitAtomic,snapshot,integrity,exportExpansionBackup,importExpansionBackup} from "./expansionInfrastructureWave26.mjs";
import {proposeCraftResultSpend,resolveCraftResultSpend} from "./fullyOperationalResultSpendWave25.mjs";
const c=x=>structuredClone(x);
export function ensureClosureState(state={}){let s=ensureInfra(state);s.expansion.closure=s.expansion.closure||{sourceQueue:[],audit:[],lastSnapshotRevision:null};return s}
export function queueSourceCandidate(state,candidate={}){
 let s=ensureClosureState(state);const id=candidate.id;if(!id)throw new Error("candidate_id");
 if(!s.expansion.closure.sourceQueue.some(x=>x.id===id))s.expansion.closure.sourceQueue.push({...c(candidate),status:candidate.mechanicallyComplete?"READY_FOR_DEDUPE":"RETAIN_UNTIL_PRIMARY_VALIDATION"});
 return s;
}
export function proposeMechanic(state,{id,type,payload,sourceEvidence}={}){
 if(!sourceEvidence?.book||!sourceEvidence?.page)throw new Error("primary_source_required");
 return beginAtomic(ensureClosureState(state),{id,type,payload:{...c(payload),sourceEvidence:c(sourceEvidence)},expectedRevision:ensureClosureState(state).expansion.infra.revision,source:"BARBARA"});
}
export function masterResolveMechanic(state,tx,{approved=false}={}){return approveAtomic(state,tx,{masterApproved:approved})}
export function controllerCommitMechanic(state,tx,{controllerId="controller",apply}={}){
 let r=commitAtomic(state,tx,{controllerId,apply});if(r.receipt){r.state.expansion.closure.audit.push({...r.receipt,sourceEvidence:tx.payload?.sourceEvidence||null});}
 return r;
}
export function commitCraftResult(state,{id,tableId,rowName,symbolCost,polarity="positive",sourceEvidence,approved=true,controllerId="controller"}={}){
 let s=ensureClosureState(state);let p=proposeCraftResultSpend(s,{tableId,rowName,symbolCost,polarity});
 if(!p.proposal.sourceValidated)return {state:s,status:"PRIMARY_SYMBOL_COST_REQUIRED"};
 let m=resolveCraftResultSpend(s,p.proposal,{approved});if(m.proposal.status!=="MASTER_APPROVED_CONTROLLER_PENDING")return {state:s,status:m.proposal.status};
 let a=proposeMechanic(s,{id,type:"CRAFT_RESULT_SPEND",payload:{...m.proposal},sourceEvidence});let b=masterResolveMechanic(a.state,a.transaction,{approved:true});
 let q=controllerCommitMechanic(b.state,b.transaction,{controllerId,apply:(z,payload)=>{z.crafting=z.crafting||{};z.crafting.resultSpends=z.crafting.resultSpends||[];z.crafting.resultSpends.push({...payload,status:"COMMITTED"});return z}});
 return {...q,status:q.receipt?.status||"FAILED"};
}
export function checkpoint(state,reason="CLOSURE"){let q=snapshot(ensureClosureState(state),{reason});q.state.expansion.closure.lastSnapshotRevision=q.snapshot.revision;return q}
export function recover(serializedBackup){return importExpansionBackup(serializedBackup)}
export function closureReport(state){let s=ensureClosureState(state),i=integrity(s).report;return {...i,queued:s.expansion.closure.sourceQueue.length,audited:s.expansion.closure.audit.length,mutationPath:"BARBARA_MASTER_CONTROLLER_ATOMIC",sourceFailClosed:true}}
export {exportExpansionBackup};
