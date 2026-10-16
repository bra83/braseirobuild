#!/usr/bin/env python3
"""Limited source-anchored audit, NOT full editorial certification."""
import hashlib,json,re,subprocess
from pathlib import Path
root=Path(__file__).resolve().parent.parent
base=Path('/mnt/data')
books={
 'under-black-sun':('Under a Black Sun.pdf',['EPISODE I: NO EASY JOBS','EPISODE II: CITY OF SPIRES AND STRIFE','EPISODE III: ENDGAME'],17),
 'long-arm-of-the-hutt':('Long Arm of the Hutt.pdf',['ACT 1','ACT 2','ACT 3'],7),
 'mask-of-the-pirate-queen':('mask of the pirate queen.pdf',['Episode I: Search on Saleucami','Episode II: Going to Ground','Episode III: Prepare for Boarders'],23)
}
items=[]
for id,(filename,headings,nodes) in books.items():
 p=base/filename
 assert p.is_file(),filename
 txt=subprocess.check_output(['pdftotext','-layout',str(p),'-'],text=True)
 info=subprocess.check_output(['pdfinfo',str(p)],text=True)
 pages=int(re.search(r'^Pages:\s*(\d+)',info,re.M).group(1))
 matches=[{'heading':h,'present':bool(re.search(re.escape(h),txt,re.I))} for h in headings]
 items.append({'adventureId':id,'file':filename,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),
               'pdfPages':pages,'sourceHeadings':matches,'sourceHeadingsFound':all(x['present'] for x in matches),
               'codeNodes':nodes,'editoriallyCertified':False})
assert all(x['sourceHeadingsFound'] for x in items)
source={'checkpoint':'P181','editoriallyCertified':False,'kind':'partial PDF chapter/setting check',
 'scope':'SWE13 Episode I-III and opening settings; other two PDFs newly recovered and chapter headings checked',
 'notes':['Mask source first episode Saleucami and Episode II Worlport; P180 incorrectly reversed the order.',
          'Mask P181 expands 5 to 23 nodes, seeds named NPCs/locations, and fixes two mutually exclusive reward outcomes.',
          'The five books have NOT been checked scene-by-scene or for stat-block/rules equivalence.',
          'Beyond the Rim and Jewel of Yavin PDFs were audited for TOC in P180, not re-read in P181.'],
 'sources':items}
out=root/'evidence/P181/PDF_SOURCE_CROSSCHECK.json';out.parent.mkdir(parents=True,exist_ok=True)
out.write_text(json.dumps(source,indent=2,ensure_ascii=False))
print(json.dumps({'sources':[(x['adventureId'],x['pdfPages']) for x in items],'allHeadingsFound':True,'editoriallyCertified':False}))
