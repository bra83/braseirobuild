import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {aorTalentCoverage,aorTalentCatalog} from '../app/src/main/assets/vtt/js/expansion/aorProgressionIntegration.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {publishedAdventureRuntimeModule} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureUaeBridge.mjs';
import {validatePublishedAdventureGraph} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureGraphGate.mjs';
import {validateAdventureSemantics} from '../app/src/main/assets/vtt/js/adventure/adventureSemanticGate.mjs';
const root=path.resolve(import.meta.dirname,'..');
const read=p=>fs.readFileSync(path.join(root,p),'utf8');
const hash=p=>crypto.createHash('sha256').update(fs.readFileSync(path.join(root,p))).digest('hex');
const java=JSON.parse(read('evidence/P158/JAVA_P157_SHA256.json'));
const javaPreserved=Object.entries(java).every(([file,sha])=>hash(file)===sha);
const coverage=aorTalentCoverage();
const talentsCorrect=[['EMBAUCADOR NATO',1],['LÍDER NATO',1]].every(([name,positions])=>
 aorTalentCatalog().filter(t=>t.originalName===name&&t.mechanicallyActive).length===positions);
const adventures=publishedAdventureCatalog().map(({id})=>{
 const module=publishedAdventureRuntimeModule(getPublishedAdventureAdapter(id).meta);
 return {id,graph:!!validatePublishedAdventureGraph(module),semantics:!!validateAdventureSemantics(module)};
});
const gradle=read('app/build.gradle.kts');
const androidVersion=/versionCode\s*=\s*10159/.test(gradle)&&/versionName\s*=\s*"1\.0\.159"/.test(gradle);
const shellVersion=read('app/src/main/assets/vtt/r46-shell.html').includes('data-version="1.0.159"');
const bundle=read('app/src/main/assets/vtt/js/core/uiBridge.bundle.js');
const bundleVersion=bundle.includes('FINAL 1.0.159 offline engine bundle')&&bundle.includes('modules:79');
const ui=read('app/src/main/assets/vtt/r46/final-runtime.js');
const uiIntegrated=ui.includes('data-p159-natural-label')&&ui.includes('data-p159-p151-natural-label')&&ui.includes('naturalRerollStatus');
const focal=read('evidence/P159/NODE_FOCAL.txt').includes('# pass 27')&&read('evidence/P159/NODE_FOCAL.txt').includes('# fail 0');
const regular=read('evidence/P159/NODE_REGULAR.txt').includes('"bad":0')&&read('evidence/P159/NODE_REGULAR.txt').includes('# fail 0');
const stress=read('evidence/P159/NODE_STRESS.txt').includes('"bad":0')&&read('evidence/P159/NODE_STRESS.txt').includes('# pass 6');
const tts=read('evidence/P159/KOKORO_TTS_REGRESSION.txt').includes('# pass 27')&&read('evidence/P159/KOKORO_TTS_REGRESSION.txt').includes('# fail 0');
const syntax=read('evidence/P159/JAVA_SYNTAX.txt').includes('JAVA_SYNTAX_PASS files=8');
const results={checkpoint:'P159',coverage,javaPreserved,javaFiles:Object.keys(java).length,talentsCorrect,adventures,
 androidVersion,shellVersion,bundleVersion,uiIntegrated,focal,regular,stress,tts,syntax};
results.allPassed=coverage.positions===360&&coverage.mechanicalPositions===270&&javaPreserved&&talentsCorrect&&
 adventures.length===5&&adventures.every(a=>a.graph&&a.semantics)&&androidVersion&&shellVersion&&bundleVersion&&uiIntegrated&&focal&&regular&&stress&&tts&&syntax;
console.log(JSON.stringify(results,null,2));
if(!results.allPassed)process.exitCode=1;
