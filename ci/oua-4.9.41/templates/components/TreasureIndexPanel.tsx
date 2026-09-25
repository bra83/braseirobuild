import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState, TreasureIndexStateEntry } from '../lib/master/types';
import { ENCHANTED_QUALITIES, type Bane, type MagicalOrigin, type MagicalTreasureKind } from '../lib/rules/magicalTreasure';
import { CANONICAL_SKILLS } from '../lib/skillRegistry';

type Props={masterState:MasterState;characters:Character[];onApplyCommands:(commands:MasterCommand[],reason:string)=>void|Promise<void>};
const kinds:Array<[MagicalTreasureKind,string]>=[['magnificent-artefact','Artefato Magnífico'],['wondrous-item','Item Maravilhoso'],['famous-war-gear','Arma/Armadura Famosa']];
const banes:Bane[]=['Orcs','Trolls','Wolves','Evil Men','Undead','Spiders'];

export const TreasureIndexPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [name,setName]=useState('');
  const [destined,setDestined]=useState(characters[0]?.id||'');
  const [kind,setKind]=useState<MagicalTreasureKind>('magnificent-artefact');
  const [blessings,setBlessings]=useState<string[]>([]);
  const [origin,setOrigin]=useState<MagicalOrigin>('Elven');
  const [baseItem,setBaseItem]=useState('Espada');
  const [qualities,setQualities]=useState<string[]>([]);
  const [selectedBanes,setSelectedBanes]=useState<Bane[]>([]);
  const [rating,setRating]=useState<'lesser'|'greater'|'marvellous'>('lesser');
  const [claimIndex,setClaimIndex]=useState<Record<string,string>>({});
  const [sacrifice,setSacrifice]=useState<Record<string,string>>({});
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(e){setMessage(e instanceof Error?e.message:String(e));}
    finally{setBusy(false);}
  };

  const addEntry=async()=>{
    const entry:TreasureIndexStateEntry={
      id:'treasure_index_'+Date.now()+'_'+Math.random().toString(36).slice(2,7),
      destinedCharacterId:destined,name:name.trim(),kind,
      blessings:kind==='famous-war-gear'?[]:blessings,
      origin:kind==='famous-war-gear'?origin:undefined,
      banes:kind==='famous-war-gear'?selectedBanes:undefined,
      qualities:kind==='famous-war-gear'?qualities:[],
      baseItemName:kind==='famous-war-gear'?baseItem.trim():undefined,
      discovered:false,claimed:false,
    };
    await run([{type:'add_treasure_index_entry',entry,reason:'Historiador adicionou peça predestinada ao Índice de Tesouro.'} as MasterCommand],'adicionar ao Índice de Tesouro');
    setName('');
  };

  const available=(discovery:{kind:string})=>(masterState.treasureIndex||[]).filter(entry=>!entry.claimed&&entry.kind===discovery.kind);
  const famousItems=useMemo(()=>characters.flatMap(hero=>hero.sheet.inventory.filter(item=>item.magicalKind==='famous-war-gear').map(item=>({hero,item}))),[characters]);

  return <section className="mt-4 rounded-lg border border-yellow-900/70 bg-zinc-950/85 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-yellow-600">Livro Básico · Tesouro Mágico</div>
    <h2 className="mt-1 text-sm font-black uppercase text-yellow-100">Índice de Tesouro</h2>
    <p className="mt-1 text-[11px] text-slate-400">A Jogada de Tesouro Mágico determina se há uma peça e sua natureza. A peça concreta é escolhida do Índice e permanece destinada a um Herói específico.</p>

    <div className="mt-3 grid gap-2 md:grid-cols-2">
      <input value={name} onChange={e=>setName(e.target.value)} placeholder="Nome da peça mágica" className="rounded border border-slate-700 bg-black p-2 text-xs"/>
      <select value={destined} onChange={e=>setDestined(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{characters.map(c=><option key={c.id} value={c.id}>{c.sheet.identity.name||c.name}</option>)}</select>
      <select value={kind} onChange={e=>{setKind(e.target.value as MagicalTreasureKind);setBlessings([]);setQualities([]);setSelectedBanes([]);}} className="rounded border border-slate-700 bg-black p-2 text-xs">{kinds.map(([id,label])=><option key={id} value={id}>{label}</option>)}</select>
      {kind!=='famous-war-gear'?<div className="rounded border border-slate-800 p-2">
        <div className="text-[10px] text-slate-500">Bênçãos · {kind==='magnificent-artefact'?'1':'2'}</div>
        <div className="mt-1 grid grid-cols-3 gap-1">{CANONICAL_SKILLS.map(skill=><label key={skill} className="flex items-center gap-1 text-[9px]"><input type="checkbox" checked={blessings.includes(skill)} disabled={!blessings.includes(skill)&&blessings.length>=(kind==='magnificent-artefact'?1:2)} onChange={e=>setBlessings(cur=>e.target.checked?[...cur,skill]:cur.filter(x=>x!==skill))}/>{skill}</label>)}</div>
      </div>:<>
        <input value={baseItem} onChange={e=>setBaseItem(e.target.value)} placeholder="Item-base: Espada, Cota de Malha..." className="rounded border border-slate-700 bg-black p-2 text-xs"/>
        <select value={origin} onChange={e=>{setOrigin(e.target.value as MagicalOrigin);setSelectedBanes([]);}} className="rounded border border-slate-700 bg-black p-2 text-xs"><option value="Elven">Élfica</option><option value="Dwarven">Anã</option><option value="Numenorean">Numenoriana</option></select>
        <div className="rounded border border-slate-800 p-2 md:col-span-2">
          <div className="text-[10px] text-slate-500">Qualidades · 1 a 3 · ordem de despertar</div>
          <div className="mt-1 grid grid-cols-2 gap-1">{ENCHANTED_QUALITIES.map(q=><label key={q.id} className="flex items-center gap-1 text-[9px]"><input type="checkbox" checked={qualities.includes(q.id)} disabled={!qualities.includes(q.id)&&qualities.length>=3} onChange={e=>setQualities(cur=>e.target.checked?[...cur,q.id]:cur.filter(x=>x!==q.id))}/>{q.name}</label>)}</div>
        </div>
        {origin!=='Dwarven'&&<div className="rounded border border-slate-800 p-2 md:col-span-2"><div className="text-[10px] text-slate-500">Flagelos · Élfica: máx. 1 · Numenoriana: máx. 2</div><div className="mt-1 flex flex-wrap gap-2">{banes.map(b=><label key={b} className="text-[9px]"><input className="mr-1" type="checkbox" checked={selectedBanes.includes(b)} disabled={!selectedBanes.includes(b)&&selectedBanes.length>=(origin==='Elven'?1:2)} onChange={e=>setSelectedBanes(cur=>e.target.checked?[...cur,b]:cur.filter(x=>x!==b))}/>{b}</label>)}</div></div>}
      </>}
    </div>
    <button disabled={busy||!name.trim()||!destined} onClick={()=>void addEntry()} className="mt-2 w-full rounded border border-yellow-700 px-3 py-2 text-xs disabled:opacity-40">Adicionar peça predestinada ao Índice</button>

    <div className="mt-4 border-t border-slate-800 pt-3">
      <div className="grid grid-cols-[1fr_auto] gap-2">
        <select value={rating} onChange={e=>setRating(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2 text-xs"><option value="lesser">Tesouro Menor · 2 Façanhas</option><option value="greater">Tesouro Maior · 4 Façanhas</option><option value="marvellous">Tesouro Maravilhoso · 6 Façanhas</option></select>
        <button disabled={busy} onClick={()=>void run([{type:'roll_magical_treasure',rating,reason:'Jogada de Tesouro Mágico após divisão do tesouro.'} as MasterCommand],'rolar Tesouro Mágico')} className="rounded border border-yellow-700 px-3 py-2 text-xs">Rolar</button>
      </div>
      <div className="mt-2 space-y-2">{(masterState.pendingMagicalTreasure||[]).map(d=><div key={d.id} className="rounded border border-amber-900/60 p-2 text-[11px]">
        <div><strong>{d.kind}</strong> · Façanha {d.featResult===11?'Olho':d.featResult===12?'Gandalf':d.featResult} · natureza d6 {d.natureDie}{d.foundOnEye?<span> · Ganância {d.greedShadow}</span>:null}</div>
        <div className="mt-2 grid grid-cols-[1fr_auto] gap-2">
          <select value={claimIndex[d.id]||''} onChange={e=>setClaimIndex(cur=>({...cur,[d.id]:e.target.value}))} className="rounded border border-slate-700 bg-black p-2 text-[10px]"><option value="">Escolher no Índice...</option>{available(d).map(entry=><option key={entry.id} value={entry.id}>{entry.name} · {characters.find(c=>c.id===entry.destinedCharacterId)?.sheet.identity.name||'Herói'}</option>)}</select>
          <button disabled={busy||!claimIndex[d.id]} onClick={()=>{const entry=(masterState.treasureIndex||[]).find(x=>x.id===claimIndex[d.id]);if(entry)void run([{type:'claim_magical_treasure',discoveryId:d.id,treasureIndexEntryId:entry.id,targetCharacterId:entry.destinedCharacterId,reason:'Peça do Índice escolhida explicitamente para a descoberta.'} as MasterCommand],'atribuir Tesouro Mágico')}} className="rounded border border-amber-700 px-2 text-[10px]">Entregar</button>
        </div>
      </div>)}</div>
    </div>

    <div className="mt-4 border-t border-slate-800 pt-3">
      <div className="text-xs font-black text-yellow-100">Peças do Índice</div>
      <div className="mt-2 space-y-1">{(masterState.treasureIndex||[]).map(entry=><div key={entry.id} className="rounded bg-black/35 p-2 text-[10px]"><strong>{entry.name}</strong> · {entry.kind} · destino {characters.find(c=>c.id===entry.destinedCharacterId)?.sheet.identity.name||entry.destinedCharacterId} · {entry.claimed?'reivindicada':entry.discovered?'descoberta':'oculta'}</div>)}</div>
    </div>

    {famousItems.length>0&&<div className="mt-4 border-t border-slate-800 pt-3">
      <div className="text-xs font-black text-yellow-100">Despertar Qualidades Famosas</div>
      <div className="mt-2 space-y-2">{famousItems.map(({hero,item})=><div key={item.id} className="rounded border border-slate-800 p-2 text-[10px]">
        <strong>{item.name}</strong> · {hero.sheet.identity.name||hero.name} · {item.magicalQualitiesUnlocked||0}/{item.magicalQualities?.length||0}
        <button disabled={busy||(item.magicalQualitiesUnlocked||0)>=(item.magicalQualities?.length||0)} onClick={()=>void run([{type:'unlock_famous_quality',targetCharacterId:hero.id,inventoryItemId:item.id,source:'valor',reason:'Novo nível de Valor usado para despertar a próxima Qualidade em vez de obter Recompensa.'} as MasterCommand],'Valor · despertar Qualidade')} className="mt-2 w-full rounded border border-amber-800 px-2 py-1 disabled:opacity-40">Elevar Valor · despertar próxima Qualidade</button>
        <select value={sacrifice[item.id]||''} onChange={e=>setSacrifice(cur=>({...cur,[item.id]:e.target.value}))} className="mt-2 w-full rounded border border-slate-700 bg-black p-1"><option value="">Equipamento para doar...</option>{hero.sheet.inventory.filter(old=>old.id!==item.id&&(old.tags||[]).some(tag=>/^Recompensa:/i.test(tag))).map(old=><option key={old.id} value={old.id}>{old.name} · {(old.tags||[]).filter(tag=>/^Recompensa:/i.test(tag)).length} Recompensa(s)</option>)}</select>
        <button disabled={busy||!sacrifice[item.id]} onClick={()=>void run([{type:'unlock_famous_quality',targetCharacterId:hero.id,inventoryItemId:item.id,source:'treasury',sacrificedInventoryItemId:sacrifice[item.id],reason:'Visita ao tesouro do povo: equipamento aprimorado doado em troca do despertar de Qualidades.'} as MasterCommand],'Visitar Tesouro · despertar Qualidades')} className="mt-1 w-full rounded border border-cyan-800 px-2 py-1 disabled:opacity-40">Visitar Tesouro do povo</button>
      </div>)}</div>
    </div>}
    {message&&<div className="mt-3 text-xs text-slate-300">{message}</div>}
  </section>;
};
