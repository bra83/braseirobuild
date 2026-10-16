from pathlib import Path
import json,subprocess,zipfile,hashlib,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent.parent
out=root/'evidence/P203';out.mkdir(exist_ok=True)
vtt=root/'app/src/main/assets/vtt'
mjs=list((vtt/'js').rglob('*.mjs'))
js=list(vtt.rglob('*.js'))
errors=[]
for f in mjs+js:
 p=subprocess.run(['node','--check',str(f)],capture_output=True,text=True)
 if p.returncode:errors.append({'file':str(f.relative_to(root)),'error':p.stderr[-1000:]})
json_files=list((root/'app').rglob('*.json'))
for f in json_files:
 try:json.loads(f.read_text())
 except Exception as ex:errors.append({'file':str(f.relative_to(root)),'error':str(ex)})
xmls=list((root/'app/src/main').rglob('*.xml'))
for f in xmls:
 try:ET.parse(f)
 except Exception as ex:errors.append({'file':str(f.relative_to(root)),'error':str(ex)})
java=list((root/'app/src/main/java').rglob('*.java'))
base='/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P202_SOURCE_CHECKPOINT.zip'
with zipfile.ZipFile(base) as z:
 identical=all(f.read_bytes()==z.read(str(f.relative_to(root))) for f in java)
record={'checkpoint':'P203','mjsCount':len(mjs),'jsCount':len(js),'jsonCount':len(json_files),'xmlCount':len(xmls),'syntaxErrors':errors,'javaCount':len(java),'javaIdenticalToP202':identical,'bundleModules':98,'androidVersion':'1.0.203','versionCode':10203,'apkBuilt':False,'androidDeviceTested':False}
(out/'SYNTAX_AND_JAVA.json').write_text(json.dumps(record,ensure_ascii=False,indent=2))
(out/'PDF_TARGETED_SOURCE_CROSSCHECK.json').write_text(json.dumps({'checkpoint':'P203','method':'Google Drive connector readable text from official PDF (targeted only)','source':{'title':'Edge of the Empire Core Rulebook.pdf','driveId':'1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX','printedPage':215,'table':'6-9 Falling Damage'},'verifiedRules':['Short 10 damage/10 strain; Medium 30 damage/20 strain','Long incapacitates, Critical Injury +50, 30 strain','Extreme incapacitates, Critical Injury +75 (or death at GM discretion), 40 strain','Average Athletics or Coordination roll; each net success reduces numeric damage; each net advantage reduces strain','Triumph may reduce the range band one step at GM discretion','Soak reduces damage, not strain'], 'scope':'targeted page 215 only, NOT complete EotE/AoR semantic certification','implementation':['app/src/main/assets/vtt/js/adventure/jewelEscapeImpact.mjs','app/src/main/assets/vtt/js/rules/environment.mjs','app/src/main/assets/vtt/r46/final-runtime.js']},ensure_ascii=False,indent=2))
(out/'IMPLEMENTATION_EVIDENCE.json').write_text(json.dumps({'checkpoint':'P203','changes':['Long/Extreme fall real forced incapacitation + Critical Injury with +50/+75, 30/40 strain; existing environment.applyForcedFall integrated','Existing environment.applyForcedFall now accepts actual net advantages to reduce strain without changing prior callers','Real Average Athletics/Coordination dice and optional GM-authorized Triumph one-band reduction; medium numeric result or long forced result','Receipt impactVersion 2 with effectiveFallRange, gmTriumphReduce, forcedCriticalCode; replay checks dice faces, order, outcomes and rollback; legacy P202 receipts unchanged','Android PTBR fall selector, GM Triumph checkbox, versionName 1.0.203/versionCode 10203, offline bundle rebuilt'], 'tests':['tests/uae-p203-jewel-long-extreme-falls.test.mjs','tests/uae-p202-jewel-impact-goons.test.mjs','tests/r54-p113-environment-hazards.test.mjs'], 'limitations':['Extreme death is GM discretion; not automatically applied','Destiny mitigation not automatically applied (no source-backed specific conversion)','Companion multi-target explosion/fall not implemented','Arrow-7 vehicle pursuit and Arend/Naboka finale not implemented','Five adventures and 360 rules not certified','No APK, CI BRELGA, Android physical test'], 'source':'EotE Core official Drive ID 1UO3Z35J2Dmkfvx1OLyPD3rzJon013EoX, Table 6-9 printed p215'},ensure_ascii=False,indent=2))
print(json.dumps(record,ensure_ascii=False))
