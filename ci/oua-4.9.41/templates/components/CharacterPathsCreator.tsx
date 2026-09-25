import React, { useMemo, useState } from 'react';
import { backgroundsForCulture, CHARACTER_PATH_BACKGROUNDS, IMPORTANT_EVENTS, type CharacterPathCultureId } from '../lib/characterPaths';
import { CHARACTER_PATH_CULTURES, type CharacterPathCreationOptions } from '../lib/characterFactory';
import { CANONICAL_SKILLS } from '../lib/skillRegistry';
import { secureRoll } from '../lib/dice';

type Props = {
  rulesMode: 'standard'|'strider';
  busy?: boolean;
  onCreate: (options: CharacterPathCreationOptions) => Promise<void>;
};

const attrLabel:Record<string,string>={strength:'Força',heart:'Coração',wits:'Astúcia'};

export const CharacterPathsCreator:React.FC<Props>=({rulesMode,busy,onCreate})=>{
  const [cultureId,setCultureId]=useState<CharacterPathCultureId>('bardings');
  const backgrounds=useMemo(()=>backgroundsForCulture(cultureId),[cultureId]);
  const [backgroundId,setBackgroundId]=useState('bardings-1-hammer-anvil');
  const [feat,setFeat]=useState(1);
  const [tnChoice,setTnChoice]=useState<''|'strength'|'heart'|'wits'>('');
  const [addFav,setAddFav]=useState('');
  const [removeFav,setRemoveFav]=useState<string[]>([]);
  const [message,setMessage]=useState('');
  const event=IMPORTANT_EVENTS.find(item=>item.feat===feat)||IMPORTANT_EVENTS[0];
  const background=CHARACTER_PATH_BACKGROUNDS.find(item=>item.id===backgroundId)||backgrounds[0];

  const changeCulture=(id:CharacterPathCultureId)=>{
    setCultureId(id);
    const first=backgroundsForCulture(id)[0];
    if(first)setBackgroundId(first.id);
  };
  const rollBackground=()=>{const die=secureRoll(1,6);const found=backgrounds.find(item=>item.die===die);if(found)setBackgroundId(found.id);};
  const rollEvent=()=>setFeat(secureRoll(1,12));

  const create=async()=>{
    setMessage('');
    try{
      const options:CharacterPathCreationOptions={
        cultureId,backgroundId,importantEventFeat:feat,rulesMode,
        tnImprovementAttribute:tnChoice||undefined,
        addedFavouredSkills:addFav?[addFav]:undefined,
        removedFavouredSkills:removeFav.length?removeFav:undefined,
      };
      await onCreate(options);
      setMessage('Herói criado por Trilhas de Personagens.');
    }catch(error){setMessage(error instanceof Error?error.message:String(error));}
  };

  return <section className="mb-4 rounded-lg border border-amber-900/70 bg-zinc-950/85 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-amber-500">Suplemento 2e</div>
    <h2 className="mt-1 text-sm font-black uppercase text-amber-100">Trilhas de Personagens</h2>
    <p className="mt-1 text-[11px] text-slate-400">Antecedente e Evento Importante são aplicados antes da Experiência Prévia. Escolhas deixadas em automático são registradas no histórico de criação.</p>

    <div className="mt-3 grid gap-2 md:grid-cols-2">
      <label className="text-[10px] text-slate-400">Cultura
        <select value={cultureId} onChange={e=>changeCulture(e.target.value as CharacterPathCultureId)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {CHARACTER_PATH_CULTURES.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}
        </select>
      </label>
      <div className="flex items-end gap-2">
        <label className="flex-1 text-[10px] text-slate-400">Antecedente
          <select value={backgroundId} onChange={e=>setBackgroundId(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
            {backgrounds.map(item=><option key={item.id} value={item.id}>{item.die}. {item.title}</option>)}
          </select>
        </label>
        <button type="button" onClick={rollBackground} className="rounded border border-amber-800 px-3 py-2 text-xs">1d6</button>
      </div>
      <div className="flex items-end gap-2">
        <label className="flex-1 text-[10px] text-slate-400">Evento Importante
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

    {event.chooseTnImprovement&&<label className="mt-3 block text-[10px] text-slate-400">NA que será reduzido em 1
      <select value={tnChoice} onChange={e=>setTnChoice(e.target.value as any)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
        <option value="">Automático · escolha legal</option>
        {(['strength','heart','wits'] as const).filter(a=>a!==event.fixedTnModifier?.attribute).map(a=><option key={a} value={a}>{attrLabel[a]}</option>)}
      </select>
    </label>}

    {Boolean(event.addFavouredSkills)&&<label className="mt-3 block text-[10px] text-slate-400">Perícia adicional Favorecida
      <select value={addFav} onChange={e=>setAddFav(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs">
        <option value="">Automático · escolha legal</option>{CANONICAL_SKILLS.map(skill=><option key={skill}>{skill}</option>)}
      </select>
    </label>}

    {Boolean(event.removeFavouredSkills)&&<div className="mt-3">
      <div className="text-[10px] text-slate-400">Perícias que deixam de ser Favorecidas · escolha {event.removeFavouredSkills} ou deixe automático</div>
      <div className="mt-1 grid grid-cols-3 gap-1">{CANONICAL_SKILLS.map(skill=><label key={skill} className="flex items-center gap-1 text-[10px]"><input type="checkbox" checked={removeFav.includes(skill)} disabled={!removeFav.includes(skill)&&removeFav.length>=Number(event.removeFavouredSkills||0)} onChange={e=>setRemoveFav(cur=>e.target.checked?[...cur,skill]:cur.filter(x=>x!==skill))}/>{skill}</label>)}</div>
    </div>}

    <div className="mt-3 rounded border border-slate-800 bg-black/30 p-2 text-[11px]">
      <strong>{event.title}</strong>
      <span> · PE {event.previousExperienceDelta ? (event.previousExperienceDelta>0?'+':'')+String(event.previousExperienceDelta) : 'normal'}</span>
      {event.fellowshipDelta ? <span> · Sociedade {event.fellowshipDelta>0?'+':''}{event.fellowshipDelta}</span> : null}
      {event.hopeMaxDelta ? <span> · Esperança {event.hopeMaxDelta>0?'+':''}{event.hopeMaxDelta}</span> : null}
      {event.enduranceMaxDelta ? <span> · Resistência {event.enduranceMaxDelta>0?'+':''}{event.enduranceMaxDelta}</span> : null}
      {event.parryDelta ? <span> · Bloqueio {event.parryDelta>0?'+':''}{event.parryDelta}</span> : null}
    </div>
    <button disabled={busy} onClick={()=>void create()} className="mt-3 w-full rounded border border-amber-700 bg-amber-950/30 px-3 py-3 text-xs font-black text-amber-100 disabled:opacity-40">Criar Herói por Trilhas</button>
    {message&&<div className="mt-2 text-xs text-slate-300">{message}</div>}
  </section>;
};
