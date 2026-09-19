import React from 'react';
import { Handshake, HeartHandshake, House, MapPinned, Sparkles, UsersRound } from 'lucide-react';
import type { Character } from '../lib/db';
import type { CompanyState, MasterState } from '../lib/master/types';
import { CompanyManager } from './CompanyManager';

interface Props { masterState: MasterState; characters: Character[]; onSave: (company: CompanyState) => void | Promise<void>; }
export const CompanyHubView: React.FC<Props> = ({ masterState, characters, onSave }) => {
  const company = masterState.company;
  return <div className="space-y-4">
    <section className="medieval-panel p-4"><div className="flex items-center gap-3"><UsersRound className="h-7 w-7 text-cyan-400" /><div><h1 className="medieval-title text-2xl">Companhia</h1><p className="text-xs text-slate-400">Patrono, Sociedade, focos, papéis de Jornada, santuários e compromissos da Companhia.</p></div></div>
      <div className="mt-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <article className="rounded border border-cyan-900 bg-black/25 p-3"><HeartHandshake className="h-5 w-5 text-cyan-400" /><b className="mt-2 block">Sociedade</b><span className="text-sm text-slate-300">{company.fellowshipCurrent}/{company.fellowshipMax}</span></article>
        <article className="rounded border border-amber-900 bg-black/25 p-3"><Handshake className="h-5 w-5 text-amber-400" /><b className="mt-2 block">Patrono</b><span className="text-sm text-slate-300">{company.patron || 'Não definido'}</span></article>
        <article className="rounded border border-emerald-900 bg-black/25 p-3"><House className="h-5 w-5 text-emerald-400" /><b className="mt-2 block">Santuários</b><span className="text-sm text-slate-300">{company.sanctuaryIds.length}</span></article>
        <article className="rounded border border-violet-900 bg-black/25 p-3"><Sparkles className="h-5 w-5 text-violet-400" /><b className="mt-2 block">Consciência do Olho</b><span className="text-sm text-slate-300">{company.eyeAwareness}</span></article>
      </div>
    </section>
    <CompanyManager company={company} characters={characters} onSave={onSave} />
    <section className="rounded-lg border border-blue-950 bg-zinc-950/80 p-4"><h2 className="flex items-center gap-2 text-sm font-bold uppercase text-blue-300"><MapPinned className="h-4 w-4" /> Situação da Companhia</h2><p className="mt-2 text-sm text-slate-300">{masterState.position.locationName} · {masterState.journey.status === 'idle' ? 'sem Jornada ativa' : `Jornada ${masterState.journey.status}`} · {masterState.quests.filter(q => q.status === 'active').length} missões ativas.</p></section>
  </div>;
};
