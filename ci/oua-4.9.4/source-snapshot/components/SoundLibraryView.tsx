import React, { useEffect, useMemo, useRef, useState } from 'react';
import { Music, Play, Search, Square, Trash2, Upload, Volume2, VolumeX } from 'lucide-react';
import { AudioChannel, AudioEngine } from '../lib/AudioEngine';
import { deleteSoundItem, listSoundMetadata, saveSoundItem, SoundItem } from '../lib/db';
import { applyRulesToSound, importSoundKeywordRules, listSoundKeywordRules, synchronizeSoundKeywordRules } from '../lib/soundKeywordRules';
import { hasNativeAudio, importNativeSoundFolder, importNativeSounds, listNativeSounds } from '../lib/native';
import { ensureSoundKeywordLibrary, invalidateSoundKeywordBootstrap } from '../lib/soundLibraryBootstrap';
import { AudioDiagnosticsPanel } from './AudioDiagnosticsPanel';

function normalize(value: unknown): string {
  return String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
}

function classifySoundFilename(filename: string): Pick<SoundItem, 'category' | 'channel' | 'tags' | 'catalogChannel'> {
  const name = normalize(filename).replace(/[_\-.]+/g, ' ');
  const tags = Array.from(new Set(name.split(/\s+/).filter(token => token.length > 2)));
  if (/(^| )(bgm|music|musica)( |$)|theme|song|melody|bardic|battle music|tension music|calm music|travel music/.test(name)) {
    return { category: 'MÚSICA', channel: 'music', catalogChannel: 'MUSIC', tags };
  }
  if (/chuva|rain|garoa|drizzle|tempestade|storm|trovao|thunder|neve|snow|vento|wind/.test(name)) {
    return { category: 'AMBIÊNCIA', channel: 'weather', catalogChannel: 'WEATHER', tags };
  }
  if (/(^| )(amb|tex|loop)( |$)|ambience|ambient|floresta|forest|taverna|tavern|fireplace|lareira|campfire|drone|swamp|cave|subterranean|river|stream/.test(name)) {
    return { category: 'AMBIÊNCIA', channel: 'ambience', catalogChannel: 'AMBIENCE_BASE', tags };
  }
  return { category: 'EFEITOS', channel: 'sfx', catalogChannel: 'SFX', tags };
}

interface SoundLibraryViewProps {
  onAudioChange: () => void;
  audioActive: boolean;
}

export const SoundLibraryView: React.FC<SoundLibraryViewProps> = ({ onAudioChange }) => {
  const [sounds, setSounds] = useState<SoundItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<'todos' | 'AMBIÊNCIA' | 'MÚSICA' | 'EFEITOS'>('todos');
  const [isUploading, setIsUploading] = useState(false);
  const [status, setStatus] = useState('');
  const [previewingId, setPreviewingId] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const keywordInputRef = useRef<HTMLInputElement>(null);
  const [keywordRuleCount, setKeywordRuleCount] = useState(0);
  const [keywordLibrarySource, setKeywordLibrarySource] = useState('');
  const [volumes, setVolumes] = useState<Record<AudioChannel, number>>({
    voice: AudioEngine.getVolume('voice'), ambience: AudioEngine.getVolume('ambience'),
    weather: AudioEngine.getVolume('weather'), music: AudioEngine.getVolume('music'), sfx: AudioEngine.getVolume('sfx'),
  });

  const loadSounds = async (forceKeywords = false) => {
    try {
      const bootstrap = await ensureSoundKeywordLibrary(forceKeywords).catch(() => ({ source: 'regras já salvas', importedRules: 0 }));
      setKeywordLibrarySource(bootstrap.source);
      const [native, local, rules] = await Promise.all([
        hasNativeAudio() ? listNativeSounds() : Promise.resolve([]),
        listSoundMetadata(),
        listSoundKeywordRules(),
      ]);
      const unique = new Map<string, SoundItem>();
      [...native, ...local].forEach(item => unique.set(item.id, item));
      setKeywordRuleCount(rules.length);
      setSounds(Array.from(unique.values()).map(item => applyRulesToSound(item, rules)).sort((a, b) => a.name.localeCompare(b.name, 'pt-BR')));
    } catch (error) {
      console.error('[BRASEIRO_AUDIO] biblioteca:', error);
      setStatus(error instanceof Error ? error.message : 'Falha ao carregar a biblioteca.');
    }
  };

  useEffect(() => {
    void loadSounds();
    const changed = () => {
      void (async () => {
        invalidateSoundKeywordBootstrap();
        await ensureSoundKeywordLibrary(true).catch(() => ({ source: 'catálogo nativo', importedRules: 0 }));
        await synchronizeSoundKeywordRules(await listNativeSounds().catch(() => [])).catch(() => ({ matchedSounds: 0, unmatchedRules: 0 }));
        await loadSounds();
        onAudioChange();
      })();
    };
    const progress = (event: Event) => {
      const detail = (event as CustomEvent<{ imported: number; skipped: number; failed: number; current?: string; state?: string; background?: boolean }>).detail;
      if (!detail) return;
      if (detail.state === 'succeeded') {
        setStatus(`Importação concluída em segundo plano: ${detail.imported} importados, ${detail.skipped} ignorados e ${detail.failed} com falha.`);
      } else if (detail.state === 'failed' || detail.state === 'cancelled') {
        setStatus(`A importação em segundo plano foi ${detail.state === 'failed' ? 'interrompida por uma falha' : 'cancelada'}. Abra a Central de saúde para retomar.`);
      } else {
        setStatus(`Importando em segundo plano: ${detail.imported} concluídos, ${detail.skipped} ignorados, ${detail.failed} falharam${detail.current ? ` — ${detail.current}` : ''}`);
      }
    };
    window.addEventListener('braseiro-native-audio-changed', changed);
    window.addEventListener('braseiro-native-audio-progress', progress);
    return () => {
      window.removeEventListener('braseiro-native-audio-changed', changed);
      window.removeEventListener('braseiro-native-audio-progress', progress);
    };
  }, []);

  const handleNativeImport = async () => {
    setIsUploading(true); setStatus('Importando por streaming para o armazenamento seguro do aplicativo…');
    try {
      const result = await importNativeSounds();
      invalidateSoundKeywordBootstrap();
      if (result.keywordLibraryText?.trim()) {
        const parsed = JSON.parse(result.keywordLibraryText);
        await importSoundKeywordRules(parsed, await listNativeSounds().catch(() => []));
      }
      const bootstrap = await ensureSoundKeywordLibrary(true);
      setStatus(`${result.imported} importados, ${result.skipped} ignorados e ${result.failed} com falha. Catálogo SQLite: ${result.catalogCount ?? 'atualizado'} sons. Biblioteca: ${bootstrap.importedRules} regras (${bootstrap.source}).${result.errors?.length ? ` ${result.errors.slice(0, 2).join(' | ')}` : ''}`);
      await synchronizeSoundKeywordRules(await listNativeSounds().catch(() => [])).catch(() => ({ matchedSounds: 0, unmatchedRules: 0 }));
      await loadSounds(); onAudioChange();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Falha na importação nativa.');
    } finally { setIsUploading(false); }
  };

  const handleNativeFolderImport = async () => {
    setIsUploading(true); setStatus('Lendo a pasta e copiando os sons por streaming…');
    try {
      const result = await importNativeSoundFolder();
      if (result.scheduled) {
        setStatus(`Importação agendada em segundo plano${result.resumed ? ' para retomar o trabalho anterior' : ''}. Pode apagar a tela ou trocar de aplicação; o Android manterá o processo recuperável.`);
        return;
      }
      invalidateSoundKeywordBootstrap();
      if (result.keywordLibraryText?.trim()) {
        const parsed = JSON.parse(result.keywordLibraryText);
        await importSoundKeywordRules(parsed, await listNativeSounds().catch(() => []));
      }
      const bootstrap = await ensureSoundKeywordLibrary(true);
      setStatus(`${result.imported} importados da pasta, ${result.skipped} ignorados e ${result.failed} com falha. Catálogo SQLite: ${result.catalogCount ?? 'atualizado'} sons. Biblioteca: ${bootstrap.importedRules} regras (${bootstrap.source}).${result.errors?.length ? ` ${result.errors.slice(0, 2).join(' | ')}` : ''}`);
      await synchronizeSoundKeywordRules(await listNativeSounds().catch(() => [])).catch(() => ({ matchedSounds: 0, unmatchedRules: 0 }));
      await loadSounds(); onAudioChange();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'Falha ao importar a pasta.');
    } finally { setIsUploading(false); }
  };

  const handleWebFiles = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files ?? []) as File[];
    if (!files.length) return;
    setIsUploading(true); setStatus('Gravando arquivos individualmente…');
    let imported = 0; let failed = 0;
    try {
      for (const file of files) {
        try {
          if (/\.zip$/i.test(file.name)) throw new Error('ZIP grande deve ser importado no APK pelo botão nativo.');
          if (!file.type.startsWith('audio/') && !/\.(mp3|wav|ogg|m4a|aac|flac|opus)$/i.test(file.name)) continue;
          const classification = classifySoundFilename(file.name);
          const item: SoundItem = {
            id: `sound_${crypto.randomUUID?.() || `${Date.now()}_${Math.random()}`}`,
            name: file.name.replace(/\.[^.]+$/, '').replace(/[-_]+/g, ' '),
            mimeType: file.type || 'audio/*', size: file.size, lastModified: file.lastModified,
            signature: `${file.name}_${file.size}_${file.lastModified}`, category: classification.category,
            channel: classification.channel, catalogChannel: classification.catalogChannel, tags: classification.tags, source: 'indexeddb', createdAt: Date.now(),
          };
          await saveSoundItem(item, file); imported += 1;
        } catch (error) { failed += 1; console.error(error); }
      }
      await synchronizeSoundKeywordRules(await listSoundMetadata()).catch(() => ({ matchedSounds: 0, unmatchedRules: 0 }));
      setStatus(`${imported} arquivos importados; ${failed} falharam.`); await loadSounds(); onAudioChange();
    } finally {
      setIsUploading(false); if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleKeywordJson = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setIsUploading(true); setStatus('Lendo o JSON de palavras-chave…');
    try {
      const raw = JSON.parse(await file.text());
      const result = await importSoundKeywordRules(raw, sounds);
      invalidateSoundKeywordBootstrap();
      setStatus(`${result.importedRules} regras importadas; ${result.matchedSounds} áudios vinculados agora; ${result.unmatchedRules} regras aguardam o respectivo áudio.`);
      await loadSounds(); onAudioChange();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : 'O JSON de palavras-chave é inválido.');
    } finally {
      setIsUploading(false);
      if (keywordInputRef.current) keywordInputRef.current.value = '';
    }
  };

  const setVolume = (channel: AudioChannel, value: number) => {
    AudioEngine.setVolume(channel, value); setVolumes(previous => ({ ...previous, [channel]: value })); onAudioChange();
  };

  const preview = async (sound: SoundItem) => {
    AudioEngine.stopChannel('sfx');
    try { setPreviewingId(sound.id); await AudioEngine.playPreview(sound); onAudioChange(); }
    catch (error) { setStatus(error instanceof Error ? error.message : 'Não foi possível reproduzir o arquivo.'); }
    finally { window.setTimeout(() => setPreviewingId(current => current === sound.id ? null : current), 1200); }
  };

  const remove = async (sound: SoundItem) => {
    if (!confirm(`Excluir “${sound.name}”?`)) return;
    if (sound.source === 'native' && sound.nativeId) await window.BraseiroAndroid?.deleteNativeSound?.(sound.nativeId);
    else await deleteSoundItem(sound.id);
    await loadSounds();
  };

  const clearAll = async () => {
    if (!confirm('Esvaziar toda a biblioteca sonora? A campanha não será apagada.')) return;
    AudioEngine.stopAll();
    await window.BraseiroAndroid?.clearNativeSounds?.();
    for (const sound of sounds.filter(item => item.source !== 'native')) await deleteSoundItem(sound.id);
    await loadSounds();
  };

  const filtered = useMemo(() => sounds.filter(sound => {
    const matchesSearch = normalize(`${sound.name} ${(sound.tags || []).join(' ')}`).includes(normalize(searchTerm));
    return matchesSearch && (selectedCategory === 'todos' || sound.category === selectedCategory);
  }), [sounds, searchTerm, selectedCategory]);

  const sliders: Array<[AudioChannel, string]> = [
    ['voice', 'Voz'], ['sfx', 'SFX'], ['ambience', 'Ambiência-base'], ['weather', 'Clima'], ['music', 'Música'],
  ];

  return (
    <div className="mx-auto grid w-full max-w-6xl gap-6 pb-16 lg:grid-cols-[320px_1fr] text-slate-100">
      <aside className="medieval-panel h-fit p-5">
        <h2 className="medieval-title mb-5 flex items-center gap-2"><Volume2 className="h-5 w-5" /> Mesa de som</h2>
        <div className="space-y-5">
          {sliders.map(([channel, label]) => (
            <label key={channel} className="block">
              <span className="mb-2 flex justify-between text-xs font-bold uppercase tracking-wider text-blue-200"><span>{label}</span><span>{Math.round(volumes[channel] * 100)}%</span></span>
              <input className="w-full accent-blue-400" type="range" min="0" max="1" step="0.01" value={volumes[channel]} onChange={event => setVolume(channel, Number(event.target.value))} />
            </label>
          ))}
        </div>
        <button onClick={() => AudioEngine.stopAll()} className="mt-6 flex w-full items-center justify-center gap-2 rounded border border-red-800 bg-red-950/40 px-3 py-3 font-bold text-red-200">
          <VolumeX className="h-4 w-4" /> Parar tudo
        </button>
      </aside>

      <section className="medieval-panel p-5">
        <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
          <h2 className="medieval-title flex items-center gap-2"><Music className="h-5 w-5" /> Biblioteca sonora <span className="text-sm text-slate-400">({sounds.length} áudios · {keywordRuleCount} regras)</span></h2>
          <button onClick={clearAll} className="flex items-center gap-2 text-xs text-red-300"><Trash2 className="h-4 w-4" /> Limpar biblioteca</button>
        </div>
        <div className="grid gap-3 md:grid-cols-[1fr_190px]">
          <label className="flex items-center gap-2 rounded border border-blue-900/70 bg-slate-950/70 px-3"><Search className="h-4 w-4 text-blue-300" /><input value={searchTerm} onChange={event => setSearchTerm(event.target.value)} placeholder="Buscar por nome ou palavra-chave…" className="w-full bg-transparent py-3 outline-none" /></label>
          <select value={selectedCategory} onChange={event => setSelectedCategory(event.target.value as typeof selectedCategory)} className="rounded border border-blue-900/70 bg-slate-950 px-3 text-blue-100">
            <option value="todos">Todos os sons</option><option value="AMBIÊNCIA">Ambiências</option><option value="MÚSICA">Músicas</option><option value="EFEITOS">Efeitos</option>
          </select>
        </div>

        {hasNativeAudio() ? (
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            <button disabled={isUploading} onClick={handleNativeImport} className="flex w-full items-center justify-center gap-2 rounded border border-blue-500 bg-blue-950/50 px-4 py-3 font-bold text-blue-100 disabled:opacity-60">
              <Upload className="h-5 w-5" /> {isUploading ? 'Importando…' : 'Arquivos ou ZIP'}
            </button>
            <button disabled={isUploading} onClick={handleNativeFolderImport} className="flex w-full items-center justify-center gap-2 rounded border border-cyan-700 bg-cyan-950/30 px-4 py-3 font-bold text-cyan-100 disabled:opacity-60">
              <Upload className="h-5 w-5" /> {isUploading ? 'Importando…' : 'Pasta inteira'}
            </button>
          </div>
        ) : (
          <label className="mt-4 flex cursor-pointer items-center justify-center gap-2 rounded border border-blue-500 bg-blue-950/50 px-4 py-3 font-bold text-blue-100">
            <Upload className="h-5 w-5" /> {isUploading ? 'Importando…' : 'Selecionar arquivos de áudio'}
            <input ref={fileInputRef} type="file" multiple accept="audio/*,.mp3,.wav,.ogg,.m4a,.aac,.flac,.opus" className="hidden" onChange={handleWebFiles} />
          </label>
        )}
        <button disabled={isUploading} onClick={() => keywordInputRef.current?.click()} className="mt-2 flex w-full items-center justify-center gap-2 rounded border border-violet-700 bg-violet-950/30 px-4 py-3 font-bold text-violet-100 disabled:opacity-60">
          <Upload className="h-5 w-5" /> Importar JSON de palavras-chave
        </button>
        <input ref={keywordInputRef} type="file" accept="application/json,.json" className="hidden" onChange={handleKeywordJson} />
        <p className="mt-2 text-[11px] leading-relaxed text-slate-400">Ao importar uma pasta ou ZIP, o app usa <strong>biblioteca_sons.json</strong> uma vez para enriquecer o catálogo SQLite e reconhece o arquivo opcional <strong>audio_overrides.json</strong>. No PLAY, a escolha é feita pelo catálogo local, sem consultar o JSON grande. Fonte de palavras-chave: {keywordLibrarySource || 'carregando…'}.</p>
        {status && <p className="mt-3 rounded border border-blue-950 bg-slate-950/70 p-3 text-sm text-slate-300">{status}</p>}

        <div className="mt-5 max-h-[60vh] space-y-2 overflow-y-auto pr-1">
          {!filtered.length ? <div className="py-16 text-center text-slate-500">Nenhum som encontrado.</div> : filtered.map(sound => (
            <article key={sound.id} className="flex items-center gap-3 rounded border border-slate-800 bg-slate-950/55 p-3">
              <button onClick={() => preview(sound)} className="rounded-full border border-blue-800 p-2 text-blue-200">{previewingId === sound.id ? <Square className="h-4 w-4" /> : <Play className="h-4 w-4" />}</button>
              <div className="min-w-0 flex-1"><p className="truncate font-semibold text-slate-100">{sound.name}</p><p className="text-[11px] uppercase tracking-wide text-blue-300">{sound.catalogChannel || sound.channel || sound.category} · {sound.familyKey || 'sem família'} · {sound.source === 'native' ? 'SQLite/Android' : 'IndexedDB'} · {(sound.size / 1024 / 1024).toFixed(1)} MB</p></div>
              <button onClick={() => remove(sound)} className="p-2 text-red-400"><Trash2 className="h-4 w-4" /></button>
            </article>
          ))}
        </div>
      </section>
      <AudioDiagnosticsPanel sounds={sounds} />
    </div>
  );
};
