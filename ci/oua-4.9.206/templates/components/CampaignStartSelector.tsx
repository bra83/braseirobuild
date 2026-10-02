import React from 'react';
import { CAMPAIGN_START_OPTIONS } from '../lib/adventureModules';
import type { CampaignStartMode } from '../lib/adventureTypes';

interface Props {
  mode: CampaignStartMode;
  onChange: (mode: CampaignStartMode) => void | Promise<void>;
  canUseStrider: boolean;
  summary?: string;
}

export const CampaignStartSelector: React.FC<Props> = ({ mode, onChange, canUseStrider, summary }) => (
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
    {summary && <div className="mt-3 rounded border border-blue-900/50 bg-blue-950/15 px-3 py-2 text-[11px] leading-relaxed text-blue-200">{summary}</div>}
  </section>
);
