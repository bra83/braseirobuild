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
cmd=req(cmd,
"import { normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
"import { isRangedWeapon, normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
"ranged import")
cmd=req(cmd,
"""  effectiveRestKind,
  gainGroundBonusDice,
  hasVirtue,""",
"""  effectiveRestKind,
  gainGroundBonusDice,
  deadlyArcheryMakesPrepareShotSecondary,
  elvenLightsTargetPower,
  hasVirtue,
  houndAvailable,""",
"stateful supplement imports")

# Artífice de Eregion: acquiring the Virtue arms one craft for a later Yule.
cmd=req(
    cmd,
    """        if(!hadHoney && afterHero && hasVirtue(afterHero.sheet,'Bolos de Mel Duas Vezes Assados')){
          nextState.company={...nextState.company,fellowshipMax:nextState.company.fellowshipMax+1,fellowshipCurrent:nextState.company.fellowshipCurrent+1};
        }
      }
      nextState.fellowshipPhase.valorWisdomAdvance""",
    """        if(!hadHoney && afterHero && hasVirtue(afterHero.sheet,'Bolos de Mel Duas Vezes Assados')){
          nextState.company={...nextState.company,fellowshipMax:nextState.company.fellowshipMax+1,fellowshipCurrent:nextState.company.fellowshipCurrent+1};
        }
        if(beforeHero && !hasVirtue(beforeHero.sheet,'Artífice de Eregion') && afterHero && hasVirtue(afterHero.sheet,'Artífice de Eregion')){
          updateHero(afterHero.id,sheet=>{
            const next=structuredClone(sheet);
            next.other.supplementState={...(next.other.supplementState||{}),artificerYuleReady:true,artificerAcquiredPhaseStartedAt:Number(nextState.fellowshipPhase.startedAt)||Date.now()};
            return next;
          });
        }
      }
      nextState.fellowshipPhase.valorWisdomAdvance""",
    "arm Artificer next Yule",
)

# Player-authoritative stateful virtue commands.
anchor="    case 'player_start_journey': {"
cases=r"""    case 'set_combat_actor_power': {
      const actor=nextState.combat.actors.find(item=>item.actorId===command.actorId);
      if(!actor) throw new Error('Combatente não encontrado.');
      nextState=upsertCombatActor(nextState,{...actor,power:Math.max(1,Math.trunc(Number(command.power)||1))});
      break;
    }

    case 'prepare_shot': {
      if(nextState.activeMode!=='combat') throw new Error('Preparar Tiro exige combate ativo.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      let actor=nextState.combat.actors.find(item=>item.actorId===command.targetCharacterId);
      if(!hero||!actor||actor.actorType!=='hero'||actor.stance!=='rearward') throw new Error('Preparar Tiro exige um Herói na Postura de Retaguarda.');
      const weapon=hero.sheet.combat.weapons[Math.max(0,Math.min(hero.sheet.combat.weapons.length-1,Math.trunc(command.weaponIndex)))]||hero.sheet.combat.weapons[0];
      if(!weapon||!isRangedWeapon(weapon.name,weapon.group)) throw new Error('Preparar Tiro exige uma arma à distância.');
      const skill=findSkill(hero.sheet,'Busca');
      const baseMode=hero.sheet.attributes.shadow>=hero.sheet.attributes.hopeMax?'ill-favoured':skill?.favorite?'favoured':'normal';
      const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,'Busca'),'Busca',baseMode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));
      nextState=recordCombatRoll(nextState,{id:roll.id,actorId:hero.id,label:'Preparar Tiro · Busca',featDie:roll.featResult===12?'gandalf':roll.featResult===11?'eye':(roll.featResult||1),successDice:roll.successResults||[],total:roll.total,targetNumber:roll.targetNumber,success:roll.success,tengwar:roll.tengwarCount||0,createdAt:roll.timestamp});
      const clean=(actor.stateNotes||[]).filter(note=>!note.startsWith('prepared-shot:')&&!note.startsWith('temp-round:main-action-used:prepare-shot'));
      const bonus=roll.success?1+Math.max(0,Number(roll.tengwarCount)||0):0;
      const secondary=deadlyArcheryMakesPrepareShotSecondary(hero.sheet,actor.stance,weapon.name);
      actor={...actor,stateNotes:[
        ...clean,
        ...(bonus>0?['prepared-shot:'+String(bonus)]:[]),
        ...(!secondary?['temp-round:main-action-used:prepare-shot']:[]),
      ]};
      nextState=upsertCombatActor(nextState,actor);
      break;
    }

    case 'activate_elven_lights': {
      if(nextState.activeMode!=='combat') throw new Error('Luzes Élficas precisam de uma cena de combate para selecionar observadores.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Luzes Élficas')) throw new Error('O Herói não possui Luzes Élficas.');
      if(hero.sheet.attributes.hopeCurrent<=0) throw new Error('Luzes Élficas exige 1 Esperança.');
      const targets=Array.from(new Set(command.targetActorIds||[])).map(id=>nextState.combat.actors.find(actor=>actor.actorId===id)).filter(Boolean);
      if(!targets.length) throw new Error('Selecione ao menos uma criatura que esteja observando a luz.');
      let totalPower=0;
      for(const actor of targets){
        if(actor!.actorType==='hero') throw new Error('Luzes Élficas seleciona Mortais/criaturas humanoides observadoras, não os Heróis da Companhia.');
        const power=elvenLightsTargetPower(actor!);
        if(power===null) throw new Error('Configure o Poder do combatente '+String(actor!.quickSheet?.name||actor!.actorId)+' antes de usar Luzes Élficas.');
        totalPower+=power;
      }
      if(totalPower>Math.max(0,Number(hero.sheet.attributes.wisdom)||0)) throw new Error('O Poder total dos observadores selecionados excede a Sabedoria do Herói.');
      updateHero(hero.id,sheet=>adjustHope(sheet,-1));
      nextState.combat={
        ...nextState.combat,
        elvenLight:{ownerCharacterId:hero.id,targetActorIds:targets.map(actor=>actor!.actorId),active:true,triggered:false,createdAt:Date.now()},
        actors:nextState.combat.actors.map(actor=>targets.some(target=>target!.actorId===actor.actorId)?{...actor,stateNotes:[...(actor.stateNotes||[]),'elven-light-attracted:'+hero.id]}:actor),
      };
      if(nextState.company.eyeAwarenessEnabled) raiseEyeAwareness(1,'+1 por exibição de magia menor: Luzes Élficas.');
      break;
    }

    case 'extinguish_elven_lights': {
      const light=nextState.combat.elvenLight;
      if(!light||light.ownerCharacterId!==command.targetCharacterId) throw new Error('Não há Luzes Élficas ativas deste Herói.');
      nextState.combat={
        ...nextState.combat,
        elvenLight:{...light,active:false},
        actors:nextState.combat.actors.map(actor=>({...actor,stateNotes:(actor.stateNotes||[]).filter(note=>!note.startsWith('elven-light-attracted:'))})),
      };
      break;
    }

    case 'resolve_hound_piercing_choice': {
      const pending=nextState.combat.pendingHoundPiercing;
      if(!pending||pending.targetCharacterId!==command.targetCharacterId) throw new Error('Nenhum Golpe Perfurante aguarda a decisão do Cão da Floresta.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Cão da Floresta das Trevas')) throw new Error('O Herói não possui Cão da Floresta das Trevas.');
      if(command.choice==='hound'){
        if(!houndAvailable(hero.sheet)) throw new Error('O Cão da Floresta já está ferido ou indisponível.');
        updateHero(hero.id,sheet=>{
          const next=structuredClone(sheet);
          next.other.supplementState={...(next.other.supplementState||{}),houndWounded:true,houndRecoveryPending:true,houndRecoveryFailed:false};
          return next;
        });
        const actor=nextState.combat.actors.find(item=>item.actorId===hero.id);
        if(actor) nextState=upsertCombatActor(nextState,{...actor,stateNotes:[...(actor.stateNotes||[]),'hound-wounded']});
        nextState=recordCombatNarrative(nextState,'Cão da Floresta das Trevas: o cão recebeu o Ferimento e cancelou o Golpe Perfurante contra '+(hero.sheet.identity.name||hero.name)+'.');
      }else{
        updateHero(hero.id,sheet=>applyHeroWound(sheet).sheet);
      }
      nextState.combat={...nextState.combat,pendingHoundPiercing:null};
      break;
    }

    case 'resolve_hound_recovery': {
      if(nextState.activeMode==='combat') throw new Error('A recuperação do Cão é verificada no início de uma sessão, fora do combate.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Cão da Floresta das Trevas')) throw new Error('O Herói não possui Cão da Floresta das Trevas.');
      const state=hero.sheet.other.supplementState||{};
      if(!state.houndWounded||!state.houndRecoveryPending) throw new Error('O Cão não está aguardando recuperação.');
      if(state.houndRecoveryFailed) throw new Error('A tentativa desta sessão já falhou; o Cão retorna apenas na próxima Fase de Aventura.');
      const skill=findSkill(hero.sheet,'Cura');
      const extra=supplementBonusDice(hero.sheet,{kind:'first-aid',skill:'Cura'});
      const mode=hero.sheet.attributes.shadow>=hero.sheet.attributes.hopeMax?'ill-favoured':skill?.favorite?'favoured':'normal';
      const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0)+extra,0,targetNumberForSkill(hero.sheet,'Cura'),'Cura',mode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));
      updateHero(hero.id,sheet=>{
        const next=structuredClone(sheet);
        next.other.supplementState={...(next.other.supplementState||{}),
          houndWounded:roll.success?false:true,
          houndRecoveryPending:roll.success?false:true,
          houndRecoveryFailed:roll.success?false:true,
        };
        return next;
      });
      break;
    }

    case 'record_journey_herbal_roll': {
      if(!nextState.journey.awaitingEndRoll) throw new Error('Remédios Herbais reduz Fadiga acumulada no encerramento da Jornada.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Remédios Herbais')) throw new Error('O Herói não possui Remédios Herbais.');
      const accumulated=Math.max(0,Number(nextState.journey.travelFatigueByHero?.[hero.id])||0);
      if(accumulated<=0) throw new Error('Não há Fadiga de Jornada acumulada para reduzir.');
      const skill=findSkill(hero.sheet,'Cura');
      const extra=supplementBonusDice(hero.sheet,{kind:'journey',skill:'Cura'});
      const mode=hero.sheet.attributes.shadow>=hero.sheet.attributes.hopeMax?'ill-favoured':skill?.favorite?'favoured':'normal';
      const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0)+extra,0,targetNumberForSkill(hero.sheet,'Cura'),'Cura',mode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));
      nextState.journey={...nextState.journey,herbalRecoveryByHero:{...(nextState.journey.herbalRecoveryByHero||{}),[hero.id]:{success:Boolean(roll.success),successIcons:Math.max(0,Number(roll.tengwarCount)||0),roll,createdAt:Date.now()}}};
      break;
    }

    case 'set_council_spokesperson': {
      if(nextState.activeMode!=='council'||!['introduction','interaction'].includes(nextState.council.status)) throw new Error('Não há Conselho ativo.');
      if(!(nextState.council.participants||[]).includes(command.targetCharacterId)) throw new Error('O porta-voz deve participar do Conselho.');
      nextState.council={...nextState.council,spokespersonId:command.targetCharacterId};
      break;
    }

    case 'apply_beauty_of_stars': {
      if(nextState.activeMode!=='council'||!['introduction','interaction'].includes(nextState.council.status)) throw new Error('Beleza das Estrelas exige Conselho ativo.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Beleza das Estrelas')) throw new Error('O Herói não possui Beleza das Estrelas.');
      if(nextState.council.spokespersonId!==hero.id) throw new Error('Beleza das Estrelas exige que o Herói seja o porta-voz.');
      if(command.choice==='time-limit'){
        if((nextState.council.timeLimitBonus||0)>=1) throw new Error('O bônus de Beleza das Estrelas já foi aplicado neste Conselho.');
        nextState.council={...nextState.council,timeLimit:Math.max(1,Number(nextState.council.timeLimit)||1)+1,timeLimitBonus:1};
      }else{
        if(nextState.council.memoryVeil) throw new Error('O véu de memória já foi aplicado neste Conselho.');
        nextState.council={...nextState.council,memoryVeil:true};
        const mortalNpcIds=new Set(nextState.council.interlocutorNpcIds||[]);
        const memoryDay=Math.max(1,Number(nextState.worldClock.day)||1);
        nextNpcs=nextNpcs.map(npc=>{
          if(!mortalNpcIds.has(npc.id)||/elf|elfo/i.test(String(npc.culture||'')+' '+String(npc.peoples||''))) return npc;
          const vague='O Conselho mais recente ficou apenas como uma lembrança vaga; detalhes do que foi dito ou acordado não podem ser recordados com precisão.';
          return {
            ...npc,
            memorySummary:[npc.memorySummary||'',vague].filter(Boolean).join(' '),
            memoryEvents:[...(npc.memoryEvents||[]),{day:memoryDay,summary:vague,createdAt:Date.now()}].slice(-80),
            updatedAt:Date.now(),
          };
        });
        if(nextState.company.eyeAwarenessEnabled) raiseEyeAwareness(1,'+1 por magia menor: Beleza das Estrelas ocultou as memórias dos Mortais do Conselho.');
      }
      break;
    }

    case 'artificer_study_item': {
      if(nextState.fellowshipPhase.status!=='active') throw new Error('Artífice de Eregion estuda Itens Mágicos durante a Fase de Sociedade.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Artífice de Eregion')) throw new Error('O Herói não possui Artífice de Eregion.');
      const used=Number(hero.sheet.other.supplementState?.artificerStudyUsedPhaseStartedAt)||0;
      if(used>0 && used===Number(nextState.fellowshipPhase.startedAt||0)) throw new Error('Artífice de Eregion já estudou um item nesta Fase de Sociedade.');
      const owner=nextCharacters.find(character=>character.sheet.inventory.some(item=>item.id===command.inventoryItemId));
      const item=owner?.sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
      if(!owner||!item||!item.magicalKind) throw new Error('Item mágico da Companhia não encontrado.');
      if(nextState.company.elrondItemsIdentified){
        updateHero(owner.id,sheet=>({...sheet,inventory:sheet.inventory.map(entry=>entry.id===item.id?{...entry,magicalIdentified:true}:entry)}));
      }else{
        const skill=findSkill(hero.sheet,command.skill);
        const mode=hero.sheet.attributes.shadow>=hero.sheet.attributes.hopeMax?'ill-favoured':skill?.favorite?'favoured':'normal';
        const roll=rollOneRing(Math.max(0,Number(skill?.rank)||0),0,targetNumberForSkill(hero.sheet,command.skill),command.skill,mode,Boolean(hero.sheet.attributes.weary),Boolean(hero.sheet.attributes.miserable));
        if(roll.success) updateHero(owner.id,sheet=>({...sheet,inventory:sheet.inventory.map(entry=>entry.id===item.id?{...entry,magicalIdentified:true}:entry)}));
      }
      updateHero(hero.id,sheet=>{const next=structuredClone(sheet);next.other.supplementState={...(next.other.supplementState||{}),artificerStudyUsedPhaseStartedAt:Number(nextState.fellowshipPhase.startedAt)||Date.now()};return next;});
      break;
    }

    case 'artificer_yule_craft': {
      if(nextState.fellowshipPhase.status!=='active'||!nextState.fellowshipPhase.isYule) throw new Error('Artífice de Eregion cria seu item durante Yule.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!hasVirtue(hero.sheet,'Artífice de Eregion')) throw new Error('O Herói não possui Artífice de Eregion.');
      if(!hero.sheet.other.supplementState?.artificerYuleReady) throw new Error('A criação única de Artífice de Eregion já foi usada ou ainda não foi preparada.');
      if(Number(hero.sheet.other.supplementState?.artificerAcquiredPhaseStartedAt||0)===Number(nextState.fellowshipPhase.startedAt||0)) throw new Error('Artífice de Eregion só pode criar no próximo Yule, não na Fase em que a Virtude foi adquirida.');
      if(Number(hero.sheet.other.supplementState?.artificerUsedYuleYear)===Number(nextState.worldClock.year)) throw new Error('Artífice de Eregion já foi usado neste Yule.');
      if(command.kind==='enchanted-reward'){
        const item=hero.sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
        if(!item||!/(weapon|arma|sword|spear|axe|bow|espada|lança|machado|arco)/i.test(item.category+' '+item.name)) throw new Error('Selecione uma arma do Herói para receber a Recompensa Encantada.');
        updateHero(hero.id,sheet=>({...sheet,inventory:sheet.inventory.map(entry=>entry.id===item.id?{...entry,magicalKind:'enchanted-reward' as const,magicalIdentified:true,craftsmanship:'Élfica',magicalQualities:entry.magicalQualities||[]}:entry),other:{...sheet.other,supplementState:{...(sheet.other.supplementState||{}),artificerUsedYuleYear:Number(nextState.worldClock.year),artificerYuleReady:false}}}));
      }else{
        const name=(command.name||'Artefato Maravilhoso de Eregion').trim();
        updateHero(hero.id,sheet=>({...sheet,inventory:[...sheet.inventory,{id:'artificer_'+Date.now(),name,quantity:1,weight:0,category:'Artefato Maravilhoso',description:'Criado durante Yule com Artífice de Eregion.',equipped:false,tags:['mágico','Eregion'],magicalKind:'wondrous-artefact' as const,magicalIdentified:true,craftsmanship:'Élfica',magicalQualities:[]}],other:{...sheet.other,supplementState:{...(sheet.other.supplementState||{}),artificerUsedYuleYear:Number(nextState.worldClock.year),artificerYuleReady:false}}}));
      }
      break;
    }

"""
cmd=req(cmd,anchor,cases+anchor,"stateful supplement command cases")

# Rearward: a healthy Mirkwood Hound counts as another melee hero.
cmd=req(cmd,
"""        const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
        if (command.stance === 'skirmish' && nextState.company.rulesMode !== 'strider') throw new Error('Postura de Escaramuça exige Modo Peregrino.');
        nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk);""",
"""        const smallFolk = Boolean(hero && /povo pequeno/i.test(hero.sheet.other.virtues || ''));
        if (command.stance === 'skirmish' && nextState.company.rulesMode !== 'strider') throw new Error('Postura de Escaramuça exige Modo Peregrino.');
        const houndMelee = hero && houndAvailable(hero.sheet) ? 1 : 0;
        nextState = setStance(nextState, command.targetCharacterId, command.stance, smallFolk, houndMelee);""",
"Hound Rearward count")

# Block combat until pending Hound piercing choice is resolved.
cmd=req(cmd,
"""    case 'resolve_hero_attack': {
      const attacker = nextCharacters.find(character => character.id === command.attackerId);""",
"""    case 'resolve_hero_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const attacker = nextCharacters.find(character => character.id === command.attackerId);""",
"Hound pending hero attack")
cmd=req(cmd,
"""    case 'resolve_adversary_attack': {
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);""",
"""    case 'resolve_adversary_attack': {
      if(nextState.combat.pendingHoundPiercing) throw new Error('Resolva primeiro a escolha do Cão da Floresta para o Golpe Perfurante pendente.');
      const attacker = nextState.combat.actors.find(item => item.actorId === command.attackerId);""",
"Hound pending adversary attack")

# A normal Prepare Shot is a main action; Deadly Archery avoids this marker.
cmd=req(cmd,
"""      let actor = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      let target = nextState.combat.actors.find(item => item.actorId === command.targetActorId);""",
"""      let actor = nextState.combat.actors.find(item => item.actorId === command.attackerId);
      let target = nextState.combat.actors.find(item => item.actorId === command.targetActorId);
      if((actor?.stateNotes||[]).includes('temp-round:main-action-used:prepare-shot')) throw new Error('Preparar Tiro já consumiu a ação principal desta rodada. Arquearia Mortal permite a exceção como ação secundária com Arco.');""",
"Prepare Shot action economy")

# Elven Lights surprise triggers when the Company attacks a selected observer.
cmd=req(cmd,
"""      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired), { terrain, isNight, escaping });
      const weapon = result.weapon;""",
"""      const result = rollHeroAttack(freshAttacker.sheet, actor, target, command.weaponIndex, hopeDice, Boolean(command.inspired), { terrain, isNight, escaping });
      const weapon = result.weapon;
      const light=nextState.combat.elvenLight;
      if(light?.active&&!light.triggered&&light.targetActorIds.includes(target.actorId)){
        const firstRound=Math.max(1,nextState.combat.round||1);
        nextState.combat={
          ...nextState.combat,
          elvenLight:{...light,triggered:true},
          actors:nextState.combat.actors.map(item=>light.targetActorIds.includes(item.actorId)?{...item,stateNotes:[...(item.stateNotes||[]),'elven-light-no-action-round:'+String(firstRound),'elven-light-weary-through:'+String(firstRound+1)]}:item),
        };
        target=nextState.combat.actors.find(item=>item.actorId===command.targetActorId)||target;
      }""",
"Elven Lights surprise trigger")

# Failed hero Protection may be intercepted by a healthy Hound.
cmd=req(cmd,
"              if (!protection.success) updateHero(target.actorId, sheet => applyHeroWound(sheet).sheet);",
"""              if (!protection.success) {
                if(houndAvailable(targetHero.sheet)) nextState.combat={...nextState.combat,pendingHoundPiercing:{targetCharacterId:targetHero.id,sourceActorId:actor.actorId,createdAt:Date.now(),round:Math.max(1,nextState.combat.round||1)}};
                else updateHero(target.actorId, sheet => applyHeroWound(sheet).sheet);
              }""",
"Hound hero-vs-hero piercing")
cmd=req(cmd,
"        if (!protection.success) updateHero(targetHero.id, sheet => applyHeroWound(sheet).sheet);",
"""        if (!protection.success) {
          if(houndAvailable(targetHero.sheet)) nextState.combat={...nextState.combat,pendingHoundPiercing:{targetCharacterId:targetHero.id,sourceActorId:attacker.actorId,createdAt:Date.now(),round:Math.max(1,nextState.combat.round||1)}};
          else updateHero(targetHero.id, sheet => applyHeroWound(sheet).sheet);
        }""",
"Hound adversary piercing")

# Hound passive penalty only while healthy.
cmd=req(cmd,
"      if (!command.ranged && hasVirtue(targetHero.sheet, 'Cão da Floresta das Trevas') && targetHero.sheet.other.supplementSelections?.houndAvailable !== false) {",
"      if (!command.ranged && houndAvailable(targetHero.sheet)) {",
"Hound healthy attack penalty")

# Elven Lights: affected enemies cannot act on first round after attack and are Weary for two rounds.
cmd=req(cmd,
"""      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0));
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;""",
"""      const lightNoAction=(attacker.stateNotes||[]).some(note=>note==='elven-light-no-action-round:'+String(Math.max(1,nextState.combat.round||1)));
      if(lightNoAction) throw new Error('Luzes Élficas: este adversário está surpreso e não age nesta rodada.');
      const wearyThrough=(attacker.stateNotes||[]).map(note=>note.startsWith('elven-light-weary-through:')?Number(note.split(':')[1]):0).reduce((a,b)=>Math.max(a,b),0);
      const elvenLightWeary=Math.max(1,nextState.combat.round||1)<=wearyThrough;
      let dice = Math.max(0, Math.trunc(Number(command.successDice) || 0));
      if (!command.ranged && targetActor.stance === 'forward') dice += 1;""",
"Elven Lights adversary state")
cmd=req(cmd,
"      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', false, false);",
"      const roll = rollOneRing(dice, 0, parry, 'Ataque do adversário', 'normal', elvenLightWeary, false);",
"Elven Lights weary roll")

# Prepared Shot applies once, on the next ranged attack.
cmd=req(cmd,
"""        actor = nextState.combat.actors.find(item => item.actorId === command.attackerId) || actor;
        nextState = upsertCombatActor(nextState, { ...actor, stateNotes: (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:')) });""",
"""        actor = nextState.combat.actors.find(item => item.actorId === command.attackerId) || actor;
        nextState = upsertCombatActor(nextState, { ...actor, stateNotes: (actor.stateNotes || []).filter(note => !note.startsWith('gain-ground:') && !(result.ranged && note.startsWith('prepared-shot:'))) });""",
"consume Prepare Shot")

# Herbal Remedies is recorded by the stateful command case above.
cmd=req(cmd,
"""        const reduction = result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0;
        const remaining = Math.max(0, accumulated - reduction);""",
"""        const herbal = nextState.journey.herbalRecoveryByHero?.[characterId];
        const herbalReduction = herbal?.success ? 1 + Math.max(0, Number(herbal.successIcons) || 0) : 0;
        const reduction = (result ? Math.max(0, Number(result.mountVigour) || 0) + (result.success ? 1 + Math.max(0, Number(result.successIcons) || 0) : 0) : 0) + herbalReduction;
        const remaining = Math.max(0, accumulated - reduction);""",
"Herbal Journey reduction")
cmd=req(cmd,
"      nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {}, endRollsByHero: {}, pendingRoll: null } });",
"      nextState = completeJourney({ ...nextState, journey: { ...nextState.journey, travelFatigueByHero: {}, endRollsByHero: {}, herbalRecoveryByHero: {}, pendingRoll: null } });",
"clear Herbal Journey state")

# A failed Hound recovery lasts until the next Adventure Phase, which starts when Fellowship ends.
cmd=req(cmd,
"""    case 'end_fellowship_phase':
      nextState = endFellowshipPhase(nextState);
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;""",
"""    case 'end_fellowship_phase':
      nextState = endFellowshipPhase(nextState);
      for(const characterId of partyIds) updateHero(characterId,sheet=>{
        if(!hasVirtue(sheet,'Cão da Floresta das Trevas')) return sheet;
        const next=structuredClone(sheet);
        next.other.supplementState={...(next.other.supplementState||{}),houndWounded:false,houndRecoveryPending:false,houndRecoveryFailed:false};
        return next;
      });
      nextState.scene = { ...nextState.scene, status: 'closed', updatedAt: Date.now() };
      break;""",
"Hound next Adventure Phase recovery")

write(rel,cmd)
print("PATCH_4940_COMMANDS_OK")
