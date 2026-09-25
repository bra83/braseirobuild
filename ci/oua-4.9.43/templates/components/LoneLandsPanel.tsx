import React, { useMemo, useState } from 'react';
import type { Character } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';

type Props={
  masterState:MasterState;
  characters:Character[];
  onApplyCommands:(commands:MasterCommand[],reason:string)=>void|Promise<void>;
};

const splitIds=(value:string)=>value.split(',').map(v=>v.trim()).filter(Boolean);

export const LoneLandsPanel:React.FC<Props>=({masterState,characters,onApplyCommands})=>{
  const [busy,setBusy]=useState(false);
  const [message,setMessage]=useState('');
  const [heroId,setHeroId]=useState(characters[0]?.id||'');
  const [ancestor,setAncestor]=useState('');
  const [lossothGear,setLossothGear]=useState(false);
  const [lossothGuides,setLossothGuides]=useState(false);
  const [specialTable,setSpecialTable]=useState(true);
  const [npcIds,setNpcIds]=useState('');
  const [objective,setObjective]=useState('');
  const [request,setRequest]=useState<'reasonable'|'bold'|'outrageous'>('reasonable');
  const [relation,setRelation]=useState<'reluctant'|'jagat-vouches'|'friendly'>('reluctant');
  const [combatHero,setCombatHero]=useState(characters[0]?.id||'');
  const [protectedHero,setProtectedHero]=useState('');
  const [task,setTask]=useState<'intimidate'|'inspire'|'protect'>('intimidate');
  const [hope,setHope]=useState(false);
  const [magic,setMagic]=useState(false);
  const [enemyId,setEnemyId]=useState('');
  const [enemyAbilities,setEnemyAbilities]=useState('');
  const [enemyTraits,setEnemyTraits]=useState('');

  const run=async(commands:MasterCommand[],reason:string)=>{
    setBusy(true);setMessage('');
    try{await onApplyCommands(commands,reason);setMessage('Aplicado: '+reason);}
    catch(error){setMessage(error instanceof Error?error.message:String(error));}
    finally{setBusy(false);}
  };

  const lone=masterState.company.loneLands||{};
  const forodwaith=masterState.journey.loneLandsForodwaith;
  const combatActors=masterState.combat.actors||[];
  const enemies=useMemo(()=>combatActors.filter(actor=>actor.actorType!=='hero'),[combatActors]);

  return <section className="mt-4 rounded-lg border border-stone-700 bg-zinc-950/90 p-4">
    <div className="text-[10px] uppercase tracking-[.16em] text-stone-400">Contos das Terras Solitárias</div>
    <h2 className="mt-1 text-sm font-black uppercase text-stone-100">Mecânicas de cenário</h2>
    <p className="mt-1 text-[11px] text-slate-400">As decisões abaixo alteram estado permanente ou resoluções reais. Nenhuma escolha de Herdeiro, pedido de Conselho, papel de combate ou preparação de viagem é feita automaticamente.</p>

    <div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-amber-100">Herdeiro · Estelang · Colina do Medo</div>
      <select value={heroId} onChange={e=>setHeroId(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">
        {characters.map(hero=><option key={hero.id} value={hero.id}>{hero.sheet.identity.name||hero.name}</option>)}
      </select>
      <input value={ancestor} onChange={e=>setAncestor(e.target.value)} placeholder="Nome do ancestral, se conhecido" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <button disabled={busy||!heroId} onClick={()=>void run([{type:'set_lone_lands_heir',targetCharacterId:heroId,ancestorName:ancestor,reason:'Herdeiro da linhagem escolhido explicitamente.'} as MasterCommand],'definir Herdeiro')} className="rounded border border-amber-800 px-2 py-2 text-xs">Definir Herdeiro</button>
        <button disabled={busy||!heroId||Boolean(lone.estelangClaimedBy)} onClick={()=>void run([{type:'claim_estelang',targetCharacterId:heroId,reason:'Estelang foi oferecida e reivindicada pelo Herdeiro.'} as MasterCommand],'reivindicar Estelang')} className="rounded border border-amber-800 px-2 py-2 text-xs disabled:opacity-40">Reivindicar Estelang</button>
        <button disabled={busy||masterState.activeMode!=='combat'||!heroId||Boolean(lone.heirOathBound)} onClick={()=>void run([{type:'invoke_heir_oath',targetCharacterId:heroId,reason:'Herdeiro assumiu o juramento ancestral em extrema necessidade.'} as MasterCommand],'assumir Juramento')} className="rounded border border-red-900 px-2 py-2 text-xs disabled:opacity-40">Assumir Juramento</button>
        <button disabled={busy||!heroId} onClick={()=>void run([{type:'mark_hill_shadow_dreamer',targetCharacterId:heroId,reason:'Herói foi marcado pelos sonhos da Sombra da Colina.'} as MasterCommand],'marcar sonhador')} className="rounded border border-violet-900 px-2 py-2 text-xs">Marcar sonhador</button>
      </div>
      <button disabled={busy||Boolean(lone.hillDestroyed)} onClick={()=>void run([{type:'destroy_hill_of_fear',reason:'A Colina do Medo foi destruída; vínculos e atração associados foram encerrados.'} as MasterCommand],'destruir Colina do Medo')} className="mt-2 w-full rounded border border-emerald-900 px-2 py-2 text-xs disabled:opacity-40">Confirmar destruição da Colina do Medo</button>
      <div className="mt-2 text-[10px] text-slate-500">Herdeiro: {characters.find(c=>c.id===lone.heirCharacterId)?.sheet.identity.name||'—'} · Estelang: {lone.estelangClaimedBy?'reivindicada':'não reivindicada'} · Juramento: {lone.heirOathBound?'ativo até cumprir':'não assumido'} · Colina: {lone.hillDestroyed?'destruída':'ativa'}</div>
    </div>

    <div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-cyan-100">Forodwaith</div>
      <label className="mt-2 flex items-center gap-2 text-[11px]"><input type="checkbox" checked={lossothGear} onChange={e=>setLossothGear(e.target.checked)}/> Equipamento Lossoth</label>
      <label className="mt-1 flex items-center gap-2 text-[11px]"><input type="checkbox" checked={lossothGuides} onChange={e=>setLossothGuides(e.target.checked)}/> Guias Lossoth</label>
      <label className="mt-1 flex items-center gap-2 text-[11px]"><input type="checkbox" checked={specialTable} onChange={e=>setSpecialTable(e.target.checked)}/> Usar tabela especial de Eventos de Forodwaith</label>
      <button disabled={busy||['travelling','paused'].includes(masterState.journey.status)} onClick={()=>void run([{type:'prepare_forodwaith_journey',lossothGear,lossothGuides,useSpecialTable:specialTable,reason:'Preparação de Forodwaith escolhida explicitamente.'} as MasterCommand],'preparar Forodwaith')} className="mt-2 w-full rounded border border-cyan-800 px-2 py-2 text-xs disabled:opacity-40">Preparar travessia</button>
      {forodwaith?.active&&<div className="mt-2">
        <label className="flex items-center gap-2 text-[11px]"><input type="checkbox" checked={Boolean(forodwaith.stormSpirits)} onChange={e=>void run([{type:'set_forodwaith_storm_spirits',active:e.target.checked,reason:'Estado dos Espíritos da Tempestade atualizado.'} as MasterCommand],e.target.checked?'ativar Espíritos da Tempestade':'encerrar Espíritos da Tempestade')}/> Espíritos da Tempestade ativos</label>
        <div className="mt-1 text-[10px] text-slate-500">Perigo 3 · Eventos especiais resolvidos: {forodwaith.specialEventsResolved||0}</div>
      </div>}
    </div>

    <div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-emerald-100">Compromissos Lossoth / Pedra dos Infelizes</div>
      <select value={heroId} onChange={e=>setHeroId(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">{characters.map(hero=><option key={hero.id} value={hero.id}>{hero.sheet.identity.name||hero.name}</option>)}</select>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <button disabled={busy||masterState.fellowshipPhase.status!=='active'} onClick={()=>void run([{type:'undertake_hunt_lossoth',targetCharacterId:heroId,reason:'Herói escolheu Caçar com os Lossoth como Compromisso.'} as MasterCommand],'Caçar com os Lossoth')} className="rounded border border-emerald-900 px-2 py-2 text-xs disabled:opacity-40">Caçar com os Lossoth</button>
        <button disabled={busy||masterState.fellowshipPhase.status!=='active'} onClick={()=>void run([{type:'undertake_stone_unhappy',targetCharacterId:heroId,reason:'Herói visitou a Pedra dos Infelizes como Compromisso.'} as MasterCommand],'Pedra dos Infelizes')} className="rounded border border-violet-900 px-2 py-2 text-xs disabled:opacity-40">Pedra dos Infelizes</button>
      </div>
      <button disabled={busy||!lone.lossothSafeRefugeNextAdventure} onClick={()=>void run([{type:'resolve_lossoth_safe_refuge_rest',reason:'Descanso Prolongado no acampamento de verão Lossoth como refúgio seguro.'} as MasterCommand],'descanso em refúgio Lossoth')} className="mt-2 w-full rounded border border-emerald-800 px-2 py-2 text-xs disabled:opacity-40">Descanso Prolongado · refúgio Lossoth</button>
    </div>

    <div className="mt-3 rounded border border-slate-800 p-3">
      <div className="text-xs font-black text-sky-100">Conselhos especiais</div>
      <input value={npcIds} onChange={e=>setNpcIds(e.target.value)} placeholder="IDs dos NPCs interlocutores, separados por vírgula" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      <div className="mt-2 grid grid-cols-2 gap-2">
        <button disabled={busy} onClick={()=>void run([{type:'start_lone_lands_dwarven_council',participants:characters.map(c=>c.id),npcIds:splitIds(npcIds),reason:'Conselho sob as Montanhas iniciado com Introdução antes da definição do pedido.'} as MasterCommand],'Conselho sob as Montanhas')} className="rounded border border-sky-800 px-2 py-2 text-xs">Conselho sob as Montanhas</button>
        <button disabled={busy} onClick={()=>void run([{type:'start_egel_council',participants:characters.map(c=>c.id),npcIds:splitIds(npcIds),request,relation,reason:'Conselho com Egel iniciado com pedido e relação escolhidos explicitamente.'} as MasterCommand],'Conselho com Egel')} className="rounded border border-sky-800 px-2 py-2 text-xs">Conselho com Egel</button>
      </div>
      {masterState.council.sequenceVariant==='lone-lands-introduction-first'&&masterState.council.status!=='introduction'&&masterState.council.resistancePending&&<>
        <input value={objective} onChange={e=>setObjective(e.target.value)} placeholder="Pedido da Companhia" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
        <div className="mt-2 grid grid-cols-3 gap-2">{([3,6,9] as const).map(resistance=><button key={resistance} disabled={busy||!objective.trim()} onClick={()=>void run([{type:'set_lone_lands_council_request',resistance,objective,reason:'Pedido e Resistência escolhidos depois da Introdução.'} as MasterCommand],'definir pedido do Conselho')} className="rounded border border-sky-900 px-2 py-2 text-xs">Resistência {resistance}</button>)}</div>
      </>}
      <div className="mt-2 grid grid-cols-2 gap-2">
        <select value={request} onChange={e=>setRequest(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2 text-[10px]"><option value="reasonable">Pedido razoável · R3</option><option value="bold">Pedido ousado · R6</option><option value="outrageous">Pedido ultrajante · R9</option></select>
        <select value={relation} onChange={e=>setRelation(e.target.value as any)} className="rounded border border-slate-700 bg-black p-2 text-[10px]"><option value="reluctant">Egel relutante</option><option value="jagat-vouches">Jagat intercede</option><option value="friendly">Egel amistoso</option></select>
      </div>
    </div>

    {masterState.activeMode==='combat'&&<div className="mt-3 rounded border border-red-950 p-3">
      <div className="text-xs font-black text-red-100">Tarefas de Combate</div>
      <select value={combatHero} onChange={e=>setCombatHero(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs">{characters.map(hero=><option key={hero.id} value={hero.id}>{hero.sheet.identity.name||hero.name}</option>)}</select>
      <select value={task} onChange={e=>setTask(e.target.value as any)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="intimidate">Intimidar Inimigo · Fascínio · Avançada</option><option value="inspire">Inspirar Camaradas · Indução · Aberta</option><option value="protect">Proteger Companheiro · Batalha · Defensiva</option></select>
      {task==='protect'&&<select value={protectedHero} onChange={e=>setProtectedHero(e.target.value)} className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Companheiro protegido...</option>{characters.filter(c=>c.id!==combatHero).map(hero=><option key={hero.id} value={hero.id}>{hero.sheet.identity.name||hero.name}</option>)}</select>}
      <div className="mt-2 flex gap-4 text-[10px]"><label><input type="checkbox" checked={hope} onChange={e=>{setHope(e.target.checked);if(e.target.checked)setMagic(false)}}/> Gastar Esperança</label><label><input type="checkbox" checked={magic} onChange={e=>{setMagic(e.target.checked);if(e.target.checked)setHope(false)}}/> Sucesso Mágico</label></div>
      <button disabled={busy||!combatHero||(task==='protect'&&!protectedHero)} onClick={()=>void run([{type:'resolve_combat_task',actorCharacterId:combatHero,task,protectedCharacterId:task==='protect'?protectedHero:undefined,spendHope:hope,useMagicalSuccess:magic,reason:'Tarefa de Combate escolhida explicitamente pelo jogador.'} as MasterCommand],'resolver Tarefa de Combate')} className="mt-2 w-full rounded border border-red-800 px-2 py-2 text-xs disabled:opacity-40">Resolver Tarefa de Combate</button>

      <div className="mt-3 border-t border-slate-800 pt-2 text-[10px]">Metadados de adversário para regras de cenário</div>
      <select value={enemyId} onChange={e=>setEnemyId(e.target.value)} className="mt-1 w-full rounded border border-slate-700 bg-black p-2 text-xs"><option value="">Adversário...</option>{enemies.map(actor=><option key={actor.actorId} value={actor.actorId}>{String(actor.quickSheet?.name||actor.actorId)}</option>)}</select>
      <input value={enemyAbilities} onChange={e=>setEnemyAbilities(e.target.value)} placeholder="Habilidades Mortais, separadas por vírgula" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      <input value={enemyTraits} onChange={e=>setEnemyTraits(e.target.value)} placeholder="Características, separadas por vírgula" className="mt-2 w-full rounded border border-slate-700 bg-black p-2 text-xs"/>
      <button disabled={busy||!enemyId} onClick={()=>void run([{type:'set_combat_actor_deadly_abilities',actorId:enemyId,deadlyAbilities:splitIds(enemyAbilities),traits:splitIds(enemyTraits),reason:'Metadados canônicos do adversário configurados para regras de cenário.'} as MasterCommand],'configurar adversário')} className="mt-2 w-full rounded border border-red-900 px-2 py-2 text-xs disabled:opacity-40">Salvar adversário</button>
    </div>}

    {message&&<div className="mt-3 text-xs text-slate-300">{message}</div>}
  </section>;
};
