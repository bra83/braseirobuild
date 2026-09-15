from pathlib import Path

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# --- combat-service.js ---
p=Path('app/src/main/assets/www/core/combat-service.js')
s=p.read_text()
s=replace_once(s,
"import { resolveSkillCheck } from './wwn-rules-core.js';",
"import { resolveSkillCheck } from './wwn-rules-core.js';\nimport { isActorProne, setActorProneInPlace, proneAttackAdjustment, effectiveProneMoveFeet } from './combat-prone-state.js';",
'combat prone import')
s=replace_once(s,
"    let range=null;let rangedMelee=null;let tacticalHitModifier=0;",
"    let range=null;let rangedMelee=null;let proneModifiers=null;let tacticalHitModifier=0;",
'attack prone declaration')
old="      rangedMelee=inspectRangedWeaponInMelee({weapon,attackerPosition:from,enemyPositions});if(rangedMelee.status!=='OK')return Object.freeze({status:`${rangedMelee.status}_NO_MUTATION`,revision:loaded.revision,rangedMelee});tacticalHitModifier+=rangedMelee.hitModifier;"
new=old+"\n      proneModifiers=proneAttackAdjustment({combat,attackerId:actorId,targetId:npcId,weapon,attackerPosition:from,targetPosition:to});tacticalHitModifier+=proneModifiers.hitModifier;"
s=replace_once(s,old,new,'player attack prone modifier')
s=replace_once(s,
"const baseMoveFeet=magicMove.moveFeet;const moveDistance=distanceFeet(from,to);",
"const proneMove=effectiveProneMoveFeet({combat,actorId,baseMoveFeet:magicMove.moveFeet});const baseMoveFeet=proneMove.moveFeet;const moveDistance=distanceFeet(from,to);",
'prone movement')
old="const baseRes=resolveNpcAttack({rng,npc:enemy,weapon:enemy.weaponContract,target:{armorClass:ac,meleeAC:ac,rangedAC:ac},streamName:`combat/free-attack/${enemyId}/${actorId}`});"
new="const proneAttackMods=proneAttackAdjustment({combat:nextCombat,attackerId:enemyId,targetId:actorId,weapon:enemy.weaponContract,attackerPosition:nextCombat.tactical.actors?.[enemyId],targetPosition:nextCombat.tactical.actors?.[actorId]});const baseRes=resolveNpcAttack({rng,npc:enemy,weapon:enemy.weaponContract,target:{armorClass:ac,meleeAC:ac,rangedAC:ac},hitModifier:proneAttackMods.hitModifier,streamName:`combat/free-attack/${enemyId}/${actorId}`});"
s=replace_once(s,old,new,'free attack prone modifier')
old="const baseAttack=resolveNpcAttack({rng,npc:enemy,weapon:enemy.weaponContract,target:{armorClass:ac,meleeAC:ac,rangedAC:ac},streamName:`combat/enemy/${enemyId}/${targetId}`});"
new="const proneAttackMods=proneAttackAdjustment({combat:c,attackerId:enemyId,targetId:targetId,weapon:enemy.weaponContract,attackerPosition:c.tactical.actors?.[enemyId],targetPosition:c.tactical.actors?.[targetId]});const baseAttack=resolveNpcAttack({rng,npc:enemy,weapon:enemy.weaponContract,target:{armorClass:ac,meleeAC:ac,rangedAC:ac},hitModifier:proneAttackMods.hitModifier,streamName:`combat/enemy/${enemyId}/${targetId}`});"
s=replace_once(s,old,new,'enemy attack prone modifier')
methods="""
  goProne({campaignId,pcId=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(isActorProne(combat,actorId))return Object.freeze({status:'ALREADY_PRONE_NO_MUTATION',revision:loaded.revision,pcId:actorId});
    const next=clone(state),c=next.encounterState.combat;const prone=setActorProneInPlace(c,actorId,true);const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PC_WENT_PRONE',suffix:`prone:${actorId}`,payload:{pcId:actorId,prone:true,actionType:'ON_TURN',sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Go Prone'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,prone:true,proneState:prone,economy:clone(c.economy?.[actorId]??freshActionEconomy())});
  }

  standUp({campaignId,pcId=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(!isActorProne(combat,actorId))return Object.freeze({status:'NOT_PRONE_NO_MUTATION',revision:loaded.revision,pcId:actorId});const economy=mainAvailableWithEcho(state,combat,actorId);const cost=economy.moveAvailable===true?'MOVE':economy.mainAvailable===true?'MAIN_AS_SECOND_MOVE':null;if(!cost)return Object.freeze({status:'MOVE_OR_MAIN_ACTION_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const next=clone(state),c=next.encounterState.combat;const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});const prone=setActorProneInPlace(c,actorId,false);const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PC_STOOD_UP',suffix:`stand-up:${actorId}`,payload:{pcId:actorId,prone:false,actionType:'MOVE',actionCost,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Stand Up'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,prone:false,proneState:prone,actionCost,economy:clone(c.economy[actorId])});
  }

"""
marker="  totalDefense({campaignId,pcId=null}){"
if '  goProne({campaignId,pcId=null}){' not in s:
    if marker not in s: raise SystemExit('totalDefense insertion marker missing')
    s=s.replace(marker,methods+marker,1)
p.write_text(s)

# --- combat-session-service.js ---
p=Path('app/src/main/assets/www/core/combat-session-service.js')
s=p.read_text()
old="  async totalDefense(args){return this._narrate(args.campaignId,this.actions.totalDefense(args),'TOTAL_DEFENSE');}"
new="  async goProne(args){return this._narrate(args.campaignId,this.actions.goProne(args),'PC_WENT_PRONE');}\n  async standUp(args){return this._narrate(args.campaignId,this.actions.standUp(args),'PC_STOOD_UP');}\n"+old
s=replace_once(s,old,new,'session prone methods')
p.write_text(s)

# --- combat-projection.js ---
p=Path('app/src/main/assets/www/core/combat-projection.js')
s=p.read_text()
s=replace_once(s,
"import { totalDefenseActive } from './wwn-defense-core.js';",
"import { totalDefenseActive } from './wwn-defense-core.js';\nimport { isActorProne, effectiveProneMoveFeet } from './combat-prone-state.js';",
'projection prone import')
old="const pc=characterById(state,id),position=combat.tactical?.actors?.[id],injury=pc?.mortalInjury??null;const armor=deriveArmorClass(pc);const movement=deriveCombatMoveFeet(pc);"
new="const pc=characterById(state,id),position=combat.tactical?.actors?.[id],injury=pc?.mortalInjury??null;const armor=deriveArmorClass(pc);const movement=deriveCombatMoveFeet(pc);const proneMove=movement.status==='OK'?effectiveProneMoveFeet({combat,actorId:id,baseMoveFeet:movement.moveFeet}):null;"
s=replace_once(s,old,new,'projection move')
old="classId:pc?.classId??pc?.class??null,level:pc?.level??null,weapons:projectWeapons(pc),"
new="classId:pc?.classId??pc?.class??null,level:pc?.level??null,prone:isActorProne(combat,id),effectiveMoveFeet:proneMove?.moveFeet??null,weapons:projectWeapons(pc),"
s=replace_once(s,old,new,'projection pc prone')
old="return Object.freeze({actorId:id,name:npc?.name??id,status:enemyVisibleStatus(npc),position:pos?clone(pos):null,distancesByPcId:Object.freeze(distances),distanceFeet:legacyPc?.actorId&&distances[legacyPc.actorId]!==undefined?distances[legacyPc.actorId]:null,defeated:(npc?.hp??0)<=0});"
new="return Object.freeze({actorId:id,name:npc?.name??id,status:enemyVisibleStatus(npc),position:pos?clone(pos):null,distancesByPcId:Object.freeze(distances),distanceFeet:legacyPc?.actorId&&distances[legacyPc.actorId]!==undefined?distances[legacyPc.actorId]:null,defeated:(npc?.hp??0)<=0,prone:isActorProne(combat,id)});"
s=replace_once(s,old,new,'projection enemy prone')
p.write_text(s)

# --- scr022.html ---
p=Path('app/src/main/assets/www/scr022.html')
s=p.read_text()
old='<button id="defenseButton" class="player-action">DEFESA TOTAL</button><button id="attackButton" class="primary player-action">ATACAR</button>'
new='<button id="defenseButton" class="player-action">DEFESA TOTAL</button><button id="proneButton" class="player-action">IR AO CHÃO</button><button id="attackButton" class="primary player-action">ATACAR</button>'
s=replace_once(s,old,new,'prone html button')
p.write_text(s)

# --- scr022.js ---
p=Path('app/src/main/assets/www/scr022.js')
s=p.read_text()
old="$('defenseButton').disabled=!pcTurn||pc.economy?.mainAvailable===false;$('defenseButton').textContent=pc.totalDefense?'DEFESA TOTAL • ATIVA':'DEFESA TOTAL';$('attackButton').disabled="
new="$('defenseButton').disabled=!pcTurn||pc.economy?.mainAvailable===false;$('defenseButton').textContent=pc.totalDefense?'DEFESA TOTAL • ATIVA':'DEFESA TOTAL';$('proneButton').disabled=!pcTurn||(pc.prone===true&&pc.economy?.moveAvailable===false&&pc.economy?.mainAvailable===false);$('proneButton').textContent=pc.prone===true?'LEVANTAR':'IR AO CHÃO';$('attackButton').disabled="
s=replace_once(s,old,new,'prone render button')
old="$('defenseButton').onclick=async()=>{const r=await session.totalDefense({campaignId,pcId:selectedPcId});logResult(r,r.status==='OK'?`Defesa Total ativa: CA ${r.armorClass} e imunidade a Choque até o próximo turno.`:r.status);render()};"
new=old+"\n$('proneButton').onclick=async()=>{const p=projection(),pc=currentPc(p);if(!pc)return;const r=pc.prone===true?await session.standUp({campaignId,pcId:pc.actorId}):await session.goProne({campaignId,pcId:pc.actorId});logResult(r,r.status==='OK'?(r.prone?'Você foi ao chão. Movimento reduzido pela metade; ataques seguem os modificadores de Prone.':'Você se levantou, consumindo uma ação de Movimento.') : r.status);render()};"
s=replace_once(s,old,new,'prone ui handler')
p.write_text(s)

print('WWN prone combat patch applied')
