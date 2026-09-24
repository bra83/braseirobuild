from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4934.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

# ---------------------------------------------------------------------------
# BRASEIRO KOKORO = VIRTUAL PROFILE OVER THE DB NEXT-GEN TTS ENGINE
#
# Android app-specific storage is sandboxed. A separate package cannot safely
# open Android/data/<DB package>/files/model on modern Android. Therefore the
# correct architecture is:
#   - DB Next-gen owns/loads model.onnx + voices.bin + tokens + espeak data.
#   - VTT exposes a logical "Braseiro Kokoro PT-BR" profile.
#   - Selecting that profile instantiates Android TextToSpeech using the DB
#     engine package, so synthesis runs inside DB's TTS service and uses the
#     model already installed in DB's own model directory.
# ---------------------------------------------------------------------------

discover_start=main.find("    private fun discoverTtsEngines(): List<Pair<String,String>> {")
discover_end=main.find("\n    private fun isInstalledTtsEnginePackage",discover_start)
if discover_start<0 or discover_end<0:
    raise SystemExit("discoverTtsEngines region missing")

new_discovery=r'''    private val braseiroKokoroProfilePackage = "braseiro.profile.kokoro.ptbr"
    private val legacyBraseiroKokoroPackage = "com.braseiro.kokoro.ptbr"
    private val dbNextGenTtsPackages = listOf(
        "com.k2fsa.sherpa.onnx.tts.engine.debug",
        "com.k2fsa.sherpa.onnx.tts.engine",
    )

    private fun isPhysicalTtsEngineInstalled(pkg:String):Boolean {
        val p=pkg.trim()
        if(p.isBlank()) return true
        if(textToSpeech?.engines.orEmpty().any{it.name==p}) return true
        val serviceMatch=runCatching{
            packageManager.queryIntentServices(android.content.Intent("android.intent.action.TTS_SERVICE"),0)
                .any{it.serviceInfo?.packageName==p}
        }.getOrDefault(false)
        if(serviceMatch) return true
        return runCatching{packageManager.getApplicationInfo(p,0)}.isSuccess
    }

    private fun installedDbNextGenTtsPackage():String? =
        dbNextGenTtsPackages.firstOrNull(::isPhysicalTtsEngineInstalled)

    private fun canonicalTtsProfilePackage(pkg:String):String {
        val p=pkg.trim()
        return if(
            p==legacyBraseiroKokoroPackage ||
            p==braseiroKokoroProfilePackage ||
            p in dbNextGenTtsPackages
        ) braseiroKokoroProfilePackage else p
    }

    private fun backendTtsPackageFor(profile:String):String {
        val p=canonicalTtsProfilePackage(profile)
        return if(p==braseiroKokoroProfilePackage) installedDbNextGenTtsPackage().orEmpty() else p
    }

    private fun discoverTtsEngines(): List<Pair<String,String>> {
        val found = linkedMapOf<String,String>()
        textToSpeech?.engines.orEmpty().forEach { info ->
            val pkg = info.name.orEmpty().trim()
            if (
                pkg.isNotBlank() &&
                pkg !in dbNextGenTtsPackages &&
                pkg != legacyBraseiroKokoroPackage
            ) found[pkg] = info.label.orEmpty().ifBlank { pkg }
        }

        runCatching {
            packageManager.queryIntentServices(
                android.content.Intent("android.intent.action.TTS_SERVICE"),
                0
            )
        }.getOrDefault(emptyList()).forEach { resolve ->
            val service = resolve.serviceInfo ?: return@forEach
            val pkg = service.packageName.orEmpty().trim()
            if (
                pkg.isBlank() ||
                pkg in dbNextGenTtsPackages ||
                pkg == legacyBraseiroKokoroPackage
            ) return@forEach
            val label = runCatching { resolve.loadLabel(packageManager)?.toString().orEmpty() }.getOrDefault("")
            found.putIfAbsent(pkg, label.ifBlank { pkg })
        }

        if(installedDbNextGenTtsPackage()!=null){
            found[braseiroKokoroProfilePackage] = "Braseiro Kokoro PT-BR · neural"
        }

        return found.entries
            .map { it.key to it.value }
            .sortedBy { it.second.lowercase(Locale.ROOT) }
    }
'''
main=main[:discover_start]+new_discovery+main[discover_end:]

# Replace installed check so the virtual profile is valid only when DB is installed.
old_installed=r'''    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested = pkg.trim()
        if (requested.isBlank()) return true
        return discoverTtsEngines().any { it.first == requested }
    }'''
new_installed=r'''    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested = canonicalTtsProfilePackage(pkg)
        if (requested.isBlank()) return true
        if(requested==braseiroKokoroProfilePackage) return installedDbNextGenTtsPackage()!=null
        return isPhysicalTtsEngineInstalled(requested)
    }'''
main=req(main,old_installed,new_installed,"installed profile check")

# Migrate old persisted DB or standalone Braseiro engine selections to virtual profile.
old_saved='''        val saved = ttsPreferences.getString("enginePackage","").orEmpty().trim()
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {'''
new_saved='''        val savedRaw = ttsPreferences.getString("enginePackage","").orEmpty().trim()
        val saved = canonicalTtsProfilePackage(savedRaw)
        if(saved!=savedRaw) ttsPreferences.edit().putString("enginePackage",saved).apply()
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {'''
main=req(main,old_saved,new_saved,"saved engine migration")

# The active label for the virtual profile must never inherit DB·Next-gen Kaldi.
old_label='activeTtsEngineLabel=discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()'
new_label='activeTtsEngineLabel=if(activeTtsEnginePackage==braseiroKokoroProfilePackage)"Braseiro Kokoro PT-BR · neural" else discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()'
main=req(main,old_label,new_label,"virtual profile label")

# Switch logical profile -> physical DB package, preserving the logical profile in prefs/UI.
switch_start=main.find("    private fun switchTtsEngine(pkg:String,callbackId:String?) {")
switch_end=main.find("\n\n    private fun friendlyNeuralVoiceLabel",switch_start)
if switch_end<0:
    # older cumulative source may still have helper immediately after switch
    switch_end=main.find("\n\n    private fun neuralSpeakerId",switch_start)
if switch_start<0 or switch_end<0:
    raise SystemExit("switchTtsEngine region missing")
new_switch=r'''    private fun switchTtsEngine(pkg:String,callbackId:String?) {
        val requested=canonicalTtsProfilePackage(pkg)
        val backend=backendTtsPackageFor(requested)
        val old=textToSpeech

        if(requested==braseiroKokoroProfilePackage && backend.isBlank()){
            deliverTtsConfigCallback(callbackId,"DB·Next-gen Kaldi TTS não está instalado. O Braseiro Kokoro usa o modelo instalado no DB.")
            return
        }
        if(backend.isNotBlank()&&!isPhysicalTtsEngineInstalled(backend)){
            deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: $backend")
            return
        }

        if(currentSpeechRootId!=null)stopNativeSpeech(true)
        runCatching{old?.shutdown()}
        ttsReady=false
        activeTtsEnginePackage=requested
        activeTtsEngineLabel=if(requested==braseiroKokoroProfilePackage)"Braseiro Kokoro PT-BR · neural" else ""
        ttsPreferences.edit().putString("enginePackage",requested).apply()

        val l=TextToSpeech.OnInitListener{s->runOnUiThread{configureInitializedTts(s,callbackId)}}
        textToSpeech=if(backend.isBlank())TextToSpeech(this,l) else TextToSpeech(this,l,backend)
    }'''
main=main[:switch_start]+new_switch+main[switch_end:]

# Make neural identity explicit for the virtual profile and both DB package variants.
old_identity='''        return x.contains("com.k2fsa.sherpa.onnx.tts.engine") || x.contains("sherpa") || x.contains("kokoro")'''
new_identity='''        return x.contains(braseiroKokoroProfilePackage) || x.contains("com.k2fsa.sherpa.onnx.tts.engine") || x.contains("sherpa") || x.contains("kokoro")'''
main=req(main,old_identity,new_identity,"neural identity")

write(main_rel,main)

# Manifest visibility for both release and debug DB package variants.
manifest_rel="android/app/src/main/AndroidManifest.xml"
manifest=read(manifest_rel)
if 'com.k2fsa.sherpa.onnx.tts.engine.debug' not in manifest:
    qend=manifest.find("</queries>")
    if qend<0: raise SystemExit("Manifest queries block missing")
    manifest=manifest[:qend]+'        <package android:name="com.k2fsa.sherpa.onnx.tts.engine.debug" />\n    '+manifest[qend:]
write(manifest_rel,manifest)

# ---------------------------------------------------------------------------
# VERSION 4.9.34
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.34"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.33"','"version": "4.9.34"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"; t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.33'","APP_VERSION = '4.9.34'",t)
t=re.sub(r"APP_VERSION_CODE = 49033","APP_VERSION_CODE = 49034",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"; t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49033","versionCode = 49034",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.33"','versionName = "4.9.34"',t)
write(gradle_rel,t)

main=read(main_rel).replace("index.html?v=49033&immersive=1","index.html?v=49034&immersive=1")
write(main_rel,main)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
main=read(main_rel); manifest=read(manifest_rel)
checks={
 "virtual-profile-id": 'braseiro.profile.kokoro.ptbr' in main,
 "db-release-backend": 'com.k2fsa.sherpa.onnx.tts.engine"' in main,
 "db-debug-backend": 'com.k2fsa.sherpa.onnx.tts.engine.debug' in main,
 "no-direct-cross-app-model-read": 'Android/data/' not in main and 'getExternalFilesDir("model")' not in main,
 "profile-resolves-db-backend": 'backendTtsPackageFor' in main and 'installedDbNextGenTtsPackage' in main,
 "old-db-selection-migrates": 'canonicalTtsProfilePackage(savedRaw)' in main,
 "legacy-braseiro-selection-migrates": 'legacyBraseiroKokoroPackage' in main,
 "ui-hides-physical-db": 'pkg !in dbNextGenTtsPackages' in main,
 "ui-hides-broken-standalone": 'pkg != legacyBraseiroKokoroPackage' in main,
 "ui-profile-label": 'Braseiro Kokoro PT-BR · neural' in main,
 "real-db-voices-preserved": 'e?.voices.orEmpty()' in main and 'e.setVoice(voice)' in main,
 "kokoro-chunk-profile-preserved": 'neuralMinChars = 250' in main and 'neuralTargetChars = 450' in main and 'neuralMaxChars = 650' in main,
 "kokoro-on-done-preserved": 'advanceSpeechQueue(p.rootId)' in main,
 "db-debug-visible-manifest": 'com.k2fsa.sherpa.onnx.tts.engine.debug' in manifest,
 "version": "4.9.34" in read(version_rel) and "49034" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4934_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.34_BRASEIRO_KOKORO_DB_PROFILE_OK")
