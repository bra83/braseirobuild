export * from './recoveryExpansionRegistryR47.mjs';
import * as base from './recoveryExpansionRegistryR47.mjs';
import * as galaxy from './galaxyGraphDarkElfR49.mjs';
export const RECOVERY_RUNTIME_WAVE_49=49;
export function coverage49(){return {...base.coverage47(),galaxyGraph:galaxy.coverage49(),travelLayer:'COMMUNITY_REFERENCE_MASTER_GATED'};}
export function masterSearch49(query='',mode='MIXED'){return {...base.masterSearch47(query,mode),hyperlanes:galaxy.findRoutes49(query),travelNodes:galaxy.VERIFIED_SEGMENT_NODES.filter(x=>!query||JSON.stringify(x).toLowerCase().includes(String(query).toLowerCase()))};}
export function prepareTravel49(input){return galaxy.prepareTravel49(input);}

export function coverage50(){return {...coverage49(),galaxyGraph:galaxy.coverage50()};}
export function routePlan50(from,to){return galaxy.routePlan50(from,to);}
export function neighbors50(name){return galaxy.neighbors50(name);}

export function coverage51(){return {...coverage49(),galaxyGraph:galaxy.coverage51()};}
export function routePlan51(from,to){return galaxy.routePlan51 ? galaxy.routePlan51(from,to) : galaxy.routePlan50(from,to);}
export function neighbors51(name){return galaxy.neighbors51 ? galaxy.neighbors51(name) : galaxy.neighbors50(name);}
export function installRecoveryExpansionR51(target=globalThis){
  if(target.__BRASEIRO_EXPANSION_R51__) return target.__BRASEIRO_EXPANSION_R51__;
  const api=Object.freeze({wave:51,coverage:coverage51(),masterSearch:masterSearch49,prepareTravel:prepareTravel49,routePlan:routePlan51,neighbors:neighbors51,prepareEquipment:base.prepareEquipment47,prepareAttachment:base.prepareAttachment47,integrity:base.integrity47,economy:base.economy,infra:base.infra});
  Object.defineProperty(target,'__BRASEIRO_EXPANSION_R51__',{value:api,writable:false,configurable:false});
  return api;
}
