import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync, statSync } from 'node:fs';

const read = (p) => readFileSync(new URL(`../${p}`, import.meta.url), 'utf8');

test('FINAL: Android Studio project has deterministic Gradle launcher and compatible pins', () => {
  const root = read('build.gradle.kts');
  const app = read('app/build.gradle.kts');
  const wrapper = read('gradle/wrapper/gradle-wrapper.properties');
  assert.match(root, /com\.android\.application"\) version "8\.13\.2"/);
  assert.match(app, /compileSdk = 36/);
  assert.match(app, /targetSdk = 36/);
  const versionCode=Number(app.match(/versionCode\s*=\s*(\d+)/)?.[1]||0);
  const versionName=app.match(/versionName\s*=\s*"([^"]+)"/)?.[1]||'';
  assert.ok(versionCode>=10074);
  assert.notEqual(versionName,'1.0.0');
  assert.match(wrapper, /gradle-8\.13-bin\.zip/);
  assert.match(wrapper, /distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78/);
  assert.ok(existsSync(new URL('../gradlew', import.meta.url)));
  assert.ok(existsSync(new URL('../gradlew.bat', import.meta.url)));
});

test('FINAL: R46 shell bootstrap is fully offline classic JS, no file-module CORS dependency', () => {
  const html = read('app/src/main/assets/vtt/r46-shell.html');
  assert.doesNotMatch(html, /type="module"/);
  assert.match(html, /js\/core\/uiBridge\.bundle\.js/);
  assert.match(html, /r46\/r46-shell\.bundle\.js/);
  assert.match(html, /r46\/final-runtime\.js/);
  assert.ok(statSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.bundle.js', import.meta.url)).size > 100_000);
});

test('FINAL: WebView exposes only required bridges and blocks universal file access', () => {
  const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
  assert.match(main, /setAllowUniversalAccessFromFileURLs\(false\)/);
  assert.match(main, /setAllowFileAccessFromFileURLs\(false\)/);
  assert.match(main, /BraseiroLibrary/);
  assert.match(main, /BraseiroGemini/);
  assert.match(main, /BraseiroImages/);
  assert.match(main, /BraseiroTts/);
  assert.match(main, /BraseiroFiles/);
  assert.match(main, /onShowFileChooser/);
  assert.match(main, /shouldOverrideUrlLoading/);
  assert.match(main, /file:\/\/\/android_asset\/vtt\/r46-shell\.html/);
});

test('FINAL: native PT-BR TTS exists and has bounded visible-text contract', () => {
  const tts = read('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
  assert.match(tts, /Locale\.forLanguageTag\("pt-BR"\)/);
  assert.match(tts, /MAX_TEXT_CHARS = 12000/);
  assert.match(tts, /TextToSpeech\.QUEUE_FLUSH/);
  assert.match(tts, /@JavascriptInterface[\s\S]*String speak/);
  const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
  assert.match(runtime, /scene-narrative-paper:not\(\.scene-narrative-reader\)/);
  assert.match(runtime, /BraseiroTts/);
  assert.match(runtime, /Ouvir narração do Mestre/);
});

test('FINAL: JSON export/import is Android-capable', () => {
  const files = read('app/src/main/java/com/braseiro/starwarsedge/FileBridge.java');
  const bundle = read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
  const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
  assert.match(files, /MediaStore\.Downloads/);
  assert.match(files, /Braseiro Star Wars/);
  assert.match(bundle, /window\.BraseiroFiles/);
  assert.match(bundle, /nativeFiles\.saveJson/);
  assert.match(main, /ACTION_OPEN_DOCUMENT/);
  assert.match(main, /FileChooserParams\.parseResult/);
});

test('FINAL: accessibility settings are functional and persistent', () => {
  const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
  const css = read('app/src/main/assets/vtt/r46/final-runtime.css');
  assert.match(runtime, /braseiro\.starwars\.ui\.preferences\.v1/);
  assert.match(runtime, /final-text-scale/);
  assert.match(runtime, /final-high-contrast/);
  assert.match(runtime, /final-reduced-motion/);
  assert.match(css, /braseiro-text-120/);
  assert.match(css, /braseiro-high-contrast/);
  assert.match(css, /braseiro-reduced-motion/);
});


test('FINAL: canonical engine bundle is synchronized with live scene bridge', () => {
  const source = read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const bundle = read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
  const contract = read('shell-r46-run87/gameBridgeClient.ts');
  assert.match(source, /scenePlayerView\(\)/);
  assert.match(source, /resolveSuggestedMasterCheck\(\)/);
  const app = read('app/build.gradle.kts');
  const version=app.match(/versionName\s*=\s*"([^"]+)"/)?.[1]||'';
  assert.ok(bundle.includes(`Braseiro FINAL ${version} offline engine bundle`));
  assert.match(bundle, /scenePlayerView\(\)/);
  assert.match(bundle, /resolveSuggestedMasterCheck\(\)/);
  assert.match(bundle, /modules:(?:6[5-9]|[7-9]\d|[1-9]\d{2,})/);
  assert.match(bundle, /adventure\/beyondTheRim_mjs|beyondTheRim\.mjs/);
  assert.match(bundle,new RegExp(`final:'${version.replaceAll('.', '\\.')}\\-r54\\-p(?:[8-9][0-9]|[1-9][0-9]{2,})'`));
  assert.match(bundle, /eoteCharacterCreation_mjs/);
  assert.doesNotMatch(bundle, /^import\s/m);
  assert.match(contract, /scenePlayerView\?: \(\) => BridgeSubmitResult/);
  assert.match(contract, /resolveSuggestedMasterCheck\?: \(\) => BridgeSubmitResult/);
});

test('FINAL: approved CENA shell is bound to real player-visible engine data and suggested checks', () => {
  const runtime = read('app/src/main/assets/vtt/r46/final-runtime.js');
  assert.match(runtime, /bridgeResult\('scenePlayerView'\)/);
  assert.match(runtime, /scene-situation-paper/);
  assert.match(runtime, /scene-participant-slot/);
  assert.match(runtime, /scene-action-slot/);
  assert.match(runtime, /scene-status-cell/);
  assert.match(runtime, /view\.suggestedActions\.slice\(0,6\)/);
  assert.match(runtime, /bridgeResult\('resolveSuggestedMasterCheck'\)/);
  assert.match(runtime, />ROLAR<\/button>/);
});

test('FINAL: Gemini master uses current models, Interactions structured output and no deprecated sampling knobs', () => {
  const gemini = read('app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java');
  assert.match(gemini, /MASTER_MODEL = "gemini-3\.8-flash"/);
  assert.match(gemini, /IMAGE_MODEL = "gemini-3\.1-flash-image"/);
  assert.match(gemini, /generativelanguage\.googleapis\.com\/v1beta\/interactions/);
  assert.match(gemini, /response_format/);
  assert.match(gemini, /thinking_level", "medium"/);
  assert.doesNotMatch(gemini, /"temperature"/);
  assert.match(gemini, /requestMasterProposal/);
});

test('FINAL: Android host disables WebView debugging and supports system back navigation', () => {
  const main = read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
  assert.match(main, /WebView\.setWebContentsDebuggingEnabled\(false\)/);
  assert.match(main, /public void onBackPressed\(\)/);
  assert.equal((main.match(/public void onBackPressed\(\)/g) ?? []).length, 1, 'onBackPressed deve ter uma única implementação');
  assert.match(main, /webView\.canGoBack\(\)/);
  assert.match(main, /webView\.goBack\(\)/);
});

test('FINAL: shell script order installs engine before React shell and final runtime', () => {
  const html = read('app/src/main/assets/vtt/r46-shell.html');
  const engine = html.indexOf('js/core/uiBridge.bundle.js');
  const shell = html.indexOf('r46/r46-shell.bundle.js');
  const runtime = html.indexOf('r46/final-runtime.js');
  assert.ok(engine >= 0 && shell > engine && runtime > shell);
});

test('FINAL: first boot explicitly initializes the canonical campaign before the React shell mounts', () => {
  const html = read('app/src/main/assets/vtt/r46-shell.html');
  const bootstrap = read('app/src/main/assets/vtt/r46/final-bootstrap.js');
  const source = read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const engine = html.indexOf('js/core/uiBridge.bundle.js');
  const boot = html.indexOf('r46/final-bootstrap.js');
  const shell = html.indexOf('r46/r46-shell.bundle.js');
  assert.ok(engine >= 0 && boot > engine && shell > boot);
  assert.match(bootstrap, /bridge\.readState\(\)/);
  assert.match(bootstrap, /bridge\.initializeCampaign\(\)/);
  assert.match(source, /initializeCampaign\(\)/);
  assert.match(source, /created:\s*true/);
});
