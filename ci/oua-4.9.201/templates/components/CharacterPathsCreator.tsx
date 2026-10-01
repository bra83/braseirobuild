import React, { useMemo, useState } from 'react';
import { backgroundsForCulture, CHARACTER_PATH_BACKGROUNDS, IMPORTANT_EVENTS, type CharacterPathCultureId } from '../lib/characterPaths';
import { CHARACTER_PATH_CALLINGS, CHARACTER_PATH_CULTURES, INITIAL_REWARDS, characterPathCultureForDie, type CharacterPathCreationOptions } from '../lib/characterFactory';
import { CORE_COMMON_VIRTUES, type CoreAttribute } from '../lib/coreVirtues';
import { CANONICAL_SKILLS } from '../lib/skillRegistry';
import { secureRoll } from '../lib/dice';
import { CreationInfoButton } from './CreationInfoButton';
import { CALLING_HELP, REWARD_HELP, VIRTUE_HELP, skillHelp } from '../lib/characterCreationHelp';

type Props = {
  rulesMode: 'standard'|'strider';
  busy?: boolean;
  onCreate: (options: CharacterPathCreationOptions) => Promise<void>;
};

const attrLabel:Record<string,string>={strength:'Força',heart:'Coração',wits:'Astúcia'};
const ALL_PROFICIENCIES=['Machados','Arcos','Lanças','Espadas'] as const;
type Proficiency=(typeof ALL_PROFICIENCIES)[number];
const SKILL_COST=[0,1,2,3,5];
const PROFICIENCY_COST=[0,2,4,6];

function advancementCost(from:number,to:number,table:number[]):number{
  let cost=0;
  for(let rank=from+1;rank<=to;rank++)cost+=table[rank]||999;
  return cost;
}

export const CharacterPathsCreator:React.FC<Props>=({rulesMode,busy,onCreate})=>{
  const [cultureId,setCultureId]=useState<CharacterPathCultureId>('bardings');
  const [culturalAttributeBonus,setCulturalAttributeBonus]=useState<'strength'|'heart'|'wits'>('strength');
  const backgrounds=useMemo(()=>backgroundsForCulture(cultureId),[cultureId]);
  const [backgroundId,setBackgroundId]=useState('bardings-1-hammer-anvil');
  const [feat,setFeat]=useState(1);
  const [tnChoice,setTnChoice]=useState<''|'strength'|'heart'|'wits'>('');
  const [addFav,setAddFav]=useState('');
  const [removeFav,setRemoveFav]=useState<string[]>([]);
  const [coreVirtue,setCoreVirtue]=useState<(typeof CORE_COMMON_VIRTUES)[number]>('Confiança');
  const [coreVirtueAttribute,setCoreVirtueAttribute]=useState<CoreAttribute>('strength');
  const [masterySkillA,setMasterySkillA]=useState('');
  const [masterySkillB,setMasterySkillB]=useState('');
  const [callingName,setCallingName]=useState(CHARACTER_PATH_CALLINGS[0].name);
  const [callingSkillA,setCallingSkillA]=useState('');
  const [callingSkillB,setCallingSkillB]=useState('');
  const [primaryProficiency,setPrimaryProficiency]=useState<Proficiency>('Arcos');
  const [secondaryProficiency,setSecondaryProficiency]=useState<Proficiency>('Espadas');
  const [xpSkillRanks,setXpSkillRanks]=useState<Record<string,number>>({});
  const [xpProficiencyRanks,setXpProficiencyRanks]=useState<Record<string,number>>({});
  const [initialReward,setInitialReward]=useState<(typeof INITIAL_REWARDS)[number]>('Afiada');
  const [rewardWeaponGroup,setRewardWeaponGroup]=useState<Proficiency>('Arcos');
  const [message,setMessage]=useState('');

  const event=IMPORTANT_EVENTS.find(item=>item.feat===feat)||IMPORTANT_EVENTS[0];
  const background=CHARACTER_PATH_BACKGROUNDS.find(item=>item.id===backgroundId)||backgrounds[0];
  const calling=CHARACTER_PATH_CALLINGS.find(item=>item.name===callingName)||CHARACTER_PATH_CALLINGS[0];
  const callingSkillOptions=calling.favoured.filter(skill=>skill!==background?.favouredSkill);
  const masteryOptions=CANONICAL_SKILLS.filter(skill=>skill!==background?.favouredSkill&&skill!==callingSkillA&&skill!==callingSkillB);
  const culture=CHARACTER_PATH_CULTURES.find(item=>item.id===cultureId)||CHARACTER_PATH_CULTURES[0];
  const allowedPrimary=(culture.proficiencyChoices||[]) as Proficiency[];
  const baseSkillRanks=useMemo(()=>Object.fromEntries((culture.baseSkills||[]).map(item=>[item.name,item.rank])),[culture]);
  const baseProfRanks=useMemo(()=>Object.fromEntries(ALL_PROFICIENCIES.map(name=>[name,name===primaryProficiency?2:name===secondaryProficiency?1:0])),[primaryProficiency,secondaryProficiency]);
  const xpBudget=Math.max(0,(rulesMode==='strider'?15:10)+Number(event.previousExperienceDelta||0));
  const skillTarget=(name:string)=>Math.max(Number(baseSkillRanks[name]||0),Number(xpSkillRanks[name]??baseSkillRanks[name]??0));
  const proficiencyTarget=(name:string)=>Math.max(Number(baseProfRanks[name]||0),Number(xpProficiencyRanks[name]??baseProfRanks[name]??0));
  const xpSpent=CANONICAL_SKILLS.reduce((sum,name)=>sum+advancementCost(Number(baseSkillRanks[name]||0),skillTarget(name),SKILL_COST),0)
    +ALL_PROFICIENCIES.reduce((sum,name)=>sum+advancementCost(Number(baseProfRanks[name]||0),proficiencyTarget(name),PROFICIENCY_COST),0);
  const xpRemaining=xpBudget-xpSpent;
  const weaponReward=['Afiada','Cruel','Atroz'].includes(initialReward);

  const resetExperience=()=>{setXpSkillRanks({});setXpProficiencyRanks({});};

  const changeCulture=(id:CharacterPathCultureId)=>{
    setCultureId(id);
    const first=backgroundsForCulture(id)[0];
    if(first)setBackgroundId(first.id);
    const nextCulture=CHARACTER_PATH_CULTURES.find(item=>item.id===id);
    const primary=((nextCulture?.proficiencyChoices||[])[0]||'Espadas') as Proficiency;
    const secondary=(ALL_PROFICIENCIES.find(group=>group!==primary)||'Arcos') as Proficiency;
    setPrimaryProficiency(primary);setSecondaryProficiency(secondary);setRewardWeaponGroup(primary);resetExperience();
  };
  const choosePrimary=(value:Proficiency)=>{
    setPrimaryProficiency(value);
    if(secondaryProficiency===value)setSecondaryProficiency((ALL_PROFICIENCIES.find(group=>group!==value)||'Espadas') as Proficiency);
    setRewardWeaponGroup(value);resetExperience();
  };
  const chooseSecondary=(value:Proficiency)=>{setSecondaryProficiency(value);resetExperience();};
  const rollBackground=()=>{const die=secureRoll(1,6);const found=backgrounds.find(item=>item.die===die);if(found)setBackgroundId(found.id);};
  const rollCulture=()=>changeCulture(characterPathCultureForDie(secureRoll(1,6)));
  const rollEvent=()=>setFeat(secureRoll(1,12));

  const create=async()=>{
    setMessage('');
    try{
      if(xpSpent!==xpBudget)throw new Error('Distribua exatamente '+xpBudget+' pontos de Experiência Prévia. Restam '+xpRemaining+'.');
      if(weaponReward&&![primaryProficiency,secondaryProficiency].includes(rewardWeaponGroup))throw new Error('Escolha qual arma recebe a Recompensa Inicial.');
      const options:CharacterPathCreationOptions={
        cultureId,backgroundId,importantEventFeat:feat,rulesMode,
        culturalAttributeBonus:cultureId==='rangers'?culturalAttributeBonus:undefined,
        callingName,callingFavouredSkills:[callingSkillA,callingSkillB],
        tnImprovementAttribute:tnChoice||undefined,
        addedFavouredSkills:addFav?[addFav]:undefined,
        removedFavouredSkills:removeFav.length?removeFav:undefined,
        coreVirtue,
        coreVirtueAttribute:coreVirtue==='Proficiência'?coreVirtueAttribute:undefined,
        coreVirtueMasterySkills:coreVirtue==='Maestria'?[masterySkillA,masterySkillB]:undefined,
        primaryProficiency,secondaryProficiency,
        previousExperienceSkillRanks:Object.fromEntries(CANONICAL_SKILLS.map(name=>[name,skillTarget(name)])),
        previousExperienceProficiencyRanks:Object.fromEntries(ALL_PROFICIENCIES.map(name=>[name,proficiencyTarget(name)])),
        initialReward,
        initialRewardWeaponGroup:weaponReward?rewardWeaponGroup:undefined,
      };
      await onCreate(options);
      setMessage('Herói criado por Trilhas de Personagens com todas as escolhas registradas.');
    }catch(error){setMessage(error instanceof Error?error.message:String(error));}
  };

  return <section className="mb-4 rounded-lg border border-amber-900/70 bg-zinc-950/85 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-amber-500">Suplemento 2e</div>
    <h2 className="mt-1 text-sm font-black uppercase text-amber-100">Trilhas de Personagens</h2>
    <p className="mt-1 text-[11px] text-slate-400">Nenhuma escolha mecânica principal é sorteada silenciosamente. Experiência Prévia, Proficiências, Virtude e Recompensa Inicial são confirmadas pelo jogador e resolvidas localmente, sem RAG.</p>

    <div className="mt-3 grid gap-2 md:grid-cols-2">
      <div className="flex items-end gap-2">
        <label className="flex-1 text-[10px] text-slate-400">Cultura
          <select value={cultureId} onChange={e=>changeCulture(e.target.value as CharacterPathCultureId)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
            {CHARACTER_PATH_CULTURES.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}
          </select>
        </label>
        <button type="button" onClick={rollCulture} className="rounded border border-amber-800 px-3 py-2 text-xs">1d6</button>
      </div>
      {cultureId==='rangers'&&<label className="text-[10px] text-slate-400">Reis dos Homens · Atributo que recebe +1
        <select value={culturalAttributeBonus} onChange={e=>setCulturalAttributeBonus(e.target.value as 'strength'|'heart'|'wits')} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          <option value="strength">Força</option><option value="heart">Coração</option><option value="wits">Astúcia</option>
        </select>
      </label>}
      <label className="text-[10px] text-slate-400">
        <span className="flex items-center gap-1">Chamado <CreationInfoButton entry={CALLING_HELP[callingName]}/></span>
        <select value={callingName} onChange={e=>{setCallingName(e.target.value);setCallingSkillA('');setCallingSkillB('');setMasterySkillA('');setMasterySkillB('');}} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">{CHARACTER_PATH_CALLINGS.map(item=><option key={item.name}>{item.name}</option>)}</select>
      </label>
      <div className="flex items-end gap-2">
        <label className="flex-1 text-[10px] text-slate-400">
          <span className="flex items-center gap-1">Antecedente <CreationInfoButton title={background?.title} summary={'Define os Atributos, uma Perícia Favorecida e duas Características Distintivas desta origem.'} use={'Use para visualizar quem o Herói era antes da aventura.'} fits={(background?.distinctiveFeatures||[]).join(' · ')} example={'Atributos: '+(background?.attributes||[]).join(' / ')+' · Favorecida: '+(background?.favouredSkill||'—')}/></span>
          <select value={backgroundId} onChange={e=>setBackgroundId(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
            {backgrounds.map(item=><option key={item.id} value={item.id}>{item.die}. {item.title}</option>)}
          </select>
        </label>
        <button type="button" onClick={rollBackground} className="rounded border border-amber-800 px-3 py-2 text-xs">1d6</button>
      </div>
      <div className="flex items-end gap-2">
        <label className="flex-1 text-[10px] text-slate-400">
          <span className="flex items-center gap-1">Evento Importante <CreationInfoButton title={event.title} summary="Um acontecimento marcante que altera a criação antes da Experiência Prévia." use="Leia os modificadores abaixo antes de confirmar; eles fazem parte da ficha final." fits="Qualquer conceito coerente com o acontecimento." example={'PE: '+(event.previousExperienceDelta?(event.previousExperienceDelta>0?'+':'')+event.previousExperienceDelta:'normal')+(event.enduranceMaxDelta?' · Resistência '+event.enduranceMaxDelta:'')+(event.hopeMaxDelta?' · Esperança '+event.hopeMaxDelta:'')}/></span>
          <select value={feat} onChange={e=>setFeat(Number(e.target.value))} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
            {IMPORTANT_EVENTS.map(item=><option key={item.id} value={item.feat}>{item.feat===11?'Olho':item.feat===12?'Gandalf':item.feat}. {item.title}</option>)}
          </select>
        </label>
        <button type="button" onClick={rollEvent} className="rounded border border-violet-800 px-3 py-2 text-xs">d12</button>
      </div>
      <div className="rounded border border-slate-800 p-2 text-[11px] text-slate-300">
        <strong>{background?.title}</strong><br/>
        Força {background?.attributes[0]} · Coração {background?.attributes[1]} · Astúcia {background?.attributes[2]}<br/>
        Favorecida: {background?.favouredSkill}<br/>
        {background?.distinctiveFeatures.join(' · ')}
      </div>
    </div>

    <div className="mt-3 grid grid-cols-2 gap-2 rounded border border-slate-800 p-2">
      <label className="text-[10px] text-slate-400">
        <span className="flex items-center gap-1">Primeira Perícia do Chamado {callingSkillA&&<CreationInfoButton entry={skillHelp(callingSkillA)}/>}</span>
        <select value={callingSkillA} onChange={e=>setCallingSkillA(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Escolha uma</option>{callingSkillOptions.filter(skill=>skill!==callingSkillB).map(skill=><option key={skill}>{skill}</option>)}</select>
      </label>
      <label className="text-[10px] text-slate-400">
        <span className="flex items-center gap-1">Segunda Perícia do Chamado {callingSkillB&&<CreationInfoButton entry={skillHelp(callingSkillB)}/>}</span>
        <select value={callingSkillB} onChange={e=>setCallingSkillB(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Escolha outra</option>{callingSkillOptions.filter(skill=>skill!==callingSkillA).map(skill=><option key={skill}>{skill}</option>)}</select>
      </label>
    </div>

    <div className="mt-3 grid gap-2 rounded border border-blue-900/60 bg-blue-950/10 p-3 md:grid-cols-2">
      <label className="text-[10px] text-slate-300">Proficiência principal · começa em 2
        <select value={primaryProficiency} onChange={e=>choosePrimary(e.target.value as Proficiency)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {allowedPrimary.map(group=><option key={group}>{group}</option>)}
        </select>
      </label>
      <label className="text-[10px] text-slate-300">Outra Proficiência · começa em 1
        <select value={secondaryProficiency} onChange={e=>chooseSecondary(e.target.value as Proficiency)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {ALL_PROFICIENCIES.filter(group=>group!==primaryProficiency).map(group=><option key={group}>{group}</option>)}
        </select>
      </label>
    </div>

    {event.chooseTnImprovement&&<label className="mt-3 block text-[10px] text-slate-400">NA que será reduzido em 1
      <select value={tnChoice} onChange={e=>setTnChoice(e.target.value as any)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
        <option value="">Automático · escolha legal</option>
        {(['strength','heart','wits'] as const).filter(a=>a!==event.fixedTnModifier?.attribute).map(a=><option key={a} value={a}>{attrLabel[a]}</option>)}
      </select>
    </label>}

    {Boolean(event.addFavouredSkills)&&<label className="mt-3 block text-[10px] text-slate-400">Perícia adicional Favorecida
      <div className="mt-1 flex items-center gap-2"><select value={addFav} onChange={e=>setAddFav(e.target.value)} className="w-full rounded border border-slate-700 bg-black p-2 text-xs">
        <option value="">Automático · escolha legal</option>{CANONICAL_SKILLS.map(skill=><option key={skill}>{skill}</option>)}
      </select>{addFav&&<CreationInfoButton entry={skillHelp(addFav)}/>}</div>
    </label>}

    {Boolean(event.removeFavouredSkills)&&<div className="mt-3">
      <div className="text-[10px] text-slate-400">Perícias que deixam de ser Favorecidas · escolha {event.removeFavouredSkills} ou deixe automático</div>
      <div className="mt-1 grid grid-cols-2 gap-1 sm:grid-cols-3">{CANONICAL_SKILLS.map(skill=><label key={skill} className="flex items-center gap-1 text-[10px]"><input type="checkbox" checked={removeFav.includes(skill)} disabled={!removeFav.includes(skill)&&removeFav.length>=Number(event.removeFavouredSkills||0)} onChange={e=>setRemoveFav(cur=>e.target.checked?[...cur,skill]:cur.filter(x=>x!==skill))}/>{skill}<CreationInfoButton entry={skillHelp(skill)}/></label>)}</div>
    </div>}

    <div className="mt-3 grid gap-2 rounded border border-cyan-900/60 bg-cyan-950/10 p-2 md:grid-cols-2">
      <label className="text-[10px] text-slate-300">
        <span className="flex items-center gap-1">Virtude Geral inicial · escolha do jogador <CreationInfoButton entry={VIRTUE_HELP[coreVirtue]}/></span>
        <select value={coreVirtue} onChange={e=>setCoreVirtue(e.target.value as (typeof CORE_COMMON_VIRTUES)[number])} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {CORE_COMMON_VIRTUES.map(virtue=><option key={virtue}>{virtue}</option>)}
        </select>
      </label>
      {coreVirtue==='Proficiência'&&<label className="text-[10px] text-slate-300">Atributo para NA −1
        <select value={coreVirtueAttribute} onChange={e=>setCoreVirtueAttribute(e.target.value as CoreAttribute)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="strength">Força</option><option value="heart">Coração</option><option value="wits">Astúcia</option></select>
      </label>}
      {coreVirtue==='Maestria'&&<>
        <label className="text-[10px] text-slate-300"><span className="flex items-center gap-1">Primeira Perícia para Maestria {masterySkillA&&<CreationInfoButton entry={skillHelp(masterySkillA)}/>}</span>
          <select value={masterySkillA} onChange={e=>setMasterySkillA(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Escolha uma Perícia</option>{masteryOptions.filter(skill=>skill!==masterySkillB).map(skill=><option key={skill}>{skill}</option>)}</select>
        </label>
        <label className="text-[10px] text-slate-300"><span className="flex items-center gap-1">Segunda Perícia para Maestria {masterySkillB&&<CreationInfoButton entry={skillHelp(masterySkillB)}/>}</span>
          <select value={masterySkillB} onChange={e=>setMasterySkillB(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Escolha outra Perícia</option>{masteryOptions.filter(skill=>skill!==masterySkillA).map(skill=><option key={skill}>{skill}</option>)}</select>
        </label>
      </>}
    </div>

    <div className="mt-3 rounded border border-amber-900/60 bg-amber-950/10 p-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="text-xs font-black text-amber-200">Experiência Prévia · {xpSpent}/{xpBudget} pontos</div>
        <div className={'text-[11px] font-bold '+(xpRemaining===0?'text-emerald-300':xpRemaining>0?'text-amber-300':'text-red-300')}>{xpRemaining===0?'Distribuição completa':xpRemaining>0?'Restam '+xpRemaining:'Excedeu em '+Math.abs(xpRemaining)}</div>
      </div>
      <p className="mt-1 text-[10px] text-slate-400">Escolha os aumentos. O VTT não distribui mais estes pontos automaticamente.</p>
      <details className="mt-2" open>
        <summary className="cursor-pointer text-[11px] font-bold text-amber-300">Perícias</summary>
        <div className="mt-2 grid gap-1 sm:grid-cols-2">{CANONICAL_SKILLS.map(name=>{
          const base=Number(baseSkillRanks[name]||0),target=skillTarget(name);
          return <div key={name} className="flex items-center gap-2 rounded border border-slate-800 px-2 py-1">
            <CreationInfoButton entry={skillHelp(name)}/>
            <span className="min-w-0 flex-1 text-[10px] text-slate-200">{name}<span className="ml-1 text-slate-500">base {base}</span></span>
            <select value={target} onChange={e=>setXpSkillRanks(cur=>({...cur,[name]:Number(e.target.value)}))} className="rounded border border-slate-700 bg-black px-1 py-1 text-[10px]">
              {Array.from({length:5-base},(_,i)=>base+i).map(rank=><option key={rank} value={rank}>{rank} · custo {advancementCost(base,rank,SKILL_COST)}</option>)}
            </select>
          </div>;
        })}</div>
      </details>
      <details className="mt-2">
        <summary className="cursor-pointer text-[11px] font-bold text-amber-300">Proficiências de Combate</summary>
        <div className="mt-2 grid gap-1 sm:grid-cols-2">{ALL_PROFICIENCIES.map(name=>{
          const base=Number(baseProfRanks[name]||0),target=proficiencyTarget(name);
          return <div key={name} className="flex items-center gap-2 rounded border border-slate-800 px-2 py-1">
            <span className="flex-1 text-[10px] text-slate-200">{name}<span className="ml-1 text-slate-500">base {base}</span></span>
            <select value={target} onChange={e=>setXpProficiencyRanks(cur=>({...cur,[name]:Number(e.target.value)}))} className="rounded border border-slate-700 bg-black px-1 py-1 text-[10px]">
              {Array.from({length:4-base},(_,i)=>base+i).map(rank=><option key={rank} value={rank}>{rank} · custo {advancementCost(base,rank,PROFICIENCY_COST)}</option>)}
            </select>
          </div>;
        })}</div>
      </details>
    </div>

    <div className="mt-3 grid gap-2 rounded border border-fuchsia-900/60 bg-fuchsia-950/10 p-3 md:grid-cols-2">
      <label className="text-[10px] text-slate-300">
        <span className="flex items-center gap-1">Recompensa Inicial · escolha obrigatória <CreationInfoButton entry={REWARD_HELP[initialReward]}/></span>
        <select value={initialReward} onChange={e=>setInitialReward(e.target.value as (typeof INITIAL_REWARDS)[number])} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {INITIAL_REWARDS.map(reward=><option key={reward}>{reward}</option>)}
        </select>
      </label>
      {weaponReward?<label className="text-[10px] text-slate-300">Arma que recebe a Recompensa
        <select value={rewardWeaponGroup} onChange={e=>setRewardWeaponGroup(e.target.value as Proficiency)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {[primaryProficiency,secondaryProficiency].filter((value,index,array)=>array.indexOf(value)===index).map(group=><option key={group}>{group}</option>)}
        </select>
      </label>:<div className="rounded border border-slate-800 p-2 text-[10px] text-slate-400">{initialReward==='Reforçado'?'Aplicada ao escudo inicial.':'Aplicada à armadura inicial.'}</div>}
    </div>

    <div className="mt-3 rounded border border-slate-800 bg-black/30 p-2 text-[11px]">
      <strong>{event.title}</strong>
      <span> · PE {event.previousExperienceDelta ? (event.previousExperienceDelta>0?'+':'')+String(event.previousExperienceDelta) : 'normal'}</span>
      {event.fellowshipDelta ? <span> · Sociedade {event.fellowshipDelta>0?'+':''}{event.fellowshipDelta}</span> : null}
      {event.hopeMaxDelta ? <span> · Esperança {event.hopeMaxDelta>0?'+':''}{event.hopeMaxDelta}</span> : null}
      {event.enduranceMaxDelta ? <span> · Resistência {event.enduranceMaxDelta>0?'+':''}{event.enduranceMaxDelta}</span> : null}
      {event.parryDelta ? <span> · Bloqueio {event.parryDelta>0?'+':''}{event.parryDelta}</span> : null}
    </div>
    <button disabled={busy||!callingSkillA||!callingSkillB||(coreVirtue==='Maestria'&&(!masterySkillA||!masterySkillB))||xpSpent!==xpBudget||(weaponReward&&!rewardWeaponGroup)} onClick={()=>void create()} className="mt-3 w-full rounded border border-amber-700 bg-amber-950/30 px-3 py-3 text-xs font-black text-amber-100 disabled:opacity-40">Criar Herói por Trilhas</button>
    {message&&<div className="mt-2 text-xs text-slate-300">{message}</div>}
  </section>;
};
