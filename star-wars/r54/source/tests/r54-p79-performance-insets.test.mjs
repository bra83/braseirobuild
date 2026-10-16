import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=p=>fs.readFileSync(p,'utf8');
const main=read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const manifest=read('app/src/main/AndroidManifest.xml');
const shell=read('app/src/main/assets/vtt/r46-shell.html');
const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const scheduler=read('app/src/main/assets/vtt/r54-dom-scheduler.js');
const p0=read('app/src/main/assets/vtt/r54-device-p0.js');
const p1=read('app/src/main/assets/vtt/r54-device-p1.js');
const p2=read('app/src/main/assets/vtt/r54-device-p2.js');
const p31=read('app/src/main/assets/vtt/r54-device-p31.js');
const p6=read('app/src/main/assets/vtt/r54-device-p6.js');
const map=read('app/src/main/assets/vtt/r54-final-map-travel.js');

test('P79 physically insets WebView inside system bars',()=>{
  assert.match(main,/rootView\.setOnApplyWindowInsetsListener/);
  assert.match(main,/applySystemInsetsToRoot\(\)/);
  assert.match(main,/rootView\.setPadding\(systemInsetLeft, systemInsetTop, systemInsetRight, systemInsetBottom\)/);
  assert.match(main,/WindowInsets\.Type\.systemBars\(\) \| WindowInsets\.Type\.displayCutout\(\)/);
  assert.match(main,/getStableInsetBottom\(\)/);
  assert.match(main,/onWindowFocusChanged/);
  assert.match(main,/rootView::requestApplyInsets/);
  assert.match(manifest,/android:hardwareAccelerated="true"/);
});

test('P79 loads a single coalesced global DOM scheduler before repair scripts',()=>{
  assert.ok(shell.indexOf('r54-dom-scheduler.js') < shell.indexOf('r54-device-p6.js'));
  assert.match(scheduler,/const tasks=new Map\(\)/);
  assert.match(scheduler,/new MutationObserver\(records=>/);
  assert.match(scheduler,/requestIdleCallback/);
  assert.match(scheduler,/suppressUntil/);
  for (const [name,src] of [['p0',p0],['p1',p1],['p6',p6],['map',map]]) {
    assert.match(src,/BraseiroDomScheduler\.register/,`${name} must register with scheduler`);
    assert.doesNotMatch(src,/new MutationObserver\(schedule\)/,`${name} must not create a global observer`);
  }
  assert.doesNotMatch(p2,/new MutationObserver/,'P2 empty observer removed');
  assert.match(p31,/BraseiroDomScheduler\.register\('device-p31'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('final-enhance'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('r53-tick'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('r54-tick'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('historical-visual'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('p26-runtime'/);
  assert.match(runtime,/BraseiroDomScheduler\.register\('p30-runtime'/);
});

test('P79 uses the optimized mobile galaxy map',()=>{
  for (const src of [p0,p1,map]) assert.match(src,/assets\/maps\/galaxy_map_mobile\.webp/);
  const original=fs.statSync('app/src/main/assets/vtt/assets/maps/galaxy_map.jpg').size;
  const mobile=fs.statSync('app/src/main/assets/vtt/assets/maps/galaxy_map_mobile.webp').size;
  assert.ok(mobile < original * 0.2,`mobile map ${mobile} should be <20% of ${original}`);
});

test('P79 keeps P78 gameplay/TTS fixes while bumping version',()=>{
  assert.match(shell,/data-version="1\.0\.(?:79|8[0-9]|9[0-9]|[1-9][0-9]{2,})"/);
  assert.match(runtime,/version:'1\.0\.(?:79|8[0-9]|9[0-9]|[1-9][0-9]{2,})'/);
  assert.match(runtime,/boundedVisualRoll/);
  assert.match(runtime,/campaignSetup/);
  assert.match(runtime,/MESTRE LOCAL/);
  assert.match(runtime,/final-published-last-roll/);
});
