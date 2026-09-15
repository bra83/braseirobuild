from pathlib import Path
import re

def ensure_named_import(text,module,names):
    pat=re.compile(r"import \{ ([^}]*) \} from '"+re.escape(module)+r"';")
    m=pat.search(text)
    if not m:raise SystemExit(f'import marker missing: {module}')
    current=[x.strip() for x in m.group(1).split(',') if x.strip()]
    for name in names:
        if name not in current:current.append(name)
    return text[:m.start()]+"import { "+', '.join(current)+" } from '"+module+"';"+text[m.end():]

def replace_once(text,old,new,label):
    if new in text:return text
    if old not in text:raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# --- combat service ---
p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=ensure_named_import(s,'./wwn-combat-core.js',['resolveAutomaticWeaponHitDamage'])
s=ensure_named_import(s,'./wwn-special-combat-core.js',['grappleStartOutcome','grappleEscapeOutcome'])
marker="import { isActorProne, setActorProneInPlace, proneAttackAdjustment, effectiveProneMoveFeet } from './combat-prone-state.js';"
imports=marker+"\nimport { grappleLinks, grappleTargetOf, grappleAssailantsOf, actorGrappleState, startGrappleInPlace, endGrappleInPlace, endAllGrapplesForActorInPlace, isActorGrappleLocked } from './combat-grapple-state.js';\nimport { inspectCharacterHands } from './combat-hand-state.js';"
s=replace_once(s,marker,imports,'grapple imports')

# Restrictions on movement and weapon use.
old="const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,actorId);"
new="const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(isActorGrappleLocked(combat,actorId))return Object.freeze({status:'GRAPPLED_CANNOT_FIGHTING_WITHDRAWAL_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,actorId);"
# Only the fightingWithdrawal occurrence should be changed; use the unique method prefix region.
fw="  fightingWithdrawal({campaignId,pcId=null}){\n    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});"+old
fw_new="  fightingWithdrawal({campaignId,pcId=null}){\n    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});"+new
s=replace_once(s,fw,fw_new,'grapple withdrawal restriction')
move_old="const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});\n    const from=combat.tactical.actors?.[actorId];"
move_new="const pc=characterById(state,actorId);if(!isCombatCapablePc(pc))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(isActorGrappleLocked(combat,actorId))return Object.freeze({status:'GRAPPLED_CANNOT_MOVE_NO_MUTATION',revision:loaded.revision,grapple:actorGrappleState(combat,actorId)});\n    const from=combat.tactical.actors?.[actorId];"
s=replace_once(s,move_old,move_new,'grapple movement restriction')
attack_old="const attacker=characterById(state,actorId);if(!attacker)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});if(combat&&!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});\n    const weaponResult=playerWeapon"
attack_new="const attacker=characterById(state,actorId);if(!attacker)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});if(combat&&!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(combat&&isActorGrappleLocked(combat,actorId)&&weaponId!=='UNARMED')return Object.freeze({status:'GRAPPLE_UNARMED_ONLY_NO_MUTATION',revision:loaded.revision,grapple:actorGrappleState(combat,actorId)});\n    const weaponResult=playerWeapon"
s=replace_once(s,attack_old,attack_new,'grapple attack restriction')

# Round-end automatic grapple damage and enemy escape policy.
advance_marker="function advanceSide(next,combat){"
helpers="""function applyGrappleEndRoundDamageInPlace(next,combat,completedRound){
  const active=[...grappleLinks(combat)];if(!active.length)return Object.freeze([]);const rng=DeterministicRng.fromState(next.rngState),records=[];
  for(const link of active){const assailant=characterById(next,link.assailantId),target=next.npcState?.[link.targetId];if(!assailant||!isCombatCapablePc(assailant)){endGrappleInPlace(combat,link);records.push(Object.freeze({...link,status:'ENDED_ASSAILANT_INCAPACITATED'}));continue;}if(!target||!Number.isInteger(target.hp)||target.hp<1){endGrappleInPlace(combat,link);records.push(Object.freeze({...link,status:'ENDED_TARGET_UNAVAILABLE'}));continue;}const weapon=unarmedWeaponContract({attribute:chooseUnarmedAttribute(assailant)}),damage=resolveAutomaticWeaponHitDamage({rng,attacker:assailant,weapon,streamName:`combat/grapple/end-round/${completedRound}/${link.assailantId}/${link.targetId}`});const hpBefore=target.hp;next.npcState[link.targetId]={...target,hp:Math.max(0,target.hp-damage.damage)};const hpAfter=next.npcState[link.targetId].hp;records.push(Object.freeze({...link,status:'DAMAGE_APPLIED',damage,hpBefore,hpAfter}));if(hpAfter===0){if(next.npcState[link.targetId].importantNamed!==true)next.npcState[link.targetId].visibleStatus='DEAD';endAllGrapplesForActorInPlace(combat,link.targetId);}}
  next.rngState=rng.exportState();const alive=(combat.enemyIds??[]).filter(id=>(next.npcState?.[id]?.hp??0)>0);if(alive.length===0){combat.active=false;combat.outcome='PC_VICTORY';combat.endedRound=completedRound;}return Object.freeze(records);
}
function resolveEnemyGrappleEscapeInPlace(next,combat,rng,enemyId){
  const raw=[...grappleAssailantsOf(combat,enemyId)];if(!raw.length)return null;const enemy=next.npcState?.[enemyId];if(!enemy||!Number.isInteger(enemy.skillBonus))return Object.freeze({status:'MISSING_GRAPPLE_ESCAPE_SKILL_EVIDENCE',handled:true});const assailants=raw.filter(id=>isCombatCapablePc(characterById(next,id)));for(const stale of raw.filter(id=>!assailants.includes(id)))endGrappleInPlace(combat,{assailantId:stale,targetId:enemyId});if(!assailants.length)return Object.freeze({status:'STALE_GRAPPLE_CLEARED',handled:false});const size=shoveSizePlan(enemy);if(size.status!=='OK')return Object.freeze({status:size.status,handled:true});const victimRoll=resolveNpcManeuverSkillRoll({rng,skillBonus:enemy.skillBonus,streamName:`combat/grapple/escape/${enemyId}/victim`}),assailantRolls=[];for(const pcId of assailants){const pc=characterById(next,pcId);if(!Number.isInteger(pc?.attributes?.Strength))return Object.freeze({status:'MISSING_GRAPPLE_ASSAILANT_STRENGTH',handled:true,pcId});assailantRolls.push(Object.freeze({pcId,roll:resolvePcManeuverSkillRoll({rng,attributeScore:pc.attributes.Strength,skillLevel:Number.isInteger(pc.skills?.Punch)?pc.skills.Punch:null,modifier:size.plan.skillCheckModifier,streamName:`combat/grapple/escape/${enemyId}/assailant/${pcId}`})}));}const escape=grappleEscapeOutcome({victimOpposedTotal:victimRoll.total,assailantOpposedTotals:assailantRolls.map(x=>x.roll.total),victimIsPc:false,assailantsArePc:assailantRolls.map(()=>true)});if(escape.escaped)for(const pcId of assailants)endGrappleInPlace(combat,{assailantId:pcId,targetId:enemyId});return Object.freeze({status:'OK',handled:true,actionType:'MAIN',kind:'GRAPPLE_ESCAPE',enemyId,victimRoll,assailantRolls:Object.freeze(assailantRolls),escape});
}

"""
if 'function applyGrappleEndRoundDamageInPlace(' not in s:
    if advance_marker not in s:raise SystemExit('advanceSide marker missing')
    s=s.replace(advance_marker,helpers+advance_marker,1)
old="if(i===combat.order.length-1){const completedRound=combat.round;combat.round+=1;combat.activeSideId=combat.order[0];resetAllEconomies(combat);effectExpirations=[...expireRoundEffectsInPlace071({state:next,completedRound})];mortalInjury=advanceMortalInjuryClocks(next,combat);transition='NEW_ROUND';}"
new="if(i===combat.order.length-1){const completedRound=combat.round;const grappleEndRound=[...applyGrappleEndRoundDamageInPlace(next,combat,completedRound)];if(combat.active===false)return{transition:'COMBAT_ENDED',mortalInjury,effectExpirations,targetTurnExpirations,grappleEndRound};combat.round+=1;combat.activeSideId=combat.order[0];resetAllEconomies(combat);effectExpirations=[...expireRoundEffectsInPlace071({state:next,completedRound})];mortalInjury=advanceMortalInjuryClocks(next,combat);transition='NEW_ROUND';return{transition,mortalInjury,effectExpirations,targetTurnExpirations,grappleEndRound};}"
s=replace_once(s,old,new,'grapple round-end hook')

# Enemy target spends its Main Action attempting to escape instead of moving/using a weapon.
old="for(const enemyId of c.enemyIds??[]){const enemy=next.npcState[enemyId],pos=c.tactical.actors[enemyId];if(!enemy||enemy.hp<1||!pos)continue;const targetChoice=nearestStandingPc(next,c,pos);"
new="for(const enemyId of c.enemyIds??[]){const enemy=next.npcState[enemyId],pos=c.tactical.actors[enemyId];if(!enemy||enemy.hp<1||!pos)continue;const grappleEscape=resolveEnemyGrappleEscapeInPlace(next,c,rng,enemyId);if(grappleEscape?.handled){if(grappleEscape.status!=='OK')return Object.freeze({status:`${grappleEscape.status}_NO_MUTATION`,revision:loaded.revision,grappleEscape});actions.push({enemyId,grappleEscape});continue;}const targetChoice=nearestStandingPc(next,c,pos);"
s=replace_once(s,old,new,'enemy grapple escape hook')

# Stateful grapple-start command.
method="""
  grappleNpc({campaignId,pcId=null,npcId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const attacker=characterById(state,actorId);if(!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});const existing=grappleTargetOf(combat,actorId);if(existing)return Object.freeze({status:existing===npcId?'GRAPPLE_ALREADY_ACTIVE_NO_MUTATION':'ASSAILANT_ALREADY_GRAPPLING_NO_MUTATION',revision:loaded.revision,targetId:existing});const economy=mainAvailableWithEcho(state,combat,actorId);if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});const hands=inspectCharacterHands(attacker);if(hands.status!=='OK')return Object.freeze({status:`${hands.status}_NO_MUTATION`,revision:loaded.revision,hands});if(hands.bothHandsFree!==true)return Object.freeze({status:'BOTH_HANDS_REQUIRED_NO_MUTATION',revision:loaded.revision,hands});
    const npc=state.npcState?.[npcId];if(!npc||!Number.isInteger(npc.hp)||npc.hp<1||!Number.isInteger(npc.meleeAC)||!Number.isInteger(npc.skillBonus))return Object.freeze({status:'MISSING_EVIDENCE_TARGET_COMBAT_STATE',revision:loaded.revision});const size=shoveSizePlan(npc);if(size.status!=='OK')return Object.freeze({status:`${size.status}_NO_MUTATION`,revision:loaded.revision});const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const weapon=unarmedWeaponContract({attribute:chooseUnarmedAttribute(attacker)}),range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon});if(range.status!=='OK')return Object.freeze({status:'TARGET_OUT_OF_RANGE_NO_MUTATION',revision:loaded.revision,range});const proneModifiers=proneAttackAdjustment({combat,attackerId:actorId,targetId:npcId,weapon,attackerPosition:from,targetPosition:to}),rng=DeterministicRng.fromState(state.rngState),attack=resolveAttackHitOnly({rng,attacker,weapon,target:npc,hitModifier:range.hitModifier+proneModifiers.hitModifier,streamName:`combat/grapple/${actorId}/${npcId}/attack`});let attackerRoll=null,defenderRoll=null,outcome=grappleStartOutcome({unarmedAttackHit:false,bothHandsFree:true,attackerIsPc:true,defenderIsPc:false});if(attack.hit){attackerRoll=resolvePcManeuverSkillRoll({rng,attributeScore:attacker.attributes.Strength,skillLevel:Number.isInteger(attacker.skills?.Punch)?attacker.skills.Punch:null,modifier:size.plan.skillCheckModifier,streamName:`combat/grapple/${actorId}/${npcId}/pc`});defenderRoll=resolveNpcManeuverSkillRoll({rng,skillBonus:npc.skillBonus,streamName:`combat/grapple/${actorId}/${npcId}/npc`});outcome=grappleStartOutcome({unarmedAttackHit:true,bothHandsFree:true,attackerOpposedTotal:attackerRoll.total,defenderOpposedTotal:defenderRoll.total,attackerIsPc:true,defenderIsPc:false});}
    const next=clone(state);next.rngState=rng.exportState();const c=next.encounterState.combat,actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});let grappleState=null;if(outcome.success){grappleState=startGrappleInPlace(c,{assailantId:actorId,targetId:npcId,round:c.round});if(grappleState.status!=='OK')return Object.freeze({status:`${grappleState.status}_NO_MUTATION`,revision:loaded.revision});}const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});if(characterById(next,actorId)?.hp?.current===0)markPcMortallyInjured(next,c,actorId);next.encounterState.lastCombatResolution={kind:'GRAPPLE_START',pcId:actorId,npcId,attack,attackerRoll,defenderRoll,outcome,hands,size,grappleState,targetHpBefore:npc.hp,targetHpAfter:npc.hp};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_GRAPPLE_START',suffix:`grapple:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,attack,attackerRoll,defenderRoll,outcome,hands,size,grappleState,actionCost,tumefaction:tumefaction?.status??null,targetHpBefore:npc.hp,targetHpAfter:npc.hp,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.7.1 Shoving and Grappling'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,attack,attackerRoll,defenderRoll,outcome,hands,size,grappleState,actionCost,targetHpBefore:npc.hp,targetHpAfter:npc.hp,combat:clone(c)});
  }

"""
marker="  shoveNpc({"
if '  grappleNpc({' not in s:
    if marker not in s:raise SystemExit('shove method marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

# --- session wrapper ---
p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text()
old="  async shoveNpc(args){return this._narrate(args.campaignId,this.actions.shoveNpc(args),'PLAYER_SHOVE');}"
new=old+"\n  async grappleNpc(args){return this._narrate(args.campaignId,this.actions.grappleNpc(args),'PLAYER_GRAPPLE_START');}"
s=replace_once(s,old,new,'session grapple wrapper');p.write_text(s)

# --- player projection ---
p=Path('app/src/main/assets/www/core/combat-projection.js');s=p.read_text()
marker="import { isActorProne, effectiveProneMoveFeet } from './combat-prone-state.js';"
imports=marker+"\nimport { actorGrappleState } from './combat-grapple-state.js';\nimport { inspectCharacterHands } from './combat-hand-state.js';"
s=replace_once(s,marker,imports,'projection grapple imports')
s=replace_once(s,
"classId:pc?.classId??pc?.class??null,level:pc?.level??null,prone:isActorProne(combat,id),effectiveMoveFeet:proneMove?.moveFeet??null,weapons:projectWeapons(pc),",
"classId:pc?.classId??pc?.class??null,level:pc?.level??null,prone:isActorProne(combat,id),effectiveMoveFeet:proneMove?.moveFeet??null,grapple:actorGrappleState(combat,id),hands:inspectCharacterHands(pc),weapons:projectWeapons(pc),",
'projection PC grapple')
s=replace_once(s,
"defeated:(npc?.hp??0)<=0,prone:isActorProne(combat,id)});",
"defeated:(npc?.hp??0)<=0,prone:isActorProne(combat,id),grapple:actorGrappleState(combat,id)});",
'projection enemy grapple');p.write_text(s)

# --- combat UI ---
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="shovePunchButton" class="weapon-choice" type="button"><b>DERRUBAR • FOR/BRIGA</b><small>Ataque melee sem dano, seguido de disputa de perícia.</small></button>'
new=old+'<button id="grappleButton" class="weapon-choice" type="button"><b>AGARRAR • FOR/BRIGA</b><small>Exige as duas mãos livres; ataque desarmado sem dano seguido de disputa.</small></button>'
s=replace_once(s,old,new,'grapple menu button');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
old="function currentWeapon(pc){const choices=weaponChoices(pc);if(!choices.length)return null;const remembered=selectedWeaponByPc.get(pc.actorId);let choice=choices.find(x=>x.choiceId===remembered);"
new="function currentWeapon(pc){const choices=weaponChoices(pc);if(!choices.length)return null;if(pc?.grapple?.locked===true){const unarmed=choices.find(x=>x.weaponId==='UNARMED')??choices[0];selectedWeaponByPc.set(pc.actorId,unarmed.choiceId);return unarmed;}const remembered=selectedWeaponByPc.get(pc.actorId);let choice=choices.find(x=>x.choiceId===remembered);"
s=replace_once(s,old,new,'grapple force unarmed')
s=replace_once(s,
"$('weaponButton').disabled=weaponChoices(pc).length<2;",
"$('weaponButton').disabled=pc.grapple?.locked===true||weaponChoices(pc).length<2;",
'grapple weapon selector lock')
s=replace_once(s,
"$('moveButton').disabled=!pcTurn||pc.economy?.moveAvailable===false;",
"$('moveButton').disabled=!pcTurn||pc.economy?.moveAvailable===false||pc.grapple?.locked===true;",
'grapple move UI lock')
old="$('shoveExertButton').disabled=!shoveReady;$('shovePunchButton').disabled=!shoveReady;$('reloadButton').disabled="
new="$('shoveExertButton').disabled=!shoveReady;$('shovePunchButton').disabled=!shoveReady;const alreadyAssailant=(pc.grapple?.asAssailant?.length??0)>0;$('grappleButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||pc.hands?.status!=='OK'||pc.hands?.bothHandsFree!==true||alreadyAssailant;$('grappleButton').querySelector('b').textContent=alreadyAssailant?'AGARRANDO ALVO':'AGARRAR • FOR/BRIGA';$('reloadButton').disabled="
s=replace_once(s,old,new,'grapple button render')
handler="""
$('grappleButton').onclick=async()=>{const p=projection(),pc=currentPc(p);if(!pc)return;const r=await session.grappleNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId});let fallback=r.status;if(r.status==='OK')fallback=!r.attack.hit?'O ataque desarmado do agarrão não acertou.':r.outcome.success?'Alvo agarrado. Ambos ficam sem movimento e só podem lutar desarmados.':'O ataque acertou, mas o alvo venceu a disputa e não foi agarrado.';logResult(r,fallback);closePopups();render()};
"""
marker="async function shoveWithSkill(opposedSkill){"
if "$('grappleButton').onclick" not in s:
    if marker not in s:raise SystemExit('shove handler marker missing')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)
print('WWN grapple stateful/player patch applied')
