from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED,ZipInfo
import hashlib,json
root=Path(__file__).resolve().parents[1]
base=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P226_SOURCE_CHECKPOINT.zip')
target=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P227_SOURCE_CHECKPOINT.zip')
delta=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P226_TO_P227_DELTA.zip')
sha=lambda b:hashlib.sha256(b).hexdigest()
with ZipFile(base) as z:
 old={name:sha(z.read(name)) for name in z.namelist() if not name.endswith('/')}
 assert z.testzip() is None
files=lambda:sorted(p for p in root.rglob('*') if p.is_file() and not any(x in p.parts for x in ['.git','__pycache__']))
manifest=root/'P227_FILE_MANIFEST_SHA256.json'
initial={p.relative_to(root).as_posix():sha(p.read_bytes()) for p in files() if p!=manifest}
manifest.write_text(json.dumps({'checkpoint':'P227','base':'P226','algorithm':'sha256','note':'Manifest excludes itself to avoid recursive hash','files':initial},ensure_ascii=False,sort_keys=True,indent=2)+'\n')
current={p.relative_to(root).as_posix():sha(p.read_bytes()) for p in files()}
assert set(old).issubset(current),f'Deleted paths: {set(old)-set(current)}'
added=sorted(set(current)-set(old));modified=sorted(k for k in old if old[k]!=current[k]);unchanged=len(old)-len(modified)
assert current[manifest.name]==sha(manifest.read_bytes())
with ZipFile(target,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for p in files():
  key=p.relative_to(root).as_posix();info=ZipInfo(key,(2026,10,15,0,0,0));info.compress_type=ZIP_DEFLATED;info.external_attr=0o644<<16
  z.writestr(info,p.read_bytes(),compress_type=ZIP_DEFLATED,compresslevel=6)
with ZipFile(target) as z:
 assert len(z.namelist())==len(current)
 assert z.testzip() is None
 actual={name:sha(z.read(name)) for name in z.namelist()}
 assert actual==current
manifest_delta={'format':'braseiro-star-wars-delta-v1','baseCheckpoint':'P226','targetCheckpoint':'P227','baseZipSha256':sha(base.read_bytes()),'targetZipSha256':sha(target.read_bytes()),'baseFileCount':len(old),'targetFileCount':len(current),'added':added,'modified':modified,'removed':[],'targetFiles':current}
with ZipFile(delta,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr('DELTA_MANIFEST.json',json.dumps(manifest_delta,ensure_ascii=False,sort_keys=True,indent=2)+'\n')
 for key in added+modified:
  info=ZipInfo(key,(2026,10,15,0,0,0));info.compress_type=ZIP_DEFLATED;z.writestr(info,(root/key).read_bytes(),compress_type=ZIP_DEFLATED,compresslevel=6)
with ZipFile(delta) as z:
 assert z.testzip() is None
 delta_data=json.loads(z.read('DELTA_MANIFEST.json'))
 replay=old.copy()
 for key in added+modified:replay[key]=sha(z.read(key))
 assert replay==delta_data['targetFiles']==current
 assert len(z.namelist())==1+len(added)+len(modified)
sidecar=Path(str(target)+'.sha256');sidecar.write_text(f'{sha(target.read_bytes())}  {target.name}\n')
result={'checkpoint':'P227','baseZipSha256':sha(base.read_bytes()),'targetZip':str(target),'targetZipSha256':sha(target.read_bytes()),'targetBytes':target.stat().st_size,'targetFiles':len(current),'targetCrc':'PASS','deltaZip':str(delta),'deltaZipSha256':sha(delta.read_bytes()),'deltaBytes':delta.stat().st_size,'deltaCrc':'PASS','deltaAdded':len(added),'deltaModified':len(modified),'deltaRemoved':0,'unchanged':unchanged,'replayHashes':len(replay),'replay':'PASS','sidecar':str(sidecar)}
Path('/mnt/data/P227_INTEGRITY_REPORT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(result,ensure_ascii=False,indent=2))
