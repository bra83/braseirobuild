# CI gate for Braseiro D&D RUN229. Does not modify user saves or historical projects.
$ErrorActionPreference='Stop'
$out=Join-Path $env:RUNNER_TEMP 'dnd-run229-ci-output'
New-Item -Force -ItemType Directory -Path $out | Out-Null
$proof=Join-Path $out 'RUN229_CI_PROOF.txt'
'G05_COMPLETE=NO; ANDROID_DEVICE_TEST=NOT_RUN; UPDATE_SIGNING_MATCH=NOT_VERIFIED' | Set-Content $proof
try {
  if($env:RUNNER_NAME -ne 'BRELGA'){throw 'RUNNER_MUST_BE_BRELGA'}
  $source='BRASEIRO_DND_6.6.4_RUN229_G05_ANDROID_STUDIO_SOURCE_FULL.zip'
  $expected='d5613362a512aaf1acd80135b6c74d6d7596bfa7220508860df650d3561a609e'
  $candidates=@('C:\DND229\incoming','C:\DND229\source-parts','G:\My Drive\DND_SANDBOX_WORKING\BRASEIRO_DND_RUN229_G05_CHECKPOINT_2026-10-09','G:\Meu Drive\DND_SANDBOX_WORKING\BRASEIRO_DND_RUN229_G05_CHECKPOINT_2026-10-09')
  if($env:RUN229_SOURCE_DIR){$candidates=@($env:RUN229_SOURCE_DIR)+$candidates}
  $manifest=@(
  '3973c13862f1a5da1b07a4ce231e25370196d41672b5cbf8f5d21333f6058079',
  'c392acfc6c9d67844b4cdfa265cf4154c26b1ed417f77c65fe04c39d2b50f68d',
  '70a6e3445c579e97e026dbb499bff7b80eefb0e0a70b0cb6d4b1d4d946c4f3e6',
  '220a186b8e3636141a4057d90911a64560061f51ccb5b50ea64f7be323ec4f3b',
  'a317c0eaaf07a666af27cdc6b4ae0695315b8423854fb173ab683d9d7dc41ae2',
  'd5eca001916710b09fb8a7d4dc1036026b6b7238855cfb9a1561c31ccea7625b',
  '133c0da30d3e03388dc4435639961c895d0e493be3b840f05a90d1258568eb85',
  '8fd8c6e3823166508ae258bc1ae09e308070c4961ab66170aa4e10bb12a6a11e',
  '0ffa6199a65d2f1e975511144f5186c425ac0ff5eec804cf3df75140f82f71df'
  )
  $zip=Join-Path $env:RUNNER_TEMP $source
  $found=$false
  foreach($dir in $candidates){
    if(-not (Test-Path -LiteralPath $dir -PathType Container)){continue}
    $whole=Join-Path $dir $source
    if(Test-Path -LiteralPath $whole){
      if((Get-FileHash $whole -Algorithm SHA256).Hash.ToLowerInvariant() -eq $expected){
        Copy-Item -LiteralPath $whole -Destination $zip -Force;$found=$true;break
      }
    }
    $good=$true
    for($i=0;$i -lt 9;$i++){
      $p=Join-Path $dir ("{0}.part{1:D2}" -f $source,$i)
      if(!(Test-Path -LiteralPath $p)){$good=$false;break}
      if((Get-FileHash $p -Algorithm SHA256).Hash.ToLowerInvariant() -ne $manifest[$i]){$good=$false;break}
    }
    if(-not $good){continue}
    $dest=[IO.File]::Create($zip)
    try{
      for($i=0;$i -lt 9;$i++){
        $p=Join-Path $dir ("{0}.part{1:D2}" -f $source,$i)
        $inputStream=[IO.File]::OpenRead($p)
        try{$inputStream.CopyTo($dest)}finally{$inputStream.Dispose()}
      }
    }finally{$dest.Dispose()}
    $found=$true;break
  }
  if(-not $found){throw 'RUN229_SOURCE_NOT_STAGED: configure RUN229_SOURCE_DIR with authenticated Drive mirror; no old checkpoint fallback'}
  $actual=(Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
  if($actual -ne $expected){throw "RUN229_SOURCE_SHA_MISMATCH=$actual"}
  "SOURCE_SHA256=$actual" | Add-Content $proof
  Add-Type -AssemblyName System.IO.Compression.FileSystem
  $archive=[IO.Compression.ZipFile]::OpenRead($zip)
  try{if($archive.Entries.Count -lt 1500){throw 'RUN229_SOURCE_ZIP_TOO_SMALL'}}finally{$archive.Dispose()}
  $project=Join-Path $env:RUNNER_TEMP ("dnd-run229-project-"+$env:GITHUB_RUN_ID)
  [IO.Directory]::CreateDirectory($project)|Out-Null
  [IO.Compression.ZipFile]::ExtractToDirectory($zip,$project)
  if(!(Test-Path (Join-Path $project 'android\gradlew.bat'))){throw 'GRADLE_WRAPPER_MISSING'}
  $version=Get-Content (Join-Path $project 'version.properties') -Raw
  if($version -notmatch '(?m)^VERSION_CODE=606529\s*$' -or $version -notmatch '(?m)^DB_SCHEMA=61\s*$' -or $version -notmatch '(?m)^BACKUP_FORMAT=21\s*$'){throw 'VERSION_OR_SAVE_CONTRACT_MISMATCH'}
  if(!$env:ANDROID_HOME){
    $sdk=Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if(Test-Path $sdk){$env:ANDROID_HOME=$sdk}
  }
  if(!$env:ANDROID_HOME -or !(Test-Path $env:ANDROID_HOME)){throw 'ANDROID_SDK_UNAVAILABLE'}
  $env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
  Set-Location (Join-Path $project 'android')
  foreach($task in @(':app:compileDebugJavaWithJavac',':app:testDebugUnitTest',':app:assembleDebug')){
    $saved=$ErrorActionPreference
    $ErrorActionPreference='Continue'
    & .\gradlew.bat $task --no-daemon --console=plain --stacktrace 2>&1 | Tee-Object -FilePath (Join-Path $out (($task -replace '[^a-zA-Z]','_')+'.log'))
    $rc=$LASTEXITCODE
    $ErrorActionPreference=$saved
    ("TASK_{0}={1}" -f $task,$rc) | Add-Content $proof
    if($rc -ne 0){throw "GRADLE_TASK_FAILED=$task ($rc)"}
  }
  $apk=Get-ChildItem (Join-Path $project 'android\app\build\outputs\apk\debug') -Filter '*.apk' -File|Select-Object -First 1
  if(!$apk){throw 'APK_NOT_GENERATED'}
  $candidate=Join-Path $out 'BRASEIRO_DND_RUN229_UNCERTIFIED_DEBUG.apk'
  Copy-Item $apk.FullName $candidate
  $bt=Get-ChildItem (Join-Path $env:ANDROID_HOME 'build-tools') -Directory|Sort-Object {[version]$_.Name} -Descending|Select-Object -First 1
  if(!$bt){throw 'ANDROID_BUILD_TOOLS_MISSING'}
  $badging=(& (Join-Path $bt.FullName 'aapt.exe') dump badging $candidate) -join "\n"
  $badging | Set-Content (Join-Path $out 'BADGING.txt')
  if($badging -notmatch "package: name='com\.braseiro\.dnd5e'" -or $badging -notmatch "versionCode='606529'"){throw 'APK_IDENTITY_INVALID'}
  & (Join-Path $bt.FullName 'apksigner.bat') verify --verbose --print-certs $candidate | Out-File (Join-Path $out 'SIGNATURE.txt')
  if($LASTEXITCODE -ne 0){throw 'APK_SIGNATURE_VERIFICATION_FAILED'}
  $sha=(Get-FileHash $candidate -Algorithm SHA256).Hash.ToLowerInvariant()
  "APK_SHA256=$sha" | Add-Content $proof
  'ANDROID_COMPILE=PASS; DEBUG_APK=VERIFIED; UPDATE_COMPATIBILITY=UNVERIFIED' | Add-Content $proof
}catch{
  ("FAILURE="+$_.Exception.Message) | Add-Content $proof
  Write-Error $_
  exit 1
}
