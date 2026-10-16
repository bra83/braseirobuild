import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile, stat} from 'node:fs/promises';
import {fileURLToPath} from 'node:url';
import path from 'node:path';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {BraseiroUiBridge} from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
import {LocalStorageRepository} from '../app/src/main/assets/vtt/js/core/storage.mjs';
import {IMAGE_LIBRARY_MANIFEST} from '../app/src/main/assets/vtt/js/library/imageLibraryData.mjs';
import {R54_LOCATION_MAP_ASSETS} from '../app/src/main/assets/vtt/js/library/r54LocationMapData.mjs';

class MemoryStorage { constructor(){this.m=new Map()} getItem(k){return this.m.has(k)?this.m.get(k):null} setItem(k,v){this.m.set(k,String(v))} removeItem(k){this.m.delete(k)} }
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');

async function text(rel){return readFile(path.join(root,rel),'utf8')}

test('P21 reiniciar sandbox preserva personagem, grupo, nave e ajustes, mas limpa mundo/sessão',()=>{
  const storage=new MemoryStorage();
  const game=new GameController(new LocalStorageRepository(storage,'restart-p21'));
  game.loadStarterCharacter('zal');
  game.updateGroupBase({name:'Eco',location:'Dantooine',description:'Base do grupo'});
  game.updateGroupShip({name:'Andarilha'});
  game.configureTts({enabled:true,rate:1.1});
  game.initializeLivingSandbox({startingPoint:'Porto de Fronteira'});
  const previousCharacter=structuredClone(game.state.player.character);
  const result=game.restartSandboxPreservingParty();
  assert.equal(result.preserved.character,true);
  assert.deepEqual(game.state.player.character,previousCharacter);
  assert.equal(game.state.group.base.name,'Eco');
  assert.equal(game.state.group.ship.name,'Andarilha');
  assert.equal(game.state.settings.ttsEnabled,true);
  assert.equal(game.state.world.sandbox?.initialized ?? false,false);
  assert.equal(game.state.session.status,'nao_iniciada');
  const reopened=new GameController(new LocalStorageRepository(storage,'restart-p21'));
  assert.equal(reopened.state.player.character.name,'Zal');
  assert.equal(reopened.state.group.ship.name,'Andarilha');
});

test('P21 botão REINICIAR usa reset seguro e não apaga estado/personagem/foto diretamente',async()=>{
  const src=await text('app/src/main/assets/vtt/r46/final-runtime.js');
  const fn=src.slice(src.indexOf('function r53RestartAdventure'),src.indexOf('function r53EnsureRestart'));
  assert.match(fn,/restartSandboxPreservingParty/);
  assert.doesNotMatch(fn,/removeItem\('braseiro\.starwars\.edge\.state\.v1'/);
  assert.doesNotMatch(fn,/removeItem\('braseiro\.starwars\.character\.photo\.v1'/);
});

test('P21 status Gemini nativo é adaptado para Result ok/data esperado pela shell',async()=>{
  const src=await text('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
  const start=src.indexOf('function getGeminiStatusThroughBridge()');
  const fn=src.slice(start,start+650);
  assert.match(fn,/\{ ok: true, data \}/);
  assert.match(fn,/Status Gemini inválido/);
});

test('P21 TTS cai para Web Speech quando o bridge Android existe mas responde indisponível',async()=>{
  const src=await text('app/src/main/assets/vtt/r46/final-runtime.js');
  const fn=src.slice(src.indexOf('function speak(text)'),src.indexOf('function stop()'));
  assert.match(fn,/nativeResult&&nativeResult\.ok/);
  assert.match(fn,/speechSynthesis/);
  assert.ok(fn.indexOf('nativeResult&&nativeResult.ok') < fn.indexOf("'speechSynthesis' in window"));
});

test('P21 seletor oferece 82 mapas locais offline válidos sem adulterar o manifesto V3.8 congelado',async()=>{
  const frozenMaps=IMAGE_LIBRARY_MANIFEST.assets.filter(a=>a.category==='map');
  assert.equal(frozenMaps.length,18);
  const locationMaps=R54_LOCATION_MAP_ASSETS;
  assert.equal(locationMaps.length,82);
  assert.equal(locationMaps.filter(a=>a.mapKind==='local_map').length,1);
  assert.equal(locationMaps.filter(a=>a.mapKind==='generated_play_map').length,81);
  for(const asset of locationMaps){
    assert.notEqual(asset.sourceType,'external_reference');
    assert.equal(asset.offlineReady,true);
    const full=path.join(root,'app/src/main/assets/vtt',asset.localPath);
    const info=await stat(full);
    assert.ok(info.isFile() && info.size>1000,asset.assetId);
  }
});

test('P21 bridge mantém catálogo congelado por padrão e entrega os 82 mapas locais ao seletor P21',()=>{
  const bridge=new BraseiroUiBridge({repository:{load:()=>null}});
  const frozen=bridge.imageMapCatalog({limit:500});
  assert.equal(frozen.ok,true);
  assert.equal(frozen.data.length,18);
  const expanded=bridge.imageMapCatalog({limit:500,includePlayableLocations:true});
  assert.equal(expanded.ok,true);
  assert.equal(expanded.data.filter(a=>String(a.assetId).startsWith('local.r54.location-map.')).length,82);
  assert.equal(expanded.data.length,100);
});

test('P21 fonte e bundle da shell solicitam mapas jogáveis e mantêm auto-cache de externos',async()=>{
  const source=await text('shell-r46-run87/VTTApp.tsx');
  const template=await text('shell-r46-run87/Templates.tsx');
  assert.match(source,/includePlayableLocations:\s*true/);
  assert.match(source,/cacheExternalImage\(asset\)/);
  assert.match(template,/Biblioteca de mapas · \$\{props\.imageMaps\.length\}/);
});

test('P21 Android TTS aceita pt-BR e recua para português genérico antes de declarar indisponível',async()=>{
  const java=await text('app/src/main/java/com/braseiro/starwarsedge/TtsBridge.java');
  assert.match(java,/Locale\.forLanguageTag\("pt-BR"\)/);
  assert.match(java,/Locale\.forLanguageTag\("pt"\)/);
  assert.match(java,/activeLanguageTag/);
  assert.match(java,/Voz em português não instalada/);
});

test('P21 Android não tenta exibir mapa externo remoto antes do cache e seleção dispara cache',async()=>{
  const src=await text('app/src/main/assets/vtt/r46/r46-shell.bundle.js');
  const urlBlock=src.slice(src.indexOf('const selectedImageUrl'),src.indexOf('(0, react_1.useEffect)',src.indexOf('const selectedImageUrl')));
  assert.doesNotMatch(urlBlock,/return selectedImageAsset\.remoteUrl/);
  const select=src.slice(src.indexOf('const selectImageMap'),src.indexOf('const cacheSelectedImageMap'));
  assert.match(select,/cacheExternalImage/);
  assert.match(select,/Baixando mapa para uso offline/);
});
