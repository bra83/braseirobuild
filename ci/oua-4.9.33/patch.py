from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4933.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# 1) MANUAL MUSIC MUST SURVIVE SCENE CHANGES
# 4.9.32 fixed long-track playback, but processTurn still called stopAll() for
# every new fictional scene. That killed the user's manually chosen library
# music (slot "library-preview"). Scene transitions now clear only scene-owned
# slots and narration/SFX. User-selected music remains until the user changes
# or stops it.
# ---------------------------------------------------------------------------
app_rel="App.tsx"
app=read(app_rel)

old_scene="""        cancelNarrationPlayback();
        AudioEngine.stopAll();
        setAudioActive(false);"""
new_scene="""        cancelNarrationPlayback();
        // New scenes replace only scene-owned sound beds. A track selected
        // manually in the sound library uses slot "library-preview" and must
        // survive scene transitions until the user stops or replaces it.
        AudioEngine.stopChannel('ambience', 'base');
        AudioEngine.stopChannel('ambience', 'distant');
        AudioEngine.stopChannel('ambience', 'detail0');
        AudioEngine.stopChannel('ambience', 'detail1');
        AudioEngine.stopChannel('weather', 'base');
        AudioEngine.stopChannel('music', 'base');
        AudioEngine.stopChannel('sfx');
        setAudioActive(AudioEngine.isLoopActive('music', 'library-preview'));"""
app=req(app,old_scene,new_scene,"scene transition stopAll")

# Scene-generated music must not play on top of manually pinned music.
old_music="""    if (prepared.music) starts.push(AudioEngine.playLoop('music', prepared.music, undefined, 'base'));
    else AudioEngine.stopChannel('music', 'base');"""
new_music="""    const manualMusicPinned = AudioEngine.isLoopActive('music', 'library-preview');
    if (manualMusicPinned) {
      AudioEngine.stopChannel('music', 'base');
      AudioDiagnostics.log({
        action: 'scene-music-preserved-manual',
        channel: 'music',
        level: 'info',
        message: 'Música escolhida manualmente foi preservada durante a mudança de cena.',
        slot: 'library-preview',
        result: 'preservada',
      });
    } else if (prepared.music) starts.push(AudioEngine.playLoop('music', prepared.music, undefined, 'base'));
    else AudioEngine.stopChannel('music', 'base');"""
app=req(app,old_music,new_music,"scene music respects manual pin")
write(app_rel,app)

# ---------------------------------------------------------------------------
# 2) AUDIO ENGINE: expose slot activity for persistence policy.
# ---------------------------------------------------------------------------
engine_rel="lib/AudioEngine.ts"
eng=read(engine_rel)
anchor="""  public stopChannel(channel: AudioChannel, slot?: string) {"""
method="""  public isLoopActive(channel: LoopChannel, slot = 'base'): boolean {
    const key = `${channel}:${slot}`;
    return this.activeLoops.has(key) || this.nativeLoopNames.has(key);
  }

"""
if "public isLoopActive(" not in eng:
    eng=req(eng,anchor,method+anchor,"isLoopActive insert")
write(engine_rel,eng)

# ---------------------------------------------------------------------------
# 3) Explicit visibility + identity for the dedicated Braseiro Kokoro V4
# engine package. Generic TTS_SERVICE discovery remains as fallback.
# ---------------------------------------------------------------------------
manifest_rel="android/app/src/main/AndroidManifest.xml"
manifest=read(manifest_rel)
if "com.braseiro.kokoro.ptbr" not in manifest:
    qend=manifest.find("</queries>")
    if qend<0: raise SystemExit("Manifest queries block missing")
    manifest=manifest[:qend]+'        <package android:name="com.braseiro.kokoro.ptbr" />\n    '+manifest[qend:]
write(manifest_rel,manifest)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

# Merge dedicated Braseiro package into robust discovery even on OEMs whose
# queryIntentServices is incomplete.
known_anchor='''        val sherpaPackage = "com.k2fsa.sherpa.onnx.tts.engine"
        runCatching { packageManager.getApplicationInfo(sherpaPackage, 0) }.getOrNull()?.let { app ->
            val label = runCatching { packageManager.getApplicationLabel(app).toString() }.getOrDefault("")
            found.putIfAbsent(sherpaPackage, label.ifBlank { "SherpaOnnx TTS Engine" })
        }'''
known_new=known_anchor+'''

        val braseiroKokoroPackage = "com.braseiro.kokoro.ptbr"
        runCatching { packageManager.getApplicationInfo(braseiroKokoroPackage, 0) }.getOrNull()?.let { app ->
            val label = runCatching { packageManager.getApplicationLabel(app).toString() }.getOrDefault("")
            found.putIfAbsent(braseiroKokoroPackage, label.ifBlank { "Braseiro Kokoro PT-BR V4" })
        }'''
if "val braseiroKokoroPackage" not in main:
    main=req(main,known_anchor,known_new,"Braseiro Kokoro package discovery")
write(main_rel,main)

# ---------------------------------------------------------------------------
# VERSION 4.9.33
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.33"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.32"','"version": "4.9.33"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"; t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.32'","APP_VERSION = '4.9.33'",t)
t=re.sub(r"APP_VERSION_CODE = 49032","APP_VERSION_CODE = 49033",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"; t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49032","versionCode = 49033",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.32"','versionName = "4.9.33"',t)
write(gradle_rel,t)

main=read(main_rel).replace("index.html?v=49032&immersive=1","index.html?v=49033&immersive=1")
write(main_rel,main)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
app=read(app_rel); eng=read(engine_rel); main=read(main_rel); manifest=read(manifest_rel)
checks={
 "scene-no-stopall": "AudioEngine.stopAll();" not in app[app.find("Música escolhida manualmente foi preservada")-1500:app.find("Música escolhida manualmente foi preservada")+1500],
 "manual-music-slot-preserved": "isLoopActive('music', 'library-preview')" in app,
 "scene-base-music-cleared-only": "AudioEngine.stopChannel('music', 'base')" in app,
 "scene-music-does-not-overlay-manual": "scene-music-preserved-manual" in app,
 "audio-engine-slot-query": "public isLoopActive(channel: LoopChannel" in eng,
 "long-track-fix-preserved": "library-preview" in eng and "durationMs >= 30_000 || sizeBytes >= 4 * 1024 * 1024" in eng,
 "braseiro-kokoro-query": 'com.braseiro.kokoro.ptbr' in manifest,
 "braseiro-kokoro-discovery": 'val braseiroKokoroPackage = "com.braseiro.kokoro.ptbr"' in main,
 "tts-real-voice-fix-preserved": "selectTtsVoiceInternal" in main and "resolveLegacyNeuralVoice" in main,
 "route-lock-preserved": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.33" in read(version_rel) and "49033" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4933_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.33_PERSISTENT_MANUAL_MUSIC_OK")
