import React, { useEffect, useMemo, useState } from 'react';
import { adviseGollum, type GuideState } from '../lib/gollumGuidance';
import type { AppTab } from './MainNavigation';

/**
 * Gollum is a contextual interface companion, NOT a world NPC.
 * It receives only public, player-visible state and never calls GM commands.
 */
interface Props extends GuideState {
  onNavigate: (tab: AppTab) => void;
  onSpeak?: (text: string) => void;
}
const prefKey=(id:string)=>'braseiro.gollumGuide.v1.'+id;
type Saved={enabled:boolean;seen:string[]};
const defaults:Saved={enabled:true,seen:[]};
const read=(id:string):Saved=>{try{const v=JSON.parse(localStorage.getItem(prefKey(id))||'null');return v&&Array.isArray(v.seen)?{enabled:v.enabled!==false,seen:v.seen.map(String).slice(-120)}:defaults;}catch{return defaults;}};

export function GollumGuide(props:Props) {
  const [open,setOpen]=useState(false);
  const [saved,setSaved]=useState<Saved>(()=>read(props.campaignId));
  const advice=useMemo(()=>adviseGollum(props),[props.tab,props.hasHero,props.busy,props.pendingTest,props.pendingRoll,props.journeyPending]);
  useEffect(()=>{setSaved(read(props.campaignId));setOpen(false);},[props.campaignId]);
  const update=(newValue:Saved)=>{setSaved(newValue);try{localStorage.setItem(prefKey(props.campaignId),JSON.stringify(newValue));}catch{}};
  const unseen=saved.enabled && advice.level==='important' && !saved.seen.includes(advice.id);
  const mark=()=>update({...saved,seen:[...saved.seen.filter(x=>x!==advice.id),advice.id].slice(-120)});
  return (
    <div className="fixed bottom-[max(16px,env(safe-area-inset-bottom))] right-3 z-[85] font-sans pointer-events-none" data-testid="gollum-guide">
      {open && (
        <section role="dialog" aria-label="Gollum — guia contextual" className="pointer-events-auto mb-2 w-[min(90vw,360px)] max-h-[68dvh] overflow-y-auto rounded-xl border border-[#726a4c] bg-[#111b18]/95 p-4 text-[#efe3c9] shadow-2xl backdrop-blur-lg">
          <div className="flex items-center justify-between gap-2">
            <strong className="text-base text-[#d5dfab]">Gollum <span className="text-xs font-normal text-[#b5bba4]">· guia da interface</span></strong>
            <button type="button" aria-label="Fechar guia" onClick={()=>setOpen(false)} className="rounded px-2 py-1 text-lg text-[#f0e5ca]">×</button>
          </div>
          <p className="mt-2 text-sm font-semibold">{advice.title}</p>
          <p className="my-2 text-sm italic leading-relaxed">“{advice.text}”</p>
          <details className="rounded-lg border border-[#455b50] p-2 text-xs">
            <summary className="cursor-pointer font-semibold text-[#bbcfb0]">Por que Gollum está avisando?</summary>
            <p className="mt-2 leading-relaxed">{advice.reason}</p>
            <p className="mt-1 text-[#98af9e]">Fonte: {advice.source}</p>
            <p className="mt-1 text-[#98af9e]">Aviso contextual; nunca uma decisão automática do Mestre.</p>
          </details>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            {advice.tab && advice.action && <button type="button" onClick={()=>{mark();props.onNavigate(advice.tab as AppTab);setOpen(false);}} className="rounded border border-[#65806d] bg-[#283e35] px-3 py-2 text-xs font-semibold">{advice.action}</button>}
            {props.onSpeak && <button type="button" onClick={()=>props.onSpeak?.(advice.text+' '+advice.reason)} className="rounded border border-[#697b74] px-3 py-2 text-xs">Ouvir conselho</button>}
            <button type="button" onClick={()=>{mark();setOpen(false);}} className="rounded border border-[#697b74] px-3 py-2 text-xs">Entendi</button>
          </div>
          <label className="mt-4 flex cursor-pointer items-start gap-2 text-xs text-[#c6c5b4]">
            <input type="checkbox" className="mt-0.5" checked={saved.enabled} onChange={e=>update({...saved,enabled:e.target.checked})}/>
            Alertas espontâneos quando uma ação estiver pendente
          </label>
          <p className="mt-2 text-[11px] text-[#b3b4a4]">Sempre disponível ao toque. Sem spoilers, rolagens, decisões ou alteração dos salvamentos.</p>
        </section>
      )}
      <button type="button" aria-label={unseen?'Gollum tem um aviso importante':'Abrir guia Gollum'} title="Gollum — guia de O Um Anel" onClick={()=>setOpen(v=>!v)}
        className="pointer-events-auto relative flex h-[52px] w-[52px] items-center justify-center rounded-full border-2 border-[#7b9568] bg-[#1a2b23] shadow-lg shadow-black/80 focus-visible:outline focus-visible:outline-2 focus-visible:outline-cyan-400">
        <svg width="37" height="39" viewBox="0 0 40 42" fill="none" aria-hidden="true">
          <path d="M9 13 1 7 7 24l3 12 19 0 4-13 6-16-10 5C25 2 13 3 9 13Z" fill="#697c5c" stroke="#c1cf94" strokeWidth="1.5"/>
          <ellipse cx="14" cy="20" rx="5" ry="4" fill="#d5d0a6"/><ellipse cx="27" cy="20" rx="5" ry="4" fill="#d5d0a6"/>
          <ellipse cx="15" cy="20" rx="1.8" ry="2.8" fill="#273a2d"/><ellipse cx="26" cy="20" rx="1.8" ry="2.8" fill="#273a2d"/>
          <path d="M17 31Q21 34 24 30" stroke="#233a32" strokeWidth="2" strokeLinecap="round"/>
        </svg>
        {unseen && <span className="absolute -right-1 -top-1 grid h-5 w-5 place-items-center rounded-full border border-black bg-amber-300 text-sm font-black text-black" aria-label="Aviso pendente">!</span>}
      </button>
    </div>
  );
}
