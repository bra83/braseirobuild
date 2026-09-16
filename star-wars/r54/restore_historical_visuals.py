#!/usr/bin/env python3
from __future__ import annotations
import hashlib,json,re,shutil,sys
from pathlib import Path

if len(sys.argv)!=3:
    raise SystemExit('usage: restore_historical_visuals.py <android-project-root> <approved-visual-pack-dir>')
ROOT=Path(sys.argv[1]).resolve();PACK=Path(sys.argv[2]).resolve();VTT=ROOT/'app/src/main/assets/vtt'
DEST=VTT/'image-library/local/final';BINDINGS=PACK/'bindings.json'
if not BINDINGS.is_file():raise SystemExit(f'missing bindings: {BINDINGS}')
data=json.loads(BINDINGS.read_text('utf-8'))
expected={'scenes':26,'vehicles':9,'creatures':3,'npcs':1,'equipment':2}
for k,n in expected.items():
    if len(data.get(k,[]))!=n:raise SystemExit(f'{k}: expected {n}, got {len(data.get(k,[]))}')

def verify(rel,sha):
    p=PACK/rel
    if not p.is_file():raise SystemExit(f'missing pack asset: {rel}')
    got=hashlib.sha256(p.read_bytes()).hexdigest()
    if sha and got!=sha:raise SystemExit(f'SHA mismatch {rel}: {got}')
    return p
for x in data['scenes']:verify(x['file'],x['fileSha256'])
for x in data['vehicles']:verify(x['file'],x['fileSha256'])
for x in data['creatures']:verify(x['file'],x['fileSha256'])
for x in data['npcs']:
    verify(x['file'],x['fileSha256']);verify(x['dossier'],x['dossierSha256'])
for x in data['equipment']:verify(x['file'],x['fileSha256'])

if DEST.exists():shutil.rmtree(DEST)
for sub in ['scenes','vehicles','creatures','npcs','equipment']:
    src=PACK/sub;dst=DEST/sub;dst.mkdir(parents=True,exist_ok=True)
    for p in src.iterdir():
        if p.is_file():shutil.copy2(p,dst/p.name)
final_bindings={'schemaVersion':1,'policy':{'matching':'explicit-name-or-key-only','semanticSceneFallback':True,'neverInferMechanicsFromImage':True},**data}
(DEST/'final-visual-bindings.json').write_text(json.dumps(final_bindings,ensure_ascii=False,indent=2)+'\n','utf-8')

manifest_path=VTT/'image-library/manifest.json'
if not manifest_path.is_file():raise SystemExit(f'missing manifest: {manifest_path}')
manifest=json.loads(manifest_path.read_text('utf-8'))
assets=[a for a in manifest.get('assets',[]) if not str(a.get('assetId','')).startswith('local.final.')]
baseline_ids={str(a.get('assetId')) for a in assets}
def lp(rel):return 'image-library/local/final/'+str(rel).replace('\\','/')
def add(asset_id,title,category,subtype,rel,sha,w,h,tags,usage,source):
    assets.append({'assetId':asset_id,'title':title,'category':category,'subtype':subtype,'sourceType':'user_generated','sourceRef':source,'tags':['star-wars','edge-of-the-galaxy',*tags],'usage':usage,'mechanicalBinding':None,'localPath':lp(rel),'sha256':sha,'width':int(w),'height':int(h)})
scene_titles={'customs_checkpoint':'Posto alfandegário','medbay':'Centro médico / Medbay','cargo_warehouse':'Entreposto de carga / Armazém','outer_rim_settlement':'Assentamento periférico da Orla Exterior','relay_station':'Estação de retransmissão','mining_complex':'Complexo de mineração','remote_hideout':'Refúgio remoto / Esconderijo','repair_dock':'Doca de reparos / Hangar-oficina','industrial_district':'Distrito industrial exterior','wreck_field':'Campo de destroços'}
for x in data['scenes']:
    arch=x['archetype'];v=int(x['variant']);add(f'local.final.scene.{arch}.{v:02d}',f"{scene_titles[arch]} — {v:02d}",'scene',arch,x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],['scene',arch,'primary' if x.get('primary') else 'variant'],'runtime_scene_fallback',x.get('source_name','Armazenamento interno.zip'))
for x in data['vehicles']:add(f"local.final.vehicle.{x['key']}",x['title'],'vehicle','explicit',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],['vehicle',x['key']],'runtime_bound_visual','user vehicle sprite sheet')
for x in data['creatures']:add(f"local.final.creature.{x['key']}",x['title'],'creature','beast',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],['creature','beast',x['key']],'runtime_bound_visual','Trio de Criaturas Alienígenas em Preto.png')
for x in data['npcs']:
    add(f"local.final.npc.{x['key']}.portrait",x['title'],'portrait','npc',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],['npc','portrait',x['key']],'runtime_bound_visual','Dossiê Imperial do Capitão Son Barab.png')
    add(f"local.final.npc.{x['key']}.dossier",x['title']+' — Dossiê','portrait','npc-dossier',x['dossier'],x['dossierSha256'],x['dossierWidth'],x['dossierHeight'],['npc','dossier',x['key']],'reference_dossier','Dossiê Imperial do Capitão Son Barab.png')
for x in data['equipment']:add(f"local.final.equipment.{x['key']}",x['title'],'equipment','gear',x['file'],x['fileSha256'],x['fileWidth'],x['fileHeight'],['equipment','gear',x['key'],*x.get('aliases',[])],'runtime_bound_visual','Dossiê Imperial de Equipamentos Especiais.png')
manifest['assets']=assets
manifest.setdefault('policy',{})['finalUserVisualBindings']='explicit_validated_runtime_map_r54'
manifest['policy']['sceneFallbackLibrary']='local_semantic_archetype_variants_r54'
cats=['token','equipment','map','portrait','droid','creature','vehicle','scene']
old=dict(manifest.get('counts') or {})
old.update({'total':len(assets),'local':sum(a.get('sourceType')!='external_reference' for a in assets),'external':sum(a.get('sourceType')=='external_reference' for a in assets),'remoteReady':sum(bool(a.get('remoteReady')) for a in assets)})
for c in cats:old['maps' if c=='map' else c+'s' if c not in {'equipment'} else 'equipment']=sum(a.get('category')==c for a in assets)
manifest['counts']=old
manifest_path.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n','utf-8')

compact=json.dumps(manifest,ensure_ascii=False,separators=(',',':'))
def replace_frozen(path:Path):
    s=path.read_text('utf-8');pos=s.find('IMAGE_LIBRARY_MANIFEST')
    if pos<0:raise SystemExit(f'{path}: manifest marker missing')
    freeze=s.find('Object.freeze(',pos);start=s.find('{',freeze)
    if freeze<0 or start<0:raise SystemExit(f'{path}: frozen manifest missing')
    depth=0;ins=False;esc=False;quote='';end=None
    for i in range(start,len(s)):
        ch=s[i]
        if ins:
            if esc:esc=False
            elif ch=='\\':esc=True
            elif ch==quote:ins=False
            continue
        if ch in ('"',"'"):ins=True;quote=ch;continue
        if ch=='{':depth+=1
        elif ch=='}':
            depth-=1
            if depth==0:end=i+1;break
    if end is None:raise SystemExit(f'{path}: unbalanced manifest')
    path.write_text(s[:start]+compact+s[end:],'utf-8')
for p in [VTT/'js/library/imageLibraryData.mjs',VTT/'js/core/uiBridge.bundle.js']:
    if not p.is_file():raise SystemExit(f'missing library code: {p}')
    replace_frozen(p)

scene_defs={}
for x in data['scenes']:scene_defs.setdefault(x['archetype'],[]).append(lp(x['file']))
keywords={'customs_checkpoint':['alfand','aduana','customs','inspeção','inspecao','checkpoint','contrabando','controle imperial'],'medbay':['medbay','centro médico','centro medico','médico','medico','hospital','bacta','enfermaria','ferido'],'cargo_warehouse':['armaz','entreposto','carga','warehouse','depósito','deposito','contêiner','conteiner','mercadoria'],'relay_station':['retransmiss','relay','antena','comunicaç','comunicac','transmissor','sinal'],'mining_complex':['minera','mining','mina','minério','minerio','escava','perfur'],'remote_hideout':['esconderijo','refúgio','refugio','hideout','fugitiv','clandestin','abrigo remoto'],'repair_dock':['doca de repar','hangar-oficina','hangar oficina','oficina','manutenção','manutencao','reparo'],'industrial_district':['distrito industrial','zona industrial','fábrica','fabrica','tubulaç','tubulac','fuligem'],'wreck_field':['destroço','destroco','wreck','sucata de nave','nave destruída','nave destruida'],'outer_rim_settlement':['assentamento','orla exterior','outer rim','colônia','colonia','povoado','fronteira','mercado','rua']}
visuals=[]
for x in data['vehicles']:
    aliases=[x['title'],x['key'].replace('_',' ')]
    if x['key']=='jumpmaster_jm5000':aliases+=['JM-5000 JumpMaster','JumpMaster']
    if x['key']=='skipray_gat12h':aliases+=['GAT-12h Skipray','Skipray']
    if x['key']=='alpha_xg1_star_wing':aliases+=['XG-1 Star Wing','Alpha-class XG-1']
    if x['key']=='vt49_decimator':aliases+=['VT-49 Decimator']
    if x['key']=='wud500':aliases+=['WUD-500','CEC WUD-500']
    visuals.append({'id':'vehicle.'+x['key'],'kind':'vehicle','title':x['title'],'aliases':aliases,'src':lp(x['file'])})
for x in data['creatures']:visuals.append({'id':'creature.'+x['key'],'kind':'creature','title':x['title'],'aliases':[x['title']],'src':lp(x['file'])})
for x in data['npcs']:visuals.append({'id':'npc.'+x['key'],'kind':'npc','title':x['title'],'aliases':['Son Barab','Capitão Son Barab','Captain Son Barab'],'src':lp(x['file'])})
for x in data['equipment']:visuals.append({'id':'equipment.'+x['key'],'kind':'equipment','title':x['title'],'aliases':[x['title'],*x.get('aliases',[])],'src':lp(x['file'])})
block=r'''
/* R54 HISTORICAL VISUAL RESTORE — approved R38 assets, visual-only. */
const FINAL_SCENE_VISUALS=Object.freeze(__SCENES__);
const FINAL_SCENE_KEYWORDS=Object.freeze(__KEYWORDS__);
const FINAL_EXPLICIT_VISUALS=Object.freeze(__VISUALS__);
function r54HistNorm(v){return String(v||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLocaleLowerCase('pt-BR')}
function r54HistHash(text){let h=2166136261>>>0;for(let i=0;i<text.length;i++){h^=text.charCodeAt(i);h=Math.imul(h,16777619)}return h>>>0}
function r54HistoricalSceneVisual(){
 const media=document.querySelector('.scene-media-slot');if(!media)return;
 const live=media.querySelector('img.scene-media-image:not(.final-scene-library-image)');let fallback=media.querySelector('img.final-scene-library-image[data-r54-historical]');
 if(live){if(fallback)fallback.remove();return}
 const root=document.querySelector('main')||document.body,context=r54HistNorm(root?.innerText||'');let best='outer_rim_settlement',score=0;
 for(const [arch,terms] of Object.entries(FINAL_SCENE_KEYWORDS)){let s=0;for(const term of terms){const n=r54HistNorm(term);if(n&&context.includes(n))s+=n.length+(n.includes(' ')?8:3)}if(s>score){score=s;best=arch}}
 const variants=FINAL_SCENE_VISUALS[best]||FINAL_SCENE_VISUALS.outer_rim_settlement;if(!variants?.length)return;
 const seed=(document.querySelector('.scene-context-title')?.textContent||'')+'|'+(document.querySelector('.scene-situation-paper')?.textContent||'')+'|'+location.search;
 const src=variants[r54HistHash(seed||best)%variants.length];
 if(!fallback){fallback=document.createElement('img');fallback.className='scene-media-image final-scene-library-image';fallback.dataset.r54Historical='1';fallback.alt='Imagem de referência compatível com a cena atual';fallback.decoding='async';media.prepend(fallback)}
 if(fallback.getAttribute('src')!==src)fallback.setAttribute('src',src);media.classList.add('has-scene-image','has-recovered-scene');media.classList.remove('is-empty');
 const surface=media.querySelector('.scene-media-surface'),caption=media.querySelector('figcaption');if(surface)surface.hidden=true;if(caption)caption.hidden=true;
}
function r54HistSmallest(alias){const n=r54HistNorm(alias),nodes=[...document.querySelectorAll('main section,main article,main [class*=card],main [class*=panel],section,article')];const hits=nodes.filter(el=>{const t=r54HistNorm(el.textContent);return n&&t.includes(n)&&t.length<7000});hits.sort((a,b)=>(a.textContent||'').length-(b.textContent||'').length);return hits[0]||null}
function r54HistMount(v){const host=v.aliases.map(r54HistSmallest).find(Boolean);if(!host||host.querySelector(`[data-final-visual="${v.id}"]`))return;const wrap=document.createElement('figure');wrap.className='final-explicit-visual final-explicit-'+v.kind;wrap.dataset.finalVisual=v.id;const img=document.createElement('img');img.src=v.src;img.alt=v.title;img.decoding='async';wrap.append(img);const cap=document.createElement('figcaption');cap.textContent=v.title;wrap.append(cap);const target=host.querySelector(':scope > div:last-child')||host;target.prepend(wrap)}
function r54HistoricalVisualTick(){if((new URLSearchParams(location.search).get('screen')||'00')==='00')r54HistoricalSceneVisual();for(const v of FINAL_EXPLICIT_VISUALS)r54HistMount(v)}
(function(){let queued=false;const schedule=()=>{if(queued)return;queued=true;requestAnimationFrame(()=>{queued=false;r54HistoricalVisualTick()})};if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();new MutationObserver(schedule).observe(document.documentElement,{subtree:true,childList:true,characterData:true});document.addEventListener('click',()=>setTimeout(schedule,0),true);window.BraseiroFinalVisualCompletion=Object.freeze({scenes:26,vehicles:9,creatures:3,npcs:1,equipment:2,policy:'explicit-validated-r54'});})();
'''.replace('__SCENES__',json.dumps(scene_defs,ensure_ascii=False,separators=(',',':'))).replace('__KEYWORDS__',json.dumps(keywords,ensure_ascii=False,separators=(',',':'))).replace('__VISUALS__',json.dumps(visuals,ensure_ascii=False,separators=(',',':')))
runtime_path=VTT/'r46/final-runtime.js';runtime=runtime_path.read_text('utf-8')
if '/* R54 HISTORICAL VISUAL RESTORE' in runtime:runtime=runtime.split('/* R54 HISTORICAL VISUAL RESTORE',1)[0].rstrip()+'\n'
runtime += block
runtime_path.write_text(runtime,'utf-8')
css_path=VTT/'r46/final-runtime.css';css=css_path.read_text('utf-8')
style='''\n/* R54 HISTORICAL VISUAL RESTORE — existing-card visuals only. */\n.final-explicit-visual{margin:0 0 .6rem;max-width:100%;border:1px solid rgba(194,157,89,.35);background:#05070a;overflow:hidden}.final-explicit-visual img{display:block;width:100%;max-height:190px;object-fit:contain;background:#020304}.final-explicit-visual figcaption{padding:.28rem .45rem;font-size:.68rem;letter-spacing:.04em;color:rgba(236,225,199,.78);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.final-explicit-npc img{max-height:170px;object-position:center top}.final-explicit-equipment img,.final-explicit-creature img{max-height:150px}\n'''
if 'R54 HISTORICAL VISUAL RESTORE — existing-card visuals only.' not in css:css+=style
css_path.write_text(css,'utf-8')

final=[a for a in assets if str(a.get('assetId','')).startswith('local.final.')]
checks={'baseline preserved':baseline_ids.issubset({str(a.get('assetId')) for a in assets}),'final count':len(final)==42,'physical':all((VTT/a['localPath']).is_file() for a in final),'runtime scene catalog':'FINAL_SCENE_VISUALS' in runtime,'runtime explicit catalog':'FINAL_EXPLICIT_VISUALS' in runtime,'policy':all(a.get('mechanicalBinding') is None for a in final)}
if not all(checks.values()):raise SystemExit('R54 historical visual gates failed: '+repr(checks))
for a in final:
    if hashlib.sha256((VTT/a['localPath']).read_bytes()).hexdigest()!=a['sha256']:raise SystemExit('R54 historical visual SHA mismatch: '+a['localPath'])
print('R54_HISTORICAL_VISUAL_RESTORE_PASS',checks)
print(json.dumps({'assets':42,'scenes':26,'vehicles':9,'creatures':3,'npcAssets':2,'equipment':2,'manifestTotal':len(assets)},ensure_ascii=False))
