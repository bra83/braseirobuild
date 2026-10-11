#!/usr/bin/env python3
"""Verify RUN247+MOD005 source against audited hashes, offline.

The .json.gz.b64 manifest pins the 1437 original ZIP members (each SHA256).
The allowed-manifest pins only 17 explicit modifications. Text files may also
match independently pinned LF hashes derived from the exact original archive.
Binary bytes and every non-newline text byte remain strictly checked.
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

def text_lf_sha256(path):
    raw=path.read_bytes()
    if b'\x00' in raw:return None
    try:raw.decode('utf-8')
    except UnicodeDecodeError:return None
    return hashlib.sha256(raw.replace(b'\r\n',b'\n')).hexdigest()

def main():
    p=argparse.ArgumentParser();p.add_argument('--root',type=pathlib.Path,required=True);p.add_argument('--baseline',type=pathlib.Path,required=True);p.add_argument('--allowed-manifest',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True)
    args=p.parse_args(); b=json.loads(gzip.decompress(base64.b64decode(args.baseline.read_text().strip(),validate=True)))
    if b.get('schema')!='dnd_normal_reconciled_sha256_manifest_v1' or b.get('source_archive_sha256')!=BASE_SHA: raise RuntimeError('BAD_BASELINE_MANIFEST_IDENTITY')
    base=b['entries'];allowed=json.loads(args.allowed_manifest.read_text(encoding='utf-8'))['expected_modified']
    if len(base)!=EXPECTED_COUNT or not isinstance(allowed,dict) or len(allowed)!=17: raise RuntimeError('WRONG_BASE_OR_DELTA_SIZE')
    if set(allowed)-set(base):raise RuntimeError('DELTA_CONTAINS_PATH_NOT_IN_ORIGINAL_BASE')
    eol=b.get('git_lf_equivalences',{})
    for path,pins in eol.items():
        if path not in base or pins.get('archive_sha256')!=base[path] or not isinstance(pins.get('git_lf_sha256'),str) or len(pins['git_lf_sha256'])!=64:
            raise RuntimeError('INVALID_PINNED_EOL_EQUIVALENCE '+path)
    found={x.relative_to(args.root).as_posix():x for x in args.root.rglob('*') if x.is_file()}
    # The Gradle project root excludes .git; source tree is exactly archived members.
    if set(found)!=set(base):
        missing=sorted(set(base)-set(found));added=sorted(set(found)-set(base))
        raise RuntimeError('EXACT_SOURCE_FILESET_MISMATCH missing='+repr(missing[:12])+' added='+repr(added[:12]))
    changed=[];normalized=[]
    for path,fp in sorted(found.items()):
        actual=sha256(fp);expected=allowed.get(path,base[path]);
        if actual!=expected:
            # Never derive a new expected hash from the checkout under test.
            # Base LF hashes were proved against the original ZIP; explicit
            # production deltas are already pinned as UTF-8 LF in their manifest.
            expected_lf=expected if path in allowed else eol.get(path,{}).get('git_lf_sha256',expected)
            if expected_lf is None or text_lf_sha256(fp)!=expected_lf:
                raise RuntimeError('SOURCE_FILE_SHA256_MISMATCH '+path+' actual='+actual+' expected='+expected)
            normalized.append(path)
        if path in allowed:
            if actual==base[path]:raise RuntimeError('PINNED_MODIFICATION_DID_NOT_CHANGE '+path)
            changed.append(path)
    if len(changed)!=17: raise RuntimeError('DELTA_MODIFIED_COUNT_MISMATCH')
    ident=(args.root/'version.properties').read_text(encoding='utf-8')
    for pair in ['VERSION_CODE=606547','DB_SCHEMA=61','BACKUP_FORMAT=21']:
        if pair not in ident:raise RuntimeError('PROJECT_IDENTITY_MISMATCH '+pair)
    if 'com.braseiro.dnd5e' not in (args.root/'app/build.gradle.kts').read_text():raise RuntimeError('PACKAGE_IDENTITY_MISMATCH')
    lines=['VERIFICATION=PINNED_SOURCE_TREE_SHA256_PASS','BASELINE_ORIGINAL_ZIP_SHA256='+BASE_SHA,'FILES='+str(len(found)),'CHANGED='+str(len(changed)),'PINNED_CRLF_LF_EQUIVALENCES_USED='+str(len(normalized)),'APP_PACKAGE=com.braseiro.dnd5e','VERSION_CODE=606547','DB_SCHEMA=61','BACKUP_FORMAT=21','G05_HOMOLOGATED=NO','RESULT=PASS']
    args.output.parent.mkdir(parents=True,exist_ok=True);args.output.write_text('\n'.join(lines)+'\n',encoding='utf-8');print('\n'.join(lines),flush=True)

if __name__=='__main__':
    try:main()
    except Exception as e:
        print('PINNED_SOURCE_TREE_VERIFICATION_FAILED='+str(e),file=sys.stderr,flush=True);sys.exit(2)
