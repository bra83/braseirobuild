$ErrorActionPreference='Stop'
$repoRoot=(Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
if($env:RUNNER_NAME -ne 'BRELGA'){ throw "Expected BRELGA, got $env:RUNNER_NAME" }

$root=Join-Path $env:RUNNER_TEMP 'pf2-r34-final'
$work=Join-Path $root 'work'
$out=Join-Path $root 'out'
Remove-Item $root -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $root,$work,$out -Force | Out-Null

Write-Host '=== BASELINE: exact published R33 source ==='
$zip=Join-Path $root 'r33-source.zip'
$shaFile=Join-Path $root 'r33-source.sha256'
& (Join-Path $PSScriptRoot 'download_r33_private.ps1') -OutputZip $zip -OutputSha $shaFile
$expected=((Get-Content $shaFile -Raw).Trim().Split()[0]).ToLowerInvariant()
$actual=(Get-FileHash $zip -Algorithm SHA256).Hash.ToLowerInvariant()
if($expected -ne $actual){throw "R33 checksum mismatch $actual != $expected"}
Expand-Archive -Path $zip -DestinationPath $work -Force
$project=$work
if(-not(Test-Path (Join-Path $project 'version.properties'))){
  $vp=Get-ChildItem $work -Recurse -Filter version.properties -File | Select-Object -First 1
  if(-not $vp){throw 'R33 source root not found'}
  $project=$vp.DirectoryName
}
$baseline=Get-Content (Join-Path $project 'version.properties') -Raw
if(-not $baseline.Contains('VERSION_NAME=9.0.34')){throw 'Wrong baseline; expected 9.0.34'}

Write-Host '=== APPLY R34 ==='
python (Join-Path $PSScriptRoot 'patch.py') $project
if($LASTEXITCODE -ne 0){throw 'R34 patch failed'}
Copy-Item (Join-Path $PSScriptRoot 'test_r34_long_campaign_world.py') (Join-Path $project 'tests_pf2\test_r34_long_campaign_world.py') -Force
Write-Host '=== FEATURE GATES ==='
$java=Join-Path $project 'android\app\src\main\java\com\braseiro\pathfinder2e'
$main=Get-Content (Join-Path $java 'MainActivity.java') -Raw
$solo=Get-Content (Join-Path $java 'KingmakerSoloDirector.java') -Raw
$repo=Get-Content (Join-Path $java 'KingmakerRepository.java') -Raw
$policy=Get-Content (Join-Path $java 'KingmakerPreludePolicy.java') -Raw
$chunk=Get-Content (Join-Path $java 'NarrationChunker.java') -Raw
$profile=Get-Content (Join-Path $java 'HumanGmNarrativeProfile.java') -Raw
$causal=Get-Content (Join-Path $java 'WorldCausalityEngine.java') -Raw
$catalog=Get-Content (Join-Path $java 'WorldCausalCatalog.java') -Raw
$calendar=Get-Content (Join-Path $java 'WorldCalendarEngine.java') -Raw
$settlement=Get-Content (Join-Path $java 'LivingSettlementEngine.java') -Raw
$simulation=Get-Content (Join-Path $java 'LivingWorldSimulationEngine.java') -Raw
$continuity=Get-Content (Join-Path $java 'KingmakerContinuityContract.java') -Raw
$appDb=Get-Content (Join-Path $java 'AppDb.java') -Raw
$version=Get-Content (Join-Path $project 'version.properties') -Raw
$checks=[ordered]@{
 'version-9.0.35'=$version.Contains('VERSION_NAME=9.0.35') -and $version.Contains('VERSION_CODE=90035') -and $version.Contains('DB_SCHEMA=60')
 'db-schema-60'=$appDb.Contains('private static final int DB_VERSION = 60;') -and $appDb.Contains('if (oldVersion < 60)')
 'restov-opening'=$main.Contains('Restov, Rostland, Brevoy') -and $policy.Contains('CHEGANDO A RESTOV')
 'restov-sandbox'=$solo.Contains('go_jamandi_mansion') -and $policy.Contains('campanha principal ainda NÃO começou')
 'kokoro-two-layer'=$chunk.Contains('KOKORO_FIRST_CHAPTER_MAX = 320') -and $chunk.Contains('KOKORO_CHAPTER_MAX = 680') -and $chunk.Contains('KOKORO_ANDROID_MAX = 150')
 'major-scenes'=$main.Contains('5.000 a 6.000 caracteres') -and $profile.Contains('5.000 a 6.000 caracteres')
 'causal-catalog-4096'=$catalog.Contains('EXPECTED_PATTERN_COUNT = 4096')
 'causal-three-waves'=$causal.Contains('propagate(shocks,targetDay,3)')
 'causal-threshold-chains'=$causal.Contains('resolveThresholdChains') -and $causal.Contains('deficit_demografico') -and $causal.Contains('crise_comercial')
 'war-locality'=$causal.Contains('Regiões distantes não recebem mortalidade de guerra direta') -and (-not $causal.Contains('else if(best<.30d)best=.18d'))
 'war-demography'=$catalog.Contains('adult_men_extra_loss_ppm') -and $catalog.Contains('soldier_loss_ppm')
 'food-health-trade'=$catalog.Contains('food_production_delta_pct') -and $catalog.Contains('disease_delta') -and $catalog.Contains('commodity_shock')
 'calendar-seasons-years'=$calendar.Contains('GOLARION_DAYS_PER_YEAR = 360') -and $calendar.Contains('season_started') -and $calendar.Contains('year_started')
 'no-180-day-loss'=(-not $settlement.Contains('Math.min(180,targetDay-last)')) -and $settlement.Contains('while(cursor<targetDay)')
 'faction-backlog'=(-not $simulation.Contains('accumulated-(due*interval)')) -and $simulation.Contains('accumulated-(executable*interval)')
 'restore-fingerprint'=$continuity.Contains('causal_world_fingerprint') -and $repo.Contains('campaign_story_phase')
}
$featureLog=Join-Path $out 'FEATURE_GATES.log'
foreach($e in $checks.GetEnumerator()){
  $status=if($e.Value){'PASS'}else{'FAIL'}
  "$status - $($e.Key)" | Tee-Object -FilePath $featureLog -Append
}
if(@($checks.GetEnumerator()|Where-Object{-not $_.Value}).Count -gt 0){throw 'R34 feature gates failed'}

Write-Host '=== PYTEST ==='
python -m pip install -q pytest
if($LASTEXITCODE -ne 0){throw 'pytest install failed'}
Push-Location $project
try{
  $focused=Join-Path $out 'FOCUSED_TESTS.log'
  $focusedTests=@('tests_pf2\test_r34_long_campaign_world.py')
  if(Test-Path 'tests_pf2\test_r32_integrated.py'){$focusedTests=@('tests_pf2\test_r32_integrated.py')+$focusedTests}
  if(Test-Path 'tests_pf2\test_r33_restov_kokoro.py'){$focusedTests=@('tests_pf2\test_r33_restov_kokoro.py')+$focusedTests}
  python -m pytest @focusedTests -q 2>&1 | Tee-Object -FilePath $focused
  if($LASTEXITCODE -ne 0){throw 'Focused baseline + R34 tests failed'}

  $kingLog=Join-Path $out 'KINGMAKER_TESTS.log'
  $tests=Get-ChildItem tests_pf2 -File -Filter 'test_kingmaker*.py' | Sort-Object Name | ForEach-Object {$_.FullName}
  python -m pytest @tests -q 2>&1 | Tee-Object -FilePath $kingLog
  if($LASTEXITCODE -ne 0){throw 'Kingmaker regression failed'}

  $fullLog=Join-Path $out 'FULL_PF2_TESTS.log'
  python -m pytest tests_pf2 -q 2>&1 | Tee-Object -FilePath $fullLog
  if($LASTEXITCODE -ne 0){throw 'Full PF2 regression failed'}
} finally { Pop-Location }

Write-Host '=== ANDROID SDK ==='
$candidates=@('C:\actions-runner\android-sdk',$env:ANDROID_SDK_ROOT,$env:ANDROID_HOME)
if($env:LOCALAPPDATA){$candidates+=(Join-Path $env:LOCALAPPDATA 'Android\Sdk')}
$sdk=$null;$mgr=$null
foreach($c in ($candidates|Where-Object{$_}|Select-Object -Unique)){
  if(Test-Path $c){
    $found=Get-ChildItem $c -Recurse -Filter sdkmanager.bat -File -ErrorAction SilentlyContinue|Select-Object -First 1
    if($found){$sdk=$c;$mgr=$found.FullName;break}
  }
}
if(-not $mgr){throw 'Android SDK not found'}
$env:ANDROID_HOME=$sdk;$env:ANDROID_SDK_ROOT=$sdk
1..100|ForEach-Object{'y'}|& $mgr --licenses|Out-Null
& $mgr 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'
if($LASTEXITCODE -ne 0){throw 'sdkmanager failed'}

Write-Host '=== GRADLE BUILD ==='
Push-Location $project
try{
  $gradleLog=Join-Path $out 'GRADLE_BUILD.log'
  & .\gradlew.bat :app:assembleDebug --stacktrace --no-daemon 2>&1 | Tee-Object -FilePath $gradleLog
  if($LASTEXITCODE -ne 0){throw 'Gradle build failed'}
  $apk=Get-ChildItem . -Recurse -Filter '*.apk' -File|Where-Object{$_.FullName -match '[\\/]build[\\/]outputs[\\/]apk[\\/]debug[\\/]'}|Select-Object -First 1
  if(-not $apk){throw 'APK missing after successful Gradle'}
} finally { Pop-Location }

Write-Host '=== PACKAGE ==='
$apkOut=Join-Path $out 'Braseiro_Pathfinder2e_9.0.35_R34_LONG_CAMPAIGN_WORLD_DEBUG.apk'
Copy-Item $apk.FullName $apkOut -Force
(Get-FileHash $apkOut -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content "$apkOut.sha256" -Encoding ascii

$validation=@'
# VALIDAÇÃO — BRASEIRO PATHFINDER 2E 9.0.35 / R34

- Baseline: fonte publicada 9.0.34/R33, validada pelo SHA-256 da própria release.
- Banco migrado explicitamente para schema 60.
- 4.096 padrões causais estruturados.
- Demografia persistente: população, homens adultos, mulheres adultas, crianças, idosos, soldados e deslocados.
- Guerra real (Warfare) afeta diretamente somente a área envolvida; regiões distantes sofrem efeitos por comércio, migração, política e abastecimento.
- Alimentos, bebidas, medicamentos, produção, preços, comércio, saúde, doença, migração, infraestrutura e estabilidade política persistem.
- Choques de commodities propagam até três ondas.
- Desdobramentos explícitos: fome, epidemia, deslocados/refugiados, déficit demográfico/mão de obra, crise política, crise comercial, crime alto e falta de atendimento/medicamentos.
- NPCs e facções continuam agindo por estado persistido.
- Dias, meses, estações e anos avançam pelo calendário.
- Feiras/festivais e marcos sazonais/anuais são gerados de forma determinística e idempotente.
- Corrigida perda de simulação acima de 180 dias.
- Corrigida perda de backlog de facções em grandes saltos temporais.
- Backup/restore compara fase Kingmaker e fingerprint causal em backups R34+, preservando compatibilidade com backups antigos.
- Teste de referência simula dez anos e choque de abastecimento entre região exportadora e importadora.
'@
[System.IO.File]::WriteAllText((Join-Path $out 'VALIDACAO_R34.md'),$validation,(New-Object System.Text.UTF8Encoding($false)))

$changed=@(
 'WorldCalendarEngine.java','WorldCausalCatalog.java','WorldCausalityEngine.java',
 'AppDb.java','LivingSettlementEngine.java','LivingWorldSimulationEngine.java','LivingWorldEngine.java',
 'WorldSixCoordinator.java','WorldEventConsequenceEngine.java','KingmakerRepository.java',
 'KingmakerSoloDirector.java','KingmakerContinuityContract.java','version.properties'
)
$changed|Set-Content (Join-Path $out 'CHANGED_FILES_R34.txt') -Encoding utf8

$sourceOut=Join-Path $out 'BRASEIRO_PATHFINDER2E_9.0.35_R34_LONG_CAMPAIGN_WORLD_ANDROID_STUDIO.zip'
Push-Location $project
try{
  $seven=@('C:\Program Files\7-Zip\7z.exe','C:\Program Files (x86)\7-Zip\7z.exe')|Where-Object{Test-Path $_}|Select-Object -First 1
  if(-not $seven){$cmd=Get-Command 7z.exe -ErrorAction SilentlyContinue;if($cmd){$seven=$cmd.Source}}
  if($seven){& $seven a -tzip -mx=5 $sourceOut . '-xr!**\build\*' '-xr!**\.gradle\*' '-xr!local.properties'}
  else{& tar.exe -a -c -f $sourceOut --exclude='*/build/*' --exclude='*/.gradle/*' --exclude='local.properties' .}
  if($LASTEXITCODE -ne 0){throw 'Source ZIP failed'}
} finally { Pop-Location }
(Get-FileHash $sourceOut -Algorithm SHA256).Hash.ToLowerInvariant() | Set-Content "$sourceOut.sha256" -Encoding ascii

Write-Host '=== RELEASE ==='
if([string]::IsNullOrWhiteSpace($env:GITHUB_TOKEN)){throw 'GITHUB_TOKEN missing'}
$repo=$env:GITHUB_REPOSITORY
$tag='PATHFINDER2E-9.0.35-R34'
$headers=@{'Authorization'="Bearer $env:GITHUB_TOKEN";'Accept'='application/vnd.github+json';'X-GitHub-Api-Version'='2022-11-28';'User-Agent'='BRELGA-PF2-R34'}
try{$release=Invoke-RestMethod -Headers $headers -Uri "https://api.github.com/repos/$repo/releases/tags/$tag"}
catch{
  $payload=@{
    tag_name=$tag;target_commitish='pf2-r34-long-campaign-world-20260929'
    name='Braseiro Pathfinder 2e 9.0.35 R34 - Long Campaign Causal World'
    body='Long-campaign persistence and autonomous causal world over the exact published 9.0.34/R33 source.'
    draft=$false;prerelease=$false
  }|ConvertTo-Json -Compress
  $payloadFile=Join-Path $root 'release.json'
  $responseFile=Join-Path $root 'release-response.json'
  [System.IO.File]::WriteAllText($payloadFile,$payload,(New-Object System.Text.UTF8Encoding($false)))
  & curl.exe -L --fail --silent --show-error --request POST --header "Authorization: Bearer $env:GITHUB_TOKEN" --header "Accept: application/vnd.github+json" --header "X-GitHub-Api-Version: 2022-11-28" --header "Content-Type: application/json; charset=utf-8" --data-binary "@$payloadFile" "https://api.github.com/repos/$repo/releases" --output $responseFile
  if($LASTEXITCODE -ne 0){throw 'Release creation failed'}
  $release=Get-Content $responseFile -Raw -Encoding UTF8|ConvertFrom-Json
}
foreach($file in Get-ChildItem $out -File){
  $existing=$release.assets|Where-Object{$_.name -eq $file.Name}|Select-Object -First 1
  if($existing){Invoke-RestMethod -Method Delete -Headers $headers -Uri "https://api.github.com/repos/$repo/releases/assets/$($existing.id)"|Out-Null}
  $encoded=[uri]::EscapeDataString($file.Name)
  $upload="https://uploads.github.com/repos/$repo/releases/$($release.id)/assets?name=$encoded"
  & curl.exe -L --fail --retry 5 --retry-delay 2 --request POST --header "Authorization: Bearer $env:GITHUB_TOKEN" --header "Accept: application/vnd.github+json" --header "X-GitHub-Api-Version: 2022-11-28" --header "Content-Type: application/octet-stream" --data-binary "@$($file.FullName)" $upload
  if($LASTEXITCODE -ne 0){throw "Upload failed: $($file.Name)"}
}
Write-Host "RELEASE_URL=$($release.html_url)"
Write-Host 'R34 COMPLETE'
