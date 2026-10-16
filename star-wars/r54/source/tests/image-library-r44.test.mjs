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
const wave=JSON.parse(fs.readFileSync(path.join(root,'R44_KAINRATH_BEAST_WAVE5_INDEX.json'),'utf8'));
const embedded=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R44_KAINRATH_BEAST_WAVE5_INDEX.json'),'utf8'));
const audit=JSON.parse(fs.readFileSync(path.join(root,'R44_IMAGE_LIBRARY_V3_7_AUDIT.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R44 manifest V3.7 baseline remains preserved after later visual promotions',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=362); assert.equal(ids.length,manifest.counts.total); assert.equal(new Set(ids).size,manifest.counts.total);
  assert.equal(manifest.counts.local,148); assert.ok(manifest.counts.external>=214); assert.ok(manifest.counts.remoteReady>=214);
  assert.equal(manifest.counts.creatures,17); assert.equal(manifest.counts.kainrathVisualEntities,77);
  assert.ok(manifest.counts.vehicles>=120); assert.equal(manifest.counts.droids,23);
});

test('R44 wave5 indexes exactly Skekfish and Nerf',()=>{
  assert.equal(wave.length,2); assert.deepEqual(embedded,wave); assert.equal(waveAssets.length,2);
  assert.deepEqual(new Set(waveAssets.map(a=>a.title)),new Set(['Skekfish','Nerf']));
  assert.equal(source.visualWave5Count,2); assert.deepEqual(source.visualWave5Entries,wave);
  assert.equal(source.beastsCount,17); assert.equal(source.visualEntitiesCount,77);
});

test('R44 direct media provenance is recorded without filename invention',()=>{
  for(const title of ['Skekfish','Nerf']){
    const e=audit.provenanceEvidence[title]; assert.ok(e); assert.ok(e.observedImageUrl.includes('?w=731'));
    assert.equal(e.canonicalRemoteUrl.includes('?'),false); assert.ok(e.evidence.includes('exposed the image link directly'));
  }
  assert.match(audit.policy,/Only direct URLs actually observed/);
});

test('R44 URLs obey secure raw Kainrath cache policy',()=>{
  for(const a of waveAssets){
    assert.equal(a.category,'creature'); assert.equal(a.subtype,'beast'); assert.equal(a.remoteReady,true); assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand'); assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/2021/08/'));
    assert.equal(a.remoteUrl.includes('?'),false); assert.match(a.remoteUrl,/\.png$/i);
  }
});

test('R44 beasts remain visual-only and mechanically unbound',()=>{
  for(const a of waveAssets){ assert.equal(a.mechanicalBinding,null); assert.equal(a.usage,'visual_reference'); assert.equal(a.indexWave,'R44_BEAST_WAVE5'); }
});

test('R44 bridge exposes 17 creatures while preserving vehicles, droids and maps',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const creatures=bridge.searchImageLibrary({category:'creature',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const droids=bridge.searchImageLibrary({category:'droid',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=197); assert.equal(creatures.ok,true); assert.equal(creatures.data.length,17);
  assert.ok(vehicles.data.length>=120); assert.equal(droids.data.length,23); assert.equal(maps.data.length,18);
});

test('R44 direct-recovery beasts are searchable by id and title',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const sk=bridge.searchImageLibrary({query:'Skekfish',category:'creature',limit:50});
  const nr=bridge.searchImageLibrary({query:'Nerf',category:'creature',limit:50});
  assert.ok(sk.data.some(a=>a.assetId==='external.kainrath.beast.skekfish'));
  assert.ok(nr.data.some(a=>a.assetId==='external.kainrath.beast.nerf'));
});

test('R44 preserves R43 recovery set and older visual waves',()=>{
  const r43v=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R43_KAINRATH_VEHICLE_WAVE4_INDEX.json'),'utf8'));
  const r43d=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R43_KAINRATH_DROID_WAVE4_INDEX.json'),'utf8'));
  assert.deepEqual([r43v.length,r43d.length],[3,1]);
  for(const e of [...r43v,...r43d]) assert.ok(manifest.assets.some(a=>a.assetId===e.assetId));
  assert.equal(audit.blockedByDirectMediaProvenance.beasts_page1_without_direct_media_url_observed.includes('Skekfish'),false);
  assert.equal(audit.blockedByDirectMediaProvenance.beasts_page1_without_direct_media_url_observed.includes('Nerf'),false);
});
