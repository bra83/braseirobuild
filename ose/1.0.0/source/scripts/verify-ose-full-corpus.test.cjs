#!/usr/bin/env node
'use strict';
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const {spawnSync} = require('node:child_process');
const {verify} = require('./verify-ose-full-corpus.cjs');
const required = ['classic-rules-tome', 'advanced-player', 'advanced-referee',
  'classic-magic', 'classic-treasures', 'advanced-bestiary'];
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'ose-r7-corpus-'));
const files = {
  kotlinExecutor: 'rules-classic/src/main/kotlin/Engine.kt',
  runtimeEntrypoint: 'session-engine/src/main/kotlin/Session.kt',
  uiEntrypoint: 'web-ui-p1/p1.js',
  persistedState: 'core-model/src/main/kotlin/State.kt',
  persistenceTest: 'integration-tests/src/test/kotlin/PersistenceTest.kt',
  integrationTest: 'integration-tests/src/test/kotlin/IntegrationTest.kt',
  androidTest: 'app-android/src/test/kotlin/AndroidTest.kt'
};
const symbols = {
  kotlinExecutor: 'applyRule', runtimeEntrypoint: 'resolveRule',
  uiEntrypoint: 'renderRule', persistedState: 'RuleState',
  persistenceTest: 'persistenceRuleTest', integrationTest: 'integrationRuleTest',
  androidTest: 'androidRuleTest'
};
const fields = ['sourceChapters','ruleIds','kotlinExecutors','runtimeEntrypoints',
  'persistenceTests','integrationTests','androidTests'];
function createFile(p, content) {
  const file = path.join(root, p);
  fs.mkdirSync(path.dirname(file), {recursive:true});
  fs.writeFileSync(file, content);
}
for (const [kind, file] of Object.entries(files))
  createFile(file, `${kind.endsWith('Test') ? '@Test\n' : ''}fun ${symbols[kind]}() { assert(true) }\n`);
function entry(id, complete=false) {
  const evidence = Object.fromEntries(fields.map(f => [f, []]));
  const result = {id, kind:'CORE_CLASSIC', sourceUrl:`https://example.test/${id}`,
    status:complete?'COMPLETE_VERIFIED':'PARTIAL_RUNTIME_UNVERIFIED', evidence};
  if (complete) {
    evidence.sourceChapters=['Chapter 1'];
    evidence.ruleIds=['rule-1'];
    evidence.kotlinExecutors=[files.kotlinExecutor];
    evidence.runtimeEntrypoints=[files.runtimeEntrypoint];
    evidence.persistenceTests=[files.persistenceTest];
    evidence.integrationTests=[files.integrationTest];
    evidence.androidTests=[files.androidTest];
    result.ruleCoverage=[{ruleId:'rule-1',sourceChapter:'Chapter 1',
      ...Object.fromEntries(Object.entries(files).map(([k,p]) => [k,{path:p,symbol:symbols[k]}]))}];
  }
  return result;
}
const body = () => ({schema:'BRASEIRO_OSE_CORPUS_V1',
  sources:required.map((id,i) => entry(id,i===0)), knownSourceGaps:['UNRESOLVED_RULE']});
function expectFailure(name, transform, code) {
  const doc=body(); transform(doc);
  const result=verify(doc,root);
  assert.equal(result.auditGatePassed,false,name);
  assert.ok(result.errors.some(e=>e.startsWith(code)),`${name}: expected ${code}, got ${result.errors}`);
  process.stdout.write(`PASS ${name}\n`);
}
try {
  const baseline=verify(body(),root);
  assert.equal(baseline.auditGatePassed,true);
  assert.equal(baseline.rulesComplete,false);
  assert.equal(baseline.verifiedCoreSources,1);
  assert.equal(baseline.requiredCoreSources,6);
  console.log('PASS partial corpus audit succeeds but is never marked complete');
  expectFailure('missing per-rule claim', d=>{delete d.sources[0].ruleCoverage;},
    'FALSE_COMPLETE_WITHOUT_PER_RULE_PROOF');
  expectFailure('missing UI file', d=>{d.sources[0].ruleCoverage[0].uiEntrypoint.path='web-ui-p1/missing.js';},
    'PROOF_FILE_MISSING');
  expectFailure('absent Kotlin symbol', d=>{d.sources[0].ruleCoverage[0].kotlinExecutor.symbol='fakeRule';},
    'PROOF_SYMBOL_ABSENT');
  expectFailure('path traversal', d=>{d.sources[0].ruleCoverage[0].persistedState.path='../escape.kt';},
    'PROOF_PATH_ESCAPE');
  expectFailure('missing Android proof', d=>{delete d.sources[0].ruleCoverage[0].androidTest;},
    'INVALID_PROOF_REFERENCE');
  expectFailure('android proof in JVM module', d=>{d.sources[0].ruleCoverage[0].androidTest.path=files.integrationTest;},
    'NON_ANDROID_TEST_PROOF');
  expectFailure('unlisted rule', d=>{d.sources[0].ruleCoverage[0].ruleId='rule-2';},
    'UNLISTED_RULE_CLAIM');
  expectFailure('orphan evidence', d=>{d.sources[0].evidence.kotlinExecutors.push('bogus.kt');},
    'UNLINKED_EVIDENCE_PATH');
  expectFailure('unproven rule ID', d=>{d.sources[0].evidence.ruleIds.push('rule-2');},
    'UNPROVEN_RULE_ID');
  expectFailure('duplicate source', d=>{d.sources.push(structuredClone(d.sources[0]));},
    'DUPLICATE_OR_INVALID_ID');
  expectFailure('source chapter mismatch', d=>{d.sources[0].ruleCoverage[0].sourceChapter='Chapter 999';},
    'UNLINKED_SOURCE_CHAPTER');
  const complete=body();
  complete.sources=required.map(id=>entry(id,true));
  assert.equal(verify(complete,root).rulesComplete,false,'known gaps must block full rules');
  complete.knownSourceGaps=[];
  assert.equal(verify(complete,root).rulesComplete,true,'fully linked six core sources may pass rules proof');
  const file=path.join(root,'corpus.json');fs.writeFileSync(file,JSON.stringify(body()));
  const run=spawnSync(process.execPath,[path.join(__dirname,'verify-ose-full-corpus.cjs'),
    '--root',root,'--manifest',file,'--require-full-rules'],{encoding:'utf8'});
  assert.equal(run.status,2,'release gate must reject partial manifest even with valid schema');
  assert.match(run.stderr,/OSE_RULES_INCOMPLETE/);
  console.log('PASS known gaps and release flag are fail-closed');
  console.log('R7 corpus audit gate: 14 deterministic checks PASS');
} finally { fs.rmSync(root,{recursive:true,force:true}); }