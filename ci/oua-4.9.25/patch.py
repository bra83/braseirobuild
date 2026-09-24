from pathlib import Path
import json,re,sys
if len(sys.argv)!=2: raise SystemExit("usage: patch_4925.py <root>")
root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)
def dol(s): return s.replace("§","$")

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)
if "import kotlin.math.abs" not in main:
    main=main.replace("import kotlin.math.max\n","import kotlin.math.max\nimport kotlin.math.abs\n",1)

main=req(main,
"""    private var speechRate = .96f
    private var speechWatchdog: Runnable? = null
""",
"""    private var speechRate = .96f
    private var speechWatchdog: Runnable? = null
    private val ttsPreferences by lazy { getSharedPreferences("braseiro_oumanel_tts", MODE_PRIVATE) }
    @Volatile private var activeTtsEnginePackage = ""
    @Volatile private var activeTtsEngineLabel = ""
    @Volatile private var selectedTtsVoiceName = ""
    @Volatile private var neuralTtsRate = 1.0f
    @Volatile private var startupTtsSelectionAttempted = false
    @Volatile private var lastNeuralPrewarmAt = 0L
    private val neuralSpeakerIds = mapOf("pf_dora" to 42, "pm_alex" to 43, "pm_santa" to 44)
    private val neuralMinChars = 250
    private val neuralTargetChars = 450
    private val neuralMaxChars = 650
""","tts state")

rx=re.compile(r"    override fun onInit\(status: Int\) \{[\s\S]*?\n    \}\n\n    private fun parseSpeechCues")
m=rx.search(main)
if not m: raise SystemExit("onInit block missing")
new=dol(r'''    override fun onInit(status: Int) {
        val saved = ttsPreferences.getString("enginePackage","").orEmpty().trim()
        if (!startupTtsSelectionAttempted && saved.isNotBlank()) {
            startupTtsSelectionAttempted=true
            if (textToSpeech?.engines.orEmpty().any { it.name==saved }) { switchTtsEngine(saved,null); return }
            ttsPreferences.edit().remove("enginePackage").apply()
        }
        startupTtsSelectionAttempted=true
        configureInitializedTts(status,null)
    }

    private fun isNeuralEngineIdentity(pkg:String,label:String="",voice:String=""):Boolean {
        val x="§pkg §label §voice".lowercase(Locale.ROOT)
        return x.contains("com.k2fsa.sherpa.onnx.tts.engine") || x.contains("sherpa") || x.contains("kokoro")
    }
    private fun isNeuralTtsProfile()=isNeuralEngineIdentity(activeTtsEnginePackage,activeTtsEngineLabel,selectedTtsVoiceName)

    private fun installTtsListener(engine:TextToSpeech) {
        engine.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
            override fun onStart(id:String){ val p=speechParts[id]?:return; if(p.index==0)evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-start'));"); fireSpeechCues(p,p.offset) }
            override fun onRangeStart(id:String,start:Int,end:Int,frame:Int){ val p=speechParts[id]?:return; evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-boundary',{detail:§{p.offset+start}}));"); fireSpeechCues(p,p.offset+end) }
            override fun onDone(id:String){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); fireSpeechCues(p,p.offset+p.text.length); runOnUiThread{advanceSpeechQueue(p.rootId)} }
            @Deprecated("Deprecated in Java") override fun onError(id:String){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); if(isNeuralTtsProfile())runOnUiThread{finishSpeech(p.rootId,"Falha TTS neural no bloco §{p.index+1}.")} else runOnUiThread{advanceSpeechQueue(p.rootId)} }
            override fun onError(id:String,code:Int){ val p=speechParts.remove(id)?:return; cancelSpeechWatchdog(); if(isNeuralTtsProfile())runOnUiThread{finishSpeech(p.rootId,"Falha TTS neural no bloco §{p.index+1} (§code).")} else runOnUiThread{advanceSpeechQueue(p.rootId)} }
        })
    }

    private fun configureInitializedTts(status:Int,callbackId:String?) {
        if(status!=TextToSpeech.SUCCESS){ttsReady=false;deliverTtsConfigCallback(callbackId,"O motor TTS selecionado não iniciou.");return}
        val e=textToSpeech?:run{ttsReady=false;deliverTtsConfigCallback(callbackId,"O Android não criou o motor TTS.");return}
        activeTtsEngineLabel=e.engines.orEmpty().firstOrNull{it.name==activeTtsEnginePackage}?.label.orEmpty()
        val neural=isNeuralTtsProfile()
        val lang=if(neural) listOf(Locale("pt","BR"),Locale("pt","PT"),Locale.getDefault()).distinct().asSequence().map{e.setLanguage(it)}.firstOrNull{it!=TextToSpeech.LANG_MISSING_DATA&&it!=TextToSpeech.LANG_NOT_SUPPORTED}?:TextToSpeech.LANG_NOT_SUPPORTED else e.setLanguage(Locale("pt","BR"))
        ttsReady=lang!=TextToSpeech.LANG_MISSING_DATA&&lang!=TextToSpeech.LANG_NOT_SUPPORTED
        e.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        neuralTtsRate=ttsPreferences.getFloat("neuralRate",1f).coerceIn(.65f,1.5f)
        val key=activeTtsEnginePackage.ifBlank{"__android_default__"}
        selectedTtsVoiceName=ttsPreferences.getString("voice.§key","").orEmpty()
        e.voices?.firstOrNull{it.name==selectedTtsVoiceName}?.let{runCatching{e.voice=it}}
        installTtsListener(e)
        deliverTtsConfigCallback(callbackId,if(ttsReady)null else "O motor não oferece voz compatível com pt-BR.")
        if(ttsReady&&neural)prewarmNeuralTts()
    }

    private fun switchTtsEngine(pkg:String,callbackId:String?) {
        val requested=pkg.trim(); val old=textToSpeech
        if(requested.isNotBlank()&&old?.engines.orEmpty().none{it.name==requested}){deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: §requested");return}
        if(currentSpeechRootId!=null)stopNativeSpeech(true)
        runCatching{old?.shutdown()}; ttsReady=false; activeTtsEnginePackage=requested
        ttsPreferences.edit().putString("enginePackage",requested).apply()
        val l=TextToSpeech.OnInitListener{s->runOnUiThread{configureInitializedTts(s,callbackId)}}
        textToSpeech=if(requested.isBlank())TextToSpeech(this,l) else TextToSpeech(this,l,requested)
    }

    private fun neuralSpeakerId()=neuralSpeakerIds[selectedTtsVoiceName.lowercase(Locale.ROOT)]
    private fun ttsCatalogJson():String {
        val e=textToSpeech; val engines=JSONArray()
        engines.put(JSONObject().put("packageName","").put("label","Padrão do Android").put("selected",activeTtsEnginePackage.isBlank()).put("neuralProfile",false))
        e?.engines.orEmpty().sortedBy{it.label.lowercase(Locale.ROOT)}.forEach{i->engines.put(JSONObject().put("packageName",i.name).put("label",i.label).put("selected",i.name==activeTtsEnginePackage).put("neuralProfile",isNeuralEngineIdentity(i.name,i.label)))}
        val voices=JSONArray(); val seen=mutableSetOf<String>()
        runCatching{e?.voices.orEmpty()}.getOrDefault(emptySet()).sortedBy{it.name}.forEach{v->
            if(seen.add(v.name))voices.put(JSONObject().put("name",v.name).put("locale",v.locale?.toLanguageTag()?:"").put("quality",v.quality).put("latency",v.latency).put("networkRequired",v.isNetworkConnectionRequired).put("selected",v.name==selectedTtsVoiceName))
        }
        if(isNeuralTtsProfile()) listOf("pf_dora" to 42,"pm_alex" to 43,"pm_santa" to 44).forEach{(n,sid)->if(seen.add(n))voices.put(JSONObject().put("name",n).put("locale","pt-BR").put("quality",500).put("latency",500).put("networkRequired",false).put("selected",n==selectedTtsVoiceName).put("speakerId",sid))}
        return JSONObject().put("ready",ttsReady).put("selectedEnginePackage",activeTtsEnginePackage).put("selectedEngineLabel",activeTtsEngineLabel.ifBlank{if(activeTtsEnginePackage.isBlank())"Padrão do Android" else activeTtsEnginePackage}).put("selectedVoice",selectedTtsVoiceName).put("neuralProfile",isNeuralTtsProfile()).put("neuralRate",neuralTtsRate).put("engines",engines).put("voices",voices).put("profile",JSONObject().put("name",if(isNeuralTtsProfile())"TTS local neural / Kokoro" else "Android TTS").put("minChars",neuralMinChars).put("targetChars",neuralTargetChars).put("maxChars",neuralMaxChars).put("maxNumSentences",if(isNeuralTtsProfile())2 else JSONObject.NULL).put("silenceScale",if(isNeuralTtsProfile())0.06 else JSONObject.NULL).put("numThreads",if(isNeuralTtsProfile())4 else JSONObject.NULL)).toString()
    }
    private fun deliverTtsConfigCallback(id:String?,error:String?){if(id.isNullOrBlank())return;val q=JSONObject.quote(id);if(error==null){val p=ttsCatalogJson();evaluate("window.__braseiroTtsConfigCallbacks?.[§q]?.resolve?.(§p); delete window.__braseiroTtsConfigCallbacks?.[§q];")}else evaluate("window.__braseiroTtsConfigCallbacks?.[§q]?.reject?.(new Error(§{JSONObject.quote(error)})); delete window.__braseiroTtsConfigCallbacks?.[§q];")}
    private fun selectTtsVoice(name:String):String{val e=textToSpeech?:return ttsCatalogJson();val n=name.trim();val v=runCatching{e.voices?.firstOrNull{it.name==n}}.getOrNull();val known=isNeuralTtsProfile()&&neuralSpeakerIds.containsKey(n.lowercase(Locale.ROOT));require(n.isBlank()||v!=null||known){"Voz TTS não disponível: §n"};selectedTtsVoiceName=n;val k=activeTtsEnginePackage.ifBlank{"__android_default__"};ttsPreferences.edit().putString("voice.§k",n).apply();if(v!=null)runCatching{e.voice=v};if(isNeuralTtsProfile())prewarmNeuralTts(true);return ttsCatalogJson()}
    private fun setNeuralTtsRate(rate:Float):String{neuralTtsRate=rate.coerceIn(.65f,1.5f);ttsPreferences.edit().putFloat("neuralRate",neuralTtsRate).apply();if(isNeuralTtsProfile())runCatching{textToSpeech?.setSpeechRate(neuralTtsRate)};return ttsCatalogJson()}
    private fun neuralParams(volume:Float)=Bundle().apply{putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,volume.coerceIn(0f,1f));putInt("maxNumSentences",2);putFloat("silenceScale",.06f);putInt("numThreads",4);putInt("braseiro.kokoro.maxNumSentences",2);putFloat("braseiro.kokoro.silenceScale",.06f);putInt("braseiro.kokoro.numThreads",4);neuralSpeakerId()?.let{putInt("speakerId",it);putInt("braseiro.kokoro.speakerId",it)}}
    private fun prewarmNeuralTts(force:Boolean=false):Boolean{if(!ttsReady||!isNeuralTtsProfile()||currentSpeechRootId!=null)return false;val now=System.currentTimeMillis();if(!force&&now-lastNeuralPrewarmAt<60000)return true;lastNeuralPrewarmAt=now;val e=textToSpeech?:return false;runCatching{e.setSpeechRate(neuralTtsRate);e.speak("Olá.",TextToSpeech.QUEUE_ADD,neuralParams(.01f),"braseiro-neural-prewarm-§now")};return true}

    private fun parseSpeechCues''')
main=main[:m.start()]+new+main[m.end():]

rx=re.compile(r"    private fun buildSpeechSegments\(text: String, cues: List<SpeechCue>\): List<SpeechSegment> \{[\s\S]*?\n    private fun stopNativeSpeech")
m=rx.search(main)
if not m: raise SystemExit("speech block missing")
new=dol(r'''    private fun chooseNeuralBoundary(text:String,start:Int,maxChars:Int):Int{
        val high=(start+maxChars).coerceAtMost(text.length);if(high>=text.length)return text.length
        val low=(start+neuralMinChars.coerceAtMost(maxChars)).coerceAtMost(high);val target=(start+neuralTargetChars.coerceAtMost(maxChars)).coerceAtMost(high)
        fun closest(chars:Set<Char>,sentence:Boolean):Int?{var best:Int?=null;var dist=Int.MAX_VALUE;for(i in low until high){if(text[i] !in chars)continue;if(sentence&&i+1<text.length&&!text[i+1].isWhitespace())continue;val b=i+1;val d=abs(b-target);if(d<dist){best=b;dist=d}};return best}
        closest(setOf('.','!','?'),true)?.let{return it};closest(setOf(';',','),false)?.let{return it}
        var best:Int?=null;var dist=Int.MAX_VALUE;for(i in low until high)if(text[i].isWhitespace()){val d=abs(i-target);if(d<dist){best=i;dist=d}};return best?:high
    }
    private fun buildNeuralSpeechSegments(text:String,cues:List<SpeechCue>,engineMax:Int):List<SpeechSegment>{
        if(text.isBlank())return emptyList();val maxChars=neuralMaxChars.coerceAtMost(engineMax).coerceAtLeast(120);val out=mutableListOf<SpeechSegment>();var cursor=0
        while(cursor<text.length){while(cursor<text.length&&text[cursor].isWhitespace())cursor++;if(cursor>=text.length)break;val boundary=chooseNeuralBoundary(text,cursor,maxChars);var end=boundary.coerceAtMost(text.length);while(end>cursor&&text[end-1].isWhitespace())end--;if(end<=cursor)end=(cursor+maxChars).coerceAtMost(text.length);val part=text.substring(cursor,end);out+=SpeechSegment(part,cursor,cues.filter{it.charIndex in cursor..end});cursor=boundary.coerceAtLeast(end)}
        return out
    }
    private fun buildSpeechSegments(text:String,cues:List<SpeechCue>):List<SpeechSegment>{if(text.isBlank())return emptyList();val max=TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(120);if(isNeuralTtsProfile())return buildNeuralSpeechSegments(text,cues,max);return NarrationSegmenter.splitIntoChapters(text,680).flatMap{chapter->NarrationSegmenter.splitForAndroidTts(chapter,180.coerceAtMost(max)).map{s->val end=s.offset+s.text.length;SpeechSegment(s.text,s.offset,cues.filter{it.charIndex in s.offset..end})}}}
    private fun fireSpeechCues(part:SpeechPart,absolutePosition:Int){part.cues.filter{it.charIndex<=absolutePosition}.forEach{cue->val key="§{part.rootId}:§{cue.id}:§{cue.charIndex}";if(!firedSpeechCues.add(key))return@forEach;val detail=JSONObject().put("id",cue.id).put("charIndex",cue.charIndex).toString();evaluate("window.dispatchEvent(new CustomEvent('braseiro-native-tts-cue',{detail:§detail}));")}}
    private fun cancelSpeechWatchdog(){speechWatchdog?.let(speechHandler::removeCallbacks);speechWatchdog=null}
    private fun scheduleSpeechWatchdog(part:SpeechPart){if(isNeuralTtsProfile())return;cancelSpeechWatchdog();val estimated=((part.text.length/(13.0*speechRate.coerceAtLeast(.65f)))*1000).toLong().coerceAtLeast(1200)+2500;speechWatchdog=Runnable{val active=currentSpeechRootId==part.rootId&&speechQueueIndex==part.index;if(!active)return@Runnable;speechParts.entries.filter{it.value.rootId==part.rootId&&it.value.index==part.index}.forEach{speechParts.remove(it.key)};textToSpeech?.stop();advanceSpeechQueue(part.rootId)}.also{speechHandler.postDelayed(it,estimated)}}
    private fun startCurrentSpeechPart(rootId:String){if(currentSpeechRootId!=rootId)return;if(speechQueueIndex>=speechQueue.size){finishSpeech(rootId,null);return};val s=speechQueue[speechQueueIndex];val id="§rootId::§speechQueueIndex";val p=SpeechPart(rootId,speechQueueIndex,speechQueue.size,s.offset,s.text,s.cues);speechParts[id]=p;val neural=isNeuralTtsProfile();val params=if(neural)neuralParams(nativeVolumes["voice"]?:1f)else Bundle().apply{putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,nativeVolumes["voice"]?:1f)};val e=textToSpeech;if(neural&&p.index==0)e?.playSilentUtterance(50L,TextToSpeech.QUEUE_ADD,"§rootId::prime");val mode=if(neural)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH;val result=e?.speak(s.text,mode,params,id)?:TextToSpeech.ERROR;if(result==TextToSpeech.ERROR){speechParts.remove(id);if(neural)finishSpeech(rootId,"O motor TTS neural recusou o bloco §{p.index+1}.")else advanceSpeechQueue(rootId)}else if(!neural)scheduleSpeechWatchdog(p)}
    private fun advanceSpeechQueue(rootId:String){if(currentSpeechRootId!=rootId)return;speechQueueIndex++;if(speechQueueIndex>=speechQueue.size)finishSpeech(rootId,null)else startCurrentSpeechPart(rootId)}
    private fun speakNative(text:String,rate:Float,rootId:String,cues:List<SpeechCue> = emptyList()){if(!ttsReady||textToSpeech==null){finishSpeech(rootId,"A voz do Android ainda não está pronta.");return};if(!requestNativeAudioFocus()){finishSpeech(rootId,"O Android não concedeu foco de áudio para a narração.");return};if(currentSpeechRootId!=null&&currentSpeechRootId!=rootId)stopNativeSpeech(true);currentSpeechRootId=rootId;val neural=isNeuralTtsProfile();speechRate=if(neural)neuralTtsRate else rate.coerceIn(.65f,1.5f);textToSpeech!!.setSpeechRate(speechRate);firedSpeechCues.clear();speechQueue=buildSpeechSegments(text,cues);speechQueueIndex=0;if(speechQueue.isEmpty()){finishSpeech(rootId,null);return};startCurrentSpeechPart(rootId)}

    private fun stopNativeSpeech''')
main=main[:m.start()]+new+main[m.end():]

main=req(main,
"""        @JavascriptInterface fun isTtsReady(): Boolean = ttsReady
        @JavascriptInterface fun speakText(text: String, rate: Double, utteranceId: String) = runOnUiThread { speakNative(text, rate.toFloat(), utteranceId.ifBlank { UUID.randomUUID().toString() }) }
""",
"""        @JavascriptInterface fun isTtsReady(): Boolean = ttsReady
        @JavascriptInterface fun getTtsCatalog(): String = ttsCatalogJson()
        @JavascriptInterface fun selectTtsEngine(packageName: String, requestId: String) = runOnUiThread { switchTtsEngine(packageName, requestId) }
        @JavascriptInterface fun selectTtsVoice(voiceName: String): String = selectTtsVoice(voiceName)
        @JavascriptInterface fun setNeuralTtsRate(rate: Double): String = setNeuralTtsRate(rate.toFloat())
        @JavascriptInterface fun prewarmTts(): Boolean { runOnUiThread { prewarmNeuralTts() }; return isNeuralTtsProfile() }
        @JavascriptInterface fun speakText(text: String, rate: Double, utteranceId: String) = runOnUiThread { speakNative(text, rate.toFloat(), utteranceId.ifBlank { UUID.randomUUID().toString() }) }
""","native bridge")

main=req(main,"              window.__braseiroTtsCallbacks = window.__braseiroTtsCallbacks || {};","""              window.__braseiroTtsCallbacks = window.__braseiroTtsCallbacks || {};
              window.__braseiroTtsConfigCallbacks = window.__braseiroTtsConfigCallbacks || {};""","js callbacks")
main=req(main,
"""                isNativeTtsAvailable: () => BraseiroNative.isTtsReady(),
                speakText: (text, rate=0.96) => new Promise((resolve,reject) => { const id='tts_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsCallbacks[id]={resolve,reject}; BraseiroNative.speakText(String(text||''), Number(rate||0.96), id); }),
""",
"""                isNativeTtsAvailable: () => BraseiroNative.isTtsReady(),
                getTtsCatalog: () => Promise.resolve(JSON.parse(BraseiroNative.getTtsCatalog() || '{}')),
                selectTtsEngine: packageName => new Promise((resolve,reject) => { const id='tts_cfg_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsConfigCallbacks[id]={resolve,reject}; BraseiroNative.selectTtsEngine(String(packageName||''),id); }),
                selectTtsVoice: voiceName => Promise.resolve(JSON.parse(BraseiroNative.selectTtsVoice(String(voiceName||'')) || '{}')),
                setNeuralTtsRate: rate => Promise.resolve(JSON.parse(BraseiroNative.setNeuralTtsRate(Number(rate||1)) || '{}')),
                prewarmTts: () => Promise.resolve(BraseiroNative.prewarmTts()),
                speakText: (text, rate=0.96) => new Promise((resolve,reject) => { const id='tts_'+Date.now()+'_'+Math.random().toString(36).slice(2); window.__braseiroTtsCallbacks[id]={resolve,reject}; BraseiroNative.speakText(String(text||''), Number(rate||0.96), id); }),
""","js bridge")
write(main_rel,main)

global_rel="global.d.ts";g=read(global_rel)
g=req(g,
"""      isNativeTtsAvailable?: () => boolean;
      speakText?: (text: string, rate?: number) => Promise<void>;
""",
"""      isNativeTtsAvailable?: () => boolean;
      getTtsCatalog?: () => Promise<{ ready:boolean; selectedEnginePackage:string; selectedEngineLabel:string; selectedVoice:string; neuralProfile:boolean; neuralRate:number; engines:Array<{packageName:string;label:string;selected:boolean;neuralProfile:boolean}>; voices:Array<{name:string;locale:string;quality:number;latency:number;networkRequired:boolean;selected:boolean;speakerId?:number}>; profile?:Record<string,any> }>;
      selectTtsEngine?: (packageName:string) => Promise<Record<string,any>>;
      selectTtsVoice?: (voiceName:string) => Promise<Record<string,any>>;
      setNeuralTtsRate?: (rate:number) => Promise<Record<string,any>>;
      prewarmTts?: () => Promise<boolean>;
      speakText?: (text: string, rate?: number) => Promise<void>;
""","global bridge")
write(global_rel,g)

settings_rel="components/SettingsView.tsx";s=read(settings_rel)
s=s.replace("import React, { useState } from 'react';","import React, { useEffect, useState } from 'react';",1)
mr=re.search(r"(  const \[testing, setTesting\] = useState<[^\n]+\n)",s)
if not mr: raise SystemExit("settings testing anchor missing")
extra=mr.group(1)+"""  const [androidTtsCatalog,setAndroidTtsCatalog]=useState<any>(null);
  const [androidTtsStatus,setAndroidTtsStatus]=useState('');
  const refreshAndroidTts=async()=>{if(!window.BraseiroAndroid?.getTtsCatalog)return;try{setAndroidTtsCatalog(await window.BraseiroAndroid.getTtsCatalog())}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  useEffect(()=>{void refreshAndroidTts();const f=()=>void refreshAndroidTts();window.addEventListener('braseiro-native-ready',f);return()=>window.removeEventListener('braseiro-native-ready',f)},[]);
  const changeAndroidTtsEngine=async(pkg:string)=>{if(!window.BraseiroAndroid?.selectTtsEngine)return;setAndroidTtsStatus('Carregando motor TTS…');try{const c=await window.BraseiroAndroid.selectTtsEngine(pkg);setAndroidTtsCatalog(c);setAndroidTtsStatus(c?.neuralProfile?'TTS local neural / Kokoro carregado e pré-aquecendo.':'Motor Android selecionado.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  const changeAndroidTtsVoice=async(name:string)=>{if(!window.BraseiroAndroid?.selectTtsVoice)return;try{const c=await window.BraseiroAndroid.selectTtsVoice(name);setAndroidTtsCatalog(c);setAndroidTtsStatus('Voz selecionada.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
  const changeNeuralRate=async(rate:number)=>{if(window.BraseiroAndroid?.setNeuralTtsRate)setAndroidTtsCatalog(await window.BraseiroAndroid.setNeuralTtsRate(rate))};
  const testAndroidTts=async()=>{if(!window.BraseiroAndroid?.speakText){setAndroidTtsStatus('TTS Android indisponível.');return}setAndroidTtsStatus('Testando continuidade…');try{await window.BraseiroAndroid.speakText('A porta range. Há marcas no chão. Algo se move no corredor. A chama vacila e o vento toca a madeira, mas ninguém ainda vê quem está além da curva.',androidTtsCatalog?.neuralProfile?Number(androidTtsCatalog?.neuralRate||1):0.96);setAndroidTtsStatus('Teste concluído até o fim.')}catch(e){setAndroidTtsStatus(e instanceof Error?e.message:String(e))}};
"""
s=s[:mr.start()]+extra+s[mr.end():]
anchor="""          <label className="block rounded border border-blue-900/60 bg-slate-950/65 p-4">
            <span className="flex items-center gap-2 font-bold text-blue-200"><MoonStar"""
if anchor not in s: raise SystemExit("atmosphere anchor missing")
card="""          <div className="rounded border border-violet-900/60 bg-violet-950/10 p-4">
            <span className="flex items-center gap-2 font-bold text-violet-200"><Volume2 className="h-4 w-4" /> TTS Android / Kokoro</span>
            <p className="mt-1 text-xs text-slate-400">Motores e vozes vêm do Android. Samsung/Google mantêm o fluxo padrão; Sherpa/Kokoro usa perfil neural local.</p>
            <label className="mt-3 block text-xs font-bold text-violet-100">Motor instalado</label>
            <select value={androidTtsCatalog?.selectedEnginePackage||''} onChange={e=>void changeAndroidTtsEngine(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50">{(androidTtsCatalog?.engines||[{packageName:'',label:'Padrão do Android'}]).map((x:any)=><option key={x.packageName||'__default__'} value={x.packageName}>{x.label}{x.neuralProfile?' · neural':''}</option>)}</select>
            <label className="mt-3 block text-xs font-bold text-violet-100">Voz do motor selecionado</label>
            <select value={androidTtsCatalog?.selectedVoice||''} onChange={e=>void changeAndroidTtsVoice(e.target.value)} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50"><option value="">Automática / padrão do motor</option>{(androidTtsCatalog?.voices||[]).map((v:any)=><option key={v.name} value={v.name}>{v.name}{v.locale?' · '+v.locale:''}{v.networkRequired?' · rede':' · local'}</option>)}</select>
            {androidTtsCatalog?.neuralProfile&&<div className="mt-3 rounded border border-cyan-900/70 bg-cyan-950/20 p-3"><div className="font-bold text-cyan-200">TTS local neural / Kokoro</div><p className="mt-1 text-[11px] text-slate-400">Blocos ~450 caracteres (250–650), maxNumSentences 2, silenceScale 0,06, 4 threads; próximo bloco somente após onDone real.</p><label className="mt-3 block text-xs font-bold text-cyan-100">Velocidade Kokoro: {Number(androidTtsCatalog?.neuralRate||1).toFixed(2)}×</label><input type="range" min="0.65" max="1.5" step="0.05" value={Number(androidTtsCatalog?.neuralRate||1)} onChange={e=>void changeNeuralRate(Number(e.target.value))} className="mt-1 w-full"/><div className="mt-1 text-[10px] text-slate-500">1.00× = velocidade neutra da engine; sem multiplicação dupla.</div></div>}
            <div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={()=>void refreshAndroidTts()} className="min-h-10 rounded border border-slate-700 bg-slate-950 text-xs font-bold text-slate-300">Atualizar lista</button><button type="button" onClick={()=>void testAndroidTts()} className="min-h-10 rounded border border-violet-700 bg-violet-950/40 text-xs font-bold text-violet-100">Testar voz local</button></div>
            {androidTtsStatus&&<p className="mt-2 text-xs text-slate-300">{androidTtsStatus}</p>}
          </div>

"""
s=s.replace(anchor,card+anchor,1);write(settings_rel,s)

app_rel="App.tsx";a=read(app_rel)
a=req(a,"  useEffect(() => { campaignRef.current = campaign; }, [campaign]);","""  useEffect(() => { campaignRef.current = campaign; }, [campaign]);
  useEffect(() => {
    if (!campaign?.history?.length) return;
    void window.BraseiroAndroid?.prewarmTts?.();
  }, [campaign?.history?.length, campaign?.history?.at(-1)?.id]);""","prewarm effect")
write(app_rel,a)

pkg=root/"package.json";d=json.loads(pkg.read_text(encoding="utf-8"));d["version"]="4.9.25";pkg.write_text(json.dumps(d,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8");t=re.sub(r'"version":\s*"4\.9\.24"','"version": "4.9.25"',t,count=2);lock.write_text(t,encoding="utf-8")
v="lib/version.ts";t=read(v);t=re.sub(r"APP_VERSION = '4\.9\.24'","APP_VERSION = '4.9.25'",t);t=re.sub(r"APP_VERSION_CODE = 49024","APP_VERSION_CODE = 49025",t);write(v,t)
gr="android/app/build.gradle.kts";t=read(gr);t=re.sub(r"versionCode\s*=\s*49024","versionCode = 49025",t);t=re.sub(r'versionName\s*=\s*"4\.9\.24"','versionName = "4.9.25"',t);write(gr,t)
main=read(main_rel).replace("index.html?v=49024&immersive=1","index.html?v=49025&immersive=1");write(main_rel,main)

main=read(main_rel);s=read(settings_rel);g=read(global_rel);a=read(app_rel);db=read("lib/db.ts");emap=read("components/EriadorMap.tsx")
checks={
"engines":"engine?.engines" in main and "getTtsCatalog" in main,
"voices":"e?.voices" in main and "selectTtsVoice" in main,
"neural":"com.k2fsa.sherpa.onnx.tts.engine" in main,
"chunk":"neuralMinChars = 250" in main and "neuralTargetChars = 450" in main and "neuralMaxChars = 650" in main,
"punct":"setOf('.','!','?')" in main and "setOf(';',',')" in main,
"queue":"val mode=if(neural)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH" in main,
"done":"override fun onDone" in main and "advanceSpeechQueue(p.rootId)" in main,
"watchdog":"if(isNeuralTtsProfile())return" in main,
"params":'putInt("maxNumSentences",2)' in main and 'putFloat("silenceScale",.06f)' in main and 'putInt("numThreads",4)' in main,
"priming":"playSilentUtterance(50L,TextToSpeech.QUEUE_ADD" in main,
"speed":"speechRate=if(neural)neuralTtsRate else rate.coerceIn" in main,
"prewarm":"prewarmNeuralTts" in main and "prewarmTts" in a,
"ui":"TTS Android / Kokoro" in s and "Velocidade Kokoro" in s,
"bridge":"getTtsCatalog?:" in g and "selectTtsEngine?:" in g,
"normal":"NarrationSegmenter.splitForAndroidTts(chapter,180.coerceAtMost(max))" in main,
"db":"const DB_NAME = 'braseiro-db';" in db and "const DB_VERSION = 19;" in db,
"party":'data-party-world-anchored="true"' in emap,
"version":"4.9.25" in read(v) and "49025" in read(gr),
}
for k,vv in checks.items(): print(("PASS" if vv else "FAIL"),k)
bad=[k for k,vv in checks.items() if not vv]
if bad: raise SystemExit("PATCH_4925_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.25_TTS_OK")
