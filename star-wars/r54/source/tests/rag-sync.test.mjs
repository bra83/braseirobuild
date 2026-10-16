import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import crypto from 'node:crypto';
import { VERIFIED_RULE_CORPUS, RULE_CORPUS_VERSION } from '../app/src/main/assets/vtt/js/rag/rulesCorpus.mjs';

test('corpus RAG permanece sincronizado com a matriz de regras',()=>{
  const raw=fs.readFileSync(new URL('../rules/rules_matrix.json',import.meta.url));
  const matrix=JSON.parse(raw.toString('utf8'));
  const manifest=JSON.parse(fs.readFileSync(new URL('../rules/rag_manifest.json',import.meta.url),'utf8'));
  assert.equal(RULE_CORPUS_VERSION,manifest.version);
  assert.equal(VERIFIED_RULE_CORPUS.length,matrix.length);
  assert.deepEqual(VERIFIED_RULE_CORPUS.map(r=>r.id),matrix.map(r=>r.regra_id));
  assert.equal(manifest.sourceSha256,crypto.createHash('sha256').update(raw).digest('hex'));
});
