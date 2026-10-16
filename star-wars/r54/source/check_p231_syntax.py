from pathlib import Path
import subprocess,json,hashlib,zipfile,xml.etree.ElementTree as ET,re
root=Path('/mnt/data/starwars_p231_work');vtt=root/'app/src/main/assets/vtt';bad=[]
mjs=list(root.rglob('*.mjs'))
for p in mjs:
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 if r.returncode:bad.append({'file':str(p.relative_to(root)),'error':r.stderr[:500]})
js=[vtt/'r46/final-runtime.js',vtt/'js/core/uiBridge.bundle.js']
for p in js:
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 if r.returncode:bad.append({'file':str(p.relative_to(root)),'error':r.stderr[:500]})
jsonfiles=list(vtt.rglob('*.json'))
for p in jsonfiles:
 try:json.loads(p.read_text())
 except Exception as e:bad.append({'file':str(p.relative_to(root)),'error':str(e)})
xmlfiles=list((root/'app/src/main').rglob('*.xml'))
for p in xmlfiles:
 try:ET.parse(p)
 except Exception as e:bad.append({'file':str(p.relative_to(root)),'error':str(e)})
with zipfile.ZipFile('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P230_SOURCE_CHECKPOINT.zip') as z:
 java=[n for n in z.namelist() if n.endswith('.java')]
 java_results={n:hashlib.sha256(z.read(n)).hexdigest()==hashlib.sha256((root/n).read_bytes()).hexdigest() for n in java}
modules=len(re.findall(r'^define\("js/',(vtt/'js/core/uiBridge.bundle.js').read_text(),re.M))
result={'checkpoint':'P231','mjs':len(mjs),'jsRuntime':len(js),'json':len(jsonfiles),'xml':len(xmlfiles),'bundleModules':modules,'javaKokoroCompared':len(java),'javaKokoroIdentical':sum(java_results.values()),'errors':bad}
(root/'evidence/P231/SYNTAX_AND_JAVA.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(result,ensure_ascii=False,indent=2))
