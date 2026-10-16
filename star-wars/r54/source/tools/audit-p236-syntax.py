from pathlib import Path
import subprocess,concurrent.futures,json,zipfile,hashlib,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[1]
files=[p for p in root.rglob('*.mjs') if 'evidence' not in p.parts]+[p for p in root.rglob('*.js') if 'evidence' not in p.parts]
# all code syntax, including standalone runtime and bundled bridge
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as ex:
    results=list(ex.map(lambda p:(str(p.relative_to(root)),subprocess.run(['node','--check',str(p)],capture_output=True,text=True).returncode),files))
errors=[p for p,rc in results if rc]
json_files=list((root/'app').rglob('*.json'))
for p in json_files:
    try:json.loads(p.read_text())
    except Exception as e:errors.append(str(p.relative_to(root))+': '+str(e))
xml_files=list((root/'app').rglob('*.xml'))
for p in xml_files:
    try:ET.parse(p)
    except Exception as e:errors.append(str(p.relative_to(root))+': '+str(e))
base=zipfile.ZipFile('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P234_SOURCE_CHECKPOINT.zip')
java=[n for n in base.namelist() if n.endswith('.java')]
java_identical=sum(1 for n in java if (root/n).is_file() and hashlib.sha256((root/n).read_bytes()).digest()==hashlib.sha256(base.read(n)).digest())
result={'checkpoint':'P236','javascriptSyntaxFiles':len(files),'jsonFiles':len(json_files),'xmlFiles':len(xml_files),'javaBaselineFiles':len(java),'javaByteIdentical':java_identical,'errors':errors,'passed':not errors and java_identical==len(java)}
(root/'evidence/P236').mkdir(parents=True,exist_ok=True)
(root/'evidence/P236/SYNTAX_AND_JAVA.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
print(json.dumps(result,ensure_ascii=False))
if not result['passed']:raise SystemExit(1)
