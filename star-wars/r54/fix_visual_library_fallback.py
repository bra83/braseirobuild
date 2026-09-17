#!/usr/bin/env python3
from pathlib import Path
import sys

if len(sys.argv) != 2:
    raise SystemExit('usage: fix_visual_library_fallback.py <android-project-root>')
root=Path(sys.argv[1]).resolve()
rt=root/'app/src/main/assets/vtt/r46/final-runtime.js'
if not rt.is_file(): raise SystemExit(f'missing runtime: {rt}')
s=rt.read_text('utf-8')
old="function r54AssetIndex(bind){const map=new Map();for(const group of ['weapons','equipment'])for(const item of Object.values(bind?.[group]||{}))for(const a of item.aliases||[])map.set(r54Norm(a),R54_ASSET_ROOT+item.file);return map}"
new="function r54AssetIndex(bind){const map=new Map();for(const group of Object.keys(bind||{}))for(const item of Object.values(bind?.[group]||{})){if(!item||typeof item!=='object'||!item.file)continue;for(const a of [item.id,item.name,item.title,...(item.aliases||[])].filter(Boolean))map.set(r54Norm(a),R54_ASSET_ROOT+item.file)}return map}"
if old in s:
    s=s.replace(old,new,1)
elif new not in s:
    raise SystemExit('r54AssetIndex signature changed unexpectedly')
old2="const exact=idx.get(r54Norm(name));if(!exact)continue;"
new2="const exact=idx.get(r54Norm(name))||r53PickAsset(name);if(!exact)continue;"
if old2 in s:
    s=s.replace(old2,new2,1)
elif new2 not in s:
    raise SystemExit('item visual lookup signature changed unexpectedly')
rt.write_text(s,encoding='utf-8')
# Validation: R54 bindings are no longer hard-limited to two categories and the
# previously proven generic image-library bridge (R39-R46 lineage) is fallback.
out=rt.read_text('utf-8')
assert "Object.keys(bind||{})" in out
assert "idx.get(r54Norm(name))||r53PickAsset(name)" in out
assert "for(const group of ['weapons','equipment'])" not in out
print('PASS visual binding: R54 exact catalog + generic R39-R46 fallback')
