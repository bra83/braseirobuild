#!/usr/bin/env python3
"""Rebuild R54 P242 byte-identically from verified P241 + delta P241->P242.
Usage: python tools/reconstruct_p242_from_p241.py P241.zip DELTA.zip OUTPUT.zip
The delta manifest includes both baseline and output SHA256, file counts and
all replaced/new entries. Nothing is downloaded and no existing file is erased.
"""
import hashlib,json,sys,zipfile
from pathlib import Path

def sha(p):
 h=hashlib.sha256()
 with open(p,'rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()

def build(base,delta,dest):
 with zipfile.ZipFile(delta) as d:
  manifest=json.loads(d.read('P242_DELTA_MANIFEST.json'))
  assert sha(base)==manifest['base_sha256'], 'P241 SHA mismatch'
  with zipfile.ZipFile(base) as b:
   assert len(b.namelist())==manifest['base_files']
   removed=set(manifest['removed']); changed=set(manifest['changed']); added=set(manifest['added'])
   assert not removed, 'P242 does not remove files'
   expected=set(b.namelist())|added
   assert set(manifest['output_file_list'])==expected
   assert changed<=set(b.namelist())
   assert set(d.namelist())==changed|added|{'P242_DELTA_MANIFEST.json'}
   with zipfile.ZipFile(dest,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6,allowZip64=True) as o:
    for name in manifest['output_file_list']:
     raw=d.read(name) if name in changed|added else b.read(name)
     info=zipfile.ZipInfo(name,date_time=(2026,10,15,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o644<<16;info.create_system=3
     o.writestr(info,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=6)
 assert sha(dest)==manifest['output_sha256'], 'P242 reconstructed SHA mismatch'
 with zipfile.ZipFile(dest) as z:assert z.testzip() is None and len(z.namelist())==manifest['output_files']
 print('P242_RECONSTRUCTION_BYTE_IDENTICAL_PASS',sha(dest),manifest['output_files'])
if __name__=='__main__':
 if len(sys.argv)!=4:raise SystemExit(__doc__)
 build(*sys.argv[1:])
