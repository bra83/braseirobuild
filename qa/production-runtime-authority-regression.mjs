import fs from 'node:fs';import path from 'node:path';import assert from 'node:assert/strict';
const root=path.resolve(new URL('../app/src/main/assets/www/',import.meta.url).pathname);
const production=['jornada.js','diario.js','mochila.js','ficha.js','scr014.js','scr022.js'];
for(const file of production){const text=fs.readFileSync(path.join(root,file),'utf8');assert.ok(!/wave\d+[^'\"\n]*fixture\.json/i.test(text),`${file} still depends on Wave fixture data`);assert.ok(!/scr0(?:15|16)\.fixture\.json/i.test(text),`${file} depends on legacy encounter fixture`)}
const starter=JSON.parse(fs.readFileSync(path.join(root,'data/wwn.starter-character.v1.json'),'utf8'));assert.ok(starter.characterId||starter.id);assert.ok(starter.name);assert.equal(starter.fixtureKind,undefined);assert.equal(starter.authority,undefined);assert.equal(starter.evidencePolicy,undefined);
const journey=fs.readFileSync(path.join(root,'jornada.js'),'utf8');const sheet=fs.readFileSync(path.join(root,'ficha.js'),'utf8');assert.ok(journey.includes('wwn.starter-character.v1.json'));assert.ok(sheet.includes('wwn.starter-character.v1.json'));
console.log('production runtime authority regression: PASS');
