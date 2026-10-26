#!/usr/bin/env node
// OSE full-source audit gate. This does NOT equate CI pass with full coverage.
'use strict';
const fs=require('node:fs');
const path=require('node:path');
const file=path.resolve(__dirname,'../docs/OSE_FULL_CORPUS_COVERAGE.json');
const body=JSON.parse(fs.readFileSync(file,'utf8'));
const failures=[];
if(body.schema!=='BRASEIRO_OSE_CORPUS_V1') failures.push('INVALID_SCHEMA');
const allowed=new Set(body.statusVocabulary);
const ids=new Set(),links=new Set();
for(const entry of body.sources||[]) {
  if(!entry.id||ids.has(entry.id))failures.push('DUPLICATE_OR_EMPTY_ID:'+entry.id);
  ids.add(entry.id);
  if(!allowed.has(entry.status))failures.push('INVALID_STATUS:'+entry.id);
  if(entry.sourceUrl && links.has(entry.sourceUrl) && entry.kind!=='CORE_CLASSIC_CONFIRMATION') {
    failures.push('DUPLICATE_CANONICAL_SOURCE_URL:'+entry.id);
  }
  if(entry.sourceUrl)links.add(entry.sourceUrl);
  if(!entry.sourceUrl && entry.status!=='SOURCE_SCOPE_REVIEW')failures.push('MISSING_URL:'+entry.id);
  const proof=entry.evidence||{};
  const fields=['sourceChapters','ruleIds','kotlinExecutors','runtimeEntrypoints','persistenceTests','integrationTests','androidTests'];
  if(fields.some(k=>!Array.isArray(proof[k])))failures.push('INCOMPLETE_EVIDENCE_SCHEMA:'+entry.id);
  if(entry.status==='COMPLETE_VERIFIED' && fields.some(k=>!proof[k]||proof[k].length===0))
    failures.push('FALSE_COMPLETE_WITHOUT_PROOF:'+entry.id);
}
for(const key of ['classic-rules-tome','advanced-player','advanced-referee','classic-magic','classic-treasures','advanced-bestiary']) {
 if(!ids.has(key))failures.push('MISSING_REQUIRED_CORE:'+key);
}
const counts={}; for(const entry of body.sources||[])counts[entry.status]=(counts[entry.status]||0)+1;
const allVerified=(body.sources||[]).every(s=>s.status==='COMPLETE_VERIFIED');
console.log(JSON.stringify({schema:body.schema,sourceCount:ids.size,statusCounts:counts,
  corpusComplete:allVerified,knownGaps:body.knownSourceGaps,auditGatePassed:failures.length===0},null,2));
if(failures.length){for(const x of failures)console.error('OSE_AUDIT_ERROR '+x);process.exit(1)}
if(allVerified)console.log('FULL_CORPUS_PROVEN');
else console.log('FULL_CORPUS_PENDING — never report 100% based on this passing audit gate');
