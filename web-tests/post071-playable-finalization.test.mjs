import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { makeEmptyCampaignState, serializeCampaignState } from '../app/src/main/assets/www/core/campaign-state.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../app/src/main');
const web=path.join(root,'assets/www');
const read=rel=>fs.readFileSync(path.join(root,rel),'utf8');

function setup(){
  const state=makeEmptyCampaignState({campaignId:'playable-final',displayName:'Playable',masterSeed:'playable-seed'});
  state.characterState={characterId:'iria',name:'IRIA',level:1,attributes:{Strength:7,Dexterity:12,Constitution:11,Intelligence:10,Wisdom:14,Charisma:9}};
  state.partyState={schemaVersion:1,activeGroupId:'g',groups:{g:{groupId:'g',label:'GRUPO',memberIds:['iria'],position:{q:17,r:22,name:'RUÍNAS DA VIGÍLIA'},encounterRiskUnit:true}}};
  state.worldState={position:{q:17,r:22,name:'RUÍNAS DA VIGÍLIA'},elapsedDays:0,expedition:{supplies:{foodUnits:10,waterUnits:10}},atlas:{hexes:{'17,22':{q:17,r:22,ruleTerrain:'DENSE_FOREST_OR_RUGGED_HILLS',terrain:'rugged-hills',terrainTitle:'RUÍNAS ROCHOSAS',terrainLabel:'COLINAS / RUÍNAS',discovered:true,entered:true,visited:true,explored:false,poi:null}}}};
  const p=new MemoryPersistenceAdapter();
  p.createCampaign({campaignId:state.campaignId,displayName:state.displayName,stateJson:serializeCampaignState(state),masterSeed:state.rngState.masterSeed,rngAlgorithm:state.rngState.algorithm,initialSnapshotId:'s0'});
  return {p,campaignId:state.campaignId};
}

test('Atlas exposes real travel and explore controls bound to WorldActionService',()=>{
  const html=fs.readFileSync(path.join(web,'scr014.html'),'utf8');
  const js=fs.readFileSync(path.join(web,'scr014.js'),'utf8');
  assert.match(html,/id="travelHex"[^>]*>VIAJAR PARA ESTE HEX/);
  assert.match(html,/id="exploreHex"[^>]*>EXPLORAR HEX/);
  assert.match(js,/worldService\.travelToAdjacentHex/);
  assert.match(js,/worldService\.exploreHex/);
});

test('travel to adjacent unknown hex commits position and reveals terrain without inventing POI',()=>{
  const {p,campaignId}=setup();const svc=new WorldActionService({persistence:p});
  const result=svc.travelToAdjacentHex({campaignId,q:18,r:22});
  assert.equal(result.status,'OK');
  const state=JSON.parse(p.loadCampaign(campaignId).stateJson);
  assert.equal(state.worldState.position.q,18);assert.equal(state.worldState.position.r,22);
  const target=state.worldState.atlas.hexes['18,22'];
  assert.equal(target.discovered,true);assert.equal(target.visited,true);assert.equal(target.poi,null);
  assert.equal(state.partyState.groups.g.position.q,18);assert.equal(state.partyState.groups.g.position.r,22);
});

test('explore current hex commits explored state and time',()=>{
  const {p,campaignId}=setup();const svc=new WorldActionService({persistence:p});
  const before=JSON.parse(p.loadCampaign(campaignId).stateJson);
  const result=svc.exploreHex({campaignId,q:17,r:22,concealmentMultiplier:1});
  assert.equal(result.status,'OK');assert.equal(result.plan.days,1);
  const after=JSON.parse(p.loadCampaign(campaignId).stateJson);
  assert.equal(after.worldState.atlas.hexes['17,22'].explored,true);
  assert.equal(after.worldState.elapsedDays,(before.worldState.elapsedDays??0)+1);
});

test('Gemini bridge is asynchronous for live narration and supports scene image inlineData',()=>{
  const main=read('java/com/braseiro/wwn/MainActivity.kt');
  const bridge=read('java/com/braseiro/wwn/bridge/BraseiroBridge.kt');
  const gemini=read('java/com/braseiro/wwn/bridge/GeminiRuntime.kt');
  const provider=fs.readFileSync(path.join(web,'core/runtime-narrative-provider.js'),'utf8');
  assert.match(main,/requestGeminiTextAsync/);assert.match(main,/braseiro-gemini/);
  assert.match(bridge,/geminiGenerateTextAsync/);assert.match(provider,/generateAsync/);
  assert.match(gemini,/inlineData/);assert.match(gemini,/resolveSceneImagePart/);assert.match(gemini,/MAX_VISION_DIMENSION/);
});

test('TTS settings expose model, Charon voice and Gemini TTS provider',()=>{
  const html=fs.readFileSync(path.join(web,'configuracoes.html'),'utf8');
  const tts=read('java/com/braseiro/wwn/bridge/NativeTtsRuntime.kt');
  assert.match(html,/gemini-3\.1-flash-tts-preview/);assert.match(html,/>Charon</);assert.match(html,/value="GEMINI"/);
  assert.match(tts,/generateSpeech/);assert.match(tts,/provider == "GEMINI"/);
});

test('PDF import provides progress and RAG narrative scope excludes rules manuals',()=>{
  const main=read('java/com/braseiro/wwn/MainActivity.kt');
  const rag=read('java/com/braseiro/wwn/bridge/RagRuntime.kt');
  const regras=fs.readFileSync(path.join(web,'regras.js'),'utf8');
  assert.match(main,/"PROCESSING"/);assert.match(rag,/progress\?\.invoke/);
  assert.match(rag,/"NARRATIVE_PUBLIC" -> p == "CAMPAIGN_REFERENCE"/);
  assert.match(rag,/cachedRoot/);assert.match(regras,/Indexando PDF… página/);
});
