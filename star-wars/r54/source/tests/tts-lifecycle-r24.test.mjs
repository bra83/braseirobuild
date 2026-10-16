import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const src=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java',import.meta.url),'utf8');
function block(start,next='\n    @JavascriptInterface'){const i=src.indexOf(start);assert.ok(i>=0,`${start} missing`);let j=src.indexOf(next,i+start.length);if(j<0)j=src.length;return src.slice(i,j)}
test('onInit e shutdown compartilham lifecycleLock para impedir reativação tardia',()=>{const init=block('public void onInit(int status)','\n    @JavascriptInterface');const shut=block('public void shutdown()','\n    private static');assert.match(src,/private final Object lifecycleLock/);assert.match(init,/synchronized \(lifecycleLock\)/);assert.match(init,/if \(closed\.get\(\)\) return/);assert.match(shut,/synchronized \(lifecycleLock\)/)});
test('shutdown é idempotente e marca TTS fechado/indisponível antes de encerrar engine',()=>{const b=block('public void shutdown()','\n    private static');assert.match(b,/compareAndSet\(false, true\)/);assert.match(b,/ready\.set\(false\)/);assert.match(b,/tts\.shutdown\(\)/)});
test('speak recusa bridge fechado e UI runnable revalida lifecycle antes de tocar TTS',()=>{const b=block('public String speak');assert.match(b,/if \(closed\.get\(\)\) return fail\("TTS_CLOSED"/);assert.match(b,/runOnUiThread\(\(\) -> \{\s*if \(closed\.get\(\)\) return;/s)});
test('stop não agenda operação efetiva depois de shutdown',()=>{const b=block('public String stop()','\n    public void shutdown');assert.match(b,/if \(closed\.get\(\)\) return fail\("TTS_CLOSED"/);assert.match(b,/if \(!closed\.get\(\)\) tts\.stop\(\)/)});
