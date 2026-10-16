import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const gemini=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java',import.meta.url),'utf8');
const cache=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/ImageLibraryBridge.java',import.meta.url),'utf8');
function block(src,start,next='\n    private '){const i=src.indexOf(start);assert.ok(i>=0,`${start} missing`);const j=src.indexOf(next,i+start.length);return src.slice(i,j<0?src.length:j)}
test('imagem principal é aberta sob o mesmo lock usado por clear/commit',()=>{const b=block(gemini,'WebResourceResponse openSceneImageResponse');assert.match(b,/synchronized \(sceneImageCommitLock\)/);assert.match(b,/new FileInputStream\(imageFile\)/)});
test('status da imagem principal usa sceneImageCommitLock',()=>{const b=block(gemini,'private JSONObject statusJson()');assert.match(b,/synchronized \(sceneImageCommitLock\)/);assert.match(b,/readJson\(imageMetaFile\)/)});
test('imagem do cache é aberta sob cacheCommitLock',()=>{const b=block(cache,'WebResourceResponse openCachedImageResponse');assert.match(b,/synchronized \(cacheCommitLock\)/);assert.match(b,/new FileInputStream\(image\)/)});
test('status do cache usa cacheCommitLock sem perder validação do assetId',()=>{const b=block(cache,'private JSONObject statusJson(String assetId)');assert.match(b,/synchronized \(cacheCommitLock\)/);assert.match(b,/!assetId\.equals\(data\.optString\("assetId"\)\)/)});
