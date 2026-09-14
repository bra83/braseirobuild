import {test,expect} from '@playwright/test';
import {mkdirSync,readFileSync} from 'node:fs';
import path from 'node:path';

mkdirSync(path.resolve('captures'),{recursive:true});
mkdirSync(path.resolve('../web-ui-p1/captures'),{recursive:true});

const screens=[['session','active'],['session','encounter'],['session','combat'],['session','recovery'],['map','active'],['sheet','active'],['company','active']] as const;
const referenceName=(screen:string,state:string)=>screen==='map'?'P1_MAP_415x915.png':screen==='sheet'?'P1_SHEET_415x915.png':screen==='company'?'P1_COMPANY_415x915.png':`P1_SESSION_${state.toUpperCase()}_415x915.png`;

async function settle(page:any){
  await page.waitForLoadState('networkidle');
  await page.evaluate(async()=>{await document.fonts.ready;await Promise.all(Array.from(document.images).map((i:any)=>i.decode?.().catch(()=>null)));});
  await page.waitForTimeout(350);
}

// The approved P1 source is the visual authority. Runtime JS must remain byte-identical;
// index/CSS are copied verbatim by npm run build. This prevents a same-engine recapture
// from masking a source-level visual drift.
test('approved P1 source equivalence',async()=>{
  expect(readFileSync(path.resolve('p1.runtime.js'))).toEqual(readFileSync(path.resolve('../web-ui-p1/p1.js')));
  expect(readFileSync(path.resolve('dist/index.html'))).toEqual(readFileSync(path.resolve('../web-ui-p1/index.html')));
  expect(readFileSync(path.resolve('dist/p1.css'))).toEqual(readFileSync(path.resolve('../web-ui-p1/p1.css')));
});

for(const [screen,state] of screens){
  test(`${screen}-${state} 415x915 approved P1 shell`,async({page,context})=>{
    const failed:string[]=[];
    page.on('requestfailed',r=>failed.push(r.url()));
    await page.goto(`/?screen=${screen}&state=${state}`);
    await settle(page);
    expect(await page.evaluate(()=>[innerWidth,innerHeight])).toEqual([415,915]);
    expect(await page.locator('#device').getAttribute('data-screen')).toBe(screen);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=415&&document.documentElement.scrollHeight<=915)).toBeTruthy();
    const bad=await page.locator('img:visible').evaluateAll((xs:HTMLImageElement[])=>xs.filter(x=>!x.complete||x.naturalWidth<1).map(x=>x.src));
    expect(bad).toEqual([]);
    expect(failed).toEqual([]);
    if(screen==='session'){
      await expect(page.locator('[data-channel="PLAYER_REACTION"]')).toHaveCount(1);
      await expect(page.locator('.gm-help')).toBeVisible();
    }
    if(screen==='map'){
      await expect(page.locator('[data-geometry-version="1"]')).toHaveCount(2);
      await expect(page.locator('[data-direct-movement="forbidden"]')).toHaveCount(1);
    }
    await page.screenshot({path:path.resolve(`captures/P1_${screen.toUpperCase()}_${state.toUpperCase()}_415x915.png`),animations:'disabled'});

    // Re-render the locked, GUIDE-approved P1 source in the exact same Chromium process.
    // Static legacy PNGs remain historical evidence, while the equality gate becomes
    // independent of Chromium-version raster differences.
    const ref=await context.newPage();
    const refFailed:string[]=[];
    ref.on('requestfailed',r=>refFailed.push(r.url()));
    await ref.goto(`http://127.0.0.1:4174/web-ui-p1/index.html?screen=${screen}&state=${state}`);
    await settle(ref);
    expect(await ref.evaluate(()=>[innerWidth,innerHeight])).toEqual([415,915]);
    expect(refFailed).toEqual([]);
    const refBad=await ref.locator('img:visible').evaluateAll((xs:HTMLImageElement[])=>xs.filter(x=>!x.complete||x.naturalWidth<1).map(x=>x.src));
    expect(refBad).toEqual([]);
    await ref.screenshot({path:path.resolve(`../web-ui-p1/captures/${referenceName(screen,state)}`),animations:'disabled'});
    await ref.close();
  });
}

test('preview reaction and GM_HELP do not mutate locally without native bridge',async({page})=>{
  await page.goto('/?screen=session&state=active');
  await page.locator('#reaction').fill('descansar');
  await page.locator('.send-reaction').click();
  await expect(page.locator('#toast')).toContainText('preview');
  await page.locator('.gm-help').click();
  await expect(page.locator('#toast')).toContainText('GM_HELP');
});
