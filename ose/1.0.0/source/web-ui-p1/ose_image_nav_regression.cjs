'use strict';
/* Focused regression gate for real Android/WebView image and layout fixes. */
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const web = __dirname;
const root = path.resolve(web, '..');
const css = fs.readFileSync(path.join(web, 'p1.css'), 'utf8');
const html = fs.readFileSync(path.join(web, 'index.html'), 'utf8');
const js = fs.readFileSync(path.join(web, 'p1.js'), 'utf8');
const cleaner = fs.readFileSync(path.join(web, 'equipment-icon-cleaner.js'), 'utf8');
const activity = fs.readFileSync(path.join(root, 'app-android/src/main/kotlin/braseiro/ose/app/MainActivity.kt'), 'utf8');
const store = fs.readFileSync(path.join(root, 'app-android/src/main/kotlin/braseiro/ose/app/PresentationMediaStore.kt'), 'utf8');
const dice = fs.readFileSync(path.join(web, 'dice3d_app.js'), 'utf8');
function verify(name, ok) {
  assert.ok(ok, name);
  console.log('PASS '+name);
}
// Keep all older/newer assertions in the same strict failure path.
const check = verify;
verify('dock final 56px in mobile WebView', css.includes('.bottom{bottom:0;height:56px;max-height:56px;padding-bottom:0}'));
verify('native bottom inset separated from CSS dock', activity.includes('lp.bottomMargin = insets.systemWindowInsetBottom') && css.includes('#phone{padding-bottom:56px}'));
verify('no asset frame CSS cropping', !css.includes('clip-path:inset(12%') && css.includes('clip-path:none!important'));
verify('black pixel alpha conversion exists', cleaner.includes('putImageData') && cleaner.includes("toDataURL('image/png')"));
verify('alpha cleaner integrated in HTML', html.includes('equipment-icon-cleaner.js') && js.includes('OSEEquipmentArt?.refresh(main)'));
verify('Android chooser receives native file selection', activity.includes('onShowFileChooser') && activity.includes('REQUEST_PICK_IMAGE ->'));
verify('manual PNG/WebP imports preserve alpha', js.includes("file.type==='image/png'||file.type==='image/webp'"));
verify('large media preserved as ARGB + correct format', store.includes('Bitmap.Config.ARGB_8888') && store.includes('outputMime'));
verify('Gemini API button does not silently open external app', js.includes('A geração integrada requer uma chave Gemini') && js.includes("postNative('PortraitImageGenerate'"));
verify('native bridge reports action operation', activity.includes('.put("operation", operation)') && activity.includes('Bridge command failed: $operation'));
verify('dice source is official FL visual adaptation', dice.includes('Forbidden Lands Dice3DStage'));
verify('dice UI cannot change referee RNG', !dice.includes('Math.random') && dice.includes('canonicalTotal'));
require('./dice3d_physics.test.cjs');
console.log('Braseiro OSE image/nav/physics regression: PASS (device QA remains separate)');

// Persistence projection must use OSE canonical item names even after equipment selection.
const nativeMain=fs.readFileSync(path.join(root,'app-android/src/main/kotlin/braseiro/ose/app/MainActivity.kt'),'utf8');
check('OSE item labels projected without active loadout',nativeMain.includes('out.optJSONArray("inventory")')&&nativeMain.includes('item.put("displayName", canonicalName)'));
check('inventory renderer reads per-item name',fs.readFileSync(path.join(__dirname,'p1.js'),'utf8').includes('catalogLabel(it.catalogId,it)'));

check('OSE dice UI never displays another game title',!fs.readFileSync(path.join(__dirname,'dice3d_app.js'),'utf8').includes("title.textContent='MESA 3D · FORBIDDEN LANDS / OSE'")&&html.includes('aria-label="Mesa de dados físicos 3D do Braseiro OSE"'));

check('Gemini image generation uses isolated worker, safe bounded downloads and resets busy', nativeMain.includes('private val imageIo = Executors.newSingleThreadExecutor()') && nativeMain.includes('imageRequestBusy.set(false)') && nativeMain.includes('startImageGeneration(scope, stableId'));
check('image reuse is cache-busted and absent files are discarded',fs.readFileSync(path.join(root,'app-android/src/main/kotlin/braseiro/ose/app/PresentationMediaStore.kt'),'utf8').includes('mediaUrl(file, entry.optString("rev"))'));

check('new sandbox worlds select varied safe land hexes',nativeMain.includes('kotlin.random.Random(worldSeed.toLong()).nextInt(candidates.size)')&&nativeMain.includes('val safeLand = setOf('));
check('portrait uses head-aware top focus rather than centered crop',css.includes('.text-first-hero .portrait img{object-fit:cover;object-position:50% 12%}'));

// Static startup gates: the Android release must not die on a bad save or
// WebView renderer failure. Saved files must remain intact during recovery.
check('native startup catches database recovery exceptions',nativeMain.includes('Campaign bootstrap failed; keeping original save intact')&&nativeMain.includes('showStartupRecovery(failure)'));
check('WebView renderer recovers without clearing player data',nativeMain.includes('override fun onRenderProcessGone(')&&nativeMain.includes('putBoolean("skip_media", true)'));
check('UI media projection supports safe startup',nativeMain.includes('private fun safeMediaUrl(')&&nativeMain.includes('if (recoveryPrefs.getBoolean("skip_media", false)) null'));

verify('repeated startup repair uses unique transition id instead of legacy fixed id',
  activity.includes('runtime-repair-v3-') &&
  activity.includes('java.util.UUID.randomUUID()') &&
  !activity.includes('StateTransition("runtime-repair-v2", repaired.campaignState)'));
verify('recovery screen renders line breaks instead of literal slash-n',
  activity.includes('recuperação de inicialização\\\\n\\\\n') === false);

check('Gemini 3.8 TTS defaults and free-tier eligible model',fs.readFileSync(path.join(root,'app-android/src/main/kotlin/braseiro/ose/app/GeminiInteractionsClient.kt'),'utf8').includes('gemini-3.8-flash-lite-tts'));
check('Kokoro long single-utterance chunks reduce repeated synthesis startup',fs.readFileSync(path.join(root,'tts-android/src/main/kotlin/braseiro/ose/tts/android/AndroidTtsController.kt'),'utf8').includes('targetChars = 3000, maxChars = 3500'));
check('OSE physical dice get immersive full-screen theater',css.includes('#dice3dOverlay .ose-dice-dialog{')&&css.includes('max-width:none;max-height:100%;height:100%'));

check('all dice selection glyphs are model-backed SVG', js.includes('function oseDieMiniature(sides)') && js.includes('${oseDieMiniature(die.s)}') && css.includes('.die-model-svg{'));
