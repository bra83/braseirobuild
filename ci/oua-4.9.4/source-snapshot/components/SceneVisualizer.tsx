import React, { useState } from 'react';
import { Maximize2, X } from 'lucide-react';
interface SceneVisualizerProps { imageData: string | null; isLoading: boolean; }
export const SceneVisualizer: React.FC<SceneVisualizerProps> = ({ imageData, isLoading }) => {
  const [open,setOpen]=useState(false);
  return <>
    <div className="group relative mx-auto aspect-[9/16] w-full max-w-[360px] overflow-hidden rounded-md border-2 border-blue-900 bg-zinc-950 shadow-md ambient-glow">
      <div className="pointer-events-none absolute inset-0 z-20 bg-[linear-gradient(rgba(217,119,6,0.02)_1px,transparent_1px),linear-gradient(90deg,rgba(217,119,6,0.02)_1px,transparent_1px)] bg-[size:16px_16px]" />
      <div className="absolute left-2 top-2 z-20 h-4 w-4 border-l-2 border-t-2 border-blue-500 opacity-70"/><div className="absolute right-2 top-2 z-20 h-4 w-4 border-r-2 border-t-2 border-blue-500 opacity-70"/><div className="absolute bottom-2 left-2 z-20 h-4 w-4 border-b-2 border-l-2 border-blue-500 opacity-70"/><div className="absolute bottom-2 right-2 z-20 h-4 w-4 border-b-2 border-r-2 border-blue-500 opacity-70"/>
      {isLoading&&<div className="absolute inset-0 z-30 flex flex-col items-center justify-center bg-[#030914]/80"><div className="mb-4 h-12 w-12 animate-spin rounded-full border-4 border-blue-950 border-t-blue-500"/><p className="animate-pulse font-serif text-xs font-bold tracking-widest text-blue-500">PINTANDO A CENA...</p></div>}
      {imageData?<button type="button" onClick={()=>setOpen(true)} className="h-full w-full" title="Abrir imagem em tela cheia"><img src={imageData} alt="Visualização da cena" className={`h-full w-full object-cover transition-all duration-700 ${isLoading?'opacity-40':'opacity-100'} contrast-[1.05]`}/><span className="absolute right-3 top-3 z-30 rounded-full border border-blue-600 bg-black/70 p-2 text-blue-200 opacity-80"><Maximize2 className="h-4 w-4"/></span></button>:<div className="flex h-full w-full flex-col items-center justify-center bg-zinc-950 p-4 text-center"><p className="select-none font-serif text-2xl font-semibold tracking-widest text-blue-950 opacity-30">A TERRA-MÉDIA ESPERA</p><p className="mt-2 select-none font-serif text-xs italic text-blue-900/40">Inicie a crônica para pintar sua jornada...</p></div>}
      <div className="pointer-events-none absolute inset-0 z-10 bg-gradient-to-b from-transparent to-black/35"/>
    </div>
    {open&&imageData&&<div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/95 p-3" onClick={()=>setOpen(false)}><button onClick={()=>setOpen(false)} className="absolute right-4 top-4 rounded-full border border-blue-700 bg-black/70 p-3 text-blue-100"><X className="h-5 w-5"/></button><img src={imageData} alt="Cena em tela cheia" className="max-h-full max-w-full object-contain"/></div>}
  </>;
};
