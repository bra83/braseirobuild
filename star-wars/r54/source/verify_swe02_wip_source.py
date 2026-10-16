from pathlib import Path
import subprocess,json,zipfile,hashlib
root=Path(__file__).resolve().parent
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P244_SOURCE_CHECKPOINT.zip')
fail=[];counts={'mjs':0,'js_runtime':0,'json':0,'xml':0,'java':0}
for p in root.rglob('*.mjs'):
 if 'node_modules' in p.parts:continue
 counts['mjs']+=1
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 if r.returncode:fail.append({'file':str(p.relative_to(root)),'error':r.stderr[-500:]})
for p in (root/'app/src/main/assets/vtt').rglob('*.json'):
 counts['json']+=1
 try:json.loads(p.read_text())
 except Exception as e:fail.append({'file':str(p.relative_to(root)),'error':str(e)})
for p in (root/'app/src/main').rglob('*.xml'):
 counts['xml']+=1
 try:
  import xml.etree.ElementTree as ET
  ET.parse(p)
 except Exception as e:fail.append({'file':str(p.relative_to(root)),'error':str(e)})
with zipfile.ZipFile(base) as z:
 for p in (root/'app/src/main').rglob('*.java'):
  counts['java']+=1
  rel=str(p.relative_to(root))
  if rel in z.namelist() and hashlib.sha256(p.read_bytes()).digest()!=hashlib.sha256(z.read(rel)).digest():fail.append({'file':rel,'error':'Java changed from P244'})
report={'base':'P244','status':'PASS' if not fail else 'FAIL','counts':counts,'failures':fail,'apk_built':False,'android_device_tested':False}
out=root/'evidence'/'SWE02_FULL_BLOCK_WIP'/'source_syntax_preservation.json';out.write_text(json.dumps(report,indent=2))
print(json.dumps(report,indent=2)[:3000])
