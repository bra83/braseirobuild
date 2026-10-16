#!/usr/bin/env node
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

const root = process.cwd();
const javaRoot = path.join(root, 'app/src/main/java');
const files = [];
function walk(dir) {
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name);
    const st = fs.statSync(p);
    if (st.isDirectory()) walk(p);
    else if (p.endsWith('.java')) files.push(p);
  }
}
walk(javaRoot);
files.sort();
if (!files.length) {
  console.error('JAVA_SYNTAX_FAIL no Java sources found');
  process.exit(1);
}

const outDir = fs.mkdtempSync(path.join(os.tmpdir(), 'eotg-javac-'));
try {
  let javacBin = 'javac';
  if (process.platform === 'win32') {
    const candidates = [
      'C:\\Program Files\\Android\\Android Studio\\jbr\\bin\\javac.exe',
      'C:\\Program Files\\Android\\Android Studio\\jre\\bin\\javac.exe'
    ];
    for (const c of candidates) {
      if (fs.existsSync(c)) { javacBin = c; break; }
    }
  }
  const run = spawnSync(javacBin, ['-proc:none', '-XDrawDiagnostics', '-Xmaxerrs', '10000', '-d', outDir, ...files], {
    cwd: root,
    encoding: 'utf8'
  });
  if (run.error) {
    console.error(`JAVA_SYNTAX_BLOCKED ${run.error.message}`);
    process.exit(2);
  }
  const diagnostics = `${run.stdout ?? ''}\n${run.stderr ?? ''}`;
  const codes = [...diagnostics.matchAll(/compiler\.err\.[a-z0-9_.]+/gi)].map(m => m[0]);
  const allowed = new Set([
    'compiler.err.doesnt.exist',
    'compiler.err.cant.resolve',
    'compiler.err.cant.resolve.location',
    'compiler.err.cant.resolve.location.args',
    'compiler.err.method.does.not.override.superclass'
  ]);
  const blocking = [...new Set(codes.filter(code => !allowed.has(code)))];
  if (blocking.length) {
    console.error(`JAVA_SYNTAX_FAIL blocking=${blocking.join(',')}`);
    for (const line of diagnostics.split(/\r?\n/)) {
      if (blocking.some(code => line.includes(code))) console.error(line);
    }
    process.exit(1);
  }
  const unresolved = codes.length;
  console.log(`JAVA_SYNTAX_PASS files=${files.length} unresolved_android_symbols=${unresolved}`);
} finally {
  fs.rmSync(outDir, { recursive: true, force: true });
}
