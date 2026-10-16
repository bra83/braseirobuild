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
const wave=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R40_KAINRATH_VEHICLE_WAVE1_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R40 manifest V3.3 has 282 unique assets',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=282);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,manifest.counts.total);
});

test('R40 vehicle wave contains exactly 43 Kainrath vehicles/ships',()=>{
  assert.equal(wave.length,43);
  assert.equal(waveAssets.length,43);
  assert.ok(manifest.counts.vehicles>=43);
  assert.ok(manifest.counts.kainrathVehicles>=43);
});

test('R40 vehicle URLs obey the existing Kainrath whitelist and cache contract',()=>{
  for(const a of waveAssets){
    assert.equal(a.category,'vehicle');
    assert.equal(a.sourceType,'external_reference');
    assert.equal(a.remoteReady,true);
    assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
  }
});

test('R40 vehicle assets remain visual-only and cannot infer mechanics',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
  }
});

test('R40 source metadata records vehicle wave separately without mutating prior visual waves',()=>{
  assert.ok(source.vehiclesCount>=43);
  assert.ok(source.vehicleEntries.length>=43);
  assert.ok(source.visualEntitiesCount>=74);
  assert.equal(source.visualWave3Count,10);
  assert.equal(source.npcWave2Count,20);
});

test('R40 generic visual bridge exposes vehicles and keeps prior visual categories',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const ships=bridge.imageVisualEntityCatalog({kind:'starships',limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=117);
  assert.equal(vehicles.ok,true); assert.ok(vehicles.data.length>=43);
  assert.equal(ships.ok,true); assert.ok(ships.data.length>=43);
});

test('R40 searchImageLibrary can query vehicle subtype without touching map catalog',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const capitals=bridge.searchImageLibrary({category:'vehicle',subtype:'capital-ship',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(capitals.ok,true); assert.equal(capitals.data.length,12);
  assert.equal(maps.ok,true); assert.equal(maps.data.length,18);
});

test('R40 Android visual library exposes Veículos/Naves filter without mechanical binding language regression',()=>{
  const html=fs.readFileSync(path.join(vtt,'index.html'),'utf8');
  const app=fs.readFileSync(path.join(vtt,'js/ui/app.mjs'),'utf8');
  assert.match(html,/value="vehicle">Veículos \/ Naves/);
  assert.match(app,/category:'vehicle'/);
  assert.match(app,/veículo\/nave/);
  assert.match(html,/não criam ficha, estatística, espécie, facção, habilidade ou regra automaticamente/);
});
