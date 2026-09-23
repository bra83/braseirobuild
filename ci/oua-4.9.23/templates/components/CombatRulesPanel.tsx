import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { CombatActorState, MasterCommand, MasterState } from '../lib/master/types';

interface Props {
  masterState: MasterState;
  characters: Character[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => Promise<void>;
}
function nameOf(actor:CombatActorState,characters:Character[]){
  const hero=characters.find(c=>c.id===actor.actorId);
  return hero?.sheet.identity.name||hero?.name||String(actor.quickSheet?.name||actor.actorId);
}
export const CombatRulesPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const actors=masterState.combat.actors.filter(a=>!a.defeated);
  const heroes=actors.filter(a=>a.actorType==='hero');
  const enemies=actors.filter(a=>a.actorType!=='hero');
  const [attackerId,setAttackerId]=useState(heroes[0]?.actorId||'');
  const [targetId,setTargetId]=useState(enemies[0]?.actorId||'');
  const [weaponIndex,setWeaponIndex]=useState(0);
  const [spendHope,setSpendHope]=useState(false);
  const [inspired,setInspired]=useState(false);
  const [special,setSpecial]=useState<'none'|'heavy'|'pierce'|'defend'>('none');
  const [enemyId,setEnemyId]=useState(enemies[0]?.actorId||'');
  const [heroTarget,setHeroTarget]=useState(heroes[0]?.actorId||'');
  const [enemyDice,setEnemyDice]=useState(2);
  const [enemyDamage,setEnemyDamage]=useState(4);
  const [enemyInjury,setEnemyInjury]=useState(14);
  const [enemyRanged,setEnemyRanged]=useState(false);
  const [healer,setHealer]=useState(heroes[0]?.actorId||'');
  const [healTarget,setHealTarget]=useState(heroes[0]?.actorId||'');
  const [status,setStatus]=useState('');
  const [busy,setBusy]=useState(false);
  const attacker=characters.find(c=>c.id===attackerId);
  const weapons=attacker?.sheet.combat.weapons||[];
  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setStatus('');
    try{await onApplyCommands(commands,reason);setStatus('Aplicado: '+reason);}
    catch(e){setStatus(e instanceof Error?e.message:String(e));}
    finally{setBusy(false);}
  };
  if(masterState.combat.status==='idle'||masterState.combat.status==='resolved') return null;
  return <section className="mb-4 rounded-lg border border-red-900/70 bg-zinc-950/90 p-4">
    <h2 className="text-sm font-black uppercase tracking-wide text-red-200">Resolução canônica de combate</h2>
    <p className="mt-1 text-[11px] text-slate-400">Ataque → Resistência → Dano Especial → Golpe Perfurante → Proteção → Ferimento/Morrendo.</p>
    <div className="mt-3 grid gap-2 md:grid-cols-2">
      <select value={attackerId} onChange={e=>{setAttackerId(e.target.value);setWeaponIndex(0);}} className="rounded border border-slate-700 bg-black p-2 text-xs">{heroes.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
      <select value={targetId} onChange={e=>setTargetId(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{enemies.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
      <select value={weaponIndex} onChange={e=>setWeaponIndex(Number(e.target.value)||0)} className="rounded border border-slate-700 bg-black p-2 text-xs">{weapons.map((w,i)=><option key={i} value={i}>{w.name} · {w.damage}/{w.injury}</option>)}</select>
      <select value={special} onChange={e=>setSpecial(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2 text-xs"><option value="none">Sem gasto de ícone</option><option value="heavy">Golpe Pesado</option><option value="pierce">Perfurar</option><option value="defend">Defender</option></select>
      <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={spendHope} onChange={e=>setSpendHope(e.target.checked)}/> Gastar 1 Esperança</label>
      <label className="flex items-center gap-2 rounded border border-slate-800 px-2 py-2 text-xs"><input type="checkbox" checked={inspired} disabled={!spendHope} onChange={e=>setInspired(e.target.checked)}/> Inspirado (+2d)</label>
      <button disabled={busy||!attackerId||!targetId||!weapons.length} onClick={()=>void run([{type:'resolve_hero_attack',attackerId,targetActorId:targetId,weaponIndex,spendHope,inspired,special:special==='none'?undefined:special,reason:'Ataque resolvido pelo motor canônico.'} as MasterCommand],'ataque do Herói')} className="md:col-span-2 rounded border border-red-700 bg-red-950/30 px-3 py-2 text-sm font-bold text-red-100 disabled:opacity-40">Resolver ataque do Herói</button>
    </div>

    <div className="mt-4 border-t border-slate-800 pt-3">
      <h3 className="text-xs font-bold uppercase text-amber-200">Ataque do adversário</h3>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <select value={enemyId} onChange={e=>setEnemyId(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{enemies.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
        <select value={heroTarget} onChange={e=>setHeroTarget(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{heroes.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
        <label className="text-[10px] text-slate-400">Dados de Sucesso<input type="number" min={0} max={12} value={enemyDice} onChange={e=>setEnemyDice(Math.max(0,Number(e.target.value)||0))} className="mt-1 w-full rounded bg-black p-2"/></label>
        <label className="text-[10px] text-slate-400">Dano<input type="number" min={0} max={30} value={enemyDamage} onChange={e=>setEnemyDamage(Math.max(0,Number(e.target.value)||0))} className="mt-1 w-full rounded bg-black p-2"/></label>
        <label className="text-[10px] text-slate-400">Ferimento<input type="number" min={2} max={30} value={enemyInjury} onChange={e=>setEnemyInjury(Math.max(2,Number(e.target.value)||2))} className="mt-1 w-full rounded bg-black p-2"/></label>
        <label className="flex items-center gap-2 rounded border border-slate-800 px-2 text-xs"><input type="checkbox" checked={enemyRanged} onChange={e=>setEnemyRanged(e.target.checked)}/> À distância</label>
        <button disabled={busy||!enemyId||!heroTarget} onClick={()=>void run([{type:'resolve_adversary_attack',attackerId:enemyId,targetCharacterId:heroTarget,successDice:enemyDice,damage:enemyDamage,injury:enemyInjury,ranged:enemyRanged,reason:'Ataque do adversário resolvido pelo motor canônico.'} as MasterCommand],'ataque do adversário')} className="col-span-2 rounded border border-amber-700 px-3 py-2 text-xs text-amber-100">Resolver ataque do adversário</button>
      </div>
    </div>

    <div className="mt-4 border-t border-slate-800 pt-3">
      <h3 className="text-xs font-bold uppercase text-emerald-200">Primeiros Socorros</h3>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <select value={healer} onChange={e=>setHealer(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{heroes.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
        <select value={healTarget} onChange={e=>setHealTarget(e.target.value)} className="rounded border border-slate-700 bg-black p-2 text-xs">{heroes.map(a=><option key={a.actorId} value={a.actorId}>{nameOf(a,characters)}</option>)}</select>
        <button disabled={busy||!healer||!healTarget} onClick={()=>void run([{type:'resolve_first_aid',healerCharacterId:healer,targetCharacterId:healTarget,reason:'Primeiros Socorros em combate.'} as MasterCommand],'Primeiros Socorros')} className="col-span-2 rounded border border-emerald-700 px-3 py-2 text-xs text-emerald-100">Rolar Cura e tratar Ferimento</button>
      </div>
    </div>
    {status&&<p className="mt-3 rounded border border-slate-800 bg-black/40 p-2 text-[11px] text-slate-200">{status}</p>}
  </section>;
};
