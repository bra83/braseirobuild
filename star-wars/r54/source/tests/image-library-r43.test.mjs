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
const vehicles=JSON.parse(fs.readFileSync(path.join(root,'R43_KAINRATH_VEHICLE_WAVE4_INDEX.json'),'utf8'));
const droids=JSON.parse(fs.readFileSync(path.join(root,'R43_KAINRATH_DROID_WAVE4_INDEX.json'),'utf8'));
const embeddedVehicles=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R43_KAINRATH_VEHICLE_WAVE4_INDEX.json'),'utf8'));
const embeddedDroids=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R43_KAINRATH_DROID_WAVE4_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const wave=[...vehicles,...droids];
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R43 manifest V3.6 baseline remains preserved after later visual promotions',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=360);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,manifest.counts.total);
  assert.equal(manifest.counts.local,148);
  assert.ok(manifest.counts.external>=212);
  assert.ok(manifest.counts.remoteReady>=212);
  assert.ok(manifest.counts.vehicles>=120);
  assert.ok(manifest.counts.kainrathVehicles>=120);
  assert.equal(manifest.counts.droids,23);
  assert.ok(manifest.counts.kainrathVisualEntities>=75);
});

test('R43 wave4 indexes exactly 3 vehicles plus 1 droid',()=>{
  assert.equal(vehicles.length,3); assert.equal(droids.length,1);
  assert.deepEqual(embeddedVehicles,vehicles); assert.deepEqual(embeddedDroids,droids);
  assert.equal(waveAssets.length,4);
  assert.equal(source.vehicleWave4Count,3);
  assert.equal(source.vehicleWave4Entries.length,3);
  assert.equal(source.visualWave4Count,1);
  assert.equal(source.visualWave4Entries.length,1);
  assert.ok(source.vehiclesCount>=120);
  assert.equal(source.droidsCount,23);
  assert.ok(source.visualEntitiesCount>=75);
});

test('R43 wave4 contains only the direct-proven recovery set',()=>{
  const titles=new Set(waveAssets.map(a=>a.title));
  for(const title of ['Y164 Slave Transport','Simiyiar Light Freighter','Customized Nova Courier “The Lucky Guess”','3-PX Series Protocol Droid']) assert.equal(titles.has(title),true,title);
});

test('R43 URLs are secure direct raw Kainrath media URLs',()=>{
  for(const a of waveAssets){
    assert.equal(a.remoteReady,true); assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
    assert.equal(a.remoteUrl.includes('?'),false,a.remoteUrl);
    assert.match(a.remoteUrl,/\.(?:png|jpe?g|webp)$/i);
  }
});

test('R43 recovery remains visual-only and never infers mechanics',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
    assert.ok(['R43_VEHICLE_WAVE4','R43_DROID_WAVE4'].includes(a.indexWave));
  }
});

test('R43 bridge exposes 120 vehicles and 75 Kainrath visual entities',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const v=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const ds=bridge.searchImageLibrary({category:'droid',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=195);
  assert.equal(v.ok,true); assert.ok(v.data.length>=120);
  assert.equal(ds.ok,true); assert.equal(ds.data.length,23);
  assert.equal(maps.ok,true); assert.equal(maps.data.length,18);
});

test('R43 direct-recovery group is searchable without affecting old walker wave',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const freighters=bridge.searchImageLibrary({category:'vehicle',subtype:'freighter',limit:500});
  const transports=bridge.searchImageLibrary({category:'vehicle',subtype:'transport',limit:500});
  const walkers=bridge.searchImageLibrary({category:'vehicle',subtype:'walker',limit:500});
  assert.equal(freighters.ok,true); assert.ok(freighters.data.some(a=>a.assetId==='external.kainrath.vehicle.simiyiar-light-freighter'));
  assert.ok(freighters.data.some(a=>a.assetId==='external.kainrath.vehicle.customized-nova-courier-the-lucky-guess'));
  assert.equal(transports.ok,true); assert.ok(transports.data.some(a=>a.assetId==='external.kainrath.vehicle.y164-slave-transport'));
  assert.equal(walkers.ok,true); assert.equal(walkers.data.length,8);
});

test('R43 preserves R40-R42 vehicle waves unchanged',()=>{
  const r40=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R40_KAINRATH_VEHICLE_WAVE1_INDEX.json'),'utf8'));
  const r41=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R41_KAINRATH_VEHICLE_WAVE2_INDEX.json'),'utf8'));
  const r42=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R42_KAINRATH_VEHICLE_WAVE3_INDEX.json'),'utf8'));
  assert.deepEqual([r40.length,r41.length,r42.length],[43,47,27]);
  const oldIds=new Set([...r40,...r41,...r42].map(e=>e.assetId));
  assert.equal(oldIds.size,117);
  assert.equal(manifest.assets.filter(a=>oldIds.has(a.assetId)).length,117);
});
