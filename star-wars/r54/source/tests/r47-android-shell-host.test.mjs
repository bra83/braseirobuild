import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const ROOT = path.resolve(import.meta.dirname, '..');
const mainActivity = path.join(ROOT, 'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const vtt = path.join(ROOT, 'app/src/main/assets/vtt');
const host = path.join(vtt, 'r46-shell.html');
const shellDir = path.join(vtt, 'r46');
const iconDir = path.join(vtt, 'assets/ui/icons-v2');

const read = (file) => fs.readFileSync(file, 'utf8');

test('R47 Android inicia pela shell visual R46 sem remover as pontes nativas', () => {
  const java = read(mainActivity);
  assert.match(java, /addJavascriptInterface\(new LibraryBridge\(\), "BraseiroLibrary"\)/);
  assert.match(java, /addJavascriptInterface\(geminiBridge\.javascriptInterface\(\), "BraseiroGemini"\)/);
  assert.match(java, /addJavascriptInterface\(imageLibraryBridge, "BraseiroImages"\)/);
  assert.match(java, /loadUrl\("file:\/\/\/android_asset\/vtt\/r46-shell\.html"\)/);
  assert.doesNotMatch(java, /loadUrl\("file:\/\/\/android_asset\/vtt\/index\.html"\)/);
});

test('host offline instala BraseiroGameBridge antes de montar o bundle React', () => {
  const html = read(host);
  assert.match(html, /react18-vendor\.js/);
  assert.match(html, /js\/core\/uiBridge\.bundle\.js/);
  assert.match(html, /r46\/r46-shell\.bundle\.js/);
  assert.ok(html.indexOf('uiBridge.bundle.js') < html.indexOf('r46-shell.bundle.js'));
  assert.doesNotMatch(html, /type="module"/);
  assert.match(html, /data-r47-host="android-offline"/);
});

test('bundle offline da shell e CSS estão presentes e sintaticamente fechados', () => {
  for (const name of ['react18-vendor.js', 'r46-shell.bundle.js', 'globals.css', 'r46.css']) {
    const file = path.join(shellDir, name);
    assert.ok(fs.statSync(file).size > 1000, `${name} deve estar empacotado`);
  }
  const bundle = read(path.join(shellDir, 'r46-shell.bundle.js'));
  assert.match(bundle, /R47 Android host bundle/);
  assert.match(bundle, /data.*r46Shell|r46Shell/);
});

test('rotas e domínio principal continuam na arquitetura R46', () => {
  const bundle = read(path.join(shellDir, 'r46-shell.bundle.js'));
  assert.match(bundle, /id:\s*`R46_\$\{String\(n \+ 1\)\.padStart\(2, '0'\)\}`/);
  const registryBlock = bundle.match(/exports\.screenRegistry = \[(.*?)\n\];/s)?.[1] ?? '';
  const routes = [...registryBlock.matchAll(/\bscreen\((\d+),/g)].map((match) => Number(match[1]));
  assert.deepEqual(routes, Array.from({ length: 31 }, (_, i) => i));
  for (const label of ['CENA', 'PERSONAGEM', 'ITENS', 'NAVE', 'REGISTROS']) assert.match(bundle, new RegExp(`label:\\s*['\"]${label}['\"]`));
});

test('CSS Android mantém viewport 415×915 e não referencia assets inexistentes', () => {
  const css = read(path.join(shellDir, 'r46.css'));
  assert.match(css, /max-width:415px/);
  assert.doesNotMatch(css, /url\(['"]\/assets\//);
  assert.doesNotMatch(css, /scene-demo-vehicle-combat\.png/);
  assert.doesNotMatch(css, /rebel-watermark\.png/);
  assert.match(css, /icons-v2\/inventory\.png/);
  assert.match(css, /icons-v2\/journal\.png/);
});

test('apenas ícones canônicos materializados são usados no host', () => {
  const expected = ['scene','map','character','inventory','ship','journal','settings','party','combat','mission','tts','dice','target','location','medical','credits'];
  for (const name of expected) {
    const file = path.join(iconDir, `${name}.png`);
    assert.ok(fs.existsSync(file), `${name}.png ausente`);
    assert.ok(fs.statSync(file).size > 1000, `${name}.png vazio ou inválido`);
  }
  assert.equal(fs.existsSync(path.join(iconDir, 'items.png')), false);
  assert.equal(fs.existsSync(path.join(iconDir, 'records.png')), false);
});

test('mapeamento de ícones não pede items.png ou records.png ausentes', () => {
  const bundle = read(path.join(shellDir, 'r46-shell.bundle.js'));
  assert.doesNotMatch(bundle, /items:\s*['"]items['"]/);
  assert.doesNotMatch(bundle, /records:\s*['"]records['"]/);
  assert.match(bundle, /items:\s*['"]inventory['"]/);
  assert.match(bundle, /records:\s*['"]journal['"]/);
});

test('entrada legada permanece no pacote apenas como fallback não carregado', () => {
  assert.ok(fs.existsSync(path.join(vtt, 'index.html')));
  assert.ok(fs.existsSync(path.join(vtt, 'js/core/uiBridge.mjs')));
  assert.ok(fs.existsSync(path.join(vtt, 'js/core/controller.mjs')));
});
