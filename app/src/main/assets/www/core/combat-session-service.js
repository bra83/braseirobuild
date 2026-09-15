import { canonicalJson } from './canonical-json.js';
import { CombatActionService } from './combat-service.js';
import { StarterCombatService } from './starter-combat-service.js';
import { EquipmentService } from './equipment-service.js';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return null;return JSON.parse(loaded.stateJson)}

export class CombatSessionService{
  constructor({persistence,barbara}){if(!persistence)throw new TypeError('persistence required');if(!barbara||typeof barbara.generate!=='function')throw new TypeError('Barbara narrative provider required');this.persistence=persistence;this.barbara=barbara;this.actions=new CombatActionService({persistence});this.starter=new StarterCombatService({persistence});this.equipment=new EquipmentService({persistence});}
  async _narrate(campaignId,result,kind){if(!result?.receipt||result.receipt.status!=='OK')return result;const state=loadState(this.persistence,campaignId);const eventId=result.receipt.eventId;const mechanicalEvent=Object.freeze({eventId,revision:result.receipt.newRevision,kind,combat:true,resolution:result.resolution??result.actions?.find(a=>a.attack)?.attack??null,outcome:result.combat?.outcome??null});try{const narrative=this.barbara.generateAsync?await this.barbara.generateAsync({mechanicalEvent,state}):await Promise.resolve(this.barbara.generate({mechanicalEvent,state}));const narrativeReceipt=this.persistence.appendNarrative({campaignId,eventId:`narrative:${eventId}`,mechanicalEventId:eventId,mechanicalRevision:result.receipt.newRevision,eventPayloadJson:canonicalJson(narrative)});return Object.freeze({...result,narrative,narrativeReceipt});}catch(error){return Object.freeze({...result,narrativeError:String(error?.message??error)});}}
  async startAtVigilia({campaignId}){return this._narrate(campaignId,this.starter.startAtVigilia({campaignId}),'COMBAT_STARTED');}
  async attackNpc(args){return this._narrate(args.campaignId,this.actions.attackNpc(args),'PLAYER_ATTACK');}
  async swarmAssistNpc(args){return this._narrate(args.campaignId,this.actions.swarmAssistNpc(args),'PLAYER_SWARM_ASSIST');}
  async swarmAttackNpc(args){return this._narrate(args.campaignId,this.actions.swarmAttackNpc(args),'PLAYER_SWARM_ATTACK');}
  async chargeNpc(args){return this._narrate(args.campaignId,this.actions.chargeNpc(args),'PLAYER_CHARGE');}
  async dualWieldAttackNpc(args){return this._narrate(args.campaignId,this.actions.dualWieldAttackNpc(args),'PLAYER_DUAL_WIELD_ATTACK');}
  async shoveNpc(args){return this._narrate(args.campaignId,this.actions.shoveNpc(args),'PLAYER_SHOVE');}
  async grappleNpc(args){return this._narrate(args.campaignId,this.actions.grappleNpc(args),'PLAYER_GRAPPLE_START');}
  async manipulateGrappledNpc(args){return this._narrate(args.campaignId,this.actions.manipulateGrappledNpc(args),'PLAYER_GRAPPLE_MANIPULATION');}
  async screenAlly(args){return this._narrate(args.campaignId,this.actions.screenAlly(args),'PLAYER_SCREEN_ALLY');}
  async movePc(args){return this._narrate(args.campaignId,this.actions.movePc(args),'PLAYER_MOVED');}
  async fightingWithdrawal(args){return this._narrate(args.campaignId,this.actions.fightingWithdrawal(args),'FIGHTING_WITHDRAWAL');}
  async goProne(args){return this._narrate(args.campaignId,this.actions.goProne(args),'PC_WENT_PRONE');}
  async standUp(args){return this._narrate(args.campaignId,this.actions.standUp(args),'PC_STOOD_UP');}
  async totalDefense(args){return this._narrate(args.campaignId,this.actions.totalDefense(args),'TOTAL_DEFENSE');}
  async stabilizePc(args){return this._narrate(args.campaignId,this.actions.stabilizePc(args),'PC_STABILIZATION_ATTEMPT');}
  async reloadWeapon(args){return this._narrate(args.campaignId,this.equipment.reload(args),'WEAPON_RELOADED');}
  async setCarryState(args){return this._narrate(args.campaignId,this.equipment.setCarryState(args),'EQUIPMENT_CARRY_STATE_CHANGED');}
  async endPcSide(args){return this._narrate(args.campaignId,this.actions.endPcSide(args),'COMBAT_SIDE_ENDED');}
  async resolveEnemySide(args){return this._narrate(args.campaignId,this.actions.resolveEnemySide(args),'ENEMY_SIDE_RESOLVED');}
}
