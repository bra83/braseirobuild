import React, { useEffect, useMemo, useState } from 'react';
import { ChevronRight, CircleMinus, CirclePlus, Shield, Skull, Swords, X } from 'lucide-react';
import { BestiaryEntry, CampaignState, Character, EncounterParticipantStatus, WorldState } from '../types';
import { BestiaryService } from '../services/BestiaryService';
import { EncounterEnemySelection, EncounterService } from '../services/EncounterService';
import { BitterReachElementalCombatService } from '../services/BitterReachElementalCombatService.ts';
import { BitterReachMagicTimeService } from '../services/BitterReachMagicTimeService.ts';
import { getDb } from '../db/db';
import { CharacterControlService, CONTROL_LABELS } from '../services/CharacterControlService';
import { DelegatedCombatService, type DelegatedCombatPlan } from '../services/DelegatedCombatService';
import { UiActionCommitGate } from '../services/UiActionCommitGate';

const STATUS_LABELS: Record<EncounterParticipantStatus, string> = {
  active: 'Ativo', broken: 'Quebrado', dead: 'Morto', fled: 'Fugiu', yielded: 'Rendeu-se',
};

export function EncounterPanel(props: {
  campaign: CampaignState;
  worldState: WorldState;
  onError: (message: string) => void;
}) {
  const { campaign, worldState, onError } = props;
  const encounter = worldState.encounter;
  const active = encounter?.status === 'active' ? encounter : null;
  const [open, setOpen] = useState(Boolean(active));
  const [modal, setModal] = useState(false);
  const [title, setTitle] = useState('Encontro');
  const [linkedJourneyEventId,setLinkedJourneyEventId]=useState('');
  const pendingOfficialCombats=worldState.journey.events.filter(event=>event.status==='open' && (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.stage==='pending_external_mechanics' && (event.result?.officialSceneDecision as Record<string,unknown>|undefined)?.pending==='combat' && ![2,3,4,5,7,8,10,11,12,17,18,20,21,23,24,25,29,30,33,35,37,40,41,42,43].includes(Number((event.result?.mechanic as Record<string,unknown>|undefined)?.encounterId)));
  const [entries, setEntries] = useState<BestiaryEntry[]>([]);
  const [quantities, setQuantities] = useState<Record<string, number>>({});
  const [busy, setBusy] = useState('');
  const actionGate = React.useRef(new UiActionCommitGate());
  const [characters, setCharacters] = useState<Character[]>([]);
  const autoKey = React.useRef('');
  const [combatPlan, setCombatPlan] = useState<DelegatedCombatPlan | null>(null);
  const [monsterTargetIds,setMonsterTargetIds]=useState<string[]>([]);
  const [wyrmAuraReach,setWyrmAuraReach]=useState<Record<string,'arms-reach'|'outside'>>({});
  const [wyrmAuraConfirmed,setWyrmAuraConfirmed]=useState(false);
  const [wyrmAreaZones,setWyrmAreaZones]=useState<Record<string,'arms-reach'|'near'|'outside'>>({});
  const [wyrmHardSurface,setWyrmHardSurface]=useState<'hard'|'soft'|''>('');
  const [elementalTargetIds,setElementalTargetIds]=useState<Record<string,string>>({});
  const [elementalRangeConfirmed,setElementalRangeConfirmed]=useState<Record<string,boolean>>({});
  const [fearDamageAttribute,setFearDamageAttribute]=useState<'wits'|'empathy'>('wits');
  const [sangrentoLightConfirm,setSangrentoLightConfirm]=useState<Record<string,boolean>>({});
  const [npcTargetId,setNpcTargetId]=useState('');
  const [npcDefense,setNpcDefense]=useState<'none'|'dodge'|'block'>('none');
  const [npcReachConfirmed,setNpcReachConfirmed]=useState(false);

  useEffect(() => { if (active) setOpen(true); }, [active?.id]);
  useEffect(() => { void getDb().then(db => db.getAll('characters')).then(setCharacters).catch(error => onError(String(error?.message || error))); }, [active?.id, active?.updatedAt]);
  useEffect(() => { if (modal) void BestiaryService.getAllEntries().then(setEntries).catch(error => onError(error.message)); }, [modal]);

  const current = active?.combatants[active.turnIndex] || null;
  const turnCharacter = current?.kind === 'character' ? characters.find(item => item.id === current.entityId) : undefined;
  const turnControl = CharacterControlService.mode(turnCharacter);
  const partyTargets=active?.combatants.filter(item=>item.status==='active'&&(current?.quickSheet?.fireWyrmCasterId?item.id!==current.id&&!(item.kind==='character'&&item.entityId===current.quickSheet.fireWyrmCasterId):item.side===(current?.side==='party'?'enemy':'party')))||[];
  const wyrmAuraTargets=active?.combatants.filter(item=>item.id!==current?.id&&(item.status==='active'||item.status==='broken'))||[];
  const isCurrentFireWyrm=current?.entityId==='bloodmarch:fire-wyrm'&&Number(current?.quickSheet?.fireAuraDice)===3;
  const auraResolved=Boolean(isCurrentFireWyrm&&current?.statusEffects?.some(effect=>effect.id===`bloodmarch:fire-wyrm-aura:${active?.round}`));
  const isWyrmAttacker=Boolean(current?.quickSheet?.fireWyrmCasterId&&['bloodmarch:fire-wyrm','bloodmarch:kton'].includes(current?.entityId||''));
  const isOfficialProcessionEnt=current?.quickSheet?.officialProcessionEnt===true;
  const pendingProcessionEntAttack=current?.quickSheet?.pendingOfficialProcessionEntAttack as {round:number;dieValue:number;attackName:string}|undefined;
  const pendingProcessionEntRow=(current?.quickSheet?.monsterAttackTable as Array<{roll:number;name:string;range?:string;tags?:string[]}>|undefined)
    ?.find(row=>row.roll===pendingProcessionEntAttack?.dieValue);
  const processionEntTargetCountInvalid=!pendingProcessionEntAttack||!pendingProcessionEntRow?true:
    pendingProcessionEntRow.tags?.includes('area')?monsterTargetIds.length<1:
    pendingProcessionEntRow.tags?.includes('two_targets')?monsterTargetIds.length!==Math.min(2,partyTargets.length):monsterTargetIds.length!==1;
  useEffect(()=>{setWyrmAuraReach({});setWyrmAuraConfirmed(false);},[active?.id,active?.round,current?.id]);
  useEffect(()=>{setNpcTargetId('');setNpcDefense('none');setNpcReachConfirmed(false);},[active?.id,active?.round,current?.id]);
  useEffect(()=>{setWyrmAreaZones({});setWyrmHardSurface('');},[active?.id,active?.round,current?.id]);
  useEffect(()=>{if(current?.kind!=='monster')return;const activeIds=new Set(partyTargets.map(item=>item.id));const valid=monsterTargetIds.filter(id=>activeIds.has(id));if(valid.length!==monsterTargetIds.length)setMonsterTargetIds(valid);else if(!valid.length&&partyTargets.length)setMonsterTargetIds(partyTargets.slice(0,2).map(item=>item.id));},[current?.id,active?.updatedAt,monsterTargetIds,partyTargets.map(item=>item.id).join('|')]);
  useEffect(() => {
    if (!active || !turnCharacter || busy) return;
    const mode = CharacterControlService.mode(turnCharacter);
    const key = `${active.id}:${active.round}:${active.turnIndex}:${current?.id}`;
    if (mode === 'player' || autoKey.current === key) return;
    autoKey.current = key;
    const plan = DelegatedCombatService.plan(active, turnCharacter);
    if (!plan) return;
    setCombatPlan(plan);
    if (mode === 'master') {
      void DelegatedCombatService.execute(campaign.id, active, turnCharacter, plan, false)
        .then(() => { setCombatPlan(null); onError(''); })
        .catch(error => onError(`Turno delegado: ${String(error?.message || error)}. Controle suspenso para evitar ação incorreta.`));
    }
  }, [active?.id, active?.round, active?.turnIndex, active?.updatedAt, turnCharacter?.id, turnCharacter?.controlMode, busy]);

  const selections = useMemo<EncounterEnemySelection[]>(() => (Object.entries(quantities) as Array<[string, number]>).filter(([, quantity]) => quantity > 0).map(([entryId, quantity]) => ({ entryId, quantity })), [quantities]);

  const act = async (label: string, action: () => Promise<unknown>) => {
    if (turnCharacter && turnControl !== 'player' && !label.startsWith('curated-combat') &&
        /^(fast|slow|stand|web|grapple|drag|possessed|rescue|warts|eggs|consume):/.test(label)) {
      onError(`${turnCharacter.name} é controlado pelo Mestre. Use a proposta tática/curadoria, não os botões manuais.`);
      return;
    }
    // Combat turns and official-encounter choices must never be dispatched
    // twice while React has not yet rendered the disabled button.
    const ticket = actionGate.current.acquire(campaign.id, label);
    if (!ticket) return;
    setBusy(label);
    try { await action(); onError(''); }
    catch (error: any) { onError(error?.message || String(error)); }
    finally { if (actionGate.current.release(ticket)) setBusy(''); }
  };

  const start = async () => {
    await act('start', async () => {
      await EncounterService.startManual(campaign.id, title, selections, linkedJourneyEventId||undefined);
      setModal(false);
      setQuantities({});
      setLinkedJourneyEventId('');
    });
  };

  return <>
    <section className={`bg-bg-surface border rounded ${active ? 'border-red-700/70' : 'border-border'}`}>
      <button onClick={() => setOpen(value => !value)} className="w-full p-3 flex items-center gap-3 text-left">
        <Swords className={active ? 'text-red-400' : 'text-accent'} />
        <div className="flex-1"><b>{active ? active.title : 'Combate e encontros'}</b><p className="text-xs text-text-secondary">{active ? `Rodada ${active.round} · ${current ? `turno de ${current.name}` : 'sem combatentes'}` : 'Sem encontro ativo'}</p></div>
        <span className="text-xs text-accent">{open ? 'Fechar' : active ? 'Abrir' : 'Gerir'}</span>
      </button>
      {open && <div className="border-t border-border p-4 space-y-3">
        {!active ? <button onClick={() => setModal(true)} className="w-full p-3 bg-accent text-white rounded flex items-center justify-center gap-2"><Skull className="w-4 h-4" />Iniciar encontro com o bestiário</button> : <>
          <div className="p-3 bg-bg-base border border-accent/50 rounded flex items-center gap-3"><div className="w-9 h-9 rounded-full border border-accent flex items-center justify-center font-bold">{current?.initiative || '—'}</div><div className="flex-1"><span className="text-[10px] uppercase text-text-secondary">Agindo agora</span><b className="block">{current?.name || 'Nenhum'}</b></div><button disabled={!!busy || (turnCharacter && turnControl !== 'player')} onClick={() => void act('next', () => EncounterService.nextTurn(campaign.id))} className="p-2 border border-accent rounded text-accent"><ChevronRight /></button></div>
          <div className="flex flex-wrap gap-2 items-center text-xs"><span>Relógio mágico: {worldState.magicElapsedMinutes||0} min · {worldState.magicEnvironmentEffects?.length||0} efeitos ambientais</span><button disabled={!!busy} className="rounded border border-sky-700 px-2 py-1" onClick={()=>void act('magic-15min',()=>BitterReachMagicTimeService.advanceMinutes(campaign.id,15))}>Avançar turno de 15 min (não é rodada de combate)</button></div>
          {turnCharacter && turnControl !== 'player' && <div className="p-3 rounded-lg border border-accent/60 bg-accent/5 space-y-2">
            <b className="text-sm">{turnCharacter.name} · {CONTROL_LABELS[turnControl]}</b>
            <p className="text-xs">{combatPlan?.label || 'O Mestre está avaliando a situação tática.'}</p>
            {turnControl === 'curated' && combatPlan && <><label className="block text-xs">Ajustar tática antes de aprovar
              <select className="field mt-1 w-full" value={combatPlan.action} onChange={event => {
                const action = event.target.value as DelegatedCombatPlan['action'];
                setCombatPlan(currentPlan => currentPlan ? { ...currentPlan, action, label: action === 'guard' ? 'Manter guarda' : action === 'advance' ? 'Aproximar-se com cautela' : 'Atacar corpo a corpo' } : null);
              }}><option value="guard">Manter guarda</option><option value="advance" disabled={!combatPlan.targetId}>Avançar até adversário</option><option value="unarmed" disabled={!combatPlan.targetId}>Atacar corpo a corpo</option></select>
            </label><div className="flex gap-2">
              <button disabled={!!busy} onClick={() => void act('curated-combat', async () => {
                if (!active) return;
                await DelegatedCombatService.execute(campaign.id, active, turnCharacter, combatPlan, true);
                setCombatPlan(null);
              })} className="rounded bg-accent px-3 py-2 text-xs text-white disabled:opacity-40">Aprovar ação do Mestre</button>
              <button disabled={!!busy} onClick={() => { setCombatPlan(null); autoKey.current = ''; setCombatPlan(DelegatedCombatService.plan(active,turnCharacter)); }} className="rounded border border-border px-3 py-2 text-xs">Reconsiderar</button>
            </div></>}
          </div>}
          {isCurrentFireWyrm && <div className="rounded border border-orange-700 bg-orange-950/20 p-3 space-y-2 text-xs">
            <b className="text-orange-200">Aura térmica da Serpe · Bloodmarch p.111</b>
            <p>Obrigatória em cada turno ativo: 3 dados base, dano 1 (+1 por sucesso adicional), sem armadura, esquiva ou bloqueio. Classifique todas as criaturas, incluindo a conjuradora. O tabuleiro não permite inferir alcance com segurança.</p>
            {wyrmAuraTargets.map(target=><label key={target.id} className="block rounded border border-border p-2">
              <span>{target.name} · {target.status==='broken'?'Quebrado':target.side}</span>
              <select className="field mt-1 w-full" value={wyrmAuraReach[target.id]||''} disabled={auraResolved||!!busy}
                onChange={event=>{setWyrmAuraReach(prev=>({...prev,[target.id]:event.target.value as 'arms-reach'|'outside'}));setWyrmAuraConfirmed(false);}}>
                <option value="">Confirmar distância</option><option value="arms-reach">Ao alcance das mãos (aura)</option><option value="outside">Fora do alcance</option>
              </select>
            </label>)}
            {auraResolved?<p className="text-green-300">Aura resolvida nesta rodada.</p>:<>
              <label className="flex items-start gap-2"><input type="checkbox" checked={wyrmAuraConfirmed} onChange={event=>setWyrmAuraConfirmed(event.target.checked)} />Confirmo que classifiquei TODOS os seres presentes; nenhum alvo próximo foi omitido.</label>
              <button disabled={!!busy||!wyrmAuraConfirmed||wyrmAuraTargets.some(target=>!wyrmAuraReach[target.id])}
                onClick={()=>void act(`fire-wyrm-aura:${current?.id}`,()=>EncounterService.resolveFireWyrmAura(campaign.id,current!.id,
                  Object.fromEntries(wyrmAuraTargets.map(target=>[target.id,wyrmAuraReach[target.id]])) as Record<string,'arms-reach'|'outside'>))}
                className="w-full rounded bg-orange-800 p-2 font-bold disabled:opacity-40">Resolver aura sem consumir ação</button>
            </>}
          </div>}
          {active.combatants.some(item=>item.entityId==='core-bestiary:blood-mist-demon'&&item.status==='active')&&
           <div className="rounded border border-rose-700 p-2 space-y-2 text-xs"><b>Luz forte contra Sangrento · D3 Força (Livro do Mestre p.111)</b>
           <p>Selecione uma tocha/lanterna REAL do inventário, acesa e ao alcance das mãos. Confirme as duas condições abaixo. Cada exposição é registrada, não consome automaticamente o item nem permite repetir a mesma fonte nesta rodada.</p>
           {active.combatants.filter(item=>item.kind==='character'&&item.side==='party'&&item.status==='active').flatMap(combatant=>{
             const actor=characters.find(item=>item.id===combatant.entityId&&item.campaignId===campaign.id);
             return (actor?.gear||[]).flatMap((item:any,index:number)=>{
               if(!/tocha|torch|lanterna|lantern/i.test(String(item?.name||item?.label||'')))return [];
               const demon=active.combatants.find(c=>c.entityId==='core-bestiary:blood-mist-demon'&&c.status==='active');
               const key=`${active.id}:${active.round}:${actor!.id}:${index}`;
               const applied=demon?.statusEffects?.some(effect=>effect.id===`bloodmist:brightlight:${active.round}:${actor!.id}:${index}`);
               return [<div key={key} className="rounded border border-rose-900 p-2 space-y-1">
                 <label className="flex items-center gap-2"><input type="checkbox" checked={Boolean(sangrentoLightConfirm[key])} disabled={!!busy||applied} onChange={e=>setSangrentoLightConfirm(prev=>({...prev,[key]:e.target.checked}))}/>
                   Confirmo: {actor!.name} tem {String(item.name||item.label)} ACESA ao alcance das mãos</label>
                 <button disabled={!!busy||!sangrentoLightConfirm[key]||applied||!demon} className="rounded border border-red-600 px-3 py-1 disabled:opacity-50" onClick={()=>void act(`bloodmist-light:${key}`,()=>EncounterService.exposeBloodMistDemonToBrightLight(campaign.id,demon!.id,actor!.id,index,true))}>
                   {applied?'D3 já aplicado nesta rodada':'Rolar D3 e aplicar luz ao Sangrento'}</button></div>];
             });
           })}</div>}
          {current?.entityId==='core-bestiary:blood-mist-demon'&&
           <div className="rounded border border-rose-800 p-2 space-y-2 text-xs">
             <p>Forma atual: {current.statusEffects?.some(effect=>effect.id==='bloodmist:incorporeal')?'Névoa incorpórea · imune a ataques físicos':'Materializado · cobre causa dano normal'}. Mudança explícita da Mestra; o livro não fixa recarga.</p>
             <button disabled={!!busy} onClick={()=>void act(`bloodmist-form:${current.id}`,()=>EncounterService.setBloodMistDemonForm(campaign.id,current.id,
               current.statusEffects?.some(effect=>effect.id==='bloodmist:incorporeal')?'manifested':'mist'))}
               className="rounded border border-rose-700 px-3 py-2 disabled:opacity-50">{current.statusEffects?.some(effect=>effect.id==='bloodmist:incorporeal')?'Materializar Sangrento':'Tornar Sangrento incorpóreo'}</button>
           </div>}
          {current?.entityId==='core-bestiary:blood-mist-demon'&&<p className="rounded border border-rose-800 bg-rose-950/30 p-2 text-xs text-rose-100">Sangrento (Livro do Mestre pp. 110–111): Força 8, Agilidade 4, Armadura 6. Cobre causa dano normal após armadura; demais armas causam metade (arredondada para cima). Em forma incorpórea, ataques físicos não causam dano. Tocha ou lanterna acesa ao alcance das mãos causa D3 adicional de Força: a exposição à luz exige confirmação e rolagem D3 pelo botão acima.</p>}
          {current?.kind==='npc'&&current.side==='enemy'&&
           (current.entityId?.startsWith('official-slaver:')||current.entityId?.startsWith('official-orc-escort:')||current.entityId?.startsWith('official-hungry-bandit:')||current.entityId==='official-khardnal'||current.entityId?.startsWith('official-r88-soul-merchant-guard:')||current.entityId?.startsWith('official-r88-saurian-hunter:')||current.entityId?.startsWith('official-r88-bounty-hunter:'))&&
           <div className="rounded border border-red-800 p-3 space-y-2 text-xs">
             <b>Turno de PNJ oficial · ataque corpo a corpo</b>
             <p>Força + Luta, arma e armadura da ficha oficial. O alvo pode Esquivar ou Bloquear; a defesa consome ação. Sem bônus de equipamento presumido.</p>
             <select className="field w-full" value={npcTargetId} onChange={e=>setNpcTargetId(e.target.value)}>
               <option value="">Escolha o aventureiro alvo</option>
               {active.combatants.filter(c=>c.kind==='character'&&c.side==='party'&&c.status==='active').map(c=><option key={c.id} value={c.id}>{c.name}</option>)}
             </select>
             <select className="field w-full" value={npcDefense} onChange={e=>setNpcDefense(e.target.value as 'none'|'dodge'|'block')}>
               <option value="none">Sem defesa reativa</option><option value="dodge">Esquivar (Agilidade + Movimentação)</option><option value="block">Bloquear (Força + Luta)</option>
             </select>
             <label className="flex items-center gap-2"><input type="checkbox" checked={npcReachConfirmed} onChange={e=>setNpcReachConfirmed(e.target.checked)}/>Confirmo que o alvo está ao alcance das mãos</label>
             <button disabled={!!busy||!current.slowActionAvailable||!npcTargetId||!npcReachConfirmed}
               onClick={()=>void act(`official-npc-melee:${current.id}`,()=>EncounterService.resolveOfficialNpcMelee(campaign.id,current.id,npcTargetId,npcDefense,npcReachConfirmed))}
               className="rounded border border-red-700 px-3 py-2 disabled:opacity-50">Rolar ataque e aplicar dano real</button>
           </div>}
          {current?.entityId==='official-generated-grelf'&&<div className="rounded border border-purple-700 p-3 space-y-2"><p>Escolha um ataque obtido na geração oficial. As regras situacionais da ficha continuam exigindo a confirmação da Mestra na jornada.</p>{(current.quickSheet?.generatedAttacks as import('../services/OfficialDemonGenerator').GeneratedDemonAttack[]||[]).map((attack,index)=><button key={attack.id} disabled={!!busy||!current.slowActionAvailable||!partyTargets.length} className="rounded border border-purple-700 px-3 py-2" onClick={()=>void act(`grelf-attack:${index}`,()=>EncounterService.resolveOfficialGrelfAttack(campaign.id,current.id,partyTargets[0].id,index))}>Grelf: {attack.id} · {attack.baseDice} dados · alvo {partyTargets[0]?.name}</button>)}</div>}
          {isOfficialProcessionEnt&&!pendingProcessionEntAttack&&<button disabled={!!busy||current?.status!=='active'||!current.slowActionAvailable} onClick={()=>void act(`ent-table:${current?.id}`,async()=>{await EncounterService.prepareOfficialProcessionEntAttack(campaign.id,current!.id);setMonsterTargetIds([]);})} className="w-full rounded border border-red-700 bg-red-950/30 p-3 text-left text-sm disabled:opacity-50">Rolar o D6 da tabela oficial do Ent</button>}
          {isOfficialProcessionEnt&&pendingProcessionEntAttack&&<div className="rounded border border-amber-800 p-3 text-xs"><b>Ent: D6 {pendingProcessionEntAttack.dieValue} — {pendingProcessionEntAttack.attackName}</b><p className="mt-1">Alcance: {pendingProcessionEntRow?.range==='short'?'Curto':'Perto'}. {pendingProcessionEntRow?.tags?.includes('area')?'Selecione todos os alvos dentro de Perto.':pendingProcessionEntRow?.tags?.includes('two_targets')?'Selecione exatamente dois alvos dentro de Perto (ou todos se houver menos de dois).':'Selecione um alvo dentro do alcance indicado.'} A ação lenta já foi consumida; o turno não pode avançar antes de resolver.</p></div>}
          {current?.kind==='monster'&&Array.isArray(current.quickSheet?.monsterAttackTable)&&<div className="rounded border border-red-900/70 bg-red-950/20 p-3"><b className="text-sm text-red-200">Ataque de monstro · Book of Beasts</b><p className="text-xs text-text-secondary mt-1">Rola a tabela D6 e faz uma rolagem de ataque para cada alvo escolhido. Em ataques de dois alvos, marque exatamente dois; em área, marque todos que estiverem no alcance. Medo, venenos, agarrões, arrasto, ingestão, roubo mágico, teias, petrificação, afogamento, possessão e ataques combinados de medo/sufocamento passam pelo motor; efeitos ainda sem regra catalogada ficam identificados como pendência.</p>{(current.quickSheet.monsterAttackTable as Array<{tags?:string[]}>).some(row=>row.tags?.includes('fear'))&&<label className="mt-2 block text-xs">Destemido 2 · atributo do dano de medo<select value={fearDamageAttribute} onChange={event=>setFearDamageAttribute(event.target.value as 'wits'|'empathy')} className="field mt-1"><option value="wits">Astúcia</option><option value="empathy">Empatia</option></select></label>}{isWyrmAttacker&&<div className="mt-3 rounded border border-orange-700 p-3 space-y-2 text-xs">
              <b>Serpe/Kton · alcance dos ataques especiais 3, 4 e 6</b>
              <p>Classifique TODOS os outros alvos elegíveis antes de rolar o D6. O motor não inventa distâncias. Céu Escurecido usa alcance das mãos; Ameaça do Subsolo e Solo em Colapso usam Perto, incluindo alcance das mãos. A conjuradora é excluída dos ataques, não da aura.</p>
              {partyTargets.map(target=><label key={target.id} className="block">{target.name}
                <select className="field mt-1 w-full" value={wyrmAreaZones[target.id]||''} disabled={!!busy}
                  onChange={event=>setWyrmAreaZones(prev=>({...prev,[target.id]:event.target.value as 'arms-reach'|'near'|'outside'}))}>
                  <option value="">Confirmar distância</option><option value="arms-reach">Alcance das mãos</option><option value="near">Perto, fora das mãos</option><option value="outside">Fora de Perto</option>
                </select></label>)}
              <label>Superfície no fundo do poço (resultado D6=6)
                <select className="field mt-1 w-full" value={wyrmHardSurface} onChange={event=>setWyrmHardSurface(event.target.value as 'hard'|'soft'|'')} disabled={!!busy}>
                  <option value="">Confirmar tipo de superfície</option><option value="hard">Dura (queda causa rolagem de dano)</option><option value="soft">Não dura (sem dano de queda por esta regra)</option>
                </select></label>
            </div>}<div className="mt-2 grid gap-1">{partyTargets.map(item=><label key={item.id} className="flex items-center gap-2 rounded border border-border bg-bg-base px-2 py-1 text-xs"><input type="checkbox" checked={monsterTargetIds.includes(item.id)} onChange={event=>setMonsterTargetIds(previous=>event.target.checked?[...previous,item.id]:previous.filter(id=>id!==item.id))}/><span className="flex-1">{item.name} · Força {item.currentStrength}</span></label>)}</div><button disabled={isOfficialProcessionEnt||!!busy||!monsterTargetIds.length||!current.slowActionAvailable||(isWyrmAttacker&&(!wyrmHardSurface||partyTargets.some(target=>!wyrmAreaZones[target.id])))} onClick={()=>void act(`monster:${current.id}`,()=>EncounterService.resolveBookOfBeastsAttack(campaign.id,current.id,monsterTargetIds,undefined,fearDamageAttribute,
            isWyrmAttacker?{zones:Object.fromEntries(partyTargets.map(target=>[target.id,wyrmAreaZones[target.id]])) as Record<string,'arms-reach'|'near'|'outside'>,hardSurface:wyrmHardSurface==='hard'}:undefined))} className="mt-2 w-full rounded bg-red-900 px-3 py-2 text-xs font-bold disabled:opacity-40">Rolar D6 e resolver</button></div>}
          {isOfficialProcessionEnt&&pendingProcessionEntAttack&&<button disabled={!!busy||processionEntTargetCountInvalid} onClick={()=>void act(`ent-attack:${current?.id}`,()=>EncounterService.resolveBookOfBeastsAttack(campaign.id,current!.id,monsterTargetIds,undefined,fearDamageAttribute,undefined,pendingProcessionEntAttack.dieValue))} className="mb-2 w-full rounded bg-red-900 px-3 py-2 text-xs font-bold disabled:opacity-40">Resolver ataque do Ent · D6 {pendingProcessionEntAttack.dieValue}</button>}
          <div className="space-y-2">{active.combatants.map((combatant, index) => <div key={combatant.id} className={`p-3 rounded border ${index === active.turnIndex ? 'border-accent bg-accent/5' : 'border-border'} ${combatant.status !== 'active' ? 'opacity-60' : ''}`}>
            <div className="flex items-center gap-2"><span className="w-7 h-7 rounded-full border border-border flex items-center justify-center text-xs">{combatant.initiative}</span><div className="flex-1"><b className="text-sm">{combatant.name}</b><p className="text-[11px] text-text-secondary">{combatant.side === 'party' ? 'Grupo' : combatant.side === 'enemy' ? 'Adversário' : 'Neutro'} · {STATUS_LABELS[combatant.status]} · Armadura {combatant.armor}</p></div><div className="text-sm font-bold">Força {combatant.currentStrength}/{combatant.maxStrength}</div></div>
            {combatant.summonedElemental&&<div className="mt-2 rounded border border-sky-800/70 bg-sky-950/20 p-2 space-y-2 text-xs">
              <p>Invocação de The Bitter Reach · {combatant.summonedElemental.kind} · poder {combatant.summonedElemental.power} · termina em {Math.max(0,combatant.summonedElemental.expiresAtMinute-(worldState.magicElapsedMinutes||0))} min de jogo.</p>
              <label>Alvo da ordem<select className="field mt-1" value={elementalTargetIds[combatant.id]||combatant.summonedElemental.commandTargetId||''} onChange={event=>setElementalTargetIds(current=>({...current,[combatant.id]:event.target.value}))}>
                <option value="">Selecione o alvo</option>{active.combatants.filter(item=>item.id!==combatant.id&&item.side!==combatant.side&&item.status==='active').map(item=><option key={item.id} value={item.id}>{item.name}</option>)}
              </select></label>
              <label className="flex gap-2 items-center"><input type="checkbox" checked={Boolean(elementalRangeConfirmed[combatant.id])} onChange={event=>setElementalRangeConfirmed(previous=>({...previous,[combatant.id]:event.target.checked}))}/>Confirme alcance real: {combatant.summonedElemental.kind==='earth'?'ao alcance das mãos':'Perto'}. Coordenadas da tela não bastam.</label>
              <div className="flex flex-wrap gap-2">
                <button className="rounded border border-sky-500 px-2 py-1 disabled:opacity-40" disabled={!!busy||current?.entityId!==combatant.summonedElemental.casterId||!((elementalTargetIds[combatant.id]||combatant.summonedElemental.commandTargetId))||!elementalRangeConfirmed[combatant.id]} onClick={()=>void act(`elemental-command:${combatant.id}`,()=>BitterReachElementalCombatService.command(campaign.id,combatant.summonedElemental!.casterId,combatant.id,elementalTargetIds[combatant.id]||combatant.summonedElemental!.commandTargetId!,Boolean(elementalRangeConfirmed[combatant.id])))}>Ordenar · grátis no turno do conjurador</button>
                <button className="rounded bg-sky-900 px-2 py-1 disabled:opacity-40" disabled={!!busy||current?.id!==combatant.id||!combatant.slowActionAvailable||!combatant.summonedElemental.commandTargetId} onClick={()=>void act(`elemental-attack:${combatant.id}`,()=>BitterReachElementalCombatService.attack(campaign.id,combatant.id,combatant.summonedElemental!.commandTargetId!))}>Atacar · ação lenta</button>
                <button className="rounded border border-border px-2 py-1 disabled:opacity-40" disabled={!!busy||!characters.some(c=>c.id===combatant.summonedElemental!.casterId&&CharacterControlService.mode(c)==='player')} onClick={()=>void act(`elemental-dismiss:${combatant.id}`,()=>BitterReachElementalCombatService.dismiss(campaign.id,combatant.summonedElemental!.casterId,combatant.id))}>Dispensar</button>
              </div>
            </div>}
            {!!combatant.statusEffects?.length && <div className="mt-2 space-y-1 text-xs text-amber-200">{combatant.statusEffects.map(effect => <p key={effect.id}><b>{effect.name}</b>{effect.detail?` — ${effect.detail}`:''}</p>)}</div>}
            {combatant.statusEffects?.some(effect=>effect.id.startsWith('official-ent-roots:')||effect.id.startsWith('official-ent-embrace:'))&&<button disabled={!!busy} onClick={()=>void act(`ent-external-rescue:${combatant.id}`,()=>EncounterService.recordOfficialEntExternalRescue(campaign.id,combatant.id))} className="mt-2 rounded border border-emerald-700 px-2 py-1 text-xs text-emerald-200 disabled:opacity-40">Registrar resgate externo / libertação adjudicada</button>}
            <div className="flex flex-wrap gap-2 mt-2">
              {combatant.statusEffects?.some(effect => effect.id === 'condition:prone') && <button disabled={!!busy || index !== active.turnIndex || combatant.status !== 'active' || !combatant.fastActionAvailable} onClick={() => void act(`stand:${combatant.id}`, () => EncounterService.standUp(campaign.id, combatant.id))} className="px-2 py-1 border border-amber-700 text-amber-200 rounded text-xs disabled:opacity-40">Levantar · ação rápida</button>}
              {combatant.statusEffects?.some(effect => effect.id.startsWith('monster-webbed:')) && <button disabled={!!busy || index !== active.turnIndex || combatant.status !== 'active' || !combatant.slowActionAvailable} onClick={() => void act(`web:${combatant.id}`, () => EncounterService.breakFreeWeb(campaign.id, combatant.id))} className="px-2 py-1 border border-red-700 text-red-200 rounded text-xs disabled:opacity-40">Romper teia · ação lenta</button>}
              {combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-grappled:fire-wyrm-mouth:'))&&<p className="text-xs text-orange-300">Na boca da Serpe: não use o teste comum de Potência. Cause 4+ dano em um ataque contra ela para sair; a criatura não tem armadura contra golpes de dentro.</p>}
              {combatant.statusEffects?.some(effect => effect.id.startsWith('monster-grappled:')&&!effect.id.startsWith('monster-grappled:fire-wyrm-mouth:')) && <button disabled={!!busy || index !== active.turnIndex || combatant.status !== 'active' || !combatant.slowActionAvailable} onClick={() => void act(`grapple:${combatant.id}`, () => EncounterService.breakFreeGrapple(campaign.id, combatant.id))} className="px-2 py-1 border border-orange-700 text-orange-200 rounded text-xs disabled:opacity-40">Escapar do agarrão · ação lenta</button>}
              {combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-dragged:'))&&<button disabled={!!busy||index!==active.turnIndex||combatant.status!=='active'||!combatant.slowActionAvailable} onClick={()=>void act(`drag:${combatant.id}`,()=>EncounterService.breakFreeFromDrag(campaign.id,combatant.id))} className="px-2 py-1 border border-orange-700 text-orange-200 rounded text-xs disabled:opacity-40">Escapar do arrasto · ação lenta</button>}
              {combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-possessed:'))&&<button disabled={!!busy||index!==active.turnIndex||combatant.status!=='active'} onClick={()=>void act(`possessed:${combatant.id}`,()=>EncounterService.resistPossession(campaign.id,combatant.id))} className="px-2 py-1 border border-purple-700 text-purple-200 rounded text-xs disabled:opacity-40">Romper possessão · Astúcia + Discernimento · grátis</button>}
              {combatant.statusEffects?.some(effect => effect.id === 'drowning') && current?.kind === 'character' && current.entityId && current.status === 'active' && current.slowActionAvailable && current.id !== combatant.id && <button disabled={!!busy || index !== active.turnIndex || current.status !== 'active' || !current.slowActionAvailable} onClick={() => void act(`rescue:${combatant.id}`, () => EncounterService.rescueDrowning(campaign.id, current.id, combatant.id))} className="px-2 py-1 border border-blue-700 text-blue-200 rounded text-xs disabled:opacity-40">Resgatar do afogamento · ação lenta</button>}
              {combatant.statusEffects?.some(effect => effect.id==='basilisk-warts') && <button disabled={!!busy || active.combatants[active.turnIndex]?.kind!=='character' || !active.combatants[active.turnIndex]?.slowActionAvailable} onClick={() => void act(`warts:${combatant.id}`, () => EncounterService.treatBasiliskWarts(campaign.id, combatant.id))} className="px-2 py-1 border border-green-700 text-green-200 rounded text-xs disabled:opacity-40">Tratar verrugas com Cura · ação lenta</button>}
              {combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-eggs:'))&&<button disabled={!!busy||active.combatants[active.turnIndex]?.kind!=='character'||!active.combatants[active.turnIndex]?.slowActionAvailable} onClick={()=>void act(`eggs:${combatant.id}`,()=>EncounterService.treatSkolopendraEggs(campaign.id,combatant.id))} className="px-2 py-1 border border-green-700 text-green-200 rounded text-xs disabled:opacity-40">Tratar ovos com Cura · ação lenta</button>}
              {combatant.statusEffects?.some(effect=>effect.id.startsWith('monster-consumed:')&&String(effect.source||'').toLocaleLowerCase().includes('shapeshifter'))&&<button disabled={!!busy||active.combatants[active.turnIndex]?.kind!=='character'||active.combatants[active.turnIndex]?.status!=='active'||active.combatants[active.turnIndex]?.id===combatant.id||!active.combatants[active.turnIndex]?.slowActionAvailable} onClick={()=>void act(`consume:${combatant.id}`,()=>EncounterService.freeConsumedAlly(campaign.id,combatant.id))} className="px-2 py-1 border border-green-700 text-green-200 rounded text-xs disabled:opacity-40">Libertar aliado do Metamorfo · ação lenta</button>}
              <button disabled={!!busy || combatant.currentStrength <= 0} onClick={() => void act(`damage:${combatant.id}`, () => EncounterService.adjustStrength(campaign.id, combatant.id, -1))} className="px-2 py-1 border border-border rounded text-xs flex items-center gap-1"><CircleMinus className="w-3 h-3" />Dano</button>
              <button disabled={!!busy || combatant.currentStrength >= combatant.maxStrength} onClick={() => void act(`heal:${combatant.id}`, () => EncounterService.adjustStrength(campaign.id, combatant.id, 1))} className="px-2 py-1 border border-border rounded text-xs flex items-center gap-1"><CirclePlus className="w-3 h-3" />Recuperar</button>
              <button disabled={!!busy || index !== active.turnIndex || combatant.status !== 'active' || !combatant.fastActionAvailable} onClick={() => void act(`fast:${combatant.id}`, () => EncounterService.spendAction(campaign.id, combatant.id, 'fast'))} className="px-2 py-1 border border-border rounded text-xs">Rápida {combatant.fastActionAvailable ? '✓' : '—'}</button>
              <button disabled={!!busy || index !== active.turnIndex || combatant.status !== 'active' || !combatant.slowActionAvailable} onClick={() => void act(`slow:${combatant.id}`, () => EncounterService.spendAction(campaign.id, combatant.id, 'slow'))} className="px-2 py-1 border border-border rounded text-xs">Lenta {combatant.slowActionAvailable ? '✓' : '—'}</button>
              <select disabled={!!busy} value={combatant.status} onChange={event => void act(`status:${combatant.id}`, () => EncounterService.setStatus(campaign.id, combatant.id, event.target.value as EncounterParticipantStatus))} className="bg-bg-base border border-border rounded px-2 text-xs"><option value="active">Ativo</option><option value="broken">Quebrado</option><option value="dead">Morto</option><option value="fled">Fugiu</option><option value="yielded">Rendeu-se</option></select>
            </div>
            {combatant.notes && <details className="mt-2"><summary className="text-[11px] text-accent cursor-pointer">Dados e ataques</summary><p className="text-xs text-text-secondary mt-1">{combatant.notes}</p></details>}
          </div>)}</div>
          {!!active.log.length && <details className="border border-border rounded"><summary className="p-3 cursor-pointer text-sm">Registro do encontro</summary><div className="p-3 pt-0 space-y-1">{active.log.slice(-20).reverse().map(item => <p key={item.id} className="text-xs text-text-secondary"><b>R{item.round}:</b> {item.text}</p>)}</div></details>}
          <button disabled={!!busy} onClick={() => void act('end', () => EncounterService.end(campaign.id))} className="w-full p-3 border border-red-700 text-red-300 rounded">Encerrar encontro</button>
        </>}
      </div>}
    </section>

    {modal && <div className="fixed inset-0 z-[195] bg-black/85 flex items-center justify-center p-3"><div className="bg-bg-surface border border-accent rounded-xl w-full max-w-2xl max-h-[90vh] flex flex-col">
      <div className="p-4 border-b border-border flex items-center gap-3"><Skull className="text-accent" /><div className="flex-1"><h2 className="text-xl font-bold">Iniciar encontro</h2><p className="text-xs text-text-secondary">Todos os heróis do grupo serão incluídos. Escolha apenas os adversários.</p></div><button onClick={() => setModal(false)} className="p-2 border border-border rounded"><X /></button></div>
      <div className="p-4 overflow-y-auto space-y-3">{pendingOfficialCombats.length>0&&<label className="block rounded border border-amber-700 p-3 text-sm">Combate oficial pendente: vínculo obrigatório<select className="field mt-2 w-full" value={linkedJourneyEventId} onChange={event=>setLinkedJourneyEventId(event.target.value)}><option value="">Selecione a ocorrência oficial</option>{pendingOfficialCombats.map(event=><option key={event.id} value={event.id}>{event.title}</option>)}</select><span className="text-xs text-text-secondary">Monte a oposição real. O resultado será gravado na ocorrência da jornada.</span></label>}<label className="block text-sm">Título<input value={title} onChange={event => setTitle(event.target.value)} className="field mt-1" /></label>{!entries.length ? <p className="text-text-secondary">Nenhuma criatura salva no Bestiário. Gere criaturas na aba Bestiário primeiro.</p> : entries.map(entry => <div key={entry.id} className="p-3 border border-border rounded flex items-center gap-3"><Skull className="w-5 h-5 text-text-secondary" /><div className="flex-1"><b>{entry.name}</b><p className="text-xs text-text-secondary">Força {entry.mechanicalData?.attributes?.strength || 0} · Armadura {entry.armor || 0}</p></div><input type="number" min={0} max={20} value={quantities[entry.id] || 0} onChange={event => setQuantities(current => ({ ...current, [entry.id]: Math.max(0, Math.min(20, Number(event.target.value) || 0)) }))} className="w-16 bg-bg-base border border-border rounded p-2 text-center" /></div>)}</div>
      <div className="p-4 border-t border-border flex gap-2"><button onClick={() => setModal(false)} className="flex-1 p-3 border border-border rounded">Cancelar</button><button disabled={!selections.length || !!busy || (pendingOfficialCombats.length>0&&!linkedJourneyEventId)} onClick={() => void start()} className="flex-1 p-3 bg-accent text-white rounded disabled:opacity-50 flex items-center justify-center gap-2"><Shield className="w-4 h-4" />Começar</button></div>
    </div></div>}
  </>;
}
