import React from 'react';
import { CAMPAIGN_START_OPTIONS, STRIDER_PATRONS } from '../lib/adventureModules';
import { STRIDER_MISSIONS } from '../lib/striderPatronMissions';
import type { CampaignStartMode } from '../lib/adventureTypes';

interface Props {
  mode: CampaignStartMode;
  onChange: (mode: CampaignStartMode) => void | Promise<void>;
  canUseStrider: boolean;
  patronId?: string;
  missionIndex?: number;
  onPatronChange: (patronId: string) => void | Promise<void>;
  onMissionChange: (mission: number | 'random') => void | Promise<void>;
  summary?: string;
}

export const CampaignStartSelector: React.FC<Props> = ({
  mode,
  onChange,
  canUseStrider,
  patronId,
  missionIndex,
  onPatronChange,
  onMissionChange,
  summary,
}) => {
  const patrons = Object.values(STRIDER_PATRONS);
  const missions = patronId ? (STRIDER_MISSIONS[patronId] || []) : [];

  return (
    <section className="mt-4 rounded border border-cyan-950/70 bg-black/25 p-3 text-left">
      <div className="mb-2">
        <span className="block text-[10px] font-black uppercase tracking-[0.18em] text-cyan-500">Como esta crônica começa?</span>
        <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
          Aventuras oficiais e Modo Peregrino posicionam a Companhia automaticamente. Sandbox é o único modo em que você escolhe o hex. Taverna sorteia um local válido do Atlas.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        {CAMPAIGN_START_OPTIONS.map(option => {
          const disabled = option.mode === 'strider' && !canUseStrider;
          const selected = option.mode === mode;
          return (
            <button
              key={option.mode}
              type="button"
              disabled={disabled}
              onClick={() => void onChange(option.mode)}
              className={`rounded border p-3 text-left transition ${selected ? 'border-cyan-500 bg-cyan-950/35 text-cyan-100' : 'border-blue-950 bg-zinc-950/60 text-slate-300'} disabled:cursor-not-allowed disabled:opacity-35`}
            >
              <span className="block text-xs font-bold">
                {option.label}{option.recommended ? ' · recomendado' : ''}
              </span>
              <span className="mt-1 block text-[11px] leading-relaxed text-slate-400">{option.description}</span>
              {disabled && <span className="mt-2 block text-[10px] text-amber-300">Disponível quando houver um Herói em Modo Peregrino.</span>}
            </button>
          );
        })}
      </div>

      {mode === 'strider' && canUseStrider && (
        <div className="mt-4 rounded border border-amber-900/45 bg-amber-950/10 p-3">
          <span className="block text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">1. Escolha o Patrono</span>
          <p className="mt-1 text-[11px] leading-relaxed text-slate-400">
            O Patrono define a tabela de seis sementes de missão do Modo Peregrino. O Gemini não escolhe nem substitui esta decisão.
          </p>

          <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-2">
            {patrons.map(patron => (
              <button
                key={patron.id}
                type="button"
                onClick={() => void onPatronChange(patron.id)}
                className={`rounded border px-3 py-2 text-left text-[11px] transition ${patronId === patron.id ? 'border-amber-400 bg-amber-950/35 text-amber-100' : 'border-zinc-800 bg-black/25 text-slate-300'}`}
              >
                {patron.label}
              </button>
            ))}
          </div>

          {patronId && (
            <div className="mt-4">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <span className="text-[10px] font-black uppercase tracking-[0.16em] text-amber-300">2. Escolha a missão</span>
                <button
                  type="button"
                  onClick={() => void onMissionChange('random')}
                  className="rounded border border-amber-500/70 bg-amber-950/30 px-3 py-1.5 text-[10px] font-bold uppercase tracking-wide text-amber-100"
                >
                  Sortear missão
                </button>
              </div>

              <div className="mt-2 grid grid-cols-1 gap-2">
                {missions.map((mission, index) => (
                  <button
                    key={index}
                    type="button"
                    onClick={() => void onMissionChange(index)}
                    className={`rounded border p-2 text-left text-[11px] leading-relaxed transition ${missionIndex === index ? 'border-amber-400 bg-amber-950/35 text-amber-50' : 'border-zinc-800 bg-black/20 text-slate-300'}`}
                  >
                    <span className="mr-2 font-black text-amber-300">{index + 1}.</span>{mission}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {summary && <div className="mt-3 rounded border border-blue-900/50 bg-blue-950/15 px-3 py-2 text-[11px] leading-relaxed text-blue-200">{summary}</div>}
    </section>
  );
};
