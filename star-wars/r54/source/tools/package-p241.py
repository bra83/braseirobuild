from pathlib import Path
from zipfile import ZipFile,ZipInfo,ZIP_DEFLATED
import hashlib,json,sys,subprocess,os
root=Path(__file__).resolve().parents[1];data=Path('/mnt/data')
base=data/'BRASEIRO_STAR_WARS_EDGE_R54_P240_SOURCE_CHECKPOINT.zip';target=data/'BRASEIRO_STAR_WARS_EDGE_R54_P241_SOURCE_CHECKPOINT.zip';delta=data/'BRASEIRO_STAR_WARS_EDGE_R54_P240_TO_P241_DELTA.zip'
sha=lambda b:hashlib.sha256(b).hexdigest()
def info(name):
 i=ZipInfo(name,(2026,10,15,0,0,0));i.compress_type=ZIP_DEFLATED;i.external_attr=(0o100644<<16);i.create_system=3;return i
files=sorted(p.relative_to(root).as_posix() for p in root.rglob('*') if p.is_file() and not p.is_symlink())
with ZipFile(base) as b:
 names=set(b.namelist());removed=sorted(names-set(files));assert not removed,removed[:10]
 changed=[];added=[];modified=[]
 for name in files:
  content=(root/name).read_bytes()
  if name not in names:changed.append(name);added.append(name)
  elif content!=b.read(name):changed.append(name);modified.append(name)
 print('added',len(added),'modified',len(modified),'removed',len(removed),'total',len(files),flush=True)
 with ZipFile(target,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as out:
  for name in files:out.writestr(info(name),(root/name).read_bytes(),compresslevel=6)
with ZipFile(target) as z:
 bad=z.testzip();assert bad is None,bad
 assert len(z.namelist())==len(files)
result_sha=sha(target.read_bytes());base_sha=sha(base.read_bytes())
manifest={'schema':'braseiro-starwars-delta-v1','base_name':base.name,'base_sha256':base_sha,'result_name':target.name,'result_sha256':result_sha,'files':files,'added':added,'modified':modified,'removed':removed,'changed_sha256':{name:sha((root/name).read_bytes()) for name in changed},'compression':'zipfile deflate level6; normalized metadata date 2026-10-15 00:00'}
reconstruct='''#!/usr/bin/env python3
# Usage: python reconstruct.py BASE_P240.zip DELTA.zip OUTPUT_P241.zip
from pathlib import Path
from zipfile import ZipFile,ZipInfo,ZIP_DEFLATED
import hashlib,json,sys
assert len(sys.argv)==4,'Usage: reconstruct.py BASE.zip DELTA.zip OUTPUT.zip'
base,delta,out=map(Path,sys.argv[1:]);sha=lambda b:hashlib.sha256(b).hexdigest()
with ZipFile(delta) as d,ZipFile(base) as b:
 m=json.loads(d.read('manifest.json'));assert sha(base.read_bytes())==m['base_sha256']
 with ZipFile(out,'w',compression=ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
  for name in m['files']:
   content=d.read('payload/'+name) if name in m['changed_sha256'] else b.read(name)
   if name in m['changed_sha256']:assert sha(content)==m['changed_sha256'][name]
   i=ZipInfo(name,(2026,10,15,0,0,0));i.compress_type=ZIP_DEFLATED;i.external_attr=(0o100644<<16);i.create_system=3
   z.writestr(i,content,compresslevel=6)
assert sha(out.read_bytes())==m['result_sha256']
with ZipFile(out) as z:assert z.testzip() is None
print('RECONSTRUCTED_BYTE_IDENTICAL',out,sha(out.read_bytes()))
'''
with ZipFile(delta,'w',compression=ZIP_DEFLATED,compresslevel=6) as z:
 z.writestr('manifest.json',json.dumps(manifest,separators=(',',':'),ensure_ascii=False))
 z.writestr('reconstruct.py',reconstruct)
 for name in changed:z.write(root/name,'payload/'+name)
script=data/'P241_RECONSTRUCT_FROM_P240.py';script.write_text(reconstruct)
rebuilt=data/'P241_RECONSTRUCTED_FROM_DELTA.zip';subprocess.run([sys.executable,str(script),str(base),str(delta),str(rebuilt)],check=True)
assert rebuilt.read_bytes()==target.read_bytes(),'not byte identical'
rebuilt.unlink()
summary=json.loads((root/'evidence/P241/regression_summary.json').read_text())
result={'checkpoint':'P241','base_sha256':base_sha,'source_zip':target.name,'size_bytes':target.stat().st_size,'sha256':result_sha,'files':len(files),'baseline_files':len(names),'baseline_preserved':not removed,'added':len(added),'modified':len(modified),'removed':len(removed),'crc':'PASS','delta_zip':delta.name,'delta_sha256':sha(delta.read_bytes()),'delta_reconstruction':'BYTE_IDENTICAL_PASS','test_files':summary['files'],'tests_pass':summary['pass'],'tests_fail':summary['fail'],'tests_skip':summary['skip'],'stress_pass':summary['stress_pass'],'android_source_version':'1.0.241','apk_built':False,'github_push':False,'ci':False,'device_test':False,'drive_readback_verified':False}
(data/'P241_INTEGRITY_REPORT.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n')
(data/(target.name+'.sha256')).write_text(result_sha+'  '+target.name+'\n')
print(json.dumps(result,ensure_ascii=False))
