#!/usr/bin/env node
import { readdirSync, readFileSync, statSync, existsSync, writeFileSync, openSync, closeSync, unlinkSync, mkdirSync } from 'node:fs';
import { join, relative, extname } from 'node:path';
import { spawnSync, spawn } from 'node:child_process';
import crypto from 'node:crypto';
import { tmpdir } from 'node:os';

const root = process.cwd();
const validationDir = join(root, 'validation');
mkdirSync(validationDir, { recursive: true });
const now = new Date().toISOString();
const lines = [];
const log = (s) => { lines.push(s); console.log(s); };
const fail = (s) => { lines.push(`FAIL: ${s}`); console.error(`FAIL: ${s}`); process.exitCode = 1; };

function walk(dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const p = join(dir, name);
    const st = statSync(p);
    if (st.isDirectory()) out.push(...walk(p)); else out.push(p);
  }
  return out;
}
function run(cmd, args, opts = {}) {
  const r = spawnSync(cmd, args, { cwd: root, encoding: 'utf8', ...opts });
  return { code: r.status ?? 1, stdout: r.stdout ?? '', stderr: r.stderr ?? '' };
}
const pythonCommand = process.platform === 'win32' ? 'python' : 'python3';
function executableOnPath(name) {
  const probe = process.platform === 'win32' ? run('where.exe', [name]) : run('which', [name]);
  return probe.code === 0 ? probe.stdout.split(/\r?\n/).find(Boolean)?.trim() ?? '' : '';
}
function runAsync(cmd, args, opts = {}) {
  const { timeoutMs = 120000, ...spawnOpts } = opts;
  return new Promise((resolve) => {
    const child = spawn(cmd, args, { cwd: root, stdio: ['ignore', 'pipe', 'pipe'], ...spawnOpts });
    let stdout = ''; let stderr = ''; let settled = false;
    const finish = (result) => { if (settled) return; settled = true; clearTimeout(timer); resolve(result); };
    const timer = setTimeout(() => {
      child.kill('SIGKILL');
      finish({ code: 124, stdout, stderr: `${stderr}process timeout after ${timeoutMs}ms` });
    }, timeoutMs);
    child.stdout?.on('data', chunk => { stdout += chunk.toString(); });
    child.stderr?.on('data', chunk => { stderr += chunk.toString(); });
    child.on('error', error => finish({ code: 1, stdout, stderr: `${stderr}${error.message}` }));
    child.on('close', code => finish({ code: code ?? 1, stdout, stderr }));
  });
}

function runAsyncToFile(cmd, args, opts = {}) {
  const { timeoutMs = 120000, outputFile = join(validationDir, '.verify-suite.tmp'), ...spawnOpts } = opts;
  return new Promise((resolve) => {
    let fd;
    try { fd = openSync(outputFile, 'w'); }
    catch (error) { resolve({ code: 1, stdout: '', stderr: error.message }); return; }
    const child = spawn(cmd, args, { cwd: root, stdio: ['ignore', fd, fd], ...spawnOpts });
    let settled = false;
    const finish = (code, stderr = '') => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      try { closeSync(fd); } catch {}
      let stdout = '';
      try { stdout = readFileSync(outputFile, 'utf8'); } catch {}
      try { unlinkSync(outputFile); } catch {}
      resolve({ code, stdout, stderr });
    };
    const timer = setTimeout(() => {
      child.kill('SIGKILL');
      finish(124, `process timeout after ${timeoutMs}ms`);
    }, timeoutMs);
    child.on('error', error => finish(1, error.message));
    child.on('close', code => finish(code ?? 1));
  });
}

async function runTestSuite(testFiles, timeoutMs = 120000) {
  const chunkSize = 40;
  let totalTests = 0, totalPass = 0, totalFail = 0;
  const chunks = [];
  const stressFiles = testFiles.filter(file => /(?:^|\/)stress(?:-|_)/i.test(file));
  const regularFiles = testFiles.filter(file => !stressFiles.includes(file));
  if (stressFiles.length) chunks.push(stressFiles);
  for (let i = 0; i < regularFiles.length; i += chunkSize) chunks.push(regularFiles.slice(i, i + chunkSize));
  const quote = value => `'${String(value).replaceAll("'", "'\"'\"'")}'`;
  const outputFiles = chunks.map((_, chunkIndex) => join(tmpdir(), `.eotg-verify-suite-${process.pid}-${chunkIndex + 1}.tap`));
  for (const outputFile of outputFiles) { try { unlinkSync(outputFile); } catch {} }
  const command = chunks.map((chunk, chunkIndex) => {
    const nodeCommand = [quote(process.execPath), '--test-reporter=tap', '--test-force-exit', '--test', ...chunk.map(quote)].join(' ');
    return `${nodeCommand} > ${quote(outputFiles[chunkIndex])} 2>&1`;
  }).join(' && ');
  const result = await runAsync('sh', ['-lc', command], { timeoutMs });
  const taps = outputFiles.map(outputFile => {
    let text = ''; try { text = readFileSync(outputFile, 'utf8'); } catch {}
    try { unlinkSync(outputFile); } catch {}
    return text;
  });
  if (result.code !== 0) return { code: result.code, stdout: taps.join('\n'), stderr: result.stderr ?? '' };
  for (let chunkIndex = 0; chunkIndex < taps.length; chunkIndex++) {
    const tap = taps[chunkIndex];
    const tests = Number(/# tests\s+(\d+)/.exec(tap)?.[1] ?? NaN);
    const pass = Number(/# pass\s+(\d+)/.exec(tap)?.[1] ?? NaN);
    const fail = Number(/# fail\s+(\d+)/.exec(tap)?.[1] ?? NaN);
    if (![tests, pass, fail].every(Number.isFinite)) return { code: 1, stdout: tap, stderr: `chunk ${chunkIndex + 1} sem resumo TAP completo` };
    totalTests += tests; totalPass += pass; totalFail += fail;
  }
  const stdout = `# tests ${totalTests}\n# pass ${totalPass}\n# fail ${totalFail}\n`;
  return { code: totalFail === 0 ? 0 : 1, stdout, stderr: '' };
}

function readTapWithSummary(outputFile, timeoutMs = 3000) {
  const started = Date.now();
  let text = '';
  while (Date.now() - started < timeoutMs) {
    try { text = readFileSync(outputFile, 'utf8'); } catch { text = ''; }
    if (/# tests\s+\d+/.test(text) && /# pass\s+\d+/.test(text) && /# fail\s+\d+/.test(text)) return text;
    Atomics.wait(new Int32Array(new SharedArrayBuffer(4)), 0, 0, 50);
  }
  return text;
}

log(`# STAR WARS EOTG — verificação reproduzível`);
log(`timestamp=${now}`);

// 1) Full Node test suite. Use the Python runner because this host can deadlock when a Node parent chains many node --test children.
const tests = walk(join(root, 'tests')).filter(f => f.endsWith('.test.mjs')).sort();
const suite = await runAsync(pythonCommand, ['tools/run-node-suite.py'], { timeoutMs: 150000 });
if (suite.code !== 0) fail('suíte Node falhou');
else {
  const testsCount = /# tests\s+(\d+)/.exec(suite.stdout)?.[1];
  const pass = /# pass\s+(\d+)/.exec(suite.stdout)?.[1];
  const failCount = /# fail\s+(\d+)/.exec(suite.stdout)?.[1];
  const skippedCount = /# skipped\s+(\d+)/.exec(suite.stdout)?.[1] || '0';
  if (!testsCount || !pass || failCount === undefined) fail('suíte Node não produziu resumo TAP completo');
  else if (failCount !== '0' || (Number(pass) + Number(skippedCount) !== Number(testsCount))) fail(`suíte Node inconsistente tests=${testsCount} pass=${pass} fail=${failCount} skipped=${skippedCount}`);
  else log(`node_tests=PASS pass=${pass} fail=${failCount} skipped=${skippedCount}`);
}

// 2) 33-session stress coverage is embedded in the full suite above.
const stressNames = [
  'tests/stress-33-aftermath-r20.test.mjs',
  'tests/stress-33-faction-plan-r19.test.mjs',
  'tests/stress-33-sessions-r15.test.mjs',
  'tests/stress-faction-plans-r19.test.mjs',
  'tests/stress-factions-r18.test.mjs',
  'tests/stress-persistent-adversary-r17.test.mjs',
];
const missingStress = stressNames.filter(name => !tests.some(file => relative(root, file).replaceAll('\\','/') === name));
if (missingStress.length) fail(`stress files ausentes: ${missingStress.join(',')}`);
else if (suite.code === 0) log(`stress_33=PASS embedded_in_full_suite files=${stressNames.length}`);

// 3) JavaScript / ESM syntax.
const jsFiles = walk(join(root, 'app', 'src', 'main', 'assets', 'vtt'))
  .filter(f => ['.js', '.mjs'].includes(extname(f))).sort();
let nextJs = 0;
const jsFailures = [];
async function jsWorker() {
  while (true) {
    const index = nextJs++;
    if (index >= jsFiles.length) return;
    const f = jsFiles[index];
    const r = await runAsync(process.execPath, ['--check', f]);
    if (r.code !== 0) jsFailures.push(`${relative(root, f)}: ${(r.stderr || r.stdout).trim()}`);
  }
}
await Promise.all(Array.from({ length: Math.min(8, jsFiles.length) }, () => jsWorker()));
if (jsFailures.length) fail(`sintaxe JS: ${jsFailures[0]}`);
else log(`js_syntax=PASS files=${jsFiles.length}`);

// 4) Java syntax/duplicate-declaration gate without requiring Android SDK.
const javaGate = run(process.execPath, ['tools/java-syntax-gate.mjs']);
if (javaGate.code !== 0) fail(`java_syntax: ${(javaGate.stderr || javaGate.stdout).trim()}`);
else log(`java_syntax=${javaGate.stdout.trim()}`);

// 5) JSON parsing.
const jsonFiles = walk(root).filter(f => f.endsWith('.json') && !f.includes('/.gradle/')).sort();
let jsonOk = 0;
for (const f of jsonFiles) {
  try { JSON.parse(readFileSync(f, 'utf8')); jsonOk++; }
  catch (e) { fail(`JSON inválido: ${relative(root, f)}: ${e.message}`); break; }
}
if (jsonOk === jsonFiles.length) log(`json_parse=PASS files=${jsonOk}`);

// 6) XML parsing via Python stdlib (available in validation/runtime environments used here).
const xmlFiles = walk(join(root, 'app', 'src', 'main')).filter(f => f.endsWith('.xml')).sort();
let xmlOk = 0;
for (const f of xmlFiles) {
  const py = run(pythonCommand, ['-c', 'import sys,xml.etree.ElementTree as ET; ET.parse(sys.argv[1])', f]);
  if (py.code !== 0) { fail(`XML inválido: ${relative(root, f)}`); break; }
  xmlOk++;
}
if (xmlOk === xmlFiles.length) log(`xml_parse=PASS files=${xmlOk}`);

// 7) Secret scan in source-like files.
const scanRoots = ['app', 'rules', 'shell-r46-run87', 'tests', 'tools'].map(p => join(root, p)).filter(existsSync);
const candidates = scanRoots.flatMap(walk).filter(f => ['.js','.mjs','.java','.kt','.kts','.html','.css','.xml','.sh'].includes(extname(f)));
const secretPatterns = [
  /AIza[0-9A-Za-z_-]{30,}/g,
  /sk-[0-9A-Za-z_-]{20,}/g,
  /BEGIN (?:RSA|OPENSSH|EC) PRIVATE KEY/g,
];
const hits = [];
for (const f of candidates) {
  const text = readFileSync(f, 'utf8');
  if (secretPatterns.some(re => re.test(text))) hits.push(relative(root, f));
  for (const re of secretPatterns) re.lastIndex = 0;
}
if (hits.length) fail(`secret_scan hits=${hits.join(',')}`); else log('secret_scan=PASS hits=0');

// 8) No APK/AAB inside source tree.
const binaries = walk(root).filter(f => /\.(apk|aab)$/i.test(f));
if (binaries.length) fail(`apk_aab_count=${binaries.length}`); else log('apk_aab_count=0');

// 9) Gradle wrapper integrity / host prerequisites.
const wrapperJar = join(root, 'gradle', 'wrapper', 'gradle-wrapper.jar');
const wrapperProps = join(root, 'gradle', 'wrapper', 'gradle-wrapper.properties');
if (!existsSync(wrapperJar) || !existsSync(wrapperProps)) fail('Gradle Wrapper incompleto');
else {
  const jarHash = crypto.createHash('sha256').update(readFileSync(wrapperJar)).digest('hex');
  const props = readFileSync(wrapperProps, 'utf8');
  const jarCommand = executableOnPath(process.platform === 'win32' ? 'jar.exe' : 'jar');
  const mainCheck = jarCommand ? run(jarCommand, ['tf', wrapperJar]) : null;
  const jarBuf = readFileSync(wrapperJar);
  const hasMain = (mainCheck && mainCheck.code === 0)
    ? mainCheck.stdout.includes('org/gradle/wrapper/GradleWrapperMain.class')
    : jarBuf.includes(Buffer.from('org/gradle/wrapper/GradleWrapperMain.class'));
  if (!hasMain) fail('gradle-wrapper.jar sem GradleWrapperMain');
  else log(`gradle_wrapper=PASS jar_sha256=${jarHash}`);
  const dist = /distributionUrl=(.+)/.exec(props)?.[1]?.replaceAll('\\:', ':') ?? '';
  const sha = /distributionSha256Sum=(.+)/.exec(props)?.[1]?.trim() ?? '';
  log(`gradle_distribution=${dist}`);
  log(`gradle_distribution_sha256=${sha}`);
}

const defaultAndroidSdk = process.platform === 'win32'
  ? join(process.env.LOCALAPPDATA || '', 'Android', 'Sdk')
  : join(process.env.HOME || '', 'Android', 'Sdk');
const envAndroid = process.env.ANDROID_SDK_ROOT || process.env.ANDROID_HOME || (existsSync(defaultAndroidSdk) ? defaultAndroidSdk : '');
const adb = executableOnPath(process.platform === 'win32' ? 'adb.exe' : 'adb')
  || (envAndroid && existsSync(join(envAndroid, 'platform-tools', process.platform === 'win32' ? 'adb.exe' : 'adb'))
    ? join(envAndroid, 'platform-tools', process.platform === 'win32' ? 'adb.exe' : 'adb') : '');
const sdkmanagerName = process.platform === 'win32' ? 'sdkmanager.bat' : 'sdkmanager';
const sdkmanager = executableOnPath(sdkmanagerName);
if (!envAndroid || !adb || !sdkmanager) {
  log(`android_gate=BLOCKED_EXTERNAL sdk=${envAndroid || 'missing'} adb=${adb || 'missing'} sdkmanager=${sdkmanager || 'missing'}`);
} else {
  log(`android_gate=PREREQS_PRESENT sdk=${envAndroid} adb=${adb} sdkmanager=${sdkmanager}`);
}

const out = join(validationDir, 'LATEST_SOURCE_VALIDATION.txt');
writeFileSync(out, lines.join('\n') + '\n', 'utf8');
log(`report=${relative(root, out)}`);

const finalExitCode = process.exitCode || 0;
process.exit(finalExitCode);
