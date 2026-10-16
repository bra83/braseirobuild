from pathlib import Path
import subprocess,hashlib,zipfile,json,concurrent.futures,xml.etree.ElementTree as ET
root=Path(__file__).parent
mjs=[p for p in root.rglob('*.mjs') if 'node_modules' not in p.parts]
js=[root/'app/src/main/assets/vtt/r46/final-runtime.js',root/'app/src/main/assets/vtt/js/core/uiBridge.bundle.js']
jsons=list((root/'app/src/main/assets/vtt').rglob('*.json'))
xmls=list((root/'app').rglob('*.xml'))
def check(p):
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True,timeout=20)
 return str(p.relative_to(root)) if r.returncode else None
with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
 failures=[x for x in pool.map(check,mjs+js) if x]
for p in jsons:
 try:json.loads(p.read_text())
 except Exception:failures.append(str(p.relative_to(root)))
for p in xmls:
 try:ET.parse(p)
 except Exception:failures.append(str(p.relative_to(root)))
with zipfile.ZipFile('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P225_SOURCE_CHECKPOINT.zip') as z:
 java=[x for x in z.namelist() if x.endswith('.java')]
 identical=[x for x in java if hashlib.sha256((root/x).read_bytes()).digest()==hashlib.sha256(z.read(x)).digest()]
report={'checkpoint':'P226','mjsChecked':len(mjs),'jsRuntimeChecked':len(js),'jsonVttChecked':len(jsons),'xmlAndroidChecked':len(xmls),'javaCompared':len(java),'javaIdentical':len(identical),'failures':failures}
out=root/'evidence/P226/SYNTAX_AND_JAVA.json';out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
if failures or len(identical)!=len(java):raise SystemExit(1)
