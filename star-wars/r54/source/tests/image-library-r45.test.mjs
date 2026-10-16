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
const wave=JSON.parse(fs.readFileSync(path.join(root,'R45_KAINRATH_VEHICLE_WAVE5_INDEX.json'),'utf8'));
const embedded=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R45_KAINRATH_VEHICLE_WAVE5_INDEX.json'),'utf8'));
const audit=JSON.parse(fs.readFileSync(path.join(root,'R45_IMAGE_LIBRARY_V3_8_AUDIT.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));
const expectedTitles=new Set(['Arunskin 32 Cargo Skiff','XP-291 Skimmer','Starhawk Speeder Bike','85-XS Odyssey Heavy Speeder Bike','Ubrikkian 9000 Z001 Landspeeder','00-3 Kravaan Medium Freighter','431 Hauler Cargo Tug']);

test('R45 manifest V3.8 has 369 unique assets and 127 vehicles',()=>{
  assert.equal(manifest.version,'3.8.0');
  assert.equal(manifest.counts.total,369); assert.equal(ids.length,369); assert.equal(new Set(ids).size,369);
  assert.equal(manifest.counts.local,148); assert.equal(manifest.counts.external,221); assert.equal(manifest.counts.remoteReady,221);
  assert.equal(manifest.counts.vehicles,127); assert.equal(manifest.counts.kainrathVehicles,127);
  assert.equal(manifest.counts.creatures,17); assert.equal(manifest.counts.droids,23);
});

test('R45 vehicle wave5 indexes exactly the 7 direct-proven recovery entries',()=>{
  assert.equal(wave.length,7); assert.deepEqual(embedded,wave); assert.equal(waveAssets.length,7);
  assert.deepEqual(new Set(waveAssets.map(a=>a.title)),expectedTitles);
  assert.equal(source.vehicleWave5Count,7); assert.deepEqual(source.vehicleWave5Entries,wave);
  assert.equal(source.vehiclesCount,127); assert.equal(source.vehicleEntries.length,127);
});

test('R45 direct-media provenance records the exact observed Kainrath links',()=>{
  for(const title of expectedTitles){
    const e=audit.provenanceEvidence[title]; assert.ok(e,title);
    assert.ok(e.observedImageUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'),title);
    assert.ok(e.canonicalRemoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'),title);
    assert.equal(e.canonicalRemoteUrl.includes('?'),false,title);
    assert.match(e.evidence,/direct wp-content\/uploads URL/);
  }
  assert.match(audit.policy,/Only direct URLs actually observed/);
});

test('R45 URLs obey secure Kainrath cache policy',()=>{
  for(const a of waveAssets){
    assert.equal(a.category,'vehicle'); assert.equal(a.remoteReady,true); assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand'); assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
    assert.equal(a.remoteUrl.includes('?'),false); assert.match(a.remoteUrl,/\.(?:png|jpe?g|webp)$/i);
  }
});

test('R45 recovered vehicles remain visual-only and mechanically unbound',()=>{
  for(const a of waveAssets){ assert.equal(a.mechanicalBinding,null); assert.equal(a.usage,'visual_reference'); assert.equal(a.indexWave,'R45_VEHICLE_WAVE5'); }
});

test('R45 bridge exposes 127 vehicles while preserving other visual categories',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const droids=bridge.searchImageLibrary({category:'droid',limit:500});
  const creatures=bridge.searchImageLibrary({category:'creature',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.equal(all.data.length,204);
  assert.equal(vehicles.ok,true); assert.equal(vehicles.data.length,127);
  assert.equal(droids.data.length,23); assert.equal(creatures.data.length,17); assert.equal(maps.data.length,18);
});

test('R45 recovered landspeeders and freighters are searchable without campaign mutation',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  for(const [query,id] of [
    ['Arunskin','external.kainrath.vehicle.arunskin-32-cargo-skiff'],
    ['Odyssey','external.kainrath.vehicle.85-xs-odyssey-heavy-speeder-bike'],
    ['Ubrikkian','external.kainrath.vehicle.ubrikkian-9000-z001-landspeeder'],
    ['Kravaan','external.kainrath.vehicle.00-3-kravaan-medium-freighter'],
    ['Cargo Tug','external.kainrath.vehicle.431-hauler-cargo-tug']
  ]){ const r=bridge.searchImageLibrary({query,category:'vehicle',limit:50}); assert.equal(r.ok,true); assert.ok(r.data.some(a=>a.assetId===id),query); }
});

test('R45 preserves R44 beast recovery and leaves unproven media blocked',()=>{
  const r44=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R44_KAINRATH_BEAST_WAVE5_INDEX.json'),'utf8'));
  assert.equal(r44.length,2); for(const e of r44) assert.ok(manifest.assets.some(a=>a.assetId===e.assetId));
  for(const blocked of ['Void-Spider TX-3 Air Taxi','A-1 Deluxe Floater','XP-38A Landspeeder','Scavenger Tug','GS-100 Salvage Ship'])
    assert.ok(audit.blockedByDirectMediaProvenance.vehicles.includes(blocked),blocked);
  assert.ok(audit.blockedByDirectMediaProvenance.droids_missing_known_page_with_direct_media_not_observed.includes('Antiquated Battle Droid'));
});
