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
