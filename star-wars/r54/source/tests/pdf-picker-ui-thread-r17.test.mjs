import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';

const root = path.resolve(import.meta.dirname, '..');
const javaPath = path.join(root, 'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const source = fs.readFileSync(javaPath, 'utf8');

test('PDF picker catches Activity launch errors inside the UI-thread runnable', () => {
  const start = source.indexOf('public String pickPdf()');
  const end = source.indexOf('@JavascriptInterface', start + 1);
  assert.ok(start >= 0 && end > start, 'pickPdf method not found');
  const block = source.slice(start, end);
  assert.match(block, /runOnUiThread\(\(\) -> \{\s*try \{/s);
  assert.match(block, /startActivityForResult\(intent, PICK_PDF_REQUEST\)/);
  assert.match(block, /catch \(Exception e\) \{\s*try \{\s*notifyLibraryEvent\("erro"/s);
});

test('PDF reindex timestamp is written once per reindex request', () => {
  const start = source.indexOf('public String reindexDocument');
  const end = source.indexOf('@JavascriptInterface', start + 1);
  assert.ok(start >= 0 && end > start, 'reindexDocument method not found');
  const block = source.slice(start, end);
  assert.equal((block.match(/reindexRequestedAt/g) || []).length, 1);
});
