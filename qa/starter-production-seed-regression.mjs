import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {makeStarterCampaignState} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {deriveFullClassChassis} from '../app/src/main/assets/www/core/wwn-srd-class-combination.js';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {AdvancementService} from '../app/src/main/assets/www/core/advancement-service.js';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'../app/src/main/assets/www');
const character=JSON.parse(fs.readFileSync(path.join(root,'data/wwn.starter-character.v1.json'),'utf8'));
const pack=JSON.parse(fs.readFileSync(path.join(root,'data/wwn.starter-region.v1.json'),'utf8'));
const state=makeStarterCampaignState({character,pack});
assert.equal(state.characterState.classId,'EXPERT','starter class ID must use the current SRD class model');
assert.equal(deriveFullClassChassis({classId:state.characterState.classId,level:1}).status,'OK');

// Backward/authoring robustness: human-spaced class labels must normalize before entering state.
const spaced=structuredClone(character); delete spaced.classId; spaced.class='Full Expert';
const spacedState=makeStarterCampaignState({character:spaced,pack,campaignId:'seed-normalization-check'});
assert.equal(spacedState.characterState.classId,'EXPERT');
const legacy=structuredClone(character); delete legacy.classId; legacy.class='FULL_EXPERT';
assert.equal(makeStarterCampaignState({character:legacy,pack,campaignId:'seed-legacy-alias'}).characterState.classId,'EXPERT');

// The production starter must be able to use the current advancement engine.
const persistence=new MemoryPersistenceAdapter();
const boot=ensureStarterCampaign({persistence,character,pack,campaignId:'seed-advancement-check'});
let loaded=persistence.loadCampaign(boot.campaignId);
let mutable=JSON.parse(loaded.stateJson);
mutable.characterState.xp=100000;
mutable.charactersState[mutable.characterState.characterId]=structuredClone(mutable.characterState);
mutable.revision=loaded.revision+1;
persistence.commit({campaignId:boot.campaignId,expectedRevision:loaded.revision,eventId:'seed-xp',eventKind:'TEST_SETUP',eventPayloadJson:'{}',newStateJson:JSON.stringify(mutable),rngCountersJson:'{}'});
const advanced=new AdvancementService({persistence}).advanceLevel({campaignId:boot.campaignId,hitDieRolls:[4,4]});
assert.equal(advanced.status,'OK');
assert.equal(advanced.character.level,2);
assert.equal(advanced.character.classId,'EXPERT');

const atlas=fs.readFileSync(path.join(root,'scr014.js'),'utf8');
assert.match(atlas,/makeStarterCampaignState/,'Atlas browser fallback must use production starter bootstrap');
assert.doesNotMatch(atlas,/canonicalIriaFixture|fixtureState\(|buildFixtureRuntime/,'Atlas must not own a fixture-backed production fallback');
assert.doesNotMatch(atlas,/scr014-browser-fixture/,'Atlas must not create fixture authority in production fallback');
console.log('STARTER_PRODUCTION_SEED_REGRESSION PASS');
