#!/usr/bin/env python3
from __future__ import annotations
import base64, json, sys, zlib
from pathlib import Path

MARKER='/* R54_SUPPLEMENT_SPECIES_REFERENCE */'
CATALOG_ZLIB_B64='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'

if len(sys.argv)!=2:
    raise SystemExit('usage: fix_supplement_species_runtime.py <android-project-root>')
ROOT=Path(sys.argv[1]).resolve()
VTT=ROOT/'app/src/main/assets/vtt'
RUNTIME=VTT/'r46/final-runtime.js'
if not RUNTIME.is_file():
    raise SystemExit(f'missing runtime: {RUNTIME}')

catalog=json.loads(zlib.decompress(base64.b64decode(CATALOG_ZLIB_B64)).decode('utf-8'))
if len(catalog)!=51:
    raise SystemExit(f'unexpected species reference count: {len(catalog)}')

def filtered_refs(row):
    return [r for r in row.get('references',[]) if r.get('line') in ('EOTE','AOR')]

def source_book(citation):
    return str(citation).split(' p.')[0].strip().rstrip(',')

eote=sum('EOTE' in x.get('activeLines',[]) for x in catalog)
aor=sum('AOR' in x.get('activeLines',[]) for x in catalog)
books=sorted({source_book(r.get('citation','')) for x in catalog for r in filtered_refs(x)})
if (eote,aor,len(books))!=(32,25,16):
    raise SystemExit(f'coverage mismatch: eote={eote} aor={aor} books={len(books)}')

def find(mode,query):
    q=query.casefold()
    return [x for x in catalog if (mode=='MIXED' or mode in x.get('activeLines',[])) and q in (' '.join([x.get('name','')]+[r.get('citation','') for r in filtered_refs(x)])).casefold()]
if len(find('AOR','Forged in Battle'))<3: raise SystemExit('AoR supplement coverage missing')
if len(find('EOTE','Lords of Nal Hutta'))<3: raise SystemExit('EotE supplement coverage missing')
if any(x.get('key')=='rodian' for x in find('AOR','Rodian')): raise SystemExit('cross-line guard failed')

outdir=VTT/'js/reference'
outdir.mkdir(parents=True,exist_ok=True)
(outdir/'speciesCreationReferenceCatalog.json').write_text(json.dumps(catalog,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
module="""import data from './speciesCreationReferenceCatalog.json' with {type:'json'};
export const SPECIES_CREATION_REFERENCE=Object.freeze(data);
const MODES=Object.freeze(['EOTE','AOR','MIXED']);
export function speciesForMode(mode='MIXED',query=''){
 if(!MODES.includes(mode)) throw new Error('campaign mode');
 const q=String(query).trim().toLocaleLowerCase('pt-BR');
 return SPECIES_CREATION_REFERENCE.filter(x=>(mode==='MIXED'||x.activeLines.includes(mode))&&(!q||`${x.name} ${x.references.filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' ')}`.toLocaleLowerCase('pt-BR').includes(q)));
}
export function speciesCreationCandidate(key,{mode='MIXED'}={}){
 const x=speciesForMode(mode).find(v=>v.key===key);if(!x)throw new Error('species reference');
 return {key:x.key,name:x.name,mode,mechanicallyActive:false,requiresMechanicalSourceBinding:true,references:x.references.filter(r=>r.line==='EOTE'||r.line==='AOR'),source:x.source};
}
export function speciesSupplementCoverage(){
 const byLine={EOTE:0,AOR:0},books={};
 for(const x of SPECIES_CREATION_REFERENCE){for(const l of x.activeLines)if(l in byLine)byLine[l]++;for(const r of x.references.filter(r=>r.line==='EOTE'||r.line==='AOR')){const b=r.citation.replace(/ p\\.\\d+.*$/,'').replace(/,$/,'');books[b]=(books[b]||0)+1;}}
 return {uniqueSpecies:SPECIES_CREATION_REFERENCE.length,byLine,books};
}
"""
(outdir/'speciesCreationReferenceIntegration.mjs').write_text(module,encoding='utf-8')

rt=RUNTIME.read_text('utf-8')
js=r'''
/* R54_SUPPLEMENT_SPECIES_REFERENCE */
var r54SupplementSpeciesPromise=null;
function r54SupplementSpeciesCatalog(){
 if(!r54SupplementSpeciesPromise)r54SupplementSpeciesPromise=fetch('js/reference/speciesCreationReferenceCatalog.json').then(r=>{if(!r.ok)throw new Error('species supplement reference');return r.json()}).catch(()=>[]);
 return r54SupplementSpeciesPromise;
}
async function r54SupplementSpeciesForMode(mode='MIXED',query=''){
 if(!['EOTE','AOR','MIXED'].includes(mode))throw new Error('campaign mode');
 const q=r54Norm(query),rows=await r54SupplementSpeciesCatalog();
 return rows.filter(x=>(mode==='MIXED'||(x.activeLines||[]).includes(mode))&&(!q||r54Norm(`${x.name||''} ${(x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' ')}`).includes(q)));
}
async function r54SupplementSpeciesCoverage(){
 const rows=await r54SupplementSpeciesCatalog(),byLine={EOTE:0,AOR:0},books={};
 for(const x of rows){for(const l of x.activeLines||[])if(l in byLine)byLine[l]++;for(const r of (x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR')){const b=String(r.citation||'').replace(/ p\.\d+.*$/,'').replace(/,$/,'');books[b]=(books[b]||0)+1}}
 return {uniqueSpecies:rows.length,byLine,books};
}
window.BraseiroSupplementReference=Object.freeze({speciesForMode:r54SupplementSpeciesForMode,coverage:r54SupplementSpeciesCoverage,mechanicalPolicy:'REFERENCE_ONLY_UNTIL_SOURCE_BINDING'});
async function r54SupplementSpeciesPanel(){
 const route=new URLSearchParams(location.search).get('screen')||'00';if(route!=='06'||document.querySelector('[data-r54-supplement-species]'))return;
 const rows=await r54SupplementSpeciesCatalog();if(!rows.length)return;
 const state=(typeof currentState==='function'?currentState():null)||{};const raw=String(state?.campaign?.mode||state?.mode||'MIXED').toUpperCase();const mode=['EOTE','AOR','MIXED'].includes(raw)?raw:'MIXED';
 const host=document.querySelector('main,.screen-content,.page-content,.r46-content')||document.body;const panel=document.createElement('section');panel.dataset.r54SupplementSpecies='1';panel.className='r54-supplement-species';
 panel.innerHTML='<h3>ESPÉCIES · SUPLEMENTOS</h3><p><small>Referências EotE/AoR recuperadas. Entradas sem bloco mecânico vinculado continuam apenas como referência.</small></p><input type="search" aria-label="Buscar espécie ou suplemento" placeholder="Buscar espécie ou suplemento…"><div class="r54-supplement-species-results"></div>';
 const input=panel.querySelector('input'),results=panel.querySelector('.r54-supplement-species-results');host.appendChild(panel);
 const render=async()=>{const found=await r54SupplementSpeciesForMode(mode,input.value);results.innerHTML=found.slice(0,12).map(x=>`<article><strong>${String(x.name||'').replace(/[<>&]/g,'')}</strong><small>${(x.references||[]).filter(r=>r.line==='EOTE'||r.line==='AOR').map(r=>r.citation).join(' · ').replace(/[<>&]/g,'')}</small></article>`).join('')||'<small>Nenhuma referência nesse modo.</small>'};
 input.addEventListener('input',render);render();
}
'''
if MARKER not in rt:
    # The historical visual restorer appends its own IIFE after the main runtime.
    # Insert supplement integration into the main runtime IIFE, never inside the
    # historical block, so a later visual refresh cannot truncate supplements.
    hist=rt.find('/* R54 HISTORICAL VISUAL RESTORE')
    prefix=rt[:hist] if hist>=0 else rt
    end=prefix.rfind('})();')
    if end<0: raise SystemExit('runtime IIFE end missing')
    rt=rt[:end]+js+'\n'+rt[end:]
old='function r54Tick(){r54FixSceneIcon();r54CharacterPortrait();r54ContextMapPreview();r54EnhanceItemImages()}'
new='function r54Tick(){r54FixSceneIcon();r54CharacterPortrait();r54ContextMapPreview();r54EnhanceItemImages();r54SupplementSpeciesPanel()}'
if old in rt: rt=rt.replace(old,new,1)
elif new not in rt: raise SystemExit('r54Tick integration point missing')
RUNTIME.write_text(rt,encoding='utf-8')
print(f'R54_SUPPLEMENTS_SPECIES_PASS species={len(catalog)} EOTE={eote} AOR={aor} books={len(books)}')
