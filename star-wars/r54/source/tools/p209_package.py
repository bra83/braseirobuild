"""P209 full Android Studio ZIP + hash manifest delta + complete hash reconstruction."""
from pathlib import Path
import hashlib,json,zipfile,os
root=Path(__file__).resolve().parent.parent
out=Path('/mnt/data')
base=out/'BRASEIRO_STAR_WARS_EDGE_R54_P208_SOURCE_CHECKPOINT.zip'
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P209_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P208_TO_P209_DELTA.zip'
files=sorted((p for p in root.rglob('*') if p.is_file()),key=lambda p:p.relative_to(root).as_posix())
new={p.relative_to(root).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
with zipfile.ZipFile(base) as z:
 old={i.filename:hashlib.sha256(z.read(i)).hexdigest() for i in z.infolist() if not i.is_dir()}
changed=sorted(k for k in new if k in old and new[k]!=old[k]);added=sorted(k for k in new if k not in old);removed=sorted(k for k in old if k not in new)
assert not removed,removed
with zipfile.ZipFile(full,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for p in files:z.write(p,p.relative_to(root).as_posix())
manifest={'checkpoint':'P209','baseCheckpoint':'P208','baseZipSha256':hashlib.sha256(base.read_bytes()).hexdigest(),
 'filesTotal':len(new),'changed':changed,'added':added,'removed':removed,'fileHashes':new}
with zipfile.ZipFile(delta,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 z.writestr('P209_DELTA_MANIFEST.json',json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')
 for k in changed+added:z.write(root/k,'files/'+k)
with zipfile.ZipFile(full) as z:
 assert len(z.infolist())==len(new)
 assert z.testzip() is None
with zipfile.ZipFile(delta) as dz,zipfile.ZipFile(base) as bz:
 assert dz.testzip() is None
 reconstructed=dict(old)
 for k in removed:reconstructed.pop(k)
 for k in changed+added:reconstructed[k]=hashlib.sha256(dz.read('files/'+k)).hexdigest()
 assert reconstructed==new
sha=hashlib.sha256(full.read_bytes()).hexdigest();dsha=hashlib.sha256(delta.read_bytes()).hexdigest()
(out/(full.name+'.sha256')).write_text(sha+'  '+full.name+'\n')
print(json.dumps({'checkpoint':'P209','zip':str(full),'zipSize':full.stat().st_size,'zipSha256':sha,
 'delta':str(delta),'deltaSize':delta.stat().st_size,'deltaSha256':dsha,
 'files':len(new),'changed':len(changed),'added':len(added),'removed':len(removed),'zipCRC':'PASS','deltaCRC':'PASS','replayHashes':'PASS'},ensure_ascii=False,indent=2))
