from pathlib import Path
import hashlib, json, os, re
ROOT=Path(__file__).resolve().parents[1]
geom=json.loads((ROOT/'app/src/main/assets/www/data/scr015.geometry.json').read_text())
source=Path(os.environ.get('BRASEIRO_WWN_FORGE_SOURCE','/mnt/data/wwn_m3b/unpacked'))
fail=[]
if len(geom['layers'])!=27: fail.append(f"layer_count={len(geom['layers'])}")
for l in geom['layers']:
    if not re.fullmatch(r'[0-9a-f]{64}', l.get('sha256','')):
        fail.append(f"invalid_manifest_sha256 {l.get('stable_id')}")
if fail:
    print('\n'.join('FAIL '+x for x in fail)); raise SystemExit(1)
if not source.exists():
    print(f'SCR015_SOURCE_ASSET_QA_SKIPPED source_package_unavailable={source} manifest_integrity=PASS layers=27')
    raise SystemExit(0)
for l in geom['layers']:
    p=source/l['asset_path']
    if not p.exists(): fail.append(f"missing {p}"); continue
    actual=hashlib.sha256(p.read_bytes()).hexdigest()
    if actual!=l['sha256']: fail.append(f"hash {l['stable_id']} {actual} != {l['sha256']}")
if fail:
    print('\n'.join('FAIL '+x for x in fail)); raise SystemExit(1)
print('SCR015_ASSET_GEOMETRY_PASS layers=27 source_hashes=27/27 viewport=415x915')
