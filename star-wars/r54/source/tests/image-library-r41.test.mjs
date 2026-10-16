import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';
import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';
const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const vtt=path.join(root,'app/src/main/assets/vtt');
const manifest=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/manifest.json'),'utf8'));
const source=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/sources/kainrath.json'),'utf8'));
const wave=JSON.parse(fs.readFileSync(path.join(root,'R41_KAINRATH_VEHICLE_WAVE2_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R41 manifest V3.4 has 329 unique assets',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=329);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,manifest.counts.total);
  assert.ok(manifest.counts.vehicles>=90);
  assert.ok(manifest.counts.kainrathVehicles>=90);
});

test('R41 vehicle wave2 adds exactly 47 direct-URL assets',()=>{
  assert.equal(wave.length,47);
  assert.equal(waveAssets.length,47);
  assert.equal(source.vehicleWave2Count,47);
  assert.ok(source.vehiclesCount>=90);
});

test('R41 wave2 grouping is 28 freighter/transport page assets + 12 landspeeders + 7 airspeeders',()=>{
  const groups=Object.fromEntries(['freighters-p2','freighters-p3','landspeeders','airspeeders'].map(g=>[g,waveAssets.filter(a=>a.tags.includes(g)).length]));
  assert.deepEqual(groups,{'freighters-p2':15,'freighters-p3':13,'landspeeders':12,'airspeeders':7});
});

test('R41 URLs obey secure Kainrath cache policy',()=>{
  for(const a of waveAssets){
    assert.equal(a.category,'vehicle');
    assert.equal(a.remoteReady,true);
    assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
  }
});

test('R41 vehicle images remain visual-only',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
  }
});

test('R41 bridge exposes all 90 vehicles while preserving maps and old visual entities',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=164);
  assert.equal(vehicles.ok,true); assert.ok(vehicles.data.length>=90);
  assert.equal(maps.ok,true); assert.equal(maps.data.length,18);
});

test('R41 category searches expose surface subtypes without mechanics',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const land=bridge.searchImageLibrary({category:'vehicle',subtype:'landspeeder',limit:500});
  const air=bridge.searchImageLibrary({category:'vehicle',subtype:'airspeeder',limit:500});
  assert.equal(land.ok,true); assert.ok(land.data.length>=5);
  assert.equal(air.ok,true); assert.equal(air.data.length,7);
  for(const a of [...land.data,...air.data]) assert.equal(a.mechanicalBinding,null);
});

test('R41 previous R40 vehicle wave remains present and unchanged',()=>{
  const old=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R40_KAINRATH_VEHICLE_WAVE1_INDEX.json'),'utf8'));
  assert.equal(old.length,43);
  const oldIds=new Set(old.map(e=>e.assetId));
  assert.equal(manifest.assets.filter(a=>oldIds.has(a.assetId)).length,43);
});
