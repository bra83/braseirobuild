import React, { useMemo, useState } from 'react';
import { CalendarDays, Clock3, FastForward, Leaf, MapPin, ScrollText, ShieldAlert, Snowflake, Sprout, Sun, Users } from 'lucide-react';
import type { Character, Npc } from '../lib/db';
import type { MasterCommand, MasterState } from '../lib/master/types';
import { LivingWorldDashboard } from './LivingWorldDashboard';

interface WorldAliveViewProps {
  masterState: MasterState;
  characters: Character[];
  npcs: Npc[];
  onApplyCommands: (commands: MasterCommand[], reason: string) => Promise<void>;
}

const SEASON_LABEL: Record<MasterState['worldClock']['season'], string> = {
  spring: 'Primavera',
  summer: 'Verão',
  autumn: 'Outono',
  winter: 'Inverno',
};

const MODE_LABEL: Record<MasterState['activeMode'], string> = {
  adventure: 'Aventura',
  journey: 'Jornada',
  council: 'Conselho',
  combat: 'Combate',
  fellowship: 'Fase de Sociedade',
};

const COMPANY_STYLE_LABEL: Record<MasterState['livingWorld']['companyStyle'], string> = {
  fellowship: 'Companhia de aventureiros',
  'itinerant-troupe': 'Trupe itinerante',
  'settled-household': 'Lares estabelecidos',
  mixed: 'Caminhos mistos',
};

const LIFESTYLE_LABEL = {
  adventurer: 'Aventureiro',
  wanderer: 'Andarilho',
  troupe: 'Integrante da trupe',
  settled: 'Estabelecido',
} as const;

const PROJECT_KIND_LABEL = {
  hunting: 'Caçada',
  livelihood: 'Ofício e sustento',
  home: 'Lar e terras',
  family: 'Família',
  craft: 'Artesanato',
  travel: 'Viagem',
  study: 'Estudo',
  other: 'Outro',
} as const;

const PASSAGES = [
  { label: '+1 hora', minutes: 60 },
  { label: '+8 horas', minutes: 480 },
  { label: '+1 dia', minutes: 1440 },
  { label: '+1 semana', minutes: 10_080 },
  { label: '+1 estação', minutes: 131_040 },
  { label: '+1 ano', minutes: 525_600 },
];

export const WorldAliveView: React.FC<WorldAliveViewProps> = ({ masterState, characters, npcs, onApplyCommands }) => {
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const partyIds = new Set(masterState.scene.participantHeroIds);
  const party = characters.filter(character => partyIds.has(character.id));
  const actors = useMemo(() => masterState.npcActors
    .map(actor => ({ actor, npc: npcs.find(item => item.id === actor.npcId) }))
    .filter(item => item.npc)
    .sort((a, b) => Number(Boolean(b.npc?.presentInScene)) - Number(Boolean(a.npc?.presentInScene))),
  [masterState.npcActors, npcs]);

  const apply = async (commands: MasterCommand[], reason: string, success: string) => {
    setBusy(true); setError(''); setStatus('');
    try {
      await onApplyCommands(commands, reason);
      setStatus(success);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause));
    } finally {
      setBusy(false);
    }
  };

  const advanceTime = async (minutes: number, label: string) => {
    if (minutes >= 131_040 && !confirm(`Confirmar ${label.toLowerCase()} no Mundo Vivo? A idade muda somente quando um ano civil é completado; nenhum benefício mecânico será concedido.`)) return;
    await apply(
      [{ type: 'advance_time', minutes, reason: `Passagem de tempo confirmada pelo jogador: ${label}.`, source: 'Mundo Vivo — calendário civil' }],
      `passagem de tempo: ${label}`,
      `${label} aplicado ao calendário canônico.`,
    );
  };

  const configureCompanyLife = async (companyStyle: MasterState['livingWorld']['companyStyle']) => {
    const troupeName = companyStyle === 'itinerant-troupe'
      ? window.prompt('Nome da trupe itinerante:', masterState.livingWorld.troupeName) ?? masterState.livingWorld.troupeName
      : masterState.livingWorld.troupeName;
    const baseLocation = window.prompt('Base, lar ou ponto de retorno da Companhia:', masterState.livingWorld.baseLocation || masterState.position.locationName);
    if (baseLocation === null) return;
    await apply(
      [{ type: 'set_living_company', companyStyle, troupeName, baseLocation, reason: 'Configuração narrativa confirmada no Mundo Vivo.' }],
      'modo de vida da Companhia',
      `${COMPANY_STYLE_LABEL[companyStyle]} registrado sem alterar regras ou recursos.`,
    );
  };

  const editHeroLife = async (character: Character) => {
    const previous = masterState.livingWorld.heroLives[character.id];
    const rawLifestyle = window.prompt('Modo de vida: adventurer, wanderer, troupe ou settled', previous?.lifestyle || 'adventurer');
    if (rawLifestyle === null) return;
    const lifestyle = (['adventurer', 'wanderer', 'troupe', 'settled'] as const).find(value => value === rawLifestyle.trim().toLowerCase());
    if (!lifestyle) { setError('Modo de vida inválido. Use adventurer, wanderer, troupe ou settled.'); return; }
    const occupation = window.prompt('Ofício ou meio de sustento:', previous?.occupation || '') ?? previous?.occupation ?? '';
    const home = window.prompt('Lar, acampamento ou lugar de origem:', previous?.home || '') ?? previous?.home ?? '';
    const troupeRole = lifestyle === 'troupe'
      ? window.prompt('Papel na trupe:', previous?.troupeRole || '') ?? previous?.troupeRole ?? ''
      : previous?.troupeRole || '';
    const householdName = window.prompt('Nome do lar ou família (opcional):', previous?.householdName || '') ?? previous?.householdName ?? '';
    await apply(
      [{ type: 'upsert_hero_life', characterId: character.id, life: { lifestyle, occupation, home, troupeRole, householdName }, reason: 'Vida cotidiana confirmada pelo jogador.' }],
      `vida cotidiana de ${character.name}`,
      `Vida de ${character.name} atualizada.`,
    );
  };

  const addHouseholdMember = async (character: Character) => {
    const previous = masterState.livingWorld.heroLives[character.id];
    const name = window.prompt(`Nome do familiar ou integrante do lar de ${character.name}:`, '');
    if (!name?.trim()) return;
    const relation = window.prompt('Relação (companheiro, filha, irmão, aprendiz...):', '') ?? '';
    const rawAge = window.prompt('Idade atual:', '0');
    if (rawAge === null) return;
    const age = Math.max(0, Math.min(150, Math.trunc(Number(rawAge) || 0)));
    const member = { id: `family_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, name: name.trim(), relation, age, notes: '' };
    await apply(
      [{ type: 'upsert_hero_life', characterId: character.id, life: { householdMembers: [...(previous?.householdMembers || []), member] }, reason: 'Novo vínculo familiar confirmado pelo jogador.' }],
      `família de ${character.name}`,
      `${member.name} passou a integrar o lar. Sua idade avançará com os anos civis.`,
    );
  };

  const createLifeProject = async () => {
    const owner = (party.length ? party : characters)[0];
    if (!owner) { setError('Adicione um Herói à Companhia antes de criar um projeto.'); return; }
    const title = window.prompt('Nome do projeto de vida (caçada, lar, ofício, viagem...):', '');
    if (!title?.trim()) return;
    const rawKind = window.prompt('Tipo: hunting, livelihood, home, family, craft, travel, study ou other', 'other');
    if (rawKind === null) return;
    const kind = (Object.keys(PROJECT_KIND_LABEL) as Array<keyof typeof PROJECT_KIND_LABEL>).find(value => value === rawKind.trim().toLowerCase());
    if (!kind) { setError('Tipo de projeto inválido.'); return; }
    const location = window.prompt('Local do projeto:', masterState.position.locationName) ?? masterState.position.locationName;
    const target = Math.max(1, Math.min(12, Math.trunc(Number(window.prompt('Quantidade de etapas (1 a 12):', '4')) || 4)));
    await apply(
      [{ type: 'upsert_life_project', project: { id: `life_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`, kind, title: title.trim(), ownerCharacterIds: [owner.id], location, target }, reason: 'Projeto narrativo criado no Mundo Vivo.' }],
      `projeto de vida: ${title}`,
      'Projeto criado. Cada avanço exigirá o registro da cena, teste ou empreendimento que o resolveu.',
    );
  };

  const advanceLifeProject = async (projectId: string, title: string) => {
    const resolution = window.prompt(`Qual cena, teste ou empreendimento resolveu uma etapa de “${title}”?`, '');
    if (!resolution?.trim()) return;
    if (!confirm('Registrar uma etapa narrativa? Isso não concede automaticamente recursos, cura, XP ou benefícios de Sociedade.')) return;
    await apply(
      [{ type: 'advance_life_project', projectId, amount: 1, resolution, reason: 'Etapa resolvida e confirmada pelo jogador.' }],
      `avanço do projeto ${title}`,
      'Etapa registrada sem recompensa mecânica automática.',
    );
  };

  const SeasonIcon = masterState.worldClock.season === 'spring' ? Sprout
    : masterState.worldClock.season === 'summer' ? Sun
      : masterState.worldClock.season === 'autumn' ? Leaf : Snowflake;

  return (
    <div className="mx-auto flex w-full max-w-6xl flex-col gap-5 pb-12">
      <LivingWorldDashboard masterState={masterState} onApplyCommands={onApplyCommands} />
      <section className="rounded-lg border-2 border-emerald-900 bg-zinc-950/90 p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="flex items-center gap-2 text-xl font-bold text-emerald-200"><Sprout className="h-5 w-5" /> Mundo Vivo</h2>
            <p className="mt-1 max-w-3xl text-xs leading-relaxed text-emerald-700">
              Calendário, posição, estações, compromissos, ameaças e atores persistem entre cenas. Passar tempo não resolve empreendimentos nem concede cura, XP, Esperança, Sociedade ou provisões.
            </p>
          </div>
          <span className="rounded border border-blue-900 bg-blue-950/30 px-3 py-2 text-xs font-bold text-blue-200">{MODE_LABEL[masterState.activeMode]}</span>
        </div>
        <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="rounded border border-emerald-900/70 bg-black/30 p-3"><p className="text-[10px] uppercase text-emerald-600">Calendário</p><p className="mt-1 flex items-center gap-2 font-bold text-emerald-100"><CalendarDays className="h-4 w-4" /> Dia {masterState.worldClock.day}, {masterState.worldClock.year} T.E.</p></div>
          <div className="rounded border border-emerald-900/70 bg-black/30 p-3"><p className="text-[10px] uppercase text-emerald-600">Estação</p><p className="mt-1 flex items-center gap-2 font-bold text-emerald-100"><SeasonIcon className="h-4 w-4" /> {SEASON_LABEL[masterState.worldClock.season]}</p></div>
          <div className="rounded border border-emerald-900/70 bg-black/30 p-3"><p className="text-[10px] uppercase text-emerald-600">Hora</p><p className="mt-1 flex items-center gap-2 font-bold text-emerald-100"><Clock3 className="h-4 w-4" /> {String(masterState.worldClock.hour).padStart(2, '0')}:{String(masterState.worldClock.minute).padStart(2, '0')}</p></div>
          <div className="rounded border border-emerald-900/70 bg-black/30 p-3"><p className="text-[10px] uppercase text-emerald-600">Posição canônica</p><p className="mt-1 flex items-center gap-2 font-bold text-emerald-100"><MapPin className="h-4 w-4" /> {masterState.position.locationName || 'Eriador'}</p></div>
        </div>
        <div className="mt-4">
          <p className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-emerald-400"><FastForward className="h-4 w-4" /> Passagem segura do tempo</p>
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-6">
            {PASSAGES.map(item => <button key={item.label} type="button" disabled={busy} onClick={() => void advanceTime(item.minutes, item.label)} className="min-h-11 rounded border border-emerald-800 bg-emerald-950/25 px-3 py-2 text-xs font-bold text-emerald-200 disabled:opacity-50">{item.label}</button>)}
          </div>
          {status && <p className="mt-3 rounded border border-emerald-800 bg-emerald-950/30 p-2 text-xs text-emerald-200">{status}</p>}
          {error && <p className="mt-3 rounded border border-red-800 bg-red-950/30 p-2 text-xs text-red-200">{error}</p>}
        </div>
      </section>

      <section className="rounded-lg border border-teal-900 bg-zinc-950/85 p-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="flex items-center gap-2 text-sm font-bold uppercase text-teal-300"><Sprout className="h-4 w-4" /> Vida entre aventuras</h3>
            <p className="mt-1 text-[10px] leading-relaxed text-teal-700">Lares, famílias, ofícios e uma trupe itinerante permanecem na crônica. São registros narrativos; resultados continuam dependendo das cenas, testes e empreendimentos previstos pelas regras.</p>
          </div>
          <button type="button" disabled={busy} onClick={() => void createLifeProject()} className="rounded border border-teal-700 px-3 py-2 text-xs font-bold text-teal-200 disabled:opacity-50">Novo projeto de vida</button>
        </div>
        <div className="mt-3 grid grid-cols-2 gap-2 lg:grid-cols-4">
          {(Object.keys(COMPANY_STYLE_LABEL) as Array<keyof typeof COMPANY_STYLE_LABEL>).map(style => (
            <button key={style} type="button" disabled={busy} onClick={() => void configureCompanyLife(style)} className={`min-h-11 rounded border px-2 py-2 text-[10px] font-bold ${masterState.livingWorld.companyStyle === style ? 'border-teal-400 bg-teal-950/50 text-teal-100' : 'border-teal-950 text-teal-600'}`}>{COMPANY_STYLE_LABEL[style]}</button>
          ))}
        </div>
        <p className="mt-3 text-xs text-teal-500"><strong>{COMPANY_STYLE_LABEL[masterState.livingWorld.companyStyle]}</strong>{masterState.livingWorld.troupeName ? ` · ${masterState.livingWorld.troupeName}` : ''} · base: {masterState.livingWorld.baseLocation || masterState.position.locationName}</p>
        <div className="mt-4 grid gap-3 lg:grid-cols-2">
          {(party.length ? party : characters).map(character => {
            const life = masterState.livingWorld.heroLives[character.id];
            return (
              <div key={character.id} className="rounded border border-teal-950 bg-black/25 p-3 text-xs">
                <div className="flex flex-wrap items-center justify-between gap-2"><span className="font-bold text-teal-100">{character.sheet.identity.name || character.name}</span><span className="text-teal-600">{LIFESTYLE_LABEL[life?.lifestyle || 'adventurer']}</span></div>
                <p className="mt-1 text-teal-700">{life?.occupation || 'Sem ofício registrado'} · {life?.home || 'sem lar fixo'}{life?.troupeRole ? ` · ${life.troupeRole}` : ''}</p>
                {life?.householdName && <p className="mt-1 text-teal-500">Lar: {life.householdName}</p>}
                {!!life?.householdMembers.length && <div className="mt-2 space-y-1">{life.householdMembers.map(member => <p key={member.id} className="rounded bg-teal-950/20 px-2 py-1 text-[10px] text-teal-500">{member.name} · {member.relation || 'vínculo'} · {member.age} anos</p>)}</div>}
                <div className="mt-2 flex flex-wrap gap-2"><button type="button" disabled={busy} onClick={() => void editHeroLife(character)} className="rounded border border-teal-800 px-2 py-1 text-[10px] font-bold text-teal-300">Editar vida</button><button type="button" disabled={busy} onClick={() => void addHouseholdMember(character)} className="rounded border border-teal-800 px-2 py-1 text-[10px] font-bold text-teal-300">Adicionar família</button></div>
              </div>
            );
          })}
        </div>
        <div className="mt-4 space-y-2">
          {!masterState.livingWorld.projects.length && <p className="text-xs text-teal-800">Nenhum projeto de vida registrado.</p>}
          {masterState.livingWorld.projects.map(project => (
            <div key={project.id} className="rounded border border-teal-950 bg-black/25 p-3 text-xs">
              <div className="flex flex-wrap justify-between gap-2"><span className="font-bold text-teal-100">{project.title}</span><span className="text-teal-600">{PROJECT_KIND_LABEL[project.kind]} · {project.progress}/{project.target} · {project.status}</span></div>
              <div className="mt-2 h-2 overflow-hidden rounded bg-teal-950"><div className="h-full bg-teal-500" style={{ width: `${project.progress / Math.max(1, project.target) * 100}%` }} /></div>
              <p className="mt-1 text-[10px] text-teal-700">{project.location}{project.lastResolution ? ` · última etapa: ${project.lastResolution}` : ''}</p>
              {project.status === 'active' && <button type="button" disabled={busy} onClick={() => void advanceLifeProject(project.id, project.title)} className="mt-2 rounded border border-teal-800 px-2 py-1 text-[10px] font-bold text-teal-300">Registrar etapa resolvida</button>}
            </div>
          ))}
        </div>
      </section>

      <div className="grid gap-5 lg:grid-cols-2">
        <section className="rounded-lg border border-blue-900 bg-zinc-950/85 p-4">
          <h3 className="flex items-center gap-2 text-sm font-bold uppercase text-blue-300"><Users className="h-4 w-4" /> Companhia através dos anos</h3>
          <div className="mt-3 space-y-2">
            {(party.length ? party : characters).map(character => (
              <div key={character.id} className="flex items-center justify-between rounded border border-blue-950 bg-black/25 px-3 py-2 text-xs">
                <span className="font-bold text-blue-100">{character.sheet.identity.name || character.name}</span>
                <span className="text-blue-500">{character.sheet.identity.age || 0} anos · {character.sheet.identity.culture || 'cultura não definida'}</span>
              </div>
            ))}
          </div>
          <p className="mt-3 text-[10px] leading-relaxed text-blue-700">Ao completar um ano civil, a idade aumenta em um. Nenhum atributo é alterado automaticamente.</p>
        </section>

        <section className="rounded-lg border border-amber-900 bg-zinc-950/85 p-4">
          <h3 className="flex items-center gap-2 text-sm font-bold uppercase text-amber-300"><ShieldAlert className="h-4 w-4" /> Ameaças em movimento</h3>
          <div className="mt-3 space-y-3">
            {!masterState.threatClocks.length && <p className="text-xs text-amber-800">Nenhuma ameaça formal registrada.</p>}
            {masterState.threatClocks.map(clock => (
              <div key={clock.id} className="rounded border border-amber-950 bg-black/25 p-3">
                <div className="flex items-center justify-between gap-2 text-xs"><span className="font-bold text-amber-100">{clock.hidden ? 'Ameaça oculta do Mestre' : clock.name}</span><span className="text-amber-500">{clock.current}/{clock.max}</span></div>
                <div className="mt-2 h-2 overflow-hidden rounded bg-amber-950"><div className="h-full bg-amber-600" style={{ width: `${Math.max(0, Math.min(100, clock.current / Math.max(1, clock.max) * 100))}%` }} /></div>
                {!clock.hidden && clock.consequence && <p className="mt-2 text-[10px] text-amber-700">{clock.consequence}</p>}
                <button type="button" disabled={busy || clock.current >= clock.max} onClick={() => void apply([{ type: 'advance_threat', clockId: clock.id, amount: 1, reason: 'Avanço confirmado no painel Mundo Vivo.' }], `avanço da ameaça ${clock.id}`, 'A ameaça avançou um segmento sem aplicar consequência automática.')} className="mt-2 rounded border border-amber-800 px-2 py-1 text-[10px] font-bold text-amber-300 disabled:opacity-40">Avançar 1</button>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-lg border border-violet-900 bg-zinc-950/85 p-4">
          <h3 className="flex items-center gap-2 text-sm font-bold uppercase text-violet-300"><ScrollText className="h-4 w-4" /> Missões e compromissos</h3>
          <div className="mt-3 space-y-2">
            {!masterState.quests.length && <p className="text-xs text-violet-800">Nenhuma missão formal registrada.</p>}
            {masterState.quests.map(quest => <div key={quest.id} className="rounded border border-violet-950 bg-black/25 p-3 text-xs"><div className="flex justify-between gap-2"><span className="font-bold text-violet-100">{quest.title}</span><span className="uppercase text-violet-600">{quest.status}</span></div><p className="mt-1 text-violet-700">{quest.currentStep || 'Próximo passo ainda não definido.'}</p>{quest.deadline && <p className="mt-1 text-[10px] text-amber-600">Prazo: {quest.deadline}</p>}</div>)}
          </div>
        </section>

        <section className="rounded-lg border border-cyan-900 bg-zinc-950/85 p-4">
          <h3 className="flex items-center gap-2 text-sm font-bold uppercase text-cyan-300"><Users className="h-4 w-4" /> Atores persistentes</h3>
          <div className="mt-3 max-h-80 space-y-2 overflow-y-auto">
            {!actors.length && <p className="text-xs text-cyan-800">Os NPCs ganharão objetivos e próximas ações conforme forem encontrados.</p>}
            {actors.map(({ actor, npc }) => <div key={actor.npcId} className="rounded border border-cyan-950 bg-black/25 p-3 text-xs"><div className="flex justify-between gap-2"><span className="font-bold text-cyan-100">{npc!.name}</span><span className="text-cyan-700">{npc!.lifeStatus==='dead'?'morto':npc!.lifeStatus==='missing'?'desaparecido':npc!.presentInScene ? 'na cena' : actor.location || 'longe'}</span></div><p className="mt-1 text-cyan-600">Estado: {actor.emotionalState || 'neutro'} · condição econômica: {npc!.wealthTier || 'modesta'}</p><p className="mt-1 text-cyan-700">Próxima ação: {npc!.lifeStatus==='dead'?'nenhuma':actor.nextAction || 'ainda não definida'}</p>{npc!.memorySummary&&<p className="mt-1 line-clamp-2 text-[10px] text-slate-500">Memória recente: {npc!.memorySummary}</p>}{(npc!.commitments||[]).filter(item=>item.status==='open').length>0&&<p className="mt-1 text-[10px] text-amber-600">Compromissos abertos: {(npc!.commitments||[]).filter(item=>item.status==='open').length}</p>}</div>)}
          </div>
        </section>
      </div>
    </div>
  );
};
