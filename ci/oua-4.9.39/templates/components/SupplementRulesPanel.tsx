import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';
import { secureRoll } from '../lib/dice';
import { virtuesForCulture } from '../lib/supplementRules';
import { hasVirtue, hunterResolveRecovery } from '../lib/rules/supplementMechanics';
import {
  resolveNarrationTable,
  striderFortune,
  striderLore,
  striderMilestoneAward,
  striderMisfortune,
  type StriderLikelihood,
  type StriderMilestone,
} from '../lib/rules/striderMode';

type Props = {
  masterState: MasterState;
  characters: Character[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => void | Promise<void>;
};

const milestones:Array<{id:StriderMilestone;label:string}> = [
  {id:'accept-patron-mission',label:'Aceitar missão de Patrono'},
  {id:'notable-personal-goal',label:'Alcançar objetivo pessoal notável'},
  {id:'complete-patron-mission',label:'Completar missão de Patrono'},
  {id:'significant-journey',label:'Completar Jornada significativa'},
  {id:'notable-journey-encounter',label:'Encontro Notável durante Jornada'},
  {id:'significant-discovery',label:'Descoberta/local significativo'},
  {id:'difficult-obstacle',label:'Superar obstáculo difícil'},
  {id:'council',label:'Participar de Conselho'},
  {id:'dangerous-combat',label:'Sobreviver a combate perigoso'},
  {id:'revelation-episode',label:'Enfrentar episódio de Revelação'},
];

export const SupplementRulesPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [heroId,setHeroId]=useState(characters[0]?.id||'');
  const [likelihood,setLikelihood]=useState<StriderLikelihood>('even');
  const [oracle,setOracle]=useState('');
  const [milestone,setMilestone]=useState<StriderMilestone>('accept-patron-mission');
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const hero=characters.find(item=>item.id===heroId)||characters[0];
  const virtues=useMemo(()=>hero?virtuesForCulture(hero.sheet.identity.culture):[],[hero]);
  const mode=masterState.company.rulesMode||'standard';

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };

  const rollNarration=()=>{
    const feat=secureRoll(1,12);
    const result=resolveNarrationTable(feat,likelihood);
    setOracle('Narração · Façanha '+feat+' · '+result.label);
  };
  const rollLore=()=>{
    const feat=secureRoll(1,12), die=secureRoll(1,6), result=striderLore(feat,die);
    setOracle('Conhecimento · '+String(feat)+'/'+String(die)+' · '+result.action+' · '+result.aspect+' · '+result.focus);
  };
  const rollFortune=(bad=false)=>{
    const feat=secureRoll(1,12);
    const result=bad?striderMisfortune(feat):striderFortune(feat);
    setOracle((bad?'Má Sorte':'Sorte')+' · Façanha '+feat+' · '+result.text);
    if(result.eyeDelta!==0) void run([{type:'adjust_eye_awareness',amount:Math.abs(result.eyeDelta),source:'dramatic',reason:result.text, direction:result.eyeDelta<0?'decrease':'increase'} as MasterCommand],bad?'Má Sorte do Modo Peregrino':'Sorte do Modo Peregrino');
  };

  return <section className="rounded-lg border border-amber-900/70 bg-zinc-950/85 p-4">
    <h2 className="text-sm font-black uppercase text-amber-200">Suplementos 2e · regras permanentes</h2>
    <div className="mt-3 grid grid-cols-2 gap-2 text-xs">
      <button disabled={busy||mode==='standard'} onClick={()=>void run([{type:'set_rules_mode',mode:'standard',reason:'Modo padrão 2e selecionado pelo jogador.'} as MasterCommand],'Modo padrão 2e')} className="rounded border border-slate-700 px-3 py-2 disabled:opacity-40">Padrão 2e</button>
      <button disabled={busy||mode==='strider'} onClick={()=>void run([{type:'set_rules_mode',mode:'strider',reason:'Modo Peregrino 2e selecionado pelo jogador.'} as MasterCommand],'Modo Peregrino 2e')} className="rounded border border-amber-700 px-3 py-2 text-amber-100 disabled:opacity-40">Modo Peregrino</button>
    </div>

    {mode==='strider'&&<div className="mt-3 rounded border border-amber-900/50 p-3">
      <div className="font-black text-amber-200">Modo Peregrino ativo</div>
      <div className="mt-1 text-[11px] text-slate-300">NA = 18 − Atributo · Sociedade base 3 · sem Foco de Sociedade · Peregrino Inspirado em Jornada · Jornada sem papéis · Postura Escaramuça habilitada.</div>
      <div className="mt-3 grid grid-cols-2 gap-2">
        <select value={likelihood} onChange={e=>setLikelihood(e.target.value as StriderLikelihood)} className="rounded border border-slate-700 bg-black p-2">
          <option value="certain">Certamente</option><option value="likely">Provável</option><option value="even">Mediano</option><option value="unlikely">Duvidoso</option><option value="unthinkable">Impensável</option>
        </select>
        <button onClick={rollNarration} className="rounded border border-amber-700 px-2 py-2">Tabela de Narração</button>
        <button onClick={rollLore} className="rounded border border-violet-700 px-2 py-2">Tabela de Conhecimento</button>
        <button onClick={()=>rollFortune(false)} className="rounded border border-emerald-700 px-2 py-2">Tabela de Sorte</button>
        <button onClick={()=>rollFortune(true)} className="rounded border border-red-800 px-2 py-2">Tabela de Má Sorte</button>
      </div>
      {oracle&&<div className="mt-2 rounded bg-black/50 p-2 text-[11px]">{oracle}</div>}
      <div className="mt-3 grid grid-cols-[1fr_auto] gap-2">
        <select value={milestone} onChange={e=>setMilestone(e.target.value as StriderMilestone)} className="rounded border border-slate-700 bg-black p-2">{milestones.map(item=><option key={item.id} value={item.id}>{item.label}</option>)}</select>
        <button disabled={busy||!hero} onClick={()=>{
          if(!hero)return;
          const award=striderMilestoneAward(milestone);
          void run([{type:'record_strider_milestone',targetCharacterId:hero.id,milestone,adventurePoints:award.adventurePoints,skillPoints:award.skillPoints,reason:'Marco de Experiência do Modo Peregrino.'} as MasterCommand],'Marco do Modo Peregrino');
        }} className="rounded border border-cyan-800 px-3 py-2">Registrar Marco</button>
      </div>
    </div>}

    <div className="mt-3">
      <select value={hero?.id||''} onChange={e=>setHeroId(e.target.value)} className="w-full rounded border border-slate-700 bg-black p-2 text-xs">{characters.map(c=><option key={c.id} value={c.id}>{c.sheet.identity.name||c.name}</option>)}</select>
      {hero&&<div className="mt-2 rounded border border-slate-800 p-3">
        <div className="text-[10px] uppercase text-slate-500">{hero.sheet.identity.culture}</div>
        <div className="mt-1 text-xs text-slate-300">{hero.sheet.other.culturalBlessing||'Bênção cultural não registrada.'}</div>
        {virtues.length>0&&<div className="mt-3 space-y-2">{virtues.map(v=><div key={v.id} className="rounded bg-black/35 p-2">
          <div className="font-bold text-amber-100">{v.name}{hasVirtue(hero.sheet,v.name)?' · ATIVA':''}</div>
          <div className="mt-1 text-[11px] text-slate-400">{v.summary}</div>
        </div>)}</div>}
        {hasVirtue(hero.sheet,'A Resolução do Caçador')&&<button disabled={busy||hunterResolveRecovery(hero.sheet)<=0} onClick={()=>void run([{type:'use_hunters_resolve',targetCharacterId:hero.id,reason:'Virtude A Resolução do Caçador.'} as MasterCommand],'A Resolução do Caçador')} className="mt-3 w-full rounded border border-emerald-800 px-3 py-2 text-xs disabled:opacity-40">Gastar 1 Esperança · recuperar {hunterResolveRecovery(hero.sheet)} Resistência</button>}
        {hasVirtue(hero.sheet,'Poder do Primogênito')&&masterState.activeMode==='combat'&&<button disabled={busy||hero.sheet.attributes.hopeCurrent<=0} onClick={()=>void run([{type:'arm_firstborn_power',targetCharacterId:hero.id,reason:'Poder do Primogênito preparado para cancelar a próxima Habilidade Mortal que gastar Ódio/Determinação.'} as MasterCommand],'Poder do Primogênito')} className="mt-2 w-full rounded border border-violet-800 px-3 py-2 text-xs disabled:opacity-40">Preparar Poder do Primogênito · 1 Esperança</button>}
      </div>}
    </div>
    {message&&<div className="mt-3 rounded border border-slate-700 bg-black/60 p-2 text-xs">{message}</div>}
  </section>;
};
