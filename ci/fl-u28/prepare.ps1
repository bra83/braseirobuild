$ErrorActionPreference='Stop'
if ($env:RUNNER_NAME -ne 'BRELGA') { throw "Expected BRELGA; got $env:RUNNER_NAME" }
if (-not(Test-Path 'D:\') -or (Get-PSDrive D).Free -lt 5GB) { throw 'BRELGA needs 5 GB on D:' }
$root="D:\brelga-fl-u28-$env:GITHUB_RUN_ID"
New-Item -ItemType Directory -Path $root -Force | Out-Null
"FL_ROOT=$root" | Out-File $env:GITHUB_ENV -Append -Encoding utf8
$env:GIT_TERMINAL_PROMPT='0'
if ($env:SOURCE_TOKEN) {
  $env:GIT_CONFIG_COUNT='1'
  $env:GIT_CONFIG_KEY_0='http.https://github.com/.extraheader'
  $env:GIT_CONFIG_VALUE_0='AUTHORIZATION: basic '+[Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("x-access-token:$env:SOURCE_TOKEN"))
}
try {
  & git.exe clone --depth 1 --single-branch --branch ci/u28-integrated 'https://github.com/bra83/braseiro-forbidden-lands.git' "$root\project"
  if ($LASTEXITCODE -ne 0) { throw 'U28 private checkout denied; set FL_SOURCE_READ_TOKEN with source repo read access' }
} finally {
  Remove-Item Env:GIT_CONFIG_COUNT,Env:GIT_CONFIG_KEY_0,Env:GIT_CONFIG_VALUE_0,Env:SOURCE_TOKEN -ErrorAction SilentlyContinue
}
$head=(& git.exe -C "$root\project" rev-parse HEAD).Trim()
if ($LASTEXITCODE -ne 0 -or $head -ne $env:SOURCE_SHA) { throw "U28 source drift: $head" }
Write-Host "EXACT_SOURCE_COMMIT=$head"
$donor="$root\$env:DONOR_NAME"
$cached=Get-ChildItem 'D:\' -Directory -Filter 'brelga-fl-u*' -ErrorAction SilentlyContinue |
  ForEach-Object { $candidate=Join-Path $_.FullName $env:DONOR_NAME; if(Test-Path $candidate) { Get-Item $candidate } } |
  Where-Object { (Get-FileHash $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() -eq $env:DONOR_SHA } |
  Select-Object -First 1
if ($cached) { Copy-Item $cached.FullName $donor }
else {
  $meta=Invoke-RestMethod -Uri ($env:DONOR_API+'/files?maxResults=100')
  $match=@($meta.files | Where-Object { $_.fileName -eq $env:DONOR_NAME })
  if ($match.Count -ne 1) { throw 'Historical donor missing. Refuse fresh debug signer.' }
  $ticket=Invoke-RestMethod -Method Post -Uri ($env:DONOR_API+'/files/'+$match[0].fileId+'/download')
  & curl.exe -fL --retry 3 -o $donor $ticket.downloadUrl
  if ($LASTEXITCODE -ne 0) { throw 'Historical donor download failed' }
}
if ((Get-FileHash $donor -Algorithm SHA256).Hash.ToLowerInvariant() -ne $env:DONOR_SHA) { throw 'Native donor digest mismatch' }
& python.exe "$env:GITHUB_WORKSPACE\ci\fl-u23\recover_native_config.py" "$root\project" $donor
if ($LASTEXITCODE -ne 0) { throw 'Native recovery failed' }
$options=@('C:\actions-runner\android-sdk',$env:ANDROID_HOME,$env:ANDROID_SDK_ROOT)
if ($env:LOCALAPPDATA) { $options+=(Join-Path $env:LOCALAPPDATA 'Android\Sdk') }
$sdk=$options | Where-Object { $_ -and (Test-Path (Join-Path $_ 'platforms\android-36')) -and (Test-Path (Join-Path $_ 'build-tools\36.0.0\aapt.exe')) } | Select-Object -First 1
if (-not $sdk) { throw 'SDK 36 or build-tools 36.0.0 missing' }
"ANDROID_HOME=$sdk" | Out-File $env:GITHUB_ENV -Append -Encoding utf8
"ANDROID_SDK_ROOT=$sdk" | Out-File $env:GITHUB_ENV -Append -Encoding utf8
"GRADLE_USER_HOME=$root\gradle-home" | Out-File $env:GITHUB_ENV -Append -Encoding utf8
