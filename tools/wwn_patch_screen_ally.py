from pathlib import Path
import re


def replace_once(text, old, new, label):
    if new in text:
        return text
    if old not in text:
        raise SystemExit(f'{label} marker missing')
    return text.replace(old, new, 1)


def ensure_named_import(text, module, names):
    pat=re.compile(r"import \{ ([^}]*) \} from '"+re.escape(module)+r"';")
    m=pat.search(text)
    if not m: raise SystemExit(f'import marker missing: {module}')
    current=[x.strip() for x in m.group(1).split(',') if x.strip()]
    for name in names:
        if name not in current: current.append(name)
    return text[:m.start()]+"import { "+', '.join(current)+" } from '"+module+"';"+text[m.end():]

p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=ensure_named_import(s,'./wwn-special-combat-core.js',['screenCapacity','resolveScreenContest'])
s=replace_once(s,
"  if(combat.activeSideId==='PC')clearTotalDefenseForPcSide(combat);",
"  if(combat.activeSideId==='PC'){clearTotalDefenseForPcSide(combat);combat.screenState={};}",
'screen expiry at next PC side')

marker="function mainAvailableWithEcho(state,combat,targetId){return actionEconomyWithEcho071({state,combat,targetId,economy:combat?.economy?.[targetId]??freshActionEconomy()});}\n"
helper="""
function highestScreenCombatSkill(character){const options=['Punch','Stab','Shoot'].map(skill=>({skill,level:Number.isInteger(character?.skills?.[skill])?character.skills[skill]:null})).filter(x=>x.level!==null&&x.level>=0).sort((a,b)=>b.level-a.level||a.skill.localeCompare(b.skill));return options[0]??null;}
function resolveScreenRedirectInPlace(next,combat,rng,enemyId,wardId){
  const enemy=next.npcState?.[enemyId];if(!enemy||!Number.isInteger(enemy.skillBonus))return Object.freeze({status:'MISSING_SCREEN_ATTACKER_SKILL_EVIDENCE'});const wardPos=combat.tactical?.actors?.[wardId];if(!wardPos)return Object.freeze({status:'OK',targetPcId:wardId,reason:'WARD_POSITION_MISSING'});const candidates=[];
  for(const [defenderId,raw] of Object.entries(combat.screenState??{})){if(raw?.wardId!==wardId)continue;const defender=characterById(next,defenderId),defPos=combat.tactical?.actors?.[defenderId];if(!isCombatCapablePc(defender)||!defPos)continue;if(distanceFeet(defPos,wardPos)>3*3.280839895+1e-9)continue;const used=[...(raw.screenedAttackerIds??[])];if(used.includes(enemyId)||used.length>=Number(raw.maxAttackers??0))continue;const skill=highestScreenCombatSkill(defender);if(!skill||skill.level<1)continue;const attributeScore=Math.max(Number(defender.attributes?.Strength??0),Number(defender.attributes?.Dexterity??0));if(!Number.isInteger(attributeScore)||attributeScore<3)continue;const roll=resolvePcManeuverSkillRoll({rng,attributeScore,skillLevel:skill.level,streamName:`combat/screen/${enemyId}/${wardId}/${defenderId}`});candidates.push({defenderId,roll,skill});}
  if(!candidates.length)return Object.freeze({status:'OK',targetPcId:wardId,reason:'NO_ELIGIBLE_SCREEN'});const attackerRoll=resolveNpcManeuverSkillRoll({rng,skillBonus:enemy.skillBonus,streamName:`combat/screen/${enemyId}/${wardId}/attacker`});const contest=resolveScreenContest({attackerTotal:attackerRoll.total,attackerIsPc:false,defenders:candidates.map(x=>({defenderId:x.defenderId,total:x.roll.total,isPc:true}))});for(const c of candidates){const raw=combat.screenState[c.defenderId];combat.screenState[c.defenderId]={...raw,screenedAttackerIds:Object.freeze([...(raw.screenedAttackerIds??[]),enemyId])};}return Object.freeze({status:'OK',wardId,targetPcId:contest.defenderId??wardId,redirected:Boolean(contest.defenderId),attackerRoll,defenderRolls:Object.freeze(candidates.map(x=>Object.freeze({defenderId:x.defenderId,roll:x.roll,skill:x.skill}))),contest});
}
"""
if 'function resolveScreenRedirectInPlace(' not in s:
    if marker not in s: raise SystemExit('screen helper marker missing')
    s=s.replace(marker,marker+helper,1)

method="""
  screenAlly({campaignId,pcId=null,wardPcId}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});const combat=tacticalCombat(state);if(!combat)return Object.freeze({status:'NO_ACTIVE_TACTICAL_COMBAT_NO_MUTATION',revision:loaded.revision});if(combat.activeSideId!=='PC')return Object.freeze({status:'NOT_PC_SIDE_NO_MUTATION',revision:loaded.revision});const defenderId=resolvePcId(state,combat,pcId);if(!defenderId)return Object.freeze({status:'PC_NOT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});const wardId=String(wardPcId??'');if(!combatPcIds(state,combat).includes(wardId)||wardId===defenderId)return Object.freeze({status:'INVALID_SCREEN_WARD_NO_MUTATION',revision:loaded.revision});const defender=characterById(state,defenderId),ward=characterById(state,wardId);if(!isCombatCapablePc(defender)||!isCombatCapablePc(ward))return Object.freeze({status:'SCREEN_COMBATANT_INCAPACITATED_NO_MUTATION',revision:loaded.revision});if(isActorGrappleLocked(combat,defenderId))return Object.freeze({status:'GRAPPLED_CANNOT_SCREEN_NO_MUTATION',revision:loaded.revision});const economy=mainAvailableWithEcho(state,combat,defenderId);if(economy.moveAvailable!==true)return Object.freeze({status:'MOVE_ACTION_ALREADY_SPENT_NO_MUTATION',revision:loaded.revision});const skill=highestScreenCombatSkill(defender);const plan=screenCapacity({highestCombatSkill:skill?.level??0});if(!plan.canScreen)return Object.freeze({status:'SCREEN_REQUIRES_COMBAT_SKILL_1_NO_MUTATION',revision:loaded.revision});
    const from=combat.tactical.actors?.[defenderId],wardPos=combat.tactical.actors?.[wardId];if(!from||!wardPos)return Object.freeze({status:'MISSING_TACTICAL_POSITION_NO_MUTATION',revision:loaded.revision});const distance=distanceFeet(from,wardPos),moveNeeded=Math.max(0,distance-5);const moveProfile=deriveCombatMoveFeet(defender);if(moveProfile.status!=='OK')return Object.freeze({status:'MISSING_EVIDENCE_MOVE_RATE_NO_MUTATION',revision:loaded.revision});const magicMove=effectiveMoveFeet071({state,targetId:defenderId,round:combat.round,baseMoveFeet:moveProfile.moveFeet}),proneMove=effectiveProneMoveFeet({combat,actorId:defenderId,baseMoveFeet:magicMove.moveFeet});if(moveNeeded>proneMove.moveFeet+1e-9)return Object.freeze({status:'SCREEN_ALLY_OUT_OF_MOVE_RANGE_NO_MUTATION',revision:loaded.revision,moveNeeded,maxMoveFeet:proneMove.moveFeet});if(moveNeeded>1e-9&&combat.tactical?.pathModel!=='CLEAR_STARTER_COURTYARD')return Object.freeze({status:'SCREEN_PATH_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision,pathModel:combat.tactical?.pathModel??null});let destination={xFeet:from.xFeet,yFeet:from.yFeet};if(moveNeeded>1e-9){const ux=(wardPos.xFeet-from.xFeet)/distance,uy=(wardPos.yFeet-from.yFeet)/distance;destination={xFeet:from.xFeet+ux*moveNeeded,yFeet:from.yFeet+uy*moveNeeded};const enemies=(combat.enemyIds??[]).filter(id=>(state.npcState?.[id]?.hp??0)>0&&state.npcState?.[id]?.weaponContract?.melee!==false).map(id=>({actorId:id,position:combat.tactical.actors?.[id]})).filter(x=>x.position);const leaving=enemies.some(e=>distanceFeet(from,e.position)<=5+1e-9&&distanceFeet(destination,e.position)>5+1e-9);if(leaving&&combat.economy?.[defenderId]?.withdrewFromMelee!==true)return Object.freeze({status:'SCREEN_FREE_ATTACK_REQUIRES_WITHDRAWAL_NO_MUTATION',revision:loaded.revision});}
    const next=clone(state),c=next.encounterState.combat;c.tactical.actors[defenderId]={...c.tactical.actors[defenderId],xFeet:destination.xFeet,yFeet:destination.yFeet};const actionCost=applyActionCostWithEchoInPlace071({state:next,combat:c,targetId:defenderId,cost:'MOVE'});if(actionCost.status!=='OK')return Object.freeze({status:`${actionCost.status}_NO_MUTATION`,revision:loaded.revision});const screen=Object.freeze({wardId,activatedRound:c.round,maxAttackers:plan.maxAttackersPerRound,combatSkill:skill.skill,combatSkillLevel:skill.level,screenedAttackerIds:Object.freeze([])});c.screenState={...(c.screenState??{}),[defenderId]:screen};next.encounterState.lastCombatResolution={kind:'SCREEN_ALLY',defenderId,wardId,from,destination,moveNeeded,screen};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'PLAYER_SCREEN_ALLY',suffix:`screen:${defenderId}:${wardId}`,payload:{defenderId,wardId,from,destination,moveNeeded,screen,actionCost,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.4 Screen an Ally'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,defenderId,wardId,from,destination,moveNeeded,screen,actionCost,combat:clone(c)});
  }

"""
insert="  movePc({campaignId,pcId=null,xFeet,yFeet,acceptFreeAttack=false,reactions={},usesVelocitousTraversal=false}){"
if '  screenAlly({' not in s:
    if insert not in s: raise SystemExit('screen method insertion marker missing')
    s=s.replace(insert,method+insert,1)

old="const targetId=targetChoice.pcId;let pcPos=c.tactical.actors[targetId];let movedFeet=0;"
new="const intendedTargetId=targetChoice.pcId;let targetId=intendedTargetId;let screen=null;let pcPos=c.tactical.actors[intendedTargetId];let movedFeet=0;"
s=replace_once(s,old,new,'screen enemy target declaration')
old="if(dist<=5+1e-9){if(enemy.weaponContract?.melee!==false)reloadInterruptions=interruptSingleShotReloadForPc(next,targetId,{reason:'ENEMY_MELEE',enemyId,round:c.round});"
new="if(dist<=5+1e-9){screen=resolveScreenRedirectInPlace(next,c,rng,enemyId,intendedTargetId);if(screen?.status!=='OK')return Object.freeze({status:`${screen.status}_NO_MUTATION`,revision:loaded.revision,screen});targetId=screen?.targetPcId??intendedTargetId;if(enemy.weaponContract?.melee!==false)reloadInterruptions=interruptSingleShotReloadForPc(next,targetId,{reason:'ENEMY_MELEE',enemyId,round:c.round});"
s=replace_once(s,old,new,'screen enemy redirect')
s=s.replace("actions.push({enemyId,targetPcId:targetId,movedFeet,attack,wardpactSave:","actions.push({enemyId,intendedTargetPcId:intendedTargetId,targetPcId:targetId,screen,movedFeet,attack,wardpactSave:")
s=s.replace("actions.push({enemyId,targetPcId:targetId,movedFeet,attack,hpBefore,hpAfter,mortal,reloadInterruptions});", "actions.push({enemyId,intendedTargetPcId:intendedTargetId,targetPcId:targetId,screen,movedFeet,attack,hpBefore,hpAfter,mortal,reloadInterruptions});")
p.write_text(s)

p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text();old="  async movePc(args){return this._narrate(args.campaignId,this.actions.movePc(args),'PLAYER_MOVED');}";new="  async screenAlly(args){return this._narrate(args.campaignId,this.actions.screenAlly(args),'PLAYER_SCREEN_ALLY');}\n"+old;s=replace_once(s,old,new,'screen session wrapper');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.html');s=p.read_text();old='<button id="chargeButton" class="weapon-choice" type="button"><b>CARGA</b><small>Gasta Ação Principal + Movimento; avance em linha reta e ataque com +2, sofrendo −2 CA até o fim da rodada.</small></button>';new=old+'<button id="screenButton" class="weapon-choice" type="button"><b>PROTEGER ALIADO</b><small>Gasta Movimento; toque no aliado que você quer proteger até seu próximo turno.</small></button>';s=replace_once(s,old,new,'screen UI button');p.write_text(s)

p=Path('app/src/main/assets/www/scr022.js');s=p.read_text()
s=replace_once(s,"const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let chargeMode=false;let shovePushSkill=null;let selectedEnemyId=null;","const $=id=>document.getElementById(id);const FIELD_FEET=90;let moveMode=false;let grappleManipulationMode=null;let chargeMode=false;let shovePushSkill=null;let screenSelectMode=false;let selectedEnemyId=null;",'screen UI state')
old="btn.onclick=e=>{e.stopPropagation();selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;chargeMode=false;shovePushSkill=null;$('moveHint').classList.add('hidden');render()};"
new="btn.onclick=async e=>{e.stopPropagation();if(screenSelectMode){if(pc.actorId===selectedPcId){$('log').textContent='Escolha outro herói para proteger.';return;}const defenderId=selectedPcId;screenSelectMode=false;$('moveHint').classList.add('hidden');const r=await session.screenAlly({campaignId,pcId:defenderId,wardPcId:pc.actorId});logResult(r,r.status==='OK'?`${pc.name} está protegido até o próximo turno do defensor.`:r.status);render();return;}selectedPcId=pc.actorId;moveMode=false;grappleManipulationMode=null;chargeMode=false;shovePushSkill=null;$('moveHint').classList.add('hidden');render()};"
s=replace_once(s,old,new,'screen hero selection')
old="$('chargeButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||pc.economy?.moveAvailable===false||!enemy||enemy.defeated||!chargeWeapon||pc.prone===true||pc.grapple?.locked===true;const shoveReady="
new="$('chargeButton').disabled=!pcTurn||pc.economy?.mainAvailable===false||pc.economy?.moveAvailable===false||!enemy||enemy.defeated||!chargeWeapon||pc.prone===true||pc.grapple?.locked===true;$('screenButton').disabled=!pcTurn||pc.economy?.moveAvailable===false||pc.grapple?.locked===true||p.pcs.filter(x=>!x.dead&&!x.mortallyInjured).length<2;const shoveReady="
s=replace_once(s,old,new,'render screen button')
handler="""
$('screenButton').onclick=()=>{moveMode=false;grappleManipulationMode=null;chargeMode=false;shovePushSkill=null;screenSelectMode=true;$('moveHint').textContent='TOQUE NO HERÓI QUE VOCÊ QUER PROTEGER';$('moveHint').classList.remove('hidden');closePopups()};
"""
marker="function armShovePush(skill){"
if "$('screenButton').onclick" not in s:
    if marker not in s: raise SystemExit('screen handler marker missing')
    s=s.replace(marker,handler+marker,1)
p.write_text(s)
print('WWN canonical Screen an Ally patch applied')
