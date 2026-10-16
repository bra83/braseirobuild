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
const wave=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R39_KAINRATH_VISUAL_WAVE3_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.entries.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R39 baseline remains present in current additive image-library manifest',()=>{
  assert.ok(Number(manifest.version.split('.')[0])>=3);
  assert.ok(manifest.counts.total>=239);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,ids.length);
});

test('R39 visual wave3 contains exactly 10 confirmed assets',()=>{
  assert.equal(wave.wave,'R39_VISUAL_WAVE3');
  assert.equal(wave.count,10);
  assert.equal(waveAssets.length,10);
  assert.deepEqual(wave.breakdown,{npcPortraits:3,droids:0,beasts:7});
});

test('R39 totals add 3 NPC portraits and 7 beasts without changing maps or droids',()=>{
  assert.equal(manifest.counts.local,148);
  assert.ok(manifest.counts.external>=91);
  assert.equal(manifest.counts.maps,18);
  assert.ok(manifest.counts.portraits>=37);
  assert.ok(manifest.counts.droids>=22);
  assert.ok(manifest.counts.creatures>=15);
  assert.ok(manifest.counts.kainrathVisualEntities>=74);
});

test('R39 direct URLs obey the existing Kainrath whitelist and cache contract',()=>{
  for(const a of waveAssets){
    assert.equal(a.sourceType,'external_reference');
    assert.equal(a.remoteReady,true);
    assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'));
  }
});

test('R39 assets remain visual-only and cannot infer mechanics',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
    assert.ok(['portrait','creature'].includes(a.category));
  }
});

test('R39 source metadata keeps prior waves and records wave3 separately',()=>{
  assert.equal(source.npcWave2Count,20);
  assert.ok(source.visualEntityEntries.length>=44);
  assert.equal(source.visualWave3Count,10);
  assert.equal(source.visualWave3Entries.length,10);
  assert.ok(source.visualEntitiesCount>=74);
});

test('generic visual bridge automatically exposes R39 expanded totals',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const npcs=bridge.imageVisualEntityCatalog({kind:'npc',limit:500});
  const droids=bridge.imageVisualEntityCatalog({kind:'droid',limit:500});
  const beasts=bridge.imageVisualEntityCatalog({kind:'beast',limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=74);
  assert.equal(npcs.ok,true); assert.ok(npcs.data.length>=37);
  assert.equal(droids.ok,true); assert.ok(droids.data.length>=22);
  assert.equal(beasts.ok,true); assert.ok(beasts.data.length>=15);
});

test('R39 visual expansion leaves map catalog exactly unchanged',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(maps.ok,true);
  assert.equal(maps.data.length,18);
});
