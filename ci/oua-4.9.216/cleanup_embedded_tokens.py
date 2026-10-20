#!/usr/bin/env python3
"""Remove vivid chroma-green anti-alias fringes from embedded OUA token PNGs.
Only near transparent edges; maintain original pixel dimensions, visible
clothes, and token manifest. Uses the verified 352-source pack, never re-draws.
"""
import sys, pathlib, json
import cv2
import numpy as np
from PIL import Image
root=pathlib.Path(sys.argv[1])
base=root/'public/tor2e-ui'
manifest=json.loads((base/'hero-token-pack-352.json').read_text())
assert manifest['tokenCount']==352 and len(manifest['tokens'])==352
changed=0
pixels=0
for t in manifest['tokens']:
    p=base/'assets'/t['asset']
    with Image.open(p) as im:
        im.load()
        assert im.mode=='RGBA' and im.size==(448,448), str(p)
        rgba=np.asarray(im).copy()
    color=rgba[:,:,:3].astype(np.float32)
    alpha=rgba[:,:,3].astype(np.float32)
    r,g,b=color[:,:,0],color[:,:,1],color[:,:,2]
    other=np.maximum(r,b)
    dominance=np.maximum(0,g-other)
    dist=cv2.distanceTransform((alpha>12).astype(np.uint8),cv2.DIST_L2,5)
    green=np.clip((dominance-12)/42,0,1)*np.clip((g/(other+12)-1.12)/.72,0,1)
    edge=np.clip((7-dist)/6,0,1)*green*(alpha>0)
    strong=np.clip((green-.46)/.42,0,1)
    corrected_alpha=alpha*(1-.97*edge*(.72+.28*strong))
    corrected_green=g-np.maximum(g-other-5,0)*edge*.95
    rgba[:,:,1]=np.clip(corrected_green,0,255).astype(np.uint8)
    rgba[:,:,3]=np.clip(corrected_alpha,0,255).astype(np.uint8)
    rgba[rgba[:,:,3]==0,:3]=0
    edited=int(np.count_nonzero(alpha-corrected_alpha>3))
    if edited:
        Image.fromarray(rgba,'RGBA').save(p,format='PNG',compress_level=6)
        changed+=1
        pixels+=edited
assert changed>=340, 'Expected most tokens to contain green edge spill'
assert pixels>1000000,'Too few chroma-key fringe pixels changed'
print(f'OUA_49216_GREEN_SPILL_CLEANUP_PASS tokens=352 changed={changed} affected_pixels={pixels}')
