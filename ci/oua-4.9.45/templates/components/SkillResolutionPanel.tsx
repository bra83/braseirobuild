import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState, SkillSpecialSuccessChoice } from '../lib/master/types';
import { CANONICAL_SKILLS } from '../lib/skillRegistry';

type Props={
  masterState:MasterState;
  characters:Character[];
  onApplyCommands:(commands:MasterCommand[],reason:string)=>void|Promise<void>;
};

const labelForChoice:Record<SkillSpecialSuccessChoice,string>={
  'cancel-failure':'Cancelar uma falha',
  'additional-success':'Obter +1 sucesso',
  'information':'Adquirir informação',
  'silent':'Agir em silêncio',
  'hurry':'Se apressar',
  'influence':'Ampliar influência',
};

export const SkillResolutionPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [label,setLabel]=useState('Teste de Perícia');
  const [required,setRequired]=useState(0);
  const [participants,setParticipants]=useState<string[]>(characters.map(c=>c.id));
  const [heroId,setHeroId]=useState(characters[0]?.id||'');
  const [skill,setSkill]=useState('Viagem');
  const [supporter,setSupporter]=useState('');
  const [supportSkill,setSupportSkill]=useState('História');
  const [spendHope,setSpendHope]=useState(false);
  const [magic,setMagic]=useState(false);
  const [cancelTargets,setCancelTargets]=useState<Record<string,string>>({});
  const [notes,setNotes]=useState<Record<string,string>>({});

  const session=masterState.skillTestSession;
  const active=session?.status==='active';

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };

  const start=()=>void run([{
    type:'start_skill_test_session',
    label,
    participants,
    requiredSuccesses:required>0?required:undefined,
    reason:'Jogador iniciou uma resolução explícita de teste de Perícia.',
  } as MasterCommand],'iniciar teste de Perícia');

  const sessionHeroes=useMemo(()=>session?characters.filter(c=>session.participants.includes(c.id)):[],[session,characters]);
  const failedIds=useMemo(()=>session?Object.values(session.rolls).filter(r=>!r.success).map(r=>r.characterId):[],[session]);
  const successes=session?Object.values(session.rolls).filter(r=>r.success).length+session.additionalSuccesses:0;

  const spendIcon=(sourceId:string,choice:SkillSpecialSuccessChoice,targetId?:string)=>{
    const note=notes[sourceId]||'';
    void run([{
      type:'spend_skill_success_icon',
      sourceCharacterId:sourceId,
      choice,
      targetCharacterId:targetId,
      note,
      reason:'Jogador gastou um ícone de Sucesso: '+labelForChoice[choice]+'.',
    } as MasterCommand],labelForChoice[choice]);
  };

  return <section className="mt-4 rounded-lg border border-emerald-900/70 bg-zinc-950/85 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-emerald-500">Livro Básico · Resolução de Ações</div>
    <h2 className="mt-1 text-sm font-black uppercase text-emerald-100">Suporte e Sucessos Especiais</h2>
    <p className="mt-1 text-[11px] text-slate-400">Suporte custa 1 Esperança do apoiador e concede +1d. Cada ícone de Sucesso obtido numa jogada bem-sucedida pode ser gasto separadamente sem reduzir o grau do sucesso.</p>

    {!active&&<div className="mt-3 rounded border border-slate-800 p-3">
      <input value={label} onChange={e=>setLabel(e.target.value)} className="w-full rounded border border-slate-700 bg-black p-2 text-xs" placeholder="Nome da ação"/>
      <label className="mt-2 block text-[10px] text-slate-400">Sucessos exigidos pela ação · 0 se não for desafio acumulativo
        <input type="number" min={0} value={required} onChange={e=>setRequired(Math.max(0,Number(e.target.value)||0))} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      </label>
      <div className="mt-2 text-[10px] text-slate-400">Participantes</div>
      <div className="mt-1 grid grid-cols-2 gap-1">{characters.map(hero=><label key={hero.id} className="flex items-center gap-2 rounded border border-slate-800 px-2 py-1 text-[10px]"><input type="checkbox" checked={participants.includes(hero.id)} onChange={e=>setParticipants(cur=>e.target.checked?Array.from(new Set([...cur,hero.id])):cur.filter(id=>id!==hero.id))}/>{hero.sheet.identity.name||hero.name}</label>)}</div>
      <button disabled={busy||participants.length===0} onClick={start} className="mt-2 w-full rounded border border-emerald-800 px-3 py-2 text-xs disabled:opacity-40">Iniciar resolução</button>
    </div>}

    {session&&<div className="mt-3">
      <div className="rounded border border-slate-800 bg-black/25 p-2 text-[11px]">
        <strong>{session.label}</strong> · {session.status==='active'?'em andamento':'encerrado'}
        {session.requiredSuccesses?<span> · Progresso {successes}/{session.requiredSuccesses}</span>:<span> · Sucessos {successes}</span>}
        {session.additionalSuccesses>0?<span> · +{session.additionalSuccesses} especiais</span>:null}
        {session.silent?<span> · silencioso</span>:null}
        {session.timeMultiplier<1?<span> · metade do tempo</span>:null}
        {session.additionalTargetGroups>0?<span> · +{session.additionalTargetGroups} alvo(s)/grupo(s)</span>:null}
        {session.informationDiscoveries>0?<span> · {session.informationDiscoveries} informação(ões)</span>:null}
      </div>

      {active&&<div className="mt-3 rounded border border-slate-800 p-3">
        <div className="text-xs font-black text-emerald-100">Fazer tentativa</div>
        <select value={heroId} onChange={e=>{setHeroId(e.target.value);if(e.target.value===supporter)setSupporter('')}} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">
          {sessionHeroes.map(hero=><option key={hero.id} value={hero.id} disabled={Boolean(session.rolls[hero.id])}>{hero.sheet.identity.name||hero.name}{session.rolls[hero.id]?' · já rolou':''}</option>)}
        </select>
        <select value={skill} onChange={e=>setSkill(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">{CANONICAL_SKILLS.map(name=><option key={name}>{name}</option>)}</select>

        <div className="mt-2 rounded border border-cyan-950/70 p-2">
          <div className="text-[10px] font-black text-cyan-200">Suporte opcional</div>
          <select value={supporter} onChange={e=>setSupporter(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-[10px]">
            <option value="">Sem Suporte</option>
            {characters.filter(hero=>hero.id!==heroId).map(hero=><option key={hero.id} value={hero.id}>{hero.sheet.identity.name||hero.name} · Esperança {hero.sheet.attributes.hopeCurrent}</option>)}
          </select>
          {supporter&&<select value={supportSkill} onChange={e=>setSupportSkill(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-[10px]">{CANONICAL_SKILLS.map(name=><option key={name} value={name}>{name} · {characters.find(c=>c.id===supporter)?.sheet.skills.find(s=>s.name===name)?.rank||0}</option>)}</select>}
          <p className="mt-1 text-[9px] text-slate-500">A escolha da Perícia de apoio representa a aprovação do Historiador de que ela é apropriada às circunstâncias.</p>
        </div>

        <div className="mt-2 flex gap-4 text-[10px]">
          <label><input type="checkbox" checked={spendHope} onChange={e=>{setSpendHope(e.target.checked);if(e.target.checked)setMagic(false)}}/> Esperança do Herói ativo</label>
          <label><input type="checkbox" checked={magic} onChange={e=>{setMagic(e.target.checked);if(e.target.checked)setSpendHope(false)}}/> Sucesso Mágico</label>
        </div>
        <button disabled={busy||!heroId||Boolean(session.rolls[heroId])} onClick={()=>void run([{
          type:'roll_skill_test_session',targetCharacterId:heroId,skill,
          supporterCharacterId:supporter||undefined,supporterSkill:supporter?supportSkill:undefined,
          spendHope,useMagicalSuccess:magic,
          reason:'Jogada de Perícia feita dentro da resolução de grupo.',
        } as MasterCommand],'rolar '+skill)} className="mt-2 w-full rounded border border-emerald-800 px-3 py-2 text-xs disabled:opacity-40">Rolar Perícia</button>
      </div>}

      <div className="mt-3 space-y-2">{Object.values(session.rolls).map(result=>{
        const hero=characters.find(c=>c.id===result.characterId);
        const available=Math.max(0,result.successIcons-result.iconsSpent);
        return <div key={result.characterId} className="rounded border border-slate-800 p-2 text-[10px]">
          <div><strong>{hero?.sheet.identity.name||hero?.name||result.characterId}</strong> · {result.skill} · {result.success?'SUCESSO':'FALHA'} · ícones {result.successIcons} · disponíveis {available}{result.cancelledFailureBy?' · falha cancelada por companheiro':''}</div>
          {result.supporterCharacterId?<div className="mt-1 text-cyan-300">Suporte: {characters.find(c=>c.id===result.supporterCharacterId)?.sheet.identity.name||result.supporterCharacterId} · {result.supporterSkill} · 1 Esperança gasta</div>:null}
          {available>0&&active&&<>
            <input value={notes[result.characterId]||''} onChange={e=>setNotes(cur=>({...cur,[result.characterId]:e.target.value}))} placeholder="Observação opcional para informação/influência/etc." className="mt-2 w-full rounded border border-slate-700 bg-black p-1 text-[9px]"/>
            <div className="mt-2 grid grid-cols-2 gap-1">
              <button disabled={!failedIds.some(id=>id!==result.characterId)} onClick={()=>{const target=cancelTargets[result.characterId]||failedIds.find(id=>id!==result.characterId);if(target)spendIcon(result.characterId,'cancel-failure',target)}} className="rounded border border-slate-700 px-1 py-1 disabled:opacity-40">Cancelar falha</button>
              <select value={cancelTargets[result.characterId]||''} onChange={e=>setCancelTargets(cur=>({...cur,[result.characterId]:e.target.value}))} className="rounded border border-slate-700 bg-black p-1"><option value="">Falha alvo...</option>{failedIds.filter(id=>id!==result.characterId).map(id=><option key={id} value={id}>{characters.find(c=>c.id===id)?.sheet.identity.name||id}</option>)}</select>
              <button disabled={!session.requiredSuccesses} onClick={()=>spendIcon(result.characterId,'additional-success')} className="rounded border border-slate-700 px-1 py-1 disabled:opacity-40">+1 sucesso</button>
              <button onClick={()=>spendIcon(result.characterId,'information')} className="rounded border border-slate-700 px-1 py-1">Informação</button>
              <button disabled={session.silent} onClick={()=>spendIcon(result.characterId,'silent')} className="rounded border border-slate-700 px-1 py-1 disabled:opacity-40">Silêncio</button>
              <button disabled={session.timeMultiplier<1} onClick={()=>spendIcon(result.characterId,'hurry')} className="rounded border border-slate-700 px-1 py-1 disabled:opacity-40">Metade do tempo</button>
              <button onClick={()=>spendIcon(result.characterId,'influence')} className="rounded border border-slate-700 px-1 py-1">+ alvo/grupo</button>
            </div>
          </>}
        </div>;
      })}</div>

      {active&&<button disabled={busy} onClick={()=>void run([{type:'end_skill_test_session',reason:'Jogador encerrou a resolução do teste de Perícia.'} as MasterCommand],'encerrar teste de Perícia')} className="mt-3 w-full rounded border border-emerald-900 px-3 py-2 text-xs">Encerrar resolução</button>}
    </div>}
    {message&&<div className="mt-3 text-xs text-slate-300">{message}</div>}
  </section>;
};
