from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import hashlib,json
root=Path(__file__).resolve().parent
out=Path('/mnt/data')
base=out/'BRASEIRO_STAR_WARS_EDGE_R54_P218_SOURCE_CHECKPOINT.zip'
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P219_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P218_TO_P219_DELTA.zip'
sha=lambda b:hashlib.sha256(b).hexdigest()
assert sha(base.read_bytes())=='5fc376d8ebb758d08b7ed7cf5aea8dcff57ae197736074882dfb9a064ce208ae'
with ZipFile(base) as z:
 assert z.testzip() is None
 old={n:sha(z.read(n)) for n in z.namelist() if not n.endswith('/')}
assert len(old)==6568
files=sorted(p for p in root.rglob('*') if p.is_file() and not p.is_symlink())
new={p.relative_to(root).as_posix():sha(p.read_bytes()) for p in files}
added=sorted(set(new)-set(old));removed=sorted(set(old)-set(new));modified=sorted(k for k in new.keys()&old.keys() if new[k]!=old[k])
assert not removed,removed
with ZipFile(full,'w',ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for p in files:z.write(p,p.relative_to(root).as_posix())
with ZipFile(full) as z:
 assert z.testzip() is None
 assert len(z.namelist())==len(new)
 for name in z.namelist():assert sha(z.read(name))==new[name],name
manifest={'format':'BRASEIRO_STAR_WARS_DELTA_V1','baseCheckpoint':'P218','baseSha256':sha(base.read_bytes()),'baseFileCount':len(old),
 'targetCheckpoint':'P219','targetFileCount':len(new),'added':added,'modified':modified,'removed':removed,'targetHashes':new}
with ZipFile(delta,'w',ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 z.writestr('MANIFEST.json',json.dumps(manifest,ensure_ascii=False,separators=(',',':')))
 for name in added+modified:z.write(root/name,'files/'+name)
with ZipFile(delta) as z:
 assert z.testzip() is None
 m=json.loads(z.read('MANIFEST.json'))
 with ZipFile(base) as b:rebuilt={n:sha(b.read(n)) for n in b.namelist()}
 for name in m['removed']:rebuilt.pop(name)
 for name in m['added']+m['modified']:rebuilt[name]=sha(z.read('files/'+name))
 assert rebuilt==new,'delta replay mismatch'
side=out/(full.name+'.sha256');side.write_text(f'{sha(full.read_bytes())}  {full.name}\n')
result={'full':str(full),'fullSha256':sha(full.read_bytes()),'fullSize':full.stat().st_size,'fullFiles':len(new),'delta':str(delta),
 'deltaSha256':sha(delta.read_bytes()),'deltaSize':delta.stat().st_size,'deltaEntries':len(added)+len(modified)+1,
 'added':len(added),'modified':len(modified),'removed':len(removed),'reconstructedHashes':len(new)}
(out/'P219_PACKAGE_MANIFEST.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,ensure_ascii=False))
