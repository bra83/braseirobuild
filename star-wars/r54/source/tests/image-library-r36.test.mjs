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
const java=fs.readFileSync(path.join(root,'app/src/main/java/com/braseiro/starwarsedge/ImageLibraryBridge.java'),'utf8');
const activity=fs.readFileSync(path.join(root,'app/src/main/java/com/braseiro/starwarsedge/MainActivity.java'),'utf8');
const html=fs.readFileSync(path.join(vtt,'index.html'),'utf8');
const app=fs.readFileSync(path.join(vtt,'js/ui/app.mjs'),'utf8');

const external=manifest.assets.filter(a=>a.sourceType==='external_reference');
const externalMaps=manifest.assets.filter(a=>a.sourceType==='external_reference' && a.category==='map');
const maps=manifest.assets.filter(a=>a.category==='map');

test('R36 image library V2 has expected local/external/map counts',()=>{
  assert.equal(manifest.schemaVersion,2);
  assert.ok(['braseiro.starwars.image-library.v2','braseiro.starwars.image-library.v3'].includes(manifest.libraryId));
  assert.equal(manifest.counts.local,148);
  assert.equal(manifest.counts.tokens,143);
  assert.equal(manifest.counts.equipment,4);
  assert.equal(manifest.counts.maps,18);
  assert.equal(externalMaps.length,17);
  assert.equal(maps.length,18);
});

test('all 17 Kainrath maps use allowed HTTPS wp-content URLs',()=>{
  assert.equal(externalMaps.length,17);
  for(const asset of externalMaps){
    assert.equal(asset.sourceRef,'external.kainrath.wordpress');
    assert.equal(asset.remoteReady,true);
    assert.equal(mod.isAllowedExternalImageUrl(asset.remoteUrl,manifest.policy),true,asset.remoteUrl);
    const url=new URL(asset.remoteUrl);
    assert.equal(url.hostname,'kainrath.wordpress.com');
    assert.ok(url.pathname.startsWith('/wp-content/uploads/'));
  }
});

test('external maps are reference/cache assets and never masquerade as packaged mechanics',()=>{
  for(const asset of externalMaps){
    assert.equal(asset.localPath,undefined);
    assert.equal(asset.mechanicalBinding,null);
    assert.equal(asset.offlineReady,false);
    assert.equal(asset.cachePolicy,'explicit_on_demand');
    assert.ok(asset.sourcePageUrl.startsWith('https://kainrath.wordpress.com/'));
  }
});

test('map catalog covers useful map kinds without inferring campaign state',()=>{
  const kinds=new Set(maps.map(a=>a.mapKind));
  for(const kind of ['galaxy','planet','settlement','district','streets','location','interior','ship']) assert.ok(kinds.has(kind),kind);
  assert.ok(maps.every(a=>a.mechanicalBinding===null));
});

test('Kainrath source index contains the complete Maps batch',()=>{
  assert.equal(source.status,'indexed_remote_catalog');
  assert.equal(source.mapsCount,17);
  assert.equal(source.mapEntries.length,17);
  assert.equal(new Set(source.mapEntries.map(e=>e.assetId)).size,17);
  assert.ok(source.mapsCategoryUrl.includes('/category/maps/'));
});

test('game bridge exposes map catalog and runtime descriptors without campaign',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const catalog=bridge.imageMapCatalog({limit:500});
  assert.equal(catalog.ok,true);
  assert.equal(catalog.data.length,18);
  assert.ok(catalog.data.some(a=>a.title.includes('Tatooine')));
  const local=bridge.imageAssetRuntime('local.map.galaxy');
  assert.equal(local.ok,true); assert.equal(local.data.mode,'packaged'); assert.equal(local.data.offlineReady,true);
  const remote=bridge.imageAssetRuntime('external.kainrath.map.hoth-planet');
  assert.equal(remote.ok,true); assert.equal(remote.data.mode,'external_cache_required'); assert.equal(remote.data.remoteReady,true);
});

test('native image cache is constrained by host path size MIME and redirect guards',()=>{
  assert.match(java,/ALLOWED_HOST\s*=\s*"kainrath\.wordpress\.com"/);
  assert.match(java,/ALLOWED_PATH_PREFIX\s*=\s*"\/wp-content\/uploads\/"/);
  assert.match(java,/MAX_REMOTE_BYTES\s*=\s*25L \* 1024L \* 1024L/);
  assert.match(java,/MAX_REDIRECTS\s*=\s*3/);
  assert.match(java,/image\/png.*image\/jpeg.*image\/webp/s);
  assert.match(java,/setInstanceFollowRedirects\(false\)/);
  assert.match(java,/validateRemoteUri\(next\.toString\(\)\)/);
});

test('native cache is private and served through braseiro.local rather than exposing file paths',()=>{
  assert.match(java,/getFilesDir\(\), "image-library-cache"/);
  assert.match(java,/RUNTIME_HOST\s*=\s*"braseiro\.local"/);
  assert.match(java,/RUNTIME_PREFIX\s*=\s*"\/library-image\/"/);
  assert.match(java,/sha256/);
});

test('MainActivity installs BraseiroImages and preserves scene image interceptor order',()=>{
  assert.match(activity,/new ImageLibraryBridge\(this, webView\)/);
  assert.match(activity,/addJavascriptInterface\(imageLibraryBridge, "BraseiroImages"\)/);
  const scene=activity.indexOf('geminiBridge.openSceneImageResponse');
  const library=activity.indexOf('imageLibraryBridge.openCachedImageResponse');
  assert.ok(scene>=0 && library>scene);
});

test('real static Android MAPA surface exposes search selection cache and provenance controls',()=>{
  for(const id of ['imageMapSearch','imageMapCatalog','imageMapSelectedTitle','imageMapSource','imageMapCache','imageMapClearCache','imageMapRefresh']) assert.ok(html.includes(`id="${id}"`),id);
  assert.match(app,/IMAGE_LIBRARY_MANIFEST/);
  assert.match(app,/searchImageAssets/);
  assert.match(app,/cacheRemoteImage/);
  assert.match(app,/clearCachedAsset/);
  assert.match(app,/Selecionar|Seleção visual|não altera posição/i);
});

test('external URL validator rejects arbitrary hosts and non-HTTPS schemes',()=>{
  assert.equal(mod.isAllowedExternalImageUrl('https://evil.example/wp-content/uploads/x.png',manifest.policy),false);
  assert.equal(mod.isAllowedExternalImageUrl('http://kainrath.wordpress.com/wp-content/uploads/x.png',manifest.policy),false);
  assert.equal(mod.isAllowedExternalImageUrl('https://kainrath.wordpress.com/not-allowed/x.png',manifest.policy),false);
});

test('map search is deterministic and bounded',()=>{
  const found=mod.searchImageAssets(manifest,{category:'map',query:'mos shuuta',limit:500});
  assert.ok(found.length>=7);
  assert.ok(found.every(a=>a.category==='map'));
  const chosen=mod.chooseImageAsset(manifest,{category:'map',query:'hoth'});
  assert.equal(chosen.assetId,'external.kainrath.map.hoth-planet');
});
