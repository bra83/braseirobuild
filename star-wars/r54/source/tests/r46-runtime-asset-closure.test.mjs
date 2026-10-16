import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve('app/src/main/assets/vtt');
const htmlPath = path.join(root, 'r46-shell.html');
const html = fs.readFileSync(htmlPath, 'utf8');

function isLocal(ref) {
  return ref && !/^(?:https?:|data:|blob:|#|javascript:)/i.test(ref);
}
function resolveFrom(file, ref) {
  const clean = ref.split(/[?#]/,1)[0];
  return path.resolve(path.dirname(file), clean);
}

test('r46-shell.html has no missing local script or stylesheet assets', () => {
  const refs = [...html.matchAll(/(?:src|href)=["']([^"']+)["']/gi)].map(m => m[1]).filter(isLocal);
  assert.ok(refs.length >= 6, `unexpectedly small local asset set: ${refs.length}`);
  const missing = refs.filter(ref => !fs.existsSync(resolveFrom(htmlPath, ref)));
  assert.deepEqual(missing, []);
});

test('R46 boot script order keeps engine before bootstrap, shell and final runtime', () => {
  const scripts = [...html.matchAll(/<script[^>]+src=["']([^"']+)["'][^>]*>/gi)].map(m => m[1]);
  const idx = name => scripts.indexOf(name);
  for (const name of ['r46/react18-vendor.js','js/core/uiBridge.bundle.js','r46/final-bootstrap.js','r46/r46-shell.bundle.js','r46/final-runtime.js']) {
    assert.notEqual(idx(name), -1, `missing script ${name}`);
  }
  assert.ok(idx('r46/react18-vendor.js') < idx('js/core/uiBridge.bundle.js'));
  assert.ok(idx('js/core/uiBridge.bundle.js') < idx('r46/final-bootstrap.js'));
  assert.ok(idx('r46/final-bootstrap.js') < idx('r46/r46-shell.bundle.js'));
  assert.ok(idx('r46/r46-shell.bundle.js') < idx('r46/final-runtime.js'));
});

test('R46 stylesheets have no missing local url(...) dependencies', () => {
  const cssRefs = [...html.matchAll(/<link[^>]+href=["']([^"']+\.css(?:[?#][^"']*)?)["'][^>]*>/gi)].map(m => m[1]).filter(isLocal);
  const missing = [];
  let checked = 0;
  for (const cssRef of cssRefs) {
    const cssPath = resolveFrom(htmlPath, cssRef);
    assert.ok(fs.existsSync(cssPath), `missing css ${cssRef}`);
    const css = fs.readFileSync(cssPath, 'utf8');
    for (const m of css.matchAll(/url\(\s*(['"]?)([^)'"\s]+)\1\s*\)/gi)) {
      const ref = m[2];
      if (!isLocal(ref)) continue;
      checked++;
      if (!fs.existsSync(resolveFrom(cssPath, ref))) missing.push(`${cssRef} -> ${ref}`);
    }
  }
  assert.ok(cssRefs.length >= 3);
  assert.deepEqual(missing, []);
  assert.ok(checked >= 0);
});
