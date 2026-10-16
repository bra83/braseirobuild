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
const freeze=JSON.parse(fs.readFileSync(path.join(root,'R46_KAINRATH_PROVENANCE_FREEZE.json'),'utf8'));
const embedded=JSON.parse(fs.readFileSync(path.join(vtt,'image-library/R46_KAINRATH_PROVENANCE_FREEZE.json'),'utf8'));
const r45=JSON.parse(fs.readFileSync(path.join(root,'R45_IMAGE_LIBRARY_V3_8_AUDIT.json'),'utf8'));
const mod=await import(pathToFileURL(path.join(vtt,'js/library/imageLibrary.mjs')).href);
const titles=new Set(manifest.assets.map(a=>a.title));
const blocked=[...freeze.deferred.vehicles,...freeze.deferred.beasts,...freeze.deferred.droids];

test('R46 freezes the validated V3.8 subset without count inflation',()=>{
  assert.equal(manifest.version,'3.8.0'); assert.deepEqual(embedded,freeze);
  assert.equal(manifest.counts.total,369); assert.equal(manifest.counts.local,148); assert.equal(manifest.counts.external,221);
  assert.equal(manifest.counts.vehicles,127); assert.equal(manifest.counts.droids,23); assert.equal(manifest.counts.creatures,17); assert.equal(manifest.counts.maps,18);
  assert.deepEqual(freeze.validatedCounts,{total:369,local:148,external:221,vehicles:127,droids:23,creatures:17,maps:18});
});

test('R46 keeps R45 unresolved provenance lists complete and deferred',()=>{
  assert.deepEqual(freeze.deferred.vehicles,r45.blockedByDirectMediaProvenance.vehicles);
  assert.deepEqual(freeze.deferred.beasts,r45.blockedByDirectMediaProvenance.beasts_page1_without_direct_media_url_observed);
  assert.deepEqual(freeze.deferred.droids,r45.blockedByDirectMediaProvenance.droids_missing_known_page_with_direct_media_not_observed);
  assert.equal(freeze.closure.unresolvedRemoteMediaBlocksGameplay,false);
  assert.equal(freeze.closure.unresolvedItemsAreDeferredOptionalVisualExtensions,true);
});

test('R46 does not silently index the specifically observed posts without direct media',()=>{
  const expected=new Set(['Void-Spider TX-3 Air Taxi','A-1 Deluxe Floater','XP-38A Landspeeder','Scavenger Tug','GS-100 Salvage Ship','Antiquated Battle Droid']);
  assert.deepEqual(new Set(freeze.observedPostsWithoutDirectMediaUrl.map(e=>e.title)),expected);
  for(const e of freeze.observedPostsWithoutDirectMediaUrl){
    assert.equal(e.status,'source_post_observed_direct_media_url_not_observed');
    assert.equal(titles.has(e.title),false,e.title);
    assert.ok(e.sourcePageUrl.startsWith('https://kainrath.wordpress.com/'));
  }
});

test('R46 policy forbids filename inference and preserves mechanical firewall',()=>{
  assert.equal(freeze.policy.inferRemoteFilename,false);
  assert.equal(freeze.policy.inferMechanicsFromImage,false);
  assert.equal(freeze.policy.indexOnlyDirectMediaActuallyObserved,true);
  assert.equal(freeze.policy.mechanicalBindingForUnvalidatedVisuals,null);
  assert.match(freeze.reason,/not sufficient evidence for a remote media filename/);
});

test('R46 every external remote-ready asset still obeys Kainrath allowlist and visual-only binding',()=>{
  const external=manifest.assets.filter(a=>a.sourceType==='external_reference' && a.remoteReady);
  assert.equal(external.length,221);
  for(const a of external){
    assert.equal(mod.isAllowedExternalImageUrl(a.remoteUrl,manifest.policy),true,a.assetId);
    assert.ok(a.remoteUrl.startsWith('https://kainrath.wordpress.com/wp-content/uploads/'),a.assetId);
    assert.equal(a.mechanicalBinding,null,a.assetId);
  }
});

test('R46 bridge remains gameplay-readable with frozen visual catalog and no repository mutation',()=>{
  const bridge=new BraseiroUiBridge({repository:{load(){return null;},save(){throw new Error('must not save');}}});
  const all=bridge.imageVisualEntityCatalog({kind:'all',limit:500});
  const vehicles=bridge.imageVisualEntityCatalog({kind:'vehicle',limit:500});
  const maps=bridge.imageMapCatalog({limit:500});
  assert.equal(all.ok,true); assert.equal(all.data.length,204);
  assert.equal(vehicles.ok,true); assert.equal(vehicles.data.length,127);
  assert.equal(maps.ok,true); assert.equal(maps.data.length,18);
});

test('R46 deferred provenance is duplicate-free and contains no synthetic URL field',()=>{
  assert.equal(new Set(blocked).size,blocked.length);
  for(const e of freeze.observedPostsWithoutDirectMediaUrl){
    assert.equal(Object.hasOwn(e,'remoteUrl'),false);
    assert.equal(Object.hasOwn(e,'canonicalRemoteUrl'),false);
    assert.equal(Object.hasOwn(e,'observedImageUrl'),false);
  }
});
