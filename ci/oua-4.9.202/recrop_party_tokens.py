import argparse, json, math, os, re
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np
import cv2

def rgba_and_mask(path):
    im=Image.open(path).convert('RGBA')
    a=np.asarray(im)
    alpha=a[:,:,3]
    transparent=float((alpha<245).mean())
    if transparent>0.01:
        mask=(alpha>10).astype(np.uint8)*255
        original_alpha=alpha
    else:
        rgb=a[:,:,:3]
        mx=np.max(rgb,axis=2)
        # Most sheets were prepared on almost-black backgrounds.
        mask=(mx>13).astype(np.uint8)*255
        original_alpha=mask
    return im, mask, original_alpha

def clean_mask(mask):
    h,w=mask.shape
    k=max(3,int(round(min(h,w)*0.006)))
    if k%2==0:k+=1
    k=min(k,9)
    kernel=cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(k,k))
    m=cv2.morphologyEx(mask,cv2.MORPH_CLOSE,kernel,iterations=1)
    # Very small dilation reconnects antialiased gaps without merging neighboring figures.
    d=max(3,min(7,k))
    dk=cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(d,d))
    return cv2.dilate(m,dk,iterations=1)

def choose_component(mask):
    h,w=mask.shape
    m=clean_mask(mask)
    n,labels,stats,cent=cv2.connectedComponentsWithStats(m,8)
    items=[]
    min_area=max(80,int(h*w*0.0012))
    for i in range(1,n):
        x,y,bw,bh,area=map(int,stats[i])
        if area<min_area: continue
        cx,cy=cent[i]
        dx=(cx-w/2)/(w/2); dy=(cy-h/2)/(h/2)
        dist=math.sqrt(dx*dx+dy*dy)
        central=max(0.15,1.0-min(1.0,dist)*0.65)
        touchL=x<=2; touchR=x+bw>=w-2; touchT=y<=2; touchB=y+bh>=h-2
        touches=sum((touchL,touchR,touchT,touchB))
        # A complete token is generally one large, central vertical figure.
        tall_bonus=1.12 if bh>=bw*0.9 else 1.0
        edge_factor=1.0
        if touches>=2: edge_factor=0.48
        elif touchT or touchL or touchR: edge_factor=0.72
        elif touchB: edge_factor=0.92
        score=area*central*tall_bonus*edge_factor
        items.append(dict(label=i,x=x,y=y,w=bw,h=bh,area=area,cx=float(cx),cy=float(cy),
                          touches=touches,touchL=touchL,touchR=touchR,touchT=touchT,touchB=touchB,score=score))
    if not items: return None,labels,m,[]
    items.sort(key=lambda q:q['score'],reverse=True)
    return items[0],labels,m,items

def recrop_one(path,out_path=None):
    im,mask,orig_alpha=rgba_and_mask(path)
    chosen,labels,dilated,items=choose_component(mask)
    if not chosen:
        return dict(ok=False,reason='no-component',path=str(path)),None
    h,w=mask.shape
    lab=chosen['label']
    keep=(labels==lab).astype(np.uint8)*255
    # Expand selection enough to restore antialiasing and small details.
    ek=cv2.getStructuringElement(cv2.MORPH_ELLIPSE,(7,7))
    keep=cv2.dilate(keep,ek,iterations=1)
    ys,xs=np.where(keep>0)
    if len(xs)==0:
        return dict(ok=False,reason='empty-component',path=str(path)),None
    x0,x1=int(xs.min()),int(xs.max()+1); y0,y1=int(ys.min()),int(ys.max()+1)
    pad=max(10,int(round(max(x1-x0,y1-y0)*0.045)))
    x0=max(0,x0-pad); y0=max(0,y0-pad); x1=min(w,x1+pad); y1=min(h,y1+pad)
    arr=np.array(im)
    # Build transparency from original foreground and selected component.
    if float((arr[:,:,3]<245).mean())>0.01:
        final_alpha=np.where(keep>0,arr[:,:,3],0).astype(np.uint8)
    else:
        # Soft black removal for opaque WebP.
        rgb=arr[:,:,:3].astype(np.float32)
        mx=np.max(rgb,axis=2)
        soft=np.clip((mx-8)*18,0,255).astype(np.uint8)
        final_alpha=np.where(keep>0,soft,0).astype(np.uint8)
    arr[:,:,3]=final_alpha
    cropped=Image.fromarray(arr[y0:y1,x0:x1,:],'RGBA')
    bbox=cropped.getchannel('A').getbbox()
    if bbox:
        cropped=cropped.crop(bbox)
        # add transparent breathing room so the figure never touches the picker frame
        p=max(8,int(max(cropped.size)*0.04))
        canvas=Image.new('RGBA',(cropped.width+2*p,cropped.height+2*p),(0,0,0,0))
        canvas.alpha_composite(cropped,(p,p)); cropped=canvas
    selected_area=int((keep>0).sum())
    total_area=int((mask>0).sum())
    dominant=selected_area/max(1,total_area)
    # hard-edge cuts at top/left/right are suspicious; bottom contact is common for standing figures.
    suspicious=bool(chosen['touches']>=2 or chosen['touchT'] or (chosen['touchL'] and chosen['touchR']))
    # Keep most recoverable figures; only suppress obvious shard-like selections.
    aspect=chosen['h']/max(1,chosen['w'])
    recoverable=(chosen['area']>=h*w*0.035 and chosen['h']>=h*0.28 and not (chosen['touches']>=3))
    info=dict(ok=recoverable,path=str(path),size=[w,h],components=len(items),
              chosen={k:v for k,v in chosen.items() if k!='label'},dominant=round(dominant,4),
              suspicious=suspicious,outputSize=list(cropped.size),aspect=round(aspect,3))
    if out_path and recoverable:
        out_path.parent.mkdir(parents=True,exist_ok=True)
        cropped.save(out_path,'WEBP',lossless=True,quality=95,method=6)
    return info,cropped

def card(im,label,w=300,h=380):
    bg=Image.new('RGB',(w,h),(4,4,4)); d=ImageDraw.Draw(bg)
    if im:
        src=im.copy(); src.thumbnail((w-18,h-42),Image.Resampling.LANCZOS)
        x=(w-src.width)//2; y=h-10-src.height
        if src.mode=='RGBA': bg.paste(src,(x,y),src)
        else:bg.paste(src,(x,y))
    d.rectangle((0,0,w-1,h-1),outline=(190,190,190),width=2)
    d.rectangle((0,0,w,28),fill=(0,0,0)); d.text((6,7),label,fill=(255,255,255))
    return bg

def contact(entries,base,out_path,after=False):
    cards=[]
    for e in sorted(entries,key=lambda x:x.get('sourceIndex',0)):
        p=base/e['asset']
        if after:
            info,im=recrop_one(p,None)
            label=e['id']+(' OK' if info['ok'] else ' DROP')
        else:
            im=Image.open(p).convert('RGBA'); label=e['id']
        cards.append(card(im,label))
    canvas=Image.new('RGB',(300*3,380*2),(0,0,0))
    for i,c in enumerate(cards[:6]): canvas.paste(c,((i%3)*300,(i//3)*380))
    canvas.save(out_path,quality=90)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('root'); ap.add_argument('out')
    ap.add_argument('--apply',action='store_true')
    args=ap.parse_args()
    root=Path(args.root); out=Path(args.out); out.mkdir(parents=True,exist_ok=True)
    mp=root/'public/tor2e-ui/party-token-manifest.json'
    m=json.loads(mp.read_text(encoding='utf-8'))
    base=root/'public/tor2e-ui/assets'
    groups={}
    for e in m['tokens']: groups.setdefault(e.get('sourceSheet') or e['id'],[]).append(e)
    report=[]; kept=[]
    preview_names={
      'seis_hobbits_aventureiros_em_ação.png','folha_de_heróis_anões_fantásticos.png',
      'folha_de_personagens_viajantes_fantásticos.png','guerreiros_de_rohan_em_folha_de_arte.png',
      'seis_heróis_de_gondor_em_aquarela.png','folha_de_rangers_dúnedain_em_ação.png'
    }
    for source,entries in groups.items():
        group_rows=[]
        if source in preview_names:
            safe=re.sub(r'[^A-Za-z0-9_-]+','_',entries[0]['id'].rsplit('_',1)[0])
            contact(entries,base,out/(safe+'_before.jpg'),False)
            contact(entries,base,out/(safe+'_after.jpg'),True)
        for e in sorted(entries,key=lambda x:x.get('sourceIndex',0)):
            p=base/e['asset']
            target=p if args.apply else None
            info,cropped=recrop_one(p,target)
            info.update(id=e['id'],asset=e['asset'],sourceSheet=source)
            report.append(info)
            if info['ok']: kept.append(e)
    (out/'recrop-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    summary={
      'before':len(m['tokens']),'recoverable':len(kept),'dropped':len(m['tokens'])-len(kept),
      'bySource':{}
    }
    for source,entries in groups.items():
      ids={e['id'] for e in entries}; rows=[r for r in report if r['id'] in ids]
      summary['bySource'][source]={'count':len(rows),'recoverable':sum(1 for r in rows if r['ok']),'suspicious':sum(1 for r in rows if r.get('suspicious'))}
    if args.apply:
        m['tokens']=kept
        m['tokenCount']=len(kept)
        m['packId']='tor2e-party-characters-v2-clean-crops'
        m['cropRevision']='4.9.202-clean-components'
        mp.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    (out/'summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(summary,ensure_ascii=False,indent=2))

if __name__=='__main__': main()
