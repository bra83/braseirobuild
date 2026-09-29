$ErrorActionPreference='Stop'
if($env:RUNNER_NAME -ne 'BRELGA'){throw 'Runner incorreto'}
$root=Join-Path $env:RUNNER_TEMP 'pf2-r31-integrated-final'
Remove-Item $root -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $root -Force|Out-Null
$rar=Join-Path $root 'pf2e_r30_work.rar'
& curl.exe -L --fail --retry 5 --retry-delay 2 "https://drive.usercontent.google.com/download?id=1UbJq8m518f1jIQkkXASvEp0j0Uf6qNkR&export=download&confirm=t" -o $rar
if($LASTEXITCODE -ne 0 -or (Get-Item $rar).Length -ne 553809972){throw 'RAR canonico invalido'}
$extract=Join-Path $root 'extract'
New-Item -ItemType Directory -Path $extract -Force|Out-Null
$tools=@('C:\Program Files\7-Zip\7z.exe','C:\Program Files (x86)\7-Zip\7z.exe','C:\Program Files\WinRAR\UnRAR.exe','C:\Program Files\WinRAR\WinRAR.exe')
$tool=$tools|Where-Object{Test-Path $_}|Select-Object -First 1
if(-not $tool){throw 'Extrator RAR nao encontrado'}
if($tool -match '7z\.exe$'){& $tool x -y $rar "-o$extract"|Out-Null}
elseif($tool -match 'UnRAR\.exe$'){& $tool x -y $rar "$extract\"|Out-Null}
else{& $tool x -ibck -y $rar "$extract\"|Out-Null}
if($LASTEXITCODE -ne 0){throw 'Extracao R30 falhou'}
$baseline=Join-Path $extract 'pf2e_r30_work'
if(-not(Test-Path (Join-Path $baseline 'version.properties'))){throw 'Projeto R30 extraido invalido'}
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
$api='https://api.firestorage.ai/dev/file'
$meta=Invoke-RestMethod -Method Post -Uri "$api/shares/2m3V8KtBWbgZ/files/01a0eb42fb1e72ac8aff54c99a52653b/download"
if(-not $meta.downloadUrl){throw 'Perfil narrativo original indisponivel'}
$profile=Join-Path $root 'PERFIL_TECNICO_MESTRE_HUMANO_IMERSIVO_VTT.md'
Invoke-WebRequest -Uri $meta.downloadUrl -OutFile $profile
$sha=(Get-FileHash $profile -Algorithm SHA256).Hash.ToLowerInvariant()
if($sha -ne '8e63dbdf963abd1bed88e7173de3adc22d1501557647bcf4a9d95ee096f75852'){throw "Perfil narrativo original incorreto: $sha"}
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
