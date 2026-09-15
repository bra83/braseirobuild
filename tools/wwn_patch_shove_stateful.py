from pathlib import Path

def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# --- combat-service.js ---
p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
old="import { computeSaveTargets, orderInitiativeSides, resolveAttack, resolveAttackFromPhysicalDice, resolveNpcAttack, resolveSavingThrow, resolveSideInitiative, unarmedWeaponContract } from './wwn-combat-core.js';"
new="import { computeSaveTargets, orderInitiativeSides, resolveAttack, resolveAttackHitOnly, resolveAttackFromPhysicalDice, resolveNpcAttack, resolveSavingThrow, resolveSideInitiative, unarmedWeaponContract } from './wwn-combat-core.js';"
s=replace_once(s,old,new,'hit-only combat import')
old="import { dualWieldPlan } from './wwn-special-combat-core.js';"
new="import { dualWieldPlan, shoveOutcome, sizePenaltyForShoveOrGrapple, resolvePcManeuverSkillRoll, resolveNpcManeuverSkillRoll } from './wwn-special-combat-core.js';"
s=replace_once(s,old,new,'shove special import')
anchor="function combatWeaponId(enemy){return enemy?.weaponContract?.weaponId??enemy?.weaponContract?.itemId??enemy?.weaponContract?.id??null;}"
helper=anchor+"\nfunction shoveSizePlan(npc){const cls=String(npc?.grappleSizeClass??'').toUpperCase();if(cls==='HUMAN_SIZED')return Object.freeze({status:'OK',sizeClass:cls,plan:sizePenaltyForShoveOrGrapple({})});if(cls==='LARGER_HUMANOID')return Object.freeze({status:'OK',sizeClass:cls,plan:sizePenaltyForShoveOrGrapple({substantiallyLargerHumanoid:true})});if(cls==='BARELY_PLAUSIBLE_OR_QUADRUPED')return Object.freeze({status:'OK',sizeClass:cls,plan:sizePenaltyForShoveOrGrapple({barelyPlausibleOrQuadruped:true})});return Object.freeze({status:'MISSING_SHOVE_SIZE_EVIDENCE'});}"
s=replace_once(s,anchor,helper,'shove size helper')
method="""
  shoveNpc({campaignId,pcId=null,npcId,weaponId='UNARMED',modeId=null,combatSkill=null,opposedSkill='Punch'}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const attacker=characterById(state,actorId);if(!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,actorId);if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});
    const skill=String(opposedSkill??'').trim();if(!['Exert','Punch'].includes(skill))return Object.freeze({status:'SHOVE_REQUIRES_EXERT_OR_PUNCH_NO_MUTATION',revision:loaded.revision});if(!Number.isInteger(attacker.attributes?.Strength))return Object.freeze({status:'MISSING_STRENGTH_EVIDENCE_NO_MUTATION',revision:loaded.revision});
    const weaponResult=playerWeapon(state,actorId,weaponId,{tactical:true,modeId,combatSkill});if(weaponResult.status!=='OK')return Object.freeze({status:weaponResult.status,revision:loaded.revision});const weapon=weaponResult.weapon;if(weapon.melee===false)return Object.freeze({status:'SHOVE_REQUIRES_MELEE_ATTACK_NO_MUTATION',revision:loaded.revision});const npc=state.npcState?.[npcId];if(!npc||!Number.isInteger(npc.hp)||npc.hp<1||!Number.isInteger(npc.meleeAC)||!Number.isInteger(npc.skillBonus))return Object.freeze({status:'MISSING_EVIDENCE_TARGET_COMBAT_STATE',revision:loaded.revision});const size=shoveSizePlan(npc);if(size.status!=='OK')return Object.freeze({status:`${size.status}_NO_MUTATION`,revision:loaded.revision,npcId});
    const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon});if(range.status!=='OK')return Object.freeze({status:'TARGET_OUT_OF_RANGE_NO_MUTATION',revision:loaded.revision,range});const proneModifiers=proneAttackAdjustment({combat,attackerId:actorId,targetId:npcId,weapon,attackerPosition:from,targetPosition:to});const weaponBonuses=weaponResult.mechanicalBonuses??{hitBonus:0};
    const rng=DeterministicRng.fromState(state.rngState);const attack=resolveAttackHitOnly({rng,attacker,weapon,target:npc,hitModifier:range.hitModifier+proneModifiers.hitModifier+(weaponBonuses.hitBonus??0),streamName:`combat/shove/${actorId}/${npcId}/attack`});let attackerRoll=null,defenderRoll=null,outcome=shoveOutcome({attackHit:false,attackerIsPc:true,defenderIsPc:false});if(attack.hit){attackerRoll=resolvePcManeuverSkillRoll({rng,attributeScore:attacker.attributes.Strength,skillLevel:Number.isInteger(attacker.skills?.[skill])?attacker.skills[skill]:null,modifier:size.plan.skillCheckModifier,streamName:`combat/shove/${actorId}/${npcId}/pc`});defenderRoll=resolveNpcManeuverSkillRoll({rng,skillBonus:npc.skillBonus,modifier:0,streamName:`combat/shove/${actorId}/${npcId}/npc`});outcome=shoveOutcome({attackHit:true,attackerOpposedTotal:attackerRoll.total,defenderOpposedTotal:defenderRoll.total,attackerIsPc:true,defenderIsPc:false});}
    const next=clone(state);next.rngState=rng.exportState();const c=next.encounterState.combat;const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});let targetProne=false;if(outcome.success){setActorProneInPlace(c,npcId,true);targetProne=true;}const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});if(characterById(next,actorId)?.hp?.current===0)markPcMortallyInjured(next,c,actorId);
    const hpBefore=npc.hp,hpAfter=next.npcState[npcId].hp;next.encounterState.lastCombatResolution={kind:'SHOVE',pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,targetHpBefore:hpBefore,targetHpAfter:hpAfter};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_SHOVE',suffix:`shove:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,actionCost,tumefaction:tumefaction?.status??null,targetHpBefore:hpBefore,targetHpAfter:hpAfter,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.7.1 Shoving and Grappling'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,attack,attackerRoll,defenderRoll,outcome,targetProne,actionCost,targetHpBefore:hpBefore,targetHpAfter:hpAfter,combat:clone(c)});
  }

"""
marker="  dualWieldAttackNpc({"
if '  shoveNpc({' not in s:
    if marker not in s: raise SystemExit('dual wield insertion marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

# --- combat-session-service.js ---
p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text()
old="  async dualWieldAttackNpc(args){return this._narrate(args.campaignId,this.actions.dualWieldAttackNpc(args),'PLAYER_DUAL_WIELD_ATTACK');}"
new=old+"\n  async shoveNpc(args){return this._narrate(args.campaignId,this.actions.shoveNpc(args),'PLAYER_SHOVE');}"
s=replace_once(s,old,new,'session shove wrapper')
p.write_text(s)

# --- scr022.html: consolidate special actions into a maneuver popup ---
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="defenseButton" class="player-action">DEFESA TOTAL</button><button id="proneButton" class="player-action">IR AO CHÃO</button><button id="attackButton" class="primary player-action">ATACAR</button><button id="dualButton" class="player-action">DUAS ARMAS</button><button id="reloadButton" class="player-action">RECARREGAR</button>'
new='<button id="defenseButton" class="player-action">DEFESA TOTAL</button><button id="maneuverButton" class="player-action">MANOBRAS</button><button id="attackButton" class="primary player-action">ATACAR</button><button id="reloadButton" class="player-action">RECARREGAR</button>'
s=replace_once(s,old,new,'maneuver grid consolidation')
anchor='<aside id="weaponPopup" class="weapon-popup hidden"><button id="closeWeapon">×</button><h3>ARMA DO ATOR</h3><div id="weaponList" class="weapon-list"></div><small>Somente equipamento real da ficha aparece aqui. Armas guardadas ou descarregadas continuam bloqueadas pelo Rules Engine.</small></aside>'
popup=anchor+'<aside id="maneuverPopup" class="weapon-popup hidden"><button id="closeManeuver">×</button><h3>MANOBRAS</h3><div class="weapon-list"><button id="proneButton" class="weapon-choice" type="button"><b>IR AO CHÃO / LEVANTAR</b><small>Prone usa as regras canônicas de ação e modificadores.</small></button><button id="dualButton" class="weapon-choice" type="button"><b>DUAS ARMAS</b><small>Exige duas armas válidas e perícia 1 nas duas.</small></button><button id="shoveExertButton" class="weapon-choice" type="button"><b>DERRUBAR • FOR/ESFORÇO</b><small>Ataque melee sem dano, seguido de disputa de perícia.</small></button><button id="shovePunchButton" class="weapon-choice" type="button"><b>DERRUBAR • FOR/BRIGA</b><small>Ataque melee sem dano, seguido de disputa de perícia.</small></button></div></aside>'
s=replace_once(s,anchor,popup,'maneuver popup')
p.write_text(s)

# --- scr022.js ---
p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
s=replace_once(s,
"function closePopups(){$('shade').classList.add('hidden');$('statusPopup').classList.add('hidden');$('weaponPopup').classList.add('hidden')}",
"function closePopups(){$('shade').classList.add('hidden');$('statusPopup').classList.add('hidden');$('weaponPopup').classList.add('hidden');$('maneuverPopup').classList.add('hidden')}",
'close maneuver popup')
old="$('proneButton').disabled=!pcTurn||(pc.prone===true&&pc.economy?.moveAvailable===false&&pc.economy?.mainAvailable===false);$('proneButton').textContent=pc.prone===true?'LEVANTAR':'IR AO CHÃO';$('attackButton').disabled="
new="$('maneuverButton').disabled=!pcTurn;$('proneButton').disabled=!pcTurn||(pc.prone===true&&pc.economy?.moveAvailable===false&&pc.economy?.mainAvailable===false);$('proneButton').querySelector('b').textContent=pc.prone===true?'LEVANTAR':'IR AO CHÃO';$('attackButton').disabled="
s=replace_once(s,old,new,'maneuver render')
old="const dualSupport=dualSupportChoice(pc,choice);$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';$('reloadButton').disabled="
new="const dualSupport=dualSupportChoice(pc,choice);$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').querySelector('b').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';const shoveReady=pcTurn&&pc.economy?.mainAvailable!==false&&enemy&&!enemy.defeated&&choice?.canAttack===true&&(choice.weaponId==='UNARMED'||choice.modeId==='MELEE');$('shoveExertButton').disabled=!shoveReady;$('shovePunchButton').disabled=!shoveReady;$('reloadButton').disabled="
s=replace_once(s,old,new,'shove buttons render')
anchor="$('defenseButton').onclick=async()=>{const r=await session.totalDefense({campaignId,pcId:selectedPcId});logResult(r,r.status==='OK'?`Defesa Total ativa: CA ${r.armorClass} e imunidade a Choque até o próximo turno.`:r.status);render()};"
menu=anchor+"\n$('maneuverButton').onclick=()=>{$('shade').classList.remove('hidden');$('maneuverPopup').classList.remove('hidden')};$('closeManeuver').onclick=closePopups;"
s=replace_once(s,anchor,menu,'maneuver popup handlers')
old="$('proneButton').onclick=async()=>{const p=projection(),pc=currentPc(p);if(!pc)return;const r=pc.prone===true?await session.standUp({campaignId,pcId:pc.actorId}):await session.goProne({campaignId,pcId:pc.actorId});logResult(r,r.status==='OK'?(r.prone?'Você foi ao chão. Movimento reduzido pela metade; ataques seguem os modificadores de Prone.':'Você se levantou, consumindo uma ação de Movimento.') : r.status);render()};"
new=old.replace("render()};","closePopups();render()};")
s=replace_once(s,old,new,'close prone popup')
old="$('dualButton').onclick=async()=>{const p=projection(),pc=currentPc(p),primary=currentWeapon(pc),secondary=dualSupportChoice(pc,primary);if(!primary||!secondary)return;let physical=null;if(physicalDice.available()){const formula=`1d20+${primary.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula} para ataque com duas armas…`;physical=await physicalDice.roll({formula});if(physical.status==='CANCELLED'){$('log').textContent='Rolagem cancelada. Nenhuma regra ou estado foi alterado.';return;}if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent='A rolagem física não foi aceita. Nenhuma regra ou estado foi alterado.';return;}}const r=await session.dualWieldAttackNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,primaryWeaponId:primary.weaponId,primaryModeId:primary.modeId,primaryCombatSkill:primary.combatSkill,secondaryWeaponId:secondary.weaponId,secondaryModeId:secondary.modeId,secondaryCombatSkill:secondary.combatSkill,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=`Duas armas: ${primary.name} com ${secondary.name}. ${r.resolution.damage} de dano; −1 para acertar e +2 de dano apenas quando ambas alcançam o alvo.`;logResult(r,fallback);render()};"
new=old.replace("logResult(r,fallback);render()};","logResult(r,fallback);closePopups();render()};")
s=replace_once(s,old,new,'close dual popup')
handler="""
async function shoveWithSkill(opposedSkill){const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice)return;const r=await session.shoveNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,opposedSkill});let fallback=r.status;if(r.status==='OK')fallback=!r.attack.hit?'A tentativa de derrubar não acertou o ataque corpo a corpo.':r.outcome.success?'O golpe não causou dano e o alvo foi derrubado.':'O golpe acertou, mas o alvo venceu a disputa e permaneceu de pé.';logResult(r,fallback);closePopups();render()}
$('shoveExertButton').onclick=()=>shoveWithSkill('Exert');$('shovePunchButton').onclick=()=>shoveWithSkill('Punch');
"""
marker="$('reloadButton').onclick=async()=>{"
if "async function shoveWithSkill(" not in s:
    if marker not in s: raise SystemExit('reload marker missing for shove handlers')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)

print('WWN shove stateful/player patch applied')
