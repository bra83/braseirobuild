from pathlib import Path
import hashlib,zipfile,json,os,time
root=Path(__file__).parent
basepath=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P225_SOURCE_CHECKPOINT.zip')
full=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P226_SOURCE_CHECKPOINT.zip')
delta=Path('/mnt/data/BRASEIRO_STAR_WARS_EDGE_R54_P225_TO_P226_DELTA.zip')
sha=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(basepath) as z:
 if z.testzip() is not None:raise Exception('P225 ZIP CRC FAILED')
 old={i.filename:sha(z.read(i.filename)) for i in z.infolist() if not i.is_dir()}
basehash=sha(basepath.read_bytes())
if basehash!='5feff46606aece0f8a4703634c4bc876dfe8e15e0cf23e44581ccaf5b2f3f88f':raise Exception('BASE HASH MISMATCH')
files={}
for p in root.rglob('*'):
 if not p.is_file() or any(x in ('.git','__pycache__') for x in p.relative_to(root).parts):continue
 rel=p.relative_to(root).as_posix()
 files[rel]=p
removed=sorted(set(old)-set(files))
if removed:raise Exception('DELETIONS FORBIDDEN '+str(removed[:20]))
new={name:sha(p.read_bytes()) for name,p in sorted(files.items())}
added=sorted(set(new)-set(old));modified=sorted(name for name in old if old[name]!=new[name]);unchanged=len(old)-len(modified)
with zipfile.ZipFile(full,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for name,p in sorted(files.items()):z.write(p,name)
with zipfile.ZipFile(full) as z:
 bad=z.testzip(); names=z.namelist()
 if bad or len(names)!=len(files):raise Exception('TARGET ZIP CRC FAIL '+str(bad))
 if any(sha(z.read(n))!=new[n] for n in names):raise Exception('TARGET HASH FAIL')
manifest={'format':'BRASEIRO_STAR_WARS_DELTA_V1','baseCheckpoint':'P225','baseSha256':basehash,'baseFileCount':len(old),'targetCheckpoint':'P226','targetFileCount':len(new),'added':added,'modified':modified,'removed':removed,'targetHashes':new}
with zipfile.ZipFile(delta,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr('MANIFEST.json',json.dumps(manifest,ensure_ascii=False,separators=(',',':')))
 for name in added+modified:z.write(files[name],'files/'+name)
with zipfile.ZipFile(delta) as z:
 if z.testzip():raise Exception('DELTA CRC FAIL')
 m=json.loads(z.read('MANIFEST.json'))
 replay=dict(old)
 for name in m['removed']:replay.pop(name)
 for name in m['added']+m['modified']:replay[name]=sha(z.read('files/'+name))
 if replay!=m['targetHashes']:raise Exception('REPLAY HASH FAIL')
 sidecar=full.with_name(full.name+'.sha256');sidecar.write_text(f'{sha(full.read_bytes())}  {full.name}\n')
 report={'checkpoint':'P226','baseSha256':basehash,'baseFileCount':len(old),'targetZip':str(full),'targetSize':full.stat().st_size,'targetSha256':sha(full.read_bytes()),'targetFileCount':len(new),'targetCrc':'PASS','deltaZip':str(delta),'deltaSize':delta.stat().st_size,'deltaSha256':sha(delta.read_bytes()),'deltaCrc':'PASS','added':len(added),'modified':len(modified),'removed':len(removed),'unchanged':unchanged,'replayedHashes':len(replay),'replay':'PASS','sidecar':str(sidecar)}
 Path('/mnt/data/PACKAGING_P226.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
