from pathlib import Path
import re


def ensure_named_import(text,module,names):
    pat=re.compile(r"import \{ ([^}]*) \} from '"+re.escape(module)+r"';")
    m=pat.search(text)
    if not m: raise SystemExit(f'import marker missing: {module}')
    current=[x.strip() for x in m.group(1).split(',') if x.strip()]
    for name in names:
        if name not in current: current.append(name)
    return text[:m.start()]+"import { "+', '.join(current)+" } from '"+module+"';"+text[m.end():]

p=Path('app/src/main/assets/www/core/combat-service.js');s=p.read_text()
s=ensure_named_import(s,'./wwn-special-combat-core.js',['rangedExecutionDifficulty','executionPhysicalSavePlan','resolveExecutionAfterSave'])
method="""
  executionAttackNpc({campaignId,pcId=null,npcId,weaponId='UNARMED',modeId=null,combatSkill=null,targetAwareness=null,uninterruptedAimSeconds=0,rangeBand='NORMAL',nonLethal=false}){
    const {loaded,state}=loadState(this.persistence,campaignId);if(!state)return Object.freeze({status:loaded.status});if(tacticalCombat(state))return Object.freeze({status:'EXECUTION_TARGET_ALERT_IN_COMBAT_NO_MUTATION',revision:loaded.revision});if(String(targetAwareness??'').toUpperCase()!=='UNSUSPECTING')return Object.freeze({status:'EXECUTION_UNSUSPECTING_EVIDENCE_REQUIRED_NO_MUTATION',revision:loaded.revision});if(Number(uninterruptedAimSeconds)<60)return Object.freeze({status:'EXECUTION_FULL_MINUTE_REQUIRED_NO_MUTATION',revision:loaded.revision,requiredSeconds:60});
    const actorId=String(pcId??state.characterState?.characterId??'');const attacker=characterById(state,actorId);if(!attacker)return Object.freeze({status:'CHARACTER_NOT_FOUND_NO_MUTATION',revision:loaded.revision});const weaponResult=playerWeapon(state,actorId,weaponId,{tactical:false,modeId,combatSkill});if(weaponResult.status!=='OK')return Object.freeze({status:weaponResult.status,revision:loaded.revision});const weapon=weaponResult.weapon,npc=state.npcState?.[npcId];if(!npc||!Number.isInteger(npc.hp)||!Number.isInteger(npc.save))return Object.freeze({status:'MISSING_EXECUTION_TARGET_EVIDENCE_NO_MUTATION',revision:loaded.revision});const rng=DeterministicRng.fromState(state.rngState);let setupCheck=null,executionHit=false,rangePlan=null;
    if(weapon.melee===false){rangePlan=rangedExecutionDifficulty({rangeBand});const shootLevel=Number.isInteger(attacker.skills?.Shoot)?attacker.skills.Shoot:null;if(!Number.isInteger(attacker.attributes?.Dexterity))return Object.freeze({status:'MISSING_EXECUTION_DEXTERITY_EVIDENCE_NO_MUTATION',revision:loaded.revision});setupCheck=resolveSkillCheck({rng,skillLevel:shootLevel,attributeScore:attacker.attributes.Dexterity,difficulty:rangePlan.difficulty,situationalModifier:0,streamName:`execution/${actorId}/${npcId}/aim`});executionHit=setupCheck.success===true;}else executionHit=true;
    const next=clone(state);next.rngState=rng.exportState();let savePlan=null,saveResolution=null,maximumDamage=null,outcome=null,hpBefore=npc.hp,hpAfter=npc.hp;if(executionHit){const skillLevel=Math.max(0,Number.isInteger(attacker.skills?.[weapon.combatSkill])?attacker.skills[weapon.combatSkill]:0);savePlan=executionPhysicalSavePlan({combatSkillLevel:skillLevel});saveResolution=resolveSavingThrow({rng,saveType:'PHYSICAL',target:npc.save,modifier:savePlan.saveModifier,streamName:`execution/${actorId}/${npcId}/save`});maximumDamage=maximumWeaponHitDamage({attacker,weapon,damageBonus:weaponResult.mechanicalBonuses?.damageBonus??0});outcome=resolveExecutionAfterSave({saveSucceeded:saveResolution.success,nonLethal:Boolean(nonLethal),maximumDamage:maximumDamage.damage});next.rngState=rng.exportState();hpAfter=outcome.setHitPointsToZero?0:Math.max(0,npc.hp-outcome.damage);next.npcState[npcId]={...next.npcState[npcId],hp:hpAfter};if(hpAfter===0){if(nonLethal)next.npcState[npcId].visibleStatus='UNCONSCIOUS';else if(next.npcState[npcId].importantNamed===true)next.npcState[npcId].visibleStatus='MORTALLY_WOUNDED';else next.npcState[npcId].visibleStatus='DEAD';}}
    next.encounterState={...(next.encounterState??{}),lastCombatResolution:{kind:'EXECUTION_ATTACK',pcId:actorId,npcId,weaponId,modeId,targetAwareness:'UNSUSPECTING',uninterruptedAimSeconds:Number(uninterruptedAimSeconds),rangeBand:weapon.melee===false?String(rangeBand).toUpperCase():null,rangePlan,setupCheck,executionHit,savePlan,saveResolution,maximumDamage,outcome,targetHpBefore:hpBefore,targetHpAfter:hpAfter}};const {receipt}=commitState({persistence:this.persistence,loaded,next,campaignId,eventKind:'EXECUTION_ATTACK_RESOLVED',suffix:`execution:${actorId}:${npcId}`,payload:{pcId:actorId,npcId,weaponId,modeId,targetAwareness:'UNSUSPECTING',uninterruptedAimSeconds:Number(uninterruptedAimSeconds),rangeBand:weapon.melee===false?String(rangeBand).toUpperCase():null,rangePlan,setupCheck,executionHit,savePlan,saveResolution,maximumDamage,outcome,targetHpBefore:hpBefore,targetHpAfter:hpAfter,sourceRefs:[{sourceId:'SRC-WWN-SRD-1',locator:'§2.4.7.3 Execution Attacks'}]}});if(receipt.status!=='OK')return Object.freeze({status:receipt.status,receipt});return Object.freeze({status:'OK',receipt,pcId:actorId,npcId,weaponId,rangePlan,setupCheck,executionHit,savePlan,saveResolution,maximumDamage,outcome,targetHpBefore:hpBefore,targetHpAfter:hpAfter});
  }

"""
marker='  holdAction({campaignId,pcId=null}){'
if '  executionAttackNpc({' not in s:
    if marker not in s: raise SystemExit('execution insertion marker missing')
    s=s.replace(marker,method+marker,1)
p.write_text(s)

p=Path('app/src/main/assets/www/core/combat-session-service.js');s=p.read_text();old="  async holdAction(args){return this._narrate(args.campaignId,this.actions.holdAction(args),'PLAYER_HELD_ACTION');}";new="  async executionAttackNpc(args){return this._narrate(args.campaignId,this.actions.executionAttackNpc(args),'EXECUTION_ATTACK');}\n"+old
if new not in s:
    if old not in s: raise SystemExit('execution session marker missing')
    s=s.replace(old,new,1)
p.write_text(s)
print('WWN evidence-gated Execution Attack patch applied')
