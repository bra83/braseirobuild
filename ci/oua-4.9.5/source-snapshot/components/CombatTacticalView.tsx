import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Crosshair, Dices, ImagePlus, Info, Move, Play, Save, Shield, SkipForward, Swords, UserPlus, X } from 'lucide-react';
import type { Character, IndividualToken, Npc } from '../lib/db';
import { getStoreData } from '../lib/db';
import type { CombatActorState, CombatStance, MasterCommand, MasterState } from '../lib/master/types';
import { rollOneRing } from '../lib/dice';

interface CombatTacticalViewProps {
  masterState: MasterState;
  characters: Character[];
  npcs: Npc[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => Promise<void>;
  onAskMasterAction: (action: string) => Promise<void>;
  onOpenDice: () => void;
}

const STANCE_LABEL: Record<CombatStance, string> = {
  forward: 'Avançada', open: 'Aberta', defensive: 'Defensiva', rearward: 'Retaguarda',
};

function uid(prefix: string): string { return `${prefix}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`; }

export const CombatTacticalView: React.FC<CombatTacticalViewProps> = ({ masterState, characters, npcs, onApplyCommands, onAskMasterAction, onOpenDice }) => {
  const combat = masterState.combat;
  const [tokenUrls, setTokenUrls] = useState<Record<string, string>>({});
  const [selectedActor, setSelectedActor] = useState<CombatActorState | null>(null);
  const [showAdd, setShowAdd] = useState(false);
  const [actorName, setActorName] = useState('');
  const [actorType, setActorType] = useState<'npc' | 'adversary'>('adversary');
  const [actorEndurance, setActorEndurance] = useState(10);
  const [actorHate, setActorHate] = useState(2);
  const [narrative, setNarrative] = useState('');
  const [rollDice, setRollDice] = useState(2);
  const [rollTarget, setRollTarget] = useState(14);
  const [rollModifier, setRollModifier] = useState(0);
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const [status, setStatus] = useState('');
  const boardRef = useRef<HTMLDivElement>(null);
  const backgroundInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    let disposed = false;
    const urls: string[] = [];
    void (async () => {
      const tokens = await getStoreData<IndividualToken>('individual_tokens').catch(() => []);
      const result: Record<string, string> = {};
      for (const token of tokens) {
        if (!token.blob) continue;
        const url = URL.createObjectURL(token.blob); urls.push(url); result[token.id] = url;
      }
      if (!disposed) setTokenUrls(result);
    })();
    return () => { disposed = true; urls.forEach(url => URL.revokeObjectURL(url)); };
  }, [combat.actors.map(actor => actor.tokenId).join('|')]);

  const isActive = combat.status !== 'idle' && combat.status !== 'resolved';
  const previewActors = useMemo<CombatActorState[]>(() => {
    const heroes: CombatActorState[] = characters.map((character, index) => ({
      actorId: character.id,
      actorType: 'hero',
      stance: 'open',
      engagedWith: [],
      endurance: Math.max(0, Number(character.sheet.attributes.enduranceCurrent || 0)),
      enduranceMax: Math.max(1, Number(character.sheet.attributes.enduranceMax || 1)),
      wounded: Boolean(character.sheet.attributes.wounded),
      defeated: Number(character.sheet.attributes.enduranceCurrent || 0) <= 0,
      initiative: index + 1,
      tokenId: character.sheet.tokenId || null,
      position: { x: 0.18 + (index % 3) * 0.32, y: 0.76 + Math.floor(index / 3) * 0.13 },
      parry: Number(character.sheet.combat.parry || 0),
      protection: Number(character.sheet.attributes.protection || 0),
      shadow: Number(character.sheet.attributes.shadow || 0),
      hope: Number(character.sheet.attributes.hopeCurrent || 0),
      fatigue: Number(character.sheet.attributes.fatigue || 0),
      quickSheet: {
        name: character.name,
        culture: character.culture,
        vocation: character.vocation,
        attributes: character.sheet.attributes,
        skills: character.sheet.skills,
        weapons: character.sheet.combat.weapons,
        armor: character.sheet.combat.armor,
        shield: character.sheet.combat.shield,
      },
      stateNotes: [],
    }));
    const sceneActors: CombatActorState[] = npcs.map((npc, index) => ({
      actorId: npc.id,
      actorType: /or[cç]|inimig|advers|hostil|servo|bandido|aranha|troll|orc/i.test(`${npc.role} ${npc.faction} ${npc.attitude}`) ? 'adversary' : 'npc',
      engagedWith: [],
      endurance: 10,
      enduranceMax: 10,
      hate: 2,
      wounded: false,
      defeated: false,
      initiative: Math.max(1, 10 - index),
      tokenId: npc.tokenId || null,
      position: { x: 0.18 + (index % 3) * 0.32, y: 0.20 + Math.floor(index / 3) * 0.13 },
      quickSheet: { name: npc.name, role: npc.role, culture: npc.culture || npc.peoples, faction: npc.faction, personality: npc.personality, attitude: npc.attitude, objectives: npc.objectives, fears: npc.fears, nextAction: npc.nextAction },
      stateNotes: [],
    }));
    return [...heroes, ...sceneActors];
  }, [characters, npcs]);
  const tacticalActors = useMemo(() => {
    if (isActive) return combat.actors;
    const persisted = new Map(combat.actors.map(actor => [actor.actorId, actor]));
    return previewActors.map(actor => {
      const saved = persisted.get(actor.actorId);
      return saved ? { ...actor, tokenId: saved.tokenId || actor.tokenId, position: saved.position || actor.position } : actor;
    });
  }, [isActive, combat.actors, previewActors]);
  const activeActors = useMemo(() => tacticalActors.filter(actor => !actor.defeated), [tacticalActors]);
  const currentActor = isActive ? (activeActors[Math.min(Math.max(0, combat.turnIndex || 0), Math.max(0, activeActors.length - 1))] || null) : null;
  const heroIds = new Set(characters.map(character => character.id));
  const selectedCharacter = selectedActor?.actorType === 'hero' ? characters.find(character => character.id === selectedActor.actorId) || null : null;
  const selectedNpc = selectedActor && selectedActor.actorType !== 'hero' ? npcs.find(npc => npc.id === selectedActor.actorId) || null : null;

  const start = async () => {
    const actorIds = [...characters.map(character => character.id), ...npcs.map(npc => npc.id)];
    await onApplyCommands([{ type: 'start_combat', actorIds, reason: 'Combate iniciado manualmente na mesa tática com a Companhia e os NPCs presentes.' }], 'iniciar combate tático');
  };

  const addActor = async () => {
    const name = actorName.trim();
    if (!name) return;
    const npc = npcs.find(item => item.name.localeCompare(name, 'pt-BR', { sensitivity: 'base' }) === 0);
    const actor: CombatActorState = {
      actorId: npc?.id || uid(actorType),
      actorType,
      engagedWith: [],
      endurance: Math.max(1, actorEndurance),
      enduranceMax: Math.max(1, actorEndurance),
      hate: actorType === 'adversary' ? Math.max(0, actorHate) : undefined,
      wounded: false,
      defeated: false,
      initiative: Math.max(1, 10 - combat.actors.length),
      tokenId: npc?.tokenId || null,
      position: { x: 0.72 + (combat.actors.length % 2) * 0.14, y: 0.24 + Math.floor(combat.actors.length / 2) * 0.12 },
      quickSheet: npc ? {
        name: npc.name, culture: npc.culture || npc.peoples, role: npc.role, faction: npc.faction,
        appearance: npc.appearance, personality: npc.personality, objectives: npc.objectives,
        fears: npc.fears, attitude: npc.attitude, emotionalState: npc.emotionalState, nextAction: npc.nextAction,
      } : { name, kind: 'Adversário', note: 'Estatísticas completas podem ser atualizadas pelo Mestre conforme o manual recuperado.' },
      stateNotes: [],
    };
    await onApplyCommands([{ type: 'upsert_combat_actor', actor, reason: `${name} entrou no campo tático.` }], 'adicionar combatente');
    setActorName(''); setShowAdd(false);
  };

  const moveActor = async (actorId: string, clientX: number, clientY: number) => {
    const rect = boardRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = Math.max(0.06, Math.min(0.94, (clientX - rect.left) / rect.width));
    const y = Math.max(0.07, Math.min(0.93, (clientY - rect.top) / rect.height));
    if (isActive) {
      await onApplyCommands([{ type: 'move_combat_actor', actorId, x, y, reason: 'Token movido na mesa tática.' }], 'mover token tático');
      return;
    }
    const actor = tacticalActors.find(item => item.actorId === actorId);
    if (!actor) return;
    await onApplyCommands([{ type: 'upsert_combat_actor', actor: { ...actor, position: { x, y } }, reason: 'Posição preparada antes do início do confronto.' }], 'posicionar token na prévia tática');
  };

  const performRoll = async () => {
    const roll = rollOneRing(Math.max(0, rollDice), rollModifier, rollTarget, 'Combate');
    await onApplyCommands([{ type: 'record_combat_roll', roll: {
      id: roll.id,
      actorId: currentActor?.actorId,
      label: currentActor ? `Ação de ${String(currentActor.quickSheet?.name || currentActor.actorId)}` : 'Rolagem de combate',
      featDie: roll.featResult === 12 ? 'gandalf' : roll.featResult === 11 ? 'eye' : (roll.featResult || 0),
      successDice: roll.successResults || [],
      total: roll.total,
      targetNumber: roll.targetNumber,
      success: roll.success,
      tengwar: roll.tengwarCount || 0,
      createdAt: roll.timestamp,
    }, reason: 'Rolagem feita na mesa tática.' }], 'registrar rolagem de combate');
    setStatus(`${roll.success ? 'Sucesso' : 'Falha'} · total ${roll.total} · ${roll.tengwarCount || 0} Tengwar.`);
  };

  const saveNarrative = async (sendToMaster: boolean) => {
    const text = narrative.trim();
    if (!text) return;
    await onApplyCommands([{ type: 'record_combat_narrative', text, reason: 'Bloco narrativo registrado na batalha.' }], 'registrar narrativa do combate');
    if (sendToMaster) await onAskMasterAction(`[AÇÃO DURANTE O COMBATE — RODADA ${combat.round || 1}] ${text}`);
    setNarrative('');
  };

  const importBackground = async (file?: File) => {
    if (!file || !file.type.startsWith('image/')) return;
    const data = await new Promise<string>((resolve, reject) => { const reader = new FileReader(); reader.onload = () => typeof reader.result === 'string' ? resolve(reader.result) : reject(new Error('Imagem inválida.')); reader.onerror = () => reject(reader.error); reader.readAsDataURL(file); });
    await onApplyCommands([{ type: 'set_combat_background', imageData: data, reason: 'Cenário visual vinculado ao encontro.' }], 'definir cenário da batalha');
  };

  const updateSelectedEndurance = async (delta: number) => {
    if (!selectedActor || !Number.isFinite(delta) || delta === 0) return;
    if (selectedActor.actorType === 'hero' && heroIds.has(selectedActor.actorId)) {
      await onApplyCommands([{ type: 'adjust_endurance', targetCharacterId: selectedActor.actorId, amount: delta, reason: 'Resistência alterada pela mesa tática.' }], 'alterar Resistência no combate');
    } else {
      const endurance = Math.max(0, Math.min(selectedActor.enduranceMax, selectedActor.endurance + Math.trunc(delta)));
      await onApplyCommands([{ type: 'upsert_combat_actor', actor: { ...selectedActor, endurance, defeated: endurance <= 0 }, reason: 'Resistência do NPC/adversário alterada pela mesa tática.' }], 'alterar Resistência do combatente');
    }
    setSelectedActor(null);
  };

  const toggleSelectedWounded = async () => {
    if (!selectedActor) return;
    const value = !selectedActor.wounded;
    if (selectedActor.actorType === 'hero' && heroIds.has(selectedActor.actorId)) {
      await onApplyCommands([{ type: 'set_condition', targetCharacterId: selectedActor.actorId, condition: 'wounded', value, reason: 'Ferimento alterado pela mesa tática.' }], 'alterar Ferimento no combate');
    } else {
      await onApplyCommands([{ type: 'upsert_combat_actor', actor: { ...selectedActor, wounded: value }, reason: 'Ferimento do NPC/adversário alterado pela mesa tática.' }], 'alterar Ferimento do combatente');
    }
    setSelectedActor(null);
  };

  const updateSelectedResource = async (delta: number) => {
    if (!selectedActor || !Number.isFinite(delta) || delta === 0) return;
    if (selectedActor.actorType === 'hero' && heroIds.has(selectedActor.actorId)) {
      await onApplyCommands([{ type: 'adjust_hope', targetCharacterId: selectedActor.actorId, amount: delta, reason: 'Esperança alterada pela mesa tática.' }], 'alterar Esperança no combate');
    } else {
      await onApplyCommands([{ type: 'upsert_combat_actor', actor: { ...selectedActor, hate: Math.max(0, (selectedActor.hate || 0) + Math.trunc(delta)) }, reason: 'Ódio do adversário alterado pela mesa tática.' }], 'alterar Ódio no combate');
    }
    setSelectedActor(null);
  };


  return (
    <div className="tor-combat-layout mx-auto grid w-full max-w-7xl gap-4 pb-16 text-slate-100">
      <section className="medieval-panel min-w-0 p-3 sm:p-5">
        <header className="mb-3 flex flex-wrap items-center justify-between gap-2 border-b border-blue-950 pb-3">
          <div><h1 className="flex items-center gap-2 font-serif text-xl font-black text-blue-100"><Swords className="h-5 w-5 text-red-400" /> Campo tático</h1><p className="text-xs text-slate-500">{isActive ? <>Rodada {Math.max(1, combat.round)} · turno de {String(currentActor?.quickSheet?.name || currentActor?.actorId || 'ninguém')}</> : <>Preparação · {characters.length} Herói(s) e {npcs.length} NPC(s) da cena já posicionados</>}</p></div>
          <div className="flex flex-wrap gap-2">
            {!isActive && <button onClick={() => void start()} disabled={!characters.length} className="medieval-button flex items-center gap-2"><Play className="h-4 w-4" /> Iniciar encontro</button>}
            {isActive && <button onClick={() => setShowAdd(value => !value)} className="medieval-button-secondary flex items-center gap-2"><UserPlus className="h-4 w-4" /> Combatente</button>}
            <button onClick={() => backgroundInputRef.current?.click()} className="medieval-button-secondary flex items-center gap-2"><ImagePlus className="h-4 w-4" /> Cenário</button>
            <input ref={backgroundInputRef} type="file" accept="image/*" className="hidden" onChange={event => { void importBackground(event.target.files?.[0]); event.currentTarget.value = ''; }} />
          </div>
        </header>

        {isActive && showAdd && <div className="mb-3 grid gap-2 rounded border border-blue-900 bg-black/30 p-3 sm:grid-cols-4">
          <input value={actorName} onChange={event => setActorName(event.target.value)} placeholder="Nome do NPC ou adversário" className="rounded border border-blue-900 bg-zinc-950 px-3 py-2 text-sm" />
          <select value={actorType} onChange={event => setActorType(event.target.value as 'npc' | 'adversary')} className="rounded border border-blue-900 bg-zinc-950 px-3 py-2 text-sm"><option value="adversary">Adversário</option><option value="npc">NPC</option></select>
          <div className="grid grid-cols-2 gap-2"><input type="number" min={1} value={actorEndurance} onChange={event => setActorEndurance(Number(event.target.value) || 1)} title="Resistência" className="rounded border border-blue-900 bg-zinc-950 px-2 py-2" /><input type="number" min={0} value={actorHate} onChange={event => setActorHate(Number(event.target.value) || 0)} title="Ódio" className="rounded border border-blue-900 bg-zinc-950 px-2 py-2" /></div>
          <button onClick={() => void addActor()} className="medieval-button">Adicionar</button>
        </div>}

        <div
          ref={boardRef}
          className="tor-tactical-board relative w-full overflow-hidden rounded-xl border-2 border-blue-900 bg-[#0a1012] touch-none"
          style={{ minHeight: '520px', height: 'clamp(520px, 64vh, 760px)', width: '100%', display: 'block', touchAction: 'none', backgroundImage: combat.backgroundImage ? `linear-gradient(rgba(2,7,19,.38),rgba(2,7,19,.6)),url(${combat.backgroundImage})` : 'linear-gradient(rgba(255,255,255,.075) 1px,transparent 1px),linear-gradient(90deg,rgba(255,255,255,.075) 1px,transparent 1px)', backgroundSize: combat.backgroundImage ? 'cover' : '8.333% 12.5%', backgroundPosition: 'center' }}
          onPointerMove={event => { if (draggingId) { const token = event.currentTarget.querySelector<HTMLElement>(`[data-actor="${CSS.escape(draggingId)}"]`); const rect = event.currentTarget.getBoundingClientRect(); if (token) { token.style.left = `${Math.max(4, Math.min(96, (event.clientX - rect.left) / rect.width * 100))}%`; token.style.top = `${Math.max(6, Math.min(94, (event.clientY - rect.top) / rect.height * 100))}%`; } } }}
          onPointerUp={event => { if (draggingId) { void moveActor(draggingId, event.clientX, event.clientY); setDraggingId(null); } }}
          onPointerCancel={() => setDraggingId(null)}
        >
          <div className="tor-tactical-zone tor-tactical-zone-enemy" aria-hidden="true"><span>ADVERSÁRIOS</span></div>
          <div className="tor-tactical-contact-line" aria-hidden="true"><span>LINHA DE CONTATO</span></div>
          <div className="tor-tactical-zone tor-tactical-zone-company" aria-hidden="true"><span>COMPANHIA</span></div>
          {!tacticalActors.length && <div className="tor-tactical-empty"><Swords className="h-8 w-8" /><strong>Tabuleiro tático pronto</strong><span>Adicione Heróis à Companhia ou NPCs à cena para posicionar os tokens.</span></div>}
          {tacticalActors.map((actor, index) => {
            const token = actor.tokenId ? tokenUrls[actor.tokenId] : null;
            const name = String(actor.quickSheet?.name || characters.find(item => item.id === actor.actorId)?.name || npcs.find(item => item.id === actor.actorId)?.name || actor.actorId);
            const isCurrent = currentActor?.actorId === actor.actorId;
            const position = actor.position || { x: actor.actorType === 'hero' ? 0.2 + index * 0.14 : 0.75, y: actor.actorType === 'hero' ? 0.76 : 0.24 + index * 0.12 };
            return <div key={actor.actorId} data-actor={actor.actorId} onPointerDown={event => { if (!(event.target as HTMLElement).closest('button')) { event.preventDefault(); (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId); setDraggingId(actor.actorId); } }} className={`absolute -translate-x-1/2 -translate-y-1/2 select-none ${actor.defeated ? 'opacity-40 grayscale' : ''}`} style={{ left: `${position.x * 100}%`, top: `${position.y * 100}%`, touchAction: 'none', cursor: draggingId === actor.actorId ? 'grabbing' : 'grab' }}>
              <div className={`tor-tactical-token-shell relative overflow-visible rounded-full border-[3px] bg-zinc-950 shadow-xl ${isCurrent ? 'border-cyan-300 shadow-cyan-500/40' : actor.actorType === 'hero' ? 'border-blue-500' : actor.actorType === 'npc' ? 'border-violet-500' : 'border-red-600'}`} style={{ width: 'clamp(50px, 14vw, 64px)', height: 'clamp(50px, 14vw, 64px)' }}>
                {token ? <img src={token} alt={name} draggable={false} className="h-full w-full rounded-full object-cover pointer-events-none" /> : <span className="flex h-full w-full items-center justify-center rounded-full bg-slate-900 font-serif text-3xl font-black text-slate-200">{name.slice(0, 1).toUpperCase()}</span>}
                <span className="absolute -left-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full border border-blue-400 bg-black text-[10px] font-black text-blue-100">{actor.initiative ?? index + 1}</span>
                <button onClick={() => setSelectedActor(actor)} className="absolute -right-2 -top-2 flex h-6 w-6 items-center justify-center rounded-full border border-amber-500 bg-black text-amber-200" aria-label={`Resumo de ${name}`}><Info className="h-3 w-3" /></button>
                <div className="absolute -bottom-2 left-1/2 flex -translate-x-1/2 gap-1 whitespace-nowrap rounded bg-black/90 px-2 py-1 text-[9px] text-slate-200"><span>{actor.endurance}/{actor.enduranceMax}</span>{actor.wounded && <span className="text-red-300">Ferido</span>}</div>
              </div>
              <div className="mt-2 max-w-24 truncate rounded bg-black/80 px-1.5 py-0.5 text-center text-[9px] font-bold">{name}</div>
            </div>;
          })}
          <div className="absolute bottom-2 left-2 flex items-center gap-2 rounded bg-black/65 px-2 py-1 text-[10px] text-slate-400"><Move className="h-3 w-3" /> {isActive ? 'Arraste os tokens' : 'Arraste para organizar a formação; iniciar o encontro habilita as regras de combate'}</div>
        </div>

        <div className="mt-3 rounded border border-blue-900/60 bg-black/25 p-3">
          <h2 className="mb-2 font-serif text-xs font-bold uppercase tracking-wider text-blue-300">Narrativa da batalha</h2>
          <div className="mb-3 max-h-44 space-y-2 overflow-y-auto">
            {(combat.narrativeBlocks || []).map(block => <div key={block.id} className="rounded border-l-2 border-amber-600 bg-zinc-950/70 p-2 text-xs leading-relaxed text-slate-300"><strong className="text-amber-300">Rodada {block.round}</strong> · {block.text}</div>)}
            {!(combat.narrativeBlocks || []).length && <p className="text-xs italic text-slate-600">Nenhum bloco narrativo registrado.</p>}
          </div>
          <textarea value={narrative} onChange={event => setNarrative(event.target.value)} rows={3} disabled={!isActive} placeholder={isActive ? 'Descreva a ação, fala ou mudança entre os turnos…' : 'Inicie o encontro para registrar a narrativa da batalha.'} className="w-full rounded border border-blue-900 bg-zinc-950 p-3 text-sm disabled:opacity-50" />
          <div className="mt-2 grid gap-2 sm:grid-cols-2"><button disabled={!isActive} onClick={() => void saveNarrative(false)} className="medieval-button-secondary flex items-center justify-center gap-2 disabled:opacity-50"><Save className="h-4 w-4" /> Registrar</button><button disabled={!isActive} onClick={() => void saveNarrative(true)} className="medieval-button flex items-center justify-center gap-2 disabled:opacity-50"><Crosshair className="h-4 w-4" /> Resolver com o Mestre</button></div>
        </div>
      </section>

      <aside className="space-y-4">
        <section className="medieval-panel p-4">
          <h2 className="flex items-center gap-2 font-serif text-sm font-bold text-blue-200"><SkipForward className="h-4 w-4" /> Ordem e rodada</h2>
          <div className="mt-3 space-y-2">{activeActors.map((actor, index) => <div key={actor.actorId} className={`rounded border p-2 text-xs ${index === (combat.turnIndex || 0) ? 'border-cyan-500 bg-cyan-950/25' : 'border-slate-800 bg-black/25'}`}><div className="flex justify-between"><strong>{String(actor.quickSheet?.name || actor.actorId)}</strong><span>{actor.initiative ?? '—'}</span></div>{actor.actorType === 'hero' && <select value={actor.stance || 'open'} onChange={event => void onApplyCommands([{ type: 'set_combat_stance', targetCharacterId: actor.actorId, stance: event.target.value as CombatStance, reason: 'Postura escolhida na mesa tática.' }], 'alterar postura')} className="mt-2 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-1"><option value="forward">Avançada</option><option value="open">Aberta</option><option value="defensive">Defensiva</option><option value="rearward">Retaguarda</option></select>}</div>)}</div>
          {isActive ? <><button onClick={() => void onApplyCommands([{ type: 'next_combat_round', reason: 'Avançar a ordem de ação.' }], 'próximo turno')} className="medieval-button mt-3 flex w-full items-center justify-center gap-2"><SkipForward className="h-4 w-4" /> Próximo turno</button><button onClick={() => void onApplyCommands([{ type: 'end_combat', reason: 'Confronto encerrado na mesa tática.' }], 'encerrar combate')} className="mt-2 w-full rounded border border-red-800 bg-red-950/30 px-3 py-2 text-xs font-bold text-red-200">Encerrar combate</button></> : <button onClick={() => void start()} disabled={!characters.length} className="medieval-button mt-3 flex w-full items-center justify-center gap-2"><Play className="h-4 w-4" /> Confirmar início do confronto</button>}
        </section>

        <section className="medieval-panel p-4">
          <h2 className="flex items-center gap-2 font-serif text-sm font-bold text-blue-200"><Dices className="h-4 w-4" /> Rolagem rápida</h2>
          <div className="mt-3 grid grid-cols-3 gap-2"><label className="text-[10px] text-slate-400">Dados<input type="number" min={0} max={12} value={rollDice} onChange={event => setRollDice(Number(event.target.value) || 0)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-sm" /></label><label className="text-[10px] text-slate-400">ND<input type="number" min={1} max={30} value={rollTarget} onChange={event => setRollTarget(Number(event.target.value) || 14)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-sm" /></label><label className="text-[10px] text-slate-400">Bônus<input type="number" value={rollModifier} onChange={event => setRollModifier(Number(event.target.value) || 0)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-sm" /></label></div>
          <button disabled={!isActive} onClick={() => void performRoll()} className="medieval-button mt-3 flex w-full items-center justify-center gap-2 disabled:opacity-50"><Dices className="h-4 w-4" /> Rolar na batalha</button>
          {status && <p className="mt-2 rounded border border-cyan-900 bg-cyan-950/20 p-2 text-xs text-cyan-200">{status}</p>}
          <button onClick={onOpenDice} className="mt-2 w-full text-xs text-blue-400 underline">Abrir mesa completa de dados</button>
          <div className="mt-3 max-h-44 space-y-1 overflow-y-auto">{(combat.rolls || []).slice().reverse().slice(0, 8).map(roll => <div key={roll.id} className="rounded border border-slate-800 bg-black/25 p-2 text-[10px]"><strong>{roll.label}</strong><div className={roll.success ? 'text-emerald-300' : 'text-red-300'}>{roll.success ? 'Sucesso' : 'Falha'} · total {roll.total} · {roll.tengwar} Tengwar</div></div>)}</div>
        </section>
      </aside>

      {selectedActor && <div className="fixed inset-0 z-[350] flex items-center justify-center bg-black/80 p-3" onMouseDown={event => { if (event.target === event.currentTarget) setSelectedActor(null); }}>
        <section className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border-2 border-blue-700 bg-[#020713] p-4 sm:p-5">
          <header className="flex items-start justify-between border-b border-blue-950 pb-3">
            <div><h2 className="font-serif text-lg font-black text-blue-100">{String(selectedActor.quickSheet?.name || selectedActor.actorId)}</h2><p className="text-xs text-slate-500">{selectedActor.actorType === 'hero' ? 'Herói' : selectedActor.actorType === 'npc' ? 'NPC' : 'Adversário'} · iniciativa {selectedActor.initiative ?? '—'}</p></div>
            <button onClick={() => setSelectedActor(null)} className="rounded border border-blue-900 p-2"><X className="h-4 w-4" /></button>
          </header>
          <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
            <div className="rounded border border-slate-800 p-2"><strong>Resistência</strong><span className="block text-blue-200">{selectedActor.endurance}/{selectedActor.enduranceMax}</span></div>
            <div className="rounded border border-slate-800 p-2"><strong>Postura</strong><span className="block text-blue-200">{selectedActor.stance ? STANCE_LABEL[selectedActor.stance] : '—'}</span></div>
            <div className="rounded border border-slate-800 p-2"><strong>Ferido</strong><span className="block text-blue-200">{selectedActor.wounded ? 'Sim' : 'Não'}</span></div>
            <div className="rounded border border-slate-800 p-2"><strong>{selectedActor.actorType === 'adversary' ? 'Ódio' : 'Esperança'}</strong><span className="block text-blue-200">{selectedActor.actorType === 'adversary' ? selectedActor.hate ?? 0 : selectedActor.hope ?? '—'}</span></div>
          </div>

          {selectedCharacter && <div className="mt-4 space-y-3 text-xs">
            <section className="rounded border border-blue-950 bg-black/25 p-3"><h3 className="mb-2 font-serif font-bold text-blue-200">Resumo do Herói</h3><div className="grid grid-cols-2 gap-x-3 gap-y-1"><span>Cultura</span><b>{selectedCharacter.culture || '—'}</b><span>Vocação</span><b>{selectedCharacter.vocation || '—'}</b><span>Força</span><b>{selectedCharacter.sheet.attributes.strength}</b><span>Coração</span><b>{selectedCharacter.sheet.attributes.heart}</b><span>Astúcia</span><b>{selectedCharacter.sheet.attributes.wits}</b><span>Bloqueio</span><b>{selectedCharacter.sheet.combat.parry}</b><span>Fadiga</span><b>{selectedCharacter.sheet.attributes.fatigue}</b><span>Sombra</span><b>{selectedCharacter.sheet.attributes.shadow}</b></div></section>
            <section className="rounded border border-slate-800 bg-black/25 p-3"><h3 className="mb-2 font-bold text-amber-200">Armas</h3>{selectedCharacter.sheet.combat.weapons.length ? selectedCharacter.sheet.combat.weapons.map((weapon, index) => <div key={`${weapon.name}-${index}`} className="flex justify-between border-b border-slate-900 py-1 last:border-0"><span>{weapon.name}{weapon.active ? ' · ativa' : ''}</span><span>Dano {weapon.damage} · Ferimento {weapon.injury}</span></div>) : <span className="text-slate-500">Nenhuma arma registrada.</span>}</section>
            <section className="rounded border border-slate-800 bg-black/25 p-3"><h3 className="mb-2 font-bold text-cyan-200">Perícias treinadas</h3><div className="flex flex-wrap gap-1.5">{selectedCharacter.sheet.skills.filter(skill => skill.rank > 0).sort((a,b) => b.rank-a.rank || a.name.localeCompare(b.name)).map(skill => <span key={skill.name} className="rounded border border-slate-700 bg-slate-950 px-2 py-1">{skill.name} {skill.rank}{skill.favorite ? ' ★' : ''}</span>)}</div></section>
          </div>}

          {selectedNpc && <div className="mt-4 space-y-3 text-xs">
            <section className="rounded border border-violet-950 bg-black/25 p-3"><h3 className="mb-2 font-serif font-bold text-violet-200">Resumo do NPC</h3><div className="grid grid-cols-2 gap-x-3 gap-y-1"><span>Povo/Cultura</span><b>{selectedNpc.culture || selectedNpc.peoples || '—'}</b><span>Papel</span><b>{selectedNpc.role || '—'}</b><span>Facção</span><b>{selectedNpc.faction || '—'}</b><span>Atitude</span><b>{selectedNpc.attitude || '—'}</b><span>Estado emocional</span><b>{selectedNpc.emotionalState || '—'}</b><span>Local</span><b>{selectedNpc.location || '—'}</b></div></section>
            {selectedNpc.personality && <section className="rounded border border-slate-800 bg-black/25 p-3"><h3 className="mb-1 font-bold text-amber-200">Personalidade</h3><p className="leading-relaxed text-slate-300">{selectedNpc.personality}</p></section>}
            {selectedNpc.objectives && <section className="rounded border border-slate-800 bg-black/25 p-3"><h3 className="mb-1 font-bold text-cyan-200">Objetivo conhecido</h3><p className="leading-relaxed text-slate-300">{selectedNpc.objectives}</p></section>}
            {selectedNpc.nextAction && <section className="rounded border border-slate-800 bg-black/25 p-3"><h3 className="mb-1 font-bold text-blue-200">Próxima ação</h3><p className="leading-relaxed text-slate-300">{selectedNpc.nextAction}</p></section>}
          </div>}

          {!selectedCharacter && !selectedNpc && <div className="mt-4 rounded border border-slate-800 bg-black/25 p-3 text-xs"><h3 className="mb-2 font-bold text-red-200">Dados do adversário</h3><p className="text-slate-300">{String(selectedActor.quickSheet?.note || selectedActor.quickSheet?.kind || 'Use as estatísticas validadas pelo Mestre e pelos manuais de O Um Anel para este adversário.')}</p>{selectedActor.stateNotes?.length ? <p className="mt-2 text-slate-400">Estado: {selectedActor.stateNotes.join(' · ')}</p> : null}</div>}

          <div className="mt-4 grid grid-cols-2 gap-2">
            <button onClick={() => void updateSelectedEndurance(-1)} className="rounded border border-red-900 bg-red-950/25 px-3 py-2 text-xs text-red-200">−1 Resistência</button>
            <button onClick={() => void updateSelectedEndurance(1)} className="rounded border border-emerald-900 bg-emerald-950/20 px-3 py-2 text-xs text-emerald-200">+1 Resistência</button>
            <button onClick={() => void toggleSelectedWounded()} className="rounded border border-amber-900 bg-amber-950/20 px-3 py-2 text-xs text-amber-200">{selectedActor.wounded ? 'Remover Ferimento' : 'Marcar Ferido'}</button>
            <div className="grid grid-cols-2 gap-1"><button onClick={() => void updateSelectedResource(-1)} className="rounded border border-slate-800 px-2 py-2 text-xs">− {selectedActor.actorType === 'adversary' ? 'Ódio' : 'Esperança'}</button><button onClick={() => void updateSelectedResource(1)} className="rounded border border-slate-800 px-2 py-2 text-xs">+ {selectedActor.actorType === 'adversary' ? 'Ódio' : 'Esperança'}</button></div>
          </div>
          <p className="mt-3 text-[10px] text-slate-500">Alterações em Heróis atualizam a ficha canônica; NPCs e adversários atualizam o estado persistente do encontro.</p>
        </section>
      </div>}
    </div>
  );
};
