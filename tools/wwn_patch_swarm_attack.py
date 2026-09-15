from pathlib import Path
import re


def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old, new, 1)


def ensure_named_import(text, module, names):
    pat = re.compile(r"import \{ ([^}]*) \} from '" + re.escape(module) + r"';")
    m = pat.search(text)
    if not m:
        raise SystemExit(f'import marker missing: {module}')
    current = [x.strip() for x in m.group(1).split(',') if x.strip()]
    for name in names:
        if name not in current:
            current.append(name)
    return text[:m.start()] + "import { " + ', '.join(current) + " } from '" + module + "';" + text[m.end():]

# --- reusable combat math ---
p = Path('app/src/main/assets/www/core/wwn-combat-core.js')
s = p.read_text()
if 'export function maximumWeaponHitDamage(' not in s:
    marker = "export function resolveAutomaticWeaponHitDamage({rng,attacker,weapon,damageBonus=0,streamName='automatic-hit'}){"
    helper = """
export function maximumWeaponHitDamage({attacker,weapon,damageBonus=0}){
  validateWeapon(weapon);if(!attacker)throw new TypeError('attacker required');requireInt(damageBonus,'damageBonus');const spec=parseDiceExpression(weapon.damage),attributeScore=attacker.attributes?.[weapon.attribute],attributeMod=attributeModifier(attributeScore),skill=combatSkillContribution(attacker.skills,weapon.combatSkill);let total=spec.count*spec.sides+spec.bonus+attributeMod+damageBonus;if(weapon.punchSkillAddsToDamage&&weapon.combatSkill==='Punch'&&skill.skillLevel!==null)total+=skill.skillLevel;return Object.freeze({rule:'WWN_MAXIMUM_WEAPON_HIT_DAMAGE_V1',damage:Math.max(0,total),diceMaximum:spec.count*spec.sides+spec.bonus,attributeModifier:attributeMod,skillLevel:skill.skillLevel,damageBonus,sourceRefs:[WWN_COMBAT_SOURCES.damage,WWN_COMBAT_SOURCES.weapons]});
}
export function weaponShockDamageFloor({attacker,weapon,shockBonus=0}){
  validateWeapon(weapon);if(!attacker)throw new TypeError('attacker required');requireInt(shockBonus,'shockBonus');if(!weapon.shock||weapon.melee===false)return Object.freeze({rule:'WWN_WEAPON_SHOCK_FLOOR_V1',applicable:false,damage:0,sourceRefs:[WWN_COMBAT_SOURCES.damage]});const attributeScore=attacker.attributes?.[weapon.attribute],attributeMod=attributeModifier(attributeScore);return Object.freeze({rule:'WWN_WEAPON_SHOCK_FLOOR_V1',applicable:true,damage:Math.max(0,weapon.shock.damage+attributeMod+shockBonus),attributeModifier:attributeMod,shockBonus,sourceRefs:[WWN_COMBAT_SOURCES.damage]});
}

"""
    if marker not in s:
        raise SystemExit('combat math insertion marker missing')
    s = s.replace(marker, helper + marker, 1)
p.write_text(s)

# --- stateful service ---
p = Path('app/src/main/assets/www/core/combat-service.js')
s = p.read_text()
s = ensure_named_import(s, './wwn-combat-core.js', ['maximumWeaponHitDamage', 'weaponShockDamageFloor'])
s = ensure_named_import(s, './wwn-special-combat-core.js', ['swarmAttackPlan'])
s = replace_once(s,
"function resetAllEconomies(combat){for(const id of Object.keys(combat.economy??{}))combat.economy[id]=freshActionEconomy();}",
"function resetAllEconomies(combat){for(const id of Object.keys(combat.economy??{}))combat.economy[id]=freshActionEconomy();combat.swarmAttacks={};}",
'swarm round reset')

start = s.index('  attackNpc({')
end = s.index('\n\n  chargeNpc({', start)
section = s[start:end]
section = replace_once(section,
"    let range=null;let rangedMelee=null;let proneModifiers=null;let charge=null;let attackFrom=null;let actionCostKind='MAIN';let tacticalHitModifier=0;",
"    let range=null;let rangedMelee=null;let proneModifiers=null;let charge=null;let swarmPlan=null;let swarmAssailantIds=[];let swarmMaximumDamage=null;let swarmShockFloor=null;let attackFrom=null;let actionCostKind='MAIN';let tacticalHitModifier=0;if(combat&&attackContext?.kind==='SWARM'){const entry=combat.swarmAttacks?.[npcId];swarmAssailantIds=entry?.round===combat.round?[...(entry.assailantIds??[])].filter(id=>id!==actorId&&isCombatCapablePc(characterById(state,id))).slice(0,3):[];swarmPlan=swarmAttackPlan({otherAssailants:swarmAssailantIds.length});tacticalHitModifier+=swarmPlan.hitBonus;}",
'swarm attack context')
section = section.replace('damageBonus:damageBonus+weaponBonuses.damageBonus,shockBonus:', 'damageBonus:damageBonus+weaponBonuses.damageBonus+(swarmPlan?.damageBonus??0),shockBonus:')
section = section.replace(' damageBonus:damageBonus+weaponBonuses.damageBonus, shockBonus:', ' damageBonus:damageBonus+weaponBonuses.damageBonus+(swarmPlan?.damageBonus??0), shockBonus:')
old = "    const callTomb=combat?applyCallOfTheTombToAttack071({state,targetId:npcId,round:combat.round,resolution:baseResolution,weapon}):{status:'NO_CALL_OF_TOMB_EFFECT',resolution:baseResolution};const npcDefense=callTomb.status==='APPLIED'?{resolution:callTomb.resolution,consumeShieldShock:false,grandPlate:null,shield:null,callTombBypass:true}:applyDefensesToAttack({character:npc,weapon,resolution:baseResolution,combat,targetId:npcId});let resolution=npcDefense.resolution;let wardpactSave=null,wardpact=null,wardpactWeapon=null;"
new = "    if(swarmPlan){swarmMaximumDamage=maximumWeaponHitDamage({attacker,weapon,damageBonus:damageBonus+weaponBonuses.damageBonus});if(baseResolution.hit===true&&Number(baseResolution.preShockDamage??0)>swarmMaximumDamage.damage){const cappedPre=swarmMaximumDamage.damage,cappedDamage=baseResolution.shockEligible?Math.max(cappedPre,Number(baseResolution.shockDamage??0)):cappedPre;baseResolution=Object.freeze({...baseResolution,preShockDamage:cappedPre,damage:cappedDamage,swarmDamageCapped:true,swarmMaximumDamage:swarmMaximumDamage.damage});}}\n    const callTomb=combat?applyCallOfTheTombToAttack071({state,targetId:npcId,round:combat.round,resolution:baseResolution,weapon}):{status:'NO_CALL_OF_TOMB_EFFECT',resolution:baseResolution};const npcDefense=callTomb.status==='APPLIED'?{resolution:callTomb.resolution,consumeShieldShock:false,grandPlate:null,shield:null,callTombBypass:true}:applyDefensesToAttack({character:npc,weapon,resolution:baseResolution,combat,targetId:npcId});let resolution=npcDefense.resolution;if(swarmPlan&&weapon.shock&&weapon.melee!==false&&!totalDefenseActive(combat,npcId)){swarmShockFloor=weaponShockDamageFloor({attacker,weapon,shockBonus:shockBonus+weaponBonuses.shockBonus});if(swarmShockFloor.applicable&&Number(resolution.damage??0)<swarmShockFloor.damage)resolution=Object.freeze({...resolution,damage:swarmShockFloor.damage,shockEligible:true,shockDamage:swarmShockFloor.damage,swarmShockAlwaysApplicable:true,damageKind:resolution.hit===true?'HIT':'SWARM_PRESSURE'});}let wardpactSave=null,wardpact=null,wardpactWeapon=null;"
section = replace_once(section, old, new, 'swarm cap and shock floor')
section = replace_once(section,
"      actionCost=applyActionCostWithEchoInPlace071({state:next,combat:nextCombat,targetId:actorId,cost:actionCostKind});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});",
"      actionCost=applyActionCostWithEchoInPlace071({state:next,combat:nextCombat,targetId:actorId,cost:actionCostKind});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});if(swarmPlan&&nextCombat.swarmAttacks)delete nextCombat.swarmAttacks[npcId];",
'swarm consume state')
section = replace_once(section,
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, proneModifiers, charge, attackContext, resolution,",
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, proneModifiers, charge, swarmPlan, swarmAssailantIds, swarmMaximumDamage, swarmShockFloor, attackContext, resolution,",
'swarm last result')
section = replace_once(section,
"eventKind:charge?'PLAYER_CHARGE':'PLAYER_ATTACK',suffix:charge?`charge:${actorId}:${npcId}`:`attack:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,charge,attackContext,resolution,",
"eventKind:charge?'PLAYER_CHARGE':swarmPlan?'PLAYER_SWARM_ATTACK':'PLAYER_ATTACK',suffix:charge?`charge:${actorId}:${npcId}`:swarmPlan?`swarm-attack:${actorId}:${npcId}`:`attack:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,charge,swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,attackContext,resolution,",
'swarm event')
section = replace_once(section,
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,proneModifiers,charge,attackContext,enfeeblingPair,",
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,proneModifiers,charge,swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,attackContext,enfeeblingPair,",
'swarm return')
s = s[:start] + section + s[end:]

if '  swarmAssistNpc({' not in s:
    marker = '  chargeNpc({campaignId,pcId=null,npcId,weaponId,modeId=null,combatSkill=null,xFeet,yFeet,physicalDice=null,enfeeblingPhysicalDiceSecond=null,wardpactPhysicalSaveDice=null}){'
    method = """
  swarmAssistNpc({campaignId,pcId=null,npcId,weaponId='UNARMED',modeId=null,combatSkill=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const attacker=characterById(state,actorId);if(!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(isActorGrappleLocked(combat,actorId)&&weaponId!=='UNARMED')return Object.freeze({status:'GRAPPLE_UNARMED_ONLY_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,actorId);if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});
    const weaponResult=playerWeapon(state,actorId,weaponId,{tactical:true,modeId,combatSkill});if(weaponResult.status!=='OK')return Object.freeze({status:weaponResult.status,revision:loaded.revision});const npc=state.npcState?.[npcId];if(!npc||!Number.isInteger(npc.hp)||npc.hp<1)return Object.freeze({status:'MISSING_EVIDENCE_TARGET_COMBAT_STATE',revision:loaded.revision});const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon:weaponResult.weapon});if(range.status!=='OK')return Object.freeze({status:'TARGET_OUT_OF_RANGE_NO_MUTATION',revision:loaded.revision,range});const entry=combat.swarmAttacks?.[npcId],existing=entry?.round===combat.round?[...(entry.assailantIds??[])]:[];if(existing.includes(actorId))return Object.freeze({status:'SWARM_ASSIST_ALREADY_DECLARED_NO_MUTATION',revision:loaded.revision,assailantIds:Object.freeze(existing)});if(existing.length>=3)return Object.freeze({status:'SWARM_ASSAILANT_CAP_REACHED_NO_MUTATION',revision:loaded.revision,assailantIds:Object.freeze(existing)});
    const next=clone(state),c=next.encounterState.combat;const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});c.swarmAttacks={...(c.swarmAttacks??{}),[npcId]:{round:c.round,assailantIds:Object.freeze([...existing,actorId])}};const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});if(characterById(next,actorId)?.hp?.current===0)markPcMortallyInjured(next,c,actorId);const record=c.swarmAttacks[npcId];next.encounterState.lastCombatResolution={kind:'SWARM_ASSIST',pcId:actorId,npcId,weaponId,modeId,range,record};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_SWARM_ASSIST',suffix:`swarm-assist:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,range,record,actionCost,tumefaction:tumefaction?.status??null,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Make a Swarm Attack'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,range,record:clone(record),actionCost,combat:clone(c)});
  }

  swarmAttackNpc(args){return this.attackNpc({...args,attackContext:Object.freeze({kind:'SWARM'})});}

"""
    if marker not in s:
        raise SystemExit('swarm methods insertion marker missing')
    s = s.replace(marker, method + marker, 1)
p.write_text(s)

# --- session wrappers ---
p = Path('app/src/main/assets/www/core/combat-session-service.js')
s = p.read_text()
old = "  async chargeNpc(args){return this._narrate(args.campaignId,this.actions.chargeNpc(args),'PLAYER_CHARGE');}"
new = "  async swarmAssistNpc(args){return this._narrate(args.campaignId,this.actions.swarmAssistNpc(args),'PLAYER_SWARM_ASSIST');}\n  async swarmAttackNpc(args){return this._narrate(args.campaignId,this.actions.swarmAttackNpc(args),'PLAYER_SWARM_ATTACK');}\n" + old
s = replace_once(s, old, new, 'swarm session wrappers')
p.write_text(s)

# --- mobile combat UI ---
p = Path('app/src/main/assets/www/scr022.html')
s = p.read_text()
old = '<button id="dualButton" class="weapon-choice" type="button"><b>DUAS ARMAS</b><small>Exige duas armas válidas e perícia 1 nas duas.</small></button>'
new = old + '<button id="swarmAssistButton" class="weapon-choice" type="button"><b>APOIAR ENXAME</b><small>Gasta sua Ação Principal e prepara bônus para outro aliado atacar este alvo.</small></button><button id="swarmAttackButton" class="weapon-choice" type="button"><b>ATACAR COM ENXAME</b><small>Usa os apoios deste turno: +2 para acertar e +1 dano por aliado, até +6/+3.</small></button>'
s = replace_once(s, old, new, 'swarm UI buttons')
p.write_text(s)

p = Path('app/src/main/assets/www/scr022.js')
s = p.read_text()
old = "$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').querySelector('b').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';const chargeWeapon="
new = "$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').querySelector('b').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';const swarmReady=pcTurn&&pc.economy?.mainAvailable!==false&&enemy&&!enemy.defeated&&choice?.canAttack===true;$('swarmAssistButton').disabled=!swarmReady;$('swarmAttackButton').disabled=!swarmReady;const chargeWeapon="
s = replace_once(s, old, new, 'render swarm buttons')
if "$('swarmAssistButton').onclick" not in s:
    marker = "function armGrappleManipulation(mode){"
    handlers = """
async function swarmAction(kind){const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice)return;if(kind==='ASSIST'){const r=await session.swarmAssistNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill});logResult(r,r.status==='OK'?`Apoio de enxame preparado. ${r.record?.assailantIds?.length??1} aliado(s) apoiando o alvo.`:r.status);closePopups();render();return;}let physical=null;if(physicalDice.available()){const formula=`1d20+${choice.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula} para o Ataque em Enxame…`;physical=await physicalDice.roll({formula});if(physical.status==='CANCELLED'){$('log').textContent='Rolagem cancelada. Nenhuma regra ou estado foi alterado.';return;}if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent='A rolagem física do Enxame não foi aceita. Nenhuma regra ou estado foi alterado.';return;}}const r=await session.swarmAttackNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=`Ataque em Enxame: +${r.swarmPlan?.hitBonus??0} para acertar, +${r.swarmPlan?.damageBonus??0} dano; ${r.resolution?.damage??0} dano final.`;logResult(r,fallback);closePopups();render()}
$('swarmAssistButton').onclick=()=>swarmAction('ASSIST');$('swarmAttackButton').onclick=()=>swarmAction('ATTACK');
"""
    if marker not in s:
        raise SystemExit('swarm handler marker missing')
    s = s.replace(marker, handlers + marker, 1)
p.write_text(s)
print('WWN canonical swarm attack patch applied')
