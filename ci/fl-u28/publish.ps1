$ErrorActionPreference='Stop'
$out=Join-Path $env:FL_ROOT 'release'
$required=@('BRASEIRO_FORBIDDEN_LANDS_3.3.25_U28_INTEGRATED.apk',
  'BRASEIRO_FORBIDDEN_LANDS_3.3.25_U28_INTEGRATED.apk.sha256',
  'BUILD_PROOF_U28.json','APK_IDENTITY.txt','APK_SIGNATURE.txt')
foreach($name in $required){if(-not(Test-Path (Join-Path $out $name))){throw "Missing required release asset: $name"}}
$files=@(Get-ChildItem $out -File)
if($files.Count -ne $required.Count){throw 'Unexpected release assets'}
$tag="forbidden-lands-u28-3.3.25-$env:GITHUB_RUN_ID"
$api="https://api.github.com/repos/$env:GITHUB_REPOSITORY/releases"
$headers=@{
  Authorization="Bearer $env:GH_TOKEN"
  Accept='application/vnd.github+json'
  'X-GitHub-Api-Version'='2022-11-28'
}
$notes=@(
  'Forbidden Lands U28 integrado 3.3.25, compilado no runner BRELGA.',
  "SOURCE_COMMIT=$env:SOURCE_SHA",
  'Matriz automatizada 43 encontros / 121 escolhas / 82 consequências: gate PASS na execução #38178048976.',
  'Verificações Android: assinatura histórica, identidade, versionCode, hashes e assets web internos.',
  'PRÉ-RELEASE DE TESTE: testes físicos em Galaxy e motores TTS externos não certificados.'
) -join [Environment]::NewLine
$body=@{tag_name=$tag;target_commitish=$env:GITHUB_SHA;
  name='Forbidden Lands U28 3.3.25 - BRELGA';body=$notes;draft=$true;prerelease=$true} | ConvertTo-Json -Depth 5
$release=Invoke-RestMethod -Method Post -Uri $api -Headers $headers -Body ([Text.Encoding]::UTF8.GetBytes($body)) -ContentType 'application/json; charset=utf-8'
if(-not $release.id -or -not $release.upload_url){throw 'GitHub draft release failed'}
$base=([string]$release.upload_url).Split('{')[0]
foreach($file in $files){
  $uri=$base+'?name='+[Uri]::EscapeDataString($file.Name)
  $uploaded=Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -InFile $file.FullName -ContentType 'application/octet-stream'
  if($uploaded.state -ne 'uploaded' -or [int64]$uploaded.size -ne [int64]$file.Length){
    throw "Upload mismatch: $($file.Name); draft retained"
  }
}
$inventory=Invoke-RestMethod -Method Get -Uri "$api/$($release.id)/assets?per_page=100" -Headers $headers
$serverFiles=New-Object 'System.Collections.Generic.List[object]'
foreach($entry in $inventory){
  if($entry -is [array]){foreach($child in $entry){[void]$serverFiles.Add($child)}}
  else{[void]$serverFiles.Add($entry)}
}
if($serverFiles.Count -ne $files.Count){throw 'Release inventory mismatch; draft retained'}
foreach($file in $files){
  $matches=0
  foreach($entry in $serverFiles){
    if([string]$entry.name -eq [string]$file.Name -and [int64]$entry.size -eq [int64]$file.Length){$matches++}
  }
  if($matches -ne 1){throw "Readback asset mismatch $($file.Name); draft retained"}
}
$published=Invoke-RestMethod -Method Patch -Uri "$api/$($release.id)" -Headers $headers -Body '{"draft":false,"prerelease":true}' -ContentType 'application/json'
if($published.draft -ne $false -or $published.tag_name -ne $tag){throw 'Release not published'}
Write-Host "U28_RELEASE_URL=$($published.html_url)"
