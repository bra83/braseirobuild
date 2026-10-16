import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const text=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java', import.meta.url),'utf8');
const start=text.indexOf('public String pickSceneImage');
const end=text.indexOf('@JavascriptInterface', start+1);
const method=text.slice(start, end);

test('seletor de imagem captura falha de startActivityForResult dentro da UI thread',()=>{
  assert.match(method,/runOnUiThread\(\(\) -> \{\s*try \{/s);
  assert.match(method,/startActivityForResult\(intent, PICK_SCENE_IMAGE_REQUEST\)/);
  assert.match(method,/catch \(Exception e\) \{\s*pendingImportSceneKey = null;\s*notifyEvent\("image_error"/s);
});

test('falha assíncrona do picker limpa a cena pendente e informa mensagem segura',()=>{
  assert.match(method,/pendingImportSceneKey = cleanSceneKey/);
  assert.match(method,/pendingImportSceneKey = null/);
  assert.match(method,/Não foi possível abrir o seletor de imagem/);
});
