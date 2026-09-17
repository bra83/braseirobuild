import fs from 'node:fs';
import assert from 'node:assert/strict';

const read=p=>fs.readFileSync(p,'utf8');
const checks=[];
function check(name,fn){try{fn();checks.push([name,'PASS'])}catch(error){checks.push([name,'FAIL',error.message]);throw error}}

const journeyHtml=read('app/src/main/assets/www/jornada.html');
const journeyJs=read('app/src/main/assets/www/jornada.js');
const atlas=read('app/src/main/assets/www/scr014.js');
const atlasCss=read('app/src/main/assets/www/scr014.css');
const ux=read('app/src/main/assets/www/ux-hotfix.js');
const cfg=read('app/src/main/assets/www/configuracoes.js');
const manager=read('app/src/main/assets/www/core/campaign-manager-service.js');
const starter=read('app/src/main/assets/www/core/starter-bootstrap.js');
const opening=read('app/src/main/assets/www/core/campaign-opening.js');
const narrative=read('app/src/main/assets/www/core/runtime-narrative-provider.js');
const imageState=read('app/src/main/assets/www/core/scene-image-state-service.js');
const main=read('app/src/main/java/com/braseiro/wwn/MainActivity.kt');
const gradle=read('app/build.gradle.kts');

check('campanha nova nao obriga personagem',()=>{
  assert.match(manager,/requiresCharacter:false/);
  assert.match(cfg,/criar um personagem quando quiser pela aba PERSONAGEM/);
  assert.match(cfg,/location\.href='jornada\.html'/);
  assert.doesNotMatch(cfg,/createCampaign[\s\S]{0,1600}location\.href='criacao\.html'/);
  assert.match(journeyJs,/SEM PERSONAGEM/);
});
check('abertura de campanha e cena canonica persistida',()=>{
  assert.match(opening,/contentType:'campaignOpening'/);
  assert.match(opening,/narrativeScale:'MAJOR_OPENING'/);
  assert.match(manager,/prepareCampaignOpening/);
  assert.match(manager,/eventKind:'SCENE_OPENED'/);
  assert.match(starter,/prepareCampaignOpening/);
  assert.match(starter,/eventKind:'SCENE_OPENED'/);
});
check('mestre trata abertura e cenas especiais como narracao grande',()=>{
  assert.match(narrative,/NÃO seja concisa quando a cena for uma abertura, chegada, descoberta, exploração ou encontro especial/);
  assert.match(narrative,/900–1400 palavras/);
  assert.match(narrative,/mechanicalEvent\?\.kind==='SCENE_OPENED'/);
  assert.match(narrative,/majorLocalFallback/);
});
check('ganchos sao oportunidades opcionais, dinamicas e sandbox livre',()=>{
  assert.match(journeyHtml,/RUMORES E OPORTUNIDADES/);
  assert.match(journeyHtml,/opcionais • sandbox livre/);
  assert.match(journeyHtml,/Você não precisa escolher nenhuma destas oportunidades/);
  assert.match(journeyJs,/OPORTUNIDADE OPCIONAL/);
  assert.match(journeyJs,/selectJourneyFeed/);
  assert.match(journeyJs,/knownJourneyLocations/);
});
check('operacoes do mestre nao vazam na jornada',()=>{
  assert.doesNotMatch(journeyHtml,/OPERAÇÕES DO MESTRE/);
  assert.match(ux,/a\[href="gestao\.html"\]/);
});
check('imagem importada e compartilhada chega e persiste por campanha',()=>{
  assert.match(journeyHtml,/sceneImportedImage/);
  assert.match(journeyHtml,/sceneRemoveImageButton/);
  assert.match(journeyJs,/Imagem importada e vinculada à cena atual/);
  assert.match(journeyJs,/storePendingSceneImageForCampaign/);
  assert.match(journeyJs,/pendingSceneImageFromState/);
  assert.match(imageState,/pendingSceneImage/);
  assert.match(imageState,/SCENE_IMAGE_ATTACHED/);
  assert.match(imageState,/SCENE_IMAGE_REMOVED/);
  assert.match(ux,/replayPendingSceneImage/);
  assert.match(main,/Intent\.ACTION_SEND/);
  assert.match(main,/Intent\.ACTION_SEND_MULTIPLE/);
  assert.match(main,/sceneImageRuntime\.importImage/);
  assert.match(main,/LINK_ONLY/);
});
check('atlas nao move por toque nem camera e so viaja um hex adjacente',()=>{
  assert.match(atlas,/h\.addEventListener\('click'[\s\S]*selectedHexKey=k/);
  assert.match(atlas,/distance===1/);
  assert.match(atlas,/worldService\.travelToAdjacentHex/);
  assert.match(atlas,/TRAVEL_INTERRUPTED_DESTINATION_LOCKED_NO_MUTATION/);
  assert.match(atlas,/map\.addEventListener\('pointermove'[\s\S]*pan\.x\+=dx/);
});
check('poi aparece horizontal e sem corte artificial',()=>{
  assert.doesNotMatch(atlas,/slice\(0,18\)/);
  assert.match(atlasCss,/WWN_REAL_FLOW_POI_HORIZONTAL_V1/);
  assert.match(atlasCss,/\.poi-symbol>i\{[^}]*transform:none/);
});
check('viagem e exploracao abrem narrativa antes de novo fluxo',()=>{
  assert.match(atlas,/await narrateWorldResult\(result\)/);
  assert.match(atlas,/location\.href='jornada\.html'/);
  assert.match(ux,/lockWorldFlow/);
});
check('personagem oferece criar e gerar aleatorio',()=>{
  assert.match(ux,/CRIAR PERSONAGEM/);
  assert.match(ux,/GERAR ALEATÓRIO/);
  assert.match(ux,/location\.href='criacao\.html'/);
  assert.match(ux,/data-generate/);
});
check('criacao e pericias visiveis em pt-br',()=>{
  assert.match(ux,/Administer:'ADMINISTRAR'/);
  assert.match(ux,/Connect:'CONECTAR'/);
  assert.match(ux,/Convince:'CONVENCER'/);
  assert.match(ux,/Notice:'PERCEPÇÃO'/);
  assert.match(ux,/Survive:'SOBREVIVÊNCIA'/);
  assert.match(ux,/patchCreation/);
  assert.match(journeyJs,/skillLabelPtBr\(k\)/);
});
check('barras explicadas com valores',()=>{
  assert.match(journeyHtml,/PV/);
  assert.match(journeyHtml,/TENSÃO DO SISTEMA/);
  assert.match(journeyHtml,/id="hpText"/);
  assert.match(journeyHtml,/id="strainText"/);
});
check('icone da tocha nunca cai em garrafa',()=>{
  const torch=ux.indexOf("return'torch'");
  const liquid=ux.indexOf("return'liquid'");
  assert.ok(torch>=0&&liquid>torch,'classificacao TOCHA deve preceder liquidos');
  assert.match(ux,/torch:'<path/);
  assert.match(ux,/FRASCO\|GARRAFA\|CANTIL/);
  assert.match(ux,/patchBackpack/);
});
check('versao corrente nao foi regredida',()=>{
  assert.match(gradle,/versionCode = 110/);
  assert.match(gradle,/versionName = "1\.0\.10"/);
});

console.log(`WWN_DEVICE_AUDIT_GATE_PASS=${checks.length}`);
for(const row of checks)console.log(row.join(' | '));
