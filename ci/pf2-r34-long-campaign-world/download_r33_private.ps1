param(
  [Parameter(Mandatory=$true)][string]$OutputZip,
  [Parameter(Mandatory=$true)][string]$OutputSha
)
$ErrorActionPreference='Stop'
if([string]::IsNullOrWhiteSpace($env:GITHUB_TOKEN)){throw 'GITHUB_TOKEN missing'}
$headers=@{
  Authorization="Bearer $env:GITHUB_TOKEN"
  Accept='application/octet-stream'
  'X-GitHub-Api-Version'='2022-11-28'
  'User-Agent'='BRELGA-PF2-R34'
}
Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri 'https://api.github.com/repos/bra83/braseirobuild/releases/assets/597592410' -OutFile $OutputZip
Invoke-WebRequest -UseBasicParsing -Headers $headers -Uri 'https://api.github.com/repos/bra83/braseirobuild/releases/assets/597592807' -OutFile $OutputSha
if(-not(Test-Path $OutputZip)){throw 'R33 source asset missing'}
if(-not(Test-Path $OutputSha)){throw 'R33 checksum asset missing'}
