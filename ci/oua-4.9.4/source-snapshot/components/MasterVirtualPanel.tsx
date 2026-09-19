import React from 'react';
import { BrainCircuit, MapPinned, Route, Swords, Handshake, House, UsersRound, BookOpenCheck, X, Target, MessageSquare, Activity } from 'lucide-react';
import type { MasterState, RuleAnswer, CompanyState } from '../lib/master/types';
import type { Character, Npc } from '../lib/db';
import { CompanyManager } from './CompanyManager';

interface MasterVirtualPanelProps {
  masterState: MasterState;
  partySize: number;
  partyCharacters: Character[];
  presentNpcs?: Npc[];
  activeCharacterId?: string | null;
  onActiveCharacterChange?: (characterId: string) => void | Promise<void>;
  onCompanySave: (company: CompanyState) => void | Promise<void>;
  ruleAnswer: RuleAnswer | null;
  onCloseRuleAnswer: () => void;
}

const MODE_LABEL: Record<MasterState['activeMode'], string> = {
  adventure: 'Aventura', journey: 'Jornada', council: 'Conselho', combat: 'Combate', fellowship: 'Sociedade',
};

const SCENE_LABEL: Record<MasterState['scene']['kind'], string> = {
  exploration: 'Exploração', dialogue: 'Diálogo', journey: 'Jornada', council: 'Conselho', combat: 'Combate', rest: 'Descanso', fellowship: 'Sociedade',
};

export const MasterVirtualPanel: React.FC<MasterVirtualPanelProps> = ({
  masterState, partySize, partyCharacters, presentNpcs = [], activeCharacterId, onActiveCharacterChange,
  onCompanySave, ruleAnswer, onCloseRuleAnswer,
}) => {
  const seasonLabel = { spring: 'Primavera', summer: 'Verão', autumn: 'Outono', winter: 'Inverno' }[masterState.worldClock.season];
  const publicClocks = masterState.threatClocks.filter(clock => !clock.hidden);
  const scene = masterState.scene;
  const nextHero = partyCharacters.find(hero => hero.id === masterState.sessionDirector.nextSuggestedHeroId);
  return (
    <section className="rounded-md border-2 border-cyan-900/70 bg-slate-950/85 p-3 shadow-lg">
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-cyan-950/70 pb-2">
        <div className="flex items-center gap-2">
          <BrainCircuit className="h-4 w-4 text-cyan-400" />
          <span className="font-serif text-xs font-black uppercase tracking-wider text-cyan-200">Mestre Virtual 2.6 · Mesa Viva</span>
        </div>
        <span className="rounded border border-cyan-800 bg-cyan-950/30 px-2 py-1 text-[10px] font-bold uppercase text-cyan-200">{MODE_LABEL[masterState.activeMode]}</span>
      </div>

      <div className="mt-3 grid grid-cols-2 gap-2 text-[11px] text-slate-300 sm:grid-cols-4">
        <div className="rounded border border-slate-800 bg-black/30 p-2"><UsersRound className="mb-1 h-4 w-4 text-blue-400" /><strong>{partySize}</strong> heróis</div>
        <div className="rounded border border-slate-800 bg-black/30 p-2"><MapPinned className="mb-1 h-4 w-4 text-blue-400" />{masterState.position.currentHexId || 'sem hex'}</div>
        <div className="rounded border border-slate-800 bg-black/30 p-2"><Route className="mb-1 h-4 w-4 text-blue-400" />{masterState.journey.status === 'idle' ? 'sem jornada' : masterState.journey.status}</div>
        <div className="rounded border border-slate-800 bg-black/30 p-2"><BookOpenCheck className="mb-1 h-4 w-4 text-blue-400" />{masterState.quests.filter(q => q.status === 'active').length} missões</div>
      </div>

      <article className="mt-3 rounded border border-blue-900/60 bg-blue-950/15 p-3">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-xs font-bold text-blue-100"><Target className="h-4 w-4 text-amber-400" /> {scene.title || SCENE_LABEL[scene.kind]}</div>
          <span className="rounded border border-slate-700 px-2 py-1 text-[9px] font-bold uppercase text-slate-300">{SCENE_LABEL[scene.kind]} · {scene.status}</span>
        </div>
        {scene.objective && <p className="mt-2 text-xs text-slate-300"><strong>Objetivo:</strong> {scene.objective}</p>}
        <div className="mt-2 flex items-center gap-2 text-[10px] text-slate-400"><Activity className="h-3.5 w-3.5" /> Tensão {scene.tension}/6 · {scene.participantHeroIds.length} heróis · {scene.participantNpcIds.length} NPCs</div>
        {scene.endCondition && <p className="mt-1 text-[10px] text-slate-500">Encerra quando: {scene.endCondition}</p>}
      </article>

      {partyCharacters.length > 0 && (
        <div className="mt-3 rounded border border-cyan-950/80 bg-black/25 p-3">
          <div className="mb-2 flex items-center justify-between gap-2">
            <span className="text-[10px] font-black uppercase tracking-wider text-cyan-300">Herói da vez</span>
            {nextHero && nextHero.id !== activeCharacterId && <span className="text-[9px] text-amber-300">Próximo foco sugerido: {nextHero.sheet.identity.name || nextHero.name}</span>}
          </div>
          <div className="flex gap-2 overflow-x-auto pb-1">
            {partyCharacters.map(hero => {
              const selected = hero.id === activeCharacterId;
              const spotlight = hero.id === scene.spotlightCharacterId;
              const count = masterState.sessionDirector.sceneCountByHero[hero.id] || 0;
              return (
                <button
                  key={hero.id}
                  type="button"
                  onClick={() => void onActiveCharacterChange?.(hero.id)}
                  className={`min-w-32 rounded border px-3 py-2 text-left text-[10px] ${selected ? 'border-cyan-400 bg-cyan-950/45 text-cyan-50' : 'border-slate-800 bg-slate-950/70 text-slate-300'}`}
                >
                  <strong className="block truncate">{hero.sheet.identity.name || hero.name}</strong>
                  <span className="mt-1 block text-[9px] opacity-70">{spotlight ? 'em foco · ' : ''}{count} ações em destaque</span>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {presentNpcs.length > 0 && (
        <div className="mt-2 flex flex-wrap items-center gap-1.5 text-[10px] text-slate-400">
          <MessageSquare className="h-3.5 w-3.5 text-violet-400" />
          {presentNpcs.map(npc => <span key={npc.id} className="rounded border border-violet-950 bg-violet-950/20 px-2 py-1">{npc.name}{npc.tokenId ? ' · retrato' : ''}</span>)}
        </div>
      )}

      <p className="mt-2 text-[10px] text-slate-400">Ano {masterState.worldClock.year} da Terceira Era · {seasonLabel} · dia {masterState.worldClock.day} · {String(masterState.worldClock.hour).padStart(2, '0')}:{String(masterState.worldClock.minute).padStart(2, '0')}</p>
      <CompanyManager company={masterState.company} characters={partyCharacters} onSave={onCompanySave} />
      {masterState.activeMode === 'combat' && <p className="mt-2 flex items-center gap-2 text-xs text-red-200"><Swords className="h-4 w-4" /> Rodada {masterState.combat.round}; {masterState.combat.actors.filter(a => !a.defeated).length} participantes ativos.</p>}
      {masterState.activeMode === 'council' && <p className="mt-2 flex items-center gap-2 text-xs text-amber-200"><Handshake className="h-4 w-4" /> Conselho: {masterState.council.successes} sucessos, {masterState.council.failures}/{masterState.council.tolerance} falhas.</p>}
      {masterState.activeMode === 'fellowship' && <p className="mt-2 flex items-center gap-2 text-xs text-emerald-200"><House className="h-4 w-4" /> Fase de Sociedade em andamento.</p>}
      {publicClocks.length > 0 && <div className="mt-2 space-y-1">{publicClocks.map(clock => <div key={clock.id} className="text-[10px] text-slate-400">{clock.name}: {clock.current}/{clock.max}</div>)}</div>}

      {ruleAnswer && (
        <article className="relative mt-3 rounded border border-amber-700/70 bg-amber-950/20 p-3 text-sm text-amber-50">
          <button type="button" onClick={onCloseRuleAnswer} className="absolute right-2 top-2 p-1 text-amber-300" aria-label="Fechar resposta de regras"><X className="h-4 w-4" /></button>
          <h3 className="pr-8 font-serif font-bold text-amber-200">Resposta de regras — não avança a história</h3>
          <p className="mt-2 whitespace-pre-wrap leading-relaxed">{ruleAnswer.answer}</p>
          {ruleAnswer.ruleApplied && <p className="mt-2 text-xs text-amber-200"><strong>Regra:</strong> {ruleAnswer.ruleApplied}</p>}
          {ruleAnswer.examples?.length > 0 && <ul className="mt-2 list-inside list-disc text-xs text-amber-100/90">{ruleAnswer.examples.map((item, index) => <li key={index}>{item}</li>)}</ul>}
          {ruleAnswer.uncertainty && <p className="mt-2 text-xs text-orange-300"><strong>Incerteza:</strong> {ruleAnswer.uncertainty}</p>}
        </article>
      )}
    </section>
  );
};
