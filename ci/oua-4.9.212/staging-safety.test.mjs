// Standalone fixture: assures V9 staging cannot copy GM secrets into Android/WebView src.
import test from 'node:test';
import assert from 'node:assert/strict';
import os from 'node:os';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const script = fileURLToPath(new URL('./stage-v9-in-android.mjs', import.meta.url));
function put(file, content = '// fixture\n') {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  fs.writeFileSync(file, content, 'utf8');
}

test('OUA-STAGE-01: no secret or native-host source lands in client staging', (t) => {
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'oua-stage-safety-'));
  t.after(() => fs.rmSync(tmp, { recursive: true, force: true }));
  const v9 = path.join(tmp, 'v9');
  const app = path.join(tmp, 'android-app');
  put(path.join(v9, 'package.json'), '{}');
  put(path.join(v9, 'src/ui/bridges/Tor2eAdventureController.mjs'), '// HARDENED_V9_5_GATES\n');
  put(path.join(v9, 'src/application/Tor2eRulesGuard.mjs'), '// HARDENED_RULES_GUARD_V9\n');
  put(path.join(v9, 'src/domain/rules.mjs'));
  put(path.join(v9, 'src/library/catalog.json'), '{}');
  put(path.join(v9, 'src/security/Tor2eGmSecretsVault.mjs'), 'DO_NOT_EXPORT_GM_VAULT');
  put(path.join(v9, 'src/host-authority/Tor2eHostSecretAuthority.mjs'), 'DO_NOT_EXPORT_HOST_KEYS');
  put(path.join(v9, 'src/barbara/Tor2eBarbaraSupervisor.mjs'));
  put(path.join(app, 'package.json'), '{}');
  put(path.join(app, 'android/.keep'), 'ANDROID_PRESERVED');
  put(path.join(app, 'src/main.ts'), 'UI_PRESERVED');

  execFileSync(process.execPath, [script, v9, app], { stdio: 'pipe' });
  const dst = path.join(app, 'src/tor2e-v49212');
  assert.ok(fs.existsSync(path.join(dst, 'ui/bridges/Tor2eAdventureController.mjs')));
  assert.ok(fs.existsSync(path.join(dst, 'application/Tor2eRulesGuard.mjs')));
  assert.ok(fs.existsSync(path.join(dst, 'barbara/Tor2eBarbaraSupervisor.mjs')));
  assert.equal(fs.existsSync(path.join(dst, 'security')), false);
  assert.equal(fs.existsSync(path.join(dst, 'host-authority')), false);
  assert.equal(fs.readFileSync(path.join(app, 'src/main.ts'), 'utf8'), 'UI_PRESERVED');
  assert.equal(fs.readFileSync(path.join(app, 'android/.keep'), 'utf8'), 'ANDROID_PRESERVED');
  const marker = JSON.parse(fs.readFileSync(path.join(dst, 'STAGING_STATUS.json'), 'utf8'));
  assert.equal(marker.status, 'STAGED_ONLY_NOT_WIRED_TO_UI');
  assert.match(marker.security, /^EXCLUDED/);
  assert.throws(() => execFileSync(process.execPath, [script, v9, app], { stdio: 'pipe' }),
    /Command failed/);
});
