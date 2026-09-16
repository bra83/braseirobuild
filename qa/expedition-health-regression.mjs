import fs from 'node:fs';import assert from 'node:assert/strict';
import {MemoryPersistenceAdapter} from '../app/src/main/assets/www/core/persistence-port.js';
import {ensureStarterCampaign} from '../app/src/main/assets/www/core/starter-bootstrap.js';
import {PlayerMechanicsFacade} from '../app/src/main/assets/www/core/player-mechanics-facade.js';
const j=p=>JSON.parse(fs.readFileSync(new URL(p,import.meta.url),'utf8'));
const p=new MemoryPersistenceAdapter(),boot=ensureStarterCampaign({persistence:p,character:j('../app/src/main/assets/www/data/wave6.iria.canonical.fixture.json'),pack:j('../app/src/main/assets/www/data/wwn.starter-region.v1.json')});assert.ok(String(boot.status).startsWith('OK'));const f=new PlayerMechanicsFacade({persistence:p}),id=boot.campaignId;
let s=f.summary({campaignId:id});assert.equal(s.status,'OK');
let r=f.forage({campaignId:id,contract:{terrain:'WOODLANDS_OR_HEAVY_VEGETATION',wisdomScore:s.character.attributes.Wisdom,bestSurviveLevel:s.character.skills.Survive??null,groupSurviveLevels:[],fullDay:true,successiveDaysInSameHex:0,encounterDie:6,nativeWildsMinimumTwo:false}});assert.ok(['OK','ALLOCATION_REQUIRED'].includes(r.status));
if(r.status==='ALLOCATION_REQUIRED'){r=f.allocateForage({campaignId:id,foodUnits:r.result.unitsFound,waterUnits:0,firewoodUnits:0});assert.equal(r.status,'OK');}
r=f.nightRest({campaignId:id,contract:{warm:true,fed:true,comfortable:true,uninterruptedHours:8}});assert.ok(String(r.status).startsWith('OK')||String(r.status).includes('REST'));
console.log('EXPEDITION_HEALTH_REGRESSION PASS forage + allocation + rest');
