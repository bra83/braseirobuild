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

if "createHeirCharacter" not in cmd:
    cmd=req(
        cmd,
        "import type { CampaignState, Character, CharacterSheet, Npc } from '../db';",
        "import type { CampaignState, Character, CharacterSheet, Npc } from '../db';\nimport { createHeirCharacter } from '../characterFactory';",
        "Heir factory import",
    )

anchor="    case 'player_start_journey': {"
cases=r"""    case 'educate_heir': {
      if(nextState.fellowshipPhase.status!=='active'||!nextState.fellowshipPhase.isYule) throw new Error('Educar um Herdeiro exige uma Fase de Sociedade em Yule.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!partyIds.has(hero.id)) throw new Error('Herói antecessor não encontrado na Companhia.');
      const amount=Math.trunc(Number(command.spend)||0);
      if(amount<1||amount>5) throw new Error('Educar um Herdeiro permite gastar de 1 a 5 pontos de Tesouro e a mesma quantidade de Pontos de Aventura.');
      const count=Math.max(0,Number(nextState.fellowshipPhase.undertakingCountByHero?.[hero.id])||0);
      if(count>=1) throw new Error('Este Herói já realizou seu Empreendimento normal nesta Fase de Sociedade.');
      const current=Math.max(0,Math.min(20,Math.trunc(Number(hero.sheet.other.heirState?.previousExperiencePool)||0)));
      if(current>=20) throw new Error('A reserva do Herdeiro já atingiu o máximo de 20 pontos.');
      if(current+amount>20) throw new Error('Este gasto ultrapassaria o máximo de 20 pontos da reserva do Herdeiro.');
      if(hero.sheet.resources.treasure<amount) throw new Error('Tesouro insuficiente para Educar o Herdeiro.');
      if(hero.sheet.resources.adventurePoints<amount) throw new Error('Pontos de Aventura insuficientes para Educar o Herdeiro.');
      const existingName=String(hero.sheet.other.heirState?.name||'').trim();
      const requestedName=String(command.heirName||'').trim();
      if(!existingName&&!requestedName) throw new Error('Na primeira vez que Educar um Herdeiro for escolhido, dê um nome ao Herdeiro.');
      if(existingName&&requestedName&&existingName!==requestedName) throw new Error('O Herdeiro já foi nomeado; continue educando o mesmo personagem.');

      updateHero(hero.id,sheet=>{
        const next=structuredClone(sheet);
        next.resources.treasure-=amount;
        next.resources.adventurePoints-=amount;
        const pool=current+amount;
        next.other.heirState={
          ...(next.other.heirState||{}),
          name:existingName||requestedName,
          previousExperiencePool:pool,
          ready:pool>=10,
          retiredOrDead:false,
        };
        return next;
      });
      nextState.fellowshipPhase.undertakings={...(nextState.fellowshipPhase.undertakings||{}),[hero.id]:'narrative'};
      nextState.fellowshipPhase.undertakingCountByHero={...(nextState.fellowshipPhase.undertakingCountByHero||{}),[hero.id]:count+1};
      break;
    }

    case 'set_heir_family_skill': {
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero||!partyIds.has(hero.id)) throw new Error('Herói antecessor não encontrado.');
      const state=hero.sheet.other.heirState;
      if(!state?.name) throw new Error('Primeiro estabeleça e eduque um Herdeiro.');
      const skill=hero.sheet.skills.find(item=>item.name===command.skill);
      if(!skill?.favorite) throw new Error('A Perícia de herança familiar deve ser Favorecida no Herói original.');
      updateHero(hero.id,sheet=>{
        const next=structuredClone(sheet);
        next.other.heirState={...(next.other.heirState||{previousExperiencePool:0}),inheritedFavouredSkill:command.skill};
        return next;
      });
      break;
    }

    case 'create_heir_character': {
      if(nextState.activeMode==='combat') throw new Error('Não é possível aposentar/substituir um Herói durante um combate.');
      const predecessor=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!predecessor||!partyIds.has(predecessor.id)) throw new Error('Herói antecessor não encontrado na Companhia.');
      const heirState=predecessor.sheet.other.heirState;
      const pool=Math.max(0,Math.min(20,Math.trunc(Number(heirState?.previousExperiencePool)||0)));
      if(!heirState?.ready||pool<10) throw new Error('O Herdeiro ainda não possui os 10 pontos mínimos de Experiência Anterior.');
      const heirName=String(heirState.name||'').trim();
      if(!heirName) throw new Error('O Herdeiro ainda não foi nomeado.');
      const familySkill=String(heirState.inheritedFavouredSkill||'').trim();
      if(!familySkill) throw new Error('Escolha a Perícia Favorecida de herança familiar antes de criar o Herdeiro.');

      const allowance=pool>=20?2:pool>=15?1:0;
      const selectedIds=Array.from(new Set((command.inheritedItemIds||[]).map(String).filter(Boolean)));
      if(selectedIds.length>allowance) throw new Error('A reserva atual permite no máximo '+allowance+' item(ns) de Herança.');
      if(allowance===0&&selectedIds.length) throw new Error('Heranças materiais exigem pelo menos 15 pontos na reserva do Herdeiro.');

      const selectedItems=selectedIds.map(id=>predecessor.sheet.inventory.find(item=>item.id===id));
      if(selectedItems.some(item=>!item)) throw new Error('Uma das Heranças selecionadas não existe mais no inventário do antecessor.');
      const eligible=(item:NonNullable<(typeof selectedItems)[number]>)=>{
        const rewarded=(item.tags||[]).some(tag=>/^Recompensa:/i.test(tag));
        const magical=['magnificent-artefact','wondrous-item','famous-war-gear'].includes(String(item.magicalKind||''));
        return rewarded||magical;
      };
      if(selectedItems.some(item=>item&&!eligible(item))) throw new Error('Herança inválida: escolha equipamento com Recompensa, Artefato/Item Maravilhoso ou Arma/Armadura Famosa.');

      const heir=createHeirCharacter({
        predecessor,
        heirName,
        previousExperienceBudget:pool,
        inheritedFavouredSkill:familySkill,
        inheritedItems:selectedItems.filter(Boolean) as CharacterSheet['inventory'],
        existingNames:nextCharacters.map(character=>character.sheet.identity.name||character.name),
        rulesMode:nextState.company.rulesMode==='strider'?'strider':'standard',
      });
      heir.createdAt=Date.now();
      heir.updatedAt=Date.now();

      const selectedNames=new Set(selectedItems.filter(Boolean).map(item=>String(item!.name)));
      const selectedTreasureEntries=new Set(selectedItems.filter(Boolean).map(item=>String(item!.treasureIndexEntryId||'')).filter(Boolean));
      nextCharacters=nextCharacters.map(character=>{
        if(character.id!==predecessor.id) return character;
        const next=structuredClone(character);
        next.sheet.inventory=next.sheet.inventory.filter(item=>!selectedIds.includes(item.id));
        next.sheet.combat.weapons=(next.sheet.combat.weapons||[]).filter(weapon=>{
          if(weapon.treasureIndexEntryId&&selectedTreasureEntries.has(String(weapon.treasureIndexEntryId))) return false;
          return !selectedNames.has(String(weapon.name));
        });
        next.sheet=normalizeEquipmentMechanics(next.sheet);
        next.sheet.other.heirState={
          ...(next.sheet.other.heirState||{previousExperiencePool:pool}),
          inheritanceItemIds:selectedIds,
          createdCharacterId:heir.id,
          retiredOrDead:true,
          ready:true,
        };
        next.updatedAt=Date.now();
        return next;
      });
      nextCharacters.push(heir);

      partyIds.delete(predecessor.id);
      partyIds.add(heir.id);
      const currentParty=(nextCampaign.partyCharacterIds||[]).filter(id=>id!==predecessor.id);
      nextCampaign.partyCharacterIds=Array.from(new Set([...currentParty,heir.id]));
      if(nextCampaign.activeCharacterId===predecessor.id||!nextCampaign.activeCharacterId){
        nextCampaign.activeCharacterId=heir.id;
        nextCampaign.characterSheet=heir.sheet;
      }
      nextState.scene={
        ...nextState.scene,
        participantHeroIds:(nextState.scene.participantHeroIds||[]).map(id=>id===predecessor.id?heir.id:id),
        spotlightCharacterId:nextState.scene.spotlightCharacterId===predecessor.id?heir.id:nextState.scene.spotlightCharacterId,
        spotlightQueue:(nextState.scene.spotlightQueue||[]).map(id=>id===predecessor.id?heir.id:id),
        updatedAt:Date.now(),
      };
      appendLivingActivity('fellowship','Herdeiro assume a aventura',(predecessor.sheet.identity.name||predecessor.name)+' foi sucedido por '+(heir.sheet.identity.name||heir.name)+'.',heir.id);
      break;
    }

"""
cmd=req(cmd,anchor,cases+anchor,"Heir command cases")
write(rel,cmd)

print("PATCH_4946_COMMANDS_OK")
