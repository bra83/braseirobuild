from pathlib import Path
import zipfile,hashlib,json,re,datetime
root=Path(__file__).resolve().parents[1]
e=root/'evidence'/'P199';e.mkdir(parents=True,exist_ok=True)
results=json.loads((e/'PER_FILE_RESULTS.json').read_text())
stress=[]
for p in sorted((root/'tests').glob('stress-*.test.mjs')):
 text=Path('/mnt/data/p199_final_'+p.name+'.log').read_text()
 def num(k):
  m=re.findall(r'^# '+k+r'\s+(\d+)\s*$',text,re.M)
  return int(m[-1]) if m else None
 stress.append({'file':p.name,'exit':0 if num('fail')==0 and num('pass')==1 else 1,'tests':num('tests'),'pass':num('pass'),'fail':num('fail')})
regular=results['files']
assert len(regular)==318 and sum(x['pass'] for x in regular)==2926 and not any(x['exit'] for x in regular)
assert len(stress)==6 and sum(x['pass'] for x in stress)==6 and not any(x['exit'] for x in stress)
results['stress']={'files':6,'tests':6,'pass':6,'fail':0,'failedFiles':[]}
results['total']={'files':324,'tests':2932,'pass':2932,'fail':0,'failedFiles':[]}
results['files']=regular+stress
(e/'PER_FILE_RESULTS.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
summary={'checkpoint':'P199','method':'node --test --test-reporter=tap individually per file; six stress sequentially, not a monolithic suite','regularFiles':318,'regularTests':2926,'regularPassed':2926,'regularFailed':0,'stressFiles':6,'stressTests':6,'stressPassed':6,'stressFailed':0,'totalFiles':324,'totalTests':2932,'totalPassed':2932,'totalFailed':0,'newFocusedTests':20,'monolithicSuiteClaim':False,'apkBuilt':False,'gradleExecuted':False,'ciBrelgaExecuted':False,'androidPhysicalValidation':False,'logs':['/mnt/data/p199_regular_FINAL.log']+['/mnt/data/p199_final_'+p.name+'.log' for p in sorted((root/'tests').glob('stress-*.test.mjs'))]}
(e/'TEST_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P198_SOURCE_CHECKPOINT.zip')
with zipfile.ZipFile(base) as z:
 old={x.filename:hashlib.sha256(z.read(x)).hexdigest() for x in z.infolist() if x.filename.endswith('.java')}
new={str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*.java') if 'node_modules' not in p.parts}
assert old==new,(set(old)^set(new))
(e/'JAVA_PRESERVATION.json').write_text(json.dumps({'base':'P198','target':'P199','javaFiles':len(new),'byteIdentical':old==new,'hashes':new},indent=2))
(e/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps({'checkpoint':'P199','sourceVerifiedOn':'2026-10-14','sources':[{'title':'Edge of the Empire Core Rulebook','driveFileId':'1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX','printedPage':179,'section':'Table 5-8: Gear and Equipment — Security','facts':['Restraining Bolt 35 credits, Encumbrance 0, Rarity 0','Slicer Gear 500 credits, Encumbrance 2, Rarity 4','Restraining bolt installation requires a few seconds and a spot-welder','Tool kit explicitly includes restraining bolts and welding tools']},{'title':'The Jewel of Yavin','productCode':'SWE09','driveFileId':'1z6Kh-0tqRxegzB_U7GL_WxicUQKIOU6K','printedPage':69,'section':'Banking on a Break, Table 2-1 and Slicing Ahead','facts':['Restraining bolt downgrades the Computers check twice','Electronic signature upgrades subsequent Cloud City slicing until slicer fully replaces tools']}],'coverage':'TARGETED_ONLY','notCertified':['all five adventures scene-by-scene','EotE/AoR/expansions full semantic coverage','SWE09 bank payout/escape']},ensure_ascii=False,indent=2))
(e/'IMPLEMENTATION_EVIDENCE.json').write_text(json.dumps({'checkpoint':'P199','baseSha256':'f08e2852483f1e055a4da3e13f2901b5354df1dd96e997ac07e47810aa6460a2','implementation':['Official EotE physical restraining_bolt catalog item: 35 credits, 0 encumbrance, rarity 0','Official slicer_gear catalog item: 500 credits, 2 encumbrance, rarity 4','Atomic GM-approved bolt purchase with debit, inventory, anti-tamper receipt','Bolt capture requires actual owned bolt and real welder/GM confirmation, applies published G-100 downgrade, moves bolt out of inventory on success','New bank equipment contract version protects new captures, old P198 unversioned saves remain replayable','After captured signature, GM-approved replacement purchases fresh slicer gear for 500 credits, removes old kit if present, stops future slicing difficulty upgrade without deleting historical evidence','Atomic rollback, receipt replay, duplicate prevention, three reopenings, Android PTBR UI, bridge and 95-module offline bundle'],'tests':'evidence/P199/TEST_SUMMARY.json','source':'evidence/P199/PDF_TARGETED_SOURCE_CROSSCHECK.json','limits':['No automatic full Cloud City tool market availability, requires GM approval','Historical signature remains incriminating evidence; replacing gear only removes future difficulty upgrade','Restraining bolt requires separately owned item, tool kit alone is not automatically an independent bolt','No generic crafting or physical weld time charged (book only says a few seconds)','No APK/SDK/Gradle/adb/CI BRELGA/physical Android certification','No full SWE09 bank settlement, Naboka betrayal or escape']},ensure_ascii=False,indent=2))
print(json.dumps({'regular':2926,'stress':6,'total':2932,'java':len(new)},indent=2))
