import React from 'react';
import {
  CAMPAIGN_START_OPTIONS,
  STRIDER_MISSIONS,
  STRIDER_PATRONS,
  type CampaignStartSelection,
  type StriderMissionChoice,
  type StriderPatronId,
} from '../lib/campaignStart';

interface Props {
  value: CampaignStartSelection;
  onChange: (value: CampaignStartSelection) => void;
  compact?: boolean;
}

export const CampaignStartSelector: React.FC<Props> = ({ value, onChange, compact = false }) => {
  const patronId = value.striderPatronId;
  const missions = patronId ? STRIDER_MISSIONS[patronId] : [];
  return <div className={compact ? 'space-y-2' : 'mt-4 space-y-3'}>
    <div className={compact ? 'grid grid-cols-1 gap-1.5' : 'grid grid-cols-1 gap-2 sm:grid-cols-2'}>
      {CAMPAIGN_START_OPTIONS.map(option => (
        <button
          key={option.id}
          type="button"
          onClick={() => onChange({ mode: option.id, ...(option.id === 'STRIDER' ? { striderMissionChoice: 'random' as const } : {}) })}
          className={`min-w-0 rounded border p-3 text-left ${value.mode === option.id ? 'border-amber-500 bg-amber-950/35 text-amber-50' : 'border-blue-950 bg-black/25 text-slate-300'}`}
        >
          <span className="block text-xs font-bold">{option.label}</span>
          {!compact && <span className="mt-1 block text-[11px] leading-relaxed text-slate-400">{option.description}</span>}
          <span className="mt-1 block text-[10px] text-amber-600/90">{option.sourceLabel}</span>
        </button>
      ))}
    </div>
    {value.mode === 'STRIDER' && <div className="rounded border border-amber-900/60 bg-black/30 p-3 text-left">
      <label className="block text-[11px] font-bold uppercase tracking-wide text-amber-300">Patrono</label>
      <select
        value={patronId || ''}
        onChange={event => onChange({ ...value, striderPatronId: (event.target.value || undefined) as StriderPatronId | undefined, striderMissionChoice: 'random' })}
        className="mt-1 w-full rounded border border-amber-900 bg-slate-950 px-3 py-2 text-sm text-amber-50"
      >
        <option value="">Escolha um Patrono…</option>
        {STRIDER_PATRONS.map(patron => <option key={patron.id} value={patron.id}>{patron.label}</option>)}
      </select>
      {patronId && <>
        <label className="mt-3 block text-[11px] font-bold uppercase tracking-wide text-amber-300">Missão</label>
        <select
          value={String(value.striderMissionChoice ?? 'random')}
          onChange={event => {
            const raw = event.target.value;
            const choice: StriderMissionChoice = raw === 'random' ? 'random' : Number(raw) as 1 | 2 | 3 | 4 | 5 | 6;
            onChange({ ...value, striderMissionChoice: choice });
          }}
          className="mt-1 w-full rounded border border-amber-900 bg-slate-950 px-3 py-2 text-sm text-amber-50"
        >
          <option value="random">Sortear com d6 ao começar</option>
          {missions.map((mission, index) => <option key={index + 1} value={index + 1}>{index + 1}. {mission}</option>)}
        </select>
        <p className="mt-2 text-[11px] leading-relaxed text-slate-400">A missão escolhida ou sorteada será persistida antes da primeira cena.</p>
      </>}
    </div>}
    {value.mode === 'SANDBOX' && <p className="rounded border border-cyan-900/60 bg-cyan-950/20 p-2 text-[11px] leading-relaxed text-cyan-100">Sandbox é a única abertura que exige posicionamento manual: escolha o hex no Atlas e toque em Posicionar.</p>}
    {value.mode === 'TAVERN' && <p className="rounded border border-amber-900/60 bg-amber-950/20 p-2 text-[11px] leading-relaxed text-amber-100">A estalagem é sorteada pelo host apenas entre locais estruturados que possuem uma âncora utilizável no Atlas.</p>}
  </div>;
};
