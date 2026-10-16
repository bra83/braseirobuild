from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import subprocess,json,zipfile,hashlib,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parent
out=root/'evidence/P197';out.mkdir(parents=True,exist_ok=True)
files=sorted((root/'app/src/main/assets/vtt/js').rglob('*.mjs'))
fail=[]
def check(p):
 x=subprocess.run(['node','--check',str(p)],cwd=root,capture_output=True,text=True)
 return (str(p.relative_to(root)),x.returncode,x.stderr[-1500:])
with ThreadPoolExecutor(max_workers=8) as ex:
 for name,code,err in ex.map(check,files):
  if code:fail.append({'file':name,'error':err})
for p in sorted((root/'app/src/main/assets/vtt').rglob('*.json')):
 try:json.loads(p.read_text())
 except Exception as e:fail.append({'file':str(p.relative_to(root)),'error':str(e)})
for p in sorted((root/'app/src/main').rglob('*.xml')):
 try:ET.parse(p)
 except Exception as e:fail.append({'file':str(p.relative_to(root)),'error':str(e)})
for name in ['app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/js/core/uiBridge.bundle.js']:
 p=root/name
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 if r.returncode:fail.append({'file':name,'error':r.stderr[-1500:]})
summary={'checkpoint':'P197','mjs':len(files),'json':len(list((root/'app/src/main/assets/vtt').rglob('*.json'))),'xml':len(list((root/'app/src/main').rglob('*.xml'))),'javascriptChecked':['app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/js/core/uiBridge.bundle.js'],'failures':fail}
(out/'SYNTAX_CHECK.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2))
base='/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P196_SOURCE_CHECKPOINT.zip'
java=sorted((root/'app/src/main/java').rglob('*.java'))
with zipfile.ZipFile(base) as z:
 checks=[]
 for p in java:
  name=str(p.relative_to(root));sha=hashlib.sha256(p.read_bytes()).hexdigest();old=hashlib.sha256(z.read(name)).hexdigest()
  checks.append({'path':name,'sha256':sha,'byteIdentical':sha==old})
(out/'JAVA_PRESERVATION.json').write_text(json.dumps({'baseline':'P196','count':len(java),'identicalCount':sum(x['byteIdentical'] for x in checks),'files':checks},indent=2))
print(json.dumps({'syntax':summary,'javaIdentical':sum(x['byteIdentical'] for x in checks)},ensure_ascii=False))
if fail or any(not x['byteIdentical'] for x in checks):raise SystemExit(1)
