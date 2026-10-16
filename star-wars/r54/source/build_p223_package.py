from pathlib import Path
import zipfile,hashlib,json,os,time
r=Path(__file__).resolve().parent
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P222_SOURCE_CHECKPOINT.zip')
target=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P223_SOURCE_CHECKPOINT.zip')
delta=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P222_TO_P223_DELTA.zip')
sha=lambda b:hashlib.sha256(b).hexdigest()
files={str(p.relative_to(r)).replace(os.sep,'/'):p for p in r.rglob('*') if p.is_file()}
assert len(files)==len(set(files))
print('START',len(files),'files',flush=True)
with zipfile.ZipFile(base) as z: old={n:sha(z.read(n)) for n in z.namelist()}
new={n:sha(p.read_bytes()) for n,p in files.items()}
added=sorted(set(new)-set(old));removed=sorted(set(old)-set(new));modified=sorted(n for n in set(new)&set(old) if new[n]!=old[n]);print('DELTA',len(added),len(modified),len(removed),flush=True)
assert not removed,removed
with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n in sorted(files):z.write(files[n],arcname=n)
print('ZIP_WRITTEN',target.stat().st_size,flush=True)
with zipfile.ZipFile(target) as z:
 assert len(z.namelist())==len(files)
 assert z.testzip() is None
 assert {n:sha(z.read(n)) for n in z.namelist()}==new
zipSha=sha(target.read_bytes())
manifest={'format':'BRASEIRO_STAR_WARS_DELTA_V1','baseCheckpoint':'P222','baseSha256':sha(base.read_bytes()),'baseFileCount':len(old),'targetCheckpoint':'P223','targetFileCount':len(new),'added':added,'modified':modified,'removed':removed,'targetHashes':new}
with zipfile.ZipFile(delta,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr('MANIFEST.json',json.dumps(manifest,ensure_ascii=False,separators=(',',':')))
 for n in added+modified:z.write(files[n],arcname='files/'+n)
with zipfile.ZipFile(delta) as z:
 assert z.testzip() is None
 m=json.loads(z.read('MANIFEST.json'))
 replay={**old}
 for n in m['removed']:replay.pop(n)
 for n in m['added']+m['modified']:replay[n]=sha(z.read('files/'+n))
 assert replay==new
 assert len(z.namelist())==1+len(added)+len(modified)
sidecar=Path(str(target)+'.sha256');sidecar.write_text(zipSha+'  '+target.name+'\n')
info={'zip':str(target),'zipSha256':zipSha,'zipBytes':target.stat().st_size,'fileCount':len(files),'zipCrcPass':True,
 'delta':str(delta),'deltaSha256':sha(delta.read_bytes()),'deltaBytes':delta.stat().st_size,'deltaCrcPass':True,
 'added':len(added),'modified':len(modified),'removed':len(removed),'replayHashesPass':len(new)}
Path('/mnt/data/P223_PACKAGE_RESULTS.json').write_text(json.dumps(info,indent=2))
print('SUCCESS',json.dumps(info),flush=True)
