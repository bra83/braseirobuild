from pathlib import Path
import zipfile,hashlib,json,subprocess,xml.etree.ElementTree as ET
root=Path(__file__).parent; prior=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P213_SOURCE_CHECKPOINT.zip')
evidence=root/'evidence/P214';evidence.mkdir(exist_ok=True)
def sha(b):return hashlib.sha256(b).hexdigest()
java=[]
with zipfile.ZipFile(prior) as z:
 for p in sorted((root/'app/src/main/java').rglob('*.java')):
  name=p.relative_to(root).as_posix();java.append({'file':name,'shaP213':sha(z.read(name)),'shaP214':sha(p.read_bytes()),'identical':sha(z.read(name))==sha(p.read_bytes())})
results={'mjs':0,'jsRuntime':0,'json':0,'xml':0,'errors':[],'javaKokoro':java,'bundleModuleCount':104}
for p in sorted(root.rglob('*.mjs')):
 if any(x in p.parts for x in ['node_modules','.git']):continue
 cp=subprocess.run(['node','--check',str(p)],capture_output=True,text=True)
 results['mjs']+=1
 if cp.returncode:results['errors'].append({'file':str(p.relative_to(root)),'error':cp.stderr[-800:]})
for name in ['app/src/main/assets/vtt/r46/final-runtime.js','app/src/main/assets/vtt/js/core/uiBridge.bundle.js']:
 cp=subprocess.run(['node','--check',str(root/name)],capture_output=True,text=True)
 results['jsRuntime']+=1
 if cp.returncode:results['errors'].append({'file':name,'error':cp.stderr[-800:]})
for p in sorted((root/'app/src/main/assets/vtt').rglob('*.json')):
 results['json']+=1
 try:json.loads(p.read_text())
 except Exception as e:results['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
for p in sorted((root/'app/src/main').rglob('*.xml')):
 results['xml']+=1
 try:ET.parse(p)
 except Exception as e:results['errors'].append({'file':str(p.relative_to(root)),'error':str(e)})
(evidence/'SYNTAX_AND_JAVA.json').write_text(json.dumps(results,indent=2,ensure_ascii=False)+'\n')
print(json.dumps({k:v for k,v in results.items() if k not in ['javaKokoro','errors']},indent=2),'JAVA',len(java),'IDENTICAL',all(x['identical'] for x in java),'ERRORS',results['errors'])
