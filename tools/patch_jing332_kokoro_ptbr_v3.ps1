
param(
  [string]$WorkDir = "$env:RUNNER_TEMP\jing332-sherpa-kokoro"
)

$ErrorActionPreference = "Stop"
$nl = [Environment]::NewLine

if (Test-Path $WorkDir) { Remove-Item $WorkDir -Recurse -Force }
git clone --no-checkout https://github.com/jing332/SherpaOnnxTtsEngineAndroid.git $WorkDir
Set-Location $WorkDir
git fetch --depth 1 origin 5d91a72ee8a58dadeb191120d34df54d52fb190b
git checkout --detach 5d91a72ee8a58dadeb191120d34df54d52fb190b

Write-Host "Using exact upstream commit:"
git rev-parse HEAD

Remove-Item "app\src\main\java\com\k2fsa\sherpa\onnx\Tts.kt" -Force
Remove-Item "app\src\main\jniLibs" -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path "app\libs" | Out-Null
$aar = "app\libs\sherpa-onnx-1.13.8.aar"
& curl.exe -L --fail --retry 4 --retry-delay 3 -o $aar "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar"
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $aar)) { throw "Failed to download sherpa-onnx 1.13.8 AAR" }

$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
if ($gradle -notmatch 'sherpa-onnx-1\.13\.8\.aar') {
  $gradle = $gradle.Replace("dependencies {", "dependencies {" + $nl + '    implementation(files("libs/sherpa-onnx-1.13.8.aar"))')
  Set-Content -Path $gradlePath -Value $gradle -Encoding utf8
}

$configManager = @'
package com.k2fsa.sherpa.onnx.tts.engine.synthesizer

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import com.k2fsa.sherpa.onnx.tts.engine.FileConst
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.config.ConfigManager
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.config.Model
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.config.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object ConfigModelManager {
    const val TAG = "ModelManager"

    private val models = mutableListOf<Model>()

    fun models(): List<Model> = models

    private val _modelsFlow by lazy { MutableStateFlow<List<Model>>(emptyList()) }
    val modelsFlow: StateFlow<List<Model>>
        get() = _modelsFlow.asStateFlow()

    private fun notifyModelsChange() {
        Log.d(TAG, "notifyModelsChange: " + models.size)
        _modelsFlow.value = models.toList()
    }

    private fun modelDir(model: Model): File = File(FileConst.modelDir, model.id)
    private fun isKokoro(model: Model): Boolean = File(modelDir(model), "voices.bin").isFile

    private fun normalizeKokoroLanguage(lang: String): String {
        val value = lang.lowercase().replace('_', '-')
        return when {
            value.startsWith("pt-br") || value == "pt" -> "pt-br"
            value.startsWith("en") -> "en"
            value.startsWith("es") -> "es"
            value.startsWith("fr") -> "fr"
            value.startsWith("hi") -> "hi"
            value.startsWith("it") -> "it"
            else -> value.substringBefore('-')
        }
    }

    private fun addPtBrKokoroVoices(model: Model) {
        if (!isKokoro(model) || !model.lang.startsWith("pt", ignoreCase = true)) return

        val wanted = listOf(
            Voice(model = model.id, id = 42, name = "Dora (pt-BR)"),
            Voice(model = model.id, id = 43, name = "Alex (pt-BR)"),
            Voice(model = model.id, id = 44, name = "Santa (pt-BR)"),
        )
        val configured = (ConfigVoiceManager.speakers() + ConfigManager.config.voices)
            .map { it.toString() }
            .toSet()
        wanted.filter { it.toString() !in configured }.forEach { ConfigVoiceManager.add(it) }
    }

    @Synchronized
    fun load() {
        if (models.isEmpty()) {
            models.addAll(ConfigManager.config.models)
        }

        var upgraded = false
        val upgradedModels = models.map { model ->
            if (isKokoro(model) && model.lang.equals("en-US", ignoreCase = true)) {
                upgraded = true
                model.copy(lang = "pt-BR")
            } else {
                model
            }
        }
        if (upgraded) {
            models.clear()
            models.addAll(upgradedModels)
        }

        instinctModels()
        models.forEach(::addPtBrKokoroVoices)
        notifyModelsChange()
    }

    fun languages(): List<String> {
        return models.distinctBy { it.lang }.map { it.lang }
    }

    fun defaultLanguage(): String {
        return models.firstOrNull()?.lang ?: "en"
    }

    @Synchronized
    private fun instinctModels() {
        val list = models.distinctBy { it.id }
        models.clear()
        models.addAll(list)
        ConfigManager.updateConfig(ConfigManager.config.copy(models = models))
    }

    @Synchronized
    fun removeModel(vararg model: Model) {
        models.removeAll(model.toSet())
        ConfigManager.updateConfig(ConfigManager.config.copy(models = models))
        notifyModelsChange()
    }

    @Synchronized
    fun addModel(vararg model: Model) {
        model.forEach { m0 ->
            val m = if (isKokoro(m0) && m0.lang.equals("en-US", ignoreCase = true)) {
                m0.copy(lang = "pt-BR")
            } else {
                m0
            }
            if (!models.any { it.id == m.id }) {
                models.add(m)
                addPtBrKokoroVoices(m)
            }
        }

        ConfigManager.updateConfig(ConfigManager.config.copy(models = models))
        notifyModelsChange()
    }

    fun updateModels(model: Model) {
        models.indexOfFirst { it.id == model.id }.takeIf { it != -1 }?.let {
            models[it] = model.copy()
            addPtBrKokoroVoices(models[it])
            ConfigManager.updateConfig(ConfigManager.config.copy(models = models))
            notifyModelsChange()
        }
    }

    @Synchronized
    fun updateModels(model: List<Model>) {
        models.clear()
        models.addAll(model)
        models.forEach(::addPtBrKokoroVoices)
        ConfigManager.updateConfig(ConfigManager.config.copy(models = models))
        notifyModelsChange()
    }

    fun getNotAddedModels(context: Context): List<Model> {
        val addedIds = models.map { it.id }
        return analyzeToModels().filter { it.id !in addedIds }
    }

    fun analyzeToModel(dir: File): Model? {
        Log.d(TAG, "load model: " + dir.name)
        val onnx = dir.listFiles { _, name -> name.endsWith(".onnx") }
            ?.run { if (isNotEmpty()) first() else null }
            ?: return null

        val dataDir = dir.resolve("espeak-ng-data").takeIf { it.exists() }
        val kokoro = dir.resolve("voices.bin").isFile

        return Model(
            id = dir.name,
            onnx = dir.name + "/" + onnx.name,
            lexicon = if (kokoro || dataDir != null) "" else dir.name + "/lexicon.txt",
            ruleFsts = if (kokoro || dataDir != null) "" else dir.name + "/rule.fst",
            tokens = dir.name + "/tokens.txt",
            dataDir = dataDir?.run { dir.name + "/espeak-ng-data" } ?: "",
            lang = if (kokoro) "pt-BR" else "en-US",
        )
    }

    fun analyzeToModels(): List<Model> {
        Log.d(TAG, "modelPath: " + FileConst.modelDir)
        val list = mutableListOf<Model>()
        File(FileConst.modelDir).listFiles()?.forEach { dir ->
            if (dir.isDirectory) {
                analyzeToModel(dir)?.let { list.add(it) }
            }
        }
        return list
    }

    fun Model.toOfflineTtsConfig(root: String = FileConst.modelDir): OfflineTtsConfig {
        fun format(str: String): String {
            return if (str.isBlank()) "" else root + "/" + str
        }

        val kokoro = isKokoro(this)
        val voicesPath = File(modelDir(this), "voices.bin").absolutePath

        return OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = if (kokoro) {
                    OfflineTtsVitsModelConfig()
                } else {
                    OfflineTtsVitsModelConfig(
                        model = format(onnx),
                        lexicon = format(lexicon),
                        tokens = format(tokens),
                        dataDir = format(dataDir),
                    )
                },
                kokoro = if (kokoro) {
                    OfflineTtsKokoroModelConfig(
                        model = format(onnx),
                        voices = voicesPath,
                        tokens = format(tokens),
                        dataDir = format(dataDir),
                        lexicon = format(lexicon),
                        lang = normalizeKokoroLanguage(lang),
                    )
                } else {
                    OfflineTtsKokoroModelConfig()
                },
                numThreads = 4,
                debug = true,
                provider = "cpu",
            ),
            ruleFsts = if (kokoro) "" else format(ruleFsts),
        )
    }
}
'@
Set-Content -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt" -Value $configManager -Encoding utf8

$synthManager = @'
package com.k2fsa.sherpa.onnx.tts.engine.synthesizer

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.tts.engine.conf.TtsConfig
import kotlin.math.max

object SynthesizerManager {
    const val TAG = "SynthesizerManager"

    private val cacheManager = SynthesizerCache()

    private fun key(cfg: OfflineTtsConfig): String {
        return cfg.model.kokoro.model.ifBlank { cfg.model.vits.model }
    }

    fun getTTS(cfg: OfflineTtsConfig): OfflineTts {
        val cacheKey = key(cfg)
        val tts = (cacheManager.getById(cacheKey) as Synthesizer?)?.tts
        tts?.let {
            Log.d(TAG, "getTTS (from cache): " + it.config)
        }

        val model = cfg.model.copy(
            numThreads = max(1, TtsConfig.threadNum.value)
        )

        return tts ?: OfflineTts(config = cfg.copy(model = model)).run {
            cacheTTS(cacheKey, this)
            this
        }
    }

    private fun cacheTTS(id: String, tts: OfflineTts) {
        Log.d(TAG, "cacheTTS: " + tts.config)
        cacheManager.cache(id, Synthesizer(tts))
    }

    private class Synthesizer(val tts: OfflineTts) : ImplCache {
        override fun destroy() {
            tts.free()
        }

        override fun canDestroy(): Boolean = true
    }
}
'@
Set-Content -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Value $synthManager -Encoding utf8

$ttsServicePath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt"
$ttsService = Get-Content $ttsServicePath -Raw
$ttsService = $ttsService.Replace(
  "        ConfigModelManager.load()" + $nl + "        languages =",
  "        ConfigVoiceManager.load()" + $nl + "        ConfigModelManager.load()" + $nl + "        languages ="
)
$ttsService = $ttsService.Replace(
  "                offset += bytesToWrite" + $nl + "            }" + $nl + $nl + "        }",
  "                offset += bytesToWrite" + $nl + "            }" + $nl + "            1" + $nl + "        }"
)
Set-Content -Path $ttsServicePath -Value $ttsService -Encoding utf8

$auditionPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\ui\AuditionDialog.kt"
$audition = Get-Content $auditionPath -Raw
$audition = $audition.Replace(
  "                track.write(it, 0, it.size, AudioTrack.WRITE_BLOCKING)" + $nl + "            }",
  "                track.write(it, 0, it.size, AudioTrack.WRITE_BLOCKING)" + $nl + "                1" + $nl + "            }"
)
Set-Content -Path $auditionPath -Value $audition -Encoding utf8

Write-Host "Patched project. Kokoro support checks:"
Select-String -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt" -Pattern "OfflineTtsKokoroModelConfig|voices.bin|pt-br"
Select-String -Path "app\build.gradle.kts" -Pattern "sherpa-onnx-1.13.8.aar"


# ============================================================
# FLOW FIX V2 -- preserve the already-working external-model
# architecture and only fix Kokoro flow/runtime behavior.
# ============================================================

Write-Host "Applying Kokoro PT-BR flow fix V3..."

# Make the update install above 1.26.092312_debug while preserving the same
# package and the BRELGA debug signing identity.
$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
$gradle = $gradle.Replace("versionCode = gitCommits", "versionCode = gitCommits + 1000")
Set-Content -Path $gradlePath -Value $gradle -Encoding utf8

# 1) Sherpa sentence/pause profile.
$configPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt"
$config = Get-Content $configPath -Raw
$needle = '            ruleFsts = if (kokoro) "" else format(ruleFsts),' + [Environment]::NewLine + '        )'
$replacement = '            ruleFsts = if (kokoro) "" else format(ruleFsts),' + [Environment]::NewLine +
  '            maxNumSentences = if (kokoro) 2 else 1,' + [Environment]::NewLine +
  '            silenceScale = if (kokoro) 0.06f else 0.2f,' + [Environment]::NewLine +
  '        )'
if (-not $config.Contains($needle)) { throw "ConfigModelManager OfflineTtsConfig marker not found" }
$config = $config.Replace($needle, $replacement)
Set-Content -Path $configPath -Value $config -Encoding utf8

# 2) Keep Kokoro resident permanently; other engines keep normal cache behavior.
$synthManager = @'
package com.k2fsa.sherpa.onnx.tts.engine.synthesizer

import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.tts.engine.conf.TtsConfig
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

object SynthesizerManager {
    const val TAG = "SynthesizerManager"

    private val cacheManager = SynthesizerCache()
    private val persistentKokoro = ConcurrentHashMap<String, OfflineTts>()

    private fun key(cfg: OfflineTtsConfig): String {
        return cfg.model.kokoro.model.ifBlank { cfg.model.vits.model }
    }

    private fun isKokoro(cfg: OfflineTtsConfig): Boolean =
        cfg.model.kokoro.model.isNotBlank()

    private fun withThreads(cfg: OfflineTtsConfig): OfflineTtsConfig {
        val threads = if (isKokoro(cfg)) 4 else max(1, TtsConfig.threadNum.value)
        return cfg.copy(model = cfg.model.copy(numThreads = threads))
    }

    @Synchronized
    fun getTTS(cfg: OfflineTtsConfig): OfflineTts {
        val cacheKey = key(cfg)

        if (isKokoro(cfg)) {
            persistentKokoro[cacheKey]?.let {
                Log.d(TAG, "getTTS Kokoro resident: " + it.config)
                return it
            }

            val created = OfflineTts(config = withThreads(cfg))
            persistentKokoro[cacheKey] = created
            Log.d(TAG, "Kokoro loaded and pinned resident: " + created.config)
            return created
        }

        val cached = (cacheManager.getById(cacheKey) as Synthesizer?)?.tts
        cached?.let {
            Log.d(TAG, "getTTS (from cache): " + it.config)
            return it
        }

        return OfflineTts(config = withThreads(cfg)).run {
            cacheManager.cache(cacheKey, Synthesizer(this))
            this
        }
    }

    private class Synthesizer(val tts: OfflineTts) : ImplCache {
        override fun destroy() {
            tts.free()
        }

        override fun canDestroy(): Boolean = !tts.isRunning
    }
}
'@
Set-Content -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Value $synthManager -Encoding utf8

# 3) Default thread setting shown by the engine also becomes 4.
$ttsConfigPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\conf\TtsConfig.kt"
$ttsConfig = Get-Content $ttsConfigPath -Raw
$ttsConfig = $ttsConfig.Replace("initialValue = 2", "initialValue = 4")
Set-Content -Path $ttsConfigPath -Value $ttsConfig -Encoding utf8

# 4) Native TTS service: pre-load/prewarm off the UI thread, stream immediately,
# protect the first phoneme, and only stop when Android explicitly asks to stop.
$ttsServicePath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt"
$ttsService = Get-Content $ttsServicePath -Raw

if ($ttsService -notmatch 'com\.k2fsa\.sherpa\.onnx\.GenerationConfig') {
  $ttsService = $ttsService.Replace(
    "import android.util.Log",
    "import android.util.Log" + [Environment]::NewLine + "import com.k2fsa.sherpa.onnx.GenerationConfig"
  )
}

$ttsService = $ttsService.Replace(
  "    private var languages: List<Locale> = emptyList()",
  "    private var languages: List<Locale> = emptyList()" + [Environment]::NewLine +
  "    @Volatile private var stopRequested = false" + [Environment]::NewLine +
  "    private val generationLock = Any()"
)

$createNeedle = "        languages = ConfigModelManager.languages().map { newLocaleFromCode(it) }" +
  [Environment]::NewLine + [Environment]::NewLine + "        super.onCreate()"
$createReplacement = "        languages = ConfigModelManager.languages().map { newLocaleFromCode(it) }" +
  [Environment]::NewLine + [Environment]::NewLine + "        super.onCreate()" +
  [Environment]::NewLine + [Environment]::NewLine +
  "        Thread({" + [Environment]::NewLine +
  "            runCatching {" + [Environment]::NewLine +
  "                val selectedModelId = TtsConfig.voice.value.model" + [Environment]::NewLine +
  "                val model = ConfigModelManager.models().firstOrNull { it.id == selectedModelId }" + [Environment]::NewLine +
  "                    ?: ConfigModelManager.models().firstOrNull()" + [Environment]::NewLine +
  "                if (model != null && java.io.File(com.k2fsa.sherpa.onnx.tts.engine.FileConst.modelDir, model.id + \"/voices.bin\").isFile) {" + [Environment]::NewLine +
  "                    val tts = SynthesizerManager.getTTS(model.toOfflineTtsConfig())" + [Environment]::NewLine +
  "                    synchronized(generationLock) {" + [Environment]::NewLine +
  "                        if (!tts.isRunning) {" + [Environment]::NewLine +
  "                            tts.generateWithConfigAndCallback(" + [Environment]::NewLine +
  "                                text = \"Olá.\"," + [Environment]::NewLine +
  "                                config = GenerationConfig(" + [Environment]::NewLine +
  "                                    sid = TtsConfig.voice.value.id," + [Environment]::NewLine +
  "                                    speed = 1.0f," + [Environment]::NewLine +
  "                                    silenceScale = 0.06f," + [Environment]::NewLine +
  "                                )," + [Environment]::NewLine +
  "                                callback = { 1 }," + [Environment]::NewLine +
  "                            )" + [Environment]::NewLine +
  "                        }" + [Environment]::NewLine +
  "                    }" + [Environment]::NewLine +
  "                }" + [Environment]::NewLine +
  "            }.onFailure { Log.w(TAG, \"Kokoro prewarm skipped/failed\", it) }" + [Environment]::NewLine +
  "        }, \"kokoro-prewarm\").start()"

if (-not $ttsService.Contains($createNeedle)) { throw "TtsService onCreate marker not found" }
$ttsService = $ttsService.Replace($createNeedle, $createReplacement)

$ttsService = $ttsService.Replace(
  "    override fun onStop() {}",
  "    override fun onStop() {" + [Environment]::NewLine +
  "        stopRequested = true" + [Environment]::NewLine +
  "    }"
)

$startNeedle = "        if (request == null || callback == null) {" + [Environment]::NewLine +
  "            return" + [Environment]::NewLine +
  "        }"
$startReplacement = $startNeedle + [Environment]::NewLine +
  "        stopRequested = false"
if (-not $ttsService.Contains($startNeedle)) { throw "TtsService request marker not found" }
$ttsService = $ttsService.Replace($startNeedle, $startReplacement)

$callbackPattern = '(?s)        val ttsCallback = \{ floatSamples: FloatArray ->.*?        callback\.done\(\)'
$callbackReplacement = @'
        var firstAudio = true
        val ttsCallback: (FloatArray) -> Int = { floatSamples ->
            if (stopRequested) {
                0
            } else {
                val samples = floatSamples.toByteArray()
                val targetBytes = min(
                    callback.maxBufferSize,
                    maxOf(2048, (tts.sampleRate() * 2 * 0.12f).toInt())
                )

                if (firstAudio) {
                    firstAudio = false
                    val prime = ByteArray((tts.sampleRate() * 2 * 0.05f).toInt())
                    var p = 0
                    while (p < prime.size && !stopRequested) {
                        val n = min(targetBytes, prime.size - p)
                        callback.audioAvailable(prime, p, n)
                        p += n
                    }
                }

                var offset = 0
                while (offset < samples.size && !stopRequested) {
                    val bytesToWrite = min(targetBytes, samples.size - offset)
                    callback.audioAvailable(samples, offset, bytesToWrite)
                    offset += bytesToWrite
                }

                if (stopRequested) 0 else 1
            }
        }

        val speed = (request.speechRate / 100f).coerceIn(0.55f, 1.20f)
        Log.i(
            TAG,
            "voice: ${voice}, text: $text, speechRate: ${request.speechRate} (speed: $speed)"
        )

        synchronized(generationLock) {
            if (!stopRequested) {
                tts.generateWithConfigAndCallback(
                    text = text,
                    config = GenerationConfig(
                        sid = voice.id,
                        speed = speed,
                        silenceScale = 0.06f,
                    ),
                    callback = ttsCallback,
                )
            }
        }

        if (!stopRequested) {
            callback.done()
        }
'@

$newTtsService = [regex]::Replace($ttsService, $callbackPattern, $callbackReplacement, 1)
if ($newTtsService -eq $ttsService) { throw "TtsService streaming block replacement failed" }
Set-Content -Path $ttsServicePath -Value $newTtsService -Encoding utf8

Write-Host "Flow fix V3 base checks:"
Select-String -Path $configPath -Pattern "maxNumSentences = if \(kokoro\) 2 else 1|silenceScale = if \(kokoro\) 0.06f"
Select-String -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Pattern "persistentKokoro|numThreads"
Select-String -Path $ttsServicePath -Pattern "generateWithConfigAndCallback|kokoro-prewarm|0.05f|0.12f"
Select-String -Path $gradlePath -Pattern "versionCode = gitCommits \+ 1000"


# ============================================================
# FLOW FIX V3 -- make Android Voice selection real and reliable.
# The Android TTS API exposes each Sherpa voice as <modelId>_<speakerId>.
# Ensure there is always a valid default and return that real name to Android.
# ============================================================

Write-Host "Applying Android Voice/default fix V3..."

$ttsServicePath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt"
$ttsService = Get-Content $ttsServicePath -Raw

$loadMarker = "        ConfigVoiceManager.load()" + [Environment]::NewLine +
  "        ConfigModelManager.load()" + [Environment]::NewLine +
  "        languages = ConfigModelManager.languages().map { newLocaleFromCode(it) }"

$loadReplacement = "        ConfigVoiceManager.load()" + [Environment]::NewLine +
  "        ConfigModelManager.load()" + [Environment]::NewLine +
  "        val configuredVoices = ConfigVoiceManager.speakers()" + [Environment]::NewLine +
  "        val selectedVoice = TtsConfig.voice.value" + [Environment]::NewLine +
  "        if (configuredVoices.none { it.toString() == selectedVoice.toString() }) {" + [Environment]::NewLine +
  "            configuredVoices.firstOrNull()?.let { TtsConfig.voice.value = it }" + [Environment]::NewLine +
  "        }" + [Environment]::NewLine +
  "        languages = ConfigModelManager.languages().map { newLocaleFromCode(it) }"

if (-not $ttsService.Contains($loadMarker)) { throw "V3 ConfigVoice/default marker not found" }
$ttsService = $ttsService.Replace($loadMarker, $loadReplacement)

$defaultOld = @'
    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        Log.i(TAG, "onGetDefaultVoiceNameFor: $lang, $country, $variant")
        return NOT_SET_VOICE_NAME
    }
'@

$defaultNew = @'
    override fun onGetDefaultVoiceNameFor(
        lang: String?,
        country: String?,
        variant: String?
    ): String {
        Log.i(TAG, "onGetDefaultVoiceNameFor: $lang, $country, $variant")
        val configured = ConfigVoiceManager.speakers()
        val selected = TtsConfig.voice.value
        return configured.firstOrNull { it.toString() == selected.toString() }?.toString()
            ?: configured.firstOrNull()?.toString()
            ?: NOT_SET_VOICE_NAME
    }
'@

if (-not $ttsService.Contains($defaultOld)) { throw "V3 default voice method marker not found" }
$ttsService = $ttsService.Replace($defaultOld, $defaultNew)

Set-Content -Path $ttsServicePath -Value $ttsService -Encoding utf8

Write-Host "V3 final checks:"
Select-String -Path $ttsServicePath -Pattern "configuredVoices|onGetDefaultVoiceNameFor|generateWithConfigAndCallback|stopRequested"
Select-String -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Pattern "persistentKokoro"

# V3 build trigger: real Android Voice names are authoritative.
