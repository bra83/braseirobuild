import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java', import.meta.url), 'utf8');

function methodBlock(signature, nextSignature) {
  const start = source.indexOf(signature);
  const end = source.indexOf(nextSignature, start + signature.length);
  assert.ok(start >= 0 && end > start, `bloco ausente: ${signature}`);
  return source.slice(start, end);
}

test('política da Biblioteca e gravações do indexador usam o mesmo lock de metadados', () => {
  assert.match(source, /private final Object libraryMetadataLock = new Object\(\);/);
  const policy = methodBlock('public String setDocumentPolicy', '@JavascriptInterface',);
  assert.match(policy, /synchronized \(libraryMetadataLock\) \{[\s\S]*readMetadata\(dir\)[\s\S]*writeMetadata\(dir, metadata\)/);

  const helper = methodBlock('private void writeIndexMetadataPreservingPolicy', 'private JSONObject readMetadata');
  assert.match(helper, /synchronized \(libraryMetadataLock\)/);
  assert.match(helper, /JSONObject latest = readMetadata\(dir\)/);
  for (const field of ['authorityType', 'enabled', 'policyUpdatedAt']) {
    assert.match(helper, new RegExp(`latest\\.has\\("${field}"\\)`), `campo mutável não preservado: ${field}`);
  }
  assert.match(helper, /writeMetadata\(dir, metadata\)/);
});

test('indexação periódica, conclusão e falha preservam a política mais recente', () => {
  const index = methodBlock('private void indexPdf', 'private JSONArray listDocumentsJson');
  assert.equal((index.match(/writeIndexMetadataPreservingPolicy\(dir, metadata\)/g) || []).length, 2,
    'indexPdf deve preservar política tanto no progresso quanto na conclusão');

  const error = methodBlock('private void markIndexError', 'private void writeIndexMetadataPreservingPolicy');
  assert.match(error, /writeIndexMetadataPreservingPolicy\(dir, metadata\)/);
});


test('remoção e acesso a metadata são serializados para impedir documento fantasma durante indexação', () => {
  const remove = methodBlock('public String removeDocument', '    }\n\n    private void importPdf');
  assert.match(remove, /synchronized \(libraryMetadataLock\)/);
  assert.match(remove, /deleteRecursively\(dir\)/);
  assert.match(remove, /if \(dir\.exists\(\)\) throw new IllegalStateException/);

  const read = methodBlock('private JSONObject readMetadata', 'private void writeMetadata');
  assert.match(read, /synchronized \(libraryMetadataLock\)/);
  const write = methodBlock('private void writeMetadata', 'private File safeDocumentDir');
  assert.match(write, /synchronized \(libraryMetadataLock\)/);

  const helper = methodBlock('private void writeIndexMetadataPreservingPolicy', 'private JSONObject readMetadata');
  assert.match(helper, /if \(!dir\.exists\(\)\) throw new IllegalStateException\("Documento removido durante a indexação\."\)/);
});
