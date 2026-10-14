#!/usr/bin/env node
// Validate actual production-source turn-flow evidence; no shell quoting or upload dependency.
import fs from 'node:fs';
import path from 'node:path';

const file = path.resolve(process.argv[2] || 'OUA_49202_TURN_FLOW_AUDIT.json');
if (!fs.existsSync(file)) {
  console.error('OUA_BASE_AUDIT_MISSING ' + file);
  process.exit(2);
}
let report;
try { report = JSON.parse(fs.readFileSync(file, 'utf8')); }
catch (error) {
  console.error('OUA_BASE_AUDIT_INVALID_JSON ' + String(error));
  process.exit(2);
}
const valid = Number.isInteger(report.assertions)
  && report.assertions > 0
  && Number.isInteger(report.passed)
  && Number.isInteger(report.failed)
  && report.failed === 0
  && report.passed === report.assertions
  && Number.isInteger(report.sessionsSimulated)
  && report.sessionsSimulated > 0
  && Array.isArray(report.failures)
  && report.failures.length === 0
  && Array.isArray(report.checks)
  && report.checks.length === report.assertions
  && report.checks.every(check => check.ok === true);
if (!valid) {
  console.error('OUA_BASE_AUDIT_FAILED ' + JSON.stringify({
    sessions: report.sessionsSimulated,
    assertions: report.assertions,
    passed: report.passed,
    failed: report.failed,
    failureCount: report.failures?.length,
    checkCount: report.checks?.length,
  }));
  process.exit(2);
}
const line = 'OUA_BASE_GAMEPLAY_PASS sessions=' + report.sessionsSimulated + ' assertions=' + report.assertions;
console.log(line);
if (process.env.GITHUB_STEP_SUMMARY) {
  fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY,
    '\n### O Um Anel 2e — core production regression\n\n' +
    '- Sessions simulated: ' + report.sessionsSimulated + '\n' +
    '- Assertions passed: ' + report.passed + '/' + report.assertions + '\n' +
    '- Failed: ' + report.failed + '\n');
}
