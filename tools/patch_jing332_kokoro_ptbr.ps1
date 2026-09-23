
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
