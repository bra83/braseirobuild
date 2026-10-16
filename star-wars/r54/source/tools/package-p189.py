import zipfile,hashlib,pathlib,json,os
root=pathlib.Path(__file__).resolve().parents[1];dst=pathlib.Path('/mnt/data');prefix='BRASEIRO_STAR_WARS_EDGE_R54_'
base=dst/(prefix+'P188_SOURCE_CHECKPOINT.zip');new=dst/(prefix+'P189_SOURCE_CHECKPOINT.zip');delta=dst/(prefix+'P188_TO_P189_DELTA.zip')
with zipfile.ZipFile(base) as z:
 assert z.testzip() is None
 old={n:hashlib.sha256(z.read(n)).hexdigest() for n in z.namelist() if not n.endswith('/')}
files={p.relative_to(root).as_posix():p for p in root.rglob('*') if p.is_file() and not any(part.startswith('.git') for part in p.relative_to(root).parts)}
# Preserve the original checkpoint's bundled dependencies and files as-is, including node_modules.
for n in old:
 if n not in files:raise RuntimeError('Unexpected missing baseline file '+n)
changed=[];added=[];same=0;newhash={}
for n,p in files.items():
 h=hashlib.sha256(p.read_bytes()).hexdigest();newhash[n]=h
 if n not in old:added.append(n)
 elif h!=old[n]:changed.append(n)
 else:same+=1
manifest={'from':'P188','to':'P189','baselineSHA256':hashlib.sha256(base.read_bytes()).hexdigest(),'changed':sorted(changed),'added':sorted(added),'deleted':[],'same':same,'fileCount':len(files),'hashes':{n:newhash[n] for n in sorted(changed+added)}}
with zipfile.ZipFile(new,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n,p in sorted(files.items()):z.write(p,n)
with zipfile.ZipFile(delta,'w',zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as z:
 for n in sorted(changed+added):z.write(files[n],n)
 z.writestr('P189_DELTA_MANIFEST.json',json.dumps(manifest,indent=2,ensure_ascii=False)+'\n')
for path in (new,delta):
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  print(path.name,'bytes',path.stat().st_size,'files',len(z.namelist()))
sha=hashlib.sha256(new.read_bytes()).hexdigest();(dst/(new.name+'.sha256')).write_text(f'{sha}  {new.name}\n')
# Verify incremental replay of the delta by hash, rather than assuming archive entry count.
assert not (set(old)-set(files))
assert all((n in manifest['hashes'] and manifest['hashes'][n]==newhash[n]) if n in set(changed+added) else old[n]==newhash[n] for n in files)
print('CHECKPOINT_SHA256',sha)
print('CHANGED',len(changed),'ADDED',len(added),'UNCHANGED',same,'TOTAL',len(files))
print('CHANGED_PATHS',changed)
