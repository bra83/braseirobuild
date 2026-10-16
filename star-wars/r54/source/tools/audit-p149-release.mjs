/** P149 — reproducible release preflight. Structural/mechanical, NOT a PDF or device certificate. */
import fs from 'node:fs';
import { publishedAdventureCatalog, getPublishedAdventureAdapter, publishedAdventureAdapterSnapshot } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import { publishedAdventureRuntimeModule } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import { validatePublishedAdventureGraph } from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import { validateAdventureSemantics } from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import { AdventureRuntime } from '../app/src/main/assets/vtt/js/adventure/adventureRuntime.mjs';
import { validateAdventureCheckpoint } from '../app/src/main/assets/vtt/js/adventure/adventureCheckpointGate.mjs';
import { aorTalentCoverage, aorTalentCatalog } from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
const root=new URL('../',import.meta.url);
const version=fs.readFileSync(new URL('app/build.gradle.kts',root),'utf8').match(/versionName\s*=\s*"([^"]+)"/)?.[1];
const bundle=fs.readFileSync(new URL('app/src/main/assets/vtt/js/core/uiBridge.bundle.js',root),'utf8');
const sourceChecks={version,androidBridgeBundled:bundle.includes('activatePendingDisorientTalent')&&bundle.includes('resolveIncomingSocialInfluenceCheck'),bundleVersion:bundle.match(/offline engine bundle — generated[^\n]*/)?.[0]||null};
const adventures=[];
for(const {id} of publishedAdventureCatalog()) {
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
 const structural=validatePublishedAdventureGraph(module),semantic=validateAdventureSemantics(module);
 let runtime=new AdventureRuntime(module);
 const actionId=`p149:${id}:audit`;
 const commands=[{type:'event',eventType:'P149_AUDIT',payload:{id}}];
 const applied=runtime.commitAction(actionId,commands).applied;
 for(let n=0;n<3;n++) {
  runtime=AdventureRuntime.restore(module,runtime.serialize());
  if(runtime.commitAction(actionId,commands).applied) throw Error(`Replay duplicou comando: ${id}`);
  validateAdventureCheckpoint(module,runtime.getState());
 }
 adventures.push({id,structural,semantic,capabilities:publishedAdventureAdapterSnapshot(id).capabilities,appliedOnce:applied,reopens:3});
}
const aor=aorTalentCoverage(),pendingByTalent=Object.entries(aorTalentCatalog().filter(x=>!x.mechanicallyActive).reduce((m,x)=>(m[x.originalName]=(m[x.originalName]||0)+1,m),{})).sort((a,b)=>b[1]-a[1]);
const allPassed=version==='1.0.149'&&sourceChecks.androidBridgeBundled&&adventures.length===5&&adventures.every(a=>a.appliedOnce&&a.reopens===3)&&aor.mechanicalPositions===229;
const result={checkpoint:'R54_P149',allPassed,certifiedAgainstOfficialPdfs:false,androidApkCompiled:false,androidDeviceHomologated:false,sourceChecks,adventures,aor:{...aor,pendingPositions:aor.positions-aor.mechanicalPositions,pendingByTalent}};
console.log(JSON.stringify(result,null,2));
if(!allPassed)process.exitCode=1;
