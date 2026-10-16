import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=p=>fs.readFileSync(new URL('../'+p, import.meta.url),'utf8');

test('P74: Android version no longer reports 1.0.0',()=>{
  const gradle=read('app/build.gradle.kts');
  const shell=read('app/src/main/assets/vtt/r46-shell.html');
  const code=Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]||0);
  const version=gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1]||'';
  assert.ok(code>=10074);
  assert.notEqual(version,'1.0.0');
  assert.ok(shell.includes(`data-version="${version}"`));
});

test('P74: Android reads modern system bars and exports bottom safe inset',()=>{
  const java=read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
  const css=read('app/src/main/assets/vtt/r46/globals.css');
  assert.match(java,/setDecorFitsSystemWindows\(false\)/);
  assert.match(java,/WindowInsets\.Type\.systemBars\(\)/);
  assert.match(java,/WindowInsets\.Type\.displayCutout\(\)/);
  assert.match(java,/--android-safe-bottom/);
  assert.match(css,/bottom-navigation\{[^}]*height:calc\(var\(--nav-h\) \+ var\(--safe-bottom\)\)/s);
  assert.match(css,/padding:0 \.2rem var\(--safe-bottom\)/);
});

test('P74: campaign hub exposes restart, new world/selection and full reset',()=>{
  const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
  assert.match(runtime,/☰ CAMPANHA/);
  assert.match(runtime,/REINICIAR AVENTURA/);
  assert.ok(runtime.includes('NOVO MUNDO / VOLTAR À SELEÇÃO'));
  assert.match(runtime,/ZERAR CAMPANHA INTEIRA/);
  assert.match(runtime,/restartSandboxPreservingParty/);
  assert.match(runtime,/bridgeResult\('newCampaign','Nova campanha'\)/);
  assert.match(runtime,/Duty e Obligation continuam separados/);
});

test('P74: UI bridge exposes canonical full newCampaign without changing Duty/Obligation logic',()=>{
  const bridge=read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
  assert.match(bridge,/newCampaign\(name = 'Nova campanha'\)/);
  assert.match(bridge,/game\.newCampaign/);
  const controller=read('app/src/main/assets/vtt/js/core/controller.mjs');
  assert.match(controller,/modeAllowsObligation/);
  assert.match(controller,/modeAllowsDuty/);
  assert.match(controller,/restartSandboxPreservingParty/);
});
