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
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const visual=manifest.assets.filter(a=>a.sourceRef==='external.kainrath.wordpress' && ['portrait','droid','creature'].includes(a.category));
const r37Ids=new Set(source.visualEntityEntries.map(x=>x.assetId));
const r37Visual=manifest.assets.filter(a=>r37Ids.has(a.assetId) && !a.indexWave);

test('R37 image library V3 adds 44 visual entities without losing R36 assets',()=>{
  assert.equal(manifest.libraryId,'braseiro.starwars.image-library.v3');
  assert.equal(manifest.counts.local,148);
  assert.equal(manifest.counts.maps,18);
  assert.ok(manifest.counts.total>=209);
  assert.ok(manifest.counts.external>=61);
  assert.equal(r37Visual.length,44);
});

test('R37 Kainrath visual entity index is complete and unique',()=>{
  assert.ok(source.visualEntityEntries.length>=44);
  assert.equal(new Set(source.visualEntityEntries.map(x=>x.assetId)).size,source.visualEntityEntries.length);
  assert.equal(r37Visual.length,44);
  assert.equal(r37Visual.filter(a=>a.category==='portrait').length,16);
  assert.equal(r37Visual.filter(a=>a.category==='droid').length,20);
  assert.equal(r37Visual.filter(a=>a.category==='creature').length,8);
});

test('all R37 visual entities are visual-only and use allowed cache URLs',()=>{
  for(const asset of r37Visual){
    assert.equal(asset.sourceType,'external_reference');
    assert.equal(asset.mechanicalBinding,null,asset.assetId);
    assert.equal(asset.usage,'visual_reference');
    assert.equal(asset.remoteReady,true);
    assert.equal(asset.offlineReady,false);
    assert.equal(asset.cachePolicy,'explicit_on_demand');
    assert.equal(mod.isAllowedExternalImageUrl(asset.remoteUrl,manifest.policy),true,asset.remoteUrl);
    assert.ok(asset.sourcePageUrl.startsWith('https://kainrath.wordpress.com/'));
  }
});

test('R37 categories contain exactly 16 NPC portraits 20 droids and 8 beasts',()=>{
  assert.equal(r37Visual.filter(a=>a.category==='portrait'&&a.subtype==='npc').length,16);
  assert.equal(r37Visual.filter(a=>a.category==='droid').length,20);
  assert.equal(r37Visual.filter(a=>a.category==='creature'&&a.subtype==='beast').length,8);
});

test('R37 visual entity bridge searches all/npc/droid/beast without campaign state',()=>{
  let saved=false;
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){saved=true;}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const npcs=bridge.imageVisualEntityCatalog({kind:'npc',limit:500});
  const droids=bridge.imageVisualEntityCatalog({kind:'droid',limit:500});
  const beasts=bridge.imageVisualEntityCatalog({kind:'beast',limit:500});
  assert.equal(all.ok,true); assert.ok(all.data.length>=44);
  assert.equal(npcs.ok,true); assert.ok(npcs.data.length>=16);
  assert.equal(droids.ok,true); assert.ok(droids.data.length>=20);
  assert.equal(beasts.ok,true); assert.ok(beasts.data.length>=8);
  assert.equal(saved,false);
});

test('R37 visual entity bridge rejects invalid kind without mutation',()=>{
  let saved=false;
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){saved=true;}}});
  const result=bridge.imageVisualEntityCatalog({kind:'weapon'});
  assert.equal(result.ok,false);
  assert.equal(result.error,'INVALID_INPUT');
  assert.equal(saved,false);
});

test('named searches resolve real visual references without mechanical inference',()=>{
  const cratala=mod.chooseImageAsset(manifest,{category:'portrait',query:'Cratala'});
  const surgical=mod.chooseImageAsset(manifest,{category:'droid',query:'2-1B'});
  const krayt=mod.chooseImageAsset(manifest,{category:'creature',query:'Krayt Dragon'});
  assert.equal(cratala.title,'Cratala');
  assert.equal(surgical.title,'2-1B Surgical Droid');
  assert.equal(krayt.title,'Juvenile Krayt Dragon');
  for(const asset of [cratala,surgical,krayt]) assert.equal(asset.mechanicalBinding,null);
});

test('R37 visual entities do not pollute the 18-map catalog',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(maps.ok,true);
  assert.equal(maps.data.length,18);
  assert.ok(maps.data.every(a=>a.category==='map'));
});

test('real static Android AJUSTES exposes visual entity search preview cache and provenance',()=>{
  const html=fs.readFileSync(path.join(vtt,'index.html'),'utf8');
  const app=fs.readFileSync(path.join(vtt,'js/ui/app.mjs'),'utf8');
  for(const id of ['imageVisualSearch','imageVisualKind','imageVisualCatalog','imageVisualSelectedTitle','imageVisualPreview','imageVisualSource','imageVisualCache','imageVisualClearCache','imageVisualRefresh']) assert.ok(html.includes(`id="${id}"`),id);
  assert.match(app,/visualEntityAssets/);
  assert.match(app,/cacheSelectedVisualAsset/);
  assert.match(app,/mechanical|vínculo mecânico/i);
});
