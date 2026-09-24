from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4931.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

# ---------------------------------------------------------------------------
# KOKORO VOICE FIX
# SherpaOnnx exposes Android Voice.name as <modelId>_<speakerId>.
# Previous Braseiro builds fabricated pf_dora/pm_alex/pm_santa as if they were
# Android Voice names. jing332 ignores arbitrary Bundle speakerId parameters,
# so the selector looked right but did not actually select the engine voice.
# ---------------------------------------------------------------------------

old_map='private val neuralSpeakerIds = mapOf("pf_dora" to 42, "pm_alex" to 43, "pm_santa" to 44)'
if old_map in main:
    main=main.replace(old_map,'private val legacyNeuralSpeakerIds = mapOf("pf_dora" to 42, "pm_alex" to 43, "pm_santa" to 44)',1)

start=main.find("    private fun neuralSpeakerId()")
end=main.find("\n\n    private fun parseSpeechCues",start)
if start<0 or end<0:
    raise SystemExit("TTS catalog helper region missing")

region=r'''    private fun friendlyNeuralVoiceLabel(name:String,locale:String):String {
        val sid=name.substringAfterLast('_',"").toIntOrNull()
        val friendly=when(sid){42->"Dora";43->"Alex";44->"Santa";else->name}
        return if(locale.isNotBlank()) "$friendly · $locale · local" else "$friendly · local"
    }

    private fun resolveLegacyNeuralVoice(engine:TextToSpeech,requested:String):String {
        val n=requested.trim()
        if(n.isBlank()) return ""
        val sid=legacyNeuralSpeakerIds[n.lowercase(Locale.ROOT)] ?: return n
        return runCatching { engine.voices.orEmpty().firstOrNull { it.name.endsWith("_$sid") }?.name }
            .getOrNull().orEmpty()
    }

    private fun ttsCatalogJson():String {
        val e=textToSpeech
        val engines=JSONArray()
        engines.put(JSONObject().put("packageName","").put("label","Padrão do Android").put("selected",activeTtsEnginePackage.isBlank()).put("neuralProfile",false))
        discoverTtsEngines().forEach{(pkg,label)->
            engines.put(JSONObject().put("packageName",pkg).put("label",label).put("selected",pkg==activeTtsEnginePackage).put("neuralProfile",isNeuralEngineIdentity(pkg,label)))
        }
        val voices=JSONArray()
        runCatching{e?.voices.orEmpty()}.getOrDefault(emptySet())
            .filter { it.name.isNotBlank() && !it.name.equals("NOT_SET_VOICE",true) }
            .sortedWith(compareBy({ it.locale?.toLanguageTag().orEmpty() },{ it.name }))
            .forEach{v->
                val locale=v.locale?.toLanguageTag().orEmpty()
                val neural=isNeuralTtsProfile()
                val label=if(neural) friendlyNeuralVoiceLabel(v.name,locale) else buildString {
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
            .put("selectedEngineLabel",activeTtsEngineLabel.ifBlank{if(activeTtsEnginePackage.isBlank())"Padrão do Android" else activeTtsEnginePackage})
            .put("selectedVoice",selectedTtsVoiceName)
            .put("neuralProfile",isNeuralTtsProfile())
            .put("neuralRate",neuralTtsRate)
            .put("engines",engines)
            .put("voices",voices)
            .put("profile",JSONObject()
                .put("name",if(isNeuralTtsProfile())"TTS local neural / Kokoro" else "Android TTS")
                .put("minChars",neuralMinChars)
                .put("targetChars",neuralTargetChars)
                .put("maxChars",neuralMaxChars)
                .put("maxNumSentences",if(isNeuralTtsProfile())2 else JSONObject.NULL)
                .put("silenceScale",if(isNeuralTtsProfile())0.06 else JSONObject.NULL)
                .put("numThreads",if(isNeuralTtsProfile())4 else JSONObject.NULL))
            .toString()
    }

    private fun deliverTtsConfigCallback(id:String?,error:String?){
        if(id.isNullOrBlank())return
        val q=JSONObject.quote(id)
        if(error==null){
            val p=ttsCatalogJson()
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.resolve?.($p); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }else{
            evaluate("window.__braseiroTtsConfigCallbacks?.[$q]?.reject?.(new Error(${JSONObject.quote(error)})); delete window.__braseiroTtsConfigCallbacks?.[$q];")
        }
    }

    private fun selectTtsVoiceInternal(name:String):String {
        val e=textToSpeech ?: throw IllegalStateException("O motor TTS ainda não foi inicializado.")
        val requested=resolveLegacyNeuralVoice(e,name)
        if(name.isNotBlank() && requested.isBlank()) throw IllegalArgumentException("A voz antiga salva não existe neste modelo Kokoro. Atualize a lista e escolha Dora, Alex ou Santa.")
        val key=activeTtsEnginePackage.ifBlank{"__android_default__"}
        if(requested.isBlank()){
            selectedTtsVoiceName=""
            ttsPreferences.edit().putString("voice.$key","").apply()
            return ttsCatalogJson()
        }
        val voice=runCatching{e.voices.orEmpty().firstOrNull{it.name==requested}}.getOrNull()
            ?: throw IllegalArgumentException("Voz TTS não disponível no motor selecionado: $requested")
        val result=e.setVoice(voice)
        if(result==TextToSpeech.ERROR) throw IllegalStateException("O motor recusou a voz ${friendlyNeuralVoiceLabel(voice.name,voice.locale?.toLanguageTag().orEmpty())}.")
        selectedTtsVoiceName=voice.name
        ttsPreferences.edit().putString("voice.$key",voice.name).apply()
        return ttsCatalogJson()
    }

    private fun setNeuralTtsRateInternal(rate:Float):String {
        neuralTtsRate=rate.coerceIn(.65f,1.5f)
        ttsPreferences.edit().putFloat("neuralRate",neuralTtsRate).apply()
        if(isNeuralTtsProfile()) runCatching{textToSpeech?.setSpeechRate(neuralTtsRate)}
        return ttsCatalogJson()
    }

    private fun neuralParams(volume:Float)=Bundle().apply{
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume.coerceIn(0f,1f))
    }

    // Corrected engine prewarms its resident OfflineTts internally. Do not
    // queue a hidden utterance from the VTT; that can race with block 1.
    private fun prewarmNeuralTts(force:Boolean=false):Boolean {
        return ttsReady && isNeuralTtsProfile()
    }
'''
region=region.replace("$","$")
main=main[:start]+region+main[end:]

old_init='''        selectedTtsVoiceName=ttsPreferences.getString("voice.$key","").orEmpty()
        e.voices?.firstOrNull{it.name==selectedTtsVoiceName}?.let{runCatching{e.voice=it}}
        installTtsListener(e)'''
new_init='''        val savedVoice=ttsPreferences.getString("voice.$key","").orEmpty()
        selectedTtsVoiceName=resolveLegacyNeuralVoice(e,savedVoice)
        if(selectedTtsVoiceName.isNotBlank()){
            e.voices.orEmpty().firstOrNull{it.name==selectedTtsVoiceName}?.let{voice->
                if(e.setVoice(voice)!=TextToSpeech.ERROR && savedVoice!=selectedTtsVoiceName){
                    ttsPreferences.edit().putString("voice.$key",selectedTtsVoiceName).apply()
                }
            }
        }
        installTtsListener(e)'''
main=req(main,old_init,new_init,"saved voice migration")

old_voice='@JavascriptInterface fun selectTtsVoice(voiceName: String): String = this@MainActivity.selectTtsVoice(voiceName)'
new_voice='''@JavascriptInterface fun selectTtsVoice(voiceName: String, requestId: String) = runOnUiThread {
            try { selectTtsVoiceInternal(voiceName); deliverTtsConfigCallback(requestId,null) }
            catch (e:Throwable) { deliverTtsConfigCallback(requestId,e.message ?: "Falha ao selecionar voz TTS.") }
        }'''
main=req(main,old_voice,new_voice,"async voice bridge")

old_rate='@JavascriptInterface fun setNeuralTtsRate(rate: Double): String = this@MainActivity.setNeuralTtsRate(rate.toFloat())'
new_rate='''@JavascriptInterface fun setNeuralTtsRate(rate: Double, requestId: String) = runOnUiThread {
            try { setNeuralTtsRateInternal(rate.toFloat()); deliverTtsConfigCallback(requestId,null) }
            catch (e:Throwable) { deliverTtsConfigCallback(requestId,e.message ?: "Falha ao ajustar velocidade TTS.") }
        }'''
main=req(main,old_rate,new_rate,"async rate bridge")

old_js_voice="selectTtsVoice: voiceName => Promise.resolve(JSON.parse(BraseiroNative.selectTtsVoice(String(voiceName||'')) || '{}')),"
new_js_voice="selectTtsVoice: voiceName => new Promise((resolve,reject) => { const id='tts_voice_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.selectTtsVoice(String(voiceName||''),id); }),"
main=req(main,old_js_voice,new_js_voice,"JS async voice bridge")

old_js_rate="setNeuralTtsRate: rate => Promise.resolve(JSON.parse(BraseiroNative.setNeuralTtsRate(Number(rate||1)) || '{}')),"
new_js_rate="setNeuralTtsRate: rate => new Promise((resolve,reject) => { const id='tts_rate_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.setNeuralTtsRate(Number(rate||1),id); }),"
main=req(main,old_js_rate,new_js_rate,"JS async rate bridge")

if 'val mode=if(neural&&p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH' not in main:
    raise SystemExit("4.9.29 neural queue behavior missing")

write(main_rel,main)

settings_rel="components/SettingsView.tsx"
s=read(settings_rel)
old_opt="{(androidTtsCatalog?.voices||[]).map((v:any)=><option key={v.name} value={v.name}>{v.name}{v.locale?' · '+v.locale:''}{v.networkRequired?' · rede':' · local'}</option>)}"
new_opt="{(androidTtsCatalog?.voices||[]).map((v:any)=><option key={v.name} value={v.name}>{v.label||[v.name,v.locale,v.networkRequired?'rede':'local'].filter(Boolean).join(' · ')}</option>)}"
s=req(s,old_opt,new_opt,"friendly real voice labels")

old_change="const changeAndroidTtsVoice=async(name:string)=>{if(!window.BraseiroAndroid?.selectTtsVoice)return;try{const c=await window.BraseiroAndroid.selectTtsVoice(name);setAndroidTtsCatalog(c);setAndroidTtsStatus('Voz selecionada.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};"
new_change="const changeAndroidTtsVoice=async(name:string)=>{if(!window.BraseiroAndroid?.selectTtsVoice)return;setAndroidTtsStatus('Selecionando voz real do motor…');try{const c=await window.BraseiroAndroid.selectTtsVoice(name);setAndroidTtsCatalog(c);const v=(c?.voices||[]).find((x:any)=>x.name===c?.selectedVoice);setAndroidTtsStatus(v?'Voz ativa: '+(v.label||v.name):'Voz automática do motor ativa.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};"
s=req(s,old_change,new_change,"voice selection status")
s=s.replace("setAndroidTtsStatus('Testando continuidade…');try{await window.BraseiroAndroid.speakText(","setAndroidTtsStatus('Testando voz local real…');try{await window.BraseiroAndroid.speakText(",1)
write(settings_rel,s)

global_rel="global.d.ts"
g=read(global_rel)
g=g.replace("voices:Array<{name:string;locale:string;quality:number;latency:number;networkRequired:boolean;selected:boolean;speakerId?:number}>","voices:Array<{name:string;label?:string;locale:string;quality:number;latency:number;networkRequired:boolean;selected:boolean}>",1)
write(global_rel,g)

pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8")); data["version"]="4.9.31"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.30"','"version": "4.9.31"',t,count=2)
    lock.write_text(t,encoding="utf-8")
version_rel="lib/version.ts"; t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.30'","APP_VERSION = '4.9.31'",t)
t=re.sub(r"APP_VERSION_CODE = 49030","APP_VERSION_CODE = 49031",t)
write(version_rel,t)
gradle_rel="android/app/build.gradle.kts"; t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49030","versionCode = 49031",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.30"','versionName = "4.9.31"',t)
write(gradle_rel,t)
main=read(main_rel).replace("index.html?v=49030&immersive=1","index.html?v=49031&immersive=1")
write(main_rel,main)

main=read(main_rel); s=read(settings_rel); app=read("App.tsx")
checks={
 "no-fake-voice-options": 'listOf("pf_dora" to 42' not in main and '.put("speakerId"' not in main,
 "real-android-voice-enumeration": 'e?.voices.orEmpty()' in main and 'friendlyNeuralVoiceLabel' in main,
 "real-voice-set": 'val result=e.setVoice(voice)' in main,
 "legacy-migration-only": 'legacyNeuralSpeakerIds' in main and 'resolveLegacyNeuralVoice' in main,
 "async-voice-bridge": 'selectTtsVoice(voiceName: String, requestId: String)' in main and "tts_voice_" in main,
 "async-rate-bridge": 'setNeuralTtsRate(rate: Double, requestId: String)' in main and "tts_rate_" in main,
 "no-vtt-queued-prewarm": 'e.speak("Olá."' not in main and 'private fun prewarmNeuralTts(force:Boolean=false):Boolean' in main,
 "block0-flush-later-add": 'if(neural&&p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH' in main,
 "no-neural-watchdog": 'if(isNeuralTtsProfile())return' in main,
 "friendly-ui-label": 'v.label||' in s and 'Voz ativa:' in s,
 "charon-kokoro-pref-preserved": 'braseiro.narrationVoiceBackend' in s and 'braseiro.narrationVoiceBackend' in app,
 "route-lock-preserved": 'Movimento livre bloqueado: selecione um destino e use Planejar rota' in app,
 "rest-handler-preserved": "const handlePartyRested = async (kind: 'short' | 'long') => {" in app,
 "db-preserved": "const DB_NAME = 'braseiro-db';" in read("lib/db.ts") and "const DB_VERSION = 19;" in read("lib/db.ts"),
 "version": "4.9.31" in read(version_rel) and "49031" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4931_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.31_REAL_KOKORO_VOICE_OK")
