import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const src=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/FileBridge.java',import.meta.url),'utf8');
test('MediaStore cria JSON como pendente antes de escrever',()=>{assert.match(src,/IS_PENDING, 1/);assert.match(src,/insert\(MediaStore\.Downloads\.EXTERNAL_CONTENT_URI, values\)/)});
test('publicação exige exatamente uma linha atualizada antes de retornar sucesso',()=>{assert.match(src,/int published = activity\.getContentResolver\(\)\.update\(uri, done, null, null\)/);assert.match(src,/if \(published != 1\) throw new IllegalStateException/);const publish=src.indexOf('int published =');const success=src.indexOf('return new JSONObject().put("ok", true)',publish);assert.ok(publish>=0&&success>publish)});
test('falha de escrita ou publicação remove o item pendente do MediaStore',()=>{const insert=src.indexOf('Uri uri =');const legacy=src.indexOf('File root =',insert);const block=src.slice(insert,legacy);assert.match(block,/catch \(Exception e\) \{\s*try \{ activity\.getContentResolver\(\)\.delete\(uri, null, null\); \} catch \(Exception ignored\) \{ \}\s*throw e;/s)});
