import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const main=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/MainActivity.java',import.meta.url),'utf8');
const shell=fs.readFileSync(new URL('../app/src/main/assets/vtt/r46/r46-shell.bundle.js',import.meta.url),'utf8');

test('R54 usa file picker apenas para imports JSON conhecidos',()=>{
  const fileInputs=[...shell.matchAll(/type:\s*"file"/g)];
  const jsonAccept=[...shell.matchAll(/accept:\s*"application\/json,\.json"/g)];
  assert.ok(fileInputs.length>=2,'a shell deve manter os imports JSON de personagem e backup');
  assert.equal(jsonAccept.length,fileInputs.length,'todo input file da shell precisa permanecer restrito a JSON');
  assert.match(shell,/importCharacterJsonFile\(file\)/);
  assert.match(shell,/importCampaignBackupFile\(file\)/);
  assert.doesNotMatch(shell,/type:\s*"file"[\s\S]{0,180}accept:\s*"(?!application\/json,\.json)/);
});

test('Android abre ACTION_OPEN_DOCUMENT restrito a JSON/texto compatível',()=>{
  assert.match(main,/new Intent\(Intent\.ACTION_OPEN_DOCUMENT\)/);
  assert.match(main,/Intent\.CATEGORY_OPENABLE/);
  assert.match(main,/intent\.setType\("application\/json"\)/);
  assert.match(main,/"application\/json",\s*"text\/json",\s*"text\/plain"/);
});

test('resultado do picker JSON volta ao input WebView pelo FileChooserParams.parseResult',()=>{
  assert.match(main,/if \(requestCode == PICK_JSON_REQUEST\)[\s\S]*pendingJsonChooser = null;[\s\S]*FileChooserParams\.parseResult\(resultCode, data\)/);
});

test('shell lê conteúdo do arquivo antes de chamar os imports do Motor Barbara',()=>{
  assert.match(shell,/importCharacterJsonThroughBridge\)\(await file\.text\(\)\)/);
  assert.match(shell,/importCampaignBackupThroughBridge\)\(await file\.text\(\)\)/);
});
