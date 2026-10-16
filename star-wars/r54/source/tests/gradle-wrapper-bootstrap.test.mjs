import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawnSync } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const ROOT = path.resolve(import.meta.dirname, '..');
const WRAPPER_JAR = path.join(ROOT, 'gradle/wrapper/gradle-wrapper.jar');
const UNIX_LAUNCHER = path.join(ROOT, 'gradlew');
const WINDOWS_LAUNCHER = path.join(ROOT, 'gradlew.bat');
const LAUNCHER = process.platform === 'win32' ? WINDOWS_LAUNCHER : UNIX_LAUNCHER;
const LAUNCHER_NAME = process.platform === 'win32' ? 'gradlew.bat' : 'gradlew';
const GRADLE_BIN_NAME = process.platform === 'win32' ? 'gradle.bat' : 'gradle';
const JAR_AVAILABLE = spawnSync('jar', ['--version'], { encoding: 'utf8' }).status === 0;
const JAR_SKIP_REASON = 'JDK jar não disponível neste ambiente; testes de empacotamento offline exigem um JDK.';

function sha256(file) {
  return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
}

function fixture() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'eotg-wrapper-'));
  fs.mkdirSync(path.join(dir, 'gradle/wrapper'), { recursive: true });
  fs.copyFileSync(WRAPPER_JAR, path.join(dir, 'gradle/wrapper/gradle-wrapper.jar'));
  const launcher = path.join(dir, LAUNCHER_NAME);
  fs.copyFileSync(LAUNCHER, launcher);
  if (process.platform !== 'win32') fs.chmodSync(launcher, 0o755);

  const payload = path.join(dir, 'payload/gradle-8.13/bin');
  fs.mkdirSync(payload, { recursive: true });
  const fakeGradle = path.join(payload, GRADLE_BIN_NAME);
  if (process.platform === 'win32') {
    fs.writeFileSync(fakeGradle, '@echo off\r\n> "%WRAPPER_TEST_LOG%" echo PWD=%CD%\r\n:args\r\nif "%~1"=="" goto done\r\n>> "%WRAPPER_TEST_LOG%" echo ARG=%~1\r\nshift\r\ngoto args\r\n:done\r\nexit /b 0\r\n');
  } else {
    fs.writeFileSync(fakeGradle, `#!/usr/bin/env sh\nset -eu\n{ printf 'PWD=%s\\n' "$PWD"; for arg in "$@"; do printf 'ARG=%s\\n' "$arg"; done; } > "$WRAPPER_TEST_LOG"\n`);
    fs.chmodSync(fakeGradle, 0o755);
  }

  const zip = path.join(dir, 'gradle-8.13-bin.zip');
  const packed = spawnSync('jar', ['cf', zip, '-C', path.join(dir, 'payload'), '.'], { encoding: 'utf8' });
  assert.equal(packed.status, 0, packed.stderr || packed.stdout);
  return { dir, zip, checksum: sha256(zip) };
}

function writeProperties(dir, zip, checksum) {
  fs.writeFileSync(path.join(dir, 'gradle/wrapper/gradle-wrapper.properties'), [
    'distributionBase=GRADLE_USER_HOME',
    'distributionPath=wrapper/dists',
    `distributionUrl=${pathToFileURL(zip).href}`,
    `distributionSha256Sum=${checksum}`,
    'networkTimeout=10000',
    'validateDistributionUrl=true',
    'zipStoreBase=GRADLE_USER_HOME',
    'zipStorePath=wrapper/dists',
    ''
  ].join('\n'));
}

test('BUILD: launchers delegate only to the packaged GradleWrapperMain', () => {
  const sh = fs.readFileSync(UNIX_LAUNCHER, 'utf8');
  const bat = fs.readFileSync(WINDOWS_LAUNCHER, 'utf8');
  assert.match(sh, /gradle\/wrapper\/gradle-wrapper\.jar/);
  assert.match(sh, /org\.gradle\.wrapper\.GradleWrapperMain/);
  assert.doesNotMatch(sh, /curl|wget|unzip|sha256sum/);
  assert.match(bat, /gradle\\wrapper\\gradle-wrapper\.jar/);
  assert.match(bat, /org\.gradle\.wrapper\.GradleWrapperMain/);
  assert.doesNotMatch(bat, /powershell|Invoke-WebRequest|Expand-Archive|Get-FileHash/i);
});

test('BUILD: packaged wrapper executes a verified offline distribution and forwards arguments', { skip: JAR_AVAILABLE ? false : JAR_SKIP_REASON }, () => {
  const fx = fixture();
  try {
    writeProperties(fx.dir, fx.zip, fx.checksum);
    const log = path.join(fx.dir, 'wrapper.log');
    const home = path.join(fx.dir, 'gradle-home');
    const forwardedArgs = process.platform === 'win32' ? ['alpha', '"two words"'] : ['alpha', 'two words'];
    const run = spawnSync(path.join(fx.dir, LAUNCHER_NAME), forwardedArgs, {
      cwd: fx.dir,
      env: { ...process.env, GRADLE_USER_HOME: home, WRAPPER_TEST_LOG: log },
      shell: process.platform === 'win32',
      encoding: 'utf8'
    });
    assert.equal(run.status, 0, run.stderr || run.stdout);
    const output = fs.readFileSync(log, 'utf8');
    assert.match(output, new RegExp(`PWD=${fx.dir.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`));
    assert.match(output, /ARG=alpha/);
    assert.match(output, /ARG=two words/);
    assert.ok(fs.existsSync(path.join(home, 'wrapper/dists/braseiro-8.13/gradle-8.13/bin', GRADLE_BIN_NAME)));
  } finally {
    fs.rmSync(fx.dir, { recursive: true, force: true });
  }
});

test('BUILD: packaged wrapper rejects a distribution with the wrong SHA-256', { skip: JAR_AVAILABLE ? false : JAR_SKIP_REASON }, () => {
  const fx = fixture();
  try {
    writeProperties(fx.dir, fx.zip, '0'.repeat(64));
    const log = path.join(fx.dir, 'should-not-run.log');
    const run = spawnSync(path.join(fx.dir, LAUNCHER_NAME), ['alpha'], {
      cwd: fx.dir,
      env: { ...process.env, GRADLE_USER_HOME: path.join(fx.dir, 'gradle-home'), WRAPPER_TEST_LOG: log },
      shell: process.platform === 'win32',
      encoding: 'utf8'
    });
    assert.notEqual(run.status, 0);
    assert.match(`${run.stdout}\n${run.stderr}`, /SHA-256 mismatch/i);
    assert.equal(fs.existsSync(log), false);
  } finally {
    fs.rmSync(fx.dir, { recursive: true, force: true });
  }
});

test('BUILD: production wrapper uses the official Gradle distribution host with pinned SHA-256', () => {
  const properties = fs.readFileSync(path.join(ROOT, 'gradle/wrapper/gradle-wrapper.properties'), 'utf8');
  assert.match(properties, /downloads\.gradle\.org\/distributions\/gradle-8\.13-bin\.zip/);
  assert.match(properties, /distributionSha256Sum=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78/);
});
