from pathlib import Path
import json,re,sys

if len(sys.argv)!=2:
    raise SystemExit("usage: patch_4928.py <root>")

root=Path(sys.argv[1]).resolve()

def read(p):
    return (root/p).read_text(encoding="utf-8")

def write(p,s):
    (root/p).write_text(s,encoding="utf-8")

def req(s,a,b,label):
    if a not in s:
        raise SystemExit("missing anchor: "+label)
    return s.replace(a,b,1)

# ---------------------------------------------------------------------------
# ANDROID TTS ENGINE DISCOVERY HOTFIX
# Problem on Android 11+:
# without package-visibility queries, TextToSpeech.getEngines()/engines can
# expose only the default/Google engine. The installed Sherpa/Kokoro engine
# com.k2fsa.sherpa.onnx.tts.engine then never reaches the VTT selector.
# Fix both layers:
#   1) Manifest <queries> for TTS_SERVICE + exact Sherpa package.
#   2) Runtime discovery merges TextToSpeech.engines, PackageManager TTS
#      services, and the known Sherpa/Kokoro package when installed.
# ---------------------------------------------------------------------------

manifest_rel="android/app/src/main/AndroidManifest.xml"
manifest=read(manifest_rel)

if "android.intent.action.TTS_SERVICE" not in manifest:
    application_anchor="<application"
    idx=manifest.find(application_anchor)
    if idx<0:
        raise SystemExit("AndroidManifest application anchor missing")
    queries="""    <queries>
        <intent>
            <action android:name="android.intent.action.TTS_SERVICE" />
        </intent>
        <package android:name="com.k2fsa.sherpa.onnx.tts.engine" />
    </queries>

"""
    manifest=manifest[:idx]+queries+manifest[idx:]
elif "com.k2fsa.sherpa.onnx.tts.engine" not in manifest:
    qend=manifest.find("</queries>")
    if qend<0:
        raise SystemExit("queries closing tag missing")
    manifest=manifest[:qend]+'        <package android:name="com.k2fsa.sherpa.onnx.tts.engine" />\n    '+manifest[qend:]

write(manifest_rel,manifest)

main_rel="android/app/src/main/java/com/braseiro/oumanel/MainActivity.kt"
main=read(main_rel)

# Insert robust engine discovery helper immediately before onInit.
oninit_anchor="    override fun onInit(status: Int) {"
if "private fun discoverTtsEngines()" not in main:
    helper=r'''    private fun discoverTtsEngines(): List<Pair<String,String>> {
        val found = linkedMapOf<String,String>()
        textToSpeech?.engines.orEmpty().forEach { info ->
            val pkg = info.name.orEmpty().trim()
            if (pkg.isNotBlank()) found[pkg] = info.label.orEmpty().ifBlank { pkg }
        }

        runCatching {
            packageManager.queryIntentServices(
                android.content.Intent("android.intent.action.TTS_SERVICE"),
                0
            )
        }.getOrDefault(emptyList()).forEach { resolve ->
            val service = resolve.serviceInfo ?: return@forEach
            val pkg = service.packageName.orEmpty().trim()
            if (pkg.isBlank()) return@forEach
            val label = runCatching { resolve.loadLabel(packageManager)?.toString().orEmpty() }.getOrDefault("")
            found.putIfAbsent(pkg, label.ifBlank { pkg })
        }

        val sherpaPackage = "com.k2fsa.sherpa.onnx.tts.engine"
        runCatching { packageManager.getApplicationInfo(sherpaPackage, 0) }.getOrNull()?.let { app ->
            val label = runCatching { packageManager.getApplicationLabel(app).toString() }.getOrDefault("")
            found.putIfAbsent(sherpaPackage, label.ifBlank { "SherpaOnnx TTS Engine" })
        }

        return found.entries
            .map { it.key to it.value }
            .sortedBy { it.second.lowercase(Locale.ROOT) }
    }

    private fun isInstalledTtsEnginePackage(pkg:String):Boolean {
        val requested = pkg.trim()
        if (requested.isBlank()) return true
        return discoverTtsEngines().any { it.first == requested }
    }

'''
    main=req(main,oninit_anchor,helper+oninit_anchor,"onInit insert")

# Saved engine restore must not clear Sherpa merely because TextToSpeech.engines
# was incomplete.
main=main.replace(
    'if (textToSpeech?.engines.orEmpty().any { it.name==saved }) { switchTtsEngine(saved,null); return }',
    'if (isInstalledTtsEnginePackage(saved)) { switchTtsEngine(saved,null); return }'
)

# Resolve selected engine label through merged discovery, not only getEngines().
main=main.replace(
    'activeTtsEngineLabel=e.engines.orEmpty().firstOrNull{it.name==activeTtsEnginePackage}?.label.orEmpty()',
    'activeTtsEngineLabel=discoverTtsEngines().firstOrNull{it.first==activeTtsEnginePackage}?.second.orEmpty()'
)

# Engine selection validation must accept a visible PackageManager TTS service.
main=main.replace(
    'if(requested.isNotBlank()&&old?.engines.orEmpty().none{it.name==requested}){deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: $requested");return}',
    'if(requested.isNotBlank()&&!isInstalledTtsEnginePackage(requested)){deliverTtsConfigCallback(callbackId,"Motor TTS não instalado: $requested");return}'
)

# Catalog must expose the merged list. Keep Android default as its own choice.
old_catalog='e?.engines.orEmpty().sortedBy{it.label.lowercase(Locale.ROOT)}.forEach{i->engines.put(JSONObject().put("packageName",i.name).put("label",i.label).put("selected",i.name==activeTtsEnginePackage).put("neuralProfile",isNeuralEngineIdentity(i.name,i.label)))}'
new_catalog='discoverTtsEngines().forEach{(pkg,label)->engines.put(JSONObject().put("packageName",pkg).put("label",label).put("selected",pkg==activeTtsEnginePackage).put("neuralProfile",isNeuralEngineIdentity(pkg,label)))}'
if old_catalog not in main:
    raise SystemExit("tts catalog engine enumeration anchor missing")
main=main.replace(old_catalog,new_catalog,1)

write(main_rel,main)

# ---------------------------------------------------------------------------
# VERSION 4.9.28
# ---------------------------------------------------------------------------
pkg=root/"package.json"
data=json.loads(pkg.read_text(encoding="utf-8"))
data["version"]="4.9.28"
pkg.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")

lock=root/"package-lock.json"
if lock.exists():
    t=lock.read_text(encoding="utf-8")
    t=re.sub(r'"version":\s*"4\.9\.27"','"version": "4.9.28"',t,count=2)
    lock.write_text(t,encoding="utf-8")

version_rel="lib/version.ts"
version=read(version_rel)
version=re.sub(r"APP_VERSION = '4\.9\.27'","APP_VERSION = '4.9.28'",version)
version=re.sub(r"APP_VERSION_CODE = 49027","APP_VERSION_CODE = 49028",version)
write(version_rel,version)

gradle_rel="android/app/build.gradle.kts"
gradle=read(gradle_rel)
gradle=re.sub(r"versionCode\s*=\s*49027","versionCode = 49028",gradle)
gradle=re.sub(r'versionName\s*=\s*"4\.9\.27"','versionName = "4.9.28"',gradle)
write(gradle_rel,gradle)

main=read(main_rel)
main=main.replace("index.html?v=49027&immersive=1","index.html?v=49028&immersive=1")
write(main_rel,main)

# ---------------------------------------------------------------------------
# GATES
# ---------------------------------------------------------------------------
main=read(main_rel)
manifest=read(manifest_rel)
app=read("App.tsx")
checks={
    "manifest-tts-service-query": 'android.intent.action.TTS_SERVICE' in manifest,
    "manifest-sherpa-package-query": 'com.k2fsa.sherpa.onnx.tts.engine' in manifest,
    "runtime-merged-discovery": 'private fun discoverTtsEngines()' in main and 'queryIntentServices' in main,
    "runtime-known-sherpa-fallback": 'packageManager.getApplicationInfo(sherpaPackage, 0)' in main,
    "restore-uses-merged-discovery": 'if (isInstalledTtsEnginePackage(saved))' in main,
    "switch-uses-merged-discovery": '!isInstalledTtsEnginePackage(requested)' in main,
    "catalog-uses-merged-discovery": 'discoverTtsEngines().forEach{(pkg,label)' in main,
    "kokoro-profile-preserved": 'com.k2fsa.sherpa.onnx.tts.engine' in main and 'isNeuralTtsProfile' in main,
    "kokoro-queue-preserved": 'if(neural)TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH' in main,
    "rest-handler-still-defined": "const handlePartyRested = async (kind: 'short' | 'long') => {" in app,
    "version": "4.9.28" in read(version_rel) and "49028" in read(gradle_rel),
}
for name,ok in checks.items():
    print(("PASS" if ok else "FAIL"),name)
failed=[name for name,ok in checks.items() if not ok]
if failed:
    raise SystemExit("PATCH_4928_FAILED: "+", ".join(failed))

print("PATCH_OUA_4.9.28_TTS_ENGINE_DISCOVERY_OK")
