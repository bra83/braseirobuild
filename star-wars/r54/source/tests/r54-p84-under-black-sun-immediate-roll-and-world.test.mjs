import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {startUnderBlackSun,adjudicateUnderBlackSunCheck,underBlackSunStatus} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
const css=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.css','utf8');

test('P84: resultado imediato usa narrativeEffects da adjudicação e render dedicado',()=>{
 assert.match(runtime,/Array\.isArray\(a\.narrativeEffects\)/);
 assert.match(runtime,/final-published-last-roll-effects/);
 assert.match(css,/\.final-published-last-roll-effects/);
});

test('P84: projeção do último teste casa por checkId antes de mostrar efeitos',()=>{
 assert.match(runtime,/lastFeedback\?\.id===lastHistory\?\.checkId/);
});

test('P84: adjudicação pública continua sem flags ocultas',()=>{
 let {world}=startUnderBlackSun({});
 const r=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});
 const pub=underBlackSunStatus(r.world).checkFeedback.at(-1);
 assert.ok(pub.effects.some(x=>x.kind==='despair'));
 const raw=JSON.stringify(pub).toLowerCase();
 assert.equal(raw.includes('secret'),false);assert.equal(raw.includes('spoiler'),false);assert.equal(raw.includes('gmonly'),false);assert.equal(raw.includes('masteronly'),false);
});
