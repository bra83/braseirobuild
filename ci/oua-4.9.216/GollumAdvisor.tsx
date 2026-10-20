import React,{useEffect,useMemo,useState} from 'react';
import {publicGollumTips,highlightedGollumTip,type GollumTip,type GollumVisibleState} from '../lib/gollumCounsel';
type AlertMode='important'|'all'|'off';
type Props={campaignId:string;visibleState:GollumVisibleState;onSpeak?:(text:string)=>Promise<void>|void;onAsk?:(question:string)=>Promise<{answer:string;sources:string[];uncertainty:string}>};
/** Read-only guide. No game commands or proactive voice playback. */
export function GollumAdvisor({campaignId,visibleState,onSpeak,onAsk}:Props){
 const [open,setOpen]=useState(false);
 const [dismissed,setDismissed]=useState<string[]>([]);
 const [mode,setMode]=useState<AlertMode>('important');
 const [speaking,setSpeaking]=useState(false);
 const [question,setQuestion]=useState('');
 const [asking,setAsking]=useState(false);
 const [reply,setReply]=useState<{answer:string;sources:string[];uncertainty:string}|null>(null);
 const key='braseiro.gollum-guide.v1:'+campaignId;
 useEffect(()=>{try{const v=JSON.parse(localStorage.getItem(key)||'{}');setDismissed(Array.isArray(v.dismissed)?v.dismissed:[]);setMode(['important','all','off'].includes(v.mode)?v.mode:'important');}catch{setDismissed([]);setMode('important')}},[key]);
 const tips=useMemo(()=>publicGollumTips(visibleState),[visibleState]);
 const highlighted=useMemo(()=>highlightedGollumTip(visibleState,dismissed,mode),[visibleState,dismissed,mode]);
 const save=(ids:string[],m:AlertMode)=>{try{localStorage.setItem(key,JSON.stringify({dismissed:ids,mode:m}));}catch{}};
 const acknowledge=(id:string)=>{const ids=[...new Set([...dismissed,id])];setDismissed(ids);save(ids,mode)};
 const speak=async (tip:GollumTip)=>{if(!onSpeak||speaking)return;setSpeaking(true);try{await onSpeak(tip.speech)}catch(e){console.warn('[GOLLUM] Falha de voz; texto permanece visível',e)}finally{setSpeaking(false)}};
 const ask=async()=>{if(!onAsk||asking||!question.trim())return;setAsking(true);setReply(null);try{setReply(await onAsk(question.trim()))}catch(e){setReply({answer:'Não consegui consultar os manuais agora. Tente novamente, precioso.',sources:[],uncertainty:e instanceof Error?e.message:'Falha de consulta'})}finally{setAsking(false)}};
 return <aside aria-label="Gollum, guia do aventureiro" className="fixed right-3 z-[65] pointer-events-auto" style={{bottom:'calc(env(safe-area-inset-bottom, 0px) + 88px)'}}>
  {open&&<section role="dialog" aria-label="Conselhos do Gollum" className="mb-2 max-h-[55dvh] w-[min(88vw,355px)] overflow-y-auto rounded-xl border border-[#8b7a52] bg-[#0b1010]/95 p-3 text-[#e9dbc0] shadow-2xl">
    <div className="flex items-center justify-between gap-3"><strong>Gollum · Guia</strong><button type="button" aria-label="Fechar guia" className="rounded border border-[#73654a] px-2 py-1" onClick={()=>setOpen(false)}>✕</button></div>
    <p className="mt-2 text-xs text-[#c5bb9d]">Só explico o que seu herói pode saber. Não revelo segredos, não faço escolhas nem rolo dados.</p>
    <label className="my-2 flex items-center gap-2 text-xs">Alertas <select aria-label="Frequência de alertas" className="rounded border border-[#73654a] bg-[#161b17] p-1" value={mode} onChange={e=>{const m=e.target.value as AlertMode;setMode(m);save(dismissed,m)}}><option value="important">Só importantes</option><option value="all">Todas as dicas</option><option value="off">Desativados</option></select></label>
    {onAsk&&<div className="my-3 rounded-lg border border-[#66573b] p-2 text-sm">
      <label htmlFor="gollum-question">Pergunte ao Gollum sobre as regras</label>
      <textarea id="gollum-question" className="mt-1 w-full rounded border border-[#73654a] bg-[#141916] p-2" rows={2} maxLength={500} placeholder="Como funciona um Conselho?" value={question} onChange={e=>setQuestion(e.target.value)}/>
      <button type="button" disabled={asking||!question.trim()} onClick={()=>void ask()} className="rounded border border-[#b49a67] px-3 py-1">{asking?'Consultando manuais...':'Perguntar'}</button>
      {reply&&<div role="status" className="mt-2 whitespace-pre-wrap text-[#f0e2ca]"><p>{reply.answer}</p>{reply.uncertainty&&<p className="mt-1 text-xs text-[#d4b17c]">Limitação: {reply.uncertainty}</p>}{reply.sources.length>0&&<p className="mt-2 text-xs text-[#aebaa9]">Fontes: {reply.sources.join(' · ')}</p>}</div>}
    </div>}
    {tips.map(t=><article key={t.id} className="mt-3 border-t border-[#66573b] pt-2">
      <div className="flex items-center gap-2"><span aria-hidden="true">{t.priority==='important'?'❗':'◈'}</span><strong>{t.heading}</strong></div>
      <p className="mt-1 italic">“{t.speech}”</p><p className="mt-2 text-sm">{t.reason}</p><small className="text-[#ada386]">Referência: {t.source}</small>
      <div className="mt-2 flex gap-2">{onSpeak&&<button type="button" disabled={speaking} onClick={()=>void speak(t)} className="rounded border border-[#73654a] px-2 py-1 text-xs">{speaking?'Falando...':'Ouvir'}</button>}<button type="button" onClick={()=>acknowledge(t.id)} className="rounded border border-[#73654a] px-2 py-1 text-xs">Já entendi</button></div>
    </article>)}
  </section>}
  <button type="button" aria-label={open?'Fechar guia Gollum':'Abrir guia Gollum'} aria-expanded={open} onClick={()=>setOpen(!open)} className="relative flex h-12 w-12 items-center justify-center rounded-full border border-[#aa945e] bg-[#23261d] text-xl shadow-lg" title="Gollum: guia de regras"><span aria-hidden="true">👁️</span>{!open&&highlighted&&<span className="absolute -right-1 -top-1 flex h-5 w-5 items-center justify-center rounded-full bg-[#f0c65f] text-xs font-bold text-[#17110a]">!</span>}</button>
 </aside>;
}
