import hashlib,json,shutil,sys
from pathlib import Path

root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent
source='423798ac3f8cefb8fc14caf114cb2779b6d57d92'
for p in (here/'overlay').rglob('*'):
    if p.is_file():
        target=root/p.relative_to(here/'overlay')
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(p,target)
faults=[]
manifest=json.loads((here/'runtime-manifest.json').read_text())
for row in manifest:
    p=root/row['path']
    if not p.is_file():faults.append(row['path']+': missing');continue
    data=p.read_bytes()
    def blob(data):return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
    if blob(data)!=row['sha'] and blob(data.replace(b'\r\n',b'\n'))!=row['sha']:
        faults.append(row['path']+': does not match canonical integrated Git blob')
if faults:
    print('\n'.join(faults));raise SystemExit('Canonical runtime verification failed; never build an approximate snapshot')
proof={'sourceCommit':source,'runtimeFilesVerified':len(manifest),'originalSourceSha256':'fd6485a92b2337b854de06279cf98fb14119b121db0c4ebb05be805317d9aa91','nativeSigning':'historical U19 private configuration retained; validated in final APK'}
(root/'public'/'fl-build-source.json').write_text(json.dumps(proof,indent=2)+'\n')
print(json.dumps(proof))
