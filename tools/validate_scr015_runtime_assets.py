from pathlib import Path
from PIL import Image
import json, hashlib
ROOT=Path(__file__).resolve().parents[1]
WWW=ROOT/'app/src/main/assets/www'
EX=WWW/'assets/encounter_hex_ruinas_exact'
prov=json.loads((EX/'PROVENANCE.json').read_text())
errors=[]; rows=[]
for rec in prov:
    p=EX/rec['derived']
    if not p.exists(): errors.append(f'missing:{p.name}'); continue
    sha=hashlib.sha256(p.read_bytes()).hexdigest()
    if sha!=rec['sha256']: errors.append(f'hash:{p.name}')
    im=Image.open(p).convert('RGBA'); a=im.getchannel('A')
    nonzero=sum(1 for v in a.getdata() if v>0); transparent=sum(1 for v in a.getdata() if v==0)
    if nonzero==0: errors.append(f'empty_alpha:{p.name}')
    if rec.get('dynamic_text_removed') and transparent==0 and not rec.get('method','').startswith('restricted_glyph_inpaint'): errors.append(f'no_transparency:{p.name}')
    rows.append({'file':p.name,'size':im.size,'nonzero_alpha':nonzero,'transparent':transparent,'method':rec['method'],'sha256':sha})
# exact scene/brand/location fixed assets
for rel in ['assets/encounter_hex_ruinas/01_compass_icon.png','assets/encounter_hex_ruinas/10_scene_ruins_framed.png','assets/encounter_hex_ruinas/06_location_badge_ruins.png','assets/encounter_hex_ruinas/23_notes_icon_button.png']:
    p=WWW/rel
    if not p.exists(): errors.append(f'missing:{rel}')
# CSS/JS guardrails
css=(WWW/'scr015.css').read_text(); js=(WWW/'scr015.js').read_text()
for token in ['background-size:contain','object-fit:contain']:
    if token not in css: errors.append(f'css_missing:{token}')
if "applyChrome" not in js: errors.append('js_missing:applyChrome')
if 'choice-label' not in js: errors.append('js_missing:native_choice_label')
report={'ok':not errors,'errors':errors,'asset_count':len(rows)+4,'derived_assets':rows,'guards':{'non_proportional_stretch_forbidden':True,'dynamic_text_native':True,'exact_static_component_reuse':True}}
out=ROOT/'docs/wave2/SCR015_RUNTIME_ASSET_QA.json'; out.write_text(json.dumps(report,indent=2),encoding='utf-8')
print('SCR015_RUNTIME_ASSET_QA_PASS' if not errors else 'SCR015_RUNTIME_ASSET_QA_FAIL', 'assets=',report['asset_count'],'errors=',len(errors))
if errors: print('\n'.join(errors)); raise SystemExit(1)
