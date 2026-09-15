import { canonicalJson } from './canonical-json.js';
import { DeterministicRng } from './rng-core.js';
import {
  createAssetEligibility, expandInfluenceCost, factionIncome, factionSpecialResolutionRequirements,
  hideAssetEligibility, inspectFactionUpkeep, repairAssetAmount, repairFactionAmount,
  resolveFactionAttack, resolveFactionAttributeCheck, sellAssetReturn,
} from './wwn-faction-core.js';

function loadState(persistence,campaignId){const loaded=persistence.loadCampaign(campaignId);if(loaded.status!=='OK')return{loaded,state:null};const state=JSON.parse(loaded.stateJson);if(state.revision!==loaded.revision)throw new Error('State/load revision mismatch');return{loaded,state};}
function commit({persistence,loaded,state,campaignId,eventKind,eventSuffix,payload}){const newRevision=loaded.revision+1;state.revision=newRevision;const eventId=`mech:${campaignId}:r${newRevision}:${eventSuffix}`;const receipt=persistence.commit({campaignId,expectedRevision:loaded.revision,eventId,eventKind,eventPayloadJson:canonicalJson({eventId,revision:newRevision,...payload}),newStateJson:canonicalJson(state),rngCountersJson:canonicalJson(state.rngState?.counters??{})});return{receipt,newRevision,eventId};}
function currentContext(state){const turn=state.worldState?.factionTurn;if(!turn)return{status:'NO_ACTIVE_FACTION_TURN'};if(turn.phase==='TURN_COMPLETE')return{status:'FACTION_TURN_COMPLETE'};const faction=state.factionState?.[turn.currentFactionId];if(!faction)return{status:'MISSING_CURRENT_FACTION'};return{status:'OK',turn,faction};}
function assetEntries(faction){if(Array.isArray(faction.assets))return faction.assets.map((a,i)=>[a?.assetId??a?.id??String(i),a,i]);if(faction.assets&&typeof faction.assets==='object')return Object.entries(faction.assets).map(([id,a])=>[a?.assetId??a?.id??id,a,id]);return[];}
function findAsset(faction,id){const entry=assetEntries(faction).find(([assetId])=>assetId===id);return entry??null;}
function deleteAsset(faction,entry){if(Array.isArray(faction.assets))faction.assets.splice(entry[2],1);else delete faction.assets[entry[2]];}
function addAsset(faction,asset){if(Array.isArray(faction.assets))faction.assets.push(asset);else{if(!faction.assets||typeof faction.assets!=='object')faction.assets={};const id=asset.assetId??asset.id;if(!id)throw new Error('canonical asset definition requires assetId/id');if(faction.assets[id])throw new Error('asset id already exists');faction.assets[id]=asset;}}
function attributeValue(faction,key){const k=String(key??'').toUpperCase();const field={FORCE:'force',CUNNING:'cunning',WEALTH:'wealth'}[k];if(!field)throw new RangeError('attribute key must be FORCE/CUNNING/WEALTH');return faction[field];}
function assetLocation(asset){return asset.location??asset.locationId??null;}
function setAssetLocation(asset,value){if('locationId'in asset)asset.locationId=value;else asset.location=value;}
function assetQualities(asset){return new Set((asset.qualities??[]).map(q=>String(q).toUpperCase()));}
function clampHp(asset,value){const max=asset.maxHp??asset.hpMax??asset.hp;if(!Number.isInteger(max)||max<0)throw new RangeError('asset max HP evidence required');return Math.min(max,Math.max(0,value));}

export class FactionTurnService{
  constructor({persistence}){this.persistence=persistence;}

  resolveUpkeep({campaignId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const ctx=currentContext(state);if(ctx.status!=='OK')return Object.freeze({status:`${ctx.status}_NO_MUTATION`,revision:loaded.revision});
    if(ctx.turn.phase!=='UPKEEP_REQUIRED')return Object.freeze({status:'UPKEEP_NOT_CURRENT_PHASE_NO_MUTATION',phase:ctx.turn.phase,revision:loaded.revision});
    const inspection=inspectFactionUpkeep(ctx.faction);
    if(!inspection.canPayAll)return Object.freeze({status:'UPKEEP_RESOLUTION_REQUIRED_NO_MUTATION',revision:loaded.revision,inspection});
    const next=structuredClone(state);const turn=next.worldState.factionTurn;const faction=next.factionState[turn.currentFactionId];
    faction.treasure=(faction.treasure??0)-inspection.totalTreasureDue;faction.lastUpkeep=inspection;
    if(faction.skipNextActionAndSpecials){faction.skipNextActionAndSpecials=false;turn.phase='GOAL_REVIEW_REQUIRED';turn.skipConsumed=true;}
    else{const requirements=factionSpecialResolutionRequirements(faction);turn.specialRequirements=requirements;turn.phase=requirements.length?'SPECIALS_REQUIRED':'ACTION_REQUIRED';}
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'FACTION_UPKEEP_RESOLVED',eventSuffix:'faction-upkeep',payload:{factionId:turn.currentFactionId,inspection,nextPhase:turn.phase}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:turn.phase,receipt,inspection,faction:structuredClone(faction)});
  }

  resolveSpecials({campaignId,resolutions=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const ctx=currentContext(state);if(ctx.status!=='OK')return Object.freeze({status:`${ctx.status}_NO_MUTATION`,revision:loaded.revision});
    if(ctx.turn.phase!=='SPECIALS_REQUIRED')return Object.freeze({status:'SPECIALS_NOT_CURRENT_PHASE_NO_MUTATION',phase:ctx.turn.phase,revision:loaded.revision});
    const required=factionSpecialResolutionRequirements(ctx.faction);
    if(required.length===0){const next=structuredClone(state);next.worldState.factionTurn.phase='ACTION_REQUIRED';const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'FACTION_SPECIALS_NONE',eventSuffix:'faction-specials-none',payload:{factionId:ctx.turn.currentFactionId}});return Object.freeze({status:receipt.status==='OK'?'ACTION_REQUIRED':receipt.status,receipt});}
    // Asset-specific effects vary by canonical asset text. The engine never invents them.
    // A future source-bound asset-effect registry can consume these resolution contracts.
    return Object.freeze({status:'ASSET_SPECIAL_CONTRACT_REQUIRED_NO_MUTATION',revision:loaded.revision,required,resolutionsReceived:Array.isArray(resolutions)?resolutions.length:0});
  }

  takeAction({campaignId,action}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const ctx=currentContext(state);if(ctx.status!=='OK')return Object.freeze({status:`${ctx.status}_NO_MUTATION`,revision:loaded.revision});
    if(ctx.turn.phase!=='ACTION_REQUIRED')return Object.freeze({status:'ACTION_NOT_CURRENT_PHASE_NO_MUTATION',phase:ctx.turn.phase,revision:loaded.revision});
    if(!action||typeof action!=='object'||!action.type)return Object.freeze({status:'MISSING_FACTION_ACTION_CONTRACT_NO_MUTATION',revision:loaded.revision});
    const type=String(action.type).toUpperCase();const next=structuredClone(state);const turn=next.worldState.factionTurn;const faction=next.factionState[turn.currentFactionId];let resolution={type};let rng=null;
    const reject=(status,extra={})=>Object.freeze({status:`${status}_NO_MUTATION`,revision:loaded.revision,...extra});
    try{
      if(type==='MOVE_ASSET'){
        const entry=findAsset(faction,action.assetId);if(!entry)return reject('ASSET_NOT_FOUND');
        if(action.destination===undefined)return reject('DESTINATION_REQUIRED');
        const qualities=assetQualities(entry[1]);
        if(action.destinationAllowed!==true&&!qualities.has('SUBTLE')&&!qualities.has('STEALTH'))return reject('GM_LOCATION_PERMISSION_REQUIRED');
        const from=assetLocation(entry[1]);setAssetLocation(entry[1],structuredClone(action.destination));resolution={type,assetId:action.assetId,from,to:action.destination};
      }else if(type==='REPAIR_ASSET'){
        const entry=findAsset(faction,action.assetId);if(!entry)return reject('ASSET_NOT_FOUND');
        const applications=action.applications??1;if(!Number.isInteger(applications)||applications<1)return reject('INVALID_REPAIR_APPLICATIONS');
        const attribute=String(entry[1].attribute??'').toUpperCase();const relevant=attributeValue(faction,attribute);let cost=0,heal=0;for(let i=1;i<=applications;i+=1){const step=repairAssetAmount({relevantAttribute:relevant,applicationNumber:i});cost+=step.treasureCost;heal+=step.hpRepaired;}
        if((faction.treasure??0)<cost)return reject('INSUFFICIENT_TREASURE',{cost});
        if(!Number.isInteger(entry[1].hp))return reject('ASSET_HP_EVIDENCE_REQUIRED');
        const before=entry[1].hp;faction.treasure-=cost;entry[1].hp=clampHp(entry[1],before+heal);resolution={type,assetId:action.assetId,cost,hpBefore:before,hpAfter:entry[1].hp};
      }else if(type==='REPAIR_FACTION'){
        const repair=repairFactionAmount(faction);if((faction.treasure??0)<repair.treasureCost)return reject('INSUFFICIENT_TREASURE',{cost:repair.treasureCost});
        if(!Number.isInteger(faction.hp)||!Number.isInteger(faction.maxHp))return reject('FACTION_HP_EVIDENCE_REQUIRED');
        const before=faction.hp;faction.treasure-=repair.treasureCost;faction.hp=Math.min(faction.maxHp,faction.hp+repair.hpRepaired);resolution={type,cost:repair.treasureCost,hpBefore:before,hpAfter:faction.hp};
      }else if(type==='CREATE_ASSET'){
        const eligibility=createAssetEligibility({faction,asset:action.assetDefinition,locationHasOwnBase:action.locationHasOwnBase===true});if(!eligibility.eligible)return reject(`CREATE_${eligibility.status}`,{eligibility});
        const asset=structuredClone(action.assetDefinition);if(!(asset.assetId??asset.id))return reject('CANONICAL_ASSET_ID_REQUIRED');
        faction.treasure-=eligibility.cost;addAsset(faction,asset);resolution={type,assetId:asset.assetId??asset.id,cost:eligibility.cost};
      }else if(type==='HIDE_ASSET'){
        const entry=findAsset(faction,action.assetId);if(!entry)return reject('ASSET_NOT_FOUND');const eligibility=hideAssetEligibility({faction,asset:entry[1],rivalBasePresent:action.rivalBasePresent===true});if(!eligibility.eligible)return reject(`HIDE_${eligibility.status}`,{eligibility});
        faction.treasure-=eligibility.cost;const q=assetQualities(entry[1]);q.add('STEALTH');entry[1].qualities=[...q];resolution={type,assetId:action.assetId,cost:eligibility.cost};
      }else if(type==='SELL_ASSET'){
        const entry=findAsset(faction,action.assetId);if(!entry)return reject('ASSET_NOT_FOUND');const sold=sellAssetReturn(entry[1]);faction.treasure=(faction.treasure??0)+sold.treasureGained;deleteAsset(faction,entry);resolution={type,assetId:action.assetId,...sold};
      }else if(type==='EXPAND_INFLUENCE'){
        if(action.location===undefined)return reject('EXPAND_LOCATION_REQUIRED');if(!Array.isArray(action.existingOwnAssetIds)||action.existingOwnAssetIds.length<1)return reject('EXPAND_OWN_ASSET_AT_LOCATION_REQUIRED');
        const sameLocation=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
        const present=action.existingOwnAssetIds.some(id=>{const e=findAsset(faction,id);return e&&sameLocation(assetLocation(e[1]),action.location);});if(!present)return reject('EXPAND_OWN_ASSET_AT_LOCATION_REQUIRED');
        const cost=expandInfluenceCost(action.baseHp);if((faction.treasure??0)<cost.treasureCost)return reject('INSUFFICIENT_TREASURE',{cost:cost.treasureCost});
        const rivals=Array.isArray(action.rivalFactionIds)?action.rivalFactionIds:[];
        const baseId=action.baseId??`boi:${turn.currentFactionId}:${Array.isArray(faction.basesOfInfluence)?faction.basesOfInfluence.length+1:1}`;
        const base={baseId,location:structuredClone(action.location),hp:action.baseHp,maxHp:action.baseHp};
        if(rivals.length===0){
          faction.treasure-=cost.treasureCost;if(!Array.isArray(faction.basesOfInfluence))faction.basesOfInfluence=[];faction.basesOfInfluence.push(base);resolution={type,baseId,location:action.location,baseHp:action.baseHp,cost:cost.treasureCost,contested:false};
        }else{
          // SRD: create the Base, then Cunning-vs-Cunning against every rival with an Asset at this location.
          // A winning rival may make an immediate Attack. Attack/defense choices are explicit contracts; they are never invented.
          if(!action.contestDecisions||typeof action.contestDecisions!=='object')return reject('EXPAND_CONTEST_CONTRACT_REQUIRED',{rivalFactionIds:rivals});
          for(const rivalId of rivals){const rival=next.factionState?.[rivalId];if(!rival)return reject('RIVAL_FACTION_NOT_FOUND',{rivalFactionId:rivalId});const hasRivalAsset=assetEntries(rival).some(([,a])=>!a?.destroyed&&sameLocation(assetLocation(a),action.location));if(!hasRivalAsset)return reject('RIVAL_ASSET_AT_LOCATION_REQUIRED',{rivalFactionId:rivalId});}
          rng=DeterministicRng.fromState(state.rngState);const contestChecks=[];const winningRivals=[];
          for(const rivalId of rivals){const rival=next.factionState[rivalId];const check=resolveFactionAttributeCheck({rng,attackerAttribute:faction.cunning,defenderAttribute:rival.cunning,streamName:`faction/${turn.currentFactionId}/expand/${baseId}/${rivalId}`});contestChecks.push({rivalFactionId:rivalId,check});if(check.defenderWins)winningRivals.push(rivalId);}
          // Preflight every immediate-attack decision before any persisted mutation. A rival can explicitly decline.
          for(const rivalId of winningRivals){const decision=action.contestDecisions[rivalId];if(!decision||typeof decision!=='object'||(!decision.decline&&!Array.isArray(decision.attacks)))return reject('EXPAND_CONTEST_ATTACK_DECISION_REQUIRED',{rivalFactionId:rivalId,contestChecks});if(decision.decline!==true&&decision.attacks.length<1)return reject('EXPAND_CONTEST_ATTACK_DECISION_REQUIRED',{rivalFactionId:rivalId,contestChecks});}
          faction.treasure-=cost.treasureCost;if(!Array.isArray(faction.basesOfInfluence))faction.basesOfInfluence=[];faction.basesOfInfluence.push(base);
          const immediateAttacks=[];
          for(const rivalId of winningRivals){const decision=action.contestDecisions[rivalId];if(decision.decline===true){immediateAttacks.push({rivalFactionId:rivalId,declined:true});continue;}const rival=next.factionState[rivalId];
            for(const plan of decision.attacks){
              const attackerEntry=findAsset(rival,plan?.attackerAssetId);if(!attackerEntry)return reject('EXPAND_ATTACKER_ASSET_NOT_FOUND',{rivalFactionId:rivalId,attackerAssetId:plan?.attackerAssetId});if(attackerEntry[1].destroyed)return reject('EXPAND_ATTACKER_ASSET_DESTROYED',{rivalFactionId:rivalId,attackerAssetId:plan.attackerAssetId});if(!sameLocation(assetLocation(attackerEntry[1]),action.location))return reject('EXPAND_ATTACKER_NOT_AT_LOCATION',{rivalFactionId:rivalId,attackerAssetId:plan.attackerAssetId});
              const atk=attackerEntry[1].attackContract;if(!atk||!atk.attackAttribute||!atk.defendAttribute||!atk.damage)return reject('EXPAND_ATTACK_CONTRACT_REQUIRED',{rivalFactionId:rivalId,attackerAssetId:plan.attackerAssetId});
              let defendingAsset=null,counter=null,defenderAttributeValue;
              if(plan.defenderAssetId){const de=findAsset(faction,plan.defenderAssetId);if(!de)return reject('EXPAND_DEFENDER_ASSET_NOT_FOUND',{defenderAssetId:plan.defenderAssetId});if(de[1].destroyed||!sameLocation(assetLocation(de[1]),action.location))return reject('EXPAND_DEFENDER_NOT_AVAILABLE',{defenderAssetId:plan.defenderAssetId});defendingAsset=de[1];counter=defendingAsset.counterattackContract??null;defenderAttributeValue=attributeValue(faction,atk.defendAttribute);}else{defenderAttributeValue=attributeValue(faction,atk.defendAttribute);}
              const attackResult=resolveFactionAttack({rng,attackerAttribute:attributeValue(rival,atk.attackAttribute),defenderAttribute:defenderAttributeValue,attackDamage:atk.damage,counterDamage:counter?.damage??null,attackerDice:atk.dice??1,defenderDice:counter?.defenseDice??1,streamName:`faction/${rivalId}/expand-attack/${baseId}/${plan.attackerAssetId}`});
              if(attackResult.damageToDefender){if(defendingAsset){if(!Number.isInteger(defendingAsset.hp))return reject('EXPAND_DEFENDER_ASSET_HP_EVIDENCE_REQUIRED',{defenderAssetId:plan.defenderAssetId});defendingAsset.hp=Math.max(0,defendingAsset.hp-attackResult.damageToDefender);if(defendingAsset.hp===0)defendingAsset.destroyed=true;}else{const applied=Math.min(base.hp,attackResult.damageToDefender);base.hp-=applied;if(!Number.isInteger(faction.hp))return reject('FACTION_HP_EVIDENCE_REQUIRED');faction.hp=Math.max(0,faction.hp-applied);if(base.hp===0)base.destroyed=true;}}
              if(attackResult.damageToAttacker){if(!Number.isInteger(attackerEntry[1].hp))return reject('EXPAND_ATTACKER_ASSET_HP_EVIDENCE_REQUIRED',{attackerAssetId:plan.attackerAssetId});attackerEntry[1].hp=Math.max(0,attackerEntry[1].hp-attackResult.damageToAttacker);if(attackerEntry[1].hp===0)attackerEntry[1].destroyed=true;}
              immediateAttacks.push({rivalFactionId:rivalId,attackerAssetId:plan.attackerAssetId,defenderAssetId:plan.defenderAssetId??null,targetBase:!plan.defenderAssetId,result:attackResult,baseHpAfter:base.hp});
              if(base.destroyed)break;
            }
            if(base.destroyed)break;
          }
          if(base.destroyed)faction.basesOfInfluence=faction.basesOfInfluence.filter(b=>b.baseId!==baseId);
          next.rngState=rng.exportState();resolution={type,baseId,location:action.location,baseHp:action.baseHp,baseHpAfter:base.hp,baseSurvived:!base.destroyed,cost:cost.treasureCost,contested:true,contestChecks,winningRivals,immediateAttacks};
        }
      }else if(type==='ATTACK'){
        const attackerEntry=findAsset(faction,action.attackerAssetId);if(!attackerEntry)return reject('ATTACKER_ASSET_NOT_FOUND');const defender=next.factionState?.[action.defenderFactionId];if(!defender)return reject('DEFENDER_FACTION_NOT_FOUND');const defenderEntry=findAsset(defender,action.defenderAssetId);if(!defenderEntry)return reject('DEFENDER_ASSET_NOT_FOUND');
        const atk=attackerEntry[1].attackContract;if(!atk||!atk.attackAttribute||!atk.defendAttribute||!atk.damage)return reject('ATTACK_CONTRACT_REQUIRED');const counter=defenderEntry[1].counterattackContract??null;
        rng=DeterministicRng.fromState(state.rngState);const result=resolveFactionAttack({rng,attackerAttribute:attributeValue(faction,atk.attackAttribute),defenderAttribute:attributeValue(defender,atk.defendAttribute),attackDamage:atk.damage,counterDamage:counter?.damage??null,attackerDice:atk.dice??1,defenderDice:counter?.defenseDice??1,streamName:`faction/${turn.currentFactionId}/attack/${action.attackerAssetId}`});
        if(result.damageToDefender){if(!Number.isInteger(defenderEntry[1].hp))return reject('DEFENDER_ASSET_HP_EVIDENCE_REQUIRED');defenderEntry[1].hp=Math.max(0,defenderEntry[1].hp-result.damageToDefender);if(defenderEntry[1].hp===0)defenderEntry[1].destroyed=true;}
        if(result.damageToAttacker){if(!Number.isInteger(attackerEntry[1].hp))return reject('ATTACKER_ASSET_HP_EVIDENCE_REQUIRED');attackerEntry[1].hp=Math.max(0,attackerEntry[1].hp-result.damageToAttacker);if(attackerEntry[1].hp===0)attackerEntry[1].destroyed=true;}
        next.rngState=rng.exportState();resolution={type,attackerAssetId:action.attackerAssetId,defenderFactionId:action.defenderFactionId,defenderAssetId:action.defenderAssetId,result};
      }else return reject('UNSUPPORTED_FACTION_ACTION');
    }catch(error){return reject('INVALID_FACTION_ACTION_CONTRACT',{reason:error.message});}
    turn.phase='GOAL_REVIEW_REQUIRED';turn.lastAction=resolution;
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'FACTION_ACTION_RESOLVED',eventSuffix:'faction-action',payload:{factionId:turn.currentFactionId,resolution,nextPhase:turn.phase}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:'GOAL_REVIEW_REQUIRED',receipt,resolution,faction:structuredClone(faction)});
  }

  reviewGoal({campaignId,outcome}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});
    const ctx=currentContext(state);if(ctx.status!=='OK')return Object.freeze({status:`${ctx.status}_NO_MUTATION`,revision:loaded.revision});
    if(ctx.turn.phase!=='GOAL_REVIEW_REQUIRED')return Object.freeze({status:'GOAL_REVIEW_NOT_CURRENT_PHASE_NO_MUTATION',phase:ctx.turn.phase,revision:loaded.revision});
    if(!outcome||typeof outcome!=='object'||!outcome.type)return Object.freeze({status:'MISSING_GOAL_OUTCOME_NO_MUTATION',revision:loaded.revision});
    const type=String(outcome.type).toUpperCase();const next=structuredClone(state);const turn=next.worldState.factionTurn;const faction=next.factionState[turn.currentFactionId];let result={type};
    if(type==='COMPLETED'){
      if(!Number.isInteger(outcome.difficulty)||outcome.difficulty<1)return Object.freeze({status:'GOAL_DIFFICULTY_REQUIRED_NO_MUTATION',revision:loaded.revision});
      if(outcome.newGoal===undefined)return Object.freeze({status:'NEW_GOAL_REQUIRED_NO_MUTATION',revision:loaded.revision});
      faction.xp=(faction.xp??0)+outcome.difficulty;faction.goal=structuredClone(outcome.newGoal);result={type,xpGained:outcome.difficulty,newGoal:faction.goal};
    }else if(type==='ABANDONED'){
      if(outcome.newGoal===undefined)return Object.freeze({status:'NEW_GOAL_REQUIRED_NO_MUTATION',revision:loaded.revision});
      faction.goal=structuredClone(outcome.newGoal);faction.skipNextActionAndSpecials=true;result={type,newGoal:faction.goal,skipNextActionAndSpecials:true};
    }else if(type==='CONTINUE')result={type};
    else return Object.freeze({status:'INVALID_GOAL_OUTCOME_NO_MUTATION',revision:loaded.revision});
    const completedFactionId=turn.currentFactionId;turn.currentIndex+=1;
    if(turn.currentIndex>=turn.initiativeOrder.length){turn.phase='TURN_COMPLETE';turn.currentFactionId=null;turn.completedAtRevision=loaded.revision+1;}
    else{turn.currentFactionId=turn.initiativeOrder[turn.currentIndex];const nextFaction=next.factionState[turn.currentFactionId];if(!nextFaction)return Object.freeze({status:'MISSING_NEXT_FACTION_NO_MUTATION',revision:loaded.revision});const income=factionIncome(nextFaction);nextFaction.treasure=(nextFaction.treasure??0)+income;nextFaction.lastIncome=income;turn.phase='UPKEEP_REQUIRED';turn.specialRequirements=[];turn.lastAction=null;turn.skipConsumed=false;result.nextFactionIncome=income;}
    const {receipt}=commit({persistence:this.persistence,loaded,state:next,campaignId,eventKind:'FACTION_GOAL_REVIEWED',eventSuffix:'faction-goal',payload:{factionId:completedFactionId,result,nextFactionId:turn.currentFactionId,nextPhase:turn.phase}});
    if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});
    return Object.freeze({status:turn.phase,receipt,result,turn:structuredClone(turn),factions:structuredClone(next.factionState)});
  }
}
