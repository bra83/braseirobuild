import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
const root=path.resolve(new URL('../app/src/main/assets/www/',import.meta.url).pathname);
const pages=['jornada.html','regras.html','scr014.html','scr022.html'];
for(const page of pages){const t=fs.readFileSync(path.join(root,page),'utf8');assert.match(t,/href="diario\.html"[^>]*aria-label="Diário"/,`${page} must route Diário to diario.html`);assert.ok(!t.includes('jornada.html#diario'),`${page} contains obsolete Diário route`);}
for(const page of fs.readdirSync(root).filter(x=>x.endsWith('.html'))){const t=fs.readFileSync(path.join(root,page),'utf8');for(const href of [...t.matchAll(/href="([^"#?]+\.html)(?:[?#][^"]*)?"/g)].map(m=>m[1]))assert.ok(fs.existsSync(path.join(root,href)),`${page} points to missing ${href}`);}
console.log('NAVIGATION_CONSISTENCY PASS');
