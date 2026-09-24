from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4932.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# AUDIO LIBRARY FIX
# Symptom on Android: long local tracks (for example soundtrack files imported
# as SFX by filename metadata) played ~1-2 seconds and stopped.
#
# Root cause:
#   1) SoundLibraryView sent EVERY preview through AudioEngine.playPreview().
#   2) playPreview() sent EVERY file through the SFX path.
#   3) Native SFX prefers SoundPool. SoundPool is only suitable for short,
#      memory-resident effects and truncates/fails on large soundtrack files.
#
# Fix:
#   - long/manual music previews use the loop MediaPlayer path, never SFX;
#   - music/ambience/weather metadata also routes to its real loop channel;
#   - SoundPool is hard-limited to genuinely short/small SFX;
#   - tapping the same library item toggles stop; selecting another replaces it.
# ---------------------------------------------------------------------------

engine_rel="lib/AudioEngine.ts"
e=read(engine_rel)

old_preview="""  public async playPreview(sound: SoundItem): Promise<void> {
    this.beginNarrationSession(`preview:${sound.id}`, 'preview');
    try {
      await this.preloadSounds([sound]);
      await this.playSfx(sound, sound.name, { semanticId: 'preview', eventId: `preview:${sound.id}:${Date.now()}` });
    } finally {
      this.finishNarrationSession('previewed');
    }
  }"""
new_preview="""  private previewPlaybackChannel(sound: SoundItem): 'music' | 'ambience' | 'weather' | 'sfx' {
    const catalog = String(sound.catalogChannel || '').toUpperCase();
    const declared = String(sound.channel || '').toLowerCase();
    const durationMs = Math.max(0, Number(sound.durationMs || 0));
    const sizeBytes = Math.max(0, Number(sound.size || 0));
    if (catalog === 'MUSIC' || declared === 'music' || sound.category === 'MÚSICA') return 'music';
    if (catalog === 'AMBIENCE_BASE' || declared === 'ambience') return 'ambience';
    if (catalog === 'WEATHER' || declared === 'weather') return 'weather';
    // Defensive recovery for soundtrack/ambience files that the legacy
    // filename classifier labelled SFX. Long/large audio must never enter
    // SoundPool or the short-SFX duration guard.
    if (durationMs >= 30_000 || sizeBytes >= 4 * 1024 * 1024) return 'music';
    return 'sfx';
  }

  public async playPreview(sound: SoundItem): Promise<void> {
    const channel = this.previewPlaybackChannel(sound);
    if (channel !== 'sfx') {
      await this.playLoop(channel, sound, sound.name, 'library-preview');
      AudioDiagnostics.log({
        action: 'library-long-preview-started',
        channel,
        level: 'success',
        message: `Biblioteca reproduzindo “${sound.name}” pelo player contínuo.`,
        soundName: sound.name,
        soundId: sound.nativeId || sound.id,
        backend: sound.nativeId ? 'Android MediaPlayer' : 'WebAudio',
        slot: 'library-preview',
        result: 'reproduzindo',
        details: { durationMs: sound.durationMs || 0, sizeBytes: sound.size || 0, originalChannel: sound.channel, catalogChannel: sound.catalogChannel },
      });
      return;
    }
    this.beginNarrationSession(`preview:${sound.id}`, 'preview');
    try {
      await this.preloadSounds([sound]);
      await this.playSfx(sound, sound.name, { semanticId: 'preview', eventId: `preview:${sound.id}:${Date.now()}` });
    } finally {
      this.finishNarrationSession('previewed');
    }
  }"""
e=req(e,old_preview,new_preview,"AudioEngine playPreview")
write(engine_rel,e)

view_rel="components/SoundLibraryView.tsx"
v=read(view_rel)

anchor="""  const setVolume = (channel: AudioChannel, value: number) => {
    AudioEngine.setVolume(channel, value); setVolumes(previous => ({ ...previous, [channel]: value })); onAudioChange();
  };

  const preview = async (sound: SoundItem) => {"""
helper="""  const setVolume = (channel: AudioChannel, value: number) => {
    AudioEngine.setVolume(channel, value); setVolumes(previous => ({ ...previous, [channel]: value })); onAudioChange();
  };

  const previewPlaybackChannel = (sound: SoundItem): AudioChannel => {
    const catalog = String(sound.catalogChannel || '').toUpperCase();
    const declared = String(sound.channel || '').toLowerCase();
    const durationMs = Math.max(0, Number(sound.durationMs || 0));
    const sizeBytes = Math.max(0, Number(sound.size || 0));
    if (catalog === 'MUSIC' || declared === 'music' || sound.category === 'MÚSICA') return 'music';
    if (catalog === 'AMBIENCE_BASE' || declared === 'ambience') return 'ambience';
    if (catalog === 'WEATHER' || declared === 'weather') return 'weather';
    if (durationMs >= 30_000 || sizeBytes >= 4 * 1024 * 1024) return 'music';
    return 'sfx';
  };

  const preview = async (sound: SoundItem) => {"""
v=req(v,anchor,helper,"SoundLibrary preview helper")

old_fn="""  const preview = async (sound: SoundItem) => {
    AudioEngine.stopChannel('sfx');
    try { setPreviewingId(sound.id); await AudioEngine.playPreview(sound); onAudioChange(); }
    catch (error) { setStatus(error instanceof Error ? error.message : 'Não foi possível reproduzir o arquivo.'); }
    finally { window.setTimeout(() => setPreviewingId(current => current === sound.id ? null : current), 1200); }
  };"""
new_fn="""  const preview = async (sound: SoundItem) => {
    const channel = previewPlaybackChannel(sound);
    if (previewingId === sound.id) {
      if (channel === 'sfx') AudioEngine.stopChannel('sfx');
      else AudioEngine.stopChannel(channel, 'library-preview');
      setPreviewingId(null);
      setStatus('Reprodução interrompida.');
      return;
    }

    // Only stop the library audition slot. Do not kill unrelated narration.
    AudioEngine.stopChannel('music', 'library-preview');
    AudioEngine.stopChannel('ambience', 'library-preview');
    AudioEngine.stopChannel('weather', 'library-preview');
    AudioEngine.stopChannel('sfx');

    try {
      setPreviewingId(sound.id);
      await AudioEngine.playPreview(sound);
      if (channel === 'sfx') {
        const visibleMs = Math.max(1200, Math.min(15_000, Number(sound.durationMs || 2500) + 250));
        window.setTimeout(() => setPreviewingId(current => current === sound.id ? null : current), visibleMs);
      } else {
        const wasRecovered = String(sound.channel || '').toLowerCase() === 'sfx' || String(sound.catalogChannel || '').toUpperCase() === 'SFX';
        setStatus(wasRecovered
          ? `“${sound.name}” estava catalogado como SFX, mas é áudio longo. Reproduzindo pelo player de música sem corte.`
          : `Reproduzindo “${sound.name}”. Toque novamente para parar.`);
      }
      onAudioChange();
    } catch (error) {
      setPreviewingId(null);
      setStatus(error instanceof Error ? error.message : 'Não foi possível reproduzir o arquivo.');
    }
  };"""
v=req(v,old_fn,new_fn,"SoundLibrary preview behavior")

# Reset the square/play state when global "Parar tudo" is used.
effect_anchor="""  useEffect(() => {
    void loadSounds();"""
if "braseiro-stop-all" not in v:
    insert="""  useEffect(() => {
    const stopped = () => setPreviewingId(null);
    window.addEventListener('braseiro-stop-all', stopped);
    return () => window.removeEventListener('braseiro-stop-all', stopped);
  }, []);

"""
    v=req(v,effect_anchor,insert+effect_anchor,"stop-all preview state")

write(view_rel,v)

# Native safeguard: SoundPool only for truly short/small effects.
main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
m=read(main_rel)

old_preload="""    private fun preloadNativeSoundIds(ids: List<String>): List<Int> {
        val samples = mutableListOf<Int>()
        for (id in ids.distinct()) {
            val existing = soundPoolSamples[id]
            if (existing != null) { samples += existing; continue }
            val found = findAudio(id) ?: continue
            try {
                val sampleId = soundPool.load(found.second.absolutePath, 1)
                if (sampleId > 0) { soundPoolSamples[id] = sampleId; samples += sampleId }
            } catch (error: Exception) {
                Log.w("BRASEIRO_AUDIO", "preload failed $id", error)
            }
        }
        return samples
    }"""
new_preload="""    private fun preloadNativeSoundIds(ids: List<String>): List<Int> {
        val samples = mutableListOf<Int>()
        for (id in ids.distinct()) {
            val existing = soundPoolSamples[id]
            if (existing != null) { samples += existing; continue }
            val found = findAudio(id) ?: continue
            val durationMs = found.first.optLong("durationMs", 0L)
            val fileBytes = found.second.length()
            // SoundPool is deliberately restricted to small one-shot effects.
            // Large soundtrack files can decode only partially and sound like
            // they stop after ~1-2 seconds. Those must use MediaPlayer.
            if (durationMs <= 0L || durationMs > 6_000L || fileBytes > 1_000_000L) continue
            try {
                val sampleId = soundPool.load(found.second.absolutePath, 1)
                if (sampleId > 0) { soundPoolSamples[id] = sampleId; samples += sampleId }
            } catch (error: Exception) {
                Log.w("BRASEIRO_AUDIO", "preload failed $id", error)
            }
        }
        return samples
    }"""
m=req(m,old_preload,new_preload,"SoundPool preload guard")

old_branch="""        if (channel == "sfx" && !loop) {
            val sampleId = soundPoolSamples[id]
            if (sampleId != null && loadedSoundPoolSamples.contains(sampleId)) {"""
new_branch="""        val soundPoolEligible = channel == "sfx" && !loop &&
            knownDurationMs in 1L..6_000L && found.second.length() <= 1_000_000L
        if (soundPoolEligible) {
            val sampleId = soundPoolSamples[id]
            if (sampleId != null && loadedSoundPoolSamples.contains(sampleId)) {"""
m=req(m,old_branch,new_branch,"SoundPool runtime guard")

# Keep preload fallback only for eligible files (the surrounding branch is now guarded).
write(main_rel,m)

# ---------------------------------------------------------------------------
# VERSION 4.9.32
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.32"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.31"','"version": "4.9.32"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.31'","APP_VERSION = '4.9.32'",t)
t=re.sub(r"APP_VERSION_CODE = 49031","APP_VERSION_CODE = 49032",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49031","versionCode = 49032",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.31"','versionName = "4.9.32"',t)
write(gradle_rel,t)

m=read(main_rel).replace("index.html?v=49031&immersive=1","index.html?v=49032&immersive=1")
write(main_rel,m)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
e=read(engine_rel); v=read(view_rel); m=read(main_rel); app=read("App.tsx")
checks={
 "preview-long-audio-routes-music": "durationMs >= 30_000 || sizeBytes >= 4 * 1024 * 1024" in e,
 "preview-loop-player": "playLoop(channel, sound, sound.name, 'library-preview')" in e,
 "preview-no-universal-sfx": "const channel = this.previewPlaybackChannel(sound);" in e,
 "library-toggle-stop": "if (previewingId === sound.id)" in v and "Toque novamente para parar" in v,
 "library-recovery-status": "estava catalogado como SFX, mas é áudio longo" in v,
 "soundpool-duration-guard": 'knownDurationMs in 1L..6_000L' in m,
 "soundpool-size-guard": 'found.second.length() <= 1_000_000L' in m,
 "soundpool-preload-guard": 'durationMs > 6_000L || fileBytes > 1_000_000L' in m,
 "diagnostic-test-kept": "Testar música" in read("components/AudioDiagnosticsPanel.tsx"),
 "tts-real-voice-fix-preserved": "resolveLegacyNeuralVoice" in m and "selectTtsVoiceInternal" in m,
 "route-lock-preserved": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.32" in read(version_rel) and "49032" in read(gradle_rel),
}
for k,vv in checks.items(): print(("PASS" if vv else "FAIL"),k)
bad=[k for k,vv in checks.items() if not vv]
if bad: raise SystemExit("PATCH_4932_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.32_AUDIO_LIBRARY_LONG_TRACK_FIX_OK")
