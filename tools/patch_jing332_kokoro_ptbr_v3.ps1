
param(
  [string]$WorkDir = "$env:RUNNER_TEMP\jing332-sherpa-kokoro"
)

$ErrorActionPreference = "Stop"
$nl = [Environment]::NewLine

if (Test-Path $WorkDir) { Remove-Item $WorkDir -Recurse -Force }
git clone --depth 1 https://github.com/jing332/SherpaOnnxTtsEngineAndroid.git $WorkDir
Set-Location $WorkDir

Write-Host "Using upstream commit:"
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
# FLOW FIX V3
# Keep the V1 external-model architecture above, then replace only the
# runtime flow with a real Kokoro streaming implementation.
# ============================================================

Write-Host "Applying Kokoro PT-BR Flow Fix V3..."

$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
$gradle = $gradle.Replace("versionCode = gitCommits", "versionCode = gitCommits + 2000")
Set-Content -Path $gradlePath -Value $gradle -Encoding utf8

$configPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt"
$config = Get-Content $configPath -Raw
$oldConfigTail = @'
            ruleFsts = if (kokoro) "" else format(ruleFsts),
        )
'@
$newConfigTail = @'
            ruleFsts = if (kokoro) "" else format(ruleFsts),
            maxNumSentences = if (kokoro) 2 else 1,
            silenceScale = if (kokoro) 0.06f else 0.2f,
        )
'@
if (-not $config.Contains($oldConfigTail)) { throw "OfflineTtsConfig profile marker not found" }
$config = $config.Replace($oldConfigTail, $newConfigTail)
Set-Content -Path $configPath -Value $config -Encoding utf8

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

    private fun key(cfg: OfflineTtsConfig): String =
        cfg.model.kokoro.model.ifBlank { cfg.model.vits.model }

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
                Log.d(TAG, "getTTS Kokoro resident: ${it.config}")
                return it
            }
            val created = OfflineTts(config = withThreads(cfg))
            persistentKokoro[cacheKey] = created
            Log.d(TAG, "Kokoro loaded and pinned resident: ${created.config}")
            return created
        }

        val cached = (cacheManager.getById(cacheKey) as Synthesizer?)?.tts
        cached?.let {
            Log.d(TAG, "getTTS from cache: ${it.config}")
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

        override fun canDestroy(): Boolean = true
    }
}
'@
Set-Content -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Value $synthManager -Encoding utf8

$ttsConfigPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\conf\TtsConfig.kt"
$ttsConfig = Get-Content $ttsConfigPath -Raw
$ttsConfig = $ttsConfig.Replace("initialValue = 2", "initialValue = 4")
Set-Content -Path $ttsConfigPath -Value $ttsConfig -Encoding utf8

$ttsService = @'
package com.k2fsa.sherpa.onnx.tts.engine.service

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.tts.engine.R
import com.k2fsa.sherpa.onnx.tts.engine.conf.TtsConfig
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.ConfigModelManager
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.ConfigModelManager.toOfflineTtsConfig
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.ConfigVoiceManager
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.SynthesizerManager
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.config.Model
import com.k2fsa.sherpa.onnx.tts.engine.synthesizer.config.Voice as EngineVoice
import com.k2fsa.sherpa.onnx.tts.engine.ui.TAG
import com.k2fsa.sherpa.onnx.tts.engine.utils.longToast
import com.k2fsa.sherpa.onnx.tts.engine.utils.newLocaleFromCode
import com.k2fsa.sherpa.onnx.tts.engine.utils.toByteArray
import java.io.File
import java.util.Locale
import kotlin.math.min

class TtsService : TextToSpeechService() {
    companion object {
        const val NOT_SET_VOICE_NAME = "NOT_SET_VOICE"
    }

    private var languages: List<Locale> = emptyList()
    @Volatile private var stopRequested = false
    private val generationLock = Any()

    override fun onCreate() {
        Log.i(TAG, "onCreate tts service FlowFixV3")

        ConfigVoiceManager.load()
        ConfigModelManager.load()

        val configured = ConfigVoiceManager.speakers()
        val selected = TtsConfig.voice.value
        if (configured.none { it.toString() == selected.toString() }) {
            configured.firstOrNull()?.let { TtsConfig.voice.value = it }
        }

        languages = ConfigModelManager.languages().map { newLocaleFromCode(it) }
        super.onCreate()

        Thread({
            runCatching { prewarmSelectedKokoro() }
                .onFailure { Log.w(TAG, "Kokoro internal prewarm skipped/failed", it) }
        }, "kokoro-prewarm").start()
    }

    override fun onDestroy() {
        stopRequested = true
        Log.i(TAG, "onDestroy tts service")
        super.onDestroy()
    }

    private fun validSelectedVoice(): EngineVoice? {
        val configured = ConfigVoiceManager.speakers()
        val selected = TtsConfig.voice.value
        return configured.firstOrNull { it.toString() == selected.toString() }
            ?: configured.firstOrNull()
    }

    private fun prewarmSelectedKokoro() {
        val voice = validSelectedVoice() ?: return
        val model = ConfigModelManager.models().firstOrNull { it.id == voice.model } ?: return
        val modelDir = File(com.k2fsa.sherpa.onnx.tts.engine.FileConst.modelDir, model.id)
        if (!File(modelDir, "voices.bin").isFile) return

        val tts = SynthesizerManager.getTTS(model.toOfflineTtsConfig())
        synchronized(generationLock) {
            tts.generateWithConfigAndCallback(
                text = "Olá.",
                config = GenerationConfig(
                    sid = voice.id,
                    speed = 1.0f,
                    silenceScale = 0.06f,
                ),
                callback = { 1 },
            )
        }
    }

    override fun onIsLanguageAvailable(_lang: String?, _country: String?, _variant: String?): Int {
        val lang = _lang ?: ""
        val country = _country ?: ""
        languages.forEach {
            val l = it.isO3Language
            val c = it.isO3Country
            if (l == lang && c == country) return TextToSpeech.LANG_COUNTRY_AVAILABLE
            if (l == lang) return TextToSpeech.LANG_AVAILABLE
        }
        return TextToSpeech.LANG_NOT_SUPPORTED
    }

    override fun onLoadLanguage(_lang: String?, _country: String?, _variant: String?): Int =
        onIsLanguageAvailable(_lang, _country, _variant)

    override fun onGetLanguage(): Array<String> {
        val locale = languages.firstOrNull() ?: Locale("pt", "BR")
        return arrayOf(locale.isO3Language, locale.isO3Country, "")
    }

    override fun onLoadVoice(voiceName: String?): Int = onIsValidVoiceName(voiceName)

    override fun onGetVoices(): MutableList<Voice> {
        val list = mutableListOf<Voice>()
        ConfigVoiceManager.speakers().forEach { speaker ->
            val model = ConfigModelManager.models().find { it.id == speaker.model } ?: return@forEach
            list.add(
                Voice(
                    model.id + "_" + speaker.id,
                    newLocaleFromCode(model.lang),
                    Voice.QUALITY_NORMAL,
                    Voice.LATENCY_NORMAL,
                    false,
                    setOf(speaker.name),
                )
            )
        }
        Log.i(TAG, "onGetVoices FlowFixV3: ${list.size}")
        return list
    }

    override fun onIsValidVoiceName(voiceName: String?): Int {
        if (voiceName.isNullOrBlank()) return TextToSpeech.ERROR
        if (voiceName == NOT_SET_VOICE_NAME) {
            return if (validSelectedVoice() != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR
        }
        val parsed = EngineVoice.from(voiceName)
        return if (
            ConfigVoiceManager.speakers().any { it.toString() == parsed.toString() } &&
            ConfigModelManager.models().any { it.id == parsed.model }
        ) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    }

    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String =
        validSelectedVoice()?.toString() ?: NOT_SET_VOICE_NAME

    override fun onStop() {
        stopRequested = true
    }

    private fun resolveRequestVoice(voiceName: String?): EngineVoice? {
        if (voiceName.isNullOrBlank() || voiceName == NOT_SET_VOICE_NAME) {
            return validSelectedVoice()
        }
        val parsed = EngineVoice.from(voiceName)
        return ConfigVoiceManager.speakers().firstOrNull { it.toString() == parsed.toString() }
    }

    private fun getTtsModel(voice: EngineVoice): Model? =
        ConfigModelManager.models().firstOrNull { it.id == voice.model }

    override fun onSynthesizeText(request: SynthesisRequest?, callback: SynthesisCallback?) {
        if (request == null || callback == null) return
        stopRequested = false

        val ret = onIsLanguageAvailable(request.language, request.country, request.variant)
        if (ret == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error()
            return
        }

        val voice = resolveRequestVoice(request.voiceName)
        if (voice == null) {
            Log.e(TAG, "No valid configured voice for ${request.voiceName}")
            longToast(R.string.tts_config_not_set)
            callback.error()
            return
        }

        val model = getTtsModel(voice)
        if (model == null) {
            Log.e(TAG, "Model not found for voice $voice")
            longToast(R.string.tts_config_not_set)
            callback.error()
            return
        }

        val tts = SynthesizerManager.getTTS(model.toOfflineTtsConfig())
        callback.start(tts.sampleRate(), AudioFormat.ENCODING_PCM_16BIT, 1)

        val text = request.charSequenceText.toString()
        if (text.isBlank()) {
            callback.done()
            return
        }

        var firstAudio = true
        val ttsCallback: (FloatArray) -> Int = { floatSamples ->
            if (stopRequested) {
                0
            } else {
                val samples = floatSamples.toByteArray()
                val targetBytes = min(
                    callback.maxBufferSize,
                    maxOf(2048, (tts.sampleRate() * 2 * 0.12f).toInt()),
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
                    val n = min(targetBytes, samples.size - offset)
                    callback.audioAvailable(samples, offset, n)
                    offset += n
                }

                if (stopRequested) 0 else 1
            }
        }

        val speed = (request.speechRate / 100f).coerceIn(0.55f, 1.50f)
        Log.i(TAG, "FlowFixV3 voice=$voice rate=${request.speechRate} speed=$speed chars=${text.length}")

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

        if (!stopRequested) callback.done()
    }
}
'@
Set-Content -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt" -Value $ttsService -Encoding utf8

Write-Host "Flow Fix V3 checks:"
Select-String -Path $configPath -Pattern "maxNumSentences = if \(kokoro\) 2|silenceScale = if \(kokoro\) 0.06f"
Select-String -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Pattern "persistentKokoro|numThreads"
Select-String -Path "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt" -Pattern "generateWithConfigAndCallback|kokoro-prewarm|onGetDefaultVoiceNameFor|speaker.id"
Select-String -Path $gradlePath -Pattern "versionCode = gitCommits \+ 2000"

# Trigger direct-asset V3 build after 1.13.8 compatibility fix.
