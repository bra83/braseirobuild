from __future__ import annotations
from pathlib import Path
from PIL import Image
import csv, hashlib, json, sys, collections

HERE=Path(__file__).resolve().parent
ROOT=HERE/'source'/'TALES_VTT_ASSET_PACK'
MANIFEST=ROOT/'manifesto_assets.csv'
ZIP=HERE/'BRASEIRO_OSE_ASSET_PACK.zip'
OUT=HERE/'BUILD_MANIFEST.json'
REPORT=HERE/'ASSET_PACK_INTEGRITY_REPORT.json'

def sha256(p:Path)->str:
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):
            h.update(b)
    return h.hexdigest()

rows=list(csv.DictReader(MANIFEST.open(encoding='utf-8-sig', newline='')))
issues=[]; entries=[]
seen_ids=set(); seen_paths=set()
for r in rows:
    rid=r['id']; rel=r['arquivo'].replace('\\','/')
    if rid in seen_ids: issues.append({'id':rid,'kind':'duplicate_id'})
    if rel in seen_paths: issues.append({'id':rid,'kind':'duplicate_path','path':rel})
    seen_ids.add(rid); seen_paths.add(rel)
    p=ROOT/rel
    if not p.exists():
        issues.append({'id':rid,'kind':'missing','path':rel}); continue
    try:
        im=Image.open(p); im.load()
    except Exception as e:
        issues.append({'id':rid,'kind':'decode','path':rel,'error':str(e)}); continue
    expected=(int(r['largura']),int(r['altura']))
    actual=(im.width,im.height)
    if expected!=actual:
        issues.append({'id':rid,'kind':'dimensions','path':rel,'expected':expected,'actual':actual})
    bands=im.getbands(); has_alpha='A' in bands; amin=amax=None
    if has_alpha: amin,amax=im.getchannel('A').getextrema()
    fundo=r['fundo'].strip().lower()
    if fundo=='transparente' and (not has_alpha or amin==255):
        issues.append({'id':rid,'kind':'alpha_expected_transparency','path':rel,'mode':im.mode,'alpha_min':amin,'alpha_max':amax})
    if fundo=='opaco' and has_alpha and amin < 255:
        issues.append({'id':rid,'kind':'opaque_expected','path':rel,'alpha_min':amin,'alpha_max':amax})
    entries.append({
        'id':rid,'name':r['nome'],'path':rel,'category':r['categoria'],'source':r['fonte'],
        'source_crop':[int(r['x']),int(r['y']),int(r['largura']),int(r['altura'])],
        'declared_background':r['fundo'],'actual_size':[im.width,im.height],
        'mode':im.mode,'alpha':{'present':has_alpha,'min':amin,'max':amax},'sha256':sha256(p)
    })

individual=list((ROOT/'assets_individuais').rglob('*.png'))
sheets=list((ROOT/'folhas_de_assets').rglob('*.png'))
all_png=list(ROOT.rglob('*.png'))
source_files=[p for p in ROOT.rglob('*') if p.is_file()]
source_hashes={str(p.relative_to(ROOT)).replace('\\','/'):sha256(p) for p in source_files}
summary={
    'schema':'BRASEIRO_OSE_USER_ASSET_BUILD_MANIFEST_V1',
    'source_pack_file':'BRASEIRO_OSE_ASSET_PACK.zip',
    'source_pack_sha256':sha256(ZIP),
    'manifest_csv_sha256':sha256(MANIFEST),
    'manifest_rows':len(rows),
    'expected_individual_pngs':676,
    'actual_individual_pngs':len(individual),
    'expected_asset_sheets':57,
    'actual_asset_sheets':len(sheets),
    'all_pngs':len(all_png),
    'source_file_count':len(source_files),
    'declared_backgrounds':dict(collections.Counter(r['fundo'] for r in rows)),
    'issue_count':len(issues),
    'issue_types':dict(collections.Counter(i['kind'] for i in issues)),
}
checks={
    'manifest_rows_676':len(rows)==676,
    'individual_pngs_676':len(individual)==676,
    'asset_sheets_57':len(sheets)==57,
    'all_manifest_paths_exist':all((ROOT/r['arquivo']).exists() for r in rows),
    'decode_dimensions_alpha':len(issues)==0,
}
summary['checks']=checks
summary['gate']='PASS' if all(checks.values()) else 'FAIL'
OUT.write_text(json.dumps({'summary':summary,'assets':entries,'source_hashes':source_hashes},ensure_ascii=False,indent=2),encoding='utf-8')
REPORT.write_text(json.dumps({'summary':summary,'issues':issues},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(summary,ensure_ascii=False,indent=2))
sys.exit(0 if summary['gate']=='PASS' else 1)
