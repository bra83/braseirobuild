$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$Tag = 'FORBIDDEN-LANDS-3.3.2-TTS-IMAGE'
$ApkName = 'BRASEIRO_FORBIDDEN_LANDS_3.3.2.apk'
$SourceName = 'Braseiro_Forbidden_Lands_3.3.2_TTS_IMAGE_PROMPT_PATCH_ANDROID_STUDIO.zip'
$SourceSha = '83b99e4ced2f635b1c93d79c0dbb9ef5093545ea1c67cf8af2dc88331ce0061c'
$FireApi = 'https://api.firestorage.ai/dev/file'
$ShareId = 'sJUeoArUdSIJ'
$FileId = '01a0bd4313427307bc87a8337de35d13'

$root = $env:GITHUB_WORKSPACE
$incoming = Join-Path $root 'fl-incoming'
$work = Join-Path $root 'fl-work'
$out = Join-Path $root 'fl-out'
Remove-Item $incoming,$work,$out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $incoming,$work,$out | Out-Null

Write-Host 'Baixando fonte exata 3.3.2...'
$list = Invoke-RestMethod -Uri "$FireApi/shares/$ShareId/files?maxResults=1000" -Method Get
if (-not ($list.files | Where-Object { [string]$_.fileId -eq $FileId })) {
  throw "Fonte Forbidden Lands 3.3.2 não encontrada no share temporário."
}
$download = Invoke-RestMethod -Uri "$FireApi/shares/$ShareId/files/$FileId/download" -Method Post
if (-not $download.downloadUrl) { throw 'URL de download não retornada.' }
$zipPath = Join-Path $incoming $SourceName
Invoke-WebRequest -Uri $download.downloadUrl -OutFile $zipPath
$actualSha = (Get-FileHash -Algorithm SHA256 $zipPath).Hash.ToLowerInvariant()
if ($actualSha -ne $SourceSha) { throw "SHA da fonte incorreto: $actualSha" }
Expand-Archive -Path $zipPath -DestinationPath $work -Force
if (-not (Test-Path (Join-Path $work 'package.json'))) { throw 'package.json não encontrado na fonte.' }
if (-not (Test-Path (Join-Path $work 'android\gradlew.bat'))) { throw 'gradlew.bat não encontrado.' }

Set-Location $work

Write-Host 'Configurando Node/npm...'
if (-not (Get-Command npm.cmd -ErrorAction SilentlyContinue)) { throw 'npm.cmd não encontrado no runner.' }
& npm.cmd config set registry https://registry.npmjs.org/
& npm.cmd config set fetch-retries 5
& npm.cmd config set fetch-timeout 120000
& npm.cmd ci --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw "npm ci falhou com código $LASTEXITCODE" }

Write-Host 'Fixando versão Android 3.3.2...'
$pkgPath = Join-Path $work 'package.json'
$pkg = Get-Content $pkgPath -Raw | ConvertFrom-Json
$pkg.version = '3.3.2'
$pkg | ConvertTo-Json -Depth 50 | Set-Content -Path $pkgPath -Encoding UTF8

$gradlePath = Join-Path $work 'android\app\build.gradle'
$gradleText = Get-Content $gradlePath -Raw
$gradleText = [regex]::Replace($gradleText, 'versionCode\s+\d+', 'versionCode 303002', 1)
$gradleText = [regex]::Replace($gradleText, 'versionName\s+"[^"]+"', 'versionName "3.3.2"', 1)
Set-Content -Path $gradlePath -Value $gradleText -Encoding UTF8

Write-Host 'Rodando gates focados da versão 3.3.2...'
& npm.cmd run verify:start-freeze
if ($LASTEXITCODE -ne 0) { throw 'verify:start-freeze falhou.' }
& npm.cmd run verify:rules
if ($LASTEXITCODE -ne 0) { throw 'verify:rules falhou.' }

Write-Host 'Recompilando web e sincronizando Android...'
& npm.cmd run android:sync
if ($LASTEXITCODE -ne 0) { throw 'android:sync falhou.' }
$assetsRoot = Join-Path $work 'android\app\src\main\assets\public'
if (-not (Test-Path (Join-Path $assetsRoot 'index.html'))) { throw 'Assets Android não foram sincronizados.' }
$assetJs = Get-ChildItem (Join-Path $assetsRoot 'assets') -Filter *.js -File -Recurse
$containsModelTts = $false
$containsPromptVisual = $false
foreach ($file in $assetJs) {
  $text = Get-Content $file.FullName -Raw -ErrorAction SilentlyContinue
  if ($text -match 'Modelo TTS') { $containsModelTts = $true }
  if ($text -match 'Prompt visual da cena') { $containsPromptVisual = $true }
  if ($containsModelTts -and $containsPromptVisual) { break }
}
if (-not $containsModelTts) { throw 'Bundle Android não contém o campo Modelo TTS.' }
if (-not $containsPromptVisual) { throw 'Bundle Android não contém o prompt visual da cena.' }

Write-Host 'Configurando Java/Android SDK...'
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) {
  $javaCandidates = @(
    (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
    (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr')
  )
  foreach ($candidate in $javaCandidates) {
    if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
      $env:JAVA_HOME = $candidate
      $env:Path = "$candidate\bin;$env:Path"
      break
    }
  }
}
if (-not (Get-Command java.exe -ErrorAction SilentlyContinue)) { throw 'Java/JDK não encontrado.' }
& java.exe -version

if (-not $env:ANDROID_HOME) {
  $sdkCandidate = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
  if (Test-Path $sdkCandidate) { $env:ANDROID_HOME = $sdkCandidate }
}
if (-not $env:ANDROID_HOME -or -not (Test-Path $env:ANDROID_HOME)) { throw 'ANDROID_HOME/Android SDK não encontrado.' }
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$sdkManager = Join-Path $env:ANDROID_HOME 'cmdline-tools\latest\bin\sdkmanager.bat'
if (Test-Path $sdkManager) {
  & $sdkManager 'platform-tools' 'platforms;android-36' 'build-tools;36.0.0'
}

Write-Host 'Gerando APK...'
Set-Location (Join-Path $work 'android')
$gradleLog = Join-Path $out 'GRADLE_BUILD.log'
& .\gradlew.bat :app:assembleDebug --stacktrace --console=plain --no-daemon 2>&1 | Tee-Object -FilePath $gradleLog
if ($LASTEXITCODE -ne 0) { throw "Gradle falhou com código $LASTEXITCODE" }
$apk = Get-ChildItem (Join-Path $work 'android\app\build\outputs\apk\debug') -Filter *.apk -File | Select-Object -First 1
if (-not $apk) { throw 'APK não encontrado após assembleDebug.' }
$finalApk = Join-Path $out $ApkName
Copy-Item $apk.FullName $finalApk -Force

Write-Host 'Validando APK...'
$apkSha = (Get-FileHash -Algorithm SHA256 $finalApk).Hash.ToLowerInvariant()
"$apkSha  $ApkName" | Set-Content (Join-Path $out "$ApkName.sha256") -Encoding ASCII
$buildTools = Join-Path $env:ANDROID_HOME 'build-tools\36.0.0'
$aapt = Join-Path $buildTools 'aapt.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
if (-not (Test-Path $aapt)) { throw 'aapt 36.0.0 não encontrado.' }
if (-not (Test-Path $apksigner)) { throw 'apksigner 36.0.0 não encontrado.' }
$badgingPath = Join-Path $out 'APK_BADGING.txt'
$badging = (& $aapt dump badging $finalApk) -join [Environment]::NewLine
$badging | Set-Content $badgingPath -Encoding UTF8
if ($badging -notmatch "package: name='com\.braseiro\.forbiddenlands'") { throw 'Package ID incorreto.' }
if ($badging -notmatch "versionCode='303002'") { throw 'versionCode incorreto.' }
if ($badging -notmatch "versionName='3\.3\.2'") { throw 'versionName incorreto.' }
$signaturePath = Join-Path $out 'APK_SIGNATURE.txt'
& $apksigner verify --verbose --print-certs $finalApk 2>&1 | Tee-Object -FilePath $signaturePath
if ($LASTEXITCODE -ne 0) { throw 'Assinatura do APK inválida.' }

Write-Host 'Publicando GitHub Release...'
$token = $env:GITHUB_TOKEN
if (-not $token) { throw 'GITHUB_TOKEN ausente.' }
$repo = $env:GITHUB_REPOSITORY
$headers = @{
  Authorization = "Bearer $token"
  Accept = 'application/vnd.github+json'
  'X-GitHub-Api-Version' = '2022-11-28'
  'User-Agent' = 'braseirobuild-self-hosted'
}
$apiBase = "https://api.github.com/repos/$repo"
$release = $null
try {
  $release = Invoke-RestMethod -Headers $headers -Uri "$apiBase/releases/tags/$Tag" -Method Get
} catch {
  if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
}
if (-not $release) {
  $body = @{
    tag_name = $Tag
    target_commitish = $env:GITHUB_SHA
    name = 'Braseiro Forbidden Lands 3.3.2 — TTS + Imagem de Cena'
    body = 'TTS Gemini com modelo, voz e direção configuráveis; modelo de imagem configurável; geração manual ou automática; prompt visual editável por cena; imagem retornada ao card. Runtime web recompilado e sincronizado com Android antes do APK.'
    draft = $false
    prerelease = $false
  } | ConvertTo-Json
  $release = Invoke-RestMethod -Headers $headers -Uri "$apiBase/releases" -Method Post -Body $body -ContentType 'application/json'
}
$assets = Invoke-RestMethod -Headers $headers -Uri "$apiBase/releases/$($release.id)/assets" -Method Get
foreach ($name in @($ApkName, "$ApkName.sha256", 'APK_BADGING.txt', 'APK_SIGNATURE.txt', 'GRADLE_BUILD.log')) {
  $existing = $assets | Where-Object { $_.name -eq $name }
  foreach ($asset in $existing) {
    Invoke-RestMethod -Headers $headers -Uri "$apiBase/releases/assets/$($asset.id)" -Method Delete | Out-Null
  }
}
$uploadBase = ($release.upload_url -replace '\{\?name,label\}$','')
function Upload-Asset([string]$Path,[string]$Name,[string]$ContentType) {
  $uri = "$uploadBase?name=$([uri]::EscapeDataString($Name))"
  Invoke-WebRequest -Headers $headers -Uri $uri -Method Post -InFile $Path -ContentType $ContentType | Out-Null
}
Upload-Asset $finalApk $ApkName 'application/vnd.android.package-archive'
Upload-Asset (Join-Path $out "$ApkName.sha256") "$ApkName.sha256" 'text/plain'
Upload-Asset $badgingPath 'APK_BADGING.txt' 'text/plain'
Upload-Asset $signaturePath 'APK_SIGNATURE.txt' 'text/plain'
Upload-Asset $gradleLog 'GRADLE_BUILD.log' 'text/plain'

Write-Host "RELEASE_URL=$($release.html_url)"
Write-Host "APK_SHA256=$apkSha"
