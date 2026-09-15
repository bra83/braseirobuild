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
s=ensure_named_import(s,'./wwn-tactical-combat.js',['inspectCharge'])
helper="""
function chargeArmorClassModifier(combat,actorId){const e=combat?.chargeArmorPenalties?.[actorId];return e&&Number(e.round)===Number(combat?.round)?Number(e.modifier??0):0;}
"""
marker="function mainAvailableWithEcho(state,combat,targetId){return actionEconomyWithEcho071({state,combat,targetId,economy:combat?.economy?.[targetId]??freshActionEconomy()});}\n"
if 'function chargeArmorClassModifier(' not in s:
    if marker not in s:raise SystemExit('charge helper marker missing')
    s=s.replace(marker,marker+helper,1)
old="function targetArmorClass(pc,combat=null,pcId=null,{state=null,rangedAttack=false}={}){const derived=deriveArmorClass(pc);if(derived.status!=='OK'||!Number.isInteger(derived.armorClass))return null;const effective=effectiveArmorClass071({character:pc,armorProjection:derived,totalDefense:Boolean(pcId&&totalDefenseActive(combat,pcId)),state,targetId:pcId,round:combat?.round,rangedAttack});return effective.status==='OK'?effective.armorClass:null;}"
new="function targetArmorClass(pc,combat=null,pcId=null,{state=null,rangedAttack=false}={}){const derived=deriveArmorClass(pc);if(derived.status!=='OK'||!Number.isInteger(derived.armorClass))return null;const effective=effectiveArmorClass071({character:pc,armorProjection:derived,totalDefense:Boolean(pcId&&totalDefenseActive(combat,pcId)),state,targetId:pcId,round:combat?.round,rangedAttack});return effective.status==='OK'?effective.armorClass+chargeArmorClassModifier(combat,pcId):null;}"
s=replace_once(s,old,new,'charge AC penalty')
old="    let range=null;let rangedMelee=null;let proneModifiers=null;let tacticalHitModifier=0;\n    if(combat){\n      if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});\n      const economy=mainAvailableWithEcho(state,combat,actorId);if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});\n      const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});\n      range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon});if(range.status!=='OK')return Object.freeze({status:'TARGET_OUT_OF_RANGE_NO_MUTATION',revision:loaded.revision,range});tacticalHitModifier+=range.hitModifier;\n      const enemyPositions=(combat.enemyIds??[]).filter(id=>(state.npcState?.[id]?.hp??0)>0).map(id=>({actorId:id,position:combat.tactical.actors?.[id]})).filter(x=>x.position);\n      rangedMelee=inspectRangedWeaponInMelee({weapon,attackerPosition:from,enemyPositions});if(rangedMelee.status!=='OK')return Object.freeze({status:`${rangedMelee.status}_NO_MUTATION`,revision:loaded.revision,rangedMelee});tacticalHitModifier+=rangedMelee.hitModifier;\n      proneModifiers=proneAttackAdjustment({combat,attackerId:actorId,targetId:npcId,weapon,attackerPosition:from,targetPosition:to});tacticalHitModifier+=proneModifiers.hitModifier;\n    }"
new="    let range=null;let rangedMelee=null;let proneModifiers=null;let charge=null;let attackFrom=null;let actionCostKind='MAIN';let tacticalHitModifier=0;\n    if(combat){\n      if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});\n      const economy=mainAvailableWithEcho(state,combat,actorId);const originalFrom=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!originalFrom||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});attackFrom=originalFrom;\n      if(attackContext?.kind==='CHARGE'){\n        if(isActorGrappleLocked(combat,actorId))return Object.freeze({status:'GRAPPLED_CANNOT_CHARGE_NO_MUTATION',revision:loaded.revision});if(isActorProne(combat,actorId))return Object.freeze({status:'CHARGE_WHILE_PRONE_REQUIRES_GM_NO_MUTATION',revision:loaded.revision});if(combat.tactical?.pathModel!=='CLEAR_STARTER_COURTYARD')return Object.freeze({status:'CHARGE_CLEAR_PATH_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision,pathModel:combat.tactical?.pathModel??null});\n        if(economy.mainAvailable!==true||economy.moveAvailable!==true)return Object.freeze({status:'CHARGE_MAIN_AND_MOVE_REQUIRED_NO_MUTATION',revision:loaded.revision});const thrown=weapon.melee===false&&weapon.traits?.includes?.('T')===true;if(weapon.melee===false&&!thrown)return Object.freeze({status:'CHARGE_MELEE_OR_THROWN_WEAPON_REQUIRED_NO_MUTATION',revision:loaded.revision});const destination={xFeet:Number(attackContext?.destination?.xFeet),yFeet:Number(attackContext?.destination?.yFeet)};if(!Number.isFinite(destination.xFeet)||!Number.isFinite(destination.yFeet))return Object.freeze({status:'CHARGE_DESTINATION_REQUIRED_NO_MUTATION',revision:loaded.revision});const moveProfile=deriveCombatMoveFeet(attacker);if(moveProfile.status!=='OK')return Object.freeze({status:'MISSING_EVIDENCE_MOVE_RATE_NO_MUTATION',revision:loaded.revision});const magicMove=effectiveMoveFeet071({state,targetId:actorId,round:combat.round,baseMoveFeet:moveProfile.moveFeet});charge=inspectCharge({from:originalFrom,to:destination,economy,normalMoveFeet:magicMove.moveFeet});if(charge.status!=='OK')return Object.freeze({status:`${charge.status}_NO_MUTATION`,revision:loaded.revision,charge});attackFrom=destination;actionCostKind='MOVE_AND_MAIN';tacticalHitModifier+=charge.hitModifier;\n      }else if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});\n      range=inspectAttackRange({attackerPosition:attackFrom,targetPosition:to,weapon});if(range.status!=='OK')return Object.freeze({status:charge?'CHARGE_END_OUT_OF_ATTACK_RANGE_NO_MUTATION':'TARGET_OUT_OF_RANGE_NO_MUTATION',revision:loaded.revision,range,charge});tacticalHitModifier+=range.hitModifier;\n      const enemyPositions=(combat.enemyIds??[]).filter(id=>(state.npcState?.[id]?.hp??0)>0).map(id=>({actorId:id,position:combat.tactical.actors?.[id]})).filter(x=>x.position);\n      rangedMelee=inspectRangedWeaponInMelee({weapon,attackerPosition:attackFrom,enemyPositions});if(rangedMelee.status!=='OK')return Object.freeze({status:`${rangedMelee.status}_NO_MUTATION`,revision:loaded.revision,rangedMelee});tacticalHitModifier+=rangedMelee.hitModifier;\n      proneModifiers=proneAttackAdjustment({combat,attackerId:actorId,targetId:npcId,weapon,attackerPosition:attackFrom,targetPosition:to});tacticalHitModifier+=proneModifiers.hitModifier;\n    }"
s=replace_once(s,old,new,'charge attack validation')
old="      if(npcDefense.consumeShieldShock)recordShieldShockUse(nextCombat,npcId);if(weaponResult.reloadTrait)setWeaponLoaded(next,actorId,weaponId,false);\n      actionCost=applyActionCostWithEchoInPlace071({state:next,combat:nextCombat,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});"
new="      if(npcDefense.consumeShieldShock)recordShieldShockUse(nextCombat,npcId);if(weaponResult.reloadTrait)setWeaponLoaded(next,actorId,weaponId,false);if(charge){nextCombat.tactical.actors[actorId]={...nextCombat.tactical.actors[actorId],xFeet:attackFrom.xFeet,yFeet:attackFrom.yFeet};nextCombat.chargeArmorPenalties={...(nextCombat.chargeArmorPenalties??{}),[actorId]:{round:nextCombat.round,modifier:charge.armorClassModifierUntilRoundEnd,source:'CHARGE'}};}\n      actionCost=applyActionCostWithEchoInPlace071({state:next,combat:nextCombat,targetId:actorId,cost:actionCostKind});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});"
s=replace_once(s,old,new,'charge state/action cost')
s=replace_once(s,
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, proneModifiers, attackContext, resolution,",
"lastCombatResolution: { pcId:actorId,npcId, weaponId, modeId, range, rangedMelee, proneModifiers, charge, attackContext, resolution,",
'charge last resolution')
s=replace_once(s,
"payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,attackContext,resolution,",
"payload:{pcId:actorId,npcId,weaponId,modeId,range,rangedMelee,proneModifiers,charge,attackContext,resolution,",
'charge event payload')
s=replace_once(s,
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,proneModifiers,attackContext,enfeeblingPair,",
"return Object.freeze({status:'OK',receipt,pcId:actorId,resolution,range,rangedMelee,proneModifiers,charge,attackContext,enfeeblingPair,",
'charge attack return')
method="""
  chargeNpc({campaignId,pcId=null,npcId,weaponId,modeId=null,combatSkill=null,xFeet,yFeet,physicalDice=null,enfeeblingPhysicalDiceSecond=null,wardpactPhysicalSaveDice=null}){
    return this.attackNpc({campaignId,pcId,npcId,weaponId,modeId,combatSkill,physicalDice,enfeeblingPhysicalDiceSecond,wardpactPhysicalSaveDice,attackContext:Object.freeze({kind:'CHARGE',destination:Object.freeze({xFeet:Number(xFeet),yFeet:Number(yFeet)})})});
  }

"""
marker="  grappleNpc({campaignId,pcId=null,npcId}){"
if '  chargeNpc({' not in s:
    if marker not in s:raise SystemExit('charge method marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

# --- session wrapper ---
p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text()
old="  async attackNpc(args){return this._narrate(args.campaignId,this.actions.attackNpc(args),'PLAYER_ATTACK');}"
new=old+"\n  async chargeNpc(args){return this._narrate(args.campaignId,this.actions.chargeNpc(args),'PLAYER_CHARGE');}"
s=replace_once(s,old,new,'session charge wrapper');p.write_text(s)

# --- projection AC penalty ---
p=Path('app/src/main/assets/www/core/combat-projection.js');s=p.read_text()
helper="""
function chargeArmorClassModifier(combat,actorId){const e=combat?.chargeArmorPenalties?.[actorId];return e&&Number(e.round)===Number(combat?.round)?Number(e.modifier??0):0;}
"""
marker="function activeCharacterId(state){return String(state?.characterState?.characterId??state?.characterState?.id??'')}\n"
if 'function chargeArmorClassModifier(' not in s:
    if marker not in s:raise SystemExit('projection helper marker missing')
    s=s.replace(marker,marker+helper,1)
old="armorClass:armor.status==='OK'?armor.armorClass+(totalDefenseActive(combat,id)?2:0):null,baseArmorClass:armor.status==='OK'?armor.armorClass:null,"
new="armorClass:armor.status==='OK'?armor.armorClass+(totalDefenseActive(combat,id)?2:0)+chargeArmorClassModifier(combat,id):null,baseArmorClass:armor.status==='OK'?armor.armorClass:null,chargeArmorClassModifier:chargeArmorClassModifier(combat,id),"
s=replace_once(s,old,new,'projection charge AC')
p.write_text(s)

# --- combat UI ---
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="dualButton" class="weapon-choice" type="button"><b>DUAS ARMAS</b><small>Exige duas armas válidas e perícia 1 nas duas.</small></button>'
new=old+'<button id="chargeButton" class="weapon-choice" type="button"><b>CARGA</b><small>Gasta Ação Principal + Movimento; avance em linha reta e ataque com +2, sofrendo −2 CA até o fim da rodada.</small></button>'
s=replace_once(s,old,new,'charge menu button');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
s=replace_once(s,
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let selectedEnemyId=null;",
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let chargeMode=false;let selectedEnemyId=null;",
'charge UI state')
s=replace_once(s,
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;$('moveHint').classList.add('hidden');render()};",
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;chargeMode=false;$('moveHint').classList.add('hidden');render()};",
'clear charge on hero select')
old="$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').querySelector('b').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';const shoveReady="
new="$('dualButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||!dualSupport;$('dualButton').querySelector('b').textContent=dualSupport?`DUAS ARMAS • ${dualSupport.name}`:'DUAS ARMAS';const chargeWeapon=choice&&(choice.weaponId==='UNARMED'||choice.modeId==='MELEE'||choice.modeId==='THROWN');$('chargeButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||pc.economy?.moveAvailable===false||!enemy||enemy.defeated||!chargeWeapon||pc.prone===true||pc.grapple?.locked===true;const shoveReady="
s=replace_once(s,old,new,'render charge button')
s=replace_once(s,
"$('moveButton').onclick=()=>{grappleManipulationMode=null;moveMode=!moveMode;",
"$('moveButton').onclick=()=>{chargeMode=false;grappleManipulationMode=null;moveMode=!moveMode;",
'clear charge on ordinary move')
old="$('battlefield').addEventListener('click',async e=>{if(e.target.closest('.token'))return;const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());if(grappleManipulationMode){"
new="$('battlefield').addEventListener('click',async e=>{if(e.target.closest('.token'))return;const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());if(chargeMode){chargeMode=false;$('moveHint').classList.add('hidden');const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice){render();return;}let physical=null;if(physicalDice.available()){const formula=`1d20+${choice.damageFormula??'1d2'}`;$('log').textContent=`Rolando ${formula} para a Carga…`;physical=await physicalDice.roll({formula});if(physical.status!=='OK'||physical.forcedSettle===true){$('log').textContent='A rolagem física da Carga não foi aceita. Nenhuma regra ou estado foi alterado.';render();return;}}const r=await session.chargeNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,...dest,physicalDice:physical});let fallback=r.status;if(r.status==='OK')fallback=`Carga resolvida: ${Math.round(r.charge?.distanceFeet??0)} pés, +2 para acertar e −2 CA até o fim da rodada.`;logResult(r,fallback);render();return;}if(grappleManipulationMode){"
s=replace_once(s,old,new,'route charge battlefield destination')
handler="""
$('chargeButton').onclick=()=>{moveMode=false;grappleManipulationMode=null;chargeMode=true;$('moveHint').textContent='TOQUE NO PONTO FINAL DA CARGA • LINHA RETA';$('moveHint').classList.remove('hidden');closePopups()};
"""
marker="$('dualButton').onclick=async()=>"
if "$('chargeButton').onclick" not in s:
    if marker not in s:raise SystemExit('dual handler marker missing')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)
print('WWN canonical charge combat patch applied')
