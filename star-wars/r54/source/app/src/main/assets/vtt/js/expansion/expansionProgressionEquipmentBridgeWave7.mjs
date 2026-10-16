import {aorTalentCoverage,aorTalentCatalog,aorProgressionTreeSeed} from "./aorProgressionIntegration.mjs";
import {equipmentCoverage,masterEquipmentSearch,creationEquipmentReference} from "./equipmentBulkIntegration.mjs";

export const EXPANSION_MODES=Object.freeze(["EOTE","AOR","MIXED"]);

function modeOk(mode){if(!EXPANSION_MODES.includes(mode))throw new Error("campaign mode");return mode;}

export function expansionCreationOptions({mode="MIXED",career=null,specialization=null,talentQuery="",equipmentQuery=""}={}){
 modeOk(mode);
 const aorTalents=(mode==="EOTE")?[]:aorTalentCatalog({career,specialization,query:talentQuery});
 const equipment=masterEquipmentSearch(equipmentQuery,{mode,mechanicalOnly:true});
 return {mode,aorTalents,equipment,purchaseRequiresMaster:true};
}

export function expansionProgressionSeed({mode="AOR",career,specialization}={}){
 modeOk(mode);
 if(mode==="EOTE") throw new Error("AoR progression unavailable in EOTE-only mode");
 return aorProgressionTreeSeed(career,specialization);
}

export function expansionMasterDiscovery(query="",{mode="MIXED"}={}){
 modeOk(mode);
 const talents=(mode==="EOTE")?[]:aorTalentCatalog({query});
 const equipment=masterEquipmentSearch(query,{mode,mechanicalOnly:false});
 return {mode,talents,equipment};
}

export function expansionAcquisitionProposal(kind,id,{mode="MIXED",characterId=null,locationId=null}={}){
 modeOk(mode);
 const ref=creationEquipmentReference(kind,id);
 return {type:"EQUIPMENT_ACQUISITION_PROPOSAL",mode,characterId,locationId,...ref,status:"PENDING_MASTER",mutatesInventory:false,mutatesCredits:false};
}

export function expansionCoverage(){
 return {modes:[...EXPANSION_MODES],aor:aorTalentCoverage(),equipment:equipmentCoverage(),inventoryMutationPolicy:"MASTER_CONFIRMATION_ONLY"};
}
