from pathlib import Path
import sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_commands.py <project-root>")
root=Path(sys.argv[1]).resolve()

def read(rel): return (root/rel).read_text(encoding="utf-8")
def write(rel,text): (root/rel).write_text(text,encoding="utf-8")
def req(text,old,new,label):
    if old not in text: raise SystemExit("missing anchor: "+label)
    return text.replace(old,new,1)

rel="lib/master/commands.ts"
cmd=read(rel)

cmd=req(
    cmd,
    "import { allowedBanes, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "import { allowedBanes, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';\nimport { activeQualitiesForItem, actorIsBane, applyIllLuckToResult, cleavingEnabled, curseBlocksHopeForWeapon, curseDarkeningDicePenalty, curseHuntedBy, famousDamageBonus, famousInjuryBonus, famousItemForWeapon, famousOnHitHateLoss, famousPiercingThreshold, famousShieldDynamicParryBonus, flameOfHopeRecovery, foeSlayerProtection, heavyKnocksDown, hollowSteelExtraVolley, luminousAutoAvoidAmbush, runeScoredHelmIgnoresCombatTaskConditions, runeScoredShieldMakesAttackerWeary } from '../rules/magicalGearMechanics';",
    "magical gear command imports",
)
cmd=req(
    cmd,
    "import { adjustShadowCanonical, firmWill, healShadowScar, resolveBoutOfMadness, resolveShadowGain, shadowAtLimit } from '../rules/shadow';",
    "import { adjustShadowCanonical, firmWill, flawsForShadowPath, healShadowScar, recomputeShadowState, resolveBoutOfMadness, resolveShadowGain, shadowAtLimit } from '../rules/shadow';",
    "curse shadow imports",
)

# Preserve non-condition state notes whenever a sheet update synchronizes the combat actor.
cmd=req(
    cmd,
    """          stateNotes: [attributes.weary ? 'Cansado' : '', attributes.miserable ? 'Arrasado' : '', attributes.wounded ? 'Ferido' : ''].filter(Boolean),""",
    """          stateNotes: [
            ...(actor.stateNotes||[]).filter(note=>!['Cansado','Arrasado','Ferido'].includes(note)),
            ...[attributes.weary?'Cansado':'',attributes.miserable?'Arrasado':'',attributes.wounded?'Ferido':''].filter(Boolean),
          ],""",
    "preserve combat state notes",
)

# Runtime state/config commands.
anchor="    case 'player_start_journey': {"
cases=r"""    case 'set_combat_actor_banes': {
      const actor=nextState.combat.actors.find(item=>item.actorId===command.actorId);
      if(!actor) throw new Error('Combatente não encontrado.');
      const allowed=new Set(['Orcs','Trolls','Wolves','Evil Men','Undead','Spiders']);
      const baneTags=Array.from(new Set((command.baneTags||[]).filter(value=>allowed.has(value))));
      let notes=(actor.stateNotes||[]).filter(note=>!note.startsWith('hunted-curse-detected:')&&!note.startsWith('luminous-warning:'));
      for(const hero of nextCharacters.filter(character=>partyIds.has(character.id))){
        for(const tag of baneTags){
          if(curseHuntedBy(hero.sheet,tag)) notes.push('hunted-curse-detected:'+hero.id+':'+tag);
        }
        if(luminousAutoAvoidAmbush(hero.sheet,baneTags as any)) notes.push('luminous-warning:'+hero.id);
      }
      nextState=upsertCombatActor(nextState,{...actor,baneTags:baneTags as any,stateNotes:Array.from(new Set(notes))});
      break;
    }

    case 'set_combat_actor_attribute_level': {
      const actor=nextState.combat.actors.find(item=>item.actorId===command.actorId);
      if(!actor) throw new Error('Combatente não encontrado.');
      nextState=upsertCombatActor(nextState,{...actor,attributeLevel:Math.max(1,Math.trunc(Number(command.attributeLevel)||1))});
      break;
    }

    case 'stand_up_combat_actor': {
      const actor=nextState.combat.actors.find(item=>item.actorId===command.actorId);
      if(!actor||(actor.stateNotes||[]).indexOf('knocked-down')<0) throw new Error('Este combatente não está derrubado.');
      nextState=upsertCombatActor(nextState,{...actor,stateNotes:[...(actor.stateNotes||[]).filter(note=>note!=='knocked-down'),'temp-round:main-action-used:stand-up']});
      break;
    }

    case 'resolve_cleaving_attack': {
      const ready=nextState.combat.cleavingReadyByHero?.[command.attackerId];
      const actor=nextState.combat.actors.find(item=>item.actorId===command.attackerId);
      const target=nextState.combat.actors.find(item=>item.actorId===command.targetActorId);
      if(!ready||!actor||!target||target.defeated) throw new Error('Não há ataque imediato de Perfuradora disponível.');
      if(!(actor.engagedWith||[]).includes(target.actorId)) throw new Error('Perfuradora só pode atacar imediatamente outro adversário já engajado.');
      const result=applyOne(
        {type:'resolve_hero_attack',attackerId:command.attackerId,targetActorId:command.targetActorId,weaponIndex:ready.weaponIndex,reason:'Ataque imediato concedido por Perfuradora.'} as MasterCommand,
        nextCampaign,nextCharacters,nextNpcs,nextState,partyIds,
      );
      nextCharacters=result.characters; nextNpcs=result.npcs; nextState=result.masterState;
      nextState.combat={...nextState.combat,cleavingReadyByHero:{...(nextState.combat.cleavingReadyByHero||{}),[command.attackerId]:undefined as any}};
      break;
    }

    case 'set_magical_curse_state': {
      updateHero(command.targetCharacterId,sheet=>{
        const next=structuredClone(sheet);
        const item=next.inventory.find(entry=>entry.id===command.inventoryItemId);
        if(!item||!item.magicalCurse) throw new Error('Item amaldiçoado não encontrado.');
        const wasActive=Boolean(item.magicalCurseActive);
        const oldShadow=Math.max(0,Number(item.magicalCurseShadowApplied)||0);
        item.magicalCurseActive=Boolean(command.active);
        if(command.revealed!==undefined) item.magicalCurseRevealed=Boolean(command.revealed);
        if(command.ownerPresent!==undefined) item.magicalCurseOwnerPresent=Boolean(command.ownerPresent);
        if(command.hunterTag!==undefined) item.magicalCurseHunterTag=String(command.hunterTag||'').trim();

        if(item.magicalCurse==='weakening'){
          item.tags=(item.tags||[]).filter(tag=>!/^curse-attribute:/i.test(tag));
          if(command.active){
            if(!command.weakeningAttribute) throw new Error('Enfraquecimento exige escolher Força, Coração ou Astúcia.');
            item.tags.push('curse-attribute:'+command.weakeningAttribute);
          }
        }

        if(item.magicalCurse==='weakness-curse'){
          if(command.active){
            const flaws=flawsForShadowPath(next.other.shadowPath);
            item.magicalCurseWeaknessFlaw=flaws.length?flaws[flaws.length-1]:'Falha extrema do Caminho da Sombra';
          }else item.magicalCurseWeaknessFlaw=undefined;
        }

        if(item.magicalCurse==='shadow-taint'){
          if(command.active&&!wasActive){
            const amount=item.magicalKind==='magnificent-artefact'?1
              :item.magicalKind==='wondrous-item'?2
              :item.magicalKind==='famous-war-gear'?2*activeQualitiesForItem(item).length:0;
            item.magicalCurseShadowApplied=amount;
            next.attributes.shadow=Math.max(0,Number(next.attributes.shadow)||0)+amount;
          }else if(!command.active&&wasActive){
            next.attributes.shadow=Math.max(0,(Number(next.attributes.shadow)||0)-oldShadow);
            item.magicalCurseShadowApplied=0;
          }
        }
        return normalizeEquipmentMechanics(recomputeShadowState(next));
      });
      break;
    }

"""
cmd=req(cmd,anchor,cases+anchor,"4.9.42 runtime command cases")

# Combat start resets Cleaving.
cmd=cmd.replace(
"""        escapedHeroIds: [],
      };""",
"""        escapedHeroIds: [],
        cleavingReadyByHero: {},
      };""",
1)

# Hero attack: Malice, opening volleys, complications.
cmd=req(
    cmd,
"""      let hopeDice = 0;
      if (command.spendHope) {
        const spent = spendHope(attacker.sheet, 'strength');""",
"""      let hopeDice = 0;
      const preWeapon=attacker.sheet.combat.weapons[Math.max(0,Math.min(attacker.sheet.combat.weapons.length-1,Math.trunc(command.weaponIndex)))]||attacker.sheet.combat.weapons[0];
      if (command.spendHope) {
        if(curseBlocksHopeForWeapon(attacker.sheet,preWeapon)) throw new Error('Malícia: esta arma amaldiçoada impede gastar Esperança no ataque.');
        const spent = spendHope(attacker.sheet, 'strength');""",
    "Malice weapon hope",
)
cmd=req(
    cmd,
"""      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired), { terrain, isNight, escaping });
      const weapon = result.weapon;""",
"""      const darkeningComplication=Math.max(0,-curseDarkeningDicePenalty(freshAttacker.sheet));
      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired), { terrain, isNight, escaping, complicationDice:Math.max(0,Number(nextState.combat.complicationDice)||0)+darkeningComplication });
      const weapon = result.weapon;
      if(nextState.combat.status==='opening-volley'&&result.ranged){
        const countNote=(actor.stateNotes||[]).find(note=>note.startsWith('opening-volley-count:'));
        const used=Math.max(0,Number(countNote?.split(':')[1])||0);
        const surprised=(actor.stateNotes||[]).includes('surprised');
        const limit=surprised?0:(hollowSteelExtraVolley(freshAttacker.sheet,weapon)?2:1);
        if(used>=limit) throw new Error(surprised?'Herói surpreso não pode fazer Saraivada de Abertura.':'A Saraivada de Abertura disponível para este Herói já foi usada.');
      }""",
    "opening volley and complications",
)

# Famous damage/piercing/injury.
cmd=req(
    cmd,
"""        const damage = heroAttackDamage(freshAttacker.sheet, weapon, result.roll, command.special === 'heavy');
        const piercing = result.roll.success && attackIsPiercing(freshAttacker.sheet, weapon, result.roll, command.special === 'pierce');""",
"""        const baseDamage = heroAttackDamage(freshAttacker.sheet, weapon, result.roll, command.special === 'heavy');
        const damage = result.roll.success ? baseDamage + famousDamageBonus(freshAttacker.sheet,weapon,target) : 0;
        const feat=Number(result.roll.featResult||0);
        const famousThreshold=famousPiercingThreshold(freshAttacker.sheet,weapon,target);
        const famousPiercing=Boolean(result.roll.success&&feat!==11&&famousThreshold!==null&&feat>=famousThreshold);
        const piercing = result.roll.success && (attackIsPiercing(freshAttacker.sheet, weapon, result.roll, command.special === 'pierce')||famousPiercing);
        const effectiveInjury=Math.max(0,Number(weapon.injury)||0)+famousInjuryBonus(freshAttacker.sheet,weapon,target);
        const targetEnduranceBefore=Math.max(0,Number(target.endurance)||0);""",
    "famous attack values",
)

# Famous on-hit Hate and Flame of Hope.
cmd=req(
    cmd,
"""        if (result.roll.success && target.actorType !== 'hero') {
          const hateLoss = supplementEnemyHateLossOnSuccessfulAttack(freshAttacker.sheet, Number(result.roll.tengwarCount) || 0);
          if (hateLoss > 0 && Number(target.hate || 0) > 0) {
            target = { ...target, hate: Math.max(0, Number(target.hate || 0) - hateLoss) };
            nextState = upsertCombatActor(nextState, target);
          }
        }""",
"""        if (result.roll.success && target.actorType !== 'hero') {
          const hateLoss = supplementEnemyHateLossOnSuccessfulAttack(freshAttacker.sheet, Number(result.roll.tengwarCount) || 0)
            + famousOnHitHateLoss(freshAttacker.sheet,weapon,target);
          if (hateLoss > 0 && Number(target.hate || 0) > 0) {
            target = { ...target, hate: Math.max(0, Number(target.hate || 0) - hateLoss) };
            nextState = upsertCombatActor(nextState, target);
          }
        }
        if(result.roll.success){
          const recovery=flameOfHopeRecovery(freshAttacker.sheet,weapon,Number(result.roll.tengwarCount)||0);
          if(recovery>0){
            for(const characterId of partyIds) updateHero(characterId,sheet=>adjustEndurance(sheet,recovery));
          }
        }""",
    "famous on-hit effects",
)

# Protection against hero target: effective injury.
cmd=cmd.replace(
"const protection = rollHeroProtection(targetHero.sheet, weapon.injury, result.ranged, protectionDice, protectionMode);",
"const protection = rollHeroProtection(targetHero.sheet, effectiveInjury, result.ranged, protectionDice, protectionMode);",
1)

# Adversary Protection: Foe Slayer and effective injury.
cmd=req(
    cmd,
"""            const ill = supplementProtectionIllFavouredForTarget(freshAttacker.sheet, !result.ranged, piercing);
            const protection = rollActorProtection(target, weapon.injury, ill ? 'ill-favoured' : 'normal');
            nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
            if (!protection.success) nextState = upsertCombatActor(nextState, { ...target, wounded: true, defeated: target.actorType === 'adversary' ? true : target.defeated });""",
"""            const ill = supplementProtectionIllFavouredForTarget(freshAttacker.sheet, !result.ranged, piercing);
            const foe=foeSlayerProtection(freshAttacker.sheet,weapon,target,piercing,ill);
            if(foe.automaticWound){
              nextState = upsertCombatActor(nextState, { ...target, wounded: true, defeated: target.actorType === 'adversary' ? true : target.defeated });
            }else{
              const protection = rollActorProtection(target, effectiveInjury, foe.illFavoured ? 'ill-favoured' : 'normal');
              nextState = recordCombatRoll(nextState, { id: protection.id, actorId: target.actorId, label: 'Proteção', featDie: protection.featResult === 12 ? 'gandalf' : protection.featResult === 11 ? 'eye' : (protection.featResult || 1), successDice: protection.successResults || [], total: protection.total, targetNumber: protection.targetNumber, success: protection.success, tengwar: protection.tengwarCount || 0, createdAt: protection.timestamp });
              if (!protection.success) nextState = upsertCombatActor(nextState, { ...target, wounded: true, defeated: target.actorType === 'adversary' ? true : target.defeated });
            }""",
    "Foe Slayer protection",
)

# Heavy/Cleaving and opening volley consumption after target state refresh.
marker="""        if (command.special === 'defend' && result.roll.success && Number(result.roll.tengwarCount || 0) > 0) {"""
insert="""        target = nextState.combat.actors.find(item => item.actorId === command.targetActorId) || target;
        const actualLoss=Math.min(targetEnduranceBefore,Math.max(0,damage));
        if(result.roll.success&&heavyKnocksDown(freshAttacker.sheet,weapon,target,actualLoss)&&!target.defeated){
          target={...target,stateNotes:Array.from(new Set([...(target.stateNotes||[]),'knocked-down']))};
          nextState=upsertCombatActor(nextState,target);
        }
        if(result.roll.success&&target.defeated&&cleavingEnabled(freshAttacker.sheet,weapon)&&!result.ranged){
          nextState.combat={...nextState.combat,cleavingReadyByHero:{...(nextState.combat.cleavingReadyByHero||{}),[freshAttacker.id]:{weaponIndex:command.weaponIndex,sourceTargetId:target.actorId,createdAt:Date.now()}}};
        }
        if(nextState.combat.status==='opening-volley'&&result.ranged){
          actor=nextState.combat.actors.find(item=>item.actorId===command.attackerId)||actor;
          const prior=(actor.stateNotes||[]).find(note=>note.startsWith('opening-volley-count:'));
          const used=Math.max(0,Number(prior?.split(':')[1])||0);
          nextState=upsertCombatActor(nextState,{...actor,stateNotes:[...(actor.stateNotes||[]).filter(note=>!note.startsWith('opening-volley-count:')),'opening-volley-count:'+String(used+1)]});
        }

"""
cmd=req(cmd,marker,insert+marker,"Heavy Cleaving opening volley")

# Dynamic famous shield parry and Rune-scored shield.
cmd=req(
    cmd,
"""      const parry = Math.max(2, Number(targetHero.sheet.combat.parry) || 2) + supplementParryBonus(targetHero.sheet, { kind:'attack', terrain });
      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', elvenLightWeary, false);""",
"""      const parry = Math.max(2, Number(targetHero.sheet.combat.parry) || 2)
        + supplementParryBonus(targetHero.sheet, { kind:'attack', terrain })
        + famousShieldDynamicParryBonus(targetHero.sheet,attacker);
      const shieldWeary=runeScoredShieldMakesAttackerWeary(targetHero.sheet);
      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', elvenLightWeary||shieldWeary, false);""",
    "famous shield attack defense",
)

# Combat Tasks that exist in this runtime ignore conditions with Rune-scored Helm.
cmd=cmd.replace(
"""const roll = rollOneRing(Math.max(0, Number(skill?.rank) || 0), 0, targetNumberForSkill(hero.sheet, command.skill), command.skill, baseMode, Boolean(hero.sheet.attributes.weary), Boolean(hero.sheet.attributes.miserable));""",
"""const ignoreTaskConditions=runeScoredHelmIgnoresCombatTaskConditions(hero.sheet);
      const roll = rollOneRing(Math.max(0, Number(skill?.rank) || 0), 0, targetNumberForSkill(hero.sheet, command.skill), command.skill, baseMode, ignoreTaskConditions?false:Boolean(hero.sheet.attributes.weary), ignoreTaskConditions?false:Boolean(hero.sheet.attributes.miserable));""",
1)
cmd=cmd.replace(
"""const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca'),'Busca',baseMode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));""",
"""const ignoreTaskConditions=runeScoredHelmIgnoresCombatTaskConditions(hero.sheet);
      const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca'),'Busca',baseMode,ignoreTaskConditions?false:Boolean(hero.sheet.attributes.weary),ignoreTaskConditions?false:Boolean(hero.sheet.attributes.miserable));""",
1)

# Remove curse permanently, including protected Shadow.
old="""    case 'remove_magical_curse': {
      updateHero(command.targetCharacterId,sheet=>{
        const item=sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
        if(!item||!item.magicalCurse) throw new Error('Item amaldiçoado não encontrado.');
        if(!item.magicalCurseActive) throw new Error('A Maldição deste item não está ativa.');
        const next=structuredClone(sheet);
        const found=next.inventory.find(entry=>entry.id===command.inventoryItemId)!;
        found.magicalCurseActive=false;
        found.tags=(found.tags||[]).filter(tag=>!/^curse-attribute:/i.test(tag));
        return normalizeEquipmentMechanics(next);
      });
      break;
    }"""
new="""    case 'remove_magical_curse': {
      updateHero(command.targetCharacterId,sheet=>{
        const item=sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
        if(!item||!item.magicalCurse) throw new Error('Item amaldiçoado não encontrado.');
        const next=structuredClone(sheet);
        const found=next.inventory.find(entry=>entry.id===command.inventoryItemId)!;
        const shadowApplied=Math.max(0,Number(found.magicalCurseShadowApplied)||0);
        if(shadowApplied>0) next.attributes.shadow=Math.max(0,Number(next.attributes.shadow)-shadowApplied);
        found.magicalCurse=undefined;
        found.magicalCurseActive=false;
        found.magicalCurseRevealed=false;
        found.magicalCurseOwnerPresent=false;
        found.magicalCurseHunterTag=undefined;
        found.magicalCurseWeaknessFlaw=undefined;
        found.magicalCurseShadowApplied=0;
        found.magicalCurseTrigger=undefined;
        found.magicalCurseRemoval=undefined;
        found.tags=(found.tags||[]).filter(tag=>!/^curse-attribute:/i.test(tag));
        return normalizeEquipmentMechanics(recomputeShadowState(next));
      });
      break;
    }"""
cmd=req(cmd,old,new,"remove curse cleanup")

write(rel,cmd)
print("PATCH_4942_COMMANDS_OK")
