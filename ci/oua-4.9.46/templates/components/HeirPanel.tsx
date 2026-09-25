import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';

type Props={
  masterState:MasterState;
  characters:Character[];
  onApplyCommands:(commands:MasterCommand[],reason:string)=>void|Promise<void>;
};

function eligibleInheritance(item:Character['sheet']['inventory'][number]):boolean{
  return (item.tags||[]).some(tag=>/^Recompensa:/i.test(tag))
    || ['magnificent-artefact','wondrous-item','famous-war-gear'].includes(String(item.magicalKind||''));
}

export const HeirPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [heroId,setHeroId]=useState(characters[0]?.id||'');
  const [heirName,setHeirName]=useState('');
  const [spend,setSpend]=useState(1);
  const [familySkill,setFamilySkill]=useState('');
  const [items,setItems]=useState<string[]>([]);

  const hero=characters.find(c=>c.id===heroId)||characters[0];
  const state=hero?.sheet.other.heirState;
  const pool=Math.max(0,Number(state?.previousExperiencePool)||0);
  const allowance=pool>=20?2:pool>=15?1:0;
  const favoured=hero?.sheet.skills.filter(skill=>skill.favorite)||[];
  const inheritables=useMemo(()=>hero?.sheet.inventory.filter(eligibleInheritance)||[],[hero]);
  const yule=masterState.fellowshipPhase.status==='active'&&Boolean(masterState.fellowshipPhase.isYule);

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };

  const educate=()=>hero&&void run([{
    type:'educate_heir',
    targetCharacterId:hero.id,
    heirName:state?.name||heirName||undefined,
    spend,
    reason:'Herói escolheu Educar um Herdeiro durante Yule.',
  } as MasterCommand],'Educar um Herdeiro');

  const setSkill=()=>hero&&familySkill&&void run([{
    type:'set_heir_family_skill',
    targetCharacterId:hero.id,
    skill:familySkill,
    reason:'Perícia Favorecida de herança familiar escolhida explicitamente.',
  } as MasterCommand],'definir Perícia de herança familiar');

  const create=()=>hero&&void run([{
    type:'create_heir_character',
    targetCharacterId:hero.id,
    inheritedItemIds:items,
    reason:'Antecessor aposentado/falecido; Herdeiro pronto assume a Companhia com as Heranças escolhidas.',
  } as MasterCommand],'criar Herdeiro e substituir antecessor');

  return <section className="mt-4 rounded-lg border border-orange-900/70 bg-zinc-950/85 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-orange-500">Livro Básico · Outras Aventuras</div>
    <h2 className="mt-1 text-sm font-black uppercase text-orange-100">Herdeiros e Heranças</h2>
    <p className="mt-1 text-[11px] text-slate-400">Em Yule, Educar um Herdeiro gasta 1–5 Tesouro e a mesma quantidade de Pontos de Aventura. A reserva chega a no máximo 20 e substitui os 10 PE normais quando o sucessor entra em jogo.</p>

    <select value={heroId} onChange={e=>{setHeroId(e.target.value);setItems([]);setFamilySkill('');}} className="mt-3 w-full rounded border border-slate-700 bg-black p-2 text-xs">
      {characters.map(c=><option key={c.id} value={c.id}>{c.sheet.identity.name||c.name}</option>)}
    </select>

    {hero&&<div className="mt-2 rounded border border-slate-800 bg-black/30 p-2 text-[10px]">
      Tesouro {hero.sheet.resources.treasure} · PA {hero.sheet.resources.adventurePoints} · reserva do Herdeiro {pool}/20 · {pool>=10?'pronto para substituir':'ainda não pronto'}
      {state?.name?<span> · Herdeiro: {state.name}</span>:null}
    </div>}

    <div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-orange-100">Educar em Yule</div>
      {!state?.name&&<input value={heirName} onChange={e=>setHeirName(e.target.value)} placeholder="Nome do Herdeiro" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>}
      <label className="mt-2 block text-[10px] text-slate-400">Gasto pareado · 1–5
        <input type="number" min={1} max={5} value={spend} onChange={e=>setSpend(Math.max(1,Math.min(5,Number(e.target.value)||1)))} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      </label>
      <button disabled={busy||!hero||!yule||pool>=20||(!state?.name&&!heirName.trim())} onClick={educate} className="mt-2 w-full rounded border border-orange-800 px-3 py-2 text-xs disabled:opacity-40">Gastar {spend} Tesouro + {spend} PA · Educar Herdeiro</button>
      {!yule&&<div className="mt-1 text-[9px] text-slate-500">Disponível apenas durante uma Fase de Sociedade em Yule.</div>}
    </div>

    {state?.name&&<div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-orange-100">Perícia Favorecida da família</div>
      <select value={familySkill||state.inheritedFavouredSkill||''} onChange={e=>setFamilySkill(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">
        <option value="">Escolher...</option>
        {favoured.map(skill=><option key={skill.name} value={skill.name}>{skill.name}</option>)}
      </select>
      <button disabled={busy||!(familySkill||state.inheritedFavouredSkill)} onClick={setSkill} className="mt-2 w-full rounded border border-orange-900 px-3 py-2 text-xs disabled:opacity-40">Salvar Perícia de família</button>
      <p className="mt-1 text-[9px] text-slate-500">A escolha deve ser uma Perícia já Favorecida no antecessor e se soma às Perícias Favorecidas normais do sucessor.</p>
    </div>}

    {state?.ready&&<div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-orange-100">Heranças · {allowance} item(ns) máximo</div>
      <p className="mt-1 text-[9px] text-slate-500">15+ pontos: 1 Herança. 20 pontos: até 2. Itens elegíveis mantêm todas as qualidades já adquiridas.</p>
      <div className="mt-2 space-y-1">{inheritables.length?inheritables.map(item=><label key={item.id} className="flex items-center gap-2 rounded border border-slate-800 px-2 py-1 text-[10px]">
        <input type="checkbox" checked={items.includes(item.id)} disabled={!items.includes(item.id)&&items.length>=allowance} onChange={e=>setItems(cur=>e.target.checked?[...cur,item.id]:cur.filter(id=>id!==item.id))}/>
        <span>{item.name}{item.magicalKind?' · Tesouro Mágico':''}{(item.tags||[]).filter(tag=>/^Recompensa:/i.test(tag)).length?' · Recompensado':''}</span>
      </label>):<div className="text-[10px] text-slate-500">Nenhuma Herança elegível no inventário.</div>}</div>
      <button disabled={busy||!state.inheritedFavouredSkill||items.length>allowance||Boolean(state.createdCharacterId)} onClick={create} className="mt-3 w-full rounded border border-red-900 bg-red-950/20 px-3 py-2 text-xs disabled:opacity-40">Aposentar/encerrar antecessor · criar Herdeiro</button>
      {state.createdCharacterId&&<div className="mt-1 text-[9px] text-emerald-500">Herdeiro já criado e ligado a este antecessor.</div>}
    </div>}

    {message&&<div className="mt-3 text-xs text-slate-300">{message}</div>}
  </section>;
};
