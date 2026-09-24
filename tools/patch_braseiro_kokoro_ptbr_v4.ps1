param(
  [string]$WorkDir = "$env:RUNNER_TEMP\braseiro-kokoro-ptbr-v4"
)

$ErrorActionPreference = "Stop"

& "$PSScriptRoot\patch_jing332_kokoro_ptbr_v3.ps1" -WorkDir $WorkDir
if ($LASTEXITCODE -ne 0) { throw "Base V3 patch failed" }
Set-Location $WorkDir

Write-Host "Applying dedicated Braseiro Kokoro PT-BR V4 identity..."

$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
$gradle = $gradle.Replace('applicationId = "com.k2fsa.sherpa.onnx.tts.engine"', 'applicationId = "com.braseiro.kokoro.ptbr"')
$gradle = $gradle.Replace('versionCode = gitCommits + 2000', 'versionCode = 40100')
$gradle = $gradle.Replace('versionCode = gitCommits', 'versionCode = 40100')
$gradle = $gradle.Replace('val version = "1.$releaseTime"', 'val version = "1.13.8-braseiro-v4"')
$gradle = $gradle.Replace('applicationIdSuffix = ".debug"', '// dedicated Braseiro engine keeps the same package in debug builds')
Set-Content -Path $gradlePath -Value $gradle -Encoding utf8

$stringsPath = "app\src\main\res\values\strings.xml"
$strings = Get-Content $stringsPath -Raw
$strings = $strings.Replace('<string name="app_name" translatable="false">Next-gen Kaldi: TTS</string>', '<string name="app_name" translatable="false">Braseiro Kokoro PT-BR V4</string>')
Set-Content -Path $stringsPath -Value $strings -Encoding utf8

$manifestPath = "app\src\main\AndroidManifest.xml"
$manifest = Get-Content $manifestPath -Raw
if ($manifest -notmatch 'android:largeHeap=') {
  $manifest = $manifest.Replace('android:allowBackup="true"', 'android:allowBackup="true"' + [Environment]::NewLine + '        android:largeHeap="true"')
}
Set-Content -Path $manifestPath -Value $manifest -Encoding utf8

Write-Host "Downloading embedded pt-BR Kokoro model..."
$assetsDir = "app\src\main\assets"
New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null
$modelZip = Join-Path $assetsDir "braseiro-kokoro-pt.zip"
$modelUrl = "https://huggingface.co/cristianoaredes/kokoro-pt-br/resolve/main/letrinhas-kokoro-pt.zip?download=true"
& curl.exe -L --fail --retry 5 --retry-delay 3 --retry-all-errors -o $modelZip $modelUrl
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $modelZip)) { throw "Failed to download embedded Kokoro pt-BR model" }
if ((Get-Item $modelZip).Length -lt 250MB) { throw "Embedded Kokoro model ZIP is unexpectedly small" }

$installerPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\EmbeddedModelInstaller.kt"
$installer = @'
package com.k2fsa.sherpa.onnx.tts.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object EmbeddedModelInstaller {
    private const val TAG = "BraseiroKokoroInstaller"
    private const val ASSET_NAME = "braseiro-kokoro-pt.zip"
    private const val MODEL_DIR_NAME = "braseiro-kokoro-pt"

    @Synchronized
    fun ensureInstalled(context: Context): File {
        val finalDir = File(FileConst.modelDir, MODEL_DIR_NAME)
        if (isComplete(finalDir)) return finalDir

        val parent = File(FileConst.modelDir)
        parent.mkdirs()
        val tempDir = File(parent, "$MODEL_DIR_NAME.installing")
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        Log.i(TAG, "Installing embedded Kokoro pt-BR model into ${finalDir.absolutePath}")
        context.assets.open(ASSET_NAME).use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(tempDir, entry.name)
                    val canonicalRoot = tempDir.canonicalPath + File.separator
                    val canonicalTarget = target.canonicalPath
                    require(canonicalTarget.startsWith(canonicalRoot)) { "Invalid ZIP entry: ${entry.name}" }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out -> zip.copyTo(out, 256 * 1024) }
                    }
                    zip.closeEntry()
                }
            }
        }

        val source = tempDir.walkTopDown()
            .firstOrNull { it.isFile && it.name == "model.onnx" }
            ?.parentFile
            ?.takeIf { File(it, "voices.bin").isFile && File(it, "tokens.txt").isFile }
            ?: throw IllegalStateException("Embedded Kokoro ZIP does not contain model.onnx + voices.bin + tokens.txt")

        finalDir.deleteRecursively()
        finalDir.mkdirs()
        source.listFiles()?.forEach { child ->
            val dst = File(finalDir, child.name)
            if (child.isDirectory) child.copyRecursively(dst, overwrite = true)
            else child.copyTo(dst, overwrite = true)
        }
        tempDir.deleteRecursively()

        require(isComplete(finalDir)) { "Embedded Kokoro model installation is incomplete" }
        File(finalDir, ".braseiro-v4-installed").writeText("ok")
        Log.i(TAG, "Embedded Kokoro pt-BR model ready")
        return finalDir
    }

    private fun isComplete(dir: File): Boolean =
        File(dir, "model.onnx").isFile &&
        File(dir, "voices.bin").isFile &&
        File(dir, "tokens.txt").isFile &&
        File(dir, "espeak-ng-data").isDirectory
}
'@
Set-Content -Path $installerPath -Value $installer -Encoding utf8

$configPath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt"
$config = Get-Content $configPath -Raw
$oldLoad = @'
        if (models.isEmpty()) {
            models.addAll(ConfigManager.config.models)
        }
'@
$newLoad = @'
        if (models.isEmpty()) {
            models.addAll(ConfigManager.config.models)
            if (models.isEmpty()) {
                models.addAll(analyzeToModels())
            }
        }
'@
if (-not $config.Contains($oldLoad)) { throw "ConfigModelManager load marker missing" }
$config = $config.Replace($oldLoad, $newLoad)
Set-Content -Path $configPath -Value $config -Encoding utf8

$ttsServicePath = "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt"
$tts = Get-Content $ttsServicePath -Raw
$oldCreate = @'
        ConfigVoiceManager.load()
        ConfigModelManager.load()
'@
$newCreate = @'
        com.k2fsa.sherpa.onnx.tts.engine.EmbeddedModelInstaller.ensureInstalled(this)
        ConfigVoiceManager.load()
        ConfigModelManager.load()
'@
if (-not $tts.Contains($oldCreate)) { throw "TtsService model-load marker missing" }
$tts = $tts.Replace($oldCreate, $newCreate)
Set-Content -Path $ttsServicePath -Value $tts -Encoding utf8

Write-Host "Braseiro Kokoro V4 identity/model checks:"
Select-String -Path $gradlePath -Pattern 'applicationId = "com.braseiro.kokoro.ptbr"|versionCode = 40100|1.13.8-braseiro-v4'
Select-String -Path $stringsPath -Pattern 'Braseiro Kokoro PT-BR V4'
Select-String -Path $installerPath -Pattern 'braseiro-kokoro-pt.zip|model.onnx|voices.bin|espeak-ng-data'
Select-String -Path $configPath -Pattern 'models.addAll\(analyzeToModels\(\)\)'
Select-String -Path $ttsServicePath -Pattern 'EmbeddedModelInstaller.ensureInstalled'
