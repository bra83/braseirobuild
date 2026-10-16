from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
from zipfile import ZipFile
import subprocess,hashlib,json,xml.etree.ElementTree as ET,re
root=Path(__file__).resolve().parents[1];vtt=root/'app/src/main/assets/vtt';base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P226_SOURCE_CHECKPOINT.zip')
sha=lambda b:hashlib.sha256(b).hexdigest()
mjs=sorted(root.rglob('*.mjs'));runtime=[vtt/'r46/final-runtime.js',vtt/'js/core/uiBridge.bundle.js'];errors=[]
def check(p):
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 return {'file':str(p.relative_to(root)),'error':r.stderr.strip()} if r.returncode else None
with ThreadPoolExecutor(max_workers=8) as ex:
 for result in ex.map(check,mjs+runtime):
  if result:errors.append(result)
jsonfiles=sorted(vtt.rglob('*.json'))
for p in jsonfiles:
 try:json.loads(p.read_text())
 except Exception as e:errors.append({'file':str(p.relative_to(root)),'error':str(e)})
xmlfiles=sorted((root/'app/src/main').rglob('*.xml'))
for p in xmlfiles:
 try:ET.parse(p)
 except Exception as e:errors.append({'file':str(p.relative_to(root)),'error':str(e)})
java=[]
with ZipFile(base) as z:
 for p in sorted((root/'app/src/main').rglob('*.java')):
  key=p.relative_to(root).as_posix();old=z.read(key);new=p.read_bytes();same=old==new
  java.append({'file':key,'shaP226':sha(old),'shaP227':sha(new),'identical':same})
  if not same:errors.append({'file':key,'error':'Java/Kokoro changed'})
bundle=(vtt/'js/core/uiBridge.bundle.js').read_text();modules=len(re.findall(r'^define\("js/',bundle,re.M))
result={'mjs':len(mjs),'jsRuntime':len(runtime),'json':len(jsonfiles),'xml':len(xmlfiles),'javaKokoro':java,'bundleModuleCount':modules,'errors':errors}
out=root/'evidence/P227/SYNTAX_AND_JAVA.json';out.write_text(json.dumps(result,indent=2,ensure_ascii=False))
print(json.dumps({'mjs':len(mjs),'jsRuntime':len(runtime),'json':len(jsonfiles),'xml':len(xmlfiles),'errors':len(errors),'javaIdentical':sum(x['identical'] for x in java),'bundleModules':modules}))
if errors:raise SystemExit(1)
