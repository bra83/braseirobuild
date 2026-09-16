export function projectFichaBinding(state){const c=state?.characterState;if(!c)return Object.freeze({status:'NO_CHARACTER'});return Object.freeze({status:'OK',name:c.name,level:c.level,classId:c.classId??c.class,attributes:structuredClone(c.attributes??{}),skills:structuredClone(c.skills??{}),hp:structuredClone(c.hp??null),savingThrows:structuredClone(c.savingThrows??{}),inventoryStatus:c.inventory===null?'EVIDÊNCIA AUSENTE':'OK',magicApplicable:c.magic!==null&&c.magic!==undefined});}
export function projectScr015Binding(state){const e=state?.encounterState??{};return Object.freeze({status:'OK',screenId:'SCR-015',contactState:e.contactState??null,lastResolution:structuredClone(e.lastResolution??null),position:structuredClone(state?.worldState?.position??null)});}
export function projectScr016Binding(state){const c=state?.characterState??{};const r=state?.encounterState?.lastResolution??null;const hasWeapon=Array.isArray(c.inventory)&&c.inventory.some(i=>i?.kind==='WEAPON'||i?.weaponContract);return Object.freeze({status:'OK',screenId:'SCR-016',testEnabled:true,saveEnabled:true,damageEnabled:Boolean(r?.damage!==undefined),attackEnabled:hasWeapon,attackBlocker:hasWeapon?null:'MISSING_EVIDENCE_INVENTORY',lastResolution:structuredClone(r)});}

import { projectPartyForPlayer } from './party-state.js';
export function projectScr014AtlasBinding(state){
  const party=projectPartyForPlayer(state);
  return Object.freeze({
    status:party.status==='OK'?'OK':party.status,
    screenId:'SCR-014',
    position:structuredClone(state?.worldState?.position??null),
    party,
    content:structuredClone(state?.contentState?.public??null),
  });
}
