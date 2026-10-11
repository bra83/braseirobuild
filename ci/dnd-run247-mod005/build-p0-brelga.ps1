$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest
$start = Get-Date
if ($env:RUNNER_NAME -ne 'BRELGA') { throw "RUNNER_MUST_BE_BRELGA: $env:RUNNER_NAME" }
$sourceSha = 'f11021b2a510c823db610e9fa9213f22ee56c441ebffcd6b15f7f3383ac09ae7'
$driveId = '1zNxBNWrIAhQeLSdAMKcAXsKeZLJuRBit'
$sourceName = 'BRASEIRO_DND_NORMAL_RUN247_MOD005_HOMOLOGADO.zip'
$folderName = 'RUN247_MOD005_HOMOLOGADO_20261011'
$stage = Join-Path $env:RUNNER_TEMP 'dnd-run247-p0-brelga'
$out = Join-Path $env:RUNNER_TEMP 'dnd-run247-p0-output'
$root = Join-Path $stage 'source'
$patch = Join-Path $env:GITHUB_WORKSPACE 'ci\dnd-run247-mod005\Run247P0Repair.patch'
Remove-Item $stage,$out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $root,$out -Force | Out-Null
$driveCandidates = @(
  "G:\Meu Drive\DND_SANDBOX_WORKING\$folderName",
  "G:\My Drive\DND_SANDBOX_WORKING\$folderName",
  "G:\MyDrive\DND_SANDBOX_WORKING\$folderName"
)
if ($env:DND_RUN247_SOURCE_DIR) { $driveCandidates = @($env:DND_RUN247_SOURCE_DIR) + $driveCandidates }
$source = $null
$driveFolder = $null
foreach ($folder in $driveCandidates) {
  $candidate = Join-Path $folder $sourceName
  if (Test-Path $candidate -PathType Leaf) { $source = $candidate; $driveFolder = $folder; break }
}
if (-not $source) {
  Write-Host 'Local Drive sync source absent; attempting shared Drive download.'
  $download = Join-Path $stage $sourceName
  $url = "https://drive.google.com/uc?export=download&id=$driveId"
  & curl.exe --location --fail --retry 2 --connect-timeout 20 --max-time 360 -o $download $url
  if ($LASTEXITCODE -ne 0 -or !(Test-Path $download)) { throw 'RUN247_SOURCE_UNAVAILABLE_IN_DRIVE_AND_HTTPS' }
  $source = $download
}
$actualSha = (Get-FileHash $source -Algorithm SHA256).Hash.ToLowerInvariant()
Write-Host "RUN247_SOURCE=$source"
Write-Host "RUN247_SOURCE_SHA256=$actualSha"
if ($actualSha -ne $sourceSha) { throw "SOURCE_PIN_MISMATCH: expected $sourceSha got $actualSha" }
if (!(Test-Path $patch)) { throw "PATCH_NOT_FOUND: $patch" }
& tar.exe -xf $source -C $root
if ($LASTEXITCODE -ne 0) { throw 'UNPACK_FAILED' }
if (!(Test-Path (Join-Path $root 'gradlew.bat'))) { throw 'RUN247_ANDROID_WRAPPER_MISSING' }
Push-Location $root
try {
  & git.exe apply --check $patch
  if ($LASTEXITCODE -ne 0) { throw 'PATCH_CONFLICT_IN_CANONICAL_BASE' }
  & git.exe apply $patch
  if ($LASTEXITCODE -ne 0) { throw 'PATCH_APPLY_FAILED' }
  Write-Host 'PATCH_APPLIED_TO_CANONICAL_RUN247'
  $javaCandidates = @(
    'C:\actions-runner\_work\_tool\Java_Temurin-Hotspot_jdk\17.0.20-101\x64',
    'C:\Program Files\Android\Android Studio\jbr',
    'C:\Program Files\Eclipse Adoptium\jdk-17'
  )
  if (-not $env:JAVA_HOME -or !(Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    foreach ($j in $javaCandidates) { if (Test-Path (Join-Path $j 'bin\java.exe')) { $env:JAVA_HOME = $j; break } }
  }
  if ($env:JAVA_HOME) { $env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH }
  if (-not $env:ANDROID_HOME -and $env:LOCALAPPDATA) { $env:ANDROID_HOME = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
  if (-not $env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT = $env:ANDROID_HOME }
  Write-Host "JAVA_HOME=$env:JAVA_HOME ANDROID_HOME=$env:ANDROID_HOME"
  $testLog = Join-Path $out 'GRADLE_TESTS.log'
  & .\gradlew.bat :app:testDebugUnitTest --no-daemon --console=plain 2>&1 | Tee-Object -FilePath $testLog
  if ($LASTEXITCODE -ne 0) { throw 'GRADLE_TESTS_FAILED' }
  $xmlDir = Join-Path $root 'app\build\test-results\testDebugUnitTest'
  $xmlFiles = @(Get-ChildItem $xmlDir -Filter 'TEST-*.xml' -File -ErrorAction Stop)
  $tests=0;$failures=0;$errors=0;$skipped=0
  foreach ($file in $xmlFiles) {
    [xml]$xml = Get-Content $file.FullName -Raw
    $tests += [int]$xml.testsuite.tests
    $failures += [int]$xml.testsuite.failures
    $errors += [int]$xml.testsuite.errors
    $skipped += [int]$xml.testsuite.skipped
  }
  Write-Host "RUN247_TEST_XML=$($xmlFiles.Count) TESTS=$tests FAILURES=$failures ERRORS=$errors SKIPPED=$skipped"
  if ($tests -lt 177 -or $failures -ne 0 -or $errors -ne 0 -or $skipped -ne 0) { throw 'TEST_GATE_NOT_MET' }
  $xmlOut = Join-Path $out 'test-results-xml'
  New-Item -ItemType Directory -Path $xmlOut -Force | Out-Null
  Copy-Item (Join-Path $xmlDir 'TEST-*.xml') $xmlOut -Force
  & .\gradlew.bat :app:assembleDebug --no-daemon --console=plain 2>&1 | Tee-Object -FilePath (Join-Path $out 'GRADLE_BUILD.log')
  if ($LASTEXITCODE -ne 0) { throw 'ASSEMBLE_DEBUG_FAILED' }
  $apk = Join-Path $root 'app\build\outputs\apk\debug\app-debug.apk'
  if (!(Test-Path $apk)) { throw 'DEBUG_APK_MISSING' }
  $apkName = 'BRASEIRO_DND_NORMAL_RUN247_MOD005_P0_BRELGA_DEBUG.apk'
  $apkOut = Join-Path $out $apkName
  Copy-Item $apk $apkOut -Force
  $clean = Join-Path $stage 'clean-source'
  New-Item -ItemType Directory -Path $clean -Force | Out-Null
  & robocopy.exe $root $clean /E /XD build .gradle .git /XF local.properties /NFL /NDL /NJH /NJS /NP | Out-Null
  if ($LASTEXITCODE -ge 8) { throw "CLEAN_SOURCE_COPY_FAILED:$LASTEXITCODE" }
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $zipOut = Join-Path $out 'BRASEIRO_DND_NORMAL_RUN247_MOD005_P0_BRELGA_SOURCE.zip'
  [IO.Compression.ZipFile]::CreateFromDirectory($clean,$zipOut,[IO.Compression.CompressionLevel]::Optimal,$false)
  $apkSha = (Get-FileHash $apkOut -Algorithm SHA256).Hash.ToLowerInvariant()
  $zipSha = (Get-FileHash $zipOut -Algorithm SHA256).Hash.ToLowerInvariant()
  $proof = @(
    'STATUS=BUILD_AND_TEST_PASS',
    "RUN_ID=$env:GITHUB_RUN_ID",
    "RUNNER=$env:RUNNER_NAME",
    "SOURCE_BASE_SHA256=$sourceSha",
    "PATCH_GITHUB_SHA=$env:GITHUB_SHA",
    "APK_SHA256=$apkSha",
    "ZIP_SHA256=$zipSha",
    "TESTS=$tests",
    "FAILURES=$failures",
    "ERRORS=$errors",
    "SKIPPED=$skipped",
    'PHYSICAL_ANDROID_TEST=NOT_RUN'
  ) -join "`r`n"
  $proof | Set-Content (Join-Path $out 'RUN247_P0_BRELGA_PROOF.txt') -Encoding UTF8
  if ($driveFolder -and (Test-Path $driveFolder)) {
    $publish = Join-Path $driveFolder 'BRELGA_P0_DELIVERY'
    New-Item $publish -ItemType Directory -Force | Out-Null
    Copy-Item $apkOut,$zipOut,(Join-Path $out 'RUN247_P0_BRELGA_PROOF.txt') $publish -Force
    Write-Host "DRIVE_PUBLISHED=$publish"
  } else { Write-Warning 'Drive sync folder unavailable: artifacts will be available via GitHub Actions.' }
  Write-Host $proof
} finally {
  Pop-Location
}
