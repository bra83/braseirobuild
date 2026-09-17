#!/usr/bin/env bash
set -euo pipefail
SHARES=(
'https://firestorage.ai/ja/f/qe47r3Xkf4KX'
'https://firestorage.ai/ja/f/_8f3hylcei_H'
'https://firestorage.ai/ja/f/FCyXOcjcnGTJ'
'https://firestorage.ai/ja/f/0s-YBpoGQYUj'
'https://firestorage.ai/ja/f/vmO1EweC_whK'
)
SOURCE_SHA256='91ae5ea70b0e309b6c381b77fb63497e43fecd6eda2dd54974f49b2be04a06d5'
mkdir -p incoming work out /tmp/dnd-download
cd /tmp/dnd-download
npm init -y >/dev/null 2>&1
npm install --silent playwright@1.55.0
npx playwright install chromium >/dev/null
cat > download.mjs <<'NODE'
import { chromium } from 'playwright';
import fs from 'fs';
const urls=process.argv.slice(2);
const names=['dnd103.part.00','dnd103.part.01','dnd103.part.02','dnd103.part.03','dnd103.part.04'];
const browser=await chromium.launch({headless:true});
for(let j=0;j<urls.length;j++){
  const ctx=await browser.newContext({acceptDownloads:true,locale:'ja-JP'});
  const page=await ctx.newPage();
  await page.goto(urls[j],{waitUntil:'domcontentloaded',timeout:60000});
  await page.waitForTimeout(1500);
  const out=process.env.GITHUB_WORKSPACE+'/incoming/'+names[j];
  let ok=false;
  for(const sel of ['text=/一括ダウンロード/i','text=/ダウンロード/i','text=/Download/i','button','a']){
    const loc=page.locator(sel), n=Math.min(await loc.count(),60);
    for(let i=0;i<n;i++){
      const el=loc.nth(i); let text=''; try{text=(await el.innerText({timeout:800})).trim()}catch{}
      if((sel==='button'||sel==='a')&&!/download|ダウンロード|一括|保存|ZIP|ファイル/i.test(text)) continue;
      try{const p=page.waitForEvent('download',{timeout:10000}); await el.click({timeout:5000}); const d=await p; await d.saveAs(out); ok=true; break}catch{}
    }
    if(ok) break;
  }
  await ctx.close();
  if(!ok || !fs.existsSync(out)) throw new Error('download failed '+j);
}
await browser.close();
NODE
node download.mjs "${SHARES[@]}"
cd "$GITHUB_WORKSPACE"
cat incoming/dnd103.part.* > incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip
echo "$SOURCE_SHA256  incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip" | sha256sum -c -
unzip -q incoming/BRASEIRO_DND_6.6.2_RUN103_DND5E_ANDROID_STUDIO.zip -d work
MAIN="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/MainActivity.java' | head -n1)"
SRD="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/Srd51BestiaryBatchEngine.java' | head -n1)"
python3 ci/dnd-run103/fix2_compile.py "$MAIN" "$SRD"
export ANDROID_HOME="${ANDROID_HOME:-/usr/local/lib/android/sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
yes | sdkmanager --licenses >/dev/null || true
sdkmanager 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0' >/dev/null
WRAPPER="$(find work -type f -path '*/android/gradlew' | head -n1)"
ROOT="$(dirname "$WRAPPER")"
chmod +x "$ROOT/gradlew"
cd "$ROOT"
./gradlew clean :app:assembleDebug --stacktrace --console=plain --no-daemon
APK="$(find app/build/outputs/apk/debug -type f -name '*.apk' | head -n1)"
test -f "$APK"
cp "$APK" "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX2_DEBUG.apk"
sha256sum "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX2_DEBUG.apk" > "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX2_DEBUG.apk.sha256"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX2_DEBUG.apk" | tee "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
grep -q "package: name='com.braseiro.dnd5e'" "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
unzip -t "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX2_DEBUG.apk" >/dev/null
printf '%s\n' 'RUN103 FIX2 compile-only; maps/assets/gameplay unchanged.' > "$GITHUB_WORKSPACE/out/BUILD_FIX.txt"
