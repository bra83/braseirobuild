import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { NativePersistencePort } from '../app/src/main/assets/www/core/persistence-port.js';

test('NativePersistencePort caches verified campaign loads and does not fetch snapshots by default',()=>{
  let loads=0,snapshots=0,commits=0;
  const native={
    loadCampaign(id){loads++;return JSON.stringify({status:'OK',campaignId:id,displayName:'X',revision:2,stateJson:'{"revision":2}',checksum:'abc',rngAlgorithm:'r',masterSeed:'s'})},
    listSnapshots(){snapshots++;return JSON.stringify({status:'OK',snapshots:[{snapshotId:'s'}]})},
    commit(raw){commits++;const r=JSON.parse(raw);return JSON.stringify({status:'OK',newRevision:r.expectedRevision+1,stateChecksum:'def'})},
    listEvents(){return JSON.stringify({status:'OK',events:[]})},
  };
  const p=new NativePersistencePort(native);
  assert.equal(p.loadCampaign('c').revision,2);
  assert.equal(p.loadCampaign('c').revision,2);
  assert.equal(loads,1);
  assert.equal(snapshots,0);
  assert.equal(p.loadCampaign('c',{includeSnapshots:true}).snapshots.length,1);
  assert.equal(snapshots,1);
  const receipt=p.commit({campaignId:'c',expectedRevision:2,newStateJson:'{"revision":3}',eventId:'e',eventKind:'X',eventPayloadJson:'{}'});
  assert.equal(receipt.status,'OK'); assert.equal(commits,1);
  assert.equal(p.loadCampaign('c').revision,3); assert.equal(loads,1);
});

test('Atlas pan uses transform-only fog update and selection avoids rebuilding the whole map',()=>{
  const src=fs.readFileSync(new URL('../app/src/main/assets/www/scr014.js',import.meta.url),'utf8');
  assert.match(src,/fogRevealMask\.setAttribute\('transform'/);
  assert.match(src,/scheduleApply\(\)/);
  assert.doesNotMatch(src,/function apply\(\)\{[^}]*refreshFogMask\(/);
  assert.match(src,/canvas\.querySelector\('\.hex\.selected'\)\?\.classList\.remove\('selected'\)/);
});

test('native campaign repository has verified load cache and targeted event lookup',()=>{
  const repo=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/wwn/data/CampaignRepository.kt',import.meta.url),'utf8');
  const dao=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/wwn/data/BraseiroDao.kt',import.meta.url),'utf8');
  const bridge=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/wwn/bridge/BraseiroBridge.kt',import.meta.url),'utf8');
  assert.match(repo,/verifiedLoadCache/);
  assert.match(repo,/fun getEvent/);
  assert.match(repo,/fun latestNarrative/);
  assert.match(dao,/latestNarrativeByPrefix/);
  assert.match(bridge,/fun getEvent/);
  assert.match(bridge,/fun latestNarrative/);
});

test('large scene images are normalized before persistent WebView display',()=>{
  const src=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/wwn/bridge/SceneImageRuntime.kt',import.meta.url),'utf8');
  assert.match(src,/MAX_STORED_DIMENSION = 2048/);
  assert.match(src,/MAX_STORED_PIXELS = 4_000_000L/);
  assert.match(src,/normalizeForMobile/);
  assert.match(src,/downscaled/);
});


test('procedural audio synthesis stays off the Android main thread',()=>{
  const src=fs.readFileSync(new URL('../app/src/main/java/com/braseiro/wwn/bridge/NativeAudioRuntime.kt',import.meta.url),'utf8');
  assert.match(src,/loopSynthesisExecutor/);
  assert.match(src,/sfxSynthesisExecutor/);
  assert.match(src,/requestLoop\("ambience", asset\)/);
  assert.match(src,/requestOneShot\(asset\)/);
  assert.doesNotMatch(src,/main\.post \{ playLoop/);
  assert.doesNotMatch(src,/main\.post \{ playOneShot/);
  assert.match(src,/shutdownNow\(\)/);
});
