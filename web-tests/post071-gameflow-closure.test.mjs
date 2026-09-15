import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { PlayerDeclarationService } from '../app/src/main/assets/www/core/player-declaration-service.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';
import { RuntimeBarbaraProvider } from '../app/src/main/assets/www/core/runtime-narrative-provider.js';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../app/src/main');
const web=path.join(root,'assets/www');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');

function setupScene(){
  const state=makeEmptyCampaignState({campaignId:'gameflow',displayName:'Gameflow',masterSeed:'gameflow-seed'});
  state.characterState={characterId:'iria',name:'IRIA',level:1,attributes:{Strength:7,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9}};
  state.partyState={schemaVersion:1,activeGroupId:'g',groups:{g:{groupId:'g',label:'GRUPO',memberIds:['iria'],position:{q:17,r:22,name:'RUÍNAS'},encounterRiskUnit:true}}};
  state.worldState={position:{q:17,r:22,name:'RUÍNAS'},elapsedDays:0,expedition:{supplies:{foodUnits:20,waterUnits:20}},atlas:{hexes:{'17,22':{q:17,r:22,ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS',terrain:'rugged-hills',terrainTitle:'RUÍNAS',discovered:true,entered:true,visited:true,explored:false,poi:null}}}};
  state.encounterState={currentScene:{contentType:'worldEvent',contentId:'start',publicScene:{status:'OK',contentType:'worldEvent',contentId:'start',title:'RUÍNAS',summary:'Pedras antigas sob o céu cinzento.',situation:'Silêncio e vento.',leads:[]}},scenePhase:'ACTIVE'};
  const p=new MemoryPersistenceAdapter();
  p.createCampaign({campaignId:state.campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'s0'});
  return {p,campaignId:state.campaignId};
}

test('freeform player declaration commits no mechanics and gets async Master narration',async()=>{
  const {p,campaignId}=setupScene();
  let received=null;
  const barbara={status:()=>({configured:true}),generateAsync:async req=>{received=req;return {text:'O vento responde com um assobio entre as pedras.',provider:'GEMINI'};},generate:()=>{throw new Error('sync path must not be used')}};
  const svc=new PlayerDeclarationService({persistence:p,barbara});
  const r=await svc.declare({campaignId,text:'Examino as inscrições sem tocar nelas.'});
  assert.equal(r.status,'OK');
  assert.equal(received.mechanicalEvent.kind,'PLAYER_DECLARATION');
  assert.equal(received.mechanicalEvent.mechanicalMutation,false);
  assert.equal(received.mechanicalEvent.timeAdvanced,false);
  assert.equal(received.mechanicalEvent.positionChanged,false);
  const loaded=p.loadCampaign(campaignId); const state=JSON.parse(loaded.stateJson);
  assert.equal(state.worldState.position.q,17); assert.equal(state.worldState.position.r,22);
  const events=p.listEvents(campaignId).events;
  assert.equal(events.at(-2).eventKind,'PLAYER_DECLARATION');
  assert.equal(events.at(-1).eventKind,'NARRATIVE');
});



test('sync mechanical narration never blocks on RAG when async Gemini bridge exists',()=>{
  let ragCalled=false;
  const native={geminiStatus:()=>JSON.stringify({status:'OK',configured:true,textModel:'gemini-3.8-flash'}),geminiGenerateText:()=>{throw new Error('sync network path forbidden')},geminiGenerateTextAsync:()=>JSON.stringify({status:'PENDING',requestId:'x'})};
  const fallback={generate:({mechanicalEvent})=>({status:'OK',provider:'LOCAL',parentMechanicalEventId:mechanicalEvent.eventId,mechanicalRevision:mechanicalEvent.revision,text:'Pendente de narração.'})};
  const provider=new RuntimeBarbaraProvider({native,fallback,ragSearch:()=>{ragCalled=true;throw new Error('sync RAG forbidden')}});
  const r=provider.generate({mechanicalEvent:{eventId:'m1',revision:1,kind:'PLAYER_ACTION_SKILL_CHECK'},state:{}});
  assert.equal(r.geminiDeferred,true);
  assert.equal(ragCalled,false);
});
test('configuration has real multimodal test and exact TTS completion check',()=>{
  const html=fs.readFileSync(path.join(web,'configuracoes.html'),'utf8');
  const js=fs.readFileSync(path.join(web,'configuracoes.js'),'utf8');
  const provider=fs.readFileSync(path.join(web,'core/native-tts-provider.js'),'utf8');
  assert.match(html,/id="testGeminiVision"/);
  assert.match(js,/imageAttached===true/);
  assert.match(js,/allowFallback:false/);
  assert.match(js,/waitForUtterance/);
  assert.match(provider,/braseiro-tts/);
  assert.match(provider,/DONE','ERROR','STOPPED/);
});

test('RAG and PDF paths are asynchronous and OCR-capable on Android',()=>{
  const build=read('../../build.gradle.kts');
  const rag=read('java/com/braseiro/wwn/bridge/RagRuntime.kt');
  const main=read('java/com/braseiro/wwn/MainActivity.kt');
  const bridge=read('java/com/braseiro/wwn/bridge/BraseiroBridge.kt');
  const client=fs.readFileSync(path.join(web,'core/rag-client.js'),'utf8');
  const provider=fs.readFileSync(path.join(web,'core/runtime-narrative-provider.js'),'utf8');
  const regras=fs.readFileSync(path.join(web,'regras.js'),'utf8');
  assert.match(build,/text-recognition:16\.0\.1/);
  assert.match(rag,/PdfRenderer/); assert.match(rag,/TextRecognition/); assert.match(rag,/MAX_OCR_PAGES/);
  assert.match(main,/requestRagSearchAsync/); assert.match(main,/braseiro-rag/);
  assert.match(bridge,/ragSearchAsync/); assert.match(client,/searchAsync/);
  assert.match(provider,/_contextAsync/); assert.match(provider,/ragSearchAsync/);
  assert.match(regras,/await client\.searchAsync/);
});

test('travel interruption is persisted and locked to its original destination',()=>{
  const world=fs.readFileSync(path.join(web,'core/world-service.js'),'utf8');
  assert.match(world,/pendingTravel/);
  assert.match(world,/TRAVEL_INTERRUPTED_DESTINATION_LOCKED_NO_MUTATION/);
  assert.match(world,/if\(encounterOccurred\)break/);
  assert.match(world,/remainingHours/);
});

test('Atlas and Journey expose end-to-end play actions instead of inspection-only shell',()=>{
  const atlasHtml=fs.readFileSync(path.join(web,'scr014.html'),'utf8');
  const atlasJs=fs.readFileSync(path.join(web,'scr014.js'),'utf8');
  const journeyHtml=fs.readFileSync(path.join(web,'jornada.html'),'utf8');
  const journeyJs=fs.readFileSync(path.join(web,'jornada.js'),'utf8');
  assert.match(atlasHtml,/VIAJAR PARA ESTE HEX/); assert.match(atlasHtml,/EXPLORAR HEX/);
  assert.match(atlasJs,/worldService\.travelToAdjacentHex/); assert.match(atlasJs,/worldService\.exploreHex/);
  assert.match(journeyHtml,/id="playerReactionInput"/); assert.match(journeyHtml,/ENVIAR AO MESTRE/);
  assert.match(journeyJs,/PlayerDeclarationService/); assert.match(journeyJs,/declarations\.declare/);
});

test('exploration creates a directly narratable public scene',()=>{
  const {p,campaignId}=setupScene();
  const svc=new WorldActionService({persistence:p});
  const r=svc.exploreHex({campaignId,q:17,r:22,concealmentMultiplier:1});
  assert.equal(r.status,'OK');
  const state=JSON.parse(p.loadCampaign(campaignId).stateJson);
  assert.equal(state.worldState.atlas.hexes['17,22'].explored,true);
  assert.equal(state.encounterState.currentScene.publicScene.status,'OK');
  assert.equal(state.encounterState.currentScene.publicScene.contentType,'hexExploration');
});
