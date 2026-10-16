from pathlib import Path
import zipfile,hashlib,json,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).parent; prior=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P214_SOURCE_CHECKPOINT.zip')
evidence=root/'evidence/P215';evidence.mkdir(exist_ok=True)
def sha(b):return hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(prior) as z:
 java=[{'file':p.relative_to(root).as_posix(),'shaP214':sha(z.read(p.relative_to(root).as_posix())),'shaP215':sha(p.read_bytes()),'identical':sha(z.read(p.relative_to(root).as_posix()))==sha(p.read_bytes())} for p in sorted((root/'app/src/main/java').rglob('*.java'))]
results={'mjs':0,'jsRuntime':0,'json':0,'xml':0,'errors':[],'javaKokoro':java,'bundleModuleCount':104}
for p in sorted(root.rglob('*.mjs')):
 if any(x in p.parts for x in ['node_modules','.git']):continue
 cp=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 results['mjs']+=1
 if cp.returncode:results['errors'].append({'file':str(p.relative_to(root)),'error':cp.stderr[-800:]})
for name in ['app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/js/core/uiBridge.bundle.js']:
 cp=subprocess.run(['node','--check',str(root/name)],capture_output=True,text=True)
 results['jsRuntime']+=1
 if cp.returncode:results['errors'].append({'file':name,'error':cp.stderr[-800:]})
for p in sorted((root/'app/src/main/assets/vtt').rglob('*.json')):
 results['json']+=1
 try:json.loads(p.read_text())
 except Exception as e:results['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
for p in sorted((root/'app/src/main').rglob('*.xml')):
 results['xml']+=1
 try:ET.parse(p)
 except Exception as e:results['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
(evidence/'SYNTAX_AND_JAVA.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
print(json.dumps({k:v for k,v in results.items() if k not in ['javaKokoro','errors']},indent=2),'JAVA',len(java),'IDENTICAL',all(x['identical'] for x in java),'ERRORS',results['errors'])
assert len(java)==8 and all(x['identical'] for x in java) and not results['errors']
rows=[]
for i in range(4):rows+=json.loads((evidence/f'shard-{i}.json').read_text())
regular=[x for x in rows if not x['file'].startswith('stress-')];stress=[x for x in rows if x['file'].startswith('stress-')]
summary={'checkpoint':'P215','mode':'node --test each file independently, four nonmonolithic shards; six stress separately rerun sequentially','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['tests'] for x in regular),'regularPassed':sum(x['passed'] for x in regular),'stressTests':sum(x['tests'] for x in stress),'stressPassed':sum(x['passed'] for x in stress),'failedFiles':[x for x in rows if x['failed'] or x['exitCode']],'totalFiles':len(rows),'totalTests':sum(x['tests'] for x in rows),'totalPassed':sum(x['passed'] for x in rows),'focalTests':4}
(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(sorted(rows,key=lambda x:x['file']),ensure_ascii=False,indent=2)+'\n')
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
print('SUMMARY',json.dumps(summary,ensure_ascii=False))
assert summary['failedFiles']==[] and summary['totalFiles']==340 and summary['totalTests']==summary['totalPassed'] and summary['stressFiles']==6
(evidence/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps({'document':'SWE09 The Jewel of Yavin','driveFileId':'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K','sourceRead':'Google Drive connector fetch of official PDF text, targeted pp.93–94','page93Christophsis':'NPC Despair: opponent dives for cover behind ornamental fountain, defense 2 until movement','page94Coruscant':'NPC Despair: target disappears into holographic crowd; opposed Stealth vs player Perception as incidental; on success cannot see target','implemented':['christophsis_fountain','coruscant_vanish'],'notImplemented':['kashyyyk_insect','kashyyyk_vertigo'],'notClaimed':'No full PDF certification or complete adventure audit'},ensure_ascii=False,indent=2)+'\n')
(evidence/'IMPLEMENTATION_EVIDENCE.json').write_text(json.dumps({'checkpoint':'P215','modulesChanged':['app/src/main/assets/vtt/js/adventure/jewelHoloParkEffects.mjs','app/src/main/assets/vtt/js/core/controller.mjs','app/src/main/assets/vtt/js/core/uiBridge.mjs','app/src/main/assets/vtt/js/core/uiBridge.bundle.js','app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/r46-shell.html'],'features':['NPC Despair reserved separately from Threat with real dice-face replay','Christophsis fountain grants real NPC ranged defense 2 until GM-confirmed movement','Coruscant opposed NPC Stealth vs player Perception; successful target cannot be attacked until GM-confirmed line of sight','GM-only unused NPC Despair fallback with real strain/wound','receipts, rollback, idempotence, three save reopenings'],'landscapeCoverage':{'implemented':14,'total':16},'android':{'versionName':'1.0.215','versionCode':10215},'javaKokoroPreserved':True,'offlineModules':104,'notClaimed':['APK','Gradle','adb','CI BRELGA','physical device certification','five-adventure full coverage','360 semantic consumers']},ensure_ascii=False,indent=2)+'\n')
