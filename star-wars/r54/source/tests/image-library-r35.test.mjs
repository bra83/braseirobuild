import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath,pathToFileURL} from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..');
const vtt=path.join(root,'app/src/main/assets/vtt');
const manifest=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/manifest.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);

test('image library manifest is valid and unique',()=>{
  const result=mod.validateImageManifest(manifest);
  assert.equal(result.ok,true,JSON.stringify(result.errors));
  assert.equal(result.count,manifest.counts.total);
  assert.equal(new Set(manifest.assets.map(a=>a.assetId)).size,manifest.counts.total);
});

test('all local runtime asset paths exist',()=>{
  for(const asset of manifest.assets.filter(a=>a.sourceType!=='external_reference')){
    assert.ok(asset.localPath,asset.assetId);
    assert.ok(fs.existsSync(path.join(vtt,asset.localPath)),asset.localPath);
  }
});

test('local token library has 143 individual tokens',()=>{
  const tokens=mod.searchImageAssets(manifest,{category:'token'});
  assert.equal(tokens.length,100); // default API limit is intentionally bounded
  const all=mod.searchImageAssets(manifest,{category:'token',limit:500});
  assert.equal(all.length,143);
  assert.ok(all.every(a=>a.width===96 && a.height===96));
});

test('search is category/tag based and does not invent mechanical binding',()=>{
  const droids=mod.searchImageAssets(manifest,{category:'token',subtype:'droids',limit:500});
  assert.equal(droids.length,12);
  assert.ok(droids.every(a=>a.mechanicalBinding===null));
  assert.equal(mod.chooseImageAsset(manifest,{category:'token',subtype:'droids'}).assetId,'local.token.droids.01');
});

test('equipment images remain visual-only until mechanical mapping is validated',()=>{
  const equipment=mod.searchImageAssets(manifest,{category:'equipment',limit:500});
  assert.equal(equipment.length,4);
  assert.ok(equipment.every(a=>a.usage==='visual_only' && a.mechanicalBinding===null));
});

test('Kainrath is an external indexed source, not silently copied into local runtime assets',()=>{
  const source=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/sources/kainrath.json'),'utf8'));
  assert.equal(source.status,'indexed_remote_catalog');
  assert.equal(source.defaultAssetPolicy,'reference_and_cache_on_demand');
  assert.equal(source.mapsCount,17);
  assert.equal(manifest.assets.filter(a=>a.sourceRef==='external.kainrath.wordpress' && a.category==='map').length,17);
});

import { BraseiroUiBridge } from '../app/src/main/assets/vtt/js/core/uiBridge.mjs';

test('BraseiroGameBridge exposes image library without requiring a campaign',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){}}});
  const info=bridge.imageLibraryInfo();
  assert.equal(info.ok,true);
  assert.equal(info.data.counts.tokens,143);
  const search=bridge.searchImageLibrary({category:'token',subtype:'vehicles',limit:500});
  assert.equal(search.ok,true);
  assert.equal(search.data.length,12);
  const one=bridge.imageAsset('local.map.galaxy');
  assert.equal(one.ok,true);
  assert.equal(one.data.localPath,'assets/maps/galaxy_map.jpg');
});

test('image bridge rejects unknown asset IDs without mutating state',()=>{
  let saved=false;
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){saved=true;}}});
  const result=bridge.imageAsset('missing.asset');
  assert.equal(result.ok,false);
  assert.equal(result.error,'IMAGE_ASSET_NOT_FOUND');
  assert.equal(saved,false);
});
