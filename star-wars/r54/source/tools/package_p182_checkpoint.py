#!/usr/bin/env python3
"""Reproducible P182 full Android Studio source ZIP + delta + manifest."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import hashlib,json,os,datetime,sys
root=Path(__file__).resolve().parent.parent
out=Path('/mnt/data')
base=out/'BRASEIRO_STAR_WARS_EDGE_R54_P181_SOURCE_CHECKPOINT.zip'
full=out/'BRASEIRO_STAR_WARS_EDGE_R54_P182_SOURCE_CHECKPOINT.zip'
delta=out/'BRASEIRO_STAR_WARS_EDGE_R54_P181_TO_P182_DELTA.zip'
ignore={'mask_source.txt','regular.log','regular_final.log','regular_verified.log','stress.log','runner.pid','runner_verified.pid'}
files=sorted(p for p in root.rglob('*') if p.is_file() and p.name not in ignore and '__pycache__' not in p.parts)
rel=lambda p:p.relative_to(root).as_posix()
hashbytes=lambda b:hashlib.sha256(b).hexdigest()
old={}
with ZipFile(base) as z:
 for info in z.infolist():
  if not info.is_dir():old[info.filename]=hashbytes(z.read(info.filename))
new={rel(p):hashbytes(p.read_bytes()) for p in files}
changed=[p for p in files if old.get(rel(p))!=new[rel(p)]]
deleted=sorted(set(old)-set(new))
manifest={'checkpoint':'P182','base':'P181','baseSha256':hashbytes(base.read_bytes()),'added':sum(rel(p) not in old for p in changed),'modified':sum(rel(p) in old for p in changed),'deleted':deleted,'newFileCount':len(files),'changedFiles':[rel(p) for p in changed],'sha256ByPath':new}
with ZipFile(full,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for p in files:z.write(p,rel(p))
with ZipFile(delta,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 z.writestr('P182_DELTA_MANIFEST.json',json.dumps(manifest,ensure_ascii=False,indent=2))
 for p in changed:z.write(p,rel(p))
for p in [full,delta]:
 with ZipFile(p) as z:
  bad=z.testzip()
  if bad:raise RuntimeError(f'ZIP CRC failed {p}: {bad}')
  print(p.name,'entries',len(z.namelist()),'bytes',p.stat().st_size,'sha256',hashbytes(p.read_bytes()))
sha=hashbytes(full.read_bytes())
(out/(full.name+'.sha256')).write_text(f'{sha}  {full.name}\n')
(out/'BRASEIRO_STAR_WARS_EDGE_R54_P182_DELTA_MANIFEST.json').write_text(json.dumps({k:v for k,v in manifest.items() if k!='sha256ByPath'},ensure_ascii=False,indent=2)+'\n')
print('CHANGED',len(changed),'DELETED',len(deleted),'NEW',len(files))
