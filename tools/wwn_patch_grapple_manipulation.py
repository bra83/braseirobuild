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
s=ensure_named_import(s,'./wwn-special-combat-core.js',['grappleMoveOutcome'])
method="""
  manipulateGrappledNpc({campaignId,pcId=null,npcId,mode='MOVE',xFeet,yFeet}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const actorId=resolvePcId(state,combat,pcId);if(!actorId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const attacker=characterById(state,actorId);if(!isCombatCapablePc(attacker))return Object.freeze({status:'PC_INCAPACITATED_NO_MUTATION',revision:loaded.revision});const held=grappleTargetOf(combat,actorId);if(!held)return Object.freeze({status:'NO_GRAPPLED_TARGET_NO_MUTATION',revision:loaded.revision});if(String(held)!==String(npcId))return Object.freeze({status:'GRAPPLED_TARGET_MISMATCH_NO_MUTATION',revision:loaded.revision,targetId:held});const assailants=[...grappleAssailantsOf(combat,npcId)];if(assailants.length!==1)return Object.freeze({status:'MULTIPLE_GRAPPLE_MANIPULATION_REQUIRES_GM_NO_MUTATION',revision:loaded.revision,assailants});const economy=mainAvailableWithEcho(state,combat,actorId);if(economy.mainAvailable!==true)return Object.freeze({status:'MAIN_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});
    const npc=state.npcState?.[npcId];if(!npc||!Number.isInteger(npc.hp)||npc.hp<1||!Number.isInteger(npc.skillBonus))return Object.freeze({status:'MISSING_EVIDENCE_TARGET_COMBAT_STATE',revision:loaded.revision});const size=shoveSizePlan(npc);if(size.status!=='OK')return Object.freeze({status:`${size.status}_NO_MUTATION`,revision:loaded.revision});const normalized=String(mode).toUpperCase();if(!['MOVE','THROW'].includes(normalized))return Object.freeze({status:'INVALID_GRAPPLE_MANIPULATION_MODE_NO_MUTATION',revision:loaded.revision});const from=combat.tactical.actors?.[actorId],targetFrom=combat.tactical.actors?.[npcId],destination={xFeet:Number(xFeet),yFeet:Number(yFeet)};if(!from||!targetFrom)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});if(!Number.isFinite(destination.xFeet)||!Number.isFinite(destination.yFeet))return Object.freeze({status:'INVALID_DESTINATION_NO_MUTATION',revision:loaded.revision});const origin=normalized==='MOVE'?from:targetFrom,maxFeet=normalized==='MOVE'?10:5,distance=distanceFeet(origin,destination);if(distance>maxFeet+1e-9)return Object.freeze({status:normalized==='MOVE'?'GRAPPLE_MOVE_TOO_FAR_NO_MUTATION':'GRAPPLE_THROW_TOO_FAR_NO_MUTATION',revision:loaded.revision,distanceFeet:distance,maxFeet});
    let targetDestination=destination;if(normalized==='MOVE'){const dx=destination.xFeet-from.xFeet,dy=destination.yFeet-from.yFeet;targetDestination={xFeet:targetFrom.xFeet+dx,yFeet:targetFrom.yFeet+dy};const otherEnemies=(combat.enemyIds??[]).filter(id=>id!==npcId&&(state.npcState?.[id]?.hp??0)>0&&state.npcState?.[id]?.weaponContract?.melee!==false).map(id=>({actorId:id,position:combat.tactical.actors?.[id]})).filter(x=>x.position);const leaving=otherEnemies.filter(e=>distanceFeet(from,e.position)<=5+1e-9&&distanceFeet(destination,e.position)>5+1e-9).map(e=>e.actorId);if(leaving.length)return Object.freeze({status:'GRAPPLE_MOVE_FREE_ATTACK_REQUIRED_NO_MUTATION',revision:loaded.revision,leavingEnemyIds:Object.freeze(leaving)});}
    const rng=DeterministicRng.fromState(state.rngState),attackerRoll=resolvePcManeuverSkillRoll({rng,attributeScore:attacker.attributes.Strength,skillLevel:Number.isInteger(attacker.skills?.Punch)?attacker.skills.Punch:null,modifier:size.plan.skillCheckModifier,streamName:`combat/grapple/manipulate/${normalized}/${actorId}/${npcId}/pc`}),defenderRoll=resolveNpcManeuverSkillRoll({rng,skillBonus:npc.skillBonus,streamName:`combat/grapple/manipulate/${normalized}/${actorId}/${npcId}/npc`}),outcome=grappleMoveOutcome({attackerOpposedTotal:attackerRoll.total,defenderOpposedTotal:defenderRoll.total,attackerIsPc:true,defenderIsPc:false,mode:normalized});
    const next=clone(state);next.rngState=rng.exportState();const c=next.encounterState.combat,actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});let grappleState=null;if(outcome.success&&normalized==='MOVE'){c.tactical.actors[actorId]={...c.tactical.actors[actorId],xFeet:destination.xFeet,yFeet:destination.yFeet};c.tactical.actors[npcId]={...c.tactical.actors[npcId],xFeet:targetDestination.xFeet,yFeet:targetDestination.yFeet};grappleState=actorGrappleState(c,actorId);}else if(outcome.success&&normalized==='THROW'){c.tactical.actors[npcId]={...c.tactical.actors[npcId],xFeet:destination.xFeet,yFeet:destination.yFeet};setActorProneInPlace(c,npcId,true);grappleState=endAllGrapplesForActorInPlace(c,npcId);}else grappleState=endGrappleInPlace(c,{assailantId:actorId,targetId:npcId});
    const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});if(characterById(next,actorId)?.hp?.current===0)markPcMortallyInjured(next,c,actorId);next.encounterState.lastCombatResolution={kind:'GRAPPLE_MANIPULATION',mode:normalized,pcId:actorId,npcId,attackerRoll,defenderRoll,outcome,destination,targetDestination,grappleState};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_GRAPPLE_MANIPULATION',suffix:`grapple-${normalized.toLowerCase()}:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,mode:normalized,attackerRoll,defenderRoll,outcome,destination,targetDestination,distanceFeet:distance,grappleState,actionCost,tumefaction:tumefaction?.status??null,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.7.1 Shoving and Grappling'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,mode:normalized,attackerRoll,defenderRoll,outcome,destination,targetDestination,distanceFeet:distance,grappleState,actionCost,combat:clone(c)});
  }

"""
marker="  shoveNpc({"
if '  manipulateGrappledNpc({' not in s:
    if marker not in s:raise SystemExit('shove method marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

# --- session wrapper ---
p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text()
old="  async grappleNpc(args){return this._narrate(args.campaignId,this.actions.grappleNpc(args),'PLAYER_GRAPPLE_START');}"
new=old+"\n  async manipulateGrappledNpc(args){return this._narrate(args.campaignId,this.actions.manipulateGrappledNpc(args),'PLAYER_GRAPPLE_MANIPULATION');}"
s=replace_once(s,old,new,'session grapple manipulation wrapper');p.write_text(s)

# --- combat HTML ---
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="grappleButton" class="weapon-choice" type="button"><b>AGARRAR • FOR/BRIGA</b><small>Exige as duas mãos livres; ataque desarmado sem dano seguido de disputa.</small></button>'
new=old+'<button id="grappleMoveButton" class="weapon-choice" type="button"><b>ARRASTAR ALVO • ATÉ 10 PÉS</b><small>Gasta Ação Principal; toque no campo para escolher o destino.</small></button><button id="grappleThrowButton" class="weapon-choice" type="button"><b>ARREMESSAR ALVO • ATÉ 5 PÉS</b><small>Gasta Ação Principal; em sucesso o alvo fica caído e o agarrão termina.</small></button>'
s=replace_once(s,old,new,'grapple manipulation buttons');p.write_text(s)

# --- combat JS ---
p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
s=replace_once(s,
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let selectedEnemyId=null;",
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let selectedEnemyId=null;",
'grapple manipulation mode state')
s=replace_once(s,
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;$('moveHint').classList.add('hidden');render()};",
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;$('moveHint').classList.add('hidden');render()};",
'clear grapple mode on hero select')
old="const alreadyAssailant=(pc.grapple?.asAssailant?.length??0)>0;$('grappleButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||pc.hands?.status!=='OK'||pc.hands?.bothHandsFree!==true||alreadyAssailant;$('grappleButton').querySelector('b').textContent=alreadyAssailant?'AGARRANDO ALVO':'AGARRAR • FOR/BRIGA';$('reloadButton').disabled="
new="const alreadyAssailant=(pc.grapple?.asAssailant?.length??0)>0;$('grappleButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||!enemy||enemy.defeated||pc.hands?.status!=='OK'||pc.hands?.bothHandsFree!==true||alreadyAssailant;$('grappleButton').querySelector('b').textContent=alreadyAssailant?'AGARRANDO ALVO':'AGARRAR • FOR/BRIGA';const canManipulate=pcTurn&&pc.economy?.mainAvailable!==false&&alreadyAssailant;$('grappleMoveButton').disabled=!canManipulate;$('grappleThrowButton').disabled=!canManipulate;$('reloadButton').disabled="
s=replace_once(s,old,new,'render grapple manipulation buttons')
s=replace_once(s,
"$('moveButton').onclick=()=>{moveMode=!moveMode;$('moveHint').classList.toggle('hidden',!moveMode)};",
"$('moveButton').onclick=()=>{grappleManipulationMode=null;moveMode=!moveMode;$('moveHint').textContent='TOQUE NO CAMPO PARA MOVER';$('moveHint').classList.toggle('hidden',!moveMode)};",
'move button mode reset')
old="$('battlefield').addEventListener('click',async e=>{if(!moveMode||e.target.closest('.token'))return;moveMode=false;$('moveHint').classList.add('hidden');const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());let r=await session.movePc({campaignId,pcId:selectedPcId,...dest});if(r.status==='FREE_ATTACK_CONFIRMATION_REQUIRED_NO_MUTATION'){if(confirm('Sair do combate corpo a corpo permite um ataque livre do inimigo. Mover mesmo assim?'))r=await session.movePc({campaignId,pcId:selectedPcId,...dest,acceptFreeAttack:true})}logResult(r,r.status==='OK'?`Movimento resolvido: ${Math.round(r.inspection?.distanceFeet??0)} pés.`:r.status);render()});"
new="$('battlefield').addEventListener('click',async e=>{if(e.target.closest('.token'))return;const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());if(grappleManipulationMode){const mode=grappleManipulationMode;grappleManipulationMode=null;$('moveHint').classList.add('hidden');const p=projection(),pc=currentPc(p),targetId=pc?.grapple?.asAssailant?.[0]?.targetId;if(!pc||!targetId){$('log').textContent='Nenhum alvo agarrado disponível.';render();return;}const r=await session.manipulateGrappledNpc({campaignId,pcId:pc.actorId,npcId:targetId,mode,...dest});let fallback=r.status;if(r.status==='OK')fallback=!r.outcome.success?'O alvo venceu ou empatou a disputa e escapou.':mode==='MOVE'?`Alvo arrastado ${Math.round(r.distanceFeet)} pés; o agarrão continua.`:'Alvo arremessado e derrubado; o agarrão terminou.';logResult(r,fallback);render();return;}if(!moveMode)return;moveMode=false;$('moveHint').classList.add('hidden');let r=await session.movePc({campaignId,pcId:selectedPcId,...dest});if(r.status==='FREE_ATTACK_CONFIRMATION_REQUIRED_NO_MUTATION'){if(confirm('Sair do combate corpo a corpo permite um ataque livre do inimigo. Mover mesmo assim?'))r=await session.movePc({campaignId,pcId:selectedPcId,...dest,acceptFreeAttack:true})}logResult(r,r.status==='OK'?`Movimento resolvido: ${Math.round(r.inspection?.distanceFeet??0)} pés.`:r.status);render()});"
s=replace_once(s,old,new,'battlefield grapple manipulation routing')
handler="""
function armGrappleManipulation(mode){moveMode=false;grappleManipulationMode=mode;$('moveHint').textContent=mode==='MOVE'?'TOQUE NO DESTINO • ARRASTAR ATÉ 10 PÉS':'TOQUE NO DESTINO • ARREMESSAR ATÉ 5 PÉS';$('moveHint').classList.remove('hidden');closePopups()}
$('grappleMoveButton').onclick=()=>armGrappleManipulation('MOVE');
$('grappleThrowButton').onclick=()=>armGrappleManipulation('THROW');
"""
marker="$('grappleButton').onclick=async()=>"
if "$('grappleMoveButton').onclick" not in s:
    if marker not in s:raise SystemExit('grapple button handler marker missing')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)
print('WWN grapple move/throw player integration applied')
