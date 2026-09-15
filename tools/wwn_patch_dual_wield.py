from pathlib import Path

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# combat-service
p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=replace_once(s,
"import { resolveSkillCheck } from './wwn-rules-core.js';",
"import { resolveSkillCheck } from './wwn-rules-core.js';\nimport { dualWieldPlan } from './wwn-special-combat-core.js';",
'dual wield import')
old="  attackNpc({ campaignId, pcId = null, npcId, weaponId, hitModifier = 0, damageBonus = 0, shockBonus = 0, modeId = null, combatSkill = null, physicalDice = null, enfeeblingPhysicalDiceSecond = null, wardpactPhysicalSaveDice = null }) {"
new="  attackNpc({ campaignId, pcId = null, npcId, weaponId, hitModifier = 0, damageBonus = 0, shockBonus = 0, modeId = null, combatSkill = null, physicalDice = null, enfeeblingPhysicalDiceSecond = null, wardpactPhysicalSaveDice = null, attackContext = null }) {"
s=replace_once(s,old,new,'attack context signature')
s=replace_once(s,
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, resolution, wardpactSave, wardpact, wardpactWeapon, targetHpBefore:npc.hp, targetHpAfter:next.npcState[npcId].hp }",
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, proneModifiers, attackContext, resolution, wardpactSave, wardpact, wardpactWeapon, targetHpBefore:npc.hp, targetHpAfter:next.npcState[npcId].hp }",
'last combat attack context')
s=replace_once(s,
"payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,resolution,enfeeblingPair:",
"payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,attackContext,resolution,enfeeblingPair:",
'attack payload context')
s=replace_once(s,
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,enfeeblingPair,tumefaction,wardpactSave,actionCost,targetHpAfter:next.npcState[npcId].hp,combat:next.encounterState.combat?clone(next.encounterState.combat):null});",
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,proneModifiers,attackContext,enfeeblingPair,tumefaction,wardpactSave,actionCost,targetHpAfter:next.npcState[npcId].hp,combat:next.encounterState.combat?clone(next.encounterState.combat):null});",
'attack return context')
method="""
  dualWieldAttackNpc({campaignId,pcId=null,npcId,primaryWeaponId,primaryModeId=null,primaryCombatSkill=null,secondaryWeaponId,secondaryModeId=null,secondaryCombatSkill=null,physicalDice=null,enfeeblingPhysicalDiceSecond=null,wardpactPhysicalSaveDice=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const attacker=characterById(state,actorId);if(!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});
    if(!primaryWeaponId||!secondaryWeaponId||primaryWeaponId===secondaryWeaponId||primaryWeaponId==='UNARMED'||secondaryWeaponId==='UNARMED')return Object.freeze({status:'TWO_DISTINCT_WEAPONS_REQUIRED_NO_MUTATION',revision:loaded.revision});
    const primary=playerWeapon(state,actorId,primaryWeaponId,{tactical:true,modeId:primaryModeId,combatSkill:primaryCombatSkill});if(primary.status!=='OK')return Object.freeze({status:`PRIMARY_${primary.status}_NO_MUTATION`,revision:loaded.revision});const secondary=playerWeapon(state,actorId,secondaryWeaponId,{tactical:true,modeId:secondaryModeId,combatSkill:secondaryCombatSkill});if(secondary.status!=='OK')return Object.freeze({status:`SECONDARY_${secondary.status}_NO_MUTATION`,revision:loaded.revision});
    if(primary.weapon?.traits?.includes?.('2H')||secondary.weapon?.traits?.includes?.('2H'))return Object.freeze({status:'TWO_HANDED_WEAPON_NOT_DUAL_WIELDABLE_NO_MUTATION',revision:loaded.revision});
    const primarySkill=attacker.skills?.[primary.weapon.combatSkill],secondarySkill=attacker.skills?.[secondary.weapon.combatSkill];const from=combat.tactical?.actors?.[actorId],to=combat.tactical?.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});
    let secondaryRange=null;try{secondaryRange=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon:secondary.weapon});}catch{return Object.freeze({status:'SECONDARY_RANGE_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision});}
    const plan=dualWieldPlan({primarySkillLevel:Number.isInteger(primarySkill)?primarySkill:0,secondarySkillLevel:Number.isInteger(secondarySkill)?secondarySkill:0,targetInRangeOfBoth:secondaryRange.status==='OK'});if(!plan.legal)return Object.freeze({status:'DUAL_WIELD_SKILL_1_REQUIRED_NO_MUTATION',revision:loaded.revision,plan,primarySkill:primarySkill??null,secondarySkill:secondarySkill??null});
    const attackContext=Object.freeze({kind:'DUAL_WIELD',primaryWeaponId,primaryModeId,primaryCombatSkill:primary.weapon.combatSkill,secondaryWeaponId,secondaryModeId,secondaryCombatSkill:secondary.weapon.combatSkill,secondaryRange,targetInRangeOfBoth:secondaryRange.status==='OK',plan});
    const result=this.attackNpc({campaignId,pcId:actorId,npcId,weaponId:primaryWeaponId,modeId:primaryModeId,combatSkill:primaryCombatSkill??primary.weapon.combatSkill,hitModifier:plan.hitModifier,damageBonus:plan.hitDamageBonus,shockBonus:0,physicalDice,enfeeblingPhysicalDiceSecond,wardpactPhysicalSaveDice,attackContext});return Object.freeze({...result,dualWield:attackContext});
  }

"""
marker="  goProne({campaignId,pcId=null}){"
if '  dualWieldAttackNpc({' not in s:
    if marker not in s: raise SystemExit('dual wield insertion marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

# session wrapper
p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text()
old="  async attackNpc(args){return this._narrate(args.campaignId,this.actions.attackNpc(args),'PLAYER_ATTACK');}"
new=old+"\n  async dualWieldAttackNpc(args){return this._narrate(args.campaignId,this.actions.dualWieldAttackNpc(args),'PLAYER_DUAL_WIELD_ATTACK');}"
s=replace_once(s,old,new,'session dual wield wrapper')
p.write_text(s)

# projection: expose the facts needed for player-side eligibility without duplicating rules in UI
p=Path('app/src/main/assets/www/core/combat-projection.js');s=p.read_text()
s=replace_once(s,
"summary:'1d2 + Briga • corpo a corpo',damageFormula:'1d2',carryState:'BUILT_IN',loaded:true,reloadTrait:null,readiness:'READY',canAttack:true,reloadRequired:false,sourceRefs:[WWN_EQUIPMENT_SOURCES.weapons]",
"summary:'1d2 + Briga • corpo a corpo',damageFormula:'1d2',carryState:'BUILT_IN',loaded:true,reloadTrait:null,readiness:'READY',canAttack:true,reloadRequired:false,twoHanded:false,skillLevel:Number.isInteger(pc?.skills?.Punch)?pc.skills.Punch:null,sourceRefs:[WWN_EQUIPMENT_SOURCES.weapons]",
'unarmed projection dual facts')
old="reloadProgress:singleShotReloadProgress(item),sourceRefs:clone(m.weapon.sourceRefs??[])"
new="reloadProgress:singleShotReloadProgress(item),twoHanded:m.weapon.traits?.includes?.('2H')===true,skillLevel:Number.isInteger(pc?.skills?.[combatSkill])?pc.skills[combatSkill]:null,sourceRefs:clone(m.weapon.sourceRefs??[])"
s=replace_once(s,old,new,'weapon projection dual facts')
p.write_text(s)

# combat HTML button
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="attackButton" class="primary player-action">ATACAR</button><button id="reloadButton" class="player-action">RECARREGAR</button>'
new='<button id="attackButton" class="primary player-action">ATACAR</button><button id="dualButton" class="player-action">DUAS ARMAS</button><button id="reloadButton" class="player-action">RECARREGAR</button>'
s=replace_once(s,old,new,'dual wield html button')
p.write_text(s)

# combat UI
p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
anchor="function readinessText(choice){if(!choice)return'INDISPONÍVEL';if(choice.weaponId==='UNARMED')return'PRONTO';if(choice.readiness==='READY')return'PRONTO';if(choice.readiness==='RELOAD_REQUIRED')return choice.actionCost==='CONTINUOUS_ROUNDS'?'RECARGA: 10 RODADAS':'RECARREGAR';if(choice.readiness==='MISSING_READIED_AMMO')return'MUNIÇÃO NÃO PREPARADA';if(choice.readiness==='WEAPON_NOT_READIED')return'GUARDADA';return choice.readiness}"
helpers=anchor+"\nfunction dualSupportChoice(pc,primary){if(!pc||!primary||primary.weaponId==='UNARMED'||primary.twoHanded===true||!Number.isInteger(primary.skillLevel)||primary.skillLevel<1)return null;const eligible=weaponChoices(pc).filter(c=>c.weaponId!=='UNARMED'&&c.weaponId!==primary.weaponId&&c.canAttack===true&&c.twoHanded!==true&&Number.isInteger(c.skillLevel)&&c.skillLevel>=1);const byItem=new Map();for(const c of eligible)if(!byItem.has(c.weaponId))byItem.set(c.weaponId,c);return byItem.size===1?[...byItem.values()][0]:null;}"
s=replace_once(s,anchor,helpers,'dual support helper')
old="$('attackButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||choice?.canAttack!==true;$('attackButton').textContent=choice?`ATACAR • ${choice.name}`:'ATACAR';$('reloadButton').disabled="
new="$('attackButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||choice?.canAttack!==true;$('attackButton').textContent=choice?`ATACAR • ${choice.name}`:'ATACAR';const dualSupport=dualSupportChoice(pc,choice);$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';$('reloadButton').disabled="
s=replace_once(s,old,new,'dual render button')
attack_handler="$('attackButton').onclick=async()=>{const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!choice)return;let physical=null;if(physicalDice.available()){const formula=`1d20+${choice.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula} com física 3D…`;physical=await physicalDice.roll({formula});if(physical.status==='CANCELLED'){$('log').textContent='Rolagem cancelada. Nenhuma regra ou estado foi alterado.';return;}if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent=physical.forcedSettle===true?'O dado não assentou naturalmente. A rolagem foi descartada sem alterar o jogo.':`Falha na física 3D: ${physical.reason??physical.status}. Nenhuma rolagem falsa foi usada.`;return;}}const r=await session.attackNpc({campaignId,pcId:selectedPcId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=`Ataque resolvido com ${choice.name}: ${r.resolution.damage} de dano.`;else if(r.status==='TWO_HANDED_RANGED_BLOCKED_IN_MELEE_NO_MUTATION')fallback='Essa arma de duas mãos não pode ser disparada enquanto um inimigo está em alcance corpo a corpo.';else if(r.status==='TARGET_OUT_OF_RANGE_NO_MUTATION')fallback='Alvo fora do alcance da arma selecionada.';logResult(r,fallback);render()};"
dual_handler=attack_handler+"\n$('dualButton').onclick=async()=>{const p=projection(),pc=currentPc(p),primary=currentWeapon(pc),secondary=dualSupportChoice(pc,primary);if(!primary||!secondary)return;let physical=null;if(physicalDice.available()){const formula=`1d20+${primary.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula} para ataque com duas armas…`;physical=await physicalDice.roll({formula});if(physical.status==='CANCELLED'){$('log').textContent='Rolagem cancelada. Nenhuma regra ou estado foi alterado.';return;}if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent='A rolagem física não foi aceita. Nenhuma regra ou estado foi alterado.';return;}}const r=await session.dualWieldAttackNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,primaryWeaponId:primary.weaponId,primaryModeId:primary.modeId,primaryCombatSkill:primary.combatSkill,secondaryWeaponId:secondary.weaponId,secondaryModeId:secondary.modeId,secondaryCombatSkill:secondary.combatSkill,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=`Duas armas: ${primary.name} com ${secondary.name}. ${r.resolution.damage} de dano; −1 para acertar e +2 de dano apenas quando ambas alcançam o alvo.`;logResult(r,fallback);render()};"
s=replace_once(s,attack_handler,dual_handler,'dual UI handler')
p.write_text(s)

print('WWN dual wield integration patch applied')
