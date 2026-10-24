'use strict';
// Static Android/WebView responsive contract. Not a physical Galaxy A57 test.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '..');
const css = fs.readFileSync(path.join(__dirname, 'p1.css'), 'utf8');
const html = fs.readFileSync(path.join(__dirname, 'index.html'), 'utf8');
const manifest = fs.readFileSync(path.join(root, 'app-android/src/main/AndroidManifest.xml'), 'utf8');
const gradle = fs.readFileSync(path.join(root, 'app-android/build.gradle.kts'), 'utf8');
const activity = fs.readFileSync(path.join(root, 'app-android/src/main/kotlin/braseiro/ose/app/MainActivity.kt'), 'utf8');
function check(label, condition) { assert.ok(condition, label); process.stdout.write('PASS ' + label + '\n'); }
check('responsive <=480 dp breakpoint', css.includes('@media (max-width: 480px)'));
check('compact <=370 dp breakpoint', css.includes('@media (max-width: 370px)'));
check('dynamic height', css.includes('min-height: 100dvh'));
check('bottom-nav safe inset', css.includes('height: calc(80px + env(safe-area-inset-bottom, 0px))'));
check('touch-size controls', css.includes('min-height: 44px'));
check('touch-size zoom controls', css.includes('min-width: 44px'));
check('keyboard-safe overlays', css.includes('max-height: 100dvh') && css.includes('.settings { max-height: 100%; overflow-y: auto;'));
check('small landscape layout', css.includes('@media (max-height: 600px) and (orientation: landscape)'));
check('native status and navigation insets', activity.includes('root.setOnApplyWindowInsetsListener') && activity.includes('lp.topMargin = insets.systemWindowInsetTop') && activity.includes('lp.bottomMargin = insets.systemWindowInsetBottom'));
check('keyboard resize mode', manifest.includes('android:windowSoftInputMode="adjustResize"'));
check('adaptive viewport and user zoom', html.includes('width=device-width,initial-scale=1,viewport-fit=cover') && !html.includes('user-scalable=no'));
check('preserved package and update capability', gradle.includes('applicationId = "braseiro.ose"') && gradle.includes('versionCode = 10'));
console.log('Galaxy A57 static WebView contract: PASS (device testing still required)');
