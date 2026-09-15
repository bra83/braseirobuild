from pathlib import Path
import re


def replace_once(text, old, new, label):
    if new in text: return text
    if old not in text: raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)


def ensure_named_import(text,module,names):
    pat=re.compile(r"import \{ ([^}]*) \} from '"+re.escape(module)+r"';")
    m=pat.search(text)
    if not m: raise SystemExit(f'import marker missing: {module}')
    current=[x.strip() for x in m.group(1).split(',') if x.strip()]
    for name in names:
        if name not in current: current.append(name)
    return text[:m.start()]+"import { "+', '.join(current)+" } from '"+module+"';"+text[m.end():]

p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=ensure_named_import(s,'./wwn-special-combat-core.js',['snapAttackPlan','holdActionPlan'])
s=replace_once(s,
"function resetAllEconomies(combat){for(const id of Object.keys(combat.economy??{}))combat.economy[id]=freshActionEconomy();combat.swarmAttacks={};}",
"function resetAllEconomies(combat){for(const id of Object.keys(combat.economy??{}))combat.economy[id]=freshActionEconomy();combat.swarmAttacks={};combat.heldActions={};}",
'held action round expiry')

start=s.index('  attackNpc({');end=s.index('\n\n  swarmAssistNpc({',start);section=s[start:end]
section=replace_once(section,
"let charge=null;let swarmPlan=null;let swarmAssailantIds=[];let swarmMaximumDamage=null;let swarmShockFloor=null;let attackFrom=null;let actionCostKind='MAIN';let tacticalHitModifier=0;",
"let charge=null;let swarmPlan=null;let swarmAssailantIds=[];let swarmMaximumDamage=null;let swarmShockFloor=null;let snapPlan=null;let heldTrigger=false;let attackFrom=null;let actionCostKind='MAIN';let tacticalHitModifier=0;",
'snap hold attack context vars')
old="    if(combat){\n      if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});\n      const economy=mainAvailableWithEcho(state,combat,actorId);const originalFrom=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!originalFrom||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});attackFrom=originalFrom;"
new="    if(combat){\n      const actionKind=String(attackContext?.kind??'').toUpperCase(),isSnap=actionKind==='SNAP',heldEntry=combat.heldActions?.[actorId];heldTrigger=actionKind==='HELD'&&heldEntry?.round===combat.round;if(actionKind==='HELD'&&!heldTrigger)return Object.freeze({status:'HELD_ACTION_NOT_AVAILABLE_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC'&&!isSnap&&!heldTrigger)return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});\n      const economy=mainAvailableWithEcho(state,combat,actorId);if(isSnap){snapPlan=snapAttackPlan({mainActionAvailable:economy.mainAvailable});if(!snapPlan.legal)return Object.freeze({status:'SNAP_ATTACK_MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision,snapPlan});tacticalHitModifier+=snapPlan.hitModifier;}const originalFrom=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!originalFrom||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});attackFrom=originalFrom;"
section=replace_once(section,old,new,'snap hold side authorization')
section=replace_once(section,
"if(swarmPlan&&nextCombat.swarmAttacks)delete nextCombat.swarmAttacks[npcId];",
"if(swarmPlan&&nextCombat.swarmAttacks)delete nextCombat.swarmAttacks[npcId];if((snapPlan||heldTrigger)&&nextCombat.heldActions)delete nextCombat.heldActions[actorId];",
'snap hold consume held state')
section=replace_once(section,
"swarmPlan, swarmAssailantIds, swarmMaximumDamage, swarmShockFloor, attackContext, resolution,",
"swarmPlan, swarmAssailantIds, swarmMaximumDamage, swarmShockFloor, snapPlan, heldTrigger, attackContext, resolution,",
'snap hold last resolution')
section=replace_once(section,
"eventKind:charge?'PLAYER_CHARGE':swarmPlan?'PLAYER_SWARM_ATTACK':'PLAYER_ATTACK',suffix:charge?`charge:${actorId}:${npcId}`:swarmPlan?`swarm-attack:${actorId}:${npcId}`:`attack:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,charge,swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,attackContext,resolution,",
"eventKind:charge?'PLAYER_CHARGE':swarmPlan?'PLAYER_SWARM_ATTACK':snapPlan?'PLAYER_SNAP_ATTACK':heldTrigger?'PLAYER_HELD_ATTACK':'PLAYER_ATTACK',suffix:charge?`charge:${actorId}:${npcId}`:swarmPlan?`swarm-attack:${actorId}:${npcId}`:snapPlan?`snap-attack:${actorId}:${npcId}`:heldTrigger?`held-attack:${actorId}:${npcId}`:`attack:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,charge,swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,snapPlan,heldTrigger,attackContext,resolution,",
'snap hold event')
section=replace_once(section,
"swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,attackContext,enfeeblingPair,",
"swarmPlan,swarmAssailantIds,swarmMaximumDamage,swarmShockFloor,snapPlan,heldTrigger,attackContext,enfeeblingPair,",
'snap hold return')
s=s[:start]+section+s[end:]

if '  holdAction({' not in s:
    marker='  swarmAssistNpc({campaignId,pcId=null,npcId,weaponId=\'UNARMED\',modeId=null,combatSkill=null}){'
    methods="""
  holdAction({campaignId,pcId=null}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});if(!isCombatCapablePc(characterById(state,actorId)))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,actorId),plan=holdActionPlan({moveActionAvailable:economy.moveAvailable});if(!plan.legal)return Object.freeze({status:'MOVE_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision,plan});const next=clone(state),c=next.encounterState.combat,actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MOVE'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});const held=Object.freeze({round:c.round,mainAvailableAtHold:economy.mainAvailable===true,triggerRestOfTurnAs:'INSTANT',expires:'END_OF_ROUND'});c.heldActions={...(c.heldActions??{}),[actorId]:held};next.encounterState.lastCombatResolution={kind:'HOLD_ACTION',pcId:actorId,held};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_HELD_ACTION',suffix:`hold:${actorId}`,payload:{pcId:actorId,held,actionCost,plan,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Hold An Action'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,held,plan,actionCost,combat:clone(c)});
  }

  snapAttackNpc(args){return this.attackNpc({...args,attackContext:Object.freeze({kind:'SNAP'})});}
  heldAttackNpc(args){return this.attackNpc({...args,attackContext:Object.freeze({kind:'HELD'})});}

"""
    if marker not in s: raise SystemExit('snap hold methods marker missing')
    s=s.replace(marker,methods+marker,1)
p.write_text(s)

p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text();old="  async swarmAssistNpc(args){return this._narrate(args.campaignId,this.actions.swarmAssistNpc(args),'PLAYER_SWARM_ASSIST');}";new="  async holdAction(args){return this._narrate(args.campaignId,this.actions.holdAction(args),'PLAYER_HELD_ACTION');}\n  async snapAttackNpc(args){return this._narrate(args.campaignId,this.actions.snapAttackNpc(args),'PLAYER_SNAP_ATTACK');}\n  async heldAttackNpc(args){return this._narrate(args.campaignId,this.actions.heldAttackNpc(args),'PLAYER_HELD_ATTACK');}\n"+old;s=replace_once(s,old,new,'snap hold session wrappers');p.write_text(s)

p=Path('app/src/main/assets/www/core/combat-projection.js');s=p.read_text();old="economy:clone(combat.economy?.[id]??freshActionEconomy()),totalDefense:totalDefenseActive(combat,id),";new="economy:clone(combat.economy?.[id]??freshActionEconomy()),heldAction:combat.heldActions?.[id]?.round===combat.round?clone(combat.heldActions[id]):null,totalDefense:totalDefenseActive(combat,id),";s=replace_once(s,old,new,'held action projection');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.html');s=p.read_text();old='<button id="screenButton" class="weapon-choice" type="button"><b>PROTEGER ALIADO</b><small>Gasta Movimento; toque no aliado que você quer proteger até seu próximo turno.</small></button>';new=old+'<button id="holdButton" class="weapon-choice" type="button"><b>SEGURAR AÇÃO</b><small>Gasta Movimento; o restante do turno pode ser disparado como ação Instantânea até o fim da rodada.</small></button>';s=replace_once(s,old,new,'hold UI button');old='<button id="maneuverButton" class="player-action">MANOBRAS</button>';new=old+'<button id="snapAttackButton" class="reaction-action hidden">SNAP ATTACK</button><button id="heldAttackButton" class="reaction-action hidden">USAR AÇÃO SEGURADA</button>';s=replace_once(s,old,new,'reaction attack buttons');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
old="$('screenButton').disabled=!pcTurn||pc.economy?.moveAvailable===false||pc.grapple?.locked===true||p.pcs.filter(x=>!x.dead&&!x.mortallyInjured).length<2;const shoveReady="
new="$('screenButton').disabled=!pcTurn||pc.economy?.moveAvailable===false||pc.grapple?.locked===true||p.pcs.filter(x=>!x.dead&&!x.mortallyInjured).length<2;$('holdButton').disabled=!pcTurn||pc.economy?.moveAvailable===false;const reactionReady=p.active&&enemy&&!enemy.defeated&&choice?.canAttack===true&&pc.economy?.mainAvailable!==false&&!pc.dead&&!pc.mortallyInjured;$('snapAttackButton').classList.toggle('hidden',!enemySide);$('snapAttackButton').disabled=!enemySide||!reactionReady;$('heldAttackButton').classList.toggle('hidden',!(enemySide&&pc.heldAction));$('heldAttackButton').disabled=!enemySide||!reactionReady||!pc.heldAction;const shoveReady="
s=replace_once(s,old,new,'render snap hold buttons')
handler="""
$('holdButton').onclick=async()=>{const p=projection(),pc=currentPc(p);if(!pc)return;const r=await session.holdAction({campaignId,pcId:pc.actorId});logResult(r,r.status==='OK'?'Ação segurada. O restante do turno pode ser usado como Instantânea até o fim da rodada.':r.status);closePopups();render()};
async function reactionAttack(kind){const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice)return;let physical=null;if(physicalDice.available()){const formula=`1d20+${choice.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula}…`;physical=await physicalDice.roll({formula});if(physical.status==='CANCELLED'){$('log').textContent='Rolagem cancelada. Nenhuma regra ou estado foi alterado.';return;}if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent='A rolagem física não foi aceita. Nenhuma regra ou estado foi alterado.';return;}}const fn=kind==='SNAP'?session.snapAttackNpc.bind(session):session.heldAttackNpc.bind(session);const r=await fn({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=kind==='SNAP'?`Snap Attack resolvido com −4 para acertar: ${r.resolution?.damage??0} dano.`:`Ação segurada disparada: ${r.resolution?.damage??0} dano.`;logResult(r,fallback);render()}
$('snapAttackButton').onclick=()=>reactionAttack('SNAP');$('heldAttackButton').onclick=()=>reactionAttack('HELD');
"""
marker="$('screenButton').onclick="
if "$('holdButton').onclick" not in s:
    idx=s.find(marker)
    if idx<0: raise SystemExit('snap hold UI handler marker missing')
    s=s[:idx]+handler+s[idx:]
p.write_text(s)
print('WWN canonical Snap Attack and Hold Action patch applied')
