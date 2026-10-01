import React, { useMemo, useRef, useState } from 'react';
import { CheckCircle2, Crown, Download, FileJson, Plus, RefreshCw, Shield, Trash2, Upload, UserRound, UsersRound } from 'lucide-react';
import type { Character, CharacterSheet } from '../lib/db';
import { CharacterSheetView } from './CharacterSheetView';
import { characterFileName, exportCharacterJson } from '../lib/characterPortability';
import { exportBackupNative } from '../lib/native';

interface CharacterRegistryViewProps {
  characters: Character[];
  activeCharacterId?: string | null;
  partyCharacterIds?: string[];
  onSelectCharacter: (characterId: string) => Promise<void> | void;
  onToggleCharacterInGame: (characterId: string) => Promise<void> | void;
  onCreateRandom: () => Promise<void> | void;
  onCreateBlank: () => Promise<void> | void;
  onSaveCharacter: (character: Character, sheet: CharacterSheet) => Promise<void> | void;
  onDeleteCharacter: (characterId: string) => Promise<void> | void;
  onImportCharacterJson: (json: string) => Promise<void> | void;
}

export const CharacterRegistryView: React.FC<CharacterRegistryViewProps> = ({
  characters,
  activeCharacterId,
  partyCharacterIds = [],
  onSelectCharacter,
  onToggleCharacterInGame,
  onCreateRandom,
  onCreateBlank,
  onSaveCharacter,
  onDeleteCharacter,
  onImportCharacterJson,
}) => {
  const [editingId, setEditingId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [portabilityMessage, setPortabilityMessage] = useState('');
  const jsonInputRef = useRef<HTMLInputElement>(null);
  const editing = useMemo(() => characters.find(character => character.id === editingId) || null, [characters, editingId]);
  const partyIds = useMemo(() => new Set(partyCharacterIds), [partyCharacterIds]);

  const run = async (action: () => Promise<void> | void) => {
    if (busy) return;
    setBusy(true);
    try { await action(); } finally { setBusy(false); }
  };


  const exportJson = async (character: Character) => {
    try {
      const json = exportCharacterJson(character);
      const fileName = characterFileName(character);
      if (window.BraseiroAndroid?.exportBackup) {
        const saved = await exportBackupNative(fileName, json);
        setPortabilityMessage(`Ficha exportada: ${saved}.`);
      } else {
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob); const anchor = document.createElement('a');
        anchor.href = url; anchor.download = fileName; document.body.appendChild(anchor); anchor.click(); anchor.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 10_000);
        setPortabilityMessage(`Download solicitado: ${fileName}.`);
      }
    } catch (error) { setPortabilityMessage(`Falha ao exportar: ${error instanceof Error ? error.message : 'erro desconhecido'}`); }
  };

  const importJsonFile = async (file?: File) => {
    if (!file) return;
    await run(async () => {
      try { await onImportCharacterJson(await file.text()); setPortabilityMessage(`Ficha importada de ${file.name}. O retrato precisa ser escolhido neste aparelho.`); }
      catch (error) { setPortabilityMessage(`Importação recusada: ${error instanceof Error ? error.message : 'JSON inválido'}`); }
      finally { if (jsonInputRef.current) jsonInputRef.current.value = ''; }
    });
  };

  if (editing) {
    const active = editing.id === activeCharacterId;
    const inGame = partyIds.has(editing.id);
    return (
      <section className="medieval-panel p-4 sm:p-6">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <button onClick={() => setEditingId(null)} className="rounded border border-blue-800 px-3 py-2 text-sm text-blue-200">← Voltar aos personagens</button>
          <div className="flex flex-wrap gap-2">
            <button type="button" onClick={() => void exportJson(editing)} className="flex items-center gap-1 rounded border border-cyan-800 px-3 py-1 text-xs text-cyan-200"><Download className="h-3.5 w-3.5" /> JSON</button>
            {inGame && <span className="rounded-full border border-emerald-700/60 bg-emerald-950/30 px-3 py-1 text-xs text-emerald-200">Em jogo</span>}
            {active && <span className="rounded-full border border-cyan-600/60 bg-cyan-950/30 px-3 py-1 text-xs text-cyan-200">Herói da vez</span>}
          </div>
        </div>
        <CharacterSheetView sheet={editing.sheet} onSaveSheet={sheet => onSaveCharacter(editing, sheet)} />
      </section>
    );
  }

  return (
    <section className="medieval-panel p-4 sm:p-6">
      <div className="mb-5 flex flex-wrap items-start justify-between gap-4 border-b border-blue-950/70 pb-4">
        <div>
          <h2 className="medieval-title text-xl sm:text-2xl">Companhia de Heróis</h2>
          <p className="mt-1 max-w-2xl text-sm text-slate-400">
            Vários heróis podem permanecer <strong className="text-emerald-300">Em jogo</strong> ao mesmo tempo. “Herói da vez” define apenas qual ficha recebe a próxima ação, teste e alteração mecânica. A criação aleatória usa o modo estrito do Livro Básico 2e e recusa qualquer ficha que não passe a auditoria das regras.
          </p>
          <div className="mt-2 flex items-center gap-2 text-xs text-blue-400">
            <UsersRound className="h-4 w-4" /> {partyCharacterIds.length} {partyCharacterIds.length === 1 ? 'herói em jogo' : 'heróis em jogo'}
          </div>
        </div>
        <div className="flex flex-wrap gap-2">
          <button disabled={busy} onClick={() => run(onCreateRandom)} className="flex items-center gap-2 rounded border border-cyan-700 bg-cyan-950/30 px-3 py-2 text-sm font-bold text-cyan-100 disabled:opacity-50">
            <RefreshCw className="h-4 w-4" /> Criar aleatório pelas regras
          </button>
          <button disabled={busy} onClick={() => run(onCreateBlank)} className="flex items-center gap-2 rounded border border-blue-700 bg-blue-950/30 px-3 py-2 text-sm font-bold text-blue-100 disabled:opacity-50">
            <Plus className="h-4 w-4" /> Criar ficha vazia
          </button>
          <button disabled={busy} onClick={() => jsonInputRef.current?.click()} className="flex items-center gap-2 rounded border border-emerald-700 bg-emerald-950/25 px-3 py-2 text-sm font-bold text-emerald-100 disabled:opacity-50">
            <Upload className="h-4 w-4" /> Importar ficha JSON
          </button>
          <input ref={jsonInputRef} type="file" accept="application/json,.json" className="hidden" onChange={event => void importJsonFile(event.target.files?.[0])} />
        </div>
      </div>

      {portabilityMessage && <div className="mb-4 rounded border border-cyan-900/60 bg-cyan-950/20 p-3 text-xs text-cyan-100"><FileJson className="mr-2 inline h-4 w-4" />{portabilityMessage}</div>}

      {characters.length === 0 ? (
        <div className="rounded border border-dashed border-blue-800/70 p-8 text-center text-slate-400">
          <UserRound className="mx-auto mb-3 h-9 w-9 text-blue-500" />
          Nenhum personagem cadastrado. Crie um herói aleatório pelas regras, importe um JSON ou abra uma ficha vazia.
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          {characters.map(character => {
            const active = character.id === activeCharacterId;
            const inGame = partyIds.has(character.id);
            const sheet = character.sheet;
            const cannotRemove = inGame && partyCharacterIds.length <= 1;
            return (
              <article key={character.id} className={`rounded-lg border p-4 transition ${active ? 'border-cyan-500 bg-cyan-950/15 shadow-[0_0_18px_rgba(34,211,238,.12)]' : inGame ? 'border-emerald-800 bg-emerald-950/10' : 'border-blue-950 bg-black/30 hover:border-blue-700'}`}>
                <div className="flex items-start justify-between gap-3">
                  <button className="min-w-0 flex-1 text-left" onClick={() => setEditingId(character.id)}>
                    <div className="flex items-center gap-2">
                      <Shield className={`h-5 w-5 ${active ? 'text-cyan-400' : inGame ? 'text-emerald-400' : 'text-blue-500'}`} />
                      <h3 className="truncate text-lg font-bold text-blue-100">{sheet.identity.name || character.name}</h3>
                    </div>
                    <p className="mt-1 text-xs text-blue-400/80">{sheet.identity.culture || character.culture} · {sheet.identity.vocation || character.vocation}</p>
                    <p className="mt-2 line-clamp-2 text-sm leading-relaxed text-slate-400">{sheet.identity.description || 'História ainda não registrada.'}</p>
                  </button>
                  <button
                    onClick={() => {
                      if (characters.length <= 1) return alert('Crie outro personagem antes de apagar o único herói da companhia.');
                      if (confirm(`Apagar ${sheet.identity.name || character.name}?`)) void run(() => onDeleteCharacter(character.id));
                    }}
                    className="rounded border border-red-950 p-2 text-red-500 hover:bg-red-950/30"
                    title="Apagar personagem"
                  ><Trash2 className="h-4 w-4" /></button>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {inGame && <span className="flex items-center gap-1 rounded-full border border-emerald-800/70 bg-emerald-950/30 px-2.5 py-1 text-[11px] font-bold text-emerald-200"><CheckCircle2 className="h-3.5 w-3.5" /> Em jogo</span>}
                  {active && <span className="flex items-center gap-1 rounded-full border border-cyan-700/70 bg-cyan-950/30 px-2.5 py-1 text-[11px] font-bold text-cyan-200"><Crown className="h-3.5 w-3.5" /> Herói da vez</span>}
                </div>

                <div className="mt-4 grid grid-cols-3 gap-2 text-center text-xs">
                  <div className="rounded bg-black/30 p-2"><span className="block text-slate-500">Resistência</span><strong className="text-red-300">{sheet.attributes.enduranceCurrent}/{sheet.attributes.enduranceMax}</strong></div>
                  <div className="rounded bg-black/30 p-2"><span className="block text-slate-500">Esperança</span><strong className="text-cyan-300">{sheet.attributes.hopeCurrent}/{sheet.attributes.hopeMax}</strong></div>
                  <div className="rounded bg-black/30 p-2"><span className="block text-slate-500">Carga</span><strong className="text-blue-200">{sheet.attributes.load}</strong></div>
                </div>

                <div className="mt-4 grid grid-cols-2 gap-2">
                  <button onClick={() => setEditingId(character.id)} className="rounded border border-blue-800 px-3 py-2 text-sm text-blue-200">Abrir ficha</button>
                  <button onClick={() => void exportJson(character)} className="rounded border border-cyan-800 px-3 py-2 text-sm text-cyan-200"><Download className="mr-1 inline h-4 w-4" /> Exportar JSON</button>
                  <button
                    disabled={busy || cannotRemove}
                    onClick={() => run(() => onToggleCharacterInGame(character.id))}
                    title={cannotRemove ? 'A companhia precisa manter pelo menos um herói em jogo.' : inGame ? 'Retirar este herói da aventura atual' : 'Adicionar este herói à aventura atual'}
                    className={`rounded border px-3 py-2 text-sm font-bold disabled:cursor-not-allowed disabled:opacity-60 ${inGame ? 'border-emerald-700 bg-emerald-950/30 text-emerald-100' : 'border-blue-700 bg-blue-950/20 text-blue-100'}`}
                  >
                    {inGame ? <span className="flex items-center justify-center gap-1"><CheckCircle2 className="h-4 w-4" /> Em jogo</span> : 'Usar este herói'}
                  </button>
                  {inGame && (
                    <button
                      disabled={active || busy}
                      onClick={() => run(() => onSelectCharacter(character.id))}
                      className={`col-span-2 rounded border px-3 py-2 text-sm font-bold ${active ? 'border-cyan-700 bg-cyan-950/30 text-cyan-200' : 'border-blue-700 bg-blue-950/20 text-blue-100'}`}
                    >
                      {active ? <span className="flex items-center justify-center gap-1"><Crown className="h-4 w-4" /> Herói da vez</span> : 'Tornar herói da vez'}
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
};
