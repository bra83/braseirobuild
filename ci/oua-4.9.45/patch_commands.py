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

anchor="    case 'player_start_journey': {"
cases=r"""    case 'start_skill_test_session': {
      const participants=Array.from(new Set((command.participants||[]).filter(id=>partyIds.has(id))));
      if(!participants.length) throw new Error('Selecione ao menos um Herói para o teste de Perícia.');
      nextState={
        ...nextState,
        skillTestSession:{
          id:'skill_test_'+Date.now(),
          label:String(command.label||'Teste de Perícia').trim()||'Teste de Perícia',
          participants,
          requiredSuccesses:command.requiredSuccesses&&command.requiredSuccesses>0?Math.trunc(command.requiredSuccesses):undefined,
          rolls:{},
          additionalSuccesses:0,
          informationDiscoveries:0,
          silent:false,
          timeMultiplier:1,
          additionalTargetGroups:0,
          effects:[],
          status:'active',
          createdAt:Date.now(),
          updatedAt:Date.now(),
        },
        updatedAt:Date.now(),
      };
      break;
    }

    case 'roll_skill_test_session': {
      const session=nextState.skillTestSession;
      if(!session||session.status!=='active') throw new Error('Nenhuma sessão ativa de teste de Perícia.');
      if(!session.participants.includes(command.targetCharacterId)) throw new Error('O Herói não participa desta resolução.');
      if(session.rolls[command.targetCharacterId]) throw new Error('Este Herói já fez sua tentativa nesta sessão. Inicie outra sessão para tentar novamente.');
      const hero=nextCharacters.find(character=>character.id===command.targetCharacterId);
      if(!hero) throw new Error('Herói não encontrado.');
      const skill=findSkill(hero.sheet,command.skill);
      if(!skill) throw new Error('Perícia não encontrada: '+command.skill+'.');
      if(command.spendHope&&command.useMagicalSuccess) throw new Error('Escolha entre o bônus normal de Esperança e Sucesso Mágico.');

      let supportDice=0;
      let supportHopeSpent=false;
      let supporterCharacterId:string|undefined;
      let supporterSkill:string|undefined;
      if(command.supporterCharacterId){
        if(command.supporterCharacterId===hero.id) throw new Error('Um Herói não pode prestar Suporte a si mesmo.');
        const supporter=nextCharacters.find(character=>character.id===command.supporterCharacterId);
        if(!supporter||!partyIds.has(supporter.id)) throw new Error('Apoiador não encontrado na Companhia.');
        const requested=String(command.supporterSkill||'').trim();
        const supportSkill=findSkill(supporter.sheet,requested);
        if(!requested||!supportSkill||Math.max(0,Number(supportSkill.rank)||0)<1) throw new Error('Suporte exige uma Perícia apropriada com pelo menos 1 ponto.');
        if(supporter.sheet.attributes.hopeCurrent<=0) throw new Error('O apoiador não possui Esperança para prestar Suporte.');
        updateHero(supporter.id,sheet=>adjustHope(sheet,-1));
        supportDice=nextState.company.fellowshipFocus?.[supporter.id]===hero.id?2:1;
        supportHopeSpent=true;
        supporterCharacterId=supporter.id;
        supporterSkill=requested;
      }

      const context={kind:'skill' as const,skill:command.skill,terrain:'',isNight:false};
      const supplementDice=supplementBonusDice(hero.sheet,context);
      const blessingDice=magicalBlessingDice(hero.sheet,command.skill);
      const darkening=curseDarkeningDicePenalty(hero.sheet);
      const inspired=automaticInspired(hero.sheet)||supplementInspired(hero.sheet,context);
      let ownHopeDice=0;
      let ownHopeSpent=false;
      if(command.spendHope){
        if(curseBlocksHopeForSkill(hero.sheet,command.skill)) throw new Error('Malícia impede gastar Esperança nesta rolagem.');
        updateHero(hero.id,sheet=>{
          const spent=spendHope(sheet,attributeForSkill(command.skill));
          if(!spent.allowed) throw new Error('Esperança insuficiente.');
          return spent.sheet;
        });
        ownHopeDice=inspired?2:1;
        ownHopeSpent=true;
      }

      const refreshed=nextCharacters.find(character=>character.id===hero.id)!;
      const baseMode=shadowAtLimit(refreshed.sheet)?'ill-favoured':skill.favorite?'favoured':'normal';
      const mode=supplementRollMode(refreshed.sheet,context,baseMode);
      const count=Math.max(0,Number(skill.rank)||0)+supportDice+supplementDice+blessingDice+ownHopeDice+darkening;
      let roll;
      if(command.useMagicalSuccess){
        if(curseBlocksHopeForSkill(refreshed.sheet,command.skill)) throw new Error('Malícia impede gastar Esperança nesta rolagem.');
        if(refreshed.sheet.attributes.hopeCurrent<=0) throw new Error('Esperança insuficiente.');
        const source=magicalSuccessSource(refreshed.sheet,context)||blessingSource(refreshed.sheet,command.skill);
        const allowed=canSpendHopeForSupplementMagicalSuccess(refreshed.sheet,context)||identifiedBlessingCanMagicalSuccess(refreshed.sheet,command.skill);
        if(!source||!allowed) throw new Error('Sucesso Mágico indisponível para esta Perícia.');
        updateHero(refreshed.id,sheet=>adjustHope(sheet,-1));
        ownHopeSpent=true;
        roll=rollMagicalSuccess(count,command.skill,source,Boolean(refreshed.sheet.attributes.weary));
      }else{
        roll=rollOneRing(
          count,0,targetNumberForSkill(refreshed.sheet,command.skill),command.skill,mode,
          Boolean(refreshed.sheet.attributes.weary),Boolean(refreshed.sheet.attributes.miserable),
        );
      }
      roll=applyFeatOneAsEye(roll,sheetFeatOneBecomesEye(refreshed.sheet));
      roll=applyIllLuckToResult(refreshed.sheet,roll);
      if(nextState.company.eyeAwarenessEnabled&&nextState.activeMode!=='combat'&&(roll.featResult===11||(roll.featResults||[]).includes(11))){
        raiseEyeAwareness(1,'+1: teste de Perícia fora de combate mostrou o Olho de Sauron.');
      }

      nextState={
        ...nextState,
        skillTestSession:{
          ...session,
          rolls:{
            ...session.rolls,
            [hero.id]:{
              characterId:hero.id,
              skill:command.skill,
              roll,
              success:Boolean(roll.success),
              successIcons:Math.max(0,Number(roll.tengwarCount)||0),
              iconsSpent:0,
              supporterCharacterId,
              supporterSkill,
              supportHopeSpent,
              ownHopeSpent,
              magicalSuccess:Boolean(roll.magicalSuccess),
            },
          },
          updatedAt:Date.now(),
        },
        updatedAt:Date.now(),
      };
      break;
    }

    case 'spend_skill_success_icon': {
      const session=nextState.skillTestSession;
      if(!session||session.status!=='active') throw new Error('Nenhuma sessão ativa de teste de Perícia.');
      const source=session.rolls[command.sourceCharacterId];
      if(!source||!source.success) throw new Error('Somente uma jogada de Perícia bem-sucedida pode gastar ícones de Sucesso.');
      if(source.iconsSpent>=source.successIcons) throw new Error('Esta jogada não possui ícones de Sucesso disponíveis.');
      const choice=command.choice;
      const rolls={...session.rolls};
      let additionalSuccesses=session.additionalSuccesses;
      let informationDiscoveries=session.informationDiscoveries;
      let silent=session.silent;
      let timeMultiplier=session.timeMultiplier;
      let additionalTargetGroups=session.additionalTargetGroups;

      if(choice==='cancel-failure'){
        if(session.participants.length<2) throw new Error('Cancelar uma falha exige uma jogada de Perícia envolvendo vários Heróis.');
        const targetId=String(command.targetCharacterId||'');
        if(!targetId||targetId===source.characterId) throw new Error('Escolha outro Herói que tenha falhado.');
        const target=rolls[targetId];
        if(!target||target.success) throw new Error('O alvo precisa ter uma jogada fracassada nesta mesma resolução.');
        rolls[targetId]={...target,success:true,cancelledFailureBy:source.characterId};
      }else if(choice==='additional-success'){
        if(!session.requiredSuccesses) throw new Error('Obter 1 Sucesso Adicional só se aplica quando a ação exige múltiplos sucessos.');
        additionalSuccesses+=1;
      }else if(choice==='information'){
        informationDiscoveries+=1;
      }else if(choice==='silent'){
        if(silent) throw new Error('A ação já foi tornada silenciosa por um ícone de Sucesso.');
        silent=true;
      }else if(choice==='hurry'){
        if(timeMultiplier<1) throw new Error('A tarefa já foi acelerada por um ícone de Sucesso.');
        timeMultiplier=0.5;
      }else if(choice==='influence'){
        additionalTargetGroups+=1;
      }

      rolls[source.characterId]={...source,iconsSpent:source.iconsSpent+1};
      nextState={
        ...nextState,
        skillTestSession:{
          ...session,
          rolls,
          additionalSuccesses,
          informationDiscoveries,
          silent,
          timeMultiplier,
          additionalTargetGroups,
          effects:[...session.effects,{
            choice,
            sourceCharacterId:source.characterId,
            targetCharacterId:command.targetCharacterId,
            note:String(command.note||'').trim()||undefined,
            createdAt:Date.now(),
          }],
          updatedAt:Date.now(),
        },
        updatedAt:Date.now(),
      };
      break;
    }

    case 'end_skill_test_session': {
      const session=nextState.skillTestSession;
      if(!session) throw new Error('Nenhuma sessão de teste de Perícia.');
      nextState={...nextState,skillTestSession:{...session,status:'resolved',updatedAt:Date.now()},updatedAt:Date.now()};
      break;
    }

"""
cmd=req(cmd,anchor,cases+anchor,"Support and Special Success commands")
write(rel,cmd)

print("PATCH_4945_COMMANDS_OK")
