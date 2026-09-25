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
    "import { isRangedWeapon, normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
    "import { canonicalWeapon, isRangedWeapon, normalizeEquipmentMechanics, rangerIgnoresJourneyFatigue, restStrengthMultiplier } from '../rules/equipment';",
    "canonical weapon import",
)
cmd=req(
    cmd,
    "import { advanceCombatProficiency, advanceSkill, advanceValor, advanceWisdom } from '../rules/progression';",
    "import { advanceCombatProficiency, advanceSkill, advanceValor, advanceValorForFamousQuality, advanceWisdom } from '../rules/progression';",
    "famous Valor import",
)
# The 4.9.23 shadow import survives later patches.
cmd=req(
    cmd,
    "import { adjustShadowCanonical, firmWill, healShadowScar, resolveBoutOfMadness, resolveShadowGain } from '../rules/shadow';",
    "import { adjustShadowCanonical, firmWill, healShadowScar, resolveBoutOfMadness, resolveShadowGain, shadowAtLimit } from '../rules/shadow';\nimport { allowedBanes, maxBanes, requiredBlessingCount, rollMagicalTreasureDiscoveries } from '../rules/magicalTreasure';",
    "treasure imports",
)

anchor="    case 'player_start_journey': {"
cases=r"""    case 'add_treasure_index_entry': {
      const entry=structuredClone(command.entry);
      if(!entry.id||!entry.name.trim()) throw new Error('A entrada do Índice de Tesouro precisa de id e nome.');
      if((nextState.treasureIndex||[]).some(item=>item.id===entry.id)) throw new Error('Já existe uma entrada com este id no Índice de Tesouro.');
      if(!nextCharacters.some(character=>character.id===entry.destinedCharacterId)) throw new Error('O item deve ser destinado a um Herói existente.');
      if(entry.kind==='magnificent-artefact'||entry.kind==='wondrous-item'){
        const expected=requiredBlessingCount(entry.kind);
        const blessings=Array.from(new Set((entry.blessings||[]).map(String).filter(Boolean)));
        if(blessings.length!==expected) throw new Error(entry.kind==='magnificent-artefact'?'Artefato Magnífico exige exatamente 1 Bênção.':'Item Maravilhoso exige exatamente 2 Bênçãos diferentes.');
        entry.blessings=blessings;
        entry.qualities=[];
      }else{
        if(!entry.baseItemName?.trim()) throw new Error('Arma/Armadura Famosa precisa do equipamento-base.');
        const qualities=Array.from(new Set((entry.qualities||[]).map(String).filter(Boolean)));
        if(qualities.length<1||qualities.length>3) throw new Error('Arma/Armadura Famosa exige de 1 a 3 qualidades, na ordem de desbloqueio.');
        entry.qualities=qualities;
        entry.blessings=[];
        if(!entry.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        const banes=Array.from(new Set((entry.banes||[]).map(String).filter(Boolean)));
        if(banes.length>maxBanes(entry.origin)) throw new Error('Quantidade de Flagelos excede o permitido pela Origem.');
        const legal=new Set(allowedBanes(entry.origin));
        if(banes.some(value=>!legal.has(value as any))) throw new Error('Há Flagelo incompatível com a Origem do item.');
        entry.banes=banes;
      }
      nextState={...nextState,treasureIndex:[...(nextState.treasureIndex||[]),entry],updatedAt:Date.now()};
      break;
    }

    case 'update_treasure_index_entry': {
      const current=(nextState.treasureIndex||[]).find(item=>item.id===command.entry.id);
      if(!current) throw new Error('Entrada do Índice de Tesouro não encontrada.');
      if(current.claimed) throw new Error('Uma entrada já reivindicada não pode ser reescrita no Índice.');
      const replacement=structuredClone(command.entry);
      if(!nextCharacters.some(character=>character.id===replacement.destinedCharacterId)) throw new Error('O item deve ser destinado a um Herói existente.');
      if(replacement.kind==='magnificent-artefact'||replacement.kind==='wondrous-item'){
        const expected=requiredBlessingCount(replacement.kind);
        replacement.blessings=Array.from(new Set((replacement.blessings||[]).map(String).filter(Boolean)));
        if(replacement.blessings.length!==expected) throw new Error('Número de Bênçãos incompatível com a natureza do item.');
        replacement.qualities=[];
      }else{
        if(!replacement.baseItemName?.trim()) throw new Error('Arma/Armadura Famosa precisa do equipamento-base.');
        replacement.qualities=Array.from(new Set((replacement.qualities||[]).map(String).filter(Boolean)));
        if(replacement.qualities.length<1||replacement.qualities.length>3) throw new Error('Arma/Armadura Famosa exige de 1 a 3 qualidades.');
        if(!replacement.origin) throw new Error('Arma/Armadura Famosa exige Origem.');
        replacement.banes=Array.from(new Set((replacement.banes||[]).map(String).filter(Boolean)));
        const legal=new Set(allowedBanes(replacement.origin));
        if(replacement.banes.length>maxBanes(replacement.origin)||replacement.banes.some(value=>!legal.has(value as any))) throw new Error('Flagelos incompatíveis com a Origem.');
        replacement.blessings=[];
      }
      nextState={...nextState,treasureIndex:(nextState.treasureIndex||[]).map(item=>item.id===replacement.id?replacement:item),updatedAt:Date.now()};
      break;
    }

    case 'roll_magical_treasure': {
      const discoveries=rollMagicalTreasureDiscoveries(command.rating)
        .filter(result=>result.found&&result.kind)
        .map(result=>({
          id:'magical_treasure_'+Date.now()+'_'+Math.random().toString(36).slice(2,8),
          hoardRating:command.rating,
          featResult:result.featResult,
          foundOnEye:result.foundOnEye,
          natureDie:result.natureDie||1,
          kind:result.kind!,
          greedShadow:Math.max(0,Number(result.greedShadow)||0),
          createdAt:Date.now(),
        }));
      nextState={...nextState,pendingMagicalTreasure:[...(nextState.pendingMagicalTreasure||[]),...discoveries],updatedAt:Date.now()};
      break;
    }

    case 'claim_magical_treasure': {
      const discovery=(nextState.pendingMagicalTreasure||[]).find(item=>item.id===command.discoveryId);
      if(!discovery) throw new Error('Descoberta de Tesouro Mágico não encontrada.');
      const entry=(nextState.treasureIndex||[]).find(item=>item.id===command.treasureIndexEntryId);
      if(!entry||entry.claimed) throw new Error('Selecione uma entrada disponível do Índice de Tesouro.');
      if(entry.kind!==discovery.kind) throw new Error('A natureza da entrada escolhida não corresponde ao resultado da Tabela de Tesouro Mágico.');
      if(entry.destinedCharacterId!==command.targetCharacterId) throw new Error('Este item foi preparado para outro Herói; Tesouro Mágico é estritamente individual.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero) throw new Error('Herói destinatário não encontrado.');

      const identified=Boolean(nextState.company.elrondItemsIdentified);
      const common={
        id:'treasure_item_'+Date.now()+'_'+Math.random().toString(36).slice(2,7),
        name:entry.name,
        quantity:1,
        weight:1,
        load:1,
        category:'Tesouro Mágico',
        description:entry.notes||'Peça singular registrada no Índice de Tesouro da campanha.',
        equipped:false,
        tags:['Tesouro Mágico'],
        magicalKind:entry.kind,
        magicalBlessings:[...(entry.blessings||[])],
        magicalOrigin:entry.origin,
        magicalBanes:[...(entry.banes||[])],
        magicalQualities:[...(entry.qualities||[])],
        magicalQualitiesUnlocked:entry.kind==='famous-war-gear'?1:0,
        magicalIdentified:identified,
        magicalCurse:entry.curse,
        magicalCurseActive:false,
        magicalCurseTrigger:entry.curseTrigger,
        magicalCurseRemoval:entry.curseRemoval,
        treasureIndexEntryId:entry.id,
        destinedCharacterId:entry.destinedCharacterId,
        craftsmanship:entry.origin,
      } as any;

      if(entry.kind==='famous-war-gear'){
        const baseName=String(entry.baseItemName||'').trim();
        if(!baseName) throw new Error('A peça famosa não possui equipamento-base no Índice.');
        common.magicalBaseItemName=baseName;
        common.name=entry.name;
        const weapon=canonicalWeapon(baseName);
        if(weapon){
          common.category='Arma';
          common.weight=weapon.load; common.load=weapon.load; common.damage=weapon.damage; common.injury=weapon.injury; common.tags=[...common.tags,weapon.group,'BaseItem:'+baseName];
        }else if(/escudo|broquel/i.test(baseName)){
          common.category='Escudo'; common.tags=[...common.tags,'escudo','BaseItem:'+baseName];
        }else if(/elmo/i.test(baseName)){
          common.category='Elmo'; common.tags=[...common.tags,'elmo','BaseItem:'+baseName];
        }else{
          common.category='Armadura'; common.tags=[...common.tags,'armadura','BaseItem:'+baseName];
        }
      }

      updateHero(hero.id,sheet=>{
        let next=structuredClone(sheet);
        next.inventory=[...(next.inventory||[]),common];
        if(entry.kind==='famous-war-gear'){
          const base=canonicalWeapon(String(entry.baseItemName||''));
          if(base){
            next.combat.weapons=[...(next.combat.weapons||[]),{
              name:entry.name,
              damage:base.damage,
              injury:base.injury,
              group:base.group,
              active:false,
              magicalBaseItemName:String(entry.baseItemName||''),
              treasureIndexEntryId:entry.id,
              magicalQualities:[...(entry.qualities||[])],
              magicalQualitiesUnlocked:1,
            }];
          }
        }
        if(discovery.foundOnEye&&discovery.greedShadow>0){
          const rank=Math.max(0,Number(next.attributes.wisdom)||0);
          const baseMode=shadowAtLimit(next)?'ill-favoured':/hobbit/i.test(next.identity.culture||'')?'favoured':'normal';
          const shadowRoll=rollOneRing(rank,0,targetNumberForSkill(next,'Sabedoria'),'Sabedoria',baseMode,Boolean(next.attributes.weary),Boolean(next.attributes.miserable));
          const resolved=resolveShadowGain(next,discovery.greedShadow,'greed',{success:Boolean(shadowRoll.success),successIcons:Math.max(0,Number(shadowRoll.tengwarCount)||0)});
          next=resolved.sheet;
          next.historyLog=[...(next.historyLog||[]),{
            timestamp:Date.now(),field:'Sombra · Ganância',oldValue:String(Math.max(0,next.attributes.shadow-resolved.gained)),newValue:String(next.attributes.shadow),
            reason:'Tesouro Mágico encontrado em resultado Olho; teste de Sabedoria '+(shadowRoll.success?'reduziu':'não reduziu')+' o ganho de '+discovery.greedShadow+'.',
          }];
        }
        return normalizeEquipmentMechanics(next);
      });

      nextState={
        ...nextState,
        treasureIndex:(nextState.treasureIndex||[]).map(item=>item.id===entry.id?{...item,discovered:true,claimed:true}:item),
        pendingMagicalTreasure:(nextState.pendingMagicalTreasure||[]).filter(item=>item.id!==discovery.id),
        updatedAt:Date.now(),
      };
      break;
    }

    case 'identify_magical_item': {
      updateHero(command.targetCharacterId,sheet=>{
        const item=sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
        if(!item||!item.magicalKind) throw new Error('Item mágico não encontrado.');
        return {...sheet,inventory:sheet.inventory.map(entry=>entry.id===item.id?{...entry,magicalIdentified:true}:entry)};
      });
      break;
    }

    case 'unlock_famous_quality': {
      if(nextState.fellowshipPhase.status!=='active') throw new Error('Desbloqueio deliberado de Qualidades ocorre durante a Fase de Sociedade.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero) throw new Error('Herói não encontrado.');
      const item=hero.sheet.inventory.find(entry=>entry.id===command.inventoryItemId);
      if(!item||item.magicalKind!=='famous-war-gear') throw new Error('Selecione uma Arma ou Armadura Famosa.');
      const total=(item.magicalQualities||[]).length;
      const unlocked=Math.max(0,Number(item.magicalQualitiesUnlocked)||0);
      if(unlocked>=total) throw new Error('Todas as Qualidades deste item já foram ativadas.');

      if(command.source==='valor'){
        if(nextState.fellowshipPhase.valorWisdomAdvance?.[hero.id]) throw new Error('Este Herói já elevou Valor ou Sabedoria nesta Fase de Sociedade.');
        updateHero(hero.id,sheet=>{
          const result=advanceValorForFamousQuality(sheet,item.id);
          if(!result.allowed) throw new Error(result.reason);
          return result.sheet;
        });
        nextState.fellowshipPhase.valorWisdomAdvance={...(nextState.fellowshipPhase.valorWisdomAdvance||{}),[hero.id]:'valor'};
      }else{
        const count=Math.max(0,Number(nextState.fellowshipPhase.undertakingCountByHero?.[hero.id])||0);
        if(count>=1) throw new Error('Este Herói já realizou seu Empreendimento normal nesta Fase de Sociedade.');
        const sacrificed=hero.sheet.inventory.find(entry=>entry.id===command.sacrificedInventoryItemId);
        if(!sacrificed||sacrificed.id===item.id) throw new Error('Escolha um equipamento de guerra anterior para doar ao tesouro de seu povo.');
        const rewardTags=(sacrificed.tags||[]).filter(tag=>/^Recompensa:/i.test(tag));
        if(!rewardTags.length) throw new Error('O equipamento doado precisa possuir ao menos uma Recompensa.');
        updateHero(hero.id,sheet=>{
          const next=structuredClone(sheet);
          const famous=next.inventory.find(entry=>entry.id===item.id)!;
          famous.magicalQualitiesUnlocked=Math.min((famous.magicalQualities||[]).length,Math.max(0,Number(famous.magicalQualitiesUnlocked)||0)+rewardTags.length);
          next.inventory=next.inventory.filter(entry=>entry.id!==sacrificed.id);
          next.combat.weapons=(next.combat.weapons||[])
            .filter(weapon=>weapon.name!==sacrificed.name)
            .map(weapon=>weapon.treasureIndexEntryId===famous.treasureIndexEntryId?{...weapon,magicalQualitiesUnlocked:famous.magicalQualitiesUnlocked}:weapon);
          return normalizeEquipmentMechanics(next);
        });
        nextState.fellowshipPhase.undertakings={...(nextState.fellowshipPhase.undertakings||{}),[hero.id]:'narrative'};
        nextState.fellowshipPhase.undertakingCountByHero={...(nextState.fellowshipPhase.undertakingCountByHero||{}),[hero.id]:count+1};
      }
      break;
    }

    case 'remove_magical_curse': {
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
    }

"""
cmd=req(cmd,anchor,cases+anchor,"magical treasure command cases")
write(rel,cmd)
print("PATCH_4941_COMMANDS_OK")
