import React, { useEffect, useMemo, useState } from 'react';
import { Activity, AlertTriangle, CheckCircle, Download, Mic, Play, RefreshCw, Trash2, Volume2 } from 'lucide-react';
import { AudioDiagnostics, AudioDiagnosticEvent, AudioDiagnosticSession } from '../lib/audioDiagnostics';
import { AudioEngine } from '../lib/AudioEngine';
import { SoundItem, SoundKeywordRule } from '../lib/db';
import { listSoundKeywordRules } from '../lib/soundKeywordRules';
import { findBestSoundForEvent, findBestSoundForQuery, planSounds } from '../lib/soundPlanner';
import { generateNarration } from '../services/geminiService';
import { speakWithAndroid } from '../lib/narration';
import { hasGeminiApiKey } from '../lib/apiKey';
import { APP_VERSION } from '../lib/version';
import { exportBackupNative } from '../lib/native';

interface AudioDiagnosticsPanelProps { sounds: SoundItem[]; }

type TestName = 'music' | 'ambience' | 'sfx' | 'charon' | 'android' | 'all' | null;

const SAMPLE_TEXT = 'A chuva murmura sobre o telhado da estalagem. Três batidas secas soam na porta. Ao longe, o ferreiro golpeia a bigorna com o martelo. Uma carroça passa pela estrada, e passos ecoam sobre a pedra.';

const delay = (ms: number) => new Promise(resolve => window.setTimeout(resolve, ms));

export const AudioDiagnosticsPanel: React.FC<AudioDiagnosticsPanelProps> = ({ sounds }) => {
  const [events, setEvents] = useState<AudioDiagnosticEvent[]>([]);
  const [session, setSession] = useState<AudioDiagnosticSession>(AudioDiagnostics.currentSession());
  const [running, setRunning] = useState<TestName>(null);
  const [status, setStatus] = useState('Pronto para testar no aparelho.');
  const [showAll, setShowAll] = useState(false);

  useEffect(() => AudioDiagnostics.subscribe((next, currentSession) => { setEvents(next); setSession(currentSession); }), []);

  const currentEvents = useMemo(() => events.filter(event => event.sessionId === session.id), [events, session.id]);
  const visibleEvents = (showAll ? events : currentEvents).slice(-120).reverse();
  const counts = useMemo(() => ({
    success: currentEvents.filter(event => event.level === 'success').length,
    warning: currentEvents.filter(event => event.level === 'warning').length,
    error: currentEvents.filter(event => event.level === 'error').length,
  }), [currentEvents]);

  const findChannelSound = (channel: 'music' | 'ambience' | 'sfx', query: string): SoundItem | null =>
    findBestSoundForQuery(sounds, channel, query)
      || sounds.find(sound => sound.channel === channel)
      || sounds.find(sound => channel === 'sfx' ? sound.category === 'EFEITOS' : channel === 'music' ? sound.category === 'MÚSICA' : sound.category === 'AMBIÊNCIA')
      || null;

  const withTest = async (name: Exclude<TestName, null>, task: () => Promise<void>) => {
    if (running) return;
    setRunning(name); setStatus('Executando teste…');
    try { await task(); setStatus('Teste concluído. Confira os eventos e o som ouvido.'); }
    catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      AudioDiagnostics.log({ action: 'diagnostic-test-error', channel: 'system', level: 'error', message, result: 'falha', details: { test: name } });
      setStatus(`Falha: ${message}`);
    } finally { setRunning(null); }
  };

  const testLoop = (channel: 'music' | 'ambience', query: string) => withTest(channel, async () => {
    AudioDiagnostics.startSession(`teste-${channel}`, { query });
    const sound = findChannelSound(channel, query);
    if (!sound) throw new Error(`Nenhum arquivo de ${channel === 'music' ? 'música' : 'ambiência'} foi encontrado.`);
    AudioDiagnostics.log({ action: 'diagnostic-file-selected', channel, level: 'success', message: `Arquivo selecionado: ${sound.name}.`, soundName: sound.name, soundId: sound.nativeId || sound.id, searchTerms: query.split(/\s+/), result: 'selecionado' });
    await AudioEngine.preloadSounds([sound]);
    await AudioEngine.playLoop(channel, sound, sound.name, 'diagnostic');
    await delay(6000);
    AudioEngine.stopChannel(channel, 'diagnostic');
  });

  const testSfx = () => withTest('sfx', async () => {
    AudioDiagnostics.startSession('teste-sfx', { phrase: 'três batidas na porta' });
    const rules = await listSoundKeywordRules();
    const plan = planSounds('Estalagem de Bri', 'Três batidas secas soam na porta de madeira.', '', undefined, [], sounds, rules);
    const event = plan.sfx[0];
    const sound = event ? findBestSoundForEvent(sounds, event) : findChannelSound('sfx', 'batida porta knock door madeira');
    if (!sound) throw new Error('Nenhum efeito de batida na porta foi encontrado.');
    AudioDiagnostics.log({ action: 'diagnostic-sfx-match', channel: 'sfx', level: 'success', message: `“${event?.matchedPhrase || 'batida na porta'}” → “${sound.name}”.`, phrase: event?.matchedPhrase || 'batida na porta', searchTerms: event ? [event.keyword, ...event.aliases] : ['batida', 'porta', 'knock', 'door'], soundName: sound.name, soundId: sound.nativeId || sound.id, result: 'vinculado' });
    await AudioEngine.preloadSounds([sound]);
    const scheduledAtMs = Date.now();
    await AudioEngine.playSfx(sound, sound.name, { semanticId: event?.id || 'door-knock', eventId: `diagnostic:${Date.now()}`, phrase: event?.matchedPhrase || 'batida na porta', searchTerms: event ? [event.keyword, ...event.aliases] : ['batida', 'porta', 'knock', 'door'], scheduledAtMs });
    await delay(2500);
  });

  const testCharon = () => withTest('charon', async () => {
    AudioDiagnostics.startSession('teste-charon', { text: 'A chama da lareira vacila no salão silencioso.' });
    if (!hasGeminiApiKey()) throw new Error('Nenhuma chave Gemini está configurada para testar Charon.');
    const requestedAt = Date.now();
    AudioDiagnostics.log({ action: 'diagnostic-charon-request', channel: 'voice', level: 'info', message: 'Solicitando uma frase curta à voz Charon.', backend: 'Gemini TTS', scheduledAtMs: requestedAt, result: 'solicitado' });
    const audio = await generateNarration('A chama da lareira vacila no salão silencioso.', 'Charon', 'narrador profundo, sábio e contido');
    if (!audio) throw new Error('Charon não devolveu áudio.');
    AudioDiagnostics.log({ action: 'diagnostic-charon-ready', channel: 'voice', level: 'success', message: 'Áudio de Charon recebido.', backend: 'Gemini TTS', scheduledAtMs: requestedAt, actualAtMs: Date.now(), latencyMs: Date.now() - requestedAt, result: 'pronto' });
    await AudioEngine.playVoice(audio);
  });

  const testAndroid = () => withTest('android', async () => {
    AudioDiagnostics.startSession('teste-voz-android', { text: 'Teste da voz Android do Historiador.' });
    AudioEngine.setNarrationDucking(true);
    try { await speakWithAndroid('Teste da voz Android do Historiador. A narração está funcionando.'); }
    finally { AudioEngine.setNarrationDucking(false); }
  });

  const runFullTest = () => withTest('all', async () => {
    AudioDiagnostics.startSession('autoteste-completo', { sampleText: SAMPLE_TEXT, soundCount: sounds.length });
    const rules: SoundKeywordRule[] = await listSoundKeywordRules();
    const inventory = {
      total: sounds.length,
      music: sounds.filter(sound => sound.channel === 'music' || sound.category === 'MÚSICA').length,
      ambience: sounds.filter(sound => sound.channel === 'ambience' || sound.category === 'AMBIÊNCIA').length,
      sfx: sounds.filter(sound => sound.channel === 'sfx' || sound.category === 'EFEITOS').length,
      native: sounds.filter(sound => sound.source === 'native').length,
      indexedDb: sounds.filter(sound => sound.source !== 'native').length,
      keywordRules: rules.length,
    };
    AudioDiagnostics.log({ action: 'autotest-inventory', channel: 'system', level: sounds.length && rules.length ? 'success' : 'error', message: `${sounds.length} áudios e ${rules.length} regras disponíveis.`, result: sounds.length && rules.length ? 'válido' : 'incompleto', details: inventory });
    await AudioEngine.ready();

    const plan = planSounds('Estalagem de Bri, estrada e ferraria', SAMPLE_TEXT, '', undefined, [], sounds, rules);
    const ambience = plan.ambience ? findBestSoundForQuery(sounds, 'ambience', plan.ambience) : findChannelSound('ambience', 'tavern rain fireplace');
    const music = plan.music ? findBestSoundForQuery(sounds, 'music', plan.music) : findChannelSound('music', 'calm tavern');
    const entries = plan.sfx.map((event, index) => ({ event, cueId: `autotest_${index}_${event.id}`, sound: findBestSoundForEvent(sounds, event) }));
    for (const entry of entries) {
      AudioDiagnostics.log({ action: entry.sound ? 'autotest-sfx-resolved' : 'autotest-sfx-missing', channel: 'sfx', level: entry.sound ? 'success' : 'error', message: entry.sound ? `“${entry.event.matchedPhrase}” → “${entry.sound.name}”.` : `Sem arquivo para “${entry.event.matchedPhrase}”.`, phrase: entry.event.matchedPhrase, searchTerms: [entry.event.keyword, ...entry.event.aliases], soundName: entry.sound?.name, soundId: entry.sound?.nativeId || entry.sound?.id, plannedAtMs: entry.event.charIndex, result: entry.sound ? 'vinculado' : 'ausente' });
    }
    const selected = [ambience, music, ...entries.map(entry => entry.sound)].filter((sound): sound is SoundItem => Boolean(sound));
    await AudioEngine.preloadSounds(selected);
    if (ambience) await AudioEngine.playLoop('ambience', ambience, ambience.name, 'diagnostic');
    else AudioDiagnostics.log({ action: 'autotest-ambience-missing', channel: 'ambience', level: 'error', message: 'Nenhuma ambiência encontrada para o cenário de teste.', result: 'ausente' });
    if (music) await AudioEngine.playLoop('music', music, music.name, 'diagnostic');
    else AudioDiagnostics.log({ action: 'autotest-music-missing', channel: 'music', level: 'warning', message: 'Nenhuma música encontrada para o cenário de teste.', result: 'ausente' });
    await delay(500);

    const byCue = new Map(entries.map(entry => [entry.cueId, entry]));
    const fired = new Set<string>();
    AudioEngine.setNarrationDucking(true);
    try {
      await speakWithAndroid(SAMPLE_TEXT, {
        cues: entries.map(entry => ({ id: entry.cueId, charIndex: entry.event.charIndex })),
        onCue: cueId => {
          const entry = byCue.get(cueId);
          if (!entry?.sound || fired.has(cueId)) return;
          fired.add(cueId);
          const scheduledAtMs = Date.now();
          void AudioEngine.playSfx(entry.sound, entry.sound.name, { semanticId: entry.event.id, eventId: `autotest:${cueId}`, phrase: entry.event.matchedPhrase, searchTerms: [entry.event.keyword, ...entry.event.aliases], plannedAtMs: entry.event.charIndex, scheduledAtMs });
        },
      });
    } finally { AudioEngine.setNarrationDucking(false); }

    if (hasGeminiApiKey()) {
      const audio = await generateNarration('A voz de Charon encerra o autoteste do Braseiro.', 'Charon', 'narrador profundo e contido');
      if (audio) await AudioEngine.playVoice(audio);
      else AudioDiagnostics.log({ action: 'autotest-charon-empty', channel: 'voice', level: 'error', message: 'Charon não devolveu áudio durante o autoteste.', backend: 'Gemini TTS', result: 'vazio' });
    } else AudioDiagnostics.log({ action: 'autotest-charon-skipped', channel: 'voice', level: 'warning', message: 'Charon não foi testado porque não há chave Gemini configurada.', backend: 'Gemini TTS', result: 'ignorado' });

    await delay(800);
    AudioEngine.stopChannel('ambience', 'diagnostic');
    AudioEngine.stopChannel('music', 'diagnostic');
    AudioDiagnostics.log({ action: 'autotest-complete', channel: 'system', level: entries.some(entry => !entry.sound) ? 'warning' : 'success', message: `Autoteste concluído: ${entries.filter(entry => entry.sound).length}/${entries.length} efeitos vinculados.`, result: entries.some(entry => !entry.sound) ? 'parcial' : 'concluído', details: inventory });
  });

  const exportReport = async () => {
    const rules = await listSoundKeywordRules().catch(() => []);
    const inventory = {
      totalSounds: sounds.length,
      nativeSounds: sounds.filter(sound => sound.source === 'native').length,
      localSounds: sounds.filter(sound => sound.source !== 'native').length,
      music: sounds.filter(sound => sound.channel === 'music' || sound.category === 'MÚSICA').length,
      ambience: sounds.filter(sound => sound.channel === 'ambience' || sound.category === 'AMBIÊNCIA').length,
      sfx: sounds.filter(sound => sound.channel === 'sfx' || sound.category === 'EFEITOS').length,
      keywordRules: rules.length,
      filenames: sounds.map(sound => sound.name),
    };
    const report = await AudioDiagnostics.buildReport({ appVersion: APP_VERSION, inventory });
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const fileName = `diagnostico_audio_oumanel_${timestamp}.json`;
    const json = JSON.stringify(report, null, 2);
    if (window.BraseiroAndroid?.exportBackup) await exportBackupNative(fileName, json);
    else {
      const url = URL.createObjectURL(new Blob([json], { type: 'application/json' }));
      const link = document.createElement('a'); link.href = url; link.download = fileName; link.click(); URL.revokeObjectURL(url);
    }
    setStatus(`Relatório exportado: ${fileName}`);
  };

  const rowColor = (level: AudioDiagnosticEvent['level']) => level === 'error' ? 'border-red-900/70 bg-red-950/20' : level === 'warning' ? 'border-amber-900/70 bg-amber-950/15' : level === 'success' ? 'border-emerald-900/60 bg-emerald-950/10' : 'border-slate-800 bg-slate-950/45';

  return (
    <section className="medieval-panel p-5 lg:col-span-2">
      <div className="flex flex-wrap items-start justify-between gap-3 border-b border-blue-900/60 pb-4">
        <div><h2 className="medieval-title flex items-center gap-2"><Activity className="h-5 w-5" /> Diagnóstico real de áudio</h2><p className="mt-1 text-xs text-slate-400">Sessão atual: <strong className="text-blue-200">{session.mode}</strong>. Registra seleção, arquivo, cache, backend, momento solicitado, início real, atraso e falhas.</p></div>
        <div className="flex gap-2 text-xs"><span className="rounded border border-emerald-800 px-2 py-1 text-emerald-300">✓ {counts.success}</span><span className="rounded border border-amber-800 px-2 py-1 text-amber-300">⚠ {counts.warning}</span><span className="rounded border border-red-800 px-2 py-1 text-red-300">✕ {counts.error}</span></div>
      </div>

      <div className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-6">
        <button disabled={Boolean(running)} onClick={() => void testLoop('music', 'calm tavern travel music')} className="medieval-button flex min-h-12 items-center justify-center gap-2 disabled:opacity-50"><Play className="h-4 w-4" /> Testar música</button>
        <button disabled={Boolean(running)} onClick={() => void testLoop('ambience', 'tavern rain fireplace forest')} className="medieval-button flex min-h-12 items-center justify-center gap-2 disabled:opacity-50"><Volume2 className="h-4 w-4" /> Testar ambiência</button>
        <button disabled={Boolean(running)} onClick={() => void testSfx()} className="medieval-button flex min-h-12 items-center justify-center gap-2 disabled:opacity-50"><Volume2 className="h-4 w-4" /> Testar SFX</button>
        <button disabled={Boolean(running)} onClick={() => void testCharon()} className="medieval-button flex min-h-12 items-center justify-center gap-2 disabled:opacity-50"><Mic className="h-4 w-4" /> Testar Charon</button>
        <button disabled={Boolean(running)} onClick={() => void testAndroid()} className="medieval-button flex min-h-12 items-center justify-center gap-2 disabled:opacity-50"><Mic className="h-4 w-4" /> Voz Android</button>
        <button disabled={Boolean(running)} onClick={() => void runFullTest()} className="flex min-h-12 items-center justify-center gap-2 rounded border border-cyan-600 bg-cyan-950/45 px-3 font-bold text-cyan-100 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${running === 'all' ? 'animate-spin' : ''}`} /> Autoteste completo</button>
      </div>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3 rounded border border-blue-950 bg-slate-950/65 p-3 text-sm text-slate-300"><span>{running ? `Executando: ${running}…` : status}</span><div className="flex flex-wrap gap-2"><button onClick={() => setShowAll(value => !value)} className="text-xs text-blue-300">{showAll ? 'Mostrar só a sessão' : 'Mostrar histórico'}</button><button onClick={() => AudioDiagnostics.clear()} className="flex items-center gap-1 text-xs text-red-300"><Trash2 className="h-3.5 w-3.5" /> Limpar</button><button onClick={() => void exportReport()} className="flex items-center gap-1 text-xs text-cyan-300"><Download className="h-3.5 w-3.5" /> Exportar relatório</button></div></div>

      <div className="mt-4 max-h-[52vh] space-y-2 overflow-y-auto pr-1">
        {!visibleEvents.length ? <p className="py-10 text-center text-slate-500">Nenhum evento registrado. Execute um teste ou reproduza uma narração.</p> : visibleEvents.map(event => (
          <article key={event.id} className={`rounded border p-3 ${rowColor(event.level)}`}>
            <div className="flex flex-wrap items-center justify-between gap-2"><p className="flex items-center gap-2 text-xs font-bold uppercase tracking-wide text-slate-200">{event.level === 'error' ? <AlertTriangle className="h-4 w-4 text-red-400" /> : event.level === 'success' ? <CheckCircle className="h-4 w-4 text-emerald-400" /> : <Activity className="h-4 w-4 text-blue-300" />}{event.channel || 'system'} · {event.action}</p><span className="text-[10px] text-slate-500">+{event.elapsedMs} ms</span></div>
            <p className="mt-1 text-sm text-slate-200">{event.message}</p>
            <div className="mt-2 grid gap-x-4 gap-y-1 text-[11px] text-slate-400 sm:grid-cols-2 lg:grid-cols-4">
              {event.phrase && <span><strong>Frase:</strong> {event.phrase}</span>}
              {event.soundName && <span className="truncate" title={event.soundName}><strong>Arquivo:</strong> {event.soundName}</span>}
              {event.backend && <span><strong>Backend:</strong> {event.backend}</span>}
              {event.latencyMs !== undefined && <span><strong>Atraso:</strong> {event.latencyMs} ms</span>}
              {event.searchTerms?.length ? <span className="sm:col-span-2 lg:col-span-4"><strong>Termos:</strong> {event.searchTerms.slice(0, 18).join(', ')}</span> : null}
            </div>
          </article>
        ))}
      </div>
    </section>
  );
};
