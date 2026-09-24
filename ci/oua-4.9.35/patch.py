from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4935.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

start=main.find('    private val braseiroKokoroProfilePackage = "braseiro.profile.kokoro.ptbr"')
end=main.find("\n    private fun isInstalledTtsEnginePackage",start)
if start<0 or end<0:
    raise SystemExit("4.9.34 TTS discovery/profile region missing")

discovery=r'''    private val kokoroBackendPackages = listOf(
        "com.k2fsa.sherpa.onnx.tts.engine.debug",
        "com.k2fsa.sherpa.onnx.tts.engine",
    )
    private val legacyKokoroPackage = "com.braseiro.kokoro.ptbr"

    private fun isKokoroBackendPackage(pkg:String):Boolean =
        pkg.trim() in kokoroBackendPackages

    private fun physicalTtsEngines():LinkedHashMap<String,String> {
        val found=linkedMapOf<String,String>()
        textToSpeech?.engines.orEmpty().forEach{info->
            val pkg=info.name.orEmpty().trim()
            if(pkg.isNotBlank()) found[pkg]=info.label.orEmpty().ifBlank{pkg}
        }
        runCatching{
            packageManager.queryIntentServices(
                android.content.Intent("android.intent.action.TTS_SERVICE"),0
            )
        }.getOrDefault(emptyList()).forEach{resolve->
            val service=resolve.serviceInfo?:return@forEach
            val pkg=service.packageName.orEmpty().trim()
            if(pkg.isBlank()) return@forEach
            val label=runCatching{resolve.loadLabel(packageManager)?.toString().orEmpty()}.getOrDefault("")
            found.putIfAbsent(pkg,label.ifBlank{pkg})
        }
        kokoroBackendPackages.forEach{pkg->
            runCatching{packageManager.getApplicationInfo(pkg,0)}.getOrNull()?.let{app->
                val label=runCatching{packageManager.getApplicationLabel(app).toString()}.getOrDefault("")
                found.putIfAbsent(pkg,label.ifBlank{pkg})
            }
        }
        return found
    }

    private fun preferredInstalledKokoroBackend(hint:String?=null):String? {
        val installed=physicalTtsEngines().keys.filter(::isKokoroBackendPackage)
        val h=hint.orEmpty().trim()
        if(h in installed) return h
        val active=activeTtsEnginePackage.trim()
        if(active in installed) return active
        return kokoroBackendPackages.firstOrNull{it in installed}
    }

    private fun migratePersistedEnginePackage(raw:String):String {
        val p=raw.trim()
        if(p.isBlank()) return ""
        if(p==legacyKokoroPackage) return preferredInstalledKokoroBackend().orEmpty()
        if(isKokoroBackendPackage(p)) return preferredInstalledKokoroBackend(p).orEmpty()
        return p
    }

    private fun discoverTtsEngines(): List<Pair<String,String>> {
        val physical=physicalTtsEngines()
        val found=linkedMapOf<String,String>()

        physical.forEach{(pkg,label)->
            if(pkg==legacyKokoroPackage || isKokoroBackendPackage(pkg)) return@forEach
            found[pkg]=label
        }

        preferredInstalledKokoroBackend()?.let{pkg->
            found[pkg]="Braseiro Kokoro PT-BR · neural"
        }

        return found.entries
            .map{it.key to it.value}
            .sortedBy{it.second.lowercase(Locale.ROOT)}
    }
'''
main=main[:start]+discovery+main[end:]

old_installed=r'''    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested = canonicalTtsProfilePackage(pkg)
        if (requested.isBlank()) return true
        if(requested==braseiroKokoroProfilePackage) return installedDbNextGenTtsPackage()!=null
        return isPhysicalTtsEngineInstalled(requested)
    }'''
new_installed=r'''    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested=migratePersistedEnginePackage(pkg)
        if(requested.isBlank()) return pkg.isBlank()
        return physicalTtsEngines().containsKey(requested)
    }'''
main=req(main,old_installed,new_installed,"real installed-package check")

old_saved='''        val savedRaw = ttsPreferences.getString("enginePackage","").orEmpty().trim()
        val saved = canonicalTtsProfilePackage(savedRaw)
        if(saved!=savedRaw) ttsPreferences.edit().putString("enginePackage",saved).apply()
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {'''
new_saved='''        val savedRaw=ttsPreferences.getString("enginePackage","").orEmpty().trim()
        val saved=migratePersistedEnginePackage(savedRaw)
        if(saved!=savedRaw) {
            if(saved.isBlank()) ttsPreferences.edit().remove("enginePackage").apply()
            else ttsPreferences.edit().putString("enginePackage",saved).apply()
        }
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {'''
main=req(main,old_saved,new_saved,"engine migration to real package")

old_label='activeTtsEngineLabel=if(activeTtsEnginePackage==braseiroKokoroProfilePackage)"Braseiro Kokoro PT-BR · neural" else discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()'
new_label='activeTtsEngineLabel=if(isKokoroBackendPackage(activeTtsEnginePackage))"Braseiro Kokoro PT-BR · neural" else discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()'
main=req(main,old_label,new_label,"Braseiro display label over real package")

switch_start=main.find("    private fun switchTtsEngine(pkg:String,callbackId:String?) {")
switch_end=main.find("\n\n    private fun friendlyNeuralVoiceLabel",switch_start)
if switch_start<0 or switch_end<0:
    raise SystemExit("switchTtsEngine region missing")
new_switch=r'''    private fun switchTtsEngine(pkg:String,callbackId:String?) {
        val requested=when{
            pkg.trim()==legacyKokoroPackage -> preferredInstalledKokoroBackend().orEmpty()
            isKokoroBackendPackage(pkg) -> preferredInstalledKokoroBackend(pkg).orEmpty()
            else -> pkg.trim()
        }
        if(pkg.trim().isNotBlank() && requested.isBlank()){
            deliverTtsConfigCallback(callbackId,"Braseiro Kokoro PT-BR não está instalado.")
            return
        }
        if(requested.isNotBlank()&&!physicalTtsEngines().containsKey(requested)){
            deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: $requested")
            return
        }

        if(requested==activeTtsEnginePackage && textToSpeech!=null){
            ttsPreferences.edit().putString("enginePackage",requested).apply()
            deliverTtsConfigCallback(callbackId,null)
            return
        }

        if(currentSpeechRootId!=null)stopNativeSpeech(true)
        val old=textToSpeech
        runCatching{old?.shutdown()}
        ttsReady=false
        activeTtsEnginePackage=requested
        activeTtsEngineLabel=if(isKokoroBackendPackage(requested))"Braseiro Kokoro PT-BR · neural" else ""
        if(requested.isBlank()) ttsPreferences.edit().remove("enginePackage").apply()
        else ttsPreferences.edit().putString("enginePackage",requested).apply()

        val listener=TextToSpeech.OnInitListener{s->
            runOnUiThread{configureInitializedTts(s,callbackId)}
        }
        textToSpeech=if(requested.isBlank()) TextToSpeech(this,listener)
        else TextToSpeech(this,listener,requested)
    }'''
new_switch=new_switch.replace("$","$")
main=main[:switch_start]+new_switch+main[switch_end:]

old_identity='''        return x.contains(braseiroKokoroProfilePackage) || x.contains("com.k2fsa.sherpa.onnx.tts.engine") || x.contains("sherpa") || x.contains("kokoro")'''
new_identity='''        return isKokoroBackendPackage(pkg)'''
main=req(main,old_identity,new_identity,"exact neural backend identity")

voice_start=main.find("    private fun friendlyNeuralVoiceLabel")
voice_end=main.find("\n\n    private fun parseSpeechCues",voice_start)
if voice_start<0 or voice_end<0:
    raise SystemExit("voice catalog region missing")

voice_region=r'''    private fun ttsCatalogJson():String {
        val e=textToSpeech
        val engines=JSONArray()
        engines.put(JSONObject()
            .put("packageName","")
            .put("label","Padrão do Android")
            .put("selected",activeTtsEnginePackage.isBlank())
            .put("neuralProfile",false))

        discoverTtsEngines().forEach{(pkg,label)->
            engines.put(JSONObject()
                .put("packageName",pkg)
                .put("label",label)
                .put("selected",pkg==activeTtsEnginePackage)
                .put("neuralProfile",isKokoroBackendPackage(pkg)))
        }

        val voices=JSONArray()
        runCatching{e?.voices.orEmpty()}.getOrDefault(emptySet())
            .filter{it.name.isNotBlank() && !it.name.equals("NOT_SET_VOICE",true)}
            .sortedWith(compareBy({it.locale?.toLanguageTag().orEmpty()},{it.name}))
            .forEach{v->
                val locale=v.locale?.toLanguageTag().orEmpty()
                val label=buildString{
                    append(v.name)
                    if(locale.isNotBlank()) append(" · ").append(locale)
                    append(if(v.isNetworkConnectionRequired)" · rede" else " · local")
                }
                voices.put(JSONObject()
                    .put("name",v.name)
                    .put("label",label)
                    .put("locale",locale)
                    .put("quality",v.quality)
                    .put("latency",v.latency)
                    .put("networkRequired",v.isNetworkConnectionRequired)
                    .put("selected",v.name==selectedTtsVoiceName))
            }

        return JSONObject()
            .put("ready",ttsReady)
            .put("selectedEnginePackage",activeTtsEnginePackage)
            .put("selectedEngineLabel",activeTtsEngineLabel.ifBlank{
                if(activeTtsEnginePackage.isBlank())"Padrão do Android" else activeTtsEnginePackage
            })
            .put("selectedVoice",selectedTtsVoiceName)
            .put("neuralProfile",isNeuralTtsProfile())
            .put("neuralRate",neuralTtsRate)
            .put("engines",engines)
            .put("voices",voices)
            .put("profile",JSONObject()
                .put("name",if(isNeuralTtsProfile())"Braseiro Kokoro PT-BR · neural" else "Android TTS")
                .put("minChars",neuralMinChars)
                .put("targetChars",neuralTargetChars)
                .put("maxChars",neuralMaxChars))
            .toString()
    }

    private fun deliverTtsConfigCallback(id:String?,error:String?){
        if(id.isNullOrBlank())return
        val q=JSONObject.quote(id)
        if(error==null){
            val payload=ttsCatalogJson()
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.resolve?.($payload); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }else{
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.reject?.(new Error(${JSONObject.quote(error)})); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }
    }

    private fun selectTtsVoiceInternal(name:String):String {
        val e=textToSpeech ?: throw IllegalStateException("O motor TTS ainda não foi inicializado.")
        val requested=name.trim()
        val key=activeTtsEnginePackage.ifBlank{"__android_default__"}

        if(requested.isBlank()){
            selectedTtsVoiceName=""
            ttsPreferences.edit().remove("voice.$key").apply()
            return ttsCatalogJson()
        }

        val voice=e.voices.orEmpty().firstOrNull{it.name==requested}
            ?: throw IllegalArgumentException("Voz TTS real não disponível no motor selecionado: $requested")
        if(e.setVoice(voice)==TextToSpeech.ERROR){
            throw IllegalStateException("O motor recusou a voz Android ${voice.name}.")
        }

        selectedTtsVoiceName=voice.name
        ttsPreferences.edit().putString("voice.$key",voice.name).apply()
        return ttsCatalogJson()
    }

    private fun setNeuralTtsRateInternal(rate:Float):String {
        neuralTtsRate=rate.coerceIn(.65f,1.5f)
        ttsPreferences.edit().putFloat("neuralRate",neuralTtsRate).apply()
        return ttsCatalogJson()
    }

    private fun neuralParams(volume:Float)=Bundle().apply{
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume.coerceIn(0f,1f))
    }

    private fun prewarmNeuralTts(force:Boolean=false):Boolean =
        ttsReady && isNeuralTtsProfile()
'''
voice_region=voice_region.replace("$","$")
main=main[:voice_start]+voice_region+main[voice_end:]

old_init='''        val savedVoice=ttsPreferences.getString("voice.$key","").orEmpty()
        selectedTtsVoiceName=resolveLegacyNeuralVoice(e,savedVoice)
        if(selectedTtsVoiceName.isNotBlank()){
            e.voices.orEmpty().firstOrNull{it.name==selectedTtsVoiceName}?.let{voice->
                if(e.setVoice(voice)!=TextToSpeech.ERROR && savedVoice!=selectedTtsVoiceName){
                    ttsPreferences.edit().putString("voice.$key",selectedTtsVoiceName).apply()
                }
            }
        }
        installTtsListener(e)'''
new_init='''        val savedVoice=ttsPreferences.getString("voice.$key","").orEmpty()
        val persistedVoice=e.voices.orEmpty().firstOrNull{it.name==savedVoice}
        selectedTtsVoiceName=""
        if(persistedVoice!=null && e.setVoice(persistedVoice)!=TextToSpeech.ERROR){
            selectedTtsVoiceName=persistedVoice.name
        }else if(savedVoice.isNotBlank()){
            ttsPreferences.edit().remove("voice.$key").apply()
        }
        installTtsListener(e)'''
main=req(main,old_init,new_init,"real Voice.name persistence restore")

main=re.sub(r'\s*private val legacyNeuralSpeakerIds = mapOf\([^\n]+\)\n','\n',main)
write(main_rel,main)

manifest_rel="android/app/src/main/AndroidManifest.xml"
manifest=read(manifest_rel)
manifest=re.sub(r'\s*<package android:name="com\.braseiro\.kokoro\.ptbr"\s*/>\s*','\n',manifest)
qend=manifest.find("</queries>")
if qend<0: raise SystemExit("Manifest queries block missing")
for pkg in ["com.k2fsa.sherpa.onnx.tts.engine.debug","com.k2fsa.sherpa.onnx.tts.engine"]:
    if f'android:name="{pkg}"' not in manifest:
        manifest=manifest[:qend]+f'        <package android:name="{pkg}" />\n    '+manifest[qend:]
        qend=manifest.find("</queries>")
write(manifest_rel,manifest)

settings_rel="components/SettingsView.tsx"
settings=read(settings_rel)
settings=settings.replace(
    "Motores e vozes vêm do Android. Samsung/Google mantêm o fluxo padrão; Sherpa/Kokoro usa perfil neural local.",
    "Motores e vozes vêm do Android. Braseiro Kokoro PT-BR usa o motor Sherpa/DB Next-gen instalado e suas vozes reais; Samsung/Google seguem o fluxo Android normal."
)
write(settings_rel,settings)

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.35"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.34"','"version": "4.9.35"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.34'","APP_VERSION = '4.9.35'",t)
t=re.sub(r"APP_VERSION_CODE = 49034","APP_VERSION_CODE = 49035",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49034","versionCode = 49035",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.34"','versionName = "4.9.35"',t)
write(gradle_rel,t)

main=read(main_rel).replace("index.html?v=49034&immersive=1","index.html?v=49035&immersive=1")
write(main_rel,main)

main=read(main_rel); manifest=read(manifest_rel); app=read("App.tsx")
checks={
 "1-single-visible-kokoro": 'found[pkg]="Braseiro Kokoro PT-BR · neural"' in main and 'pkg==legacyKokoroPackage || isKokoroBackendPackage(pkg)' in main,
 "1-exact-supported-packages": 'com.k2fsa.sherpa.onnx.tts.engine.debug' in main and 'com.k2fsa.sherpa.onnx.tts.engine"' in main,
 "1-old-package-not-backend": 'com.braseiro.kokoro.ptbr' not in manifest and 'TextToSpeech(this,listener,legacyKokoroPackage)' not in main,
 "2-real-voices-only": 'e?.voices.orEmpty()' in main and 'e.voices.orEmpty().firstOrNull{it.name==requested}' in main,
 "2-no-fabricated-voice-aliases": 'pf_dora' not in main and 'pm_alex' not in main and 'pm_santa' not in main and 'speakerId' not in main,
 "3-package-plus-voice-persistence": 'putString("voice.$key",voice.name)' in main and 'val key=activeTtsEnginePackage.ifBlank' in main,
 "3-restore-real-voice-object": 'persistedVoice!=null && e.setVoice(persistedVoice)!=TextToSpeech.ERROR' in main,
 "4-async-safe-select-bridge": 'selectTtsVoice(voiceName: String, requestId: String)' in main and 'runOnUiThread' in main,
 "4-real-setVoice": 'e.setVoice(voice)==TextToSpeech.ERROR' in main,
 "5-natural-kokoro-chunks": 'neuralMinChars = 250' in main and 'neuralTargetChars = 450' in main and 'neuralMaxChars = 650' in main,
 "5-punctuation-priority": "setOf('.','!','?')" in main and "setOf(';',',')" in main,
 "5-first-flush-later-add": 'if(neural&&p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH' in main,
 "5-real-onDone-chain": 'override fun onDone' in main and 'advanceSpeechQueue(p.rootId)' in main,
 "5-no-neural-watchdog": 'private fun scheduleSpeechWatchdog(part:SpeechPart){if(isNeuralTtsProfile())return;' in main,
 "5-no-silent-prewarm": 'playSilentUtterance' not in main and 'e.speak("Olá."' not in main,
 "5-single-rate-application": 'if(isNeuralTtsProfile()) runCatching{textToSpeech?.setSpeechRate' not in main and 'textToSpeech!!.setSpeechRate(speechRate)' in main,
 "6-normal-android-path": 'NarrationSegmenter.splitForAndroidTts(chapter,180.coerceAtMost(max))' in main,
 "6-neural-exact-package-only": 'return isKokoroBackendPackage(pkg)' in main,
 "no-cross-app-model-access": 'Android/data/' not in main and 'model.onnx' not in main and 'voices.bin' not in main,
 "music-fix-preserved": "scene-music-preserved-manual" in app,
 "route-lock-preserved": "Movimento livre bloqueado: selecione um destino e use Planejar rota" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.35" in read(version_rel) and "49035" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4935_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.35_TTS_ANDROID_KOKORO_CURRENT_ARCH_OK")
