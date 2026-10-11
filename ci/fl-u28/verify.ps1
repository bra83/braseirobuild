$ErrorActionPreference='Stop'
$root=$env:FL_ROOT
$out=Join-Path $root 'release'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$apk=Join-Path $out 'BRASEIRO_FORBIDDEN_LANDS_3.3.25_U28_INTEGRATED.apk'
$from=Join-Path $root 'project\android\app\build\outputs\apk\debug\app-debug.apk'
if (-not(Test-Path $from)) { throw 'Gradle did not produce debug APK' }
Copy-Item $from $apk
$bt=Join-Path $env:ANDROID_HOME 'build-tools\36.0.0'
$badging=(& (Join-Path $bt 'aapt.exe') dump badging $apk | Out-String)
if ($LASTEXITCODE -ne 0 -or $badging -notmatch "package: name='com\.braseiro\.forbiddenlands'" -or
    $badging -notmatch "versionCode='303925'" -or $badging -notmatch "versionName='3\.3\.25'") {
  throw 'APK package name or version is not the pinned U28 update'
}
$signature=(& (Join-Path $bt 'apksigner.bat') verify --verbose --print-certs $apk | Out-String)
if ($LASTEXITCODE -ne 0) { throw 'APK verification failed' }
$m=[regex]::Match($signature,'(?im)certificate SHA-1 digest:\s*([0-9a-f:]+)')
if (-not $m.Success -or $m.Groups[1].Value.Replace(':','').ToLowerInvariant() -ne $env:EXPECTED_SIGNER) {
  throw 'APK is not signed with historical certificate: publication prohibited'
}
& python.exe "$env:GITHUB_WORKSPACE\ci\fl-u28\verify.py"
if ($LASTEXITCODE -ne 0) { throw 'Source/asset proof failed' }
$signature | Set-Content (Join-Path $out 'APK_SIGNATURE.txt')
$badging | Set-Content (Join-Path $out 'APK_IDENTITY.txt')
Write-Host 'U28_NATIVE_SIGNATURE_AND_ASSETS_PASS'
