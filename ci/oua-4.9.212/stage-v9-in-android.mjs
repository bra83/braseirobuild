// Copies patched V9 modules into an isolated staging namespace of Android Studio source.
// Usage: node ci/oua-4.9.212/stage-v9-in-android.mjs V9_EXTRACTED ANDROID_49211_EXTRACTED
// Never overwrites existing UI/TTS/saves and never bundles host-authority or private GM keys.
import fs from 'node:fs';
import path from 'node:path';

if (process.argv.length < 4) throw new Error('Usage: node stage-v9-in-android.mjs V9_DIR ANDROID_49211_DIR');
const from = path.resolve(process.argv[2]);
const app = path.resolve(process.argv[3]);
const src = path.join(from,'src');
const destination = path.join(app,'src','tor2e-v49212');
if (!fs.existsSync(path.join(from,'package.json')) ||
    !fs.existsSync(path.join(from,'src','ui','bridges','Tor2eAdventureController.mjs'))) {
  throw new Error('V9 source root invalid');
}
if (!fs.existsSync(path.join(app,'package.json')) || !fs.existsSync(path.join(app,'android'))) {
  throw new Error('Android Studio 4.9.211 project root invalid (requires package.json + android)');
}
if (fs.existsSync(destination)) throw new Error('Destination already exists: no overwrite/reimport allowed');
if (!fs.readFileSync(path.join(src,'ui','bridges','Tor2eAdventureController.mjs'),'utf8')
    .includes('HARDENED_V9_5_GATES')) {
  throw new Error('V9 hardening is not applied; staging prohibited');
}
if (!fs.readFileSync(path.join(src,'application','Tor2eRulesGuard.mjs'),'utf8')
    .includes('HARDENED_RULES_GUARD_V9')) {
  throw new Error('Production command bus hardening missing');
}
// Trusted-host security and secret-vault modules must never be staged under the WebView src tree.
const allowed = ['application','domain','library','ui'];
const copied = [];
for (const dir of allowed) {
  const sourceDir=path.join(src,dir);
  if (!fs.existsSync(sourceDir)) throw new Error('Source module missing: ' + dir);
}
try {
  fs.mkdirSync(destination,{recursive:true});
  for (const dir of allowed) {
    fs.cpSync(path.join(src,dir),path.join(destination,dir),{recursive:true});
    copied.push(dir);
  }
  fs.mkdirSync(path.join(destination,'barbara'),{recursive:true});
  fs.copyFileSync(path.join(src,'barbara','Tor2eBarbaraSupervisor.mjs'),
    path.join(destination,'barbara','Tor2eBarbaraSupervisor.mjs'));
  copied.push('barbara-supervisor');
  const hostBridgePath = path.join(destination,'ui','bridges','Tor2eHostGmBridge.mjs');
  fs.mkdirSync(path.dirname(hostBridgePath),{recursive:true});
  fs.writeFileSync(hostBridgePath, [
    '// WebView-safe Host Authority Bridge (no secrets, hashes, or crypto in client bundle).',
    'export class Tor2eGmSecretsVault {',
    '  static getActiveAuthorityBridge() {',
    '    return globalThis.__TOR2E_HOST_AUTHORITY_BRIDGE__ || globalThis.window?.BraseiroAndroid?.tor2eHostAuthority || null;',
    '  }',
    '  static verifyHostGmKey(gmSecretKey, hostGmSecretHash) {',
    '    const bridge = this.getActiveAuthorityBridge();',
    '    if (!bridge || typeof bridge.verifyHostKey !== "function") return false;',
    '    return Boolean(bridge.verifyHostKey(gmSecretKey, hostGmSecretHash));',
    '  }',
    '  static unlockSceneSecrets(sceneId, gmSecretKey, hostGmSecretHash, customProvider = null) {',
    '    if (!this.verifyHostGmKey(gmSecretKey, hostGmSecretHash)) return null;',
    '    if (typeof customProvider === "function") return customProvider(sceneId, gmSecretKey);',
    '    const bridge = this.getActiveAuthorityBridge();',
    '    if (!bridge || typeof bridge.requestSceneSecrets !== "function") return null;',
    '    return bridge.requestSceneSecrets(sceneId, gmSecretKey, hostGmSecretHash);',
    '  }',
    '  static unlockRagSecrets(gmSecretKey, hostGmSecretHash, customProvider = null) {',
    '    if (!this.verifyHostGmKey(gmSecretKey, hostGmSecretHash)) return [];',
    '    if (typeof customProvider === "function") return customProvider(gmSecretKey);',
    '    const bridge = this.getActiveAuthorityBridge();',
    '    if (!bridge || typeof bridge.requestRagSecrets !== "function") return [];',
    '    return bridge.requestRagSecrets(gmSecretKey, hostGmSecretHash);',
    '  }',
    '}',
    ''
  ].join('\n'), 'utf8');
  const stagedCtrlPath = path.join(destination,'ui','bridges','Tor2eAdventureController.mjs');
  if (fs.existsSync(stagedCtrlPath)) {
    const ctrlSrc = fs.readFileSync(stagedCtrlPath,'utf8')
      .replace('../../security/Tor2eGmSecretsVault.mjs','./Tor2eHostGmBridge.mjs');
    fs.writeFileSync(stagedCtrlPath, ctrlSrc, 'utf8');
  }
  const stagedRagPath = path.join(destination,'library','Tor2eRAGService.mjs');
  if (fs.existsSync(stagedRagPath)) {
    const ragSrc = fs.readFileSync(stagedRagPath,'utf8')
      .replace('../security/Tor2eGmSecretsVault.mjs','../ui/bridges/Tor2eHostGmBridge.mjs');
    fs.writeFileSync(stagedRagPath, ragSrc, 'utf8');
  }
  const marker = {
    base:'OUA-v4.9.211', overlay:'4.9.212-V9-hardening',
    status:'STAGED_ONLY_NOT_WIRED_TO_UI', nativeBarbara:'PENDING',
    hostAuthority:'EXCLUDED_FROM_WEBVIEW', security:'EXCLUDED_REQUIRES_NATIVE_HOST_BRIDGE',
    androidBuild:'PENDING',
    included:copied
  };
  fs.writeFileSync(path.join(destination,'STAGING_STATUS.json'),JSON.stringify(marker,null,2)+'\n');
  console.log('OUA_STAGE_SAFE_PASS '+JSON.stringify(marker));
} catch(err) {
  fs.rmSync(destination,{recursive:true,force:true});
  throw err;
}
