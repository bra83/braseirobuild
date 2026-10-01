import json, os, sys, math
from pathlib import Path
from PIL import Image, ImageOps, ImageDraw
import numpy as np

root=Path(sys.argv[1])
out=Path(sys.argv[2]); out.mkdir(parents=True,exist_ok=True)
manifest_path=root/'public/tor2e-ui/party-token-manifest.json'
m=json.loads(manifest_path.read_text(encoding='utf-8'))
base=root/'public/tor2e-ui/assets'
groups={}
for e in m['tokens']:
    groups.setdefault(e.get('sourceSheet') or e['id'],[]).append(e)

def load(e):
    return Image.open(base/e['asset']).convert('RGBA')

def stitch(entries, cols):
    entries=sorted(entries,key=lambda e:e.get('sourceIndex',0))
    ims=[load(e) for e in entries]
    w=max(i.width for i in ims); h=max(i.height for i in ims)
    rows=math.ceil(len(ims)/cols)
    canvas=Image.new('RGBA',(w*cols,h*rows),(0,0,0,0))
    for k,im in enumerate(ims):
        x=(k%cols)*w; y=(k//cols)*h
        canvas.alpha_composite(im,(x,y))
    return canvas

def fg_mask(im):
    a=np.asarray(im)
    alpha=a[:,:,3]
    transparent=(alpha<240).mean()
    if transparent>0.01:
        mask=alpha>12
    else:
        rgb=a[:,:,:3].astype(np.int16)
        # source uses black background; retain dark character pixels adjacent to brighter pixels later
        mask=np.max(rgb,axis=2)>14
    return (mask.astype(np.uint8)*255)

def seam_score(entries, cols):
    entries=sorted(entries,key=lambda e:e.get('sourceIndex',0))
    arr=[np.asarray(load(e),dtype=np.int16) for e in entries]
    rows=math.ceil(len(arr)/cols)
    score=0.0; n=0
    for k,A in enumerate(arr):
        row,col=divmod(k,cols)
        if col+1<cols and k+1<len(arr):
            B=arr[k+1]
            ah=A[:,-1,:]; bh=B[:,0,:]
            active=(ah[:,3]>10)|(bh[:,3]>10)|(np.max(ah[:,:3],1)>14)|(np.max(bh[:,:3],1)>14)
            if active.any():
                score+=np.abs(ah[active,:3]-bh[active,:3]).mean(); n+=1
        j=k+cols
        if row+1<rows and j<len(arr):
            B=arr[j]
            ah=A[-1,:,:]; bh=B[0,:,:]
            active=(ah[:,3]>10)|(bh[:,3]>10)|(np.max(ah[:,:3],1)>14)|(np.max(bh[:,:3],1)>14)
            if active.any():
                score+=np.abs(ah[active,:3]-bh[active,:3]).mean(); n+=1
    return score/max(1,n)

def preview(im,path,label):
    maxw=900
    if im.width>maxw:
        h=round(im.height*maxw/im.width); im=im.resize((maxw,h),Image.Resampling.LANCZOS)
    bg=Image.new('RGB',im.size,(8,8,8)); bg.paste(im,mask=im.getchannel('A'))
    draw=ImageDraw.Draw(bg); draw.rectangle((0,0,min(bg.width,500),28),fill=(0,0,0)); draw.text((8,7),label,fill=(255,255,255))
    bg.save(path,quality=88)

lines=[]
for source,entries in sorted(groups.items()):
    dims=[(load(e).width,load(e).height) for e in entries]
    s2=seam_score(entries,2); s3=seam_score(entries,3)
    chosen=2 if s2<=s3 else 3
    lines.append(f'{source}\tcount={len(entries)}\tdims={dims[0]}\tseam2={s2:.2f}\tseam3={s3:.2f}\tchosen_cols={chosen}')
    if source in {
        'seis_hobbits_aventureiros_em_ação.png',
        'folha_de_heróis_anões_fantásticos.png',
        'folha_de_personagens_viajantes_fantásticos.png',
        'guerreiros_de_rohan_em_folha_de_arte.png',
        'seis_heróis_de_gondor_em_aquarela.png',
    }:
        preview(stitch(entries,2),out/(entries[0]['id'].rsplit('_',1)[0]+'_2cols.jpg'),f'{source} 2 cols')
        preview(stitch(entries,3),out/(entries[0]['id'].rsplit('_',1)[0]+'_3cols.jpg'),f'{source} 3 cols')
(out/'analysis.txt').write_text('\n'.join(lines),encoding='utf-8')
print('\n'.join(lines))
