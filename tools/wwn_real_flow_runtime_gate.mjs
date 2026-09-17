import assert from 'node:assert/strict';
import fs from 'node:fs';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { storePendingSceneImageForCampaign, attachSceneImageToCampaign, removeSceneImageFromCampaign, pendingSceneImageFromState } from '../app/src/main/assets/www/core/scene-image-state-service.js';
import { selectJourneyFeed, knownJourneyLocations } from '../app/src/main/assets/www/core/journey-discovery.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';
import { RuntimeBarbaraProvider } from '../app/src/main/assets/www/core/runtime-narrative-provider.js';

function create(p,id,state){
  p.createCampaign({campaignId:id,displayName:id,stateJson:JSON.stringify(state),masterSeed:'seed:'+id,rngAlgorithm:'xoshiro128ss-v1',initialSnapshotId:'s0'});
}

const p=new MemoryPersistenceAdapter();
create(p,'A',{campaignId:'A',revision:0,rngState:{counters:{}},worldState:{position:{q:17,r:22,name:'RUÍNAS DA VIGÍLIA'},atlas:{hexes:{'17,22':{q:17,r:22,discovered:true,visited:true,poiName:'RUÍNAS DA VIGÍLIA'}}}},encounterState:{}});
create(p,'B',{campaignId:'B',revision:0,rngState:{counters:{}},worldState:{position:{q:3,r:3}},encounterState:{}});
const img={url:'https://appassets.androidplatform.net/scene-images/a.png',sha256:'abc',width:100,height:60};
assert.equal(storePendingSceneImageForCampaign({persistence:p,campaignId:'A',result:img}).status,'OK');
let a=JSON.parse(p.loadCampaign('A').stateJson),b=JSON.parse(p.loadCampaign('B').stateJson);
assert.equal(pendingSceneImageFromState(a).url,img.url);
assert.equal(pendingSceneImageFromState(b),null);

let loaded=p.loadCampaign('A');a=JSON.parse(loaded.stateJson);a.revision=loaded.revision+1;
a.encounterState.currentScene={contentType:'majorSite',contentId:'site',publicScene:{status:'OK',contentType:'majorSite',contentId:'site',title:'LOCAL',summary:'Uma ruína conhecida.'}};
assert.equal(p.commit({campaignId:'A',expectedRevision:loaded.revision,eventId:'scene-open',eventKind:'SCENE_OPENED',eventPayloadJson:'{}',newStateJson:JSON.stringify(a),rngCountersJson:'{}'}).status,'OK');
assert.equal(attachSceneImageToCampaign({persistence:p,campaignId:'A',result:img}).status,'OK');
a=JSON.parse(p.loadCampaign('A').stateJson);
assert.equal(a.encounterState.currentScene.imageRef,img.url);
assert.equal(a.encounterState.pendingSceneImage,undefined);
assert.equal(removeSceneImageFromCampaign({persistence:p,campaignId:'A'}).status,'OK');
a=JSON.parse(p.loadCampaign('A').stateJson);
assert.equal(a.encounterState.currentScene.imageRef,undefined);

const feed1=selectJourneyFeed({revision:1,rngState:{masterSeed:'x'}},{startingHooks:[{id:'h1'},{id:'h2'},{id:'h3'}],rumors:['r1','r2','r3']},{hookCount:2,rumorCount:1});
const feed2=selectJourneyFeed({revision:2,rngState:{masterSeed:'x'}},{startingHooks:[{id:'h1'},{id:'h2'},{id:'h3'}],rumors:['r1','r2','r3']},{hookCount:2,rumorCount:1});
assert.equal(feed1.hooks.length,2);
assert.notDeepEqual(feed1.hooks,feed2.hooks);
const known=knownJourneyLocations({worldState:{position:{name:'A'},atlas:{hexes:{'1,1':{discovered:true,poiName:'B'}}}}},{majorSites:[{name:'A'},{name:'B'},{name:'C'}],minorPois:[]});
assert.deepEqual(known.majorSites.map(x=>x.name),['A','B']);

const p2=new MemoryPersistenceAdapter();
create(p2,'M',{campaignId:'M',revision:0,rngState:{counters:{}},worldState:{position:{q:10,r:10},pendingTravel:{q:11,r:10,hoursRemaining:2,terrain:'PLAINS'}},partyState:{groups:{g:{memberIds:[]}}}});
const world=new WorldActionService({persistence:p2});
assert.equal(world.travelToAdjacentHex({campaignId:'M',q:12,r:10}).status,'DESTINATION_MUST_BE_ADJACENT_NO_MUTATION');
assert.equal(world.travelToAdjacentHex({campaignId:'M',q:10,r:11}).status,'TRAVEL_INTERRUPTED_DESTINATION_LOCKED_NO_MUTATION');
assert.equal(p2.loadCampaign('M').revision,0);

const local=new RuntimeBarbaraProvider({native:null,ragSearch:null,ragSearchAsync:null});
const narrative=local.localFallback({mechanicalEvent:{eventId:'e',revision:1,kind:'SCENE_OPENED',scene:{contentType:'campaignOpening',title:'ABERTURA',summary:'Ruínas antigas dominam o horizonte.',situation:'O caminho adiante permanece aberto.',leads:['Uma estrada segue ao norte.']}},state:{worldState:{position:{name:'RUÍNAS'}}}});
assert.equal(narrative.status,'OK');
assert.ok(narrative.text.split(/\s+/).length>=180,'fallback maior deve ser substancial');

const journey=fs.readFileSync('app/src/main/assets/www/jornada.js','utf8');
const html=fs.readFileSync('app/src/main/assets/www/jornada.html','utf8');
const atlas=fs.readFileSync('app/src/main/assets/www/scr014.js','utf8');
const css=fs.readFileSync('app/src/main/assets/www/scr014.css','utf8');
assert.match(journey,/skillLabelPtBr\(k\)/);
assert.match(journey,/storePendingSceneImageForCampaign/);
assert.match(journey,/knownJourneyLocations/);
assert.match(journey,/SEM PERSONAGEM/);
assert.doesNotMatch(html,/OPERAÇÕES DO MESTRE/);
assert.doesNotMatch(atlas,/slice\(0,18\)/);
assert.match(css,/WWN_REAL_FLOW_POI_HORIZONTAL_V1/);
assert.match(css,/\.poi-symbol>i\{[^}]*transform:none/);
assert.match(html,/sceneRemoveImageButton/);
console.log('WWN_REAL_FLOW_RUNTIME_GATE_PASS');
