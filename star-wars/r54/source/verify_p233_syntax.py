from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import subprocess,zipfile,hashlib,json,xml.etree.ElementTree as ET
root=Path(__file__).parent;vtt=root/'app/src/main/assets/vtt';base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P232_SOURCE_CHECKPOINT.zip')
mjs=list(root.rglob('*.mjs'));js=[vtt/'r46/final-runtime.js',vtt/'js/core/uiBridge.bundle.js'];jsons=list(vtt.rglob('*.json'));xmls=list((root/'app/src/main').rglob('*.xml'))
def nodecheck(p):
 r=subprocess.run(['node','--check',str(p)],capture_output=True,text=True,timeout=30);return str(p.relative_to(root)),r.returncode,r.stderr[:250]
with ThreadPoolExecutor(max_workers=8) as pool:checks=list(pool.map(nodecheck,mjs+js))
fail=[x for x in checks if x[1]]
for p in jsons:
 try:json.loads(p.read_text())
 except Exception as e:fail.append((str(p.relative_to(root)),1,str(e)))
for p in xmls:
 try:ET.parse(p)
 except Exception as e:fail.append((str(p.relative_to(root)),1,str(e)))
with zipfile.ZipFile(base) as z:
 javas=[p for p in (root/'app/src/main/java').rglob('*.java')]
 java=[{'file':str(p.relative_to(root)),'identical':z.read(str(p.relative_to(root)))==p.read_bytes()} for p in javas]
result={'checkpoint':'P233','mjs':len(mjs),'jsRuntime':len(js),'json':len(jsons),'xml':len(xmls),'failures':fail,'java':java,'javaByteIdentical':all(x['identical'] for x in java),'bundleModules':119}
(root/'evidence/P233/SYNTAX_AND_JAVA.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k not in ('java','failures')},ensure_ascii=False));print('FAILURES',fail[:10]);assert not fail and result['javaByteIdentical']
