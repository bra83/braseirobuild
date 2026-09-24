from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4929.py <root>")

root=Path(sys.argv[1]).resolve()
def read(p): return (root/p).read_text(encoding="utf-8")
def write(p,s): (root/p).write_text(s,encoding="utf-8")
def req(s,a,b,label):
    if a not in s: raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# 1) FIX JS BRIDGE RECURSION FOR VOICE/RATE SELECTION
# The @JavascriptInterface methods introduced in 4.9.25 called themselves:
#   selectTtsVoice(...) = selectTtsVoice(...)
#   setNeuralTtsRate(...) = setNeuralTtsRate(...)
# This produces "Java exception was raised during method invocation" and the
# UI falls back to "Automática / padrão do motor".
# ---------------------------------------------------------------------------
main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

main=req(
    main,
    '@JavascriptInterface fun selectTtsVoice(voiceName: String): String = selectTtsVoice(voiceName)',
    '@JavascriptInterface fun selectTtsVoice(voiceName: String): String = this@MainActivity.selectTtsVoice(voiceName)',
    "selectTtsVoice bridge recursion"
)
main=req(
    main,
    '@JavascriptInterface fun setNeuralTtsRate(rate: Double): String = setNeuralTtsRate(rate.toFloat())',
    '@JavascriptInterface fun setNeuralTtsRate(rate: Double): String = this@MainActivity.setNeuralTtsRate(rate.toFloat())',
    "setNeuralTtsRate bridge recursion"
)

# ---------------------------------------------------------------------------
# 2) KOKORO QUEUE STARTUP
# The neural path used QUEUE_ADD even for block 0. That means a slow/stale
# prewarm utterance can remain in front of the real test/narration forever.
# The user's rule is "no stop/QUEUE_FLUSH BETWEEN blocks", so use FLUSH only
# once for block 0; every subsequent block remains QUEUE_ADD and is started
# only by the real onDone() of the previous block.
# ---------------------------------------------------------------------------
old='val mode=if(neural)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH;val result=e?.speak(s.text,mode,params,id)?:TextToSpeech.ERROR'
new='val mode=if(neural&&p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH;val result=e?.speak(s.text,mode,params,id)?:TextToSpeech.ERROR'
main=req(main,old,new,"neural first-block queue mode")

# The old 50 ms synthetic prime is redundant once we already asynchronously
# prewarm the engine and can itself create a queue item ahead of block 0.
main=main.replace(
    'if(neural&&p.index==0)e?.playSilentUtterance(50L,TextToSpeech.QUEUE_ADD,"$rootId::prime");',
    ''
)

write(main_rel,main)

# ---------------------------------------------------------------------------
# 3) USER-SELECTABLE PRIMARY NARRATION BACKEND: CHARON OR KOKORO
# Persisted in localStorage so it survives app restarts and does not touch DB.
# ---------------------------------------------------------------------------
settings_rel="components/SettingsView.tsx"
s=read(settings_rel)

# Add local preference state next to Android TTS state.
anchor="  const [androidTtsCatalog,setAndroidTtsCatalog]=useState<any>(null);"
if anchor not in s:
    raise SystemExit("Android TTS settings state anchor missing")
pref="""  const [narrationBackend,setNarrationBackend]=useState<'charon'|'kokoro'>(()=>localStorage.getItem('braseiro.narrationVoiceBackend')==='kokoro'?'kokoro':'charon');
  const changeNarrationBackend=(value:'charon'|'kokoro')=>{setNarrationBackend(value);localStorage.setItem('braseiro.narrationVoiceBackend',value);setAndroidTtsStatus(value==='kokoro'?'Narração principal: Kokoro / TTS local.':'Narração principal: Charon / Gemini TTS.');};
"""
if "braseiro.narrationVoiceBackend" not in s:
    s=s.replace(anchor,pref+anchor,1)

# Insert preference selector at the top of the TTS Android/Kokoro card.
card_anchor='<p className="mt-1 text-xs text-slate-400">Motores e vozes vêm do Android. Samsung/Google mantêm o fluxo padrão; Sherpa/Kokoro usa perfil neural local.</p>'
if card_anchor not in s:
    raise SystemExit("TTS card description anchor missing")
selector=card_anchor+"""            <label className="mt-3 block text-xs font-bold text-violet-100">Preferência de narração</label>
            <select value={narrationBackend} onChange={e=>changeNarrationBackend(e.target.value as 'charon'|'kokoro')} className="mt-1 min-h-11 w-full rounded border border-violet-900 bg-[#030712] px-2 text-sm text-violet-50">
              <option value="charon">Charon · Gemini TTS</option>
              <option value="kokoro">Kokoro · TTS local selecionado</option>
            </select>
            <p className="mt-1 text-[11px] text-slate-500">{narrationBackend==='kokoro'?'O botão Ouvir narração usará o motor local selecionado abaixo.':'O botão Ouvir narração usará Charon; o Kokoro continua disponível para teste local.'}</p>"""
s=s.replace(card_anchor,selector,1)
write(settings_rel,s)

# Route the main "Ouvir narração" action through the saved preference.
app_rel="App.tsx"
app=read(app_rel)
old_handle="""  const handlePlayNarration = async (sceneNode?: SceneNode) => {
    const node = sceneNode || campaignRef.current?.history.at(-1);
    if (node?.atmospheric_narration) await playCharonNarration(node);
  };"""
new_handle="""  const handlePlayNarration = async (sceneNode?: SceneNode) => {
    const node = sceneNode || campaignRef.current?.history.at(-1);
    if (!node?.atmospheric_narration) return;
    const backend = localStorage.getItem('braseiro.narrationVoiceBackend') === 'kokoro' ? 'kokoro' : 'charon';
    if (backend === 'kokoro') await playAndroidNarration(node);
    else await playCharonNarration(node);
  };"""
app=req(app,old_handle,new_handle,"handlePlayNarration preference")
write(app_rel,app)

# ---------------------------------------------------------------------------
# VERSION 4.9.29
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"));data["version"]="4.9.29"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.28"','"version": "4.9.29"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
t=read(version_rel)
t=re.sub(r"APP_VERSION = '4\.9\.28'","APP_VERSION = '4.9.29'",t)
t=re.sub(r"APP_VERSION_CODE = 49028","APP_VERSION_CODE = 49029",t)
write(version_rel,t)

gradle_rel="android/app/build.gradle.kts"
t=read(gradle_rel)
t=re.sub(r"versionCode\s*=\s*49028","versionCode = 49029",t)
t=re.sub(r'versionName\s*=\s*"4\.9\.28"','versionName = "4.9.29"',t)
write(gradle_rel,t)

main=read(main_rel)
main=main.replace("index.html?v=49028&immersive=1","index.html?v=49029&immersive=1")
write(main_rel,main)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
main=read(main_rel); s=read(settings_rel); app=read(app_rel)
checks={
 "voice-bridge-qualified": 'this@MainActivity.selectTtsVoice(voiceName)' in main,
 "rate-bridge-qualified": 'this@MainActivity.setNeuralTtsRate(rate.toFloat())' in main,
 "neural-first-flush-only": 'if(neural&&p.index>0)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH' in main,
 "no-silent-prime": 'playSilentUtterance(50L,TextToSpeech.QUEUE_ADD' not in main,
 "real-onDone-chain": 'override fun onDone' in main and 'advanceSpeechQueue(p.rootId)' in main,
 "neural-watchdog-disabled": 'if(isNeuralTtsProfile())return' in main,
 "voice-preference-setting": "braseiro.narrationVoiceBackend" in s and "Charon · Gemini TTS" in s and "Kokoro · TTS local selecionado" in s,
 "voice-preference-routing": "braseiro.narrationVoiceBackend" in app and "await playAndroidNarration(node)" in app and "await playCharonNarration(node)" in app,
 "tts-engine-discovery-preserved": 'discoverTtsEngines' in main and 'android.intent.action.TTS_SERVICE' in read("android/app/src/main/AndroidManifest.xml"),
 "rest-handler-preserved": "const handlePartyRested = async (kind: 'short' | 'long') => {" in app,
 "version": "4.9.29" in read(version_rel) and "49029" in read(gradle_rel),
}
for k,v in checks.items(): print(("PASS" if v else "FAIL"),k)
bad=[k for k,v in checks.items() if not v]
if bad: raise SystemExit("PATCH_4929_FAILED: "+", ".join(bad))
print("PATCH_OUA_4.9.29_KOKORO_VOICE_AND_PREFERENCE_OK")
