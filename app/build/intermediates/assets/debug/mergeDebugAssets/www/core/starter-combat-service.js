import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import { orderInitiativeSides, resolveSideInitiative } from './wwn-combat-core.js';
import { freshActionEconomy } from './wwn-tactical-combat.js';
import { SRD_NPC_PROFILES, rollNpcHitPoints, instantiateNpcCombatant } from './wwn-npc-combat.js';
import { activePartyCombatantIds, bestPcDexterityScore, characterById } from './combat-roster.js';

export const STARTER_COMBAT_ID='vigilia-raider-ambush-v1';
export const STARTER_ENEMY_ID='vigilia-raider-1';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function clone(v){return structuredClone(v)}

export class StarterCombatService{
  constructor({persistence}){this.persistence=persistence;}
  startAtVigilia({campaignId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    if(state.encounterState?.combat?.active===true)return Object.freeze({status:'COMBAT_ALREADY_ACTIVE_NO_MUTATION',combat:clone(state.encounterState.combat),revision:loaded.revision});
    const sceneId=state.encounterState?.currentScene?.contentId;if(sceneId!=='site_vigilia')return Object.freeze({status:'VIGILIA_SCENE_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const pcIds=[...activePartyCombatantIds(state)];if(pcIds.length<1)return Object.freeze({status:'MISSING_EVIDENCE_PARTY_COMBATANTS',revision:loaded.revision});
    const bestDexterityScore=bestPcDexterityScore(state,pcIds);if(!Number.isInteger(bestDexterityScore))return Object.freeze({status:'MISSING_EVIDENCE_CHARACTER_DEXTERITY',revision:loaded.revision});
    const rng=DeterministicRng.fromState(state.rngState);
    const hpRoll=rollNpcHitPoints({rng,profile:SRD_NPC_PROFILES.THUG_OR_MILITIA,streamName:`combat/${STARTER_COMBAT_ID}/enemy-hp`});
    const enemy=instantiateNpcCombatant({npcId:STARTER_ENEMY_ID,name:'SAQUEADOR DA VIGÍLIA',profileId:'THUG_OR_MILITIA',weaponId:'LIGHT_SPEAR_MELEE',hp:hpRoll.hp});
    const pcInit=resolveSideInitiative({rng,bestDexterityScore,streamName:`combat/${STARTER_COMBAT_ID}/initiative/pc`});
    const enemyInit=resolveSideInitiative({rng,dexterityModifier:0,streamName:`combat/${STARTER_COMBAT_ID}/initiative/enemy`});
    const order=orderInitiativeSides([{sideId:'PC',isPcSide:true,total:pcInit.total,roll:pcInit},{sideId:'ENEMY',isPcSide:false,total:enemyInit.total,roll:enemyInit}]);
    const next=clone(state);next.rngState=rng.exportState();next.npcState=next.npcState??{};
    next.npcState[STARTER_ENEMY_ID]={...enemy,publicFacts:['Saqueador humano armado com uma lança leve.'],gmSecrets:[],memories:[],visibleStatus:'HOSTIL',currentIntent:'impedir a passagem pelas ruínas'};
    next.encounterState={...(next.encounterState??{}),combat:{
      active:true,encounterId:STARTER_COMBAT_ID,siteId:'site_vigilia',round:1,order:order.map(s=>s.sideId),activeSideId:order[0].sideId,
      initiative:{PC:pcInit,ENEMY:enemyInit},pcId:pcIds[0],pcIds,enemyIds:[STARTER_ENEMY_ID],outcome:null,
      tactical:{units:'feet',pathModel:'CLEAR_STARTER_COURTYARD',actors:{...Object.fromEntries(pcIds.map((id,index)=>[id,{actorId:id,sideId:'PC',xFeet:-(index%2)*5,yFeet:-Math.floor(index/2)*5}])),[STARTER_ENEMY_ID]:{actorId:STARTER_ENEMY_ID,sideId:'ENEMY',xFeet:20,yFeet:0}}},
      economy:{...Object.fromEntries(pcIds.map(id=>[id,freshActionEconomy()])),[STARTER_ENEMY_ID]:freshActionEconomy()},
      sourceRefs:[...pcInit.sourceRefs,...enemyInit.sourceRefs,...hpRoll.sourceRefs,...enemy.sourceRefs],
    }};
    const newRevision=loaded.revision+1;next.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:combat:${STARTER_COMBAT_ID}:start`;
    const receipt=this.persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind:'STARTER_COMBAT_STARTED',eventPayloadJson:canonicalJson({eventId,revision:newRevision,encounterId:STARTER_COMBAT_ID,order:next.encounterState.combat.order,pcIds,hpRoll,sourceRefs:next.encounterState.combat.sourceRefs}),newStateJson:canonicalJson(next),rngCountersJson:canonicalJson(next.rngState.counters)});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'OK',receipt,combat:clone(next.encounterState.combat),enemy:clone(next.npcState[STARTER_ENEMY_ID])});
  }
}
