#!/usr/bin/env python3
"""352 OUA RGBA token edge despill. Pixels far from silhouette remain untouched."""
import zipfile,io,json,sys,hashlib
from pathlib import Path
import cv2,numpy as np
from PIL import Image

SRC=Path(sys.argv[1]);OUT=Path(sys.argv[2])
OUT.parent.mkdir(parents=True,exist_ok=True)
changes={'assets':0,'adjusted_pixels':0,'erased_fringe':0,'despill_pixels':0}

def cleaned(raw):
    image=Image.open(io.BytesIO(raw)).convert('RGBA')
    a=np.asarray(image).copy()
    rgb=a[:,:,:3].astype(np.float32)
    alpha=a[:,:,3].astype(np.float32)
    dist=cv2.distanceTransform((alpha>12).astype(np.uint8),cv2.DIST_L2,3)
    r,g,b=rgb[:,:,0],rgb[:,:,1],rgb[:,:,2]
    strongest=(g>55)&(g>r*1.75+9)&(g>b*1.55+9)&(g-r>40)&(g-b>32)
    medium=(g>45)&(g>r*1.32+10)&(g>b*1.22+9)&(g-r>21)&(g-b>14)
    fringe=strongest&(dist<=7.5)&(alpha>0)
    semitrans=medium&(alpha<240)&(dist<=12)&(alpha>0)
    halo=medium&(dist<=3.5)&(alpha>0)&~fringe
    modifier=fringe|semitrans|halo
    harsh=fringe&(dist<=2.4)&(g>90)&(g>2.1*np.maximum(r,b)+10)
    alpha[harsh]*=0.08
    partial=semitrans&~harsh
    alpha[partial]*=np.clip(1-(g[partial]-np.maximum(r[partial],b[partial]))/310,0.53,1.0)
    before=g.copy()
    target=np.maximum(r,b)*1.045+7
    strength=np.clip((g-np.maximum(r,b)-11)/75,0,1)
    g[modifier]=g[modifier]*(1-strength[modifier])+np.minimum(g[modifier],target[modifier])*strength[modifier]
    a[:,:,1]=np.clip(np.rint(g),0,255).astype(np.uint8)
    a[:,:,3]=np.clip(np.rint(alpha),0,255).astype(np.uint8)
    buf=io.BytesIO();Image.fromarray(a,'RGBA').save(buf,'PNG',compress_level=1)
    changes['assets']+=1
    changes['adjusted_pixels']+=int(modifier.sum())
    changes['erased_fringe']+=int(harsh.sum())
    changes['despill_pixels']+=int((before-g>8).sum())
    return buf.getvalue()

with zipfile.ZipFile(SRC) as zin,zipfile.ZipFile(OUT,'w') as zout:
    for info in zin.infolist():
        if info.is_dir():continue
        raw=zin.read(info.filename)
        if info.filename.lower().endswith('.png'):raw=cleaned(raw)
        zout.writestr(info.filename,raw,compress_type=zipfile.ZIP_DEFLATED,compresslevel=1)
    zout.writestr('GREEN_CLEANUP_REPORT.json',json.dumps({'version':'1','source_sha256':hashlib.sha256(SRC.read_bytes()).hexdigest(),'metrics':changes},indent=2))
with zipfile.ZipFile(OUT) as z:
    assert len([x for x in z.namelist() if x.lower().endswith('.png')])==352
    assert z.testzip() is None
print('OUA_49216_352_GREEN_FRINGES_CLEANED '+json.dumps(changes))
