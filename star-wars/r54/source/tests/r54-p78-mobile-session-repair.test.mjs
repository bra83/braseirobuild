import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';

const read=p=>readFileSync(new URL('../'+p,import.meta.url),'utf8');
const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const css=read('app/src/main/assets/vtt/r46/final-runtime.css');
class MemoryStorage{constructor(){this.m=new Map()}getItem(k){return this.m.has(k)?this.m.get(k):null}setItem(k,v){this.m.set(k,String(v))}removeItem(k){this.m.delete(k)}}

test('P78: início de jogo depende de estado explícito, nunca de texto de narração',()=>{
  const a=runtime.indexOf('function adventureStarted(){');
  const b=runtime.indexOf('function masterNarrationAvailable(){',a);
  const block=runtime.slice(a,b);
  assert.match(block,/session\?\.status==='ativa'/);
  assert.doesNotMatch(block,/scene\?\.narration|master\?\.narration|\.trim\(\)/);
});

test('P78: Novo mundo força seleção em SCREEN 00 e o overlay consome campaignSetup uma vez',()=>{
  assert.match(runtime,/searchParams\.set\('screen','00'\)/);
  assert.match(runtime,/searchParams\.set\('campaignSetup','1'\)/);
  assert.match(runtime,/location\.replace\(target\.toString\(\)\)/);
  assert.match(runtime,/forceSetup=url\.searchParams\.get\('campaignSetup'\)==='1'/);
  assert.match(runtime,/history\.replaceState\(\{\},'',url\)/);
  assert.match(runtime,/\(!forceSetup&&sandboxReady\(\)\)/);
});

test('P78: reset preserva personagem, grupo, veículos e modo mas limpa sessão/mundo da campanha anterior',()=>{
  const storage=new MemoryStorage();
  const game=new GameController(new LocalStorageRepository(storage,'p78-reset'));
  game.loadStarterCharacter('cael');
  game.setCampaignMode('MIXED');
  game.initializeLivingSandbox({startingPoint:'Coruscant',regionLabel:'Coruscant'});
  game.startSandboxSession({rng:()=>0.25});
  assert.equal(game.state.session.status,'ativa');
  assert.equal(game.state.world.sandbox.initialized,true);
  const characterName=game.state.player.character.name;
  const groupBefore=JSON.stringify(game.state.group);
  const result=game.restartSandboxPreservingParty();
  assert.equal(result.preserved.character,true);
  assert.equal(game.state.player.character.name,characterName);
  assert.equal(game.state.campaignMode,'MIXED');
  assert.equal(JSON.stringify(game.state.group),groupBefore);
  assert.equal(game.state.session.status,'nao_iniciada');
  assert.equal(Boolean(game.state.world.sandbox?.initialized),false);
  assert.equal(game.state.world.publishedAdventure??null,null);
});

test('P78: testes publicados mantêm resultado visível e animação não controla a mecânica',()=>{
  assert.match(runtime,/const lastHistory=\(status\?\.progress\?\.checkHistory\|\|\[\]\)\.slice\(-1\)\[0\]\|\|null/);
  assert.match(runtime,/final-published-last-roll/);
  assert.match(runtime,/out\.textContent=`\$\{String\(spec\.skill\|\|'Teste'\)\} · \$\{summary\}`/);
  assert.match(runtime,/boundedVisualRoll\(x,/);
  assert.doesNotMatch(runtime,/setTimeout\(\(\)=>enhancePublishedAdventurePanel\(\),650\)/);
});

test('P78: teste sugerido mostra perícia, dificuldade e resultado antes de pedir consequência ao Mestre',()=>{
  const a=runtime.indexOf('async function resolveSuggestedCheckWithDice');
  const b=runtime.indexOf('function enhanceSceneFromEngine',a);
  const block=runtime.slice(a,b);
  const resultPos=block.indexOf('out.textContent=`${skill} · ${summary}`');
  const visualPos=block.indexOf('boundedVisualRoll');
  const masterPos=block.indexOf("bridgeAsync('submitPlayerIntent'");
  assert.ok(resultPos>=0 && visualPos>resultPos && masterPos>visualPos);
});

test('P78: Mestre local não é rotulado como offline e Gemini configurado é reconhecido antes da primeira ação',()=>{
  assert.match(runtime,/geminiStatus\(\)\?\.configured/);
  assert.match(runtime,/return \['MESTRE LOCAL','is-local'\]/);
  assert.doesNotMatch(runtime,/return \['MESTRE OFFLINE','is-offline'\]/);
  assert.match(runtime,/requestPublishedMasterNarration\('abertura de Sob um Sol Negro'\)/);
  assert.match(runtime,/8 a 14 parágrafos, normalmente 900 a 1400 palavras/);
  assert.match(runtime,/preserve anti-spoiler/);
});

test('P78: alvos touch e textos críticos ficam legíveis em 415x915',()=>{
  assert.match(css,/\.final-suggested-check button\{min-width:92px;min-height:48px/);
  assert.match(css,/\.final-suggested-check small\{font-size:\.76rem/);
  assert.match(css,/\.scene-narrative-paper:not\(\.scene-narrative-reader\) \.scene-paper-copy\{font-size:1\.08rem!important;line-height:1\.52!important\}/);
  assert.match(css,/\.final-start-map button\[data-point\]\{width:44px!important;height:44px!important/);
  assert.match(css,/\.final-published-checks li>button\{min-width:92px;min-height:48px/);
  assert.match(css,/\.final-published-last-roll/);
});

test('P78: Android/shell/runtime permanecem em P78 ou posterior',()=>{
  const gradle=read('app/build.gradle.kts');
  const shell=read('app/src/main/assets/vtt/r46-shell.html');
  const bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
  const code=Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]||0);
  const name=gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1]||'';
  assert.ok(code>=10078);
  assert.match(name,/^1\.0\.(?:7[8-9]|[89]\d|\d{3,})$/);
  assert.match(shell,/data-version="1\.0\.(?:7[8-9]|[89]\d|\d{3,})"/);
  assert.match(runtime,/R54 P(?:78|79|[89]\d|\d{3,}) · Android 1\.0\.(?:7[8-9]|[89]\d|\d{3,})/);
  assert.match(bundle,/final:'1\.0\.(?:7[8-9]|[89]\d|\d{3,})-r54-p(?:78|79|[89]\d|\d{3,})'/);
});
