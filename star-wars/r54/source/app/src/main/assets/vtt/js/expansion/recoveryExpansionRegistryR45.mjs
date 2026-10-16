import * as fleet from './starshipsSpeedersClosureWave36.mjs';
import * as gadgets from './gadgetsGearMechanicalCrosswalkWave40.mjs';
export const RECOVERY_EXPANSION_R45=Object.freeze({wave:45,mode:'RECOVERY',sourceFailClosed:true,masterControllerOnly:true,fleet,gadgets});
export function installRecoveryExpansionR45(target=globalThis){
  if(target.__BRASEIRO_EXPANSION_R45__) return target.__BRASEIRO_EXPANSION_R45__;
  const api=Object.freeze({wave:45,coverage:{fleet:fleet.coverage36(),gadgets:gadgets.coverage40()},findVehicle:fleet.findVehicle36,prepareVehicle:fleet.prepareVehicle36,findAttachment:gadgets.find40,proposeAttachment:gadgets.propose40,resolveAttachment:gadgets.resolve40});
  Object.defineProperty(target,'__BRASEIRO_EXPANSION_R45__',{value:api,writable:false,configurable:false});
  return api;
}
