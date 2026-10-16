const clone=v=>structuredClone(v);
const MODES=new Set(["EOTE","AOR","MIXED"]);

export function ensureModificationState(state={}){
 const s=clone(state);
 s.expansion=s.expansion&&typeof s.expansion==="object"?s.expansion:{};
 s.expansion.schema=Math.max(Number(s.expansion.schema)||0,9);
 s.expansion.modificationProposals=Array.isArray(s.expansion.modificationProposals)?s.expansion.modificationProposals:[];
 return s;
}

export function proposeAttachmentModification(state,input={}){
 const s=ensureModificationState(state);
 const mode=input.mode??s.campaignMode??"MIXED";
 if(!MODES.has(mode))throw new Error("campaign mode");
 if(!input.itemId||!input.attachmentId)throw new Error("attachment identity");
 if(!input.source||!input.source.book||input.source.printedPage==null)throw new Error("primary source required");
 if(input.hardPoints==null||!input.effect)throw new Error("hard points/effect required");
 const proposal={
  id:input.proposalId??`mod_${Date.now()}_${s.expansion.modificationProposals.length}`,
  type:"ATTACHMENT_MODIFICATION_PROPOSAL",mode,itemId:input.itemId,attachmentId:input.attachmentId,
  hardPoints:Number(input.hardPoints),effect:clone(input.effect),mods:Array.isArray(input.mods)?clone(input.mods):[],
  source:clone(input.source),status:"PENDING_MASTER",mutatesItem:false,mutatesCredits:false,requiresControllerCommit:false
 };
 s.expansion.modificationProposals.push(proposal);
 return {state:s,proposal};
}

export function resolveAttachmentModification(state,proposalId,{approved,masterId=null}={}){
 const s=ensureModificationState(state);
 const i=s.expansion.modificationProposals.findIndex(x=>x.id===proposalId);
 if(i<0)throw new Error("proposal");
 const old=s.expansion.modificationProposals[i];
 if(old.status!=="PENDING_MASTER")throw new Error("proposal already resolved");
 const next={...old,status:approved?"MASTER_APPROVED":"MASTER_REJECTED",masterId,
  mutatesItem:false,mutatesCredits:false,requiresControllerCommit:Boolean(approved)};
 s.expansion.modificationProposals[i]=next;
 return {state:s,proposal:next};
}

export function modificationMasterQueue(state){
 const s=ensureModificationState(state);
 return s.expansion.modificationProposals.filter(x=>x.status==="PENDING_MASTER").map(clone);
}

export function attachmentActivationPolicy(candidate={}){
 const missing=[];
 if(!candidate.source?.book||candidate.source?.printedPage==null)missing.push("source");
 if(candidate.hardPoints==null)missing.push("hardPoints");
 if(!candidate.effect)missing.push("effect");
 return {mechanicallyActivatable:missing.length===0,missing,policy:"PRIMARY_SOURCE_FIELDS_REQUIRED"};
}
