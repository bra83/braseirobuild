import * as fleet from './starshipsSpeedersClosureWave36.mjs';
import * as gadgets from './gadgetsGearMechanicalCrosswalkWave40.mjs';
import * as infra from './expansionInfrastructureWave26.mjs';
import * as economy from './sandboxEconomyWave21.mjs';
export const RECOVERY_EXPANSION_R46=Object.freeze({wave:46,mode:'RECOVERY_INTEGRATED',sourceFailClosed:true,masterControllerOnly:true,fleet,gadgets,infra,economy});
export function installRecoveryExpansionR46(target=globalThis){
 if(target.__BRASEIRO_EXPANSION_R46__) return target.__BRASEIRO_EXPANSION_R46__;
 const api=Object.freeze({wave:46,coverage:{fleet:fleet.coverage36(),gadgets:gadgets.coverage40()},findVehicle:fleet.findVehicle36,prepareVehicle:fleet.prepareVehicle36,findAttachment:gadgets.find40,proposeAttachment:gadgets.propose40,resolveAttachment:gadgets.resolve40,ensureInfra:infra.ensureInfra,beginAtomic:infra.beginAtomic,approveAtomic:infra.approveAtomic,commitAtomic:infra.commitAtomic,integrity:infra.integrity,snapshot:infra.snapshot,restoreSnapshot:infra.restoreSnapshot,exportBackup:infra.exportExpansionBackup,importBackup:infra.importExpansionBackup,ensureEconomy:economy.ensureEconomy,registerAsset:economy.registerAsset,proposePurchase:economy.proposePurchase,proposeSale:economy.proposeSale,proposeReward:economy.proposeReward,proposeLoot:economy.proposeLoot,proposeLoss:economy.proposeLoss,proposeRepair:economy.proposeRepair,economyMasterResolve:economy.masterResolve,economyControllerCommit:economy.controllerCommit});
 Object.defineProperty(target,'__BRASEIRO_EXPANSION_R46__',{value:api,writable:false,configurable:false}); return api;
}
