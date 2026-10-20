#!/usr/bin/env python3
"""Generate Android-bundled 352-token library. Fail on missing, invalid or fake PNGs."""
import sys,zipfile,io,json,re,hashlib,unicodedata
from pathlib import Path
from PIL import Image
if len(sys.argv)!=3:raise SystemExit('usage: embed-tokens.py PACK.zip OUA_ROOT')
zip_path,root=Path(sys.argv[1]),Path(sys.argv[2]);base=root/'public/tor2e-ui'
if not zip_path.is_file():raise SystemExit('SOURCE_BLOCKED: 352-token ZIP absent')
original=json.loads((base/'party-token-manifest.json').read_text())
normalize=lambda s:''.join(c for c in unicodedata.normalize('NFD',s.lower()) if unicodedata.category(c)!='Mn')
races={
'human':r'human|humano|homem|mulher|bree|gondor|dunedain|rohan',
'elf':r'elf|elfo|elfa|lindon|lorien|mirkwood',
'dwarf':r'dwarf|anao|erebor|khazad',
'hobbit':r'hobbit|halfling|shire|condado'}
def race_for(name):
 s=normalize(name).replace('_',' ').replace('-',' ')
 a=[k for k,p in races.items() if re.search(r'(^|[^a-z])('+p+r')([^a-z]|$)',s)]
 return a[0] if len(a)==1 else 'unclassified'
culture={'human':'bree','elf':'woodland','dwarf':'erebor','hobbit':'shire','unclassified':'unclassified'}
scale={'human':1,'elf':1.03,'dwarf':.89,'hobbit':.8,'unclassified':.96}
tokens=[];hashes=set()
with zipfile.ZipFile(zip_path) as z:
 imgs=sorted([x for x in z.infolist() if not x.is_dir() and x.filename.lower().endswith('.png') and not x.filename.startswith('__MACOSX/')],key=lambda x:x.filename.lower())
 if len(imgs)!=352:raise SystemExit(f'SOURCE_BLOCKED: expected 352 PNG, got {len(imgs)}')
 for i,x in enumerate(imgs,1):
  with Image.open(io.BytesIO(z.read(x))) as im:
   im.load();rgba=im.convert('RGBA')
   if min(im.size)<48 or max(im.size)>4096 or rgba.getchannel('A').getextrema()[0]>=250:
    raise SystemExit('Invalid or nontransparent token: '+x.filename)
   digest=hashlib.sha256(rgba.tobytes()).hexdigest()
   if digest in hashes:raise SystemExit('Duplicate token: '+x.filename)
   hashes.add(digest)
   race=race_for(x.filename);tid=f'oua352_{i:04d}'
   relative=f'party/characters/braseiro352/{race}/{tid}.png'
   dest=base/'assets'/relative;dest.parent.mkdir(parents=True,exist_ok=True);rgba.save(dest,'PNG',optimize=True)
   tokens.append(dict(id=tid,asset=relative,race=race,culture=culture[race],category=f'party:{race}:{culture[race]}',visualScale=scale[race],anchorX=.5,anchorY=.96,preferredLayer='front',sourceSheet=x.filename,sourceIndex=i))
if len(tokens)!=352 or set(t['id'] for t in tokens)&set(t['id'] for t in original['tokens']):raise SystemExit('Pack ID validation failed')
out=dict(schemaVersion=1,packId='braseiro-oua-352-v1',basePath='tor2e-ui/assets',tokenCount=352,tokens=tokens)
(base/'hero-token-pack-352.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n')
print('OUA_352_TOKENS_EMBEDDED_PASS originals=120 additional=352 total=472')
print('RACE_GROUPS='+json.dumps({r:sum(t['race']==r for t in tokens) for r in culture}))
