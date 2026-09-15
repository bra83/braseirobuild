from pathlib import Path
import base64, json, re, sys
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
WWW = ROOT / 'app/src/main/assets/www'
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT/'docs/wave2/SCR015_RUNTIME_CAPTURE_415x915.png'
OUT.parent.mkdir(parents=True, exist_ok=True)

html=(WWW/'scr015.html').read_text(encoding='utf-8')
css=(WWW/'scr015.css').read_text(encoding='utf-8')
js=(WWW/'scr015.js').read_text(encoding='utf-8')
geom=json.loads((WWW/'data/scr015.geometry.json').read_text(encoding='utf-8'))
fixture=json.loads((WWW/'data/scr015.fixture.json').read_text(encoding='utf-8'))
derived=json.loads((WWW/'data/scr015.derived-assets.json').read_text(encoding='utf-8'))
scene_rules=json.loads((WWW/'data/scr015.rules.json').read_text(encoding='utf-8'))
wave3_character=json.loads((WWW/'data/wave3.core-character.fixture.json').read_text(encoding='utf-8'))
vg=(WWW/'core/visual-geometry.js').read_text(encoding='utf-8')
ar=(WWW/'core/asset-resolver.js').read_text(encoding='utf-8')

def data_url(rel):
    p=(WWW/rel).resolve()
    mime='image/png' if p.suffix.lower()=='.png' else 'application/octet-stream'
    return f'data:{mime};base64,'+base64.b64encode(p.read_bytes()).decode('ascii')

# Make every runtime image URL self-contained without changing application files.
all_paths=set()
for r in derived: all_paths.add(r['path'])
for rel in [
 'assets/encounter_hex_ruinas/01_compass_icon.png',
 'assets/encounter_hex_ruinas/10_scene_ruins_framed.png',
 'assets/encounter_hex_ruinas/06_location_badge_ruins.png',
 'assets/encounter_hex_ruinas/23_notes_icon_button.png',
]: all_paths.add(rel)
urlmap={rel:data_url(rel) for rel in all_paths}
for r in derived: r['path']=urlmap[r['path']]
for rel,url in urlmap.items():
    js=js.replace(f"'./{rel}'", repr(url)).replace(f'"./{rel}"', json.dumps(url))

# Inline every additional image literal introduced by the real implementation (exact chrome/static assets).
for rel in re.findall(r"['\"]\./(assets/[^'\"]+\.png)['\"]", js):
    if rel not in urlmap:
        urlmap[rel]=data_url(rel)
for rel,url in urlmap.items():
    js=js.replace(f"'./{rel}'", repr(url)).replace(f'"./{rel}"', json.dumps(url))

# Strip module imports and async fetch preamble; inject the exact data used by the app.
js=re.sub(r"import \{ validateGeometry, applyGeometry \} from './core/visual-geometry.js';\s*",'',js)
js=re.sub(r"import \{ AssetResolver \} from './core/asset-resolver.js';\s*",'',js)
js=re.sub(r"import \{ createWave3Runtime \} from './core/wave3-runtime.js';\s*",'',js)
js=re.sub(r"const \[geometry, fixture, derived, sceneRules, wave3Character\] = await Promise\.all\(\[.*?\]\);", 'const geometry=window.__GEOMETRY; const fixture=window.__FIXTURE; const derived=window.__DERIVED; const sceneRules=window.__SCENE_RULES; const wave3Character=window.__WAVE3_CHARACTER;', js, flags=re.S)
js=js.replace("`./${record.path}`", "record.path")
vg=vg.replace('export function ','function ').replace('export const ','const ')
ar=ar.replace('export class ','class ')

# Keep only actual markup body and actual stylesheet, then execute actual rendering source.
body=re.search(r'<body>(.*?)</body>', html, flags=re.S).group(1)
body=re.sub(r'<script[^>]*src="scr015.js"[^>]*></script>','',body)
page_html=f'''<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>{css}</style></head><body>{body}<script>
window.__GEOMETRY={json.dumps(geom, ensure_ascii=False)};
window.__FIXTURE={json.dumps(fixture, ensure_ascii=False)};
window.__DERIVED={json.dumps(derived, ensure_ascii=False)};
window.__SCENE_RULES={json.dumps(scene_rules, ensure_ascii=False)};
window.__WAVE3_CHARACTER={json.dumps(wave3_character, ensure_ascii=False)};
function createWave3Runtime(){{return {{status:'CAPTURE_HARNESS',actionService:null,campaignId:null}};}}
{vg}\n{ar}\n{js}
</script></body></html>'''

with sync_playwright() as pw:
    browser=pw.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
    page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
    page.set_content(page_html, wait_until='load')
    page.wait_for_function("document.fonts ? document.fonts.status === 'loaded' : true")
    page.wait_for_function("Array.from(document.images).every(i => i.complete && i.naturalWidth > 0)")
    page.wait_for_timeout(150)
    assert page.locator('#screen').get_attribute('data-geometry-layers') == '27'
    page.screenshot(path=str(OUT), full_page=False)
    # record runtime geometry for audit
    data=page.evaluate('''() => ({
      viewport:{w:innerWidth,h:innerHeight,dpr:devicePixelRatio},
      scroll:{w:document.documentElement.scrollWidth,h:document.documentElement.scrollHeight},
      elements:[...document.querySelectorAll('#screen > [id]')].map(e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e); return {id:e.id,x:r.x,y:r.y,w:r.width,h:r.height,font:s.fontFamily,fontSize:s.fontSize,fontWeight:s.fontWeight,lineHeight:s.lineHeight,color:s.color,bg:s.backgroundColor,overflow:s.overflow}})
    })''')
    (OUT.with_suffix('.runtime.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
    browser.close()
print('CAPTURE_PASS',OUT)
