from pathlib import Path
from concurrent.futures import ThreadPoolExecutor,as_completed
from zipfile import ZipFile
import subprocess,hashlib,json,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent
vtt=root/'app/src/main/assets/vtt'
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P219_SOURCE_CHECKPOINT.zip')
sha=lambda b:hashlib.sha256(b).hexdigest()
js_files=sorted(root.rglob('*.mjs'))
runtime=[vtt/'r46/final-runtime.js',vtt/'js/core/uiBridge.bundle.js']
errors=[]
def check(p):
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 return (str(p.relative_to(root)),r.returncode,r.stderr.strip())
with ThreadPoolExecutor(max_workers=8) as pool:
 for f in as_completed([pool.submit(check,p) for p in js_files+runtime]):
  path,code,err=f.result()
  if code:errors.append({'path':path,'error':err})
json_files=sorted(vtt.rglob('*.json'))
for p in json_files:
 try:json.loads(p.read_text())
 except Exception as e:errors.append({'path':str(p.relative_to(root)),'error':str(e)})
xml_files=sorted((root/'app/src/main').rglob('*.xml'))
for p in xml_files:
 try:ET.parse(p)
 except Exception as e:errors.append({'path':str(p.relative_to(root)),'error':str(e)})
with ZipFile(base) as z:
 java=[]
 for p in sorted((root/'app/src/main').rglob('*.java')):
  key=p.relative_to(root).as_posix()
  old=z.read(key);new=p.read_bytes()
  java.append({'file':key,'shaP220':sha(old),'shaP220':sha(new),'identical':old==new})
  if old!=new:errors.append({'path':key,'error':'Java/Kokoro altered'})
import re
bundle=(vtt/'js/core/uiBridge.bundle.js').read_text()
modules=len(re.findall(r'^define\("js/',bundle,re.M))
result={'mjs':len(js_files),'jsRuntime':len(runtime),'json':len(json_files),'xml':len(xml_files),'errors':errors,'javaKokoro':java,'bundleModuleCount':modules}
out=root/'evidence/P220/SYNTAX_AND_JAVA.json';out.write_text(json.dumps(result,indent=2,ensure_ascii=False))
print(json.dumps({'mjs':len(js_files),'jsRuntime':len(runtime),'json':len(json_files),'xml':len(xml_files),'errors':len(errors),'javaIdentical':sum(x['identical'] for x in java),'bundleModules':modules}))
if errors:raise SystemExit(1)
