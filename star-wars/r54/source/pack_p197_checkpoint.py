from pathlib import Path
import zipfile,hashlib,json,os
root=Path(__file__).resolve().parent
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P196_SOURCE_CHECKPOINT.zip')
full=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P197_SOURCE_CHECKPOINT.zip')
delta=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P196_TO_P197_DELTA.zip')
def sha(data):return hashlib.sha256(data).hexdigest()
with zipfile.ZipFile(base) as z:
 old={n.filename:sha(z.read(n)) for n in z.infolist() if not n.is_dir()}
files={str(p.relative_to(root)):p for p in root.rglob('*') if p.is_file()}
new={name:sha(path.read_bytes()) for name,path in sorted(files.items())}
added=sorted(new.keys()-old.keys());removed=sorted(old.keys()-new.keys());modified=sorted(n for n in new.keys()&old.keys() if new[n]!=old[n])
manifest={'base':'P196','target':'P197','baseZipSha256':sha(base.read_bytes()),'added':added,'modified':modified,'removed':removed,'targetFileHashes':new}
assert manifest['baseZipSha256']=='02a62d595d6da473947227908f9e8ede675b1ed875671d1c0d2d94cb781eb7c5'
for out in [full,delta]:
 if out.exists():out.unlink()
with zipfile.ZipFile(full,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for name,path in sorted(files.items()):z.write(path,name)
with zipfile.ZipFile(delta,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 z.writestr('manifest.json',json.dumps(manifest,indent=2,ensure_ascii=False))
 for name in added+modified:z.write(files[name],'files/'+name)
with zipfile.ZipFile(full) as z:
 assert z.testzip() is None
 assert len([n for n in z.infolist() if not n.is_dir()])==len(new)
with zipfile.ZipFile(delta) as z:
 assert z.testzip() is None
 m=json.loads(z.read('manifest.json'))
 rebuilt=old.copy()
 for name in m['removed']:rebuilt.pop(name)
 for name in m['added']+m['modified']:rebuilt[name]=sha(z.read('files/'+name))
 assert rebuilt==new
result={'full':str(full),'size':full.stat().st_size,'sha256':sha(full.read_bytes()),'files':len(new),'delta':str(delta),'deltaSize':delta.stat().st_size,'deltaSha256':sha(delta.read_bytes()),'added':len(added),'modified':len(modified),'removed':len(removed),'deltaReconstructedHashes':len(rebuilt)}
Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P197_SOURCE_CHECKPOINT.zip.sha256').write_text(f"{result['sha256']}  {full.name}\n")
Path('/mnt/data/P197_PACKAGING_RESULT.json').write_text(json.dumps(result,indent=2))
print(json.dumps(result,indent=2))
