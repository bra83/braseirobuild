/** P151 reproducible mechanical/structural audit. No claim of official-PDF or APK certification. */
import fs from 'node:fs';
import crypto from 'node:crypto';
import { publishedAdventureCatalog, getPublishedAdventureAdapter } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { validatePublishedAdventureGraph } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { GameController } from '../app/src/main/assets/vtt/js/core/controller.mjs';
import { createCampaign, validateState } from '../app/src/main/assets/vtt/js/core/state.mjs';
import { aorTalentCoverage, aorTalentCatalog } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
const root=new URL('../',import.meta.url),read=path=>fs.readFileSync(new URL(path,root),'utf8');
const version=read('app/build.gradle.kts').match(/versionName\s*=\s*"([^"]+)"/)?.[1];
const shell=read('app/src/main/assets/vtt/r46-shell.html');
const bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
const javaBase=JSON.parse(read('evidence/P151/JAVA_P150_BASELINE_SHA256.json'));
const javaUnchanged=Object.entries(javaBase).every(([file,sha])=>
  crypto.createHash('sha256').update(fs.readFileSync(new URL(file,root))).digest('hex')===sha);
const bridgeMethods=['resolveDirectedDroidSkillCheck','plotHyperspaceCourse','resolveIncomingComputerIntrusionCheck','resolveIncomingVehicleAttack',
  'activateIncomingVehicleCritical','declineIncomingVehicleCritical'];
const sourceChecks={version,shellVersion:shell.includes('data-version="1.0.151"'),
  bundleVersion:bundle.includes('FINAL 1.0.151 offline engine bundle'),
  bridgeMethodsBundled:bridgeMethods.every(method=>bundle.includes(method)),
  javaUnchanged,javaSources:Object.keys(javaBase).length};
const adventures=[];
for(const {id} of publishedAdventureCatalog()) {
  const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
  const structural=validatePublishedAdventureGraph(module),semantic=validateAdventureSemantics(module);
  class Repo{constructor(){this.saved=createCampaign(`p151:audit:${id}`);this.writes=0;}
    load(){return structuredClone(this.saved);}save(value){this.saved=structuredClone(value);this.writes++;}}
  const repo=new Repo();let game=new GameController(repo);game.loadStarterCharacter('cael');
  const startId=`p151:${id}:start`;
  const started=game.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id});
  const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;
  let moved=false;
  if(choiceId){const actionId=`p151:${id}:choice`;
    moved=game.executePublishedAdventureCommand({actionId,kind:'choice',adventureId:id,choiceId}).applied;
  }
  const before=structuredClone(repo.saved),writes=repo.writes;
  for(let n=0;n<3;n++){
    game=new GameController(repo);
    const replay=game.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id});
    // After a choice, start proof remains in current run. No writes on replay.
    if(!replay.duplicate || repo.writes!==writes || JSON.stringify(repo.saved)!==JSON.stringify(before))
      throw Error(`Idempotent replay failed: ${id}`);
    validateState(repo.saved);
  }
  adventures.push({id,structural,semantic,started:started.applied,choiceApplied:moved,
    reopens:3,sourcePdfCompared:false});
}
const coverage=aorTalentCoverage();
const pending=aorTalentCatalog().filter(row=>!row.mechanicallyActive);
const result={checkpoint:'R54_P151',base:'R54_P150',sourceChecks,
  adventures,aor:{...coverage,pendingPositions:pending.length},
  certifiedAgainstOfficialPdfs:false,androidApkCompiled:false,androidDeviceHomologated:false,
  allPassed:version==='1.0.151'&&sourceChecks.shellVersion&&sourceChecks.bundleVersion&&
    sourceChecks.bridgeMethodsBundled&&sourceChecks.javaUnchanged&&sourceChecks.javaSources===8&&
    adventures.length===5&&adventures.every(a=>a.started&&a.choiceApplied&&a.reopens===3)&&
    coverage.positions===360&&coverage.mechanicalPositions===247&&pending.length===113};
console.log(JSON.stringify(result,null,2));
if(!result.allPassed)process.exitCode=1;
