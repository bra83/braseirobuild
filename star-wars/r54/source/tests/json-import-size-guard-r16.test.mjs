import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const source=fs.readFileSync(new URL('../shell-r46-run87/VTTApp.tsx',import.meta.url),'utf8');
const bundle=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/r46-shell.bundle.js',import.meta.url),'utf8');

test('fonte R46 limita importação de ficha e backup ao mesmo teto de 16 MB do FileBridge',()=>{
  assert.match(source,/const MAX_JSON_IMPORT_BYTES = 16 \* 1024 \* 1024;/);
  assert.match(source,/importCharacterJsonFile[\s\S]*file\.size > MAX_JSON_IMPORT_BYTES[\s\S]*O arquivo JSON excede 16 MB/);
  assert.match(source,/importCampaignBackupFile[\s\S]*file\.size > MAX_JSON_IMPORT_BYTES[\s\S]*O backup excede 16 MB/);
});

test('bundle Android contém os mesmos guards antes de file.text()',()=>{
  assert.match(bundle,/const MAX_JSON_IMPORT_BYTES = 16 \* 1024 \* 1024;/);
  const char=bundle.indexOf("O arquivo JSON excede 16 MB.");
  const charRead=bundle.indexOf('importCharacterJsonThroughBridge)(await file.text())');
  const backup=bundle.indexOf("O backup excede 16 MB.");
  const backupRead=bundle.indexOf('importCampaignBackupThroughBridge)(await file.text())');
  assert.ok(char>=0 && charRead>char);
  assert.ok(backup>=0 && backupRead>backup);
});
