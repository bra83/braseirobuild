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

# Imports.
cmd=cmd.replace(
    "import { applyFeatOneAsEye, rollOneRing } from '../dice';",
    "import { applyFeatOneAsEye, rollMagicalSuccess, rollOneRing } from '../dice';",
    1,
)
cmd=cmd.replace(
    "import { canonicalWeapon, isRangedWeapon, normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
    "import { automaticInspired, canonicalWeapon, isRangedWeapon, normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
    1,
)
cmd=cmd.replace(
    "import { allowedBanes, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "import { allowedBanes, blessingSource, curseBlocksHopeForSkill, identifiedBlessingCanMagicalSuccess, magicalBlessingDice, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    1,
)
# supplement imports live in a multi-line import; add names if absent.
if "canSpendHopeForSupplementMagicalSuccess" not in cmd:
    cmd=cmd.replace(
        "  applyHuntersResolve,\n",
        "  applyHuntersResolve,\n  canSpendHopeForSupplementMagicalSuccess,\n",
        1,
    )
if "magicalSuccessSource" not in cmd:
    cmd=cmd.replace(
        "  journeyFatigueAfterSupplementReduction,\n",
        "  journeyFatigueAfterSupplementReduction,\n  magicalSuccessSource,\n",
        1,
    )
if "supplementInspired" not in cmd:
    cmd=cmd.replace(
        "  supplementEnemyHateLossOnSuccessfulAttack,\n",
        "  supplementEnemyHateLossOnSuccessfulAttack,\n  supplementInspired,\n",
        1,
    )
cmd=cmd.replace(
    "import { activeQualitiesForItem, actorIsBane, applyIllLuckToResult, cleavingEnabled, curseBlocksHopeForWeapon, curseDarkeningDicePenalty, curseHuntedBy, famousDamageBonus, famousInjuryBonus, famousItemForWeapon, famousOnHitHateLoss, famousPiercingThreshold, famousShieldDynamicParryBonus, flameOfHopeRecovery, foeSlayerProtection, heavyKnocksDown, hollowSteelExtraVolley, luminousAutoAvoidAmbush, runeScoredHelmIgnoresCombatTaskConditions, runeScoredShieldMakesAttackerWeary } from '../rules/magicalGearMechanics';",
    "import { activeQualitiesForItem, actorIsBane, actorIsBane as magicalActorIsBane, applyIllLuckToResult, cleavingEnabled, curseBlocksHopeForWeapon, curseDarkeningDicePenalty, curseHuntedBy, famousDamageBonus, famousInjuryBonus, famousItemForWeapon, famousOnHitHateLoss, famousPiercingThreshold, famousShieldDynamicParryBonus, flameOfHopeRecovery, foeSlayerProtection, heavyKnocksDown, hollowSteelExtraVolley, luminousAutoAvoidAmbush, runeScoredHelmIgnoresCombatTaskConditions, runeScoredShieldMakesAttackerWeary } from '../rules/magicalGearMechanics';",
    1,
)
cmd += "" if "from '../rules/loneLands'" in cmd else "\n"
if "from '../rules/loneLands'" not in cmd:
    import_anchor="import type { CommandAuditEntry, MasterCommand, MasterState, NpcDraft, SceneKind } from './types';"
    cmd=req(cmd,import_anchor,
        "import { actorHasDeadlyAbility, actorIsUndead, ESTELANG, estelangMakesFearful, forodwaithJourneyDiceModifier, forodwaithRollMode, lossothHuntCouncilBonus, weaponIsEstelang } from '../rules/loneLands';\n"+import_anchor,
        "Lone-lands command imports")

# New permanent/scenario commands.
anchor="    case 'player_start_journey': {"
cases=r"""    case 'set_combat_actor_deadly_abilities': {
      const actor=nextState.combat.actors.find(item=>item.actorId===command.actorId);
      if(!actor) throw new Error('Combatente não encontrado.');
      nextState=upsertCombatActor(nextState,{
        ...actor,
        deadlyAbilities:Array.from(new Set((command.deadlyAbilities||[]).map(String).filter(Boolean))),
        traits:Array.from(new Set((command.traits||[]).map(String).filter(Boolean))),
      });
      break;
    }

    case 'set_lone_lands_heir': {
      if(!partyIds.has(command.targetCharacterId)) throw new Error('O Herdeiro precisa fazer parte da Companhia.');
      nextState.company={
        ...nextState.company,
        loneLands:{
          ...(nextState.company.loneLands||{}),
          heirCharacterId:command.targetCharacterId,
          ancestorName:String(command.ancestorName||'').trim()||undefined,
        },
      };
      break;
    }

    case 'claim_estelang': {
      const lone=nextState.company.loneLands||{};
      if(lone.estelangClaimedBy) throw new Error('Estelang já foi reivindicada.');
      if(lone.heirCharacterId && lone.heirCharacterId!==command.targetCharacterId) throw new Error('Estelang deve ser oferecida ao Herdeiro designado desta linhagem.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!partyIds.has(hero.id)) throw new Error('Herói inválido para reivindicar Estelang.');
      const base=canonicalWeapon(ESTELANG.baseItemName);
      if(!base) throw new Error('Espada base de Estelang não encontrada no catálogo canônico.');
      updateHero(hero.id,sheet=>{
        let next=structuredClone(sheet);
        const treasureId='lone_lands_estelang';
        next.inventory=(next.inventory||[]).filter(item=>!(item.tags||[]).includes('estelang'));
        next.inventory.push({
          id:'estelang_'+Date.now(),
          name:ESTELANG.name,
          quantity:1,
          weight:base.load,
          load:base.load,
          category:'Arma',
          description:'Espada Númenoriana ancestral destinada a enfrentar a Sombra da Colina do Medo.',
          equipped:true,
          damage:base.damage,
          injury:base.injury,
          tags:['Tesouro Mágico','estelang','Recompensa:Afiada','BaseItem:'+ESTELANG.baseItemName],
          magicalBaseItemName:ESTELANG.baseItemName,
          magicalKind:'famous-war-gear',
          magicalOrigin:ESTELANG.origin,
          magicalBanes:[...ESTELANG.banes],
          magicalQualities:[...ESTELANG.enchantedQualities],
          magicalQualitiesUnlocked:ESTELANG.enchantedQualities.length,
          magicalIdentified:true,
          treasureIndexEntryId:treasureId,
          destinedCharacterId:hero.id,
          craftsmanship:'Númenoriana',
        } as any);
        next.combat.weapons=(next.combat.weapons||[]).filter(weapon=>weapon.treasureIndexEntryId!==treasureId);
        next.combat.weapons.push({
          name:ESTELANG.name,
          damage:base.damage,
          injury:base.injury,
          group:base.group,
          active:true,
          magicalBaseItemName:ESTELANG.baseItemName,
          treasureIndexEntryId:treasureId,
          magicalQualities:[...ESTELANG.enchantedQualities],
          magicalQualitiesUnlocked:ESTELANG.enchantedQualities.length,
        } as any);
        return normalizeEquipmentMechanics(next);
      });
      nextState.company={
        ...nextState.company,
        loneLands:{
          ...lone,
          heirCharacterId:lone.heirCharacterId||hero.id,
          estelangClaimedBy:hero.id,
          estelangWraithAttraction:true,
        },
      };
      break;
    }

    case 'invoke_heir_oath': {
      if(nextState.activeMode!=='combat') throw new Error('O juramento herdado é assumido em extrema necessidade durante um combate.');
      const lone=nextState.company.loneLands||{};
      if(lone.heirCharacterId!==command.targetCharacterId) throw new Error('Somente o Herdeiro designado pode assumir o juramento ancestral.');
      if(lone.heirOathBound) throw new Error('O Herdeiro já está vinculado ao juramento de destruir a Colina do Medo.');
      const heir=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!heir) throw new Error('Herdeiro não encontrado.');
      nextState.company={...nextState.company,loneLands:{...lone,heirOathBound:true}};
      nextState.combat={
        ...nextState.combat,
        loneLandsOathActive:true,
        actors:nextState.combat.actors.map(actor=>actor.actorType==='hero'
          ? {...actor,stateNotes:Array.from(new Set([...(actor.stateNotes||[]),'lone-lands-oath-hero']))}
          : {...actor,stateNotes:Array.from(new Set([...(actor.stateNotes||[]),'lone-lands-oath-enemy']))}),
        updatedAt:Date.now(),
      };
      nextState=recordCombatNarrative(nextState,(heir.sheet.identity.name||heir.name)+' assumiu o juramento ancestral: todos os Heróis +1d em ataques e Testes de Sombra; inimigos −1d em ataques pelo resto deste combate. O Herdeiro fica obrigado a destruir a Colina do Medo.');
      break;
    }

    case 'mark_hill_shadow_dreamer': {
      const lone=nextState.company.loneLands||{};
      const ids=Array.from(new Set([...(lone.hillDreamerIds||[]),command.targetCharacterId]));
      nextState.company={...nextState.company,loneLands:{...lone,hillDreamerIds:ids}};
      break;
    }

    case 'destroy_hill_of_fear': {
      const lone=nextState.company.loneLands||{};
      nextState.company={...nextState.company,loneLands:{...lone,hillDestroyed:true,hillDreamerIds:[],heirOathBound:false,estelangWraithAttraction:false}};
      break;
    }

    case 'undertake_hunt_lossoth': {
      if(nextState.fellowshipPhase.status!=='active') throw new Error('Caça com os Lossoth é um Compromisso de Sociedade.');
      const count=Math.max(0,Number(nextState.fellowshipPhase.undertakingCountByHero?.[command.targetCharacterId])||0);
      if(count>=1) throw new Error('Este Herói já realizou seu Compromisso normal nesta Fase de Sociedade.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero) throw new Error('Herói não encontrado.');
      nextState.company={...nextState.company,loneLands:{...(nextState.company.loneLands||{}),lossothSafeRefugeNextAdventure:true,lossothHuntCompleted:true}};
      nextState.fellowshipPhase.undertakings={...(nextState.fellowshipPhase.undertakings||{}),[hero.id]:'narrative'};
      nextState.fellowshipPhase.undertakingCountByHero={...(nextState.fellowshipPhase.undertakingCountByHero||{}),[hero.id]:count+1};
      break;
    }

    case 'undertake_stone_unhappy': {
      if(nextState.fellowshipPhase.status!=='active') throw new Error('Viagem à Pedra dos Infelizes é um Compromisso de Sociedade.');
      const count=Math.max(0,Number(nextState.fellowshipPhase.undertakingCountByHero?.[command.targetCharacterId])||0);
      if(count>=1) throw new Error('Este Herói já realizou seu Compromisso normal nesta Fase de Sociedade.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero) throw new Error('Herói não encontrado.');
      const scars=Math.max(0,Number((hero.sheet.attributes as any).shadowScars)||0);
      updateHero(hero.id,sheet=>adjustHope(sheet,scars));
      nextState.fellowshipPhase.undertakings={...(nextState.fellowshipPhase.undertakings||{}),[hero.id]:'narrative'};
      nextState.fellowshipPhase.undertakingCountByHero={...(nextState.fellowshipPhase.undertakingCountByHero||{}),[hero.id]:count+1};
      break;
    }

    case 'resolve_lossoth_safe_refuge_rest': {
      if(!nextState.company.loneLands?.lossothSafeRefugeNextAdventure) throw new Error('O acampamento de verão Lossoth ainda não é um refúgio seguro desta Fase de Aventuras.');
      const nested=applyOne(
        {type:'resolve_rest',kind:'long',safeRefuge:true,reason:'Descanso Prolongado no acampamento de verão Lossoth, considerado refúgio seguro por Caça com os Lossoth.'} as MasterCommand,
        nextCampaign,nextCharacters,nextNpcs,nextState,partyIds,
      );
      nextCharacters=nested.characters;nextNpcs=nested.npcs;nextState=nested.masterState;
      break;
    }

    case 'prepare_forodwaith_journey': {
      if(['travelling','paused'].includes(nextState.journey.status)) throw new Error('Configure Forodwaith antes de iniciar a travessia.');
      nextState=setDangerousArea(nextState,3);
      nextState.journey={
        ...nextState.journey,
        loneLandsForodwaith:{
          active:true,
          lossothGear:Boolean(command.lossothGear),
          lossothGuides:Boolean(command.lossothGuides),
          useSpecialTable:Boolean(command.useSpecialTable),
          stormSpirits:false,
          specialEventsResolved:0,
        },
      };
      break;
    }

    case 'set_forodwaith_storm_spirits': {
      const state=nextState.journey.loneLandsForodwaith;
      if(!state?.active) throw new Error('A Companhia não está usando a travessia especial de Forodwaith.');
      nextState.journey={...nextState.journey,loneLandsForodwaith:{...state,stormSpirits:Boolean(command.active)}};
      break;
    }

    case 'start_lone_lands_dwarven_council': {
      nextState=startCouncil(nextState,'Conselho sob as Montanhas',1,command.participants,command.npcIds);
      nextState.council={
        ...nextState.council,
        sequenceVariant:'lone-lands-introduction-first',
        resistancePending:true,
        resistance:0,
        tolerance:0,
        timeLimit:0,
        rollDiceModifier:0,
      };
      break;
    }

    case 'set_lone_lands_council_request': {
      if(nextState.activeMode!=='council'||nextState.council.sequenceVariant!=='lone-lands-introduction-first') throw new Error('Este comando pertence ao Conselho sob as Montanhas.');
      if(nextState.council.status==='introduction') throw new Error('Resolva primeiro a Introdução; só depois a Companhia decide o pedido.');
      const resistance=Math.max(1,Math.trunc(Number(command.resistance)||1));
      const icons=nextState.council.introductionSucceeded?Math.max(0,Number(nextState.council.introductionSuccessIcons)||0):0;
      nextState.council={
        ...nextState.council,
        objective:command.objective,
        tolerance:resistance,
        resistance,
        timeLimit:resistance+icons,
        resistancePending:false,
      };
      break;
    }

    case 'start_egel_council': {
      const resistance=command.request==='reasonable'?3:command.request==='bold'?6:9;
      const hunted=lossothHuntCouncilBonus(nextState)>0;
      const modifier=(command.relation==='friendly'||hunted)?1:command.relation==='jagat-vouches'?0:-1;
      nextState=startCouncil(nextState,'Conselho com Egel',resistance,command.participants,command.npcIds);
      nextState.council={...nextState.council,egelCouncil:true,rollDiceModifier:modifier};
      break;
    }

    case 'resolve_combat_task': {
      if(nextState.activeMode!=='combat') throw new Error('Tarefas de Combate exigem combate ativo.');
      const hero=nextCharacters.find(character=>character.id===command.actorCharacterId);
      let actor=nextState.combat.actors.find(item=>item.actorId===command.actorCharacterId);
      if(!hero||!actor||actor.actorType!=='hero') throw new Error('Herói da Tarefa de Combate não encontrado.');
      if((actor.stateNotes||[]).some(note=>note.startsWith('temp-round:main-action-used:'))) throw new Error('A ação principal deste Herói já foi usada nesta rodada.');
      const task=command.task;
      const skillName=task==='intimidate'?'Fascínio':task==='inspire'?'Indução':'Batalha';
      const requiredStance=task==='intimidate'?'forward':task==='inspire'?'open':'defensive';
      if(actor.stance!==requiredStance) throw new Error(
        task==='intimidate'?'Intimidar Inimigo exige Postura Avançada.':
        task==='inspire'?'Inspirar Camaradas exige Postura Aberta.':'Proteger Companheiro exige Postura Defensiva.'
      );
      if(command.spendHope&&command.useMagicalSuccess) throw new Error('Escolha entre o bônus normal de Esperança e Sucesso Mágico.');
      if((command.spendHope||command.useMagicalSuccess)&&curseBlocksHopeForSkill(hero.sheet,skillName)) throw new Error('Malícia impede gastar Esperança nesta Tarefa.');
      const skill=findSkill(hero.sheet,skillName);
      const context={kind:'skill' as const,skill:skillName,terrain:'',isNight:false};
      const supplementDice=supplementBonusDice(hero.sheet,context);
      const blessingDice=magicalBlessingDice(hero.sheet,skillName);
      const darkening=curseDarkeningDicePenalty(hero.sheet);
      const automaticallyInspired=automaticInspired(hero.sheet)||supplementInspired(hero.sheet,context);
      const hopeDice=command.spendHope?(command.inspired||automaticallyInspired?2:1):0;
      if(command.spendHope){
        updateHero(hero.id,sheet=>{
          const spent=spendHope(sheet,attributeForSkill(skillName));
          if(!spent.allowed) throw new Error('Esperança insuficiente.');
          return spent.sheet;
        });
      }
      const refreshed=nextCharacters.find(character=>character.id===hero.id)!;
      const baseMode=shadowAtLimit(refreshed.sheet)?'ill-favoured':skill?.favorite?'favoured':'normal';
      const mode=supplementRollMode(refreshed.sheet,context,baseMode);
      const ignoreConditions=runeScoredHelmIgnoresCombatTaskConditions(refreshed.sheet);
      const count=Math.max(0,Number(skill?.rank)||0)+supplementDice+blessingDice+hopeDice+darkening;
      let roll;
      if(command.useMagicalSuccess){
        if(refreshed.sheet.attributes.hopeCurrent<=0) throw new Error('Esperança insuficiente.');
        const source=magicalSuccessSource(refreshed.sheet,context)||blessingSource(refreshed.sheet,skillName);
        const allowed=canSpendHopeForSupplementMagicalSuccess(refreshed.sheet,context)||identifiedBlessingCanMagicalSuccess(refreshed.sheet,skillName);
        if(!source||!allowed) throw new Error('Sucesso Mágico indisponível para esta Tarefa.');
        updateHero(refreshed.id,sheet=>adjustHope(sheet,-1));
        roll=rollMagicalSuccess(count,skillName,source,ignoreConditions?false:Boolean(refreshed.sheet.attributes.weary));
      }else{
        roll=rollOneRing(count,0,targetNumberForSkill(refreshed.sheet,skillName),skillName,mode,ignoreConditions?false:Boolean(refreshed.sheet.attributes.weary),ignoreConditions?false:Boolean(refreshed.sheet.attributes.miserable));
      }
      roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(refreshed.sheet));
      roll=applyIllLuckToResult(refreshed.sheet,roll);
      nextState=recordCombatRoll(nextState,{
        id:roll.id,actorId:refreshed.id,label:
          task==='intimidate'?'Intimidar Inimigo · Fascínio':
          task==='inspire'?'Inspirar Camaradas · Indução':'Proteger Companheiro · Batalha',
        featDie:roll.featResult===12?'gandalf':roll.featResult===11?'eye':(roll.featResult||1),
        successDice:roll.successResults||[],total:roll.total,targetNumber:roll.targetNumber,
        success:roll.success,tengwar:roll.tengwarCount||0,createdAt:roll.timestamp,
      });
      actor=nextState.combat.actors.find(item=>item.actorId===refreshed.id)||actor;
      nextState=upsertCombatActor(nextState,{...actor,stateNotes:[...(actor.stateNotes||[]),'temp-round:main-action-used:combat-task:'+task]});
      if(!roll.success) break;
      const icons=Math.max(0,Number(roll.tengwarCount)||0);

      if(task==='intimidate'){
        const maxPower=icons>=2?Number.POSITIVE_INFINITY:icons===1?2:1;
        nextState.combat={
          ...nextState.combat,
          actors:nextState.combat.actors.map(enemy=>{
            if(enemy.actorType==='hero'||enemy.defeated||Math.max(1,Number(enemy.power)||1)>maxPower) return enemy;
            const estelangFearful=(enemy.stateNotes||[]).includes('estelang-fearful');
            if(actorHasDeadlyAbility(enemy,'Desumano')&&!estelangFearful&&!roll.magicalSuccess) return enemy;
            const nextEnemy={...enemy,stateNotes:Array.from(new Set([...(enemy.stateNotes||[]),'combat-task-weary-next-attack']))};
            if(estelangFearful&&Number(nextEnemy.hate||0)>0) nextEnemy.hate=Math.max(0,Number(nextEnemy.hate||0)-1);
            return nextEnemy;
          }),
          updatedAt:Date.now(),
        };
      }else if(task==='inspire'){
        const round=Math.max(1,Number(nextState.combat.round)||1);
        if(Number(nextState.combat.combatTaskInspiredUsedRound)===round) throw new Error('Apenas um Herói pode escolher Inspirar Camaradas em cada rodada.');
        nextState.combat={
          ...nextState.combat,
          combatTaskInspiredUsedRound:round,
          actors:nextState.combat.actors.map(ally=>{
            if(ally.actorType!=='hero'||ally.defeated) return ally;
            const eligible=icons>=2?ally.stance!=='rearward':icons===1?['forward','open'].includes(String(ally.stance)):ally.stance==='forward';
            return eligible?{...ally,stateNotes:Array.from(new Set([...(ally.stateNotes||[]),'combat-task-inspired-next-attack:1']))}:ally;
          }),
          updatedAt:Date.now(),
        };
      }else{
        const protectedId=command.protectedCharacterId;
        if(!protectedId||protectedId===hero.id) throw new Error('Proteger Companheiro exige outro Herói.');
        const protectedActor=nextState.combat.actors.find(item=>item.actorId===protectedId);
        if(!protectedActor||protectedActor.actorType!=='hero'||protectedActor.stance==='rearward') throw new Error('O companheiro protegido precisa estar em combate corpo a corpo.');
        const penalty=1+icons;
        nextState=upsertCombatActor(nextState,{
          ...protectedActor,
          stateNotes:[
            ...(protectedActor.stateNotes||[]).filter(note=>!note.startsWith('combat-task-protected-next-attack:')),
            'combat-task-protected-next-attack:'+String(penalty),
          ],
        });
      }
      break;
    }

"""
cmd=req(cmd,anchor,cases+anchor,"4.9.43 Lone-lands commands")

# Main-action task markers block ordinary hero attacks.
cmd=req(cmd,
"""    case 'resolve_hero_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const attacker = nextCharacters.find(character => character.id === command.attackerId);""",
"""    case 'resolve_hero_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const preActor=nextState.combat.actors.find(item=>item.actorId===command.attackerId);
      if((preActor?.stateNotes||[]).some(note=>note.startsWith('temp-round:main-action-used:'))) throw new Error('A ação principal deste Herói já foi usada nesta rodada.');
      const attacker = nextCharacters.find(character => character.id === command.attackerId);""",
"combat task consumes Hero main action")

# Estelang transforms Undead while it is wielded against them.
cmd=req(cmd,
"""      const weapon = result.weapon;
      const light=nextState.combat.elvenLight;""",
"""      const weapon = result.weapon;
      if(estelangMakesFearful(freshAttacker.sheet,weapon.name,target)){
        target={
          ...target,
          deadlyAbilities:(target.deadlyAbilities||[]).filter(name=>!['desumano','inhuman'].includes(String(name).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase())),
          traits:Array.from(new Set([...(target.traits||[]),'Medroso'])),
          stateNotes:Array.from(new Set([...(target.stateNotes||[]),'estelang-fearful'])),
        };
        nextState=upsertCombatActor(nextState,target);
      }
      const light=nextState.combat.elvenLight;""",
"Estelang undead property")

# Consume Inspire after the next Hero attack.
cmd=req(cmd,
"""        nextState = upsertCombatActor(nextState, { ...actor, stateNotes: (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:') && !(result.ranged && note.startsWith('prepared-shot:'))) });""",
"""        nextState = upsertCombatActor(nextState, { ...actor, stateNotes: (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:') && !note.startsWith('combat-task-inspired-next-attack:') && !(result.ranged && note.startsWith('prepared-shot:'))) });""",
"consume Inspire task")

# Enemy attack: oath -1d, Protect penalty, Intimidate Weary; consume one-use notes.
cmd=req(cmd,
"""      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0));
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;""",
"""      const protectedNote=(targetActor.stateNotes||[]).find(note=>note.startsWith('combat-task-protected-next-attack:'));
      const protectPenalty=Math.max(0,Number(protectedNote?.split(':')[1])||0);
      const intimidatedWeary=(attacker.stateNotes||[]).includes('combat-task-weary-next-attack');
      const oathPenalty=(attacker.stateNotes||[]).includes('lone-lands-oath-enemy')?1:0;
      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0)-protectPenalty-oathPenalty);
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;""",
"Protect Intimidate oath attack effects")
cmd=req(cmd,
"""      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', elvenLightWeary||shieldWeary, false);""",
"""      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', elvenLightWeary||shieldWeary||intimidatedWeary, false);
      nextState=upsertCombatActor(nextState,{...attacker,stateNotes:(attacker.stateNotes||[]).filter(note=>note!=='combat-task-weary-next-attack')});
      if(protectedNote) nextState=upsertCombatActor(nextState,{...targetActor,stateNotes:(targetActor.stateNotes||[]).filter(note=>!note.startsWith('combat-task-protected-next-attack:'))});""",
"consume Intimidate Protect")
# Standing up consumes enemy action too.
cmd=req(cmd,
"""    case 'resolve_adversary_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);""",
"""    case 'resolve_adversary_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      if((attacker?.stateNotes||[]).some(note=>note.startsWith('temp-round:main-action-used:'))) throw new Error('A ação principal deste adversário já foi usada nesta rodada.');""",
"stand-up blocks adversary attack")

# Special Forodwaith table: every resolved Event adds 2 Fatigue to everyone.
marker="""      if (shadowEyeGain > 0) raiseEyeAwareness(shadowEyeGain, '+' + String(shadowEyeGain) + ' por Sombra ganha no Evento de Jornada.');

      nextState.journey = {"""
insert="""      if(nextState.journey.loneLandsForodwaith?.active&&nextState.journey.loneLandsForodwaith.useSpecialTable){
        for(const characterId of partyIds){
          const hero=nextCharacters.find(character=>character.id===characterId);
          if(hero&&!rangerIgnoresJourneyFatigue(hero.sheet)){
            const cramReduction=/\\bcram\\b/i.test(hero.sheet.other.virtues||'')?1:0;
            pendingFatigue[characterId]=Math.max(0,Number(pendingFatigue[characterId])||0)+Math.max(0,2-cramReduction);
          }
        }
        nextState.journey={
          ...nextState.journey,
          loneLandsForodwaith:{
            ...nextState.journey.loneLandsForodwaith,
            specialEventsResolved:Math.max(0,Number(nextState.journey.loneLandsForodwaith.specialEventsResolved)||0)+1,
          },
        };
      }

"""
cmd=req(cmd,marker,marker.replace("\n      nextState.journey = {","\n")+insert+"      nextState.journey = {","Forodwaith special table fatigue")

# Clear Forodwaith at end of Journey, preserving other end-state fields.
cmd=cmd.replace(
"herbalRecoveryByHero: {}, pendingRoll: null",
"herbalRecoveryByHero: {}, loneLandsForodwaith: null, pendingRoll: null",
1,
)

# At the start of a later Fellowship phase, the "next Adventure Phase" Lossoth refuge benefit expires.
cmd=req(cmd,
"""    case 'start_fellowship_phase':
      nextState = startFellowshipPhase(nextState, command.locationId);""",
"""    case 'start_fellowship_phase':
      nextState = startFellowshipPhase(nextState, command.locationId);
      if(nextState.company.loneLands?.lossothSafeRefugeNextAdventure){
        nextState.company={...nextState.company,loneLands:{...nextState.company.loneLands,lossothSafeRefugeNextAdventure:false}};
      }""",
"Lossoth next-Adventure expiry")

# Oath attack effects last only through this combat.
cmd=req(cmd,
"""      nextState = endCombat(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };""",
"""      nextState = endCombat(nextState);
      nextState.combat={
        ...nextState.combat,
        loneLandsOathActive:false,
        actors:nextState.combat.actors.map(actor=>({...actor,stateNotes:(actor.stateNotes||[]).filter(note=>!['lone-lands-oath-hero','lone-lands-oath-enemy'].includes(note))})),
      };
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };""",
"end inherited oath combat bonus")

write(rel,cmd)
print("PATCH_4943_COMMANDS_OK")
