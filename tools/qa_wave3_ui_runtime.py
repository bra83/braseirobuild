from pathlib import Path
import base64, json, re
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
WWW=ROOT/'app/src/main/assets/www'
OUTDIR=ROOT/'docs/wave3'
OUTDIR.mkdir(parents=True,exist_ok=True)

def strip_module(src):
    src=re.sub(r"^import .*?;\s*$",'',src,flags=re.M)
    src=src.replace('export function ','function ').replace('export const ','const ').replace('export class ','class ')
    return src

def data_url(rel):
    p=(WWW/rel).resolve()
    return 'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode('ascii')

html=(WWW/'scr015.html').read_text(encoding='utf-8')
css=(WWW/'scr015.css').read_text(encoding='utf-8')
scr=strip_module((WWW/'scr015.js').read_text(encoding='utf-8'))
geom=json.loads((WWW/'data/scr015.geometry.json').read_text(encoding='utf-8'))
fixture=json.loads((WWW/'data/scr015.fixture.json').read_text(encoding='utf-8'))
derived=json.loads((WWW/'data/scr015.derived-assets.json').read_text(encoding='utf-8'))
scene_rules=json.loads((WWW/'data/scr015.rules.json').read_text(encoding='utf-8'))
character=json.loads((WWW/'data/wave3.core-character.fixture.json').read_text(encoding='utf-8'))

modules=[]
for rel in [
    'core/canonical-json.js','core/rng-core.js','core/campaign-state.js','core/persistence-port.js',
    'core/wwn-rules-core.js','core/barbara-test-adapter.js','core/gm-help-service.js',
    'core/action-service.js','core/wave3-runtime.js','core/visual-geometry.js','core/asset-resolver.js',
]:
    modules.append(strip_module((WWW/rel).read_text(encoding='utf-8')))

# Inline every image used by the real implementation.
paths={r['path'] for r in derived}
paths |= set(re.findall(r"['\"]\./(assets/[^'\"]+\.png)['\"]",scr))
urlmap={rel:data_url(rel) for rel in paths}
for r in derived: r['path']=urlmap[r['path']]
for rel,url in urlmap.items():
    scr=scr.replace(f"'./{rel}'",repr(url)).replace(f'"./{rel}"',json.dumps(url))
scr=scr.replace("`./${record.path}`", "record.path")

# Replace fetch preamble with injected exact JSON fixtures.
scr=re.sub(
    r"const \[geometry, fixture, derived, sceneRules, wave3Character\] = await Promise\.all\(\[.*?\]\);",
    'const geometry=window.__GEOMETRY; const fixture=window.__FIXTURE; const derived=window.__DERIVED; const sceneRules=window.__SCENE_RULES; const wave3Character=window.__WAVE3_CHARACTER;',
    scr,flags=re.S,
)
body=re.search(r'<body>(.*?)</body>',html,flags=re.S).group(1)
body=re.sub(r'<script[^>]*src="scr015.js"[^>]*></script>','',body)
page_html=f'''<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>{css}</style></head><body>{body}<script>
window.__GEOMETRY={json.dumps(geom,ensure_ascii=False)};
window.__FIXTURE={json.dumps(fixture,ensure_ascii=False)};
window.__DERIVED={json.dumps(derived,ensure_ascii=False)};
window.__SCENE_RULES={json.dumps(scene_rules,ensure_ascii=False)};
window.__WAVE3_CHARACTER={json.dumps(character,ensure_ascii=False)};
{chr(10).join(modules)}
{scr}
</script></body></html>'''

results={}
with sync_playwright() as pw:
    browser=pw.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
    ctx=browser.new_context(viewport={'width':415,'height':915},device_scale_factor=1,has_touch=True,is_mobile=True)
    page=ctx.new_page(); page.set_content(page_html,wait_until='load')
    page.wait_for_function("document.fonts ? document.fonts.status === 'loaded' : true")
    page.wait_for_function("Array.from(document.images).every(i=>i.complete && i.naturalWidth>0)")
    page.wait_for_timeout(100)
    results['runtime_status']=page.locator('#screen').get_attribute('data-wave3-runtime')
    results['initial_contact']=page.locator('#HEX_ENCOUNTER_RUINAS_09').inner_text()
    page.screenshot(path=str(OUTDIR/'SCR015_WAVE3_UI_BEFORE_415x915.png'),full_page=False)
    page.locator('#HEX_ENCOUNTER_RUINAS_16').tap()
    page.wait_for_timeout(50)
    debug=json.loads(page.locator('#debug').text_content())
    results['observe_status']=debug.get('status')
    results['observe_revision']=debug.get('revision')
    results['after_contact']=page.locator('#HEX_ENCOUNTER_RUINAS_09').inner_text()
    results['after_narrative']=page.locator('#HEX_ENCOUNTER_RUINAS_14').inner_text()
    page.screenshot(path=str(OUTDIR/'SCR015_WAVE3_UI_AFTER_OBSERVE_415x915.png'),full_page=False)
    page.locator('#HEX_ENCOUNTER_RUINAS_17').tap()
    page.wait_for_timeout(20)
    deferred=json.loads(page.locator('#debug').text_content())
    results['approach_status']=deferred.get('status')
    results['approach_revision']=deferred.get('revision')
    results['pass']=(
        results['runtime_status']=='OK' and results['observe_status']=='OK' and results['observe_revision']==1
        and results['after_contact'] in ('CONTATO OBSERVADO','CONTATO INESPERADO')
        and 'teste totalizou' in results['after_narrative']
        and results['approach_status']=='DEFERRED_NO_MUTATION' and results['approach_revision']==1
    )
    browser.close()
(OUTDIR/'WAVE3_UI_RUNTIME_QA.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(results,ensure_ascii=False,indent=2))
print('WAVE3_UI_RUNTIME_QA_PASS' if results['pass'] else 'WAVE3_UI_RUNTIME_QA_FAIL')
raise SystemExit(0 if results['pass'] else 1)
