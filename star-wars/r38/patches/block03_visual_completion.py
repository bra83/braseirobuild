#!/usr/bin/env python3
from __future__ import annotations
import hashlib, json, re, shutil, sys
from pathlib import Path

# CI retrigger after validated runtime-emission fix; no gameplay or visual design changes.
if len(sys.argv) != 3:
    raise SystemExit('usage: block03_visual_completion.py <android-project-root> <visual-pack-dir>')

ROOT=Path(sys.argv[1]).resolve(); PACK=Path(sys.argv[2]).resolve()
VTT=ROOT/'app/src/main/assets/vtt'
DEST=VTT/'image-library/local/final'
BINDINGS=PACK/'bindings.json'
if not BINDINGS.is_file(): raise SystemExit(f'missing bindings: {BINDINGS}')
data=json.loads(BINDINGS.read_text(encoding='utf-8'))

expected={'scenes':26,'vehicles':9,'creatures':3,'npcs':1,'equipment':2}
for k,n in expected.items():
    if len(data.get(k,[]))!=n: raise SystemExit(f'{k}: expected {n}, got {len(data.get(k,[]))}')

def verify(rel, sha):
    p=PACK/rel
    if not p.is_file(): raise SystemExit(f'missing pack asset: {rel}')
    got=hashlib.sha256(p.read_bytes()).hexdigest()
    if sha and got!=sha: raise SystemExit(f'SHA mismatch {rel}: {got}')
    return p
for x in data['scenes']: verify(x['file'],x['fileSha256'])
for x in data['vehicles']: verify(x['file'],x['fileSha256'])
for x in data['creatures']: verify(x['file'],x['fileSha256'])
for x in data['npcs']:
    verify(x['file'],x['fileSha256']); verify(x['dossier'],x['dossierSha256'])
for x in data['equipment']: verify(x['file'],x['fileSha256'])

if DEST.exists(): shutil.rmtree(DEST)
for sub in ['scenes','vehicles','creatures','npcs','equipment']:
    src=PACK/sub; dst=DEST/sub
    dst.mkdir(parents=True,exist_ok=True)
    for p in src.iterdir():
        if p.is_file(): shutil.copy2(p,dst/p.name)

final_bindings={
  'schemaVersion':1,
  'policy':{'matching':'explicit-name-or-key-only','semanticSceneFallback':True,'neverInferMechanicsFromImage':True},
  **data,
}
(DEST/'final-visual-bindings.json').write_text(json.dumps(final_bindings,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

manifest_path=VTT/'image-library/manifest.json'
if not manifest_path.is_file(): raise SystemExit(f'missing manifest {manifest_path}')
manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
assets=[a for a in manifest.get('assets',[]) if not str(a.get('assetId','')).startswith('local.final.')]

baseline_assets=json.loads(json.dumps(assets))

def local_path(rel): return 'image-library/local/final/'+rel.replace('\\','/')
def base_asset(assetId,title,category,subtype,rel,sha,w,h,tags,usage,sourceRef):
    return {'assetId':assetId,'title':title,'category':category,'subtype':subtype,'sourceType':'user_generated','sourceRef':sourceRef,
            'tags':['star-wars','edge-of-the-galaxy',*tags],'usage':usage,'mechanicalBinding':None,'localPath':local_path(rel),
            'sha256':sha,'width':int(w),'height':int(h)}

scene_titles={
 'customs_checkpoint':'Posto alfandegário','medbay':'Centro médico / Medbay','cargo_warehouse':'Entreposto de carga / Armazém',
 'outer_rim_settlement':'Assentamento periférico da Orla Exterior','relay_station':'Estação de retransmissão',
 'mining_complex':'Complexo de mineração','remote_hideout':'Refúgio remoto / Esconderijo','repair_dock':'Doca de reparos / Hangar-oficina',
 'industrial_district':'Distrito industrial exterior','wreck_field':'Campo de destroços'}
for x in data['scenes']:
    arch=x['archetype']; v=int(x['variant']); title=f"{scene_titles[arch]} — {v:02d}"
    assets.append(base_asset(f'local.final.scene.{arch}.{v:02d}',title,'scene',arch,x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],
                             ['scene',arch,'primary' if x.get('primary') else 'variant'],'runtime_scene_fallback',x.get('source_name','Armazenamento interno.zip')))
for x in data['vehicles']:
    assets.append(base_asset(f"local.final.vehicle.{x['key']}",x['title'],'vehicle','explicit',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],
                             ['vehicle',x['key'],x['title'].lower().replace(' ','-')],'runtime_bound_visual','user vehicle sprite sheet'))
for x in data['creatures']:
    assets.append(base_asset(f"local.final.creature.{x['key']}",x['title'],'creature','beast',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],
                             ['creature','beast',x['key']],'runtime_bound_visual','Trio de Criaturas Alienígenas em Preto.png'))
for x in data['npcs']:
    assets.append(base_asset(f"local.final.npc.{x['key']}.portrait",x['title'],'portrait','npc',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],
                             ['npc','portrait',x['key']],'runtime_bound_visual','Dossiê Imperial do Capitão Son Barab.png'))
    assets.append(base_asset(f"local.final.npc.{x['key']}.dossier",x['title']+' — Dossiê','portrait','npc-dossier',x['dossier'],x['dossierSha256'],x['dossierWidth'],x['dossierHeight'],
                             ['npc','dossier',x['key']],'reference_dossier','Dossiê Imperial do Capitão Son Barab.png'))
for x in data['equipment']:
    assets.append(base_asset(f"local.final.equipment.{x['key']}",x['title'],'equipment','gear',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],
                             ['equipment','gear',x['key'],*x.get('aliases',[])],'runtime_bound_visual','Dossiê Imperial de Equipamentos Especiais.png'))

manifest['assets']=assets
policy=manifest.setdefault('policy',{})
policy['finalUserVisualBindings']='explicit_validated_runtime_map'
policy['sceneFallbackLibrary']='local_semantic_archetype_variants'

def recompute_counts(old):
    c=dict(old or {})
    c.update({
      'total':len(assets),'local':sum(a.get('sourceType')!='external_reference' for a in assets),
      'external':sum(a.get('sourceType')=='external_reference' for a in assets),
      'remoteReady':sum(bool(a.get('remoteReady')) for a in assets),
      'tokens':sum(a.get('category')=='token' for a in assets),'equipment':sum(a.get('category')=='equipment' for a in assets),
      'maps':sum(a.get('category')=='map' for a in assets),'portraits':sum(a.get('category')=='portrait' for a in assets),
      'droids':sum(a.get('category')=='droid' for a in assets),'creatures':sum(a.get('category')=='creature' for a in assets),
      'vehicles':sum(a.get('category')=='vehicle' for a in assets),'scenes':sum(a.get('category')=='scene' for a in assets),
    })
    return c
manifest['counts']=recompute_counts(manifest.get('counts'))
manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

manifest_compact=json.dumps(manifest,ensure_ascii=False,separators=(',',':'))
def replace_frozen_manifest(path:Path):
    s=path.read_text(encoding='utf-8')
    marker='IMAGE_LIBRARY_MANIFEST'
    pos=s.find(marker)
    if pos<0: raise SystemExit(f'{path}: manifest marker missing')
    freeze=s.find('Object.freeze(',pos)
    if freeze<0: raise SystemExit(f'{path}: Object.freeze missing')
    start=s.find('{',freeze)
    if start<0: raise SystemExit(f'{path}: object start missing')
    depth=0; in_str=False; esc=False; quote=''; end=None
    for i in range(start,len(s)):
        ch=s[i]
        if in_str:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch==quote: in_str=False
            continue
        if ch in ('"',"'"):
            in_str=True; quote=ch; continue
        if ch=='{': depth+=1
        elif ch=='}':
            depth-=1
            if depth==0: end=i+1; break
    if end is None: raise SystemExit(f'{path}: unbalanced manifest')
    path.write_text(s[:start]+manifest_compact+s[end:],encoding='utf-8')

source_data=VTT/'js/library/imageLibraryData.mjs'
bundle=VTT/'js/core/uiBridge.bundle.js'
for p in [source_data,bundle]:
    if not p.is_file(): raise SystemExit(f'missing library code: {p}')
    replace_frozen_manifest(p)

runtime_path=VTT/'r46/final-runtime.js'
css_path=VTT/'r46/final-runtime.css'
runtime=runtime_path.read_text(encoding='utf-8')
scene_defs={}
for x in data['scenes']:
    scene_defs.setdefault(x['archetype'],[]).append(local_path(x['file']))
keywords={
 'customs_checkpoint':['alfand','aduana','customs','inspeção','inspecao','checkpoint','contrabando','controle imperial'],
 'medbay':['medbay','centro médico','centro medico','médico','medico','hospital','bacta','enfermaria','ferido'],
 'cargo_warehouse':['armaz','entreposto','carga','warehouse','depósito','deposito','contêiner','conteiner','mercadoria'],
 'relay_station':['retransmiss','relay','antena','comunicaç','comunicac','transmissor','sinal'],
 'mining_complex':['minera','mining','mina','minério','minerio','escava','perfur'],
 'remote_hideout':['esconderijo','refúgio','refugio','hideout','fugitiv','clandestin','abrigo remoto'],
 'repair_dock':['doca de repar','hangar-oficina','hangar oficina','oficina','manutenção','manutencao','reparo'],
 'industrial_district':['distrito industrial','zona industrial','fábrica','fabrica','tubulaç','tubulac','fuligem'],
 'wreck_field':['destroço','destroco','wreck','sucata de nave','nave destruída','nave destruida'],
 'outer_rim_settlement':['assentamento','orla exterior','outer rim','colônia','colonia','povoado','fronteira','mercado','rua']}
js_scene=json.dumps(scene_defs,ensure_ascii=False,separators=(',',':'))
js_kw=json.dumps(keywords,ensure_ascii=False,separators=(',',':'))
new_chooser=f'''const FINAL_SCENE_VISUALS=Object.freeze({js_scene});\nconst FINAL_SCENE_KEYWORDS=Object.freeze({js_kw});\nfunction finalSceneHash(text){{let h=2166136261>>>0;for(let i=0;i<text.length;i++){{h^=text.charCodeAt(i);h=Math.imul(h,16777619)}}return h>>>0}}\nfunction recoveredSceneVisual(view){{\n const scene=currentSceneRecord()||{{}}, location=view?.scene?.location||{{}};\n const context=[scene.id,scene.title,scene.narration,view?.situation,location.name,location.description,...(Array.isArray(view?.participants)?view.participants.flatMap(p=>[p?.name,p?.meta,p?.status]):[])].filter(Boolean).join(' ').toLocaleLowerCase('pt-BR');\n let best='outer_rim_settlement',score=0;\n for(const [arch,terms] of Object.entries(FINAL_SCENE_KEYWORDS)){{let s=0;for(const term of terms)if(context.includes(String(term).toLocaleLowerCase('pt-BR')))s+=term.length+(term.includes(' ')?8:3);if(s>score){{score=s;best=arch}}}}\n const variants=FINAL_SCENE_VISUALS[best]||FINAL_SCENE_VISUALS.outer_rim_settlement;\n const seed=[scene.id,scene.title,location.name,view?.situation,scene.narration].filter(Boolean).join('|');\n const src=variants[finalSceneHash(seed||best)%variants.length];\n return {{id:'final-'+best,src,archetype:best}};\n}}\nfunction syncRecoveredSceneVisual'''
pat=r'function recoveredSceneVisual\(view\)\{.*?\n\}\nfunction syncRecoveredSceneVisual'
repl_count=len(re.findall(pat,runtime,flags=re.S))
if repl_count!=1: raise SystemExit(f'expected one recoveredSceneVisual function, found {repl_count}')
runtime=re.sub(pat,new_chooser,runtime,count=1,flags=re.S)

visuals=[]
for x in data['vehicles']:
    aliases=[x['title'],x['key'].replace('_',' ')]
    if x['key']=='jumpmaster_jm5000': aliases += ['JM-5000 JumpMaster','JumpMaster']
    if x['key']=='skipray_gat12h': aliases += ['GAT-12h Skipray','Skipray']
    if x['key']=='alpha_xg1_star_wing': aliases += ['XG-1 Star Wing','Alpha-class XG-1']
    if x['key']=='vt49_decimator': aliases += ['VT-49 Decimator']
    if x['key']=='wud500': aliases += ['WUD-500','CEC WUD-500']
    visuals.append({'id':'vehicle.'+x['key'],'kind':'vehicle','title':x['title'],'aliases':aliases,'src':local_path(x['file'])})
for x in data['creatures']:
    visuals.append({'id':'creature.'+x['key'],'kind':'creature','title':x['title'],'aliases':[x['title']],'src':local_path(x['file'])})
for x in data['npcs']:
    visuals.append({'id':'npc.'+x['key'],'kind':'npc','title':x['title'],'aliases':['Son Barab','Capitão Son Barab','Captain Son Barab'],'src':local_path(x['file'])})
for x in data['equipment']:
    visuals.append({'id':'equipment.'+x['key'],'kind':'equipment','title':x['title'],'aliases':[x['title'],*x.get('aliases',[])],'src':local_path(x['file'])})
js_visuals=json.dumps(visuals,ensure_ascii=False,separators=(',',':'))
completion=r'''
/* R38 VISUAL COMPLETION — explicit user-approved assets; no R46 redesign. */
const FINAL_EXPLICIT_VISUALS=Object.freeze(__VISUALS__);
function finalNormVisualText(v){return String(v||'').normalize('NFD').replace(/\p{M}/gu,'').toLocaleLowerCase('pt-BR')}
function finalSmallestHostForAlias(alias){
 const n=finalNormVisualText(alias), nodes=[...document.querySelectorAll('main section,main article,main [class*=card],main [class*=panel],section,article')];
 const hits=nodes.filter(el=>{const t=finalNormVisualText(el.textContent);return t.includes(n)&&t.length<7000});
 hits.sort((a,b)=>(a.textContent||'').length-(b.textContent||'').length);return hits[0]||null;
}
function finalMountExplicitVisual(v){
 const host=v.aliases.map(finalSmallestHostForAlias).find(Boolean);if(!host||host.querySelector(`[data-final-visual="${v.id}"]`))return;
 const wrap=document.createElement('figure');wrap.className='final-explicit-visual final-explicit-'+v.kind;wrap.dataset.finalVisual=v.id;
 const img=document.createElement('img');img.src=v.src;img.alt=v.title;img.decoding='async';wrap.append(img);
 const cap=document.createElement('figcaption');cap.textContent=v.title;wrap.append(cap);
 const target=host.querySelector(':scope > div:last-child')||host;target.prepend(wrap);
}
function enhanceFinalExplicitVisuals(){for(const v of FINAL_EXPLICIT_VISUALS)finalMountExplicitVisual(v)}
(function installFinalVisualCompletion(){
 let queued=false;const schedule=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;enhanceFinalExplicitVisuals()})};
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
 new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true,characterData:true});
 document.addEventListener('click',()=>setTimeout(schedule,0),true);
 window.BraseiroFinalVisualCompletion=Object.freeze({scenes:26,vehicles:9,creatures:3,npcs:1,equipment:2,policy:'explicit-validated'});
})();
'''.replace('__VISUALS__',js_visuals)
if 'R38 VISUAL COMPLETION' in runtime:
    runtime=runtime.split('/* R38 VISUAL COMPLETION',1)[0].rstrip()+'\n'
runtime += completion
runtime_path.write_text(runtime,encoding='utf-8')

css=css_path.read_text(encoding='utf-8')
style='''\n/* R38 VISUAL COMPLETION — assets inside existing cards only. */\n.final-explicit-visual{margin:0 0 .6rem;max-width:100%;border:1px solid rgba(194,157,89,.35);background:#05070a;overflow:hidden}\n.final-explicit-visual img{display:block;width:100%;max-height:190px;object-fit:contain;background:#020304}\n.final-explicit-visual figcaption{padding:.28rem .45rem;font-size:.68rem;letter-spacing:.04em;color:rgba(236,225,199,.78);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}\n.final-explicit-npc img{max-height:170px;object-position:center top}.final-explicit-equipment img,.final-explicit-creature img{max-height:150px}\n'''
if 'R38 VISUAL COMPLETION — assets inside existing cards only.' not in css: css += style
css_path.write_text(css,encoding='utf-8')

final_assets=[a for a in manifest['assets'] if str(a.get('assetId','')).startswith('local.final.')]
if len(final_assets)!=42: raise SystemExit(f'expected 42 final manifest assets, got {len(final_assets)}')
for a in final_assets:
    p=VTT/a['localPath']
    if not p.is_file(): raise SystemExit(f'manifest localPath missing: {p}')
    if hashlib.sha256(p.read_bytes()).hexdigest()!=a['sha256']: raise SystemExit(f'manifest SHA mismatch: {p}')
if manifest['counts'].get('scenes',0)<26: raise SystemExit('scene count was not updated')
for token in ['FINAL_SCENE_VISUALS','FINAL_EXPLICIT_VISUALS','Son Barab','Digger Crawler','Gigantic Dragonsnake','Traje espacial','Power Pack']:
    if token not in runtime: raise SystemExit('runtime token missing: '+token)
print('BLOCK03_VISUAL_COMPLETION_OK')
print(json.dumps({'newAssets':42,'scenes':26,'vehicles':9,'creatures':3,'npcAssets':2,'equipment':2,'manifestTotal':manifest['counts']['total']},ensure_ascii=False))


# Update current-catalog expectations after the approved additive visual pack.
# Historical source-wave counts and the R46 provenance freeze stay unchanged.
changes = {
 'manifest.counts.local,148': 'manifest.counts.local,190',
 'manifest.counts.equipment,4': 'manifest.counts.equipment,6',
 'manifest.counts.total,369': 'manifest.counts.total,411',
 'ids.length,369': 'ids.length,411',
 'new Set(ids).size,369': 'new Set(ids).size,411',
 'manifest.counts.vehicles,127': 'manifest.counts.vehicles,136',
 'manifest.counts.creatures,17': 'manifest.counts.creatures,20',
 'all.data.length,204': 'all.data.length,217',
 'vehicles.data.length,127': 'vehicles.data.length,136',
 'creatures.data.length,17': 'creatures.data.length,20',
 'equipment.length,4': 'equipment.length,6',
 "a.usage==='visual_only' && a.mechanicalBinding===null": "['visual_only','runtime_bound_visual'].includes(a.usage) && a.mechanicalBinding===null",
}
for testfile in sorted((ROOT/'tests').glob('image-library-r*.test.mjs')):
    original=testfile.read_text(encoding='utf-8')
    updated=original
    for before,after in changes.items(): updated=updated.replace(before,after)
    if updated!=original: testfile.write_text(updated,encoding='utf-8')
# The additive pack must preserve every original manifest record exactly.
assert [a for a in manifest['assets'] if not a['assetId'].startswith('local.final.')]==baseline_assets
final_ids={a['assetId'] for a in final_assets}
assert len(final_ids)==42
assert len(assets)==411
assert manifest['counts']['local']==190
assert manifest['counts']['external']==221
assert manifest['counts']['vehicles']==136
assert manifest['counts']['creatures']==20
assert manifest['counts']['equipment']==6
print('VISUAL_CATALOG_EXPECTATIONS_UPDATED_WITH_EXACT_COUNTS')
