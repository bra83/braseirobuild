#!/usr/bin/env python3
"""Verify RUN247+MOD005 source byte-for-byte against the audited base, offline.

The .json.gz.b64 manifest pins the 1437 original ZIP members (each SHA256).
The allowed-manifest pins only 10 explicit modifications. Nothing else passes.
Does not use RUN245 archives or infer validation from gradle task existence.
"""
from __future__ import annotations
import argparse,base64,gzip,hashlib,json,pathlib,sys

BASE_SHA='2774b450ecf5be179976199b22bab8584f76939dcf305d82a84171a199282e06'
EXPECTED_COUNT=1437

def sha256(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda:f.read(1048576),b''):h.update(chunk)
    return h.hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--root',type=pathlib.Path,required=True);p.add_argument('--baseline',type=pathlib.Path,required=True);p.add_argument('--allowed-manifest',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True)
    args=p.parse_args(); b=json.loads(gzip.decompress(base64.b64decode(args.baseline.read_text().strip(),validate=True)))
    if b.get('schema')!='dnd_normal_reconciled_sha256_manifest_v1' or b.get('source_archive_sha256')!=BASE_SHA: raise RuntimeError('BAD_BASELINE_MANIFEST_IDENTITY')
    base=b['entries'];allowed=json.loads(args.allowed_manifest.read_text(encoding='utf-8'))['expected_modified']
    if len(base)!=EXPECTED_COUNT or not isinstance(allowed,dict) or len(allowed)!=10: raise RuntimeError('WRONG_BASE_OR_DELTA_SIZE')
    if set(allowed)-set(base):raise RuntimeError('DELTA_CONTAINS_PATH_NOT_IN_ORIGINAL_BASE')
    found={x.relative_to(args.root).as_posix():x for x in args.root.rglob('*') if x.is_file()}
    # The Gradle project root excludes .git; source tree is exactly archived members.
    if set(found)!=set(base):
        missing=sorted(set(base)-set(found));added=sorted(set(found)-set(base))
        raise RuntimeError('EXACT_SOURCE_FILESET_MISMATCH missing='+repr(missing[:12])+' added='+repr(added[:12]))
    changed=[]
    for path,fp in sorted(found.items()):
        actual=sha256(fp);expected=allowed.get(path,base[path]);
        if actual!=expected:raise RuntimeError('SOURCE_FILE_SHA256_MISMATCH '+path+' actual='+actual+' expected='+expected)
        if path in allowed:
            if actual==base[path]:raise RuntimeError('PINNED_MODIFICATION_DID_NOT_CHANGE '+path)
            changed.append(path)
    if len(changed)!=10: raise RuntimeError('DELTA_MODIFIED_COUNT_MISMATCH')
    ident=(args.root/'version.properties').read_text(encoding='utf-8')
    for pair in ['VERSION_CODE=606547','DB_SCHEMA=61','BACKUP_FORMAT=21']:
        if pair not in ident:raise RuntimeError('PROJECT_IDENTITY_MISMATCH '+pair)
    if 'com.braseiro.dnd5e' not in (args.root/'app/build.gradle.kts').read_text():raise RuntimeError('PACKAGE_IDENTITY_MISMATCH')
    lines=['VERIFICATION=EXACT_SOURCE_TREE_SHA256_PASS','BASELINE_ORIGINAL_ZIP_SHA256='+BASE_SHA,'FILES='+str(len(found)),'CHANGED='+str(len(changed)),'APP_PACKAGE=com.braseiro.dnd5e','VERSION_CODE=606547','DB_SCHEMA=61','BACKUP_FORMAT=21','G05_HOMOLOGATED=NO','RESULT=PASS']
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text('\n'.join(lines)+'\n',encoding='utf-8');print('\n'.join(lines),flush=True)

if __name__=='__main__':
    try:main()
    except Exception as e:
        print('PINNED_SOURCE_TREE_VERIFICATION_FAILED='+str(e),file=sys.stderr,flush=True);sys.exit(2)
