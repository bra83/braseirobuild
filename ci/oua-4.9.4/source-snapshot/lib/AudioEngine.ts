import { decodeBase64ToBytes } from '../utils/audio';
import { getSoundBlob, SoundItem } from './db';
import { hasNativeAudio } from './native';
import { SfxPlaybackOptions } from './sfxPlaybackPolicy';
import { AudioDiagnostics } from './audioDiagnostics';
import { speakWithAndroid } from './narration';

export type AudioChannel = 'voice' | 'ambience' | 'weather' | 'music' | 'sfx';
type LoopChannel = 'ambience' | 'weather' | 'music';

type ActiveLoop = { name: string; source: AudioBufferSourceNode; gain: GainNode };
type ActiveOneShot = { source: AudioBufferSourceNode; gain: GainNode; timer?: number };

export interface VoicePlaybackOptions {
  onStarted?: () => void;
  timedCues?: Array<{ id: string; relativePosition: number; trigger: () => void }>;
}

const DEFAULT_VOLUMES: Record<AudioChannel, number> = {
  voice: 1,
  ambience: 0.35,
  weather: 0.28,
  music: 0.14,
  sfx: 0.80,
};

const MAX_WEB_SFX_STREAMS = 6;
const MIN_LOOP_DURATION_MS = 10_000;
const NATIVE_TTS_PREFIX = 'braseiro-native-tts:';
const ANDROID_TTS_FALLBACK_PREFIX = 'braseiro-android-tts-fallback:';
const MAX_SFX_PLAY_MS = 14_000;
const SFX_FILE_DEDUP_MS = 180;


interface NativeTtsReference { token: string; durationMs: number; model?: string; tier?: string }

function parseNativeTtsReference(value: string): NativeTtsReference | null {
  if (!value.startsWith(NATIVE_TTS_PREFIX)) return null;
  try {
    const parsed = JSON.parse(decodeURIComponent(value.slice(NATIVE_TTS_PREFIX.length))) as NativeTtsReference;
    return parsed?.token && Number.isFinite(parsed.durationMs) && parsed.durationMs > 0 ? parsed : null;
  } catch {
    return null;
  }
}

function parseAndroidTtsFallback(value: string): { text: string } | null {
  if (!value.startsWith(ANDROID_TTS_FALLBACK_PREFIX)) return null;
  try {
    const parsed = JSON.parse(decodeURIComponent(value.slice(ANDROID_TTS_FALLBACK_PREFIX.length))) as { text?: string };
    return parsed?.text?.trim() ? { text: parsed.text } : null;
  } catch {
    return null;
  }
}

function storedVolume(channel: AudioChannel): number {
  try {
    const value = Number(localStorage.getItem(`braseiro.audio.volume.${channel}`));
    return Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : DEFAULT_VOLUMES[channel];
  } catch {
    return DEFAULT_VOLUMES[channel];
  }
}

class AudioEngineImpl {
  private ctx: AudioContext | null = null;
  private channelGains: Record<AudioChannel, GainNode | null> = { voice: null, ambience: null, weather: null, music: null, sfx: null };
  private activeLoops = new Map<string, ActiveLoop>();
  private nativeLoopNames = new Map<string, string>();
  private activeOneShots = new Set<ActiveOneShot>();
  private currentVoiceSource: AudioBufferSourceNode | null = null;
  private volumes: Record<AudioChannel, number> = {
    voice: storedVolume('voice'),
    ambience: storedVolume('ambience'),
    weather: storedVolume('weather'),
    music: storedVolume('music'),
    sfx: storedVolume('sfx'),
  };
  private voiceGeneration = 0;
  private narrationDuckMusic = 0.42;
  private narrationDuckAmbience = 0.62;
  private sfxSession = 0;
  private nativeSessionId: string | null = null;
  private sfxFileCooldowns = new Map<string, number>();
  private playedSfxEvents = new Set<string>();
  private bufferCache = new Map<string, Promise<AudioBuffer>>();
  private voiceCueTimers = new Set<number>();
  private loopGenerations = new Map<string, number>();
  private sfxGeneration = 0;

  public init() {
    if (!this.ctx) {
      try {
        this.ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
        AudioDiagnostics.log({ action: 'audio-context-created', channel: 'system', level: 'success', message: `AudioContext criado em ${this.ctx.sampleRate} Hz.`, backend: 'WebAudio', result: this.ctx.state, details: { sampleRate: this.ctx.sampleRate, state: this.ctx.state } });
        (Object.keys(this.channelGains) as AudioChannel[]).forEach(channel => {
          const gain = this.ctx!.createGain();
          gain.gain.value = this.volumes[channel];
          gain.connect(this.ctx!.destination);
          this.channelGains[channel] = gain;
        });
      } catch (error) {
        console.error('[BRASEIRO_AUDIO] não foi possível criar AudioContext:', error);
        AudioDiagnostics.log({ action: 'audio-context-error', channel: 'system', level: 'error', message: error instanceof Error ? error.message : 'Não foi possível criar AudioContext.', backend: 'WebAudio', result: 'falha' });
      }
    }
    if (this.ctx?.state === 'suspended') void this.ctx.resume().catch(() => undefined);
  }

  public async ready(): Promise<void> {
    this.init();
    if (this.ctx?.state === 'suspended') {
      const startedAt = Date.now();
      try {
        await this.ctx.resume();
        AudioDiagnostics.log({ action: 'audio-context-resumed', channel: 'system', level: 'success', message: 'AudioContext retomado por interação do usuário.', backend: 'WebAudio', actualAtMs: Date.now(), latencyMs: Date.now() - startedAt, result: this.ctx.state });
      } catch (error) {
        AudioDiagnostics.log({ action: 'audio-context-resume-error', channel: 'system', level: 'error', message: error instanceof Error ? error.message : 'Falha ao retomar AudioContext.', backend: 'WebAudio', result: 'falha' });
        throw error;
      }
    }
  }

  public getContext(): AudioContext | null {
    this.init();
    return this.ctx;
  }

  public setVolume(channel: AudioChannel, volume: number) {
    const safe = Math.max(0, Math.min(1, volume));
    this.volumes[channel] = safe;
    try { localStorage.setItem(`braseiro.audio.volume.${channel}`, String(safe)); } catch {}
    const gain = this.channelGains[channel];
    if (gain && this.ctx) gain.gain.setTargetAtTime(safe, this.ctx.currentTime, 0.05);
    try { window.BraseiroAndroid?.setNativeAudioVolume?.(channel, safe); } catch {}
    AudioDiagnostics.log({ action: 'volume-changed', channel, level: 'info', message: `Volume de ${channel} ajustado para ${Math.round(safe * 100)}%.`, volume: safe, result: 'aplicado' });
  }

  public getVolume(channel: AudioChannel): number { return this.volumes[channel]; }

  public beginNarrationSession(sceneId = '', backend = 'hybrid'): string {
    if (this.nativeSessionId) {
      try { window.BraseiroAndroid?.finishAudioSession?.(this.nativeSessionId, 'replaced'); } catch {}
    }
    this.sfxSession += 1;
    this.nativeSessionId = `audio_${Date.now()}_${this.sfxSession}_${Math.random().toString(36).slice(2, 8)}`;
    try { window.BraseiroAndroid?.beginAudioSession?.(this.nativeSessionId, sceneId, backend); } catch {}
    AudioDiagnostics.log({ action: 'narration-audio-session-reset', channel: 'system', level: 'info', message: `Sessão interna de SFX reiniciada (${this.sfxSession}).`, details: { sfxSession: this.sfxSession, persistentSessionId: this.nativeSessionId, sceneId, backend } });
    this.sfxFileCooldowns.clear();
    this.playedSfxEvents.clear();
    this.stopChannel('sfx');
    return this.nativeSessionId;
  }

  public isNarrationSessionActive(expectedSessionId: string): boolean {
    return Boolean(expectedSessionId) && this.nativeSessionId === expectedSessionId;
  }

  public finishNarrationSession(status = 'finished', expectedSessionId?: string) {
    const id = expectedSessionId || this.nativeSessionId;
    if (!id || (expectedSessionId && this.nativeSessionId !== expectedSessionId)) return;
    this.nativeSessionId = null;
    try { window.BraseiroAndroid?.finishAudioSession?.(id, status); } catch {}
    AudioDiagnostics.log({ action: 'narration-audio-session-finished', channel: 'system', level: status === 'finished' ? 'success' : 'warning', message: `Sessão de áudio ${status}.`, details: { persistentSessionId: id, status } });
  }

  private soundCacheKey(sound: SoundItem): string {
    return `${sound.source || 'indexeddb'}:${sound.nativeId || sound.id}:${sound.signature || sound.size || 0}`;
  }

  private async resolveBlob(source: Blob | SoundItem): Promise<Blob> {
    if (source instanceof Blob) return source;
    if (source.blob) return source.blob;
    const blob = await getSoundBlob(source.id);
    if (!blob) throw new Error(`Arquivo de áudio não encontrado: ${source.name}`);
    return blob;
  }

  private async decodeBlob(blob: Blob): Promise<AudioBuffer> {
    await this.ready();
    if (!this.ctx) throw new Error('Mecanismo de áudio indisponível.');
    const raw = await blob.arrayBuffer();
    return this.ctx.decodeAudioData(raw.slice(0));
  }

  private decodeSound(sound: SoundItem): Promise<AudioBuffer> {
    const key = this.soundCacheKey(sound);
    let promise = this.bufferCache.get(key);
    if (!promise) {
      promise = this.resolveBlob(sound).then(blob => this.decodeBlob(blob));
      this.bufferCache.set(key, promise);
      promise.catch(() => this.bufferCache.delete(key));
    }
    return promise;
  }

  public async preloadSounds(sounds: SoundItem[]): Promise<void> {
    const unique = Array.from(new Map(sounds.filter(Boolean).map(sound => [sound.nativeId || sound.id, sound])).values());
    const nativeIds = unique
      .filter(sound => sound.nativeId && sound.source === 'native' && (sound.channel === 'sfx' || sound.category === 'EFEITOS'))
      .map(sound => sound.nativeId!);
    const web = unique.filter(sound => !(sound.nativeId && sound.source === 'native'));
    const startedAt = Date.now();
    AudioDiagnostics.log({ action: 'preload-start', channel: 'system', level: 'info', message: `Preparando ${unique.length} arquivo(s) de áudio.`, details: { native: nativeIds.length, web: web.length, sounds: unique.map(sound => sound.name) } });
    const failures: string[] = [];
    await Promise.all([
      nativeIds.length ? window.BraseiroAndroid?.preloadNativeSounds?.(nativeIds).catch(error => { failures.push(`Android: ${error instanceof Error ? error.message : String(error)}`); }) : Promise.resolve(),
      ...web.map(sound => this.decodeSound(sound).then(() => undefined).catch(error => { failures.push(`${sound.name}: ${error instanceof Error ? error.message : String(error)}`); })),
    ]);
    AudioDiagnostics.log({ action: 'preload-complete', channel: 'system', level: failures.length ? 'warning' : 'success', message: failures.length ? `Pré-carregamento concluído com ${failures.length} falha(s).` : `Pré-carregamento concluído para ${unique.length} arquivo(s).`, actualAtMs: Date.now(), latencyMs: Date.now() - startedAt, result: failures.length ? 'parcial' : 'concluído', details: { failures } });
  }

  private async playNative(sound: SoundItem, channel: AudioChannel, loop: boolean, slot: string): Promise<boolean> {
    if (!sound.nativeId || !hasNativeAudio() || !window.BraseiroAndroid?.playNativeSound) return false;
    const scheduledAtMs = Date.now();
    AudioDiagnostics.log({ action: 'native-play-request', channel, level: 'info', message: `Solicitando reprodução nativa de “${sound.name}”.`, soundName: sound.name, soundId: sound.nativeId, backend: channel === 'sfx' && !loop ? 'SoundPool/MediaPlayer' : 'MediaPlayer', slot, scheduledAtMs, volume: this.volumes[channel], result: 'solicitado' });
    try {
      const accepted = Boolean(await window.BraseiroAndroid.playNativeSound(sound.nativeId, channel, loop, this.volumes[channel], slot, scheduledAtMs));
      AudioDiagnostics.log({ action: accepted ? 'native-play-accepted' : 'native-play-rejected', channel, level: accepted ? 'success' : 'warning', message: accepted ? `Android aceitou “${sound.name}” para reprodução.` : `Android rejeitou “${sound.name}”; será tentado o mecanismo WebAudio.`, soundName: sound.name, soundId: sound.nativeId, backend: 'Android', slot, scheduledAtMs, actualAtMs: Date.now(), latencyMs: Date.now() - scheduledAtMs, volume: this.volumes[channel], result: accepted ? 'aceito' : 'rejeitado' });
      if (!accepted) window.BraseiroAndroid?.markAudioFailure?.(sound.nativeId);
      return accepted;
    } catch (error) {
      AudioDiagnostics.log({ action: 'native-play-error', channel, level: 'error', message: error instanceof Error ? error.message : `Falha nativa ao reproduzir ${sound.name}.`, soundName: sound.name, soundId: sound.nativeId, backend: 'Android', slot, result: 'falha' });
      window.BraseiroAndroid?.markAudioFailure?.(sound.nativeId);
      return false;
    }
  }

  public async playLoop(channel: LoopChannel, source: Blob | SoundItem, name?: string, slot = 'base'): Promise<void> {
    const soundName = source instanceof Blob ? (name || 'loop') : source.name;
    const key = `${channel}:${slot}`;
    const generation = (this.loopGenerations.get(key) || 0) + 1;
    this.loopGenerations.set(key, generation);
    await this.ready();
    if (this.loopGenerations.get(key) !== generation) return;
    const scheduledAtMs = Date.now();
    const knownDurationMs = source instanceof Blob ? 0 : Number(source.durationMs || 0);
    if (knownDurationMs > 0 && knownDurationMs < MIN_LOOP_DURATION_MS) {
      AudioDiagnostics.log({ action: 'loop-rejected-short', channel, level: 'warning', message: `Loop recusado: “${soundName}” tem menos de 10 segundos.`, soundName, soundId: source instanceof Blob ? undefined : (source.nativeId || source.id), slot, result: 'rejeitado', details: { durationMs: knownDurationMs, minimumDurationMs: MIN_LOOP_DURATION_MS } });
      return;
    }
    AudioDiagnostics.log({ action: 'loop-request', channel, level: 'info', message: `Preparando loop de ${channel}: “${soundName}”.`, soundName, soundId: source instanceof Blob ? undefined : (source.nativeId || source.id), slot, scheduledAtMs, volume: this.volumes[channel], result: 'solicitado' });
    const existing = this.activeLoops.get(key);
    if (existing?.name === soundName || this.nativeLoopNames.get(key) === soundName) return;

    if (!(source instanceof Blob) && await this.playNative(source, channel, true, slot)) {
      if (this.loopGenerations.get(key) !== generation) {
        try { window.BraseiroAndroid?.stopNativeAudio?.(channel, slot); } catch {}
        return;
      }
      this.stopWebLoop(key, 650);
      this.nativeLoopNames.set(key, soundName);
      return;
    }
    if (this.nativeLoopNames.has(key)) {
      try { window.BraseiroAndroid?.stopNativeAudio?.(channel, slot); } catch {}
      this.nativeLoopNames.delete(key);
    }
    if (!this.ctx || !this.channelGains[channel]) return;
    const buffer = source instanceof Blob ? await this.decodeBlob(source) : await this.decodeSound(source);
    if (this.loopGenerations.get(key) !== generation) return;
    if (buffer.duration * 1000 < MIN_LOOP_DURATION_MS) {
      AudioDiagnostics.log({ action: 'loop-rejected-short', channel, level: 'warning', message: `Loop recusado após leitura: “${soundName}” tem ${buffer.duration.toFixed(2)} s.`, soundName, soundId: source instanceof Blob ? undefined : source.id, slot, backend: 'WebAudio', result: 'rejeitado', details: { durationSeconds: buffer.duration, minimumDurationSeconds: 10 } });
      return;
    }
    const audioSource = this.ctx.createBufferSource();
    const localGain = this.ctx.createGain();
    audioSource.buffer = buffer;
    audioSource.loop = true;
    localGain.gain.value = 0;
    audioSource.connect(localGain);
    localGain.connect(this.channelGains[channel]!);
    audioSource.start();
    AudioDiagnostics.log({ action: 'loop-started', channel, level: 'success', message: `Loop WebAudio iniciado: “${soundName}”.`, soundName, soundId: source instanceof Blob ? undefined : source.id, backend: 'WebAudio', slot, scheduledAtMs, actualAtMs: Date.now(), latencyMs: Date.now() - scheduledAtMs, volume: this.volumes[channel], result: 'reproduzindo' });
    localGain.gain.linearRampToValueAtTime(1, this.ctx.currentTime + 3);
    if (existing) this.stopWebLoop(key, 3000);
    this.activeLoops.set(key, { name: soundName, source: audioSource, gain: localGain });
  }

  private stopWebLoop(key: string, fadeMs = 0) {
    const loop = this.activeLoops.get(key);
    if (!loop) return;
    this.activeLoops.delete(key);
    if (fadeMs && this.ctx) {
      loop.gain.gain.cancelScheduledValues(this.ctx.currentTime);
      loop.gain.gain.setValueAtTime(loop.gain.gain.value, this.ctx.currentTime);
      loop.gain.gain.linearRampToValueAtTime(0, this.ctx.currentTime + fadeMs / 1000);
      window.setTimeout(() => { try { loop.source.stop(); } catch {} }, fadeMs + 60);
    } else {
      try { loop.source.stop(); } catch {}
    }
  }

  public async playSfx(source: Blob | SoundItem, name?: string, options: SfxPlaybackOptions = {}): Promise<void> {
    if (options.expectedSessionId && !this.isNarrationSessionActive(options.expectedSessionId)) return;
    const now = Date.now();
    const key = source instanceof Blob ? (name || 'blob') : (source.nativeId || source.id || source.name);
    const eventId = options.eventId || `${this.sfxSession}:${options.semanticId || key}:${now}`;
    if (this.playedSfxEvents.has(eventId)) {
      AudioDiagnostics.log({ action: 'sfx-deduplicated-event', channel: 'sfx', level: 'warning', message: `SFX ignorado por evento duplicado: ${name || key}.`, soundName: name || (source instanceof Blob ? undefined : source.name), soundId: source instanceof Blob ? undefined : (source.nativeId || source.id), phrase: options.phrase, searchTerms: options.searchTerms, scheduledAtMs: options.scheduledAtMs, result: 'ignorado' });
      return;
    }
    if (now - (this.sfxFileCooldowns.get(key) || 0) < SFX_FILE_DEDUP_MS) {
      AudioDiagnostics.log({ action: 'sfx-deduplicated-file', channel: 'sfx', level: 'warning', message: `SFX ignorado por repetição imediata do arquivo: ${name || key}.`, soundName: name || (source instanceof Blob ? undefined : source.name), soundId: source instanceof Blob ? undefined : (source.nativeId || source.id), phrase: options.phrase, searchTerms: options.searchTerms, scheduledAtMs: options.scheduledAtMs, result: 'ignorado' });
      return;
    }
    this.playedSfxEvents.add(eventId);
    this.sfxFileCooldowns.set(key, now);
    AudioDiagnostics.log({ action: 'sfx-request', channel: 'sfx', level: 'info', message: `Disparo solicitado para “${name || (source instanceof Blob ? 'blob' : source.name)}”.`, soundName: name || (source instanceof Blob ? undefined : source.name), soundId: source instanceof Blob ? undefined : (source.nativeId || source.id), phrase: options.phrase, searchTerms: options.searchTerms, plannedAtMs: options.plannedAtMs, scheduledAtMs: options.scheduledAtMs || now, volume: this.volumes.sfx, result: 'solicitado', details: { semanticId: options.semanticId, eventId, priority: options.priority } });
    const generation = this.sfxGeneration;
    await this.performSfx(source, name, key, eventId, generation, options);
  }

  private async performSfx(source: Blob | SoundItem, name: string | undefined, key: string, eventId: string, generation: number, options: SfxPlaybackOptions = {}): Promise<void> {
    await this.ready();
    if (generation !== this.sfxGeneration || (options.expectedSessionId && !this.isNarrationSessionActive(options.expectedSessionId))) return;
    const nativeSlot = `cue_${this.sfxSession}_${eventId.replace(/[^a-zA-Z0-9_-]/g, '_').slice(-72)}`;
    if (!(source instanceof Blob) && await this.playNative(source, 'sfx', false, nativeSlot)) {
      if (generation !== this.sfxGeneration || (options.expectedSessionId && !this.isNarrationSessionActive(options.expectedSessionId))) {
        try { window.BraseiroAndroid?.stopNativeAudio?.('sfx', nativeSlot); } catch {}
        return;
      }
      console.debug('[BRASEIRO_AUDIO] SFX nativo:', name || source.name);
      return;
    }
    if (!this.ctx || !this.channelGains.sfx) return;
    const buffer = source instanceof Blob ? await this.decodeBlob(source) : await this.decodeSound(source);
    if (generation !== this.sfxGeneration || (options.expectedSessionId && !this.isNarrationSessionActive(options.expectedSessionId))) return;
    if (buffer.duration > 45) {
      console.warn(`[BRASEIRO_AUDIO] arquivo longo demais para SFX (${buffer.duration.toFixed(1)}s): ${name || key}`);
      AudioDiagnostics.log({ action: 'sfx-rejected-duration', channel: 'sfx', level: 'error', message: `Arquivo longo demais para SFX: ${name || key} (${buffer.duration.toFixed(1)} s).`, soundName: name || key, phrase: options.phrase, searchTerms: options.searchTerms, backend: 'WebAudio', result: 'rejeitado' });
      return;
    }
    while (this.activeOneShots.size >= MAX_WEB_SFX_STREAMS) {
      const oldest = this.activeOneShots.values().next().value as ActiveOneShot | undefined;
      if (!oldest) break;
      if (oldest.timer) window.clearTimeout(oldest.timer);
      try { oldest.source.stop(); } catch {}
      this.activeOneShots.delete(oldest);
    }
    const node = this.ctx.createBufferSource();
    const localGain = this.ctx.createGain();
    node.buffer = buffer;
    localGain.gain.value = 1;
    node.connect(localGain);
    localGain.connect(this.channelGains.sfx);
    const active: ActiveOneShot = { source: node, gain: localGain };
    this.activeOneShots.add(active);
    node.onended = () => {
      if (active.timer) window.clearTimeout(active.timer);
      this.activeOneShots.delete(active);
    };
    node.start();
    const actualAtMs = Date.now();
    AudioDiagnostics.log({ action: 'sfx-started', channel: 'sfx', level: 'success', message: `SFX WebAudio iniciado: “${name || (source instanceof Blob ? 'blob' : source.name)}”.`, soundName: name || (source instanceof Blob ? undefined : source.name), soundId: source instanceof Blob ? undefined : source.id, phrase: options.phrase, searchTerms: options.searchTerms, backend: 'WebAudio', scheduledAtMs: options.scheduledAtMs, actualAtMs, latencyMs: options.scheduledAtMs ? Math.max(0, actualAtMs - options.scheduledAtMs) : undefined, volume: this.volumes.sfx, result: 'reproduzindo', details: { durationSeconds: buffer.duration, eventId } });
    if (buffer.duration * 1000 > MAX_SFX_PLAY_MS) {
      active.timer = window.setTimeout(() => {
        if (!this.ctx || !this.activeOneShots.has(active)) return;
        localGain.gain.setTargetAtTime(0, this.ctx.currentTime, 0.15);
        window.setTimeout(() => { try { node.stop(); } catch {} }, 450);
      }, MAX_SFX_PLAY_MS);
    }
    console.debug('[BRASEIRO_AUDIO] SFX:', name || (source instanceof Blob ? 'blob' : source.name));
  }

  public async playPreview(sound: SoundItem): Promise<void> {
    this.beginNarrationSession(`preview:${sound.id}`, 'preview');
    try {
      await this.preloadSounds([sound]);
      await this.playSfx(sound, sound.name, { semanticId: 'preview', eventId: `preview:${sound.id}:${Date.now()}` });
    } finally {
      this.finishNarrationSession('previewed');
    }
  }

  private async decodeVoice(base64Audio: string): Promise<AudioBuffer> {
    await this.ready();
    if (!this.ctx) throw new Error('Mecanismo de voz indisponível.');
    const bytes = decodeBase64ToBytes(base64Audio);
    try {
      return await this.ctx.decodeAudioData(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength));
    } catch {
      const safeLength = bytes.length - (bytes.length % 2);
      const pcm = new Int16Array(bytes.buffer, bytes.byteOffset, safeLength / 2);
      const buffer = this.ctx.createBuffer(1, pcm.length, 24000);
      const channel = buffer.getChannelData(0);
      for (let index = 0; index < pcm.length; index += 1) channel[index] = pcm[index] / 32768;
      return buffer;
    }
  }

  public async playVoice(base64Audio: string, options: VoicePlaybackOptions = {}): Promise<void> {
    const androidFallback = parseAndroidTtsFallback(base64Audio);
    if (androidFallback) {
      const currentGeneration = ++this.voiceGeneration;
      this.clearVoiceCueTimers();
      this.setNarrationDucking(true);
      const cueMap = new Map<string, () => void>();
      const cues = (options.timedCues || []).map((cue, index) => {
        const id = `charon_fallback_${currentGeneration}_${index}`;
        cueMap.set(id, cue.trigger);
        return { id, charIndex: Math.round(androidFallback.text.length * Math.max(0, Math.min(1, cue.relativePosition))) };
      });
      AudioDiagnostics.log({ action: 'charon-android-fallback', channel: 'voice', level: 'warning', message: 'Charon não respondeu; a narração continuará automaticamente com a voz Android.', backend: 'Android TextToSpeech', actualAtMs: Date.now(), result: 'fallback', details: { textLength: androidFallback.text.length, cueCount: cues.length } });
      try {
        await speakWithAndroid(androidFallback.text, {
          cues,
          onStart: () => { if (currentGeneration === this.voiceGeneration) options.onStarted?.(); },
          onCue: id => { if (currentGeneration === this.voiceGeneration) cueMap.get(id)?.(); },
        });
      } finally {
        if (currentGeneration === this.voiceGeneration) this.setNarrationDucking(false);
      }
      return;
    }
    const nativeReference = parseNativeTtsReference(base64Audio);
    if (nativeReference && window.BraseiroAndroid?.playGeneratedVoice) {
      const currentGeneration = ++this.voiceGeneration;
      this.clearVoiceCueTimers();
      this.setNarrationDucking(true);
      let started = false;
      const onNativeStart = (event: Event) => {
        const detail = (event as CustomEvent<{ token?: string; durationMs?: number }>).detail || {};
        if (detail.token !== nativeReference.token || currentGeneration !== this.voiceGeneration || started) return;
        started = true;
        const durationMs = Number(detail.durationMs || nativeReference.durationMs);
        AudioDiagnostics.log({ action: 'voice-started', channel: 'voice', level: 'success', message: 'Voz Gemini/Charon iniciada pelo Android.', backend: 'Android MediaPlayer', actualAtMs: Date.now(), volume: this.volumes.voice, result: 'reproduzindo', details: { durationSeconds: durationMs / 1000, timedCues: options.timedCues?.length || 0, token: nativeReference.token } });
        for (const cue of options.timedCues || []) {
          const atMs = Math.max(0, Math.min(durationMs - 20, durationMs * Math.max(0, Math.min(1, cue.relativePosition))));
          const timer = window.setTimeout(() => {
            this.voiceCueTimers.delete(timer);
            if (currentGeneration === this.voiceGeneration) cue.trigger();
          }, atMs);
          this.voiceCueTimers.add(timer);
        }
        options.onStarted?.();
      };
      window.addEventListener('braseiro-native-gemini-voice-start', onNativeStart as EventListener);
      try {
        await window.BraseiroAndroid.playGeneratedVoice(nativeReference.token, this.volumes.voice);
        AudioDiagnostics.log({ action: 'voice-ended', channel: 'voice', level: 'success', message: 'Voz Gemini/Charon concluída pelo Android.', backend: 'Android MediaPlayer', actualAtMs: Date.now(), result: 'concluído' });
      } finally {
        window.removeEventListener('braseiro-native-gemini-voice-start', onNativeStart as EventListener);
        this.clearVoiceCueTimers();
        if (currentGeneration === this.voiceGeneration) this.setNarrationDucking(false);
      }
      return;
    }
    await this.ready();
    if (!this.ctx || !this.channelGains.voice) return;
    const currentGeneration = ++this.voiceGeneration;
    this.clearVoiceCueTimers();
    if (this.currentVoiceSource) { try { this.currentVoiceSource.stop(); } catch {} }
    const decodeStartedAt = Date.now();
    const buffer = await this.decodeVoice(base64Audio);
    AudioDiagnostics.log({ action: 'voice-decoded', channel: 'voice', level: 'success', message: `Voz decodificada (${buffer.duration.toFixed(2)} s).`, backend: 'WebAudio', actualAtMs: Date.now(), latencyMs: Date.now() - decodeStartedAt, result: 'pronta', details: { durationSeconds: buffer.duration } });
    if (currentGeneration !== this.voiceGeneration) return;
    const source = this.ctx.createBufferSource();
    source.buffer = buffer;
    source.connect(this.channelGains.voice);
    this.currentVoiceSource = source;
    this.setNarrationDucking(true);
    await new Promise<void>(resolve => {
      source.onended = () => {
        this.clearVoiceCueTimers();
        if (this.currentVoiceSource === source) this.currentVoiceSource = null;
        if (currentGeneration === this.voiceGeneration) this.setNarrationDucking(false);
        AudioDiagnostics.log({ action: 'voice-ended', channel: 'voice', level: 'success', message: 'Voz Gemini/Charon concluída.', backend: 'WebAudio', actualAtMs: Date.now(), result: 'concluído' });
        resolve();
      };
      source.start();
      for (const cue of options.timedCues || []) {
        const atMs = Math.max(0, Math.min(buffer.duration * 1000 - 20, buffer.duration * 1000 * Math.max(0, Math.min(1, cue.relativePosition))));
        const timer = window.setTimeout(() => {
          this.voiceCueTimers.delete(timer);
          if (currentGeneration === this.voiceGeneration) cue.trigger();
        }, atMs);
        this.voiceCueTimers.add(timer);
      }
      AudioDiagnostics.log({ action: 'voice-started', channel: 'voice', level: 'success', message: 'Voz Gemini/Charon iniciada.', backend: 'WebAudio', actualAtMs: Date.now(), volume: this.volumes.voice, result: 'reproduzindo', details: { durationSeconds: buffer.duration, timedCues: options.timedCues?.length || 0 } });
      options.onStarted?.();
    });
  }

  private clearVoiceCueTimers() {
    for (const timer of this.voiceCueTimers) window.clearTimeout(timer);
    this.voiceCueTimers.clear();
  }

  public configureBarbaraAudioMix(mix?: Record<string, unknown> | null) {
    const music = Number((mix as any)?.narration_duck_music);
    const ambience = Number((mix as any)?.narration_duck_ambience);
    if (Number.isFinite(music)) this.narrationDuckMusic = Math.max(0, Math.min(1, music));
    if (Number.isFinite(ambience)) this.narrationDuckAmbience = Math.max(0, Math.min(1, ambience));
  }

  public setNarrationDucking(active: boolean) {
    if (this.ctx) {
      this.channelGains.music?.gain.setTargetAtTime(active ? this.volumes.music * this.narrationDuckMusic : this.volumes.music, this.ctx.currentTime, 0.16);
      this.channelGains.ambience?.gain.setTargetAtTime(active ? this.volumes.ambience * this.narrationDuckAmbience : this.volumes.ambience, this.ctx.currentTime, 0.16);
    }
    try {
      window.BraseiroAndroid?.setNativeAudioVolume?.('music', active ? this.volumes.music * this.narrationDuckMusic : this.volumes.music);
      window.BraseiroAndroid?.setNativeAudioVolume?.('ambience', active ? this.volumes.ambience * this.narrationDuckAmbience : this.volumes.ambience);
    } catch {}
  }

  public stopChannel(channel: AudioChannel, slot?: string) {
    AudioDiagnostics.log({ action: 'channel-stop', channel, level: 'info', message: `Canal ${channel}${slot ? ` (${slot})` : ''} interrompido.`, slot, result: 'parado' });
    if (channel === 'voice') {
      this.voiceGeneration += 1;
      this.clearVoiceCueTimers();
      try { window.BraseiroAndroid?.stopSpeech?.(); } catch {}
      try { window.speechSynthesis?.cancel(); } catch {}
      if (this.currentVoiceSource) { try { this.currentVoiceSource.stop(); } catch {} this.currentVoiceSource = null; }
      this.setNarrationDucking(false);
    } else if (channel === 'ambience' || channel === 'weather' || channel === 'music') {
      const keys = new Set<string>([
        ...Array.from(this.activeLoops.keys()),
        ...Array.from(this.nativeLoopNames.keys()),
        ...Array.from(this.loopGenerations.keys()),
      ]);
      for (const key of keys) {
        if (key.startsWith(`${channel}:`) && (!slot || key === `${channel}:${slot}`)) {
          this.loopGenerations.set(key, (this.loopGenerations.get(key) || 0) + 1);
        }
      }
      for (const key of Array.from(this.activeLoops.keys())) {
        if (key.startsWith(`${channel}:`) && (!slot || key === `${channel}:${slot}`)) this.stopWebLoop(key, 450);
      }
      for (const key of Array.from(this.nativeLoopNames.keys())) {
        if (key.startsWith(`${channel}:`) && (!slot || key === `${channel}:${slot}`)) this.nativeLoopNames.delete(key);
      }
    } else {
      this.sfxGeneration += 1;
      for (const active of this.activeOneShots) {
        if (active.timer) window.clearTimeout(active.timer);
        try { active.source.stop(); } catch {}
      }
      this.activeOneShots.clear();
    }
    try { window.BraseiroAndroid?.stopNativeAudio?.(channel, slot); } catch {}
  }

  public stopAll() {
    this.finishNarrationSession('stopped');
    AudioDiagnostics.log({ action: 'stop-all', channel: 'system', level: 'info', message: 'Todos os canais de áudio foram interrompidos.', result: 'parado' });
    this.voiceGeneration += 1;
    this.stopChannel('voice');
    this.stopChannel('ambience');
    this.stopChannel('weather');
    this.stopChannel('music');
    this.stopChannel('sfx');
    try { window.BraseiroAndroid?.stopNativeAudio?.(); } catch {}
    window.dispatchEvent(new Event('braseiro-stop-all'));
  }
}

export const AudioEngine = new AudioEngineImpl();
export default AudioEngine;
