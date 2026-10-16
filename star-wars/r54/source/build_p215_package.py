"""Reproducible P215 Android Studio source and P214→P215 delta with hash replay."""
from pathlib import Path
from hashlib import sha256
from zipfile import ZipFile,ZIP_DEFLATED
import json
root=Path(__file__).resolve().parent
out=Path('/mnt/data')
base=out/'BRASEIRO_STAR_WARS_EDGE_R54_P214_SOURCE_CHECKPOINT.zip'
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P215_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P214_TO_P215_DELTA.zip'
manifest='P214_TO_P215_MANIFEST.json'
def hashb(data):return sha256(data).hexdigest()
with ZipFile(base) as z:
 assert z.testzip() is None
 old={name:hashb(z.read(name)) for name in z.namelist() if not name.endswith('/')}
new={p.relative_to(root).as_posix():hashb(p.read_bytes()) for p in sorted(root.rglob('*')) if p.is_file()}
added=sorted(new.keys()-old.keys());modified=sorted(n for n in new.keys()&old.keys() if new[n]!=old[n]);removed=sorted(old.keys()-new.keys())
assert not removed, f'Removal would require review: {removed}'
with ZipFile(full,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for name in sorted(new):z.write(root/name,name)
with ZipFile(full) as z:
 assert z.testzip() is None
 assert len(z.namelist())==len(new)
 assert all(hashb(z.read(n))==h for n,h in new.items())
fullhash=hashb(full.read_bytes())
m={'from':'P214','to':'P215','baseSha256':hashb(base.read_bytes()),'added':added,'modified':modified,'removed':removed,'allFileSha256':new,'fullZipSha256':fullhash,'fullFileCount':len(new)}
with ZipFile(delta,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr(manifest,json.dumps(m,ensure_ascii=False,indent=2)+'\n')
 for n in added+modified:z.write(root/n,n)
with ZipFile(delta) as z:
 assert z.testzip() is None
 assert len(z.namelist())==1+len(added)+len(modified)
 reconstructed=dict(old)
 for n in added+modified:reconstructed[n]=hashb(z.read(n))
 for n in removed:reconstructed.pop(n,None)
 assert reconstructed==new,'Delta replay mismatch'
deltahash=hashb(delta.read_bytes())
sidecar=out/(full.name+'.sha256');sidecar.write_text(f'{fullhash}  {full.name}\n')
print(json.dumps({'full':str(full),'fullSize':full.stat().st_size,'fullSha256':fullhash,'files':len(new),'delta':str(delta),'deltaSize':delta.stat().st_size,'deltaSha256':deltahash,'added':len(added),'modified':len(modified),'removed':len(removed),'replayedHashes':len(new),'crc':'PASS','sidecar':str(sidecar)},indent=2))
