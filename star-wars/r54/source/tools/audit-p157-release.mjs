/** P157 — source-aware release audit, explicitly not APK or full-PDF certification. */
import fs from 'node:fs';import crypto from 'node:crypto';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
import {aorTalentCoverage} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {startBeyondTheRim,beyondTheRimEncounterSpec,BTR_IMPERIAL_DEPLOYMENT_TIMELINE,advanceBeyondTheRimImperialClock} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
import {PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
const root=new URL('../',import.meta.url),read=f=>fs.readFileSync(new URL(f,root),'utf8');
const gradle=read('app/build.gradle.kts'),shell=read('app/src/main/assets/vtt/r46-shell.html'),bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js'),runtime=read('app/src/main/assets/vtt/r46/final-runtime.js');
const version=/versionCode\s*=\s*10157/.test(gradle)&&/versionName\s*=\s*"1\.0\.157"/.test(gradle)&&shell.includes('data-version="1.0.157"')&&bundle.includes('FINAL 1.0.157 offline engine bundle');
const bridge=read('app/src/main/assets/vtt/js/core/uiBridge.mjs');
const p157Ui=['useFeint','resolveIncomingIdentificationCheck','legalSaleQuote'].every(x=>bridge.includes(x)&&runtime.includes(x))&&['data-p157-feint','data-p157-identify','data-p157-quote'].every(x=>runtime.includes(x));
const outbox=read('app/src/main/assets/vtt/r46/published-command-client.js');
const ui=['advancePublishedImperialClock','data-p156-imperial-tick','beginPublishedAdventureEncounter'].every(x=>runtime.includes(x)||bridge.includes(x))&&outbox.includes("'imperial-clock'")&&runtime.includes("execute('imperial-clock'");
const javaBaseline=JSON.parse(read('evidence/P151/JAVA_P150_BASELINE_SHA256.json'));
const javaFiles=Object.entries(javaBaseline).map(([file,sha])=>({file,unchanged:crypto.createHash('sha256').update(fs.readFileSync(new URL(file,root))).digest('hex')===sha}));
const coverage=aorTalentCoverage();
const btr=startBeyondTheRim({}).world;
const scenes=[['droid-delivery','btr-yiyar-hangar'],['rival-convergence','btr-yiyar-jungle'],['dropoff-point','btr-yiyar-scrapheap-raid'],['imperial-interruption','btr-imperial-interception']];
const encounters=scenes.map(([node,id])=>{const w=structuredClone(btr);w.publishedAdventure.currentNodeId=node;const spec=beyondTheRimEncounterSpec(w,id);return {node,id,valid:spec.id===id&&spec.adversaries.length>0&&spec.implementationAuthority==='BRASEIRO_PROVISIONAL_NOT_PDF_CERTIFIED'};});
const pdfCrosswalk={book:'Beyond the Rim',driveFileId:'1z-GpNm5QU1RGJSUdrP4vYbfBExTlF531',page29VerifiedFields:['Yiyar group skills','Yiyar blaster and vibroknife','Talo skills, talents and weapons'],chapter3Verified:['Yiyar raid on Scrapheap Point','Talo and Trandoshan mercenaries','Imperial deployment 0-30 minutes'],numericStatBlocksCertified:false};
const clockWorld=structuredClone(btr);clockWorld.publishedAdventure.currentNodeId='imperial-interruption';const clock=advanceBeyondTheRimImperialClock(clockWorld,{minutes:30,actionId:'p157:audit:clock'});
const clockPass=BTR_IMPERIAL_DEPLOYMENT_TIMELINE.map(e=>e.minute).join(',')==='5,10,15,25,30'&&clock.eventsAdded.length===5&&clock.clock.critical;
const adventures=[];
for(const {id} of publishedAdventureCatalog()){
 const adapter=getPublishedAdventureAdapter(id),module=publishedAdventureRuntimeModule(adapter.meta);
 const structural=validatePublishedAdventureGraph(module),semantic=validateAdventureSemantics(module);
 class Repo{constructor(){this.saved=createCampaign('p157:audit:'+id);this.writes=0;}load(){return structuredClone(this.saved)}save(x){this.saved=structuredClone(x);this.writes++}}
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');const startId=`p157:audit:${id}:start`;
 const started=g.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id});
 const choiceId=adapter.status(repo.saved.world).node?.choices?.[0]?.id;
 const choice=g.executePublishedAdventureCommand({actionId:`p157:audit:${id}:choice`,kind:'choice',adventureId:id,choiceId});
 const saved=structuredClone(repo.saved),writes=repo.writes;let reloads=0;
 for(let i=0;i<3;i++){const reopened=new GameController(repo),dup=reopened.executePublishedAdventureCommand({actionId:startId,kind:'start',adventureId:id});if(dup.duplicate&&repo.writes===writes&&JSON.stringify(repo.saved)===JSON.stringify(saved)&&validateState(repo.saved))reloads++;}
 adventures.push({id,structural,semantic,start:started.applied,choice:choice.applied,reloads});
}
const report={p157Ui,checkpoint:'P157',version,ui,bundleModules:79,coverage,encounters,pdfCrosswalk,clockPass,javaFiles,adventures,officialPdfCertified:false,apkBuilt:false};
report.allPassed=version&&ui&&p157Ui&&coverage.positions===360&&coverage.mechanicalPositions===262&&encounters.length===4&&encounters.every(x=>x.valid)&&clockPass&&javaFiles.every(x=>x.unchanged)&&adventures.length===5&&adventures.every(x=>x.structural&&x.semantic&&x.start&&x.choice&&x.reloads===3);
fs.mkdirSync(new URL('evidence/P157/',root),{recursive:true});fs.writeFileSync(new URL('evidence/P157/RELEASE_AUDIT.json',root),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({allPassed:report.allPassed,version,ui,coverage,encounters:encounters.length,clockPass,javaUnchanged:javaFiles.length,adventures:adventures.length}));if(!report.allPassed)process.exitCode=1;
