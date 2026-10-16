from pathlib import Path
import json,subprocess,hashlib,zipfile
root=Path(__file__).resolve().parent.parent
out=root/'evidence/P206';out.mkdir(parents=True,exist_ok=True)
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P205_SOURCE_CHECKPOINT.zip')
with zipfile.ZipFile(base) as z:
 java=[n for n in z.namelist() if n.endswith('.java') and ('Kokoro' in n or 'Tts' in n or 'Speech' in n or 'Gemini' in n)]
 # canonical eight files from P205 evidence, including unchanged Android Java
 print('java candidates',len(java),java[:12])
 unchanged={n:hashlib.sha256(z.read(n)).hexdigest()==hashlib.sha256((root/n).read_bytes()).hexdigest() for n in z.namelist() if n.endswith('.java')}
 (out/'JAVA_PRESERVATION.json').write_text(json.dumps({'totalJava':len(unchanged),'allJavaByteIdentical':all(unchanged.values()),'modifiedJava':[k for k,v in unchanged.items() if not v]},indent=2))
 assert all(unchanged.values())
vtt=root/'app/src/main/assets/vtt'
files={ext:list(vtt.rglob('*.'+ext)) for ext in ('mjs','js','json','xml')}
results={}
for ext,paths in files.items():
 if ext in ('mjs','js'):
  checked=0;failed=[]
  for p in paths:
   if p.stat().st_size>15_000_000:continue
   r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
   checked+=1
   if r.returncode:failed.append({'path':str(p.relative_to(root)),'error':r.stderr[-2000:]})
  results[ext]={'checked':checked,'failed':failed}
 elif ext=='json':
  bad=[]
  for p in paths:
   try:json.loads(p.read_text())
   except Exception as e:bad.append({'path':str(p.relative_to(root)),'error':str(e)})
  results[ext]={'checked':len(paths),'failed':bad}
 else:
  import xml.etree.ElementTree as ET
  bad=[]
  for p in paths:
   try:ET.parse(p)
   except Exception as e:bad.append({'path':str(p.relative_to(root)),'error':str(e)})
  results[ext]={'checked':len(paths),'failed':bad}
results['allPass']=all(not x['failed'] for x in results.values())
(out/'SYNTAX_AND_JAVA.json').write_text(json.dumps(results,indent=2))
print(json.dumps(results,indent=2)[:3000]);assert results['allPass']
