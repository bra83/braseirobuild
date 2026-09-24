param(
  [string]$WorkDir = "$env:RUNNER_TEMP\braseiro-kokoro-db-inplace-v5"
)

$ErrorActionPreference = "Stop"

# Start from the proven FlowFix V3: sherpa-onnx 1.13.8, real Android voices,
# resident Kokoro, streaming callback, 4 threads, maxNumSentences=2,
# silenceScale=0.06. V3 intentionally uses external model storage.
& "$PSScriptRoot\patch_jing332_kokoro_ptbr_v3.ps1" -WorkDir $WorkDir
if ($LASTEXITCODE -ne 0) { throw "FlowFix V3 base patch failed" }
Set-Location $WorkDir

Write-Host "Applying in-place DB model-host identity..."

# CRITICAL: do NOT change applicationId and do NOT remove debug suffix.
# The user's installed DB·Next-gen build is the debug package. Keeping
# com.k2fsa.sherpa.onnx.tts.engine.debug means Android preserves its
# app-specific files/model directory during install-over.
$gradlePath = "app\build.gradle.kts"
$gradle = Get-Content $gradlePath -Raw
if ($gradle -notmatch 'applicationId = "com\.k2fsa\.sherpa\.onnx\.tts\.engine"') {
  throw "Unexpected applicationId; refusing to create a separate model sandbox"
}
if ($gradle -notmatch 'applicationIdSuffix = "\.debug"') {
  throw "Debug suffix missing; refusing to break install-over DB model storage"
}
$gradle = $gradle.Replace('versionCode = gitCommits + 2000', 'versionCode = gitCommits + 5000')
$gradle = $gradle.Replace('val version = "1.$releaseTime"', 'val version = "1.13.8-braseiro-db-v5"')
Set-Content -Path $gradlePath -Value $gradle -Encoding utf8

# Rebrand every source-set app_name, including app/src/debug/res/values where
# upstream otherwise forces "DB·Next-gen Kaldi: TTS".
Get-ChildItem "app\src" -Recurse -File -Filter "strings.xml" |
  Where-Object { $_.FullName -match "[\\/]res[\\/]" } |
  ForEach-Object {
    $xml = Get-Content $_.FullName -Raw
    $xml = [regex]::Replace(
      $xml,
      '(<string\s+name="app_name"[^>]*>).*?(</string>)',
      '$1Braseiro Kokoro PT-BR$2'
    )
    Set-Content -Path $_.FullName -Value $xml -Encoding utf8
  }

# This build must not contain a second copy of the model. It consumes the
# model already in this package's existing files/model directory.
$embedded = Get-ChildItem "app\src\main\assets" -Recurse -File -ErrorAction SilentlyContinue |
  Where-Object { $_.Name -match 'kokoro.*\.(zip|onnx|bin)$|model\.onnx|voices\.bin' }
if ($embedded) {
  throw "Unexpected embedded Kokoro model detected: $($embedded.FullName -join ', ')"
}

Write-Host "In-place DB-host gates:"
Select-String -Path $gradlePath -Pattern 'applicationId = "com.k2fsa.sherpa.onnx.tts.engine"|applicationIdSuffix = ".debug"|gitCommits \+ 5000'
Get-ChildItem "app\src" -Recurse -File -Filter "strings.xml" |
  Select-String -Pattern 'Braseiro Kokoro PT-BR'

$flowSource = (
  Get-Content "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\service\TtsService.kt" -Raw
) + (
  Get-Content "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\ConfigModelManager.kt" -Raw
) + (
  Get-Content "app\src\main\java\com\k2fsa\sherpa\onnx\tts\engine\synthesizer\SynthesizerManager.kt" -Raw
)

$checks = @{
  'real-voices' = $flowSource.Contains('model.id + "_" + speaker.id')
  'resident-kokoro' = $flowSource.Contains('persistentKokoro')
  'streaming' = $flowSource.Contains('generateWithConfigAndCallback')
  'max-two-sentences' = $flowSource.Contains('maxNumSentences = if (kokoro) 2')
  'short-silence' = $flowSource.Contains('silenceScale = if (kokoro) 0.06f')
  'four-threads' = $flowSource.Contains('if (isKokoro(cfg)) 4')
}
foreach ($p in $checks.GetEnumerator()) {
  Write-Host "$($p.Key)=$($p.Value)"
  if (-not $p.Value) { throw "Flow V5 gate failed: $($p.Key)" }
}
