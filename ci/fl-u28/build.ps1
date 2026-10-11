$ErrorActionPreference='Stop'
Set-Location (Join-Path $env:FL_ROOT 'project')
& npm.cmd ci --no-audit --no-fund
if ($LASTEXITCODE -ne 0) { throw 'npm ci failed' }
foreach ($row in @(@('@rollup/rollup-win32-x64-msvc','rollup'),@('@esbuild/win32-x64','esbuild'))) {
  & node.exe -e "try{require.resolve('$($row[0])/package.json')}catch(e){process.exit(1)}"
  if ($LASTEXITCODE -ne 0) {
    $v=(& node.exe -p "require('./node_modules/$($row[1])/package.json').version").Trim()
    $packed=& npm.cmd pack "$($row[0])@$v" --json --ignore-scripts --pack-destination $env:FL_ROOT
    if ($LASTEXITCODE -ne 0) { throw 'Native Windows npm module unavailable' }
    $manifest=(($packed -join [Environment]::NewLine) | ConvertFrom-Json)
    $archive=Join-Path $env:FL_ROOT $manifest[0].filename
    $module=Join-Path (Join-Path (Get-Location) 'node_modules') $row[0]
    New-Item -ItemType Directory -Path $module -Force | Out-Null
    & tar.exe -xf $archive -C $module --strip-components=1
    if ($LASTEXITCODE -ne 0) { throw 'Native Windows npm module extraction failed' }
  }
}
& npm.cmd run lint
if ($LASTEXITCODE -ne 0) { throw 'U28 TypeScript compilation failed' }
& npm.cmd run android:sync
if ($LASTEXITCODE -ne 0) { throw 'U28 Capacitor sync failed' }
& npm.cmd run verify:android-assets
if ($LASTEXITCODE -ne 0) { throw 'U28 Android asset integrity check failed' }
& npm.cmd run verify:google-drive-signing
if ($LASTEXITCODE -ne 0) { throw 'Historical signing guard failed' }
& node.exe scripts/run-gradle-task.mjs assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'U28 Gradle assembleDebug failed' }
& node.exe scripts/run-gradle-task.mjs testDebugUnitTest
if ($LASTEXITCODE -ne 0) { throw 'U28 Android unit tests failed' }
