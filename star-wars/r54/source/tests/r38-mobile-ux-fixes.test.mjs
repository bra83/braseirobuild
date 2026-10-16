import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
const read=p=>fs.readFileSync(p,'utf8');
const runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const css=read('app/src/main/assets/vtt/r46/r46.css')+'\n'+read('app/src/main/assets/vtt/r46/final-runtime.css');
const controller=read('app/src/main/assets/vtt/js/core/controller.mjs');
const ui=read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
const bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
const shell=read('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
const main=read('app/src/main/java/com/braseiro/starwarsedge/MainActivity.java');
const manifest=read('app/src/main/AndroidManifest.xml');
const gemini=read('app/src/main/java/com/braseiro/starwarsedge/GeminiBridge.java');

test('R38: início do mundo não exige texto livre e oferece seleção visual e aleatória',()=>{
  assert.match(runtime,/const PLANET_START_CARDS=\[/);
  assert.match(runtime,/id="sandbox-planet-carousel"/);
  assert.match(runtime,/data-planet-point="\$\{i\}"/);
  assert.match(runtime,/data-random-planet/);
  assert.match(runtime,/initializeLivingSandbox/);
  assert.match(runtime,/startingPoint:point/);
  assert.doesNotMatch(runtime,/id="sandbox-start"[^>]*<input|id="sandbox-start"/);
});

test('R38: existe gate explícito INICIAR SANDBOX ligado ao motor',()=>{
  assert.match(runtime,/INICIAR SANDBOX/);
  assert.match(runtime,/bridgeResult\('startSandboxSession'/);
  assert.match(controller,/startSandboxSession\(/);
  assert.match(ui,/startSandboxSession\(/);
  assert.match(bundle,/startSandboxSession\(/);
});

test('R38: cena oferece geração Gemini diretamente e modelos configuráveis',()=>{
  assert.match(runtime,/data-r54-scene-illustration/);
  assert.match(runtime,/ILUSTRAÇÃO/);
  assert.match(runtime,/generateSceneImage/);
  assert.match(runtime,/SALVAR MODELOS/);
  assert.match(gemini,/configureModels\(/);
  assert.match(gemini,/DEFAULT_MASTER_MODEL/);
  assert.match(gemini,/DEFAULT_IMAGE_MODEL/);
  assert.match(gemini,/DEFAULT_TTS_MODEL/);
});

test('R38: Gemini TTS é funcional com Android TTS como fallback',()=>{
  assert.match(gemini,/speakTts\(/);
  assert.match(gemini,/response_format/);
  assert.match(gemini,/speech_config/);
  assert.match(runtime,/Gemini TTS/);
  assert.match(runtime,/Android TTS/);
});

test('R38: personagem vazio oferece geração completa aleatória e import JSON',()=>{
  assert.match(runtime,/GERAR FICHA ALEATÓRIA/);
  assert.match(runtime,/IMPORTAR \.JSON/);
  assert.match(runtime,/generateRandomCharacter/);
  assert.match(runtime,/importCharacterJson/);
  assert.match(shell,/Exportar JSON/);
});

test('R38: Biblioteca é descoberta por utilidade e atalho de Ajustes',()=>{
  assert.match(shell,/label: 'BIBLIOTECA'/);
  assert.match(runtime,/>BIBLIOTECA<\/button>/);
  assert.match(shell,/Biblioteca \/ PDFs/);
});

test('R38: Android respeita insets e teclado sem sobrepor UI',()=>{
  assert.match(main,/setOnApplyWindowInsetsListener/);
  assert.match(main,/get(?:SystemWindow|Stable)InsetTop/);
  assert.match(main,/get(?:SystemWindow|Stable)InsetBottom/);
  assert.match(main,/applySystemInsetsToRoot/);
  assert.match(manifest,/windowSoftInputMode="adjustResize"/);
  assert.match(css,/PERSONAGEM|font-size:clamp\(/);
});

test('R38: launcher profissional aprovado está configurado',()=>{
  assert.match(manifest,/android:icon="@mipmap\/ic_launcher"/);
  assert.match(manifest,/android:roundIcon="@mipmap\/ic_launcher_round"/);
  for(const d of ['mdpi','hdpi','xhdpi','xxhdpi','xxxhdpi']) assert.ok(fs.existsSync(`app/src/main/res/mipmap-${d}/ic_launcher.png`));
});

import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { LocalStorageRepository } from '../app/src/main/assets/vtt/js/core/storage.mjs';
class MemoryStorageR38 { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }

test('R38: startSandboxSession produz narração e ações sem exigir prompt do jogador',()=>{
  const g=new GameController(new LocalStorageRepository(new MemoryStorageR38(),'r38-start'));
  g.loadStarterCharacter('zal');
  g.initializeLivingSandbox({startingPoint:'Mos Eisley · Tatooine',regionLabel:'Tatooine'});
  const result=g.startSandboxSession({rng:()=>0.99});
  assert.equal(g.state.session.status,'ativa');
  assert.ok(String(g.state.scene.narration||'').length>30);
  assert.ok(Array.isArray(g.state.master.suggestedActions));
  assert.ok(g.state.master.suggestedActions.length>=3);
  assert.match(g.state.scene.narration,/Nada obriga você|campanha começa/i);
  assert.ok(result.opening.ready);
});

test('R38: chave de imagem da cena é compatível entre shell, botão direto e host Android',()=>{
  assert.match(shell,/stableGeminiSceneKey/);
  assert.match(shell,/scene_\$\{\(h >>> 0\)\.toString\(16\)\.padStart\(8, '0'\)\}/);
  assert.match(shell,/stableGeminiSceneKey\(scene\.id \?\? scene\.sceneId \?\? title\)/);
  assert.match(runtime,/currentSceneKey\(\)/);
  assert.match(runtime,/sceneKeyFor\(scene\?\.id\?\?scene\?\.sceneId\?\?scene\?\.title\?\?'Cena'\)/);
  assert.match(gemini,/scene_\[0-9a-f\]\{8\}/);
});
