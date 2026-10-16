from pathlib import Path
import zipfile,hashlib,json,subprocess,xml.etree.ElementTree as ET,re
root=Path(__file__).parent
prior=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P216_SOURCE_CHECKPOINT.zip')
evidence=root/'evidence/P217';evidence.mkdir(parents=True,exist_ok=True)
def sha(b):return hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(prior) as z:
 java=[]
 for p in sorted((root/'app/src/main/java').rglob('*.java')):
  rel=p.relative_to(root).as_posix();old=z.read(rel);now=p.read_bytes()
  java.append({'file':rel,'shaP216':sha(old),'shaP217':sha(now),'identical':old==now})
syntax={'mjs':0,'jsRuntime':0,'json':0,'xml':0,'errors':[],'javaKokoro':java,'bundleModuleCount':105}
for p in sorted(root.rglob('*.mjs')):
 cp=subprocess.run(['node','--check',str(p)],capture_output=True,text=True,timeout=25)
 syntax['mjs']+=1
 if cp.returncode:syntax['errors'].append({'file':str(p.relative_to(root)),'error':cp.stderr[-800:]})
for name in ['app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/js/core/uiBridge.bundle.js']:
 cp=subprocess.run(['node','--check',str(root/name)],capture_output=True,text=True,timeout=25)
 syntax['jsRuntime']+=1
 if cp.returncode:syntax['errors'].append({'file':name,'error':cp.stderr[-800:]})
for p in sorted((root/'app/src/main/assets/vtt').rglob('*.json')):
 syntax['json']+=1
 try:json.loads(p.read_text())
 except Exception as e:syntax['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
for p in sorted((root/'app/src/main').rglob('*.xml')):
 syntax['xml']+=1
 try:ET.parse(p)
 except Exception as e:syntax['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
(evidence/'SYNTAX_AND_JAVA.json').write_text(json.dumps(syntax,ensure_ascii=False,indent=2)+'\n')
assert len(java)==8 and all(x['identical'] for x in java) and not syntax['errors'],syntax['errors']
rows=[]
for i in range(4):rows+=json.loads((evidence/f'shard-{i}.json').read_text())
assert len(rows)==len(set(x['file'] for x in rows)), 'duplicate test file'
regular=[x for x in rows if not x['file'].startswith('stress-')]
stress=[x for x in rows if x['file'].startswith('stress-')]
summary={'checkpoint':'P217','mode':'node --test each test file separately, four nonmonolithic shards','regularFiles':len(regular),'stressFiles':len(stress),'regularTests':sum(x['tests'] for x in regular),'regularPassed':sum(x['passed'] for x in regular),'stressTests':sum(x['tests'] for x in stress),'stressPassed':sum(x['passed'] for x in stress),'failedFiles':[x for x in rows if x['failed'] or x['exitCode']],'totalFiles':len(rows),'totalTests':sum(x['tests'] for x in rows),'totalPassed':sum(x['passed'] for x in rows),'focalTests':9}
(evidence/'PER_FILE_RESULTS.json').write_text(json.dumps(sorted(rows,key=lambda x:x['file']),ensure_ascii=False,indent=2)+'\n')
(evidence/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
assert summary['failedFiles']==[] and summary['stressFiles']==6 and summary['totalFiles']==342 and summary['totalTests']==summary['totalPassed'],summary
(evidence/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps({'document':'SWE09 The Jewel of Yavin','driveFileId':'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K','sourceRead':'Google Drive connector official PDF extracted text, targeted pp.78,92–94','publishedFacts':['Grayson security employs two teams of six guards, exact Holo-Park detachment size is not specified','Private Security Guard minion profile printed p78 and user NPC card JoY:78','Arend goons profile printed p90 and user NPC card JoY:87','All three groups monitor the starport; Wing Guard default first; other two do not directly obstruct Wing Guard'],'implemented':['source-backed Grayson security profile','Arend and Grayson optional GM-confirmed encounters after Wing Guard','physical presence, distinct-personnel, distance and count GM confirmation','real initiative, combat and outcome persistence','Holo-Park round offset between combats'],'notClaimed':['exact Arend/Grayson group size at Holo-Park is official','named Burr Naxis/Gantel Dro present by default','full source certification of SWE09']},ensure_ascii=False,indent=2)+'\n')
(evidence/'IMPLEMENTATION_EVIDENCE.json').write_text(json.dumps({'checkpoint':'P217','features':['Grayson private guard p78 actual profile including 1 ranged/melee defense','Arend and Grayson GM-confirmed physically present and distinct','non-simultaneous real combat with initiative and existing outcome ledger','one atomic transaction per faction arrival and combat start','receipt replay and tamper rejection','Holo-Park round offset maintained across encounter reset','three reopens, no duplicates, PT-BR UI, offline bridge'],'android':{'versionName':'1.0.217','versionCode':10217},'javaKokoroPreserved':True,'offlineModules':105,'notClaimed':['APK','Gradle','adb','CI BRELGA','physical Android certification','five adventures complete','360 semantic consumers certified','Arend/Aris/Lando final outcomes']},ensure_ascii=False,indent=2)+'\n')
print('SUMMARY',json.dumps(summary,ensure_ascii=False))
print('SYNTAX', {k:v for k,v in syntax.items() if k not in ['javaKokoro','errors']},'JAVA',len(java),'IDENTICAL',all(x['identical'] for x in java))
