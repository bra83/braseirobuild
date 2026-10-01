import React, { useState } from 'react';
import { Info, X } from 'lucide-react';
import type { CreationHelpEntry } from '../lib/characterCreationHelp';

type Props = {
  entry?: CreationHelpEntry | null;
  title?: string;
  summary?: string;
  use?: string;
  fits?: string;
  example?: string;
  mechanics?: string;
  className?: string;
};

export const CreationInfoButton:React.FC<Props>=({entry,title,summary,use,fits,example,mechanics,className=''})=>{
  const [open,setOpen]=useState(false);
  const data:CreationHelpEntry={
    title:entry?.title||title||'Ajuda',
    summary:entry?.summary||summary||'',
    use:entry?.use||use||'',
    fits:entry?.fits||fits||'',
    example:entry?.example||example||'',
    mechanics:entry?.mechanics||mechanics,
  };
  return <>
    <button
      type="button"
      onClick={event=>{event.preventDefault();event.stopPropagation();setOpen(true);}}
      className={'inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full border border-cyan-700/70 bg-cyan-950/30 text-cyan-300 active:bg-cyan-900/60 '+className}
      aria-label={'Informações sobre '+data.title}
      title={'Informações sobre '+data.title}
    ><Info className="h-3.5 w-3.5"/></button>
    {open&&<div className="fixed inset-0 z-[300] flex items-end justify-center bg-black/75 p-3 sm:items-center" onClick={()=>setOpen(false)}>
      <section className="max-h-[82vh] w-full max-w-lg overflow-y-auto rounded-xl border border-cyan-800 bg-[#050b14] p-4 text-left shadow-2xl" onClick={event=>event.stopPropagation()}>
        <div className="flex items-start justify-between gap-3">
          <div><div className="text-[10px] font-bold uppercase tracking-[.16em] text-cyan-500">Ajuda de criação · local</div><h3 className="mt-1 text-lg font-black text-cyan-100">{data.title}</h3></div>
          <button type="button" onClick={()=>setOpen(false)} className="rounded border border-slate-700 p-2 text-slate-300" aria-label="Fechar"><X className="h-4 w-4"/></button>
        </div>
        {data.summary&&<p className="mt-3 text-sm leading-relaxed text-slate-200">{data.summary}</p>}
        <div className="mt-3 space-y-3 text-xs leading-relaxed text-slate-300">
          {data.use&&<p><b className="text-cyan-300">Quando entra em jogo:</b> {data.use}</p>}
          {data.fits&&<p><b className="text-cyan-300">Combina com:</b> {data.fits}</p>}
          {data.example&&<p><b className="text-cyan-300">Exemplo:</b> {data.example}</p>}
          {data.mechanics&&<p className="rounded border border-amber-900/60 bg-amber-950/20 p-2"><b className="text-amber-300">Efeito mecânico:</b> {data.mechanics}</p>}
        </div>
        <p className="mt-4 border-t border-slate-800 pt-3 text-[10px] text-slate-500">Esta ajuda é embarcada no aplicativo. Não consulta Gemini, PDF ou RAG durante a criação.</p>
      </section>
    </div>}
  </>;
};
