import fs from 'node:fs';
import assert from 'node:assert/strict';
import { makeStarterCampaignState, ensureStarterCampaign } from '../app/src/main/assets/www/core/starter-bootstrap.js';
import { MemoryPersistenceAdapter } from '../app/src/main/assets/www/core/persistence-port.js';
import { CharacterCreationService } from '../app/src/main/assets/www/core/character-creation-service.js';
import { WorldActionService } from '../app/src/main/assets/www/core/world-service.js';
import { storePendingSceneImageForCampaign, pendingSceneImageFromState } from '../app/src/main/assets/www/core/scene-image-state-service.js';

const pack=JSON.parse(fs.readFileSync('app/src/main/assets/www/data/wwn.starter-region.v1.json','utf8'));
const iria=JSON.parse(fs.readFileSync('app/src/main/assets/www/data/wwn.starter-character.v1.json','utf8'));
const jornada=fs.readFileSync('app/src/main/assets/www/jornada.js','utf8');
const ficha=fs.readFileSync('app/src/main/assets/www/ficha.js','utf8');
const atlas=fs.readFileSync('app/src/main/assets/www/scr014.js','utf8');
const atlasCss=fs.readFileSync('app/src/main/assets/www/scr014.css','utf8');
const activity=fs.readFileSync('app/src/main/java/com/braseiro/wwn/MainActivity.kt','utf8');
const hotfix=fs.readFileSync('app/src/main/assets/www/ux-hotfix.js','utf8');

// New-install contract: world/campaign exist before the player chooses a hero.
const empty=makeStarterCampaignState({pack,campaignId:'qa-empty',masterSeed:'qa-empty-seed'});
assert.equal(empty.characterState,null);
assert.deepEqual(empty.charactersState,{});
assert.equal(empty.partyState,null);
assert.ok(Number.isInteger(empty.worldState?.position?.q));
assert.equal(empty.encounterState?.currentScene?.publicScene?.contentType,'campaignOpening');

// QA/backward-compatible direct factory remains able to materialize an explicit fixture hero.
const withHero=makeStarterCampaignState({character:iria,pack,campaignId:'qa-fixture',masterSeed:'qa-fixture-seed'});
assert.equal(withHero.characterState?.characterId,iria.characterId);
assert.ok(withHero.partyState);

// Production bootstrap defaults to empty even though the historical starter JSON is supplied.
const persistence=new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character:iria,pack,campaignId:'qa-production'});
assert.match(boot.status,/^OK/);
let loaded=persistence.loadCampaign('qa-production');
let state=JSON.parse(loaded.stateJson);
assert.equal(state.characterState,null);
assert.equal(state.partyState,null);

// First generated hero must become active and create the first party in that existing world.
const creation=new CharacterCreationService({persistence});
const created=creation.generateRandom({campaignId:'qa-production',characterId:'qa:first',name:'Primeiro Herói',goal:'Explorar o mundo',setActive:true});
assert.equal(created.status,'OK');
assert.equal(created.projectedState.characterState.characterId,'qa:first');
assert.ok(created.projectedState.partyState?.activeGroupId);
const members=Object.values(created.projectedState.partyState.groups).flatMap(g=>g.memberIds??[]);
assert.ok(members.includes('qa:first'));

// No map teleport: non-adjacent destinations are rejected without mutation.
const world=new WorldActionService({persistence});
loaded=persistence.loadCampaign('qa-production'); state=JSON.parse(loaded.stateJson);
const q=state.worldState.position.q,r=state.worldState.position.r,rev=loaded.revision;
const teleport=world.travelToAdjacentHex({campaignId:'qa-production',q:q+2,r});
assert.equal(teleport.status,'DESTINATION_MUST_BE_ADJACENT_NO_MUTATION');
assert.equal(persistence.loadCampaign('qa-production').revision,rev);

// Campaign-local image staging must not leak to another campaign.
ensureStarterCampaign({persistence,character:iria,pack,campaignId:'qa-other'});
const staged=storePendingSceneImageForCampaign({persistence,campaignId:'qa-production',result:{url:'https://appassets.androidplatform.net/private-scene/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.png',sha256:'a'.repeat(64),width:1024,height:576}});
assert.equal(staged.status,'OK');
const a=JSON.parse(persistence.loadCampaign('qa-production').stateJson);
const b=JSON.parse(persistence.loadCampaign('qa-other').stateJson);
assert.ok(pendingSceneImageFromState(a)?.url);
assert.equal(pendingSceneImageFromState(b),null);

// Concrete phone-regression contracts present in production source.
assert.doesNotMatch(jornada,/toggle\('hidden',!imageRef\)const observe/);
assert.match(jornada,/skillLabelPtBr\(k\)/);
assert.match(jornada,/selectJourneyFeed/);
assert.match(jornada,/storePendingSceneImageForCampaign/);
assert.match(jornada,/removeSceneImageFromCampaign/);
assert.doesNotMatch(jornada,/OPERAÇÕES DO MESTRE/i);
assert.match(ficha,/SEM PERSONAGEM/);
assert.match(ficha,/data-generate/);
assert.match(ficha,/CRIAÇÃO MANUAL/);
assert.doesNotMatch(atlas,/poiName\?\?'POI'\)\.slice\(0,18\)/);
assert.match(atlasCss,/WWN_REAL_FLOW_POI_HORIZONTAL_V1/);
assert.match(activity,/Intent\.ACTION_SEND/);
assert.match(activity,/SCENE_IMAGE_IMPORT/);
assert.match(hotfix,/function artKind/);
assert.match(hotfix,/\btorch:/);
assert.match(hotfix,/\blantern:/);
assert.match(hotfix,/\brope:/);
assert.match(hotfix,/\bfood:/);
assert.match(hotfix,/\barmor:/);
assert.match(hotfix,/\bweapon:/);

console.log('WWN_FINISH_REAL_FLOW_GATE_PASS=26');
