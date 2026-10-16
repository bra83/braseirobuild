import * as infra from './expansionInfrastructureWave26.mjs';
import * as economy from './sandboxEconomyWave21.mjs';
import * as fleet from './starshipsSpeedersClosureWave36.mjs';
import * as gg from './gadgetsGearMechanicalCrosswalkWave40.mjs';
import * as progression from './expansionProgressionEquipmentBridgeWave7.mjs';
import * as world from './galacticWorldCatalogWave6.mjs';
import * as mod from './attachmentModificationGateWave9.mjs';
import * as npc from './masterNpcCompendium.mjs';
export const RECOVERY_RUNTIME_WAVE=47;
export function coverage47(){return {wave:47,modes:progression.expansionCoverage().modes,progression:progression.expansionCoverage(),world:{nodes:world.GALACTIC_WORLD_CATALOG.length,detailed:world.GALACTIC_WORLD_CATALOG.filter(x=>x.detailStatus==='DETAILED').length},fleet:fleet.coverage36(),gadgets:gg.coverage40(),mutationPath:'BARBARA_MASTER_CONTROLLER_ATOMIC',sourceFailClosed:true};}
export function masterSearch47(query='',mode='MIXED'){return {equipment:progression.expansionMasterDiscovery(query,{mode}),vehicles:fleet.findVehicle36(query),gadgets:gg.find40(query),npcs:npc.masterAdversarySearch(query,{combatReadyOnly:true}),world:world.GALACTIC_WORLD_CATALOG.filter(x=>!query||JSON.stringify(x).toLowerCase().includes(String(query).toLowerCase()))};}
export function prepareEquipment47(kind,id,opts={}){return progression.expansionAcquisitionProposal(kind,id,opts);}
export function prepareAttachment47(state,input){return mod.proposeAttachmentModification(state,input);}
export function integrity47(state){return infra.integrity(state);}
export {economy,infra};
