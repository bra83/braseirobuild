#!/usr/bin/env python3
"""Guard a pre-existing source subtree against silent replacement on CI reruns."""
import argparse
import hashlib
import json
import pathlib
import sys

def inventory(root):
    result = {}
    for f in root.rglob('*'):
        if f.is_file():
            if f.is_symlink():
                raise RuntimeError('SYMLINK_REFUSED:' + str(f))
            rel = f.relative_to(root).as_posix()
            result[rel] = hashlib.sha256(f.read_bytes()).hexdigest()
    return result

if __name__ == '__main__':
    ap=argparse.ArgumentParser()
    ap.add_argument('--reference', type=pathlib.Path, required=True)
    ap.add_argument('--existing', type=pathlib.Path, required=True)
    ap.add_argument('--allowed-manifest', type=pathlib.Path)
    args=ap.parse_args()
    a, b = inventory(args.reference), inventory(args.existing)
    if args.allowed_manifest:
        manifest=json.loads(args.allowed_manifest.read_text(encoding='utf-8'))
        allowed=manifest['expected_modified']
        if len(a)!=manifest['baseline_file_count'] or a.keys()!=b.keys():
            raise RuntimeError('SOURCE_FILE_INVENTORY_LOST_OR_ADDED')
        changed=set()
        newline_equivalent=[]
        text_extensions={'.java','.kt','.kts','.json','.csv','.md','.txt','.patch','.log','.gradle','.properties','.xml','.yaml','.yml','.py','.sh','.bat','.html','.css','.js'}
        for name in sorted(a.keys()):
            if a[name]==b[name]:
                continue
            if name in allowed and b[name]==allowed[name]:
                changed.add(name)
                continue
            source_bytes=(args.reference/name).read_bytes()
            checkout_bytes=(args.existing/name).read_bytes()
            extension=pathlib.PurePosixPath(name).suffix.lower()
            if (extension in text_extensions or name.startswith('.git')) and b'\x00' not in source_bytes and b'\x00' not in checkout_bytes and source_bytes.replace(b'\r\n',b'\n')==checkout_bytes.replace(b'\r\n',b'\n'):
                newline_equivalent.append(name)
                continue
            print('SOURCE_DRIFT_NOT_WHITELISTED='+name+' SHA_CHECKOUT='+b[name],file=sys.stderr)
            sys.exit(3)
        if changed!=set(allowed):
            print('PINNED_FIX_NOT_PRESENT:'+str(set(allowed)-changed),file=sys.stderr)
            sys.exit(4)
        print('SOURCE_EXACT_BASE_PLUS_PINNED_FIX=YES FILES='+str(len(a))+' FIXED='+str(len(changed)))
        print('WINDOWS_CRLF_LF_ONLY_EQUIVALENCES='+str(len(newline_equivalent)))
        print('SOURCE_EXTRA_OR_MISSING=0 SOURCE_UNVERIFIED_SEMANTIC_DRIFT=0')
        sys.exit(0)
    if a != b:
        only_a=sorted(a.keys()-b.keys())
        only_b=sorted(b.keys()-a.keys())
        dif=sorted(k for k in a.keys()&b.keys() if a[k]!=b[k])
        print(f'SOURCE_TREE_MISMATCH:only_archive={only_a[:8]} only_existing={only_b[:8]} differing={dif[:8]}',file=sys.stderr)
        sys.exit(2)
    print(f'SOURCE_TREE_EXACT_MATCH=YES FILES={len(a)}')
