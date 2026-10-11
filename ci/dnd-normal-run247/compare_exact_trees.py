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
        dif={k:b[k] for k in a if a[k]!=b[k]}
        if dif != allowed:
            print('SOURCE_FIX_DELTA_MISMATCH:'+str(dif),file=sys.stderr)
            sys.exit(3)
        print('SOURCE_EXACT_BASE_PLUS_PINNED_FIX=YES FILES='+str(len(a))+' FIXED='+str(len(dif)))
        sys.exit(0)
    if a != b:
        only_a=sorted(a.keys()-b.keys())
        only_b=sorted(b.keys()-a.keys())
        dif=sorted(k for k in a.keys()&b.keys() if a[k]!=b[k])
        print(f'SOURCE_TREE_MISMATCH:only_archive={only_a[:8]} only_existing={only_b[:8]} differing={dif[:8]}',file=sys.stderr)
        sys.exit(2)
    print(f'SOURCE_TREE_EXACT_MATCH=YES FILES={len(a)}')
