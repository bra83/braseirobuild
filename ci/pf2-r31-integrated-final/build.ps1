$ErrorActionPreference='Stop'
if($env:RUNNER_NAME -ne 'BRELGA'){throw 'Runner incorreto'}
$baseline=Join-Path $env:RUNNER_TEMP 'pf2-kingmaker-r30-independent-audit\extracted\pf2e_r30_work'
if(-not(Test-Path (Join-Path $baseline 'version.properties'))){throw 'Baseline R30 auditada nao encontrada no BRELGA'}
$root=Join-Path $env:RUNNER_TEMP 'pf2-r31-integrated-final'
Remove-Item $root -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $root -Force|Out-Null
$work=Join-Path $root 'work'
Copy-Item $baseline $work -Recurse -Force
$src=Join-Path $env:GITHUB_WORKSPACE 'ci\pf2-r30-live-src'
$files=@(
'android\app\src\main\java\com\braseiro\pathfinder2e\AppDb.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\PartyDirector.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\MasterOrchestrator.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\MainActivity.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\KingmakerRulesEngine.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\HumanMasterNarrativeProfile.java',
'android\app\src\main\java\com\braseiro\pathfinder2e\KingmakerTurnGuide.java',
'tests_pf2\test_r30_user_requested_integrated_features.py',
'tests_pf2\java\KingmakerTurnGuideRulesHarness.java'
)
foreach($f in $files){
  $from=Join-Path $src $f
  if(-not(Test-Path $from)){throw "Overlay ausente: $f"}
  $to=Join-Path $work $f
  New-Item -ItemType Directory -Path (Split-Path $to -Parent) -Force|Out-Null
  Copy-Item $from $to -Force
}
$profile=Join-Path $env:GITHUB_WORKSPACE 'ci\pf2-r30-narrative\PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md'
if(-not(Test-Path $profile)){throw 'Perfil narrativo ausente'}
$sha=(Get-FileHash $profile -Algorithm SHA256).Hash.ToLowerInvariant()
if($sha -ne '8e63dbdf963abd1bed88e7173de3adc22d1501557647bcf4a9d95ee096f75852'){throw "Perfil narrativo alterado: $sha"}
$assetDir=Join-Path $work 'android\app\src\main\assets\narrative'
New-Item -ItemType Directory -Path $assetDir -Force|Out-Null
Copy-Item $profile (Join-Path $assetDir 'PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md') -Force
$vp=Join-Path $work 'version.properties'
$v=Get-Content $vp -Raw
$v=[regex]::Replace($v,'VERSION_NAME=\d+\.\d+\.\d+','VERSION_NAME=9.0.32')
$v=[regex]::Replace($v,'VERSION_CODE=\d+','VERSION_CODE=90032')
Set-Content $vp $v -Encoding ascii
Push-Location $work
python -m pip install -q pytest
python -m pytest tests_pf2\test_r30_user_requested_integrated_features.py -q
if($LASTEXITCODE -ne 0){throw 'Teste focado falhou'}
$km=Get-ChildItem tests_pf2 -File -Filter 'test_kingmaker*.py'|Sort-Object Name|ForEach-Object{$_.FullName}
python -m pytest @km -q
if($LASTEXITCODE -ne 0){throw 'Regressao Kingmaker'}
python -m pytest tests_pf2 -q
if($LASTEXITCODE -ne 0){throw 'Regressao PF2'}
Pop-Location
$candidates=@('C:\actions-runner\android-sdk',$env:ANDROID_SDK_ROOT,$env:ANDROID_HOME)
if($env:LOCALAPPDATA){$candidates+=(Join-Path $env:LOCALAPPDATA 'Android\Sdk')}
$sdk=$null
foreach($x in ($candidates|Where-Object{$_}|Select-Object -Unique)){
  if(Test-Path (Join-Path $x 'platform-tools')){$sdk=$x;break}
}
if(-not $sdk){throw 'Android SDK nao encontrado'}
$env:ANDROID_HOME=$sdk;$env:ANDROID_SDK_ROOT=$sdk
Push-Location (Join-Path $work 'android')
& .\gradlew.bat :app:assembleDebug --no-daemon
if($LASTEXITCODE -ne 0){throw 'Gradle falhou'}
Pop-Location
$apk=Get-ChildItem $work -Recurse -Filter '*.apk' -File|Where-Object{$_.FullName -match '[\\/]build[\\/]outputs[\\/]apk[\\/]debug[\\/]'}|Select-Object -First 1
if(-not $apk){throw 'APK nao encontrado'}
$desktop=[Environment]::GetFolderPath('Desktop')
$dest=Join-Path $desktop 'BRASEIRO_PATHFINDER2E_R31_INTEGRATED'
New-Item -ItemType Directory -Path $dest -Force|Out-Null
$out=Join-Path $dest 'Braseiro_Pathfinder2e_9.0.32_R31_INTEGRATED_DEBUG.apk'
Copy-Item $apk.FullName $out -Force
(Get-FileHash $out -Algorithm SHA256).Hash.ToLowerInvariant()|Set-Content "$out.sha256" -Encoding ascii
@(
'R30 pos-Antigravity preservado.',
'Controle JOGADOR/MESTRE por personagem integrado.',
'Guia completo do Turno do Reino integrado ao motor real.',
'Perfil Mestre Humano Imersivo integrado ao MasterOrchestrator.',
'TTS nao alterado.',
'Focused + Kingmaker + PF2 + Gradle: PASS.'
)|Set-Content (Join-Path $dest 'VALIDACAO.txt') -Encoding utf8
Write-Host "FINAL_APK=$out"
