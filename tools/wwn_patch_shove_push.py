from pathlib import Path

def replace_once(text,old,new,label):
    if new in text:return text
    if old not in text:raise SystemExit(f'{label} marker missing')
    return text.replace(old,new,1)

# --- combat service ---
p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=replace_once(s,
"  shoveNpc({campaignId,pcId=null,npcId,weaponId='UNARMED',modeId=null,combatSkill=null,opposedSkill='Punch'}){",
"  shoveNpc({campaignId,pcId=null,npcId,weaponId='UNARMED',modeId=null,combatSkill=null,opposedSkill='Punch',effect='PRONE',pushFeet=null}){",
'shove effect signature')
old="    const skill=String(opposedSkill??'').trim();if(!['Exert','Punch'].includes(skill))return Object.freeze({status:'SHOVE_REQUIRES_EXERT_OR_PUNCH_NO_MUTATION',revision:loaded.revision});if(!Number.isInteger(attacker.attributes?.Strength))return Object.freeze({status:'MISSING_STRENGTH_EVIDENCE_NO_MUTATION',revision:loaded.revision});"
new="    const skill=String(opposedSkill??'').trim();if(!['Exert','Punch'].includes(skill))return Object.freeze({status:'SHOVE_REQUIRES_EXERT_OR_PUNCH_NO_MUTATION',revision:loaded.revision});if(!Number.isInteger(attacker.attributes?.Strength))return Object.freeze({status:'MISSING_STRENGTH_EVIDENCE_NO_MUTATION',revision:loaded.revision});const effectKind=String(effect??'PRONE').toUpperCase();if(!['PRONE','PUSH'].includes(effectKind))return Object.freeze({status:'INVALID_SHOVE_EFFECT_NO_MUTATION',revision:loaded.revision});const maxPushFeet=3*3.280839895;const requestedPushFeet=effectKind==='PUSH'?Number(pushFeet):0;if(effectKind==='PUSH'&&(!Number.isFinite(requestedPushFeet)||requestedPushFeet<=0||requestedPushFeet>maxPushFeet+1e-9))return Object.freeze({status:'SHOVE_PUSH_DISTANCE_INVALID_NO_MUTATION',revision:loaded.revision,maxPushFeet});if(effectKind==='PUSH'&&combat.tactical?.pathModel!=='CLEAR_STARTER_COURTYARD')return Object.freeze({status:'SHOVE_PUSH_PATH_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision,pathModel:combat.tactical?.pathModel??null});if(effectKind==='PUSH'&&isActorGrappleLocked(combat,npcId))return Object.freeze({status:'SHOVE_GRAPPLED_TARGET_REQUIRES_GM_NO_MUTATION',revision:loaded.revision});"
s=replace_once(s,old,new,'shove effect validation')
old="    const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon});"
new="    const from=combat.tactical.actors?.[actorId],to=combat.tactical.actors?.[npcId];if(!from||!to)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const shoveVectorFeet=distanceFeet(from,to);if(effectKind==='PUSH'&&shoveVectorFeet<=1e-9)return Object.freeze({status:'SHOVE_PUSH_DIRECTION_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision});const range=inspectAttackRange({attackerPosition:from,targetPosition:to,weapon});"
s=replace_once(s,old,new,'shove push vector validation')
old="    const next=clone(state);next.rngState=rng.exportState();const c=next.encounterState.combat;const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});let targetProne=false;if(outcome.success){setActorProneInPlace(c,npcId,true);targetProne=true;}const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});"
new="    const next=clone(state);next.rngState=rng.exportState();const c=next.encounterState.combat;const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:actorId,cost:'MAIN'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});let targetProne=isActorProne(c,npcId),pushDestination=null;if(outcome.success){if(effectKind==='PRONE'){setActorProneInPlace(c,npcId,true);targetProne=true;}else{const ux=(to.xFeet-from.xFeet)/shoveVectorFeet,uy=(to.yFeet-from.yFeet)/shoveVectorFeet;pushDestination={xFeet:to.xFeet+ux*requestedPushFeet,yFeet:to.yFeet+uy*requestedPushFeet};c.tactical.actors[npcId]={...c.tactical.actors[npcId],xFeet:pushDestination.xFeet,yFeet:pushDestination.yFeet};}}const tumefaction=applyTumefactionVigorousActionInPlace071({state:next,targetId:actorId,round:c.round});"
s=replace_once(s,old,new,'shove push outcome')
old="next.encounterState.lastCombatResolution={kind:'SHOVE',pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,targetHpBefore:hpBefore,targetHpAfter:hpAfter};"
new="next.encounterState.lastCombatResolution={kind:'SHOVE',effect:effectKind,pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,pushFeet:effectKind==='PUSH'?requestedPushFeet:null,pushDestination,targetHpBefore:hpBefore,targetHpAfter:hpAfter};"
s=replace_once(s,old,new,'shove last resolution')
old="payload:{pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,actionCost,"
new="payload:{pcId:actorId,npcId,weaponId,modeId,opposedSkill:skill,effect:effectKind,range,proneModifiers,size,attack,attackerRoll,defenderRoll,outcome,targetProne,pushFeet:effectKind==='PUSH'?requestedPushFeet:null,pushDestination,actionCost,"
s=replace_once(s,old,new,'shove payload')
old="return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,attack,attackerRoll,defenderRoll,outcome,targetProne,actionCost,targetHpBefore:hpBefore,targetHpAfter:hpAfter,combat:clone(c)});"
new="return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,effect:effectKind,attack,attackerRoll,defenderRoll,outcome,targetProne,pushFeet:effectKind==='PUSH'?requestedPushFeet:null,pushDestination,actionCost,targetHpBefore:hpBefore,targetHpAfter:hpAfter,combat:clone(c)});"
s=replace_once(s,old,new,'shove return')
p.write_text(s)

# --- combat HTML ---
p=Path('app/src/main/assets/www/scr022.html');s=p.read_text()
old='<button id="shovePunchButton" class="weapon-choice" type="button"><b>DERRUBAR • FOR/BRIGA</b><small>Ataque melee sem dano, seguido de disputa de perícia.</small></button>'
new=old+'<button id="shovePushExertButton" class="weapon-choice" type="button"><b>EMPURRAR • FOR/ESFORÇO</b><small>Ataque melee sem dano; em sucesso empurra o alvo para trás até 3 metros.</small></button><button id="shovePushPunchButton" class="weapon-choice" type="button"><b>EMPURRAR • FOR/BRIGA</b><small>Ataque melee sem dano; em sucesso empurra o alvo para trás até 3 metros.</small></button>'
s=replace_once(s,old,new,'shove push buttons');p.write_text(s)

# --- combat JS ---
p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
s=replace_once(s,
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let chargeMode=false;let selectedEnemyId=null;",
"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let chargeMode=false;let shovePushSkill=null;let selectedEnemyId=null;",
'shove push UI state')
s=replace_once(s,
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;chargeMode=false;$('moveHint').classList.add('hidden');render()};",
"btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;chargeMode=false;shovePushSkill=null;$('moveHint').classList.add('hidden');render()};",
'clear shove push on hero select')
old="$('shoveExertButton').disabled=!shoveReady;$('shovePunchButton').disabled=!shoveReady;const alreadyAssailant="
new="$('shoveExertButton').disabled=!shoveReady;$('shovePunchButton').disabled=!shoveReady;$('shovePushExertButton').disabled=!shoveReady;$('shovePushPunchButton').disabled=!shoveReady;const alreadyAssailant="
s=replace_once(s,old,new,'render shove push buttons')
s=replace_once(s,
"$('moveButton').onclick=()=>{chargeMode=false;grappleManipulationMode=null;moveMode=!moveMode;",
"$('moveButton').onclick=()=>{chargeMode=false;grappleManipulationMode=null;shovePushSkill=null;moveMode=!moveMode;",
'clear shove push on move')
old="$('battlefield').addEventListener('click',async e=>{if(e.target.closest('.token'))return;const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());if(chargeMode){"
new="$('battlefield').addEventListener('click',async e=>{if(e.target.closest('.token'))return;const dest=feetFromPointer(e,$('battlefield').getBoundingClientRect());if(shovePushSkill){const skill=shovePushSkill;shovePushSkill=null;$('moveHint').classList.add('hidden');const p=projection(),pc=currentPc(p),choice=currentWeapon(pc),enemy=p.enemies.find(x=>x.actorId===selectedEnemyId);if(!pc||!choice||!enemy?.position){render();return;}const requested=Math.min(3*3.280839895,Math.hypot(dest.xFeet-enemy.position.xFeet,dest.yFeet-enemy.position.yFeet));if(requested<=0.1){$('log').textContent='Escolha uma distância maior para o empurrão.';render();return;}const r=await session.shoveNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,opposedSkill:skill,effect:'PUSH',pushFeet:requested});let fallback=r.status;if(r.status==='OK')fallback=!r.attack.hit?'A tentativa de empurrar não acertou o ataque corpo a corpo.':r.outcome.success?`Alvo empurrado ${Math.round(r.pushFeet)} pés para trás.`:'O golpe acertou, mas o alvo venceu a disputa e não foi empurrado.';logResult(r,fallback);render();return;}if(chargeMode){"
s=replace_once(s,old,new,'route shove push battlefield distance')
old="async function shoveWithSkill(opposedSkill){const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice)return;const r=await session.shoveNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,opposedSkill});"
new="async function shoveWithSkill(opposedSkill){const p=projection(),pc=currentPc(p),choice=currentWeapon(pc);if(!pc||!choice)return;const r=await session.shoveNpc({campaignId,pcId:pc.actorId,npcId:selectedEnemyId,weaponId:choice.weaponId,modeId:choice.modeId,combatSkill:choice.combatSkill,opposedSkill,effect:'PRONE'});"
s=replace_once(s,old,new,'explicit prone shove')
handler="""
function armShovePush(skill){moveMode=false;grappleManipulationMode=null;chargeMode=false;shovePushSkill=skill;$('moveHint').textContent='TOQUE NO CAMPO PARA ESCOLHER A DISTÂNCIA • MÁX. 3 METROS';$('moveHint').classList.remove('hidden');closePopups()}
$('shovePushExertButton').onclick=()=>armShovePush('Exert');$('shovePushPunchButton').onclick=()=>armShovePush('Punch');
"""
marker="$('shoveExertButton').onclick=()=>shoveWithSkill('Exert');"
if "$('shovePushExertButton').onclick" not in s:
    if marker not in s:raise SystemExit('shove prone handlers marker missing')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)
print('WWN shove push option applied')
