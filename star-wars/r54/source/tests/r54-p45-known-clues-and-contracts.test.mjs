import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import {startUnderBlackSun,advanceUnderBlackSun,adjudicateUnderBlackSunCheck,underBlackSunStatus,currentUnderBlackSunSystems} from '../app/src/main/assets/vtt/js/adventure/underBlackSun.mjs';
const base=()=>({clock:{tick:0},locations:{},npcs:{},factions:{}});
const step=(w,id)=>advanceUnderBlackSun(w,id).world;
const hub=()=>{let w=startUnderBlackSun(base()).world;w=step(w,'escape-clean');w=step(w,'begin-investigation');return w};

test('P45 pistas começam vazias e não antecipam investigação futura',()=>{const s=underBlackSunStatus(startUnderBlackSun(base()).world);assert.deepEqual(s.knownClues,[]);assert.doesNotMatch(JSON.stringify(s.knownClues),/Sector 943|NovaCORE|Spyder|Umbra/i)});
test('P45 hub expõe somente alvo já conhecido',()=>{const s=underBlackSunStatus(hub());assert.ok(s.knownClues.some(c=>c.id==='target-kaato'));assert.doesNotMatch(JSON.stringify(s.knownClues),/Sector 943|NovaCORE|retorno|kaatoAlerted/i)});
test('P45 pista Umbra persiste ao voltar ao hub sem vazar outras rotas',()=>{let w=hub();w=step(w,'go-umbra');w=step(w,'umbra-talk');w=step(w,'umbra-social-clean');const s=underBlackSunStatus(w);assert.ok(s.knownClues.some(c=>c.id==='umbra-trail'));assert.doesNotMatch(JSON.stringify(s.knownClues),/sector-943|novacore-convergence|kaato-return/i)});
test('P45 Zelcomm libera Sector 943 apenas após a rota correspondente',()=>{let w=hub();assert.equal(underBlackSunStatus(w).knownClues.some(c=>c.id==='sector-943'),false);w=step(w,'go-zelcomm');w=step(w,'zelcomm-clean');assert.equal(underBlackSunStatus(w).knownClues.some(c=>c.id==='sector-943'),true)});
test('P45 convergência NovaCORE só aparece quando o endgame foi alcançado',()=>{let w=hub();w=step(w,'go-umbra');w=step(w,'umbra-return');w=step(w,'go-spyder');w=step(w,'spyder-return');assert.equal(underBlackSunStatus(w).knownClues.some(c=>c.id==='novacore-convergence'),false);w=step(w,'move-endgame');assert.equal(underBlackSunStatus(w).knownClues.some(c=>c.id==='novacore-convergence'),true)});
test('P45 rota spyder-sabacc usa subsistema real e o exibe na cena',()=>{let w=hub();w=step(w,'go-spyder');w=step(w,'spyder-table');assert.ok(currentUnderBlackSunSystems(w).some(x=>x.id==='sabacc'));const before=w.publishedAdventure.systems.sabacc.rounds;const r=adjudicateUnderBlackSunCheck(w,{checkIndex:0,result:{success:true,netAdvantage:1}});assert.equal(r.world.publishedAdventure.systems.sabacc.rounds,before+1);assert.notEqual(r.world.publishedAdventure.systems.sabacc.creditsDelta,0)});
test('P45 UI possui superfície explícita de pistas conhecidas',()=>{const runtime=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.js','utf8');const css=fs.readFileSync('app/src/main/assets/vtt/r46/final-runtime.css','utf8');assert.match(runtime,/knownClues/);assert.match(runtime,/PISTAS CONHECIDAS/);assert.match(css,/final-published-clues/)});
