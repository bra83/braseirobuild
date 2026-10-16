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
const wave=JSON.parse(fs.readFileSync(path.join(root,'R42_KAINRATH_VEHICLE_WAVE3_INDEX.json'),'utf8'));
const embeddedWave=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R42_KAINRATH_VEHICLE_WAVE3_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R42 manifest V3.5 baseline remains preserved after later library promotions',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=356);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,manifest.counts.total);
  assert.equal(manifest.counts.local,148);
  assert.ok(manifest.counts.external>=208);
  assert.ok(manifest.counts.remoteReady>=208);
  assert.ok(manifest.counts.vehicles>=117);
  assert.ok(manifest.counts.kainrathVehicles>=117);
});

test('R42 vehicle wave3 adds exactly 27 direct-URL assets',()=>{
  assert.equal(wave.length,27);
  assert.equal(embeddedWave.length,27);
  assert.deepEqual(embeddedWave,wave);
  assert.equal(waveAssets.length,27);
  assert.equal(source.vehicleWave3Count,27);
  assert.equal(source.vehicleWave3Entries.length,27);
  assert.ok(source.vehiclesCount>=117);
  assert.ok(source.vehicleEntries.length>=117);
});

test('R42 wave3 grouping is 19 landspeeders page2 plus 8 walkers',()=>{
  const groups=Object.fromEntries(['landspeeders-p2','walkers'].map(g=>[g,waveAssets.filter(a=>a.tags.includes(g)).length]));
  assert.deepEqual(groups,{'landspeeders-p2':19,'walkers':8});
  assert.equal(waveAssets.filter(a=>a.subtype==='walker').length,8);
});

test('R42 URLs obey secure Kainrath cache policy and are raw direct image URLs',()=>{
  for(const a of waveAssets){
    assert.equal(a.category,'vehicle');
    assert.equal(a.remoteReady,true);
    assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
    assert.equal(a.remoteUrl.includes('?'),false,a.remoteUrl);
    assert.match(a.remoteUrl,/\.(?:png|jpe?g|webp)$/i);
  }
});

test('R42 vehicle images remain visual-only with no inferred mechanics',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
    assert.equal(a.indexWave,'R42_VEHICLE_WAVE3');
  }
});

test('R42 bridge preserves at least the R42 vehicle/visual baseline',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=191);
  assert.equal(vehicles.ok,true); assert.ok(vehicles.data.length>=117);
  assert.equal(maps.ok,true); assert.equal(maps.data.length,18);
});

test('R42 subtype searches expose walkers and added surface vehicle classes without mechanics',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const walkers=bridge.searchImageLibrary({category:'vehicle',subtype:'walker',limit:500});
  const swoops=bridge.searchImageLibrary({category:'vehicle',subtype:'swoop',limit:500});
  const bikes=bridge.searchImageLibrary({category:'vehicle',subtype:'speeder-bike',limit:500});
  assert.equal(walkers.ok,true); assert.equal(walkers.data.length,8);
  assert.equal(swoops.ok,true); assert.ok(swoops.data.length>=4);
  assert.equal(bikes.ok,true); assert.ok(bikes.data.length>=3);
  for(const a of [...walkers.data,...swoops.data,...bikes.data]) assert.equal(a.mechanicalBinding,null);
});

test('R42 preserves both previous vehicle waves unchanged',()=>{
  const r40=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R40_KAINRATH_VEHICLE_WAVE1_INDEX.json'),'utf8'));
  const r41=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R41_KAINRATH_VEHICLE_WAVE2_INDEX.json'),'utf8'));
  assert.equal(r40.length,43);
  assert.equal(r41.length,47);
  const oldIds=new Set([...r40,...r41].map(e=>e.assetId));
  assert.equal(oldIds.size,90);
  assert.equal(manifest.assets.filter(a=>oldIds.has(a.assetId)).length,90);
});
