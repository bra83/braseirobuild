import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const root = process.cwd();
const read = (path) => readFileSync(join(root, path), 'utf8');

function balancedBlock(source, startPattern) {
  const match = startPattern.exec(source);
  assert.ok(match, `bloco não encontrado: ${startPattern}`);
  const open = source.indexOf('{', match.index);
  assert.ok(open >= 0, 'abertura de bloco não encontrada');
  let depth = 0;
  for (let i = open; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1;
    else if (source[i] === '}') {
      depth -= 1;
      if (depth === 0) return source.slice(open + 1, i);
    }
  }
  assert.fail('bloco sem fechamento');
}

function interfaceMethods(source, name) {
  const body = balancedBlock(source, new RegExp(`export\\s+interface\\s+${name}\\s*`));
  return [...body.matchAll(/^\s*([A-Za-z_$][\w$]*)\??\s*:/gm)]
    .map((m) => m[1])
    .sort();
}

function classMethods(source, name) {
  const body = balancedBlock(source, new RegExp(`export\\s+class\\s+${name}\\s*`));
  return [...body.matchAll(/^\s{4}(?:async\s+)?(?!constructor\b)([A-Za-z_$][\w$]*)\s*\([^\n]*\)\s*\{/gm)]
    .map((m) => m[1])
    .filter((name) => !name.startsWith('_'))
    .sort();
}

function exportedSymbols(source) {
  const out = new Set();
  for (const m of source.matchAll(/^export\s+(?:declare\s+)?(?:async\s+)?(?:type|interface|class|function|const|let|var)\s+([A-Za-z_$][\w$]*)/gm)) out.add(m[1]);
  for (const m of source.matchAll(/^export\s*\{([^}]+)\}/gm)) {
    for (const item of m[1].split(',')) {
      const clean = item.trim().replace(/^type\s+/, '');
      const alias = /\bas\s+([A-Za-z_$][\w$]*)$/.exec(clean);
      const direct = /^([A-Za-z_$][\w$]*)/.exec(clean);
      if (alias) out.add(alias[1]); else if (direct) out.add(direct[1]);
    }
  }
  return out;
}

function importedSymbolsFromGameBridge(source) {
  const out = [];
  const re = /import\s+(?:type\s+)?\{([^}]*)\}\s+from\s+['"](?:@\/src\/state\/gameBridgeClient|\.\/gameBridgeClient)['"]\s*;/g;
  for (const m of source.matchAll(re)) {
    for (const raw of m[1].split(',')) {
      const clean = raw.trim().replace(/^type\s+/, '');
      if (!clean) continue;
      const imported = /^([A-Za-z_$][\w$]*)/.exec(clean)?.[1];
      if (imported) out.push(imported);
    }
  }
  return out;
}

test('R54 bridge contract: shell interface and BraseiroUiBridge implementation are exact peers', () => {
  const client = read('shell-r46-run87/gameBridgeClient.ts');
  const bridge = read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
  const declared = interfaceMethods(client, 'BraseiroGameBridgeContract');
  const implemented = classMethods(bridge, 'BraseiroUiBridge');
  assert.ok(declared.length >= 53, `contrato encolheu inesperadamente: ${declared.length} métodos`);
  const missing = declared.filter((name) => !implemented.includes(name));
  assert.deepEqual(missing, [], `métodos declarados ausentes na implementação: ${missing.join(', ')}`);
});

test('R46 bridge contract: every gameBridgeClient import used by shell files is actually exported', () => {
  const client = read('shell-r46-run87/gameBridgeClient.ts');
  const exports = exportedSymbols(client);
  const consumers = [
    'shell-r46-run87/VTTApp.tsx',
    'shell-r46-run87/Templates.tsx',
    'shell-r46-run87/imageLibraryBridgeClient.ts',
  ];
  const missing = [];
  let importCount = 0;
  for (const path of consumers) {
    const names = importedSymbolsFromGameBridge(read(path));
    importCount += names.length;
    for (const name of names) if (!exports.has(name)) missing.push(`${path}:${name}`);
  }
  assert.ok(importCount > 0, 'nenhum import do gameBridgeClient foi detectado');
  assert.deepEqual(missing, []);
});

test('R46 bridge contract: public bridge expõe perfil compartilhado sem comandos Duty/AoR crus na shell', () => {
  const client = read('shell-r46-run87/gameBridgeClient.ts');
  const declared = interfaceMethods(client, 'BraseiroGameBridgeContract');
  const forbidden = declared.filter((name) => /duty|contribution|aor|rebel/i.test(name));
  assert.deepEqual(forbidden, []);
  assert.ok(declared.includes('creationCatalog'));
  assert.ok(declared.includes('creationPreview'));
  assert.ok(declared.includes('createCustomCharacter'));
  for (const method of ['campaignModeSummary','setCampaignMode','initializeLivingSandbox','livingSandboxStatus','sandboxOpening','startSandboxSession','restartSandboxPreservingParty','travelToLocation']) assert.ok(declared.includes(method), `sandbox/mode não exposto pela bridge: ${method}`);
  for (const method of ['ownedSpecializations','specializationPurchaseCost','buySpecialization','talentTree','eligibleTalents','buyTalent','forceStatus','forceCatalog','buyForcePower','buyForceUpgrade','rollForcePowerCheck','commitForce','releaseForce','startSession','closeSession','encumbranceStatus','liftingDifficulty','statusSummary','applyStatus','spendDestiny','resolveFearCheck']) assert.ok(declared.includes(method), `mecânica EotE não exposta pela bridge: ${method}`);
});
