import React, { useEffect, useState } from 'react';
import { Save, UsersRound } from 'lucide-react';
import type { Character } from '../lib/db';
import type { CompanyState, JourneyRole } from '../lib/master/types';
import { SUPPLEMENT_PATRONS } from '../lib/supplementRules';

interface CompanyManagerProps {
  company: CompanyState;
  characters: Character[];
  onSave: (company: CompanyState) => void | Promise<void>;
}

const ROLE_LABELS: Array<[JourneyRole, string]> = [
  ['guide', 'Guia'],
  ['scout', 'Batedor'],
  ['hunter', 'Caçador'],
  ['lookout', 'Vigia'],
];

export const CompanyManager: React.FC<CompanyManagerProps> = ({ company, characters, onSave }) => {
  const [draft, setDraft] = useState<CompanyState>(() => structuredClone(company));
  const [saved, setSaved] = useState(false);
  useEffect(() => setDraft(structuredClone(company)), [JSON.stringify(company)]);

  const updateRole = (role: JourneyRole, characterId: string) => {
    setDraft(current => ({ ...current, journeyRoles: { ...current.journeyRoles, [role]: characterId || undefined } }));
  };
  const updateFocus = (heroId: string, focusId: string) => {
    setDraft(current => ({ ...current, fellowshipFocus: { ...current.fellowshipFocus, [heroId]: focusId || '' } }));
  };
  const save = async () => {
    await onSave({
      ...draft,
      spotlightByHero: draft.spotlightByHero || {},
      fellowshipCurrent: Math.max(0, Math.min(draft.fellowshipMax, Number(draft.fellowshipCurrent) || 0)),
      fellowshipMax: Math.max(0, Number(draft.fellowshipMax) || 0),
      eyeAwareness: Math.max(0, Number(draft.eyeAwareness) || 0),
    });
    setSaved(true);
    window.setTimeout(() => setSaved(false), 1600);
  };

  return (
    <details className="mt-3 rounded border border-blue-900/70 bg-black/25 p-3">
      <summary className="cursor-pointer list-none text-xs font-bold uppercase tracking-wider text-blue-300">
        <span className="inline-flex items-center gap-2"><UsersRound className="h-4 w-4" /> Configurar Companhia e papéis</span>
      </summary>
      <div className="mt-3 space-y-3 text-xs">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
          <label className="text-slate-400">Sociedade atual<input type="number" min={0} value={draft.fellowshipCurrent} onChange={event => setDraft({ ...draft, fellowshipCurrent: Number(event.target.value) || 0 })} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100" /></label>
          <label className="text-slate-400">Sociedade máxima<input type="number" min={0} value={draft.fellowshipMax} onChange={event => setDraft({ ...draft, fellowshipMax: Number(event.target.value) || 0 })} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100" /></label>
          <label className="text-slate-400">Vigilância do Olho<input type="number" min={0} value={draft.eyeAwareness} onChange={event => setDraft({ ...draft, eyeAwareness: Number(event.target.value) || 0 })} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100" /></label>
          <label className="text-slate-400">Patrono<input list="tor2e-patrons" value={draft.patron} onChange={event => setDraft({ ...draft, patron: event.target.value })} className="mt-1 min-h-[48px] w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100" placeholder="Patrono da Companhia" /><datalist id="tor2e-patrons">{SUPPLEMENT_PATRONS.map(patron=><option key={patron.id} value={patron.name}>{patron.name} — Valfenda</option>)}</datalist></label>
        </div>
        <div>
          <p className="mb-2 font-bold uppercase text-cyan-300">Papéis de Jornada</p>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {ROLE_LABELS.map(([role, label]) => <label key={role} className="text-slate-400">{label}<select value={draft.journeyRoles[role] || ''} onChange={event => updateRole(role, event.target.value)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100"><option value="">Não definido</option>{characters.map(character => <option key={character.id} value={character.id}>{character.sheet.identity.name || character.name}</option>)}</select></label>)}
          </div>
        </div>
        <div>
          <p className="mb-2 font-bold uppercase text-cyan-300">Foco da Sociedade</p>
          <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
            {characters.map(character => <label key={character.id} className="text-slate-400">{character.sheet.identity.name || character.name}<select value={draft.fellowshipFocus[character.id] || ''} onChange={event => updateFocus(character.id, event.target.value)} className="mt-1 w-full rounded border border-blue-900 bg-zinc-950 px-2 py-2 text-blue-100"><option value="">Sem foco definido</option>{characters.filter(other => other.id !== character.id).map(other => <option key={other.id} value={other.id}>{other.sheet.identity.name || other.name}</option>)}</select></label>)}
          </div>
        </div>
        <button type="button" onClick={() => void save()} className="flex w-full items-center justify-center gap-2 rounded border border-emerald-700 bg-emerald-950/30 px-3 py-2 font-bold text-emerald-200"><Save className="h-4 w-4" /> {saved ? 'Companhia salva' : 'Salvar Companhia'}</button>
      </div>
    </details>
  );
};
