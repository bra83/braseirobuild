from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from hashlib import sha256
import json,io
root=Path(__file__).parent
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P216_SOURCE_CHECKPOINT.zip')
output=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P217_SOURCE_CHECKPOINT.zip')
delta=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P216_TO_P217_DELTA.zip')
def h(b):return sha256(b).hexdigest()
with ZipFile(base) as z:
 old={n:h(z.read(n)) for n in z.namelist()}
files={p.relative_to(root).as_posix():p for p in root.rglob('*') if p.is_file()}
assert set(old).issubset(files),'missing base files'
new={name:h(path.read_bytes()) for name,path in files.items()}
added=sorted(set(new)-set(old));modified=sorted(n for n in set(new)&set(old) if new[n]!=old[n]);removed=sorted(set(old)-set(new))
assert not removed
with ZipFile(output,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 for name in sorted(files):z.write(files[name],name)
with ZipFile(output) as z:
 assert z.testzip() is None
 assert len(z.namelist())==len(files)
 assert all(h(z.read(n))==new[n] for n in z.namelist())
manifest={'format':'BRASEIRO_STAR_WARS_DELTA_V1','baseCheckpoint':'P216','baseSha256':h(base.read_bytes()),'baseFileCount':len(old),
 'targetCheckpoint':'P217','targetFileCount':len(new),'added':added,'modified':modified,'removed':removed,'targetHashes':dict(sorted(new.items()))}
with ZipFile(delta,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr('MANIFEST.json',json.dumps(manifest,ensure_ascii=False,separators=(',',':')))
 for n in added+modified:z.write(files[n],'files/'+n)
with ZipFile(delta) as z:
 assert z.testzip() is None
 m=json.loads(z.read('MANIFEST.json'))
 reconstructed=old.copy()
 for n in m['removed']:reconstructed.pop(n)
 for n in m['added']+m['modified']:reconstructed[n]=h(z.read('files/'+n))
 assert reconstructed==m['targetHashes']==new
sha_file=Path(str(output)+'.sha256');sha_file.write_text(f'{h(output.read_bytes())}  {output.name}\n')
print(json.dumps({'zip':str(output),'zipBytes':output.stat().st_size,'zipSha256':h(output.read_bytes()),'files':len(new),'delta':str(delta),'deltaBytes':delta.stat().st_size,'deltaSha256':h(delta.read_bytes()),'deltaEntries':len(added)+len(modified)+1,'added':len(added),'modified':len(modified),'removed':len(removed),'replay':len(new)},ensure_ascii=False))
