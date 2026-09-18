#!/usr/bin/env bash
set -euo pipefail
SHARES=(
'https://firestorage.ai/ja/f/Sy3YCjPaN7Sm'
'https://firestorage.ai/ja/f/Iq4CwCL2TVk3'
'https://firestorage.ai/ja/f/DeupuleyZ282'
'https://firestorage.ai/ja/f/RxP4P8Zf7PIe'
'https://firestorage.ai/ja/f/4TfPzIrf8u7n'
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

async function tryRequest(ctx, rawUrl, out) {
  try {
    const u=new URL(rawUrl);
    if (!/^https?:$/.test(u.protocol)) return false;
    const r=await ctx.request.get(u.toString(), {timeout:60000});
    const h=r.headers();
    const ct=(h['content-type']||'').toLowerCase();
    const cd=(h['content-disposition']||'').toLowerCase();
    const len=Number(h['content-length']||0);
    if (r.ok() && (cd.includes('attachment') || ct.includes('octet-stream') || ct.includes('zip') || len>1000000)) {
      const body=await r.body();
      if (body.length>1000000) {
        fs.writeFileSync(out, body);
        console.log('DIRECT_OK', u.toString(), body.length, ct, cd);
        return true;
      }
    }
  } catch (e) {
    console.log('DIRECT_FAIL', rawUrl, String(e).slice(0,240));
  }
  return false;
}

async function clickForDownload(page, locator, out, label) {
  try {
    if (!(await locator.isVisible({timeout:1200}))) return false;
    const p=page.waitForEvent('download',{timeout:30000});
    await locator.click({timeout:5000});
    const d=await p;
    await d.saveAs(out);
    console.log('CLICK_DOWNLOAD_OK', label, await d.suggestedFilename());
    return true;
  } catch (e) {
    console.log('CLICK_NO_DOWNLOAD', label, String(e).slice(0,180));
    return false;
  }
}

for(let j=0;j<urls.length;j++){
  const ctx=await browser.newContext({acceptDownloads:true,locale:'ja-JP'});
  const page=await ctx.newPage();
  const out=process.env.GITHUB_WORKSPACE+'/incoming/'+names[j];
  console.log('OPEN', j, urls[j]);
  await page.goto(urls[j],{waitUntil:'domcontentloaded',timeout:60000});
  await page.waitForTimeout(5000);
  console.log('PAGE', await page.title(), page.url());

  let ok=false;
  // First try any explicit direct links already present in the DOM.
  const hrefs=await page.locator('a[href]').evaluateAll(as=>as.map(a=>({href:a.href,text:(a.innerText||a.textContent||'').trim(),download:a.getAttribute('download')||''})));
  for (const a of hrefs) {
    if (/download|file|asset|storage|object|blob|zip/i.test(a.href+' '+a.text+' '+a.download)) {
      console.log('CANDIDATE_LINK', JSON.stringify(a));
      if (await tryRequest(ctx,a.href,out)) { ok=true; break; }
    }
  }

  // Then click download-looking controls. Firestorage free pages may insert an ad/countdown step.
  if (!ok) {
    const candidates=[];
    for (const sel of ['button','a','[role="button"]','input[type="submit"]']) {
      const loc=page.locator(sel); const n=Math.min(await loc.count(),100);
      for(let i=0;i<n;i++) {
        const el=loc.nth(i);
        let text='';
        try { text=((await el.innerText({timeout:500}))||'').trim(); } catch {}
        let aria=''; try { aria=await el.getAttribute('aria-label')||''; } catch {}
        let title=''; try { title=await el.getAttribute('title')||''; } catch {}
        const desc=(text+' '+aria+' '+title).trim();
        if (/download|ダウンロード|一括|保存|zip|ファイル/i.test(desc)) candidates.push({el,desc:desc.slice(0,160)});
      }
    }
    console.log('CLICK_CANDIDATES', candidates.map(x=>x.desc));
    for (const c of candidates) {
      if (await clickForDownload(page,c.el,out,c.desc)) { ok=true; break; }
      await page.waitForTimeout(6000);
      // After first click, an ad/countdown may reveal a second download control or direct href.
      const secondHrefs=await page.locator('a[href]').evaluateAll(as=>as.map(a=>({href:a.href,text:(a.innerText||a.textContent||'').trim(),download:a.getAttribute('download')||''})));
      for (const a of secondHrefs) {
        if (/download|file|asset|storage|object|blob|zip/i.test(a.href+' '+a.text+' '+a.download)) {
          console.log('SECOND_LINK', JSON.stringify(a));
          if (await tryRequest(ctx,a.href,out)) { ok=true; break; }
        }
      }
      if(ok) break;
      const second=page.getByText(/download|ダウンロード|一括|保存|ファイル/i).first();
      if(await second.count()) {
        if(await clickForDownload(page,second,out,'SECOND_TEXT')) {ok=true; break;}
      }
    }
  }

  if(!ok || !fs.existsSync(out) || fs.statSync(out).size<1000000) {
    const snapshot=await page.locator('body').innerText().catch(()=> '');
    console.log('BODY_SNAPSHOT', snapshot.slice(0,6000));
    const all=await page.locator('a[href],button,[role="button"]').evaluateAll(es=>es.slice(0,150).map(e=>({tag:e.tagName,text:(e.innerText||e.textContent||'').trim().slice(0,180),href:e.href||'',aria:e.getAttribute('aria-label')||''})));
    console.log('DOM_CONTROLS', JSON.stringify(all).slice(0,12000));
    await ctx.close();
    throw new Error('download failed '+j);
  }
  console.log('PART_OK',j,fs.statSync(out).size);
  await ctx.close();
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
ORCH="$(find work -type f -path '*/android/app/src/main/java/com/braseiro/dnd5e/MasterOrchestrator.java' | head -n1)"
test -f "$MAIN" && test -f "$SRD" && test -f "$ORCH"
python3 ci/dnd-run103/fix2_compile.py "$MAIN" "$SRD" "$ORCH"
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
cp "$APK" "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX7_DEBUG.apk"
sha256sum "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX7_DEBUG.apk" > "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX7_DEBUG.apk.sha256"
"$ANDROID_HOME/build-tools/35.0.0/aapt" dump badging "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX7_DEBUG.apk" | tee "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
grep -q "package: name='com.braseiro.dnd5e'" "$GITHUB_WORKSPACE/out/APK_BADGING.txt"
unzip -t "$GITHUB_WORKSPACE/out/BRASEIRO_DND_6.6.2_RUN103_FIX7_DEBUG.apk" >/dev/null
printf '%s\n' 'RUN103 FIX7 compile-only; maps/assets/gameplay unchanged.' > "$GITHUB_WORKSPACE/out/BUILD_FIX.txt"
