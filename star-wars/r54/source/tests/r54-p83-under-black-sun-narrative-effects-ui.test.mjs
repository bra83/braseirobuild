import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {startUnderBlackSun,adjudicateUnderBlackSunCheck,underBlackSunStatus} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';

const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');
const css=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.css','utf8');

test('P83: feedback público expõe efeitos narrativos concretos sem estado oculto',()=>{
 let {world}=startUnderBlackSun({});
 ({world}=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:true,netSuccess:1,netAdvantage:2,triumph:1,despair:0}}));
 const status=underBlackSunStatus(world),fb=status.checkFeedback.at(-1);
 assert.ok(fb.effects.length>=2);
 assert.ok(fb.effects.some(x=>x.kind==='triumph'));
 assert.ok(fb.effects.some(x=>/rota de fuga segura/i.test(x.summary)));
 assert.equal(JSON.stringify(fb).includes('secret'),false);
 assert.equal(JSON.stringify(fb).includes('spoiler'),false);
});

test('P83: Desespero de perseguição fica visível e custo mecânico continua persistente',()=>{
 let {world}=startUnderBlackSun({});
 const resolved=adjudicateUnderBlackSunCheck(world,{checkIndex:0,result:{success:false,netSuccess:-1,netAdvantage:-2,triumph:0,despair:1}});world=resolved.world;
 const fb=underBlackSunStatus(world).checkFeedback.at(-1);
 assert.ok(fb.effects.some(x=>x.kind==='despair'));
 assert.ok(fb.effects.some(x=>/speeder sofre dano sério/i.test(x.summary)));
 assert.equal(resolved.adjudication.playerStrainDelta,1);
 assert.equal(world.publishedAdventure.flags.speederDamaged,true);
});

test('P83: runtime renderiza consequências por tipo em bloco compacto',()=>{
 assert.match(runtime,/final-published-narrative-effects/);
 assert.match(runtime,/CONSEQUÊNCIA DO TESTE/);
 assert.match(runtime,/TRIUNFO/);
 assert.match(runtime,/DESESPERO/);
 assert.match(css,/\.final-published-narrative-effects/);
});
