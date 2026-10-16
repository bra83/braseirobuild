/** P155 release gate: code/runtime/asset consistency; not official PDF or APK certification. */
import fs from 'node:fs';import crypto from 'node:crypto';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter,publishedAdventureAdapterSnapshot} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {startBeyondTheRim,beyondTheRimEncounterSpec} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
const read=f=>fs.readFileSync(new URL('../'+f,import.meta.url),'utf8');
const gradle=read('app/build.gradle.kts'),shell=read('app/src/main/assets/vtt/r46-shell.html'),bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js'),runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const javaBaseline=JSON.parse(read('evidence/P151/JAVA_P150_BASELINE_SHA256.json'));
const javaFiles=Object.entries(javaBaseline).map(([file,sha])=>({file,unchanged:crypto.createHash('sha256').update(fs.readFileSync(new URL('../'+file,import.meta.url))).digest('hex')===sha}));
const version=/versionCode\s*=\s*10155/.test(gradle)&&/versionName\s*=\s*"1\.0\.155"/.test(gradle)&&shell.includes('data-version="1.0.155"')&&bundle.includes('FINAL 1.0.155 offline engine bundle');
const ui=['useUtilityBelt','utilityBeltStatus','data-p155-utility-use','beginPublishedAdventureEncounter'].every(key=>runtime.includes(key)||bundle.includes(key));
const coverage=aorTalentCoverage(),utilitySlots=aorTalentCatalog().filter(n=>n.originalName==='CINTURÓN DE ACCESORIOS'&&n.mechanicallyActive).length;
const btr=startBeyondTheRim({}).world;
const encounterIds=['btr-yiyar-hangar','btr-yiyar-jungle','btr-imperial-interception'];
const encounters=encounterIds.map((id,index)=>{const node=['droid-delivery','rival-convergence','imperial-interruption'][index];const w=structuredClone(btr);w.publishedAdventure.currentNodeId=node;const spec=beyondTheRimEncounterSpec(w,id);return {node,id,profile:spec.adversaries[0].profileKey,authority:spec.implementationAuthority,valid:spec.id===id&&spec.adversaries.length>0&&spec.implementationAuthority==='BRASEIRO_PROVISIONAL_NOT_PDF_CERTIFIED'};});
const adventures=[];
for(const {id} of publishedAdventureCatalog()){
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta),structural=validatePublishedAdventureGraph(module),semantic=validateAdventureSemantics(module);
 class Repo{constructor(){this.saved=createCampaign('p155-audit:'+id);this.writes=0;}load(){return structuredClone(this.saved);}save(x){this.saved=structuredClone(x);this.writes++;}}
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');const startAction=`p155:audit:${id}:start`;
 const start=g.executePublishedAdventureCommand({actionId:startAction,kind:'start',adventureId:id});
 const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;
 const choice=choiceId?g.executePublishedAdventureCommand({actionId:`p155:audit:${id}:choice`,kind:'choice',adventureId:id,choiceId}):null;
 const before=structuredClone(repo.saved),writes=repo.writes;
 let reloads=0;
 for(let i=0;i<3;i++){
  const reopened=new GameController(repo),dup=reopened.executePublishedAdventureCommand({actionId:startAction,kind:'start',adventureId:id});
  if(dup.duplicate&&repo.writes===writes&&JSON.stringify(repo.saved)===JSON.stringify(before)&&validateState(repo.saved))reloads++;
 }
 adventures.push({id,structural,semantic,start:Boolean(start.applied),choice:Boolean(choice?.applied),reloads,encounters:publishedAdventureAdapterSnapshot(id).capabilities.encounters});
}
const report={checkpoint:'P155',version,ui,coverage,utilitySlots,javaFiles,encounters,adventures,officialPdfCertified:false,apkBuilt:false};
report.allPassed=version&&ui&&coverage.positions===360&&coverage.mechanicalPositions===254&&utilitySlots===2&&javaFiles.every(x=>x.unchanged)&&encounters.every(x=>x.valid)&&adventures.length===5&&adventures.every(x=>x.structural&&x.semantic&&x.start&&x.choice&&x.reloads===3&&x.encounters);
fs.mkdirSync(new URL('../evidence/P155/',import.meta.url),{recursive:true});fs.writeFileSync(new URL('../evidence/P155/RELEASE_AUDIT.json',import.meta.url),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({allPassed:report.allPassed,coverage,utilitySlots,encounters:encounters.length,adventures:adventures.length,javaUnchanged:javaFiles.length,version,ui}));
if(!report.allPassed)process.exitCode=1;
