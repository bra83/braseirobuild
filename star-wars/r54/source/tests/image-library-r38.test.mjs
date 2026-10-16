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
const wave=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R38_KAINRATH_NPC_WAVE2_INDEX.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const ids=manifest.assets.map(a=>a.assetId);
const waveIds=new Set(wave.entries.map(e=>e.assetId));
const waveAssets=manifest.assets.filter(a=>waveIds.has(a.assetId));

test('R38 manifest grows exactly to 229 assets with unique IDs',()=>{
  assert.ok(manifest.counts.total>=229);
  assert.equal(ids.length,manifest.counts.total);
  assert.equal(new Set(ids).size,ids.length);
});

test('R38 wave adds exactly 20 entries from Enemies and NPCs page 3',()=>{
  assert.equal(wave.wave,'R38_NPC_PAGE3');
  assert.equal(wave.count,20);
  assert.equal(wave.entries.length,20);
  assert.equal(new Set(wave.entries.map(e=>e.assetId)).size,20);
  assert.equal(waveAssets.length,20);
  assert.equal(wave.breakdown.npcPortraits,18);
  assert.equal(wave.breakdown.droids,2);
});

test('R38 totals preserve R37 categories and add 18 NPCs plus 2 droids',()=>{
  assert.equal(manifest.counts.local,148);
  assert.ok(manifest.counts.external>=81);
  assert.ok(manifest.counts.remoteReady>=81);
  assert.equal(manifest.counts.maps,18);
  assert.ok(manifest.counts.portraits>=34);
  assert.ok(manifest.counts.droids>=22);
  assert.ok(manifest.counts.creatures>=8);
  assert.ok(manifest.counts.kainrathVisualEntities>=64);
});

test('all R38 wave URLs pass the same Kainrath external-image security policy',()=>{
  for(const a of waveAssets){
    assert.equal(a.sourceType,'external_reference');
    assert.equal(a.remoteReady,true);
    assert.equal(a.offlineReady,false);
    assert.equal(a.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.remoteUrl);
    assert.ok(a.sourcePageUrl.endsWith('/category/enemies-and-npcs/page/3/'));
  }
});

test('R38 wave remains visual-only with no mechanical inference',()=>{
  for(const a of waveAssets){
    assert.equal(a.mechanicalBinding,null,a.assetId);
    assert.equal(a.usage,'visual_reference');
    assert.ok(['portrait','droid'].includes(a.category));
  }
});

test('R38 source metadata records the second NPC wave without overwriting R37 entries',()=>{
  assert.equal(source.npcWave2Count,20);
  assert.equal(source.npcWave2Entries.length,20);
  assert.ok(source.visualEntitiesCount>=64);
  assert.ok(source.npcsCount>=34);
  assert.ok(source.droidsCount>=22);
  assert.ok(source.beastsCount>=8);
  assert.ok(source.visualEntityEntries.length>=44);
});

test('visual entity bridge automatically exposes expanded R38 totals without API changes',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const npcs=bridge.imageVisualEntityCatalog({kind:'npc',limit:500});
  const droids=bridge.imageVisualEntityCatalog({kind:'droid',limit:500});
  const beasts=bridge.imageVisualEntityCatalog({kind:'beast',limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=64);
  assert.equal(npcs.ok,true); assert.ok(npcs.data.length>=34);
  assert.equal(droids.ok,true); assert.ok(droids.data.length>=22);
  assert.equal(beasts.ok,true); assert.ok(beasts.data.length>=8);
});

test('R38 expansion does not alter the 18-map catalog',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(maps.ok,true);
  assert.equal(maps.data.length,18);
});
