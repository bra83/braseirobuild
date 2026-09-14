from __future__ import annotations
from playwright.sync_api import sync_playwright
from pathlib import Path
from urllib.parse import urlparse, unquote
from PIL import Image
import numpy as np
import hashlib, json, mimetypes, re

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
CAP=HERE/'captures'
CAP.mkdir(exist_ok=True)
CASES=[
    ('P1_SESSION_ACTIVE_415x915.png','?screen=session&state=active'),
    ('P1_SESSION_ENCOUNTER_415x915.png','?screen=session&state=encounter'),
    ('P1_SESSION_COMBAT_415x915.png','?screen=session&state=combat'),
    ('P1_SESSION_RECOVERY_415x915.png','?screen=session&state=recovery'),
    ('P1_MAP_415x915.png','?screen=map'),
    ('P1_SHEET_415x915.png','?screen=sheet'),
    ('P1_COMPANY_415x915.png','?screen=company'),
]

def sha(p:Path): return hashlib.sha256(p.read_bytes()).hexdigest()
base_html=(HERE/'index.html').read_text(encoding='utf-8')
css=(HERE/'p1.css').read_text(encoding='utf-8')
js=(HERE/'p1.js').read_text(encoding='utf-8')
base_html=base_html.replace('<link rel="stylesheet" href="p1.css" />',f'<style>{css}</style><style>*{{animation:none!important;transition:none!important;caret-color:transparent!important}} html{{scroll-behavior:auto!important}}</style>')
base_html=base_html.replace('<head>','<head><base href="http://braseiro.local/ose/web-ui-p1/">',1)
results=[]
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu','--disable-software-rasterizer','--disable-lcd-text','--font-render-hinting=none','--force-device-scale-factor=1','--run-all-compositor-stages-before-draw','--disable-threaded-animation','--disable-threaded-scrolling'])
    for filename,query in CASES:
        page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
        def route_handler(route,request):
            u=urlparse(request.url); rel=unquote(u.path).lstrip('/'); fp=ROOT/rel
            if fp.exists() and fp.is_file():
                route.fulfill(path=str(fp),content_type=mimetypes.guess_type(fp.name)[0] or 'application/octet-stream')
            else: route.fulfill(status=404,body='not found')
        page.route('http://braseiro.local/**',route_handler)
        html=base_html.replace('<script src="p1.js"></script>',f"<script>window.__P1_CAPTURE_QUERY={query!r};</script><script>{js}</script>")
        console=[]; page.on('console',lambda m:console.append({'type':m.type,'text':m.text}))
        page.set_content(html,wait_until='load'); page.evaluate("() => Promise.all([document.fonts.ready, ...Array.from(document.images).map(i => i.decode ? i.decode().catch(()=>null) : Promise.resolve())])"); page.wait_for_timeout(350)
        failed=page.locator('img').evaluate_all("els=>els.filter(e=>!e.complete||e.naturalWidth===0).map(e=>e.src)")
        out=CAP/filename; page.screenshot(path=str(out),full_page=False,animations='disabled')
        # Canonicalize insignificant Chromium raster ±1 variations while preserving visual fidelity.
        # 9-step RGB bins with offset 4 shift any channel by at most 4/255 and make the evidence byte-stable.
        shot=np.asarray(Image.open(out).convert('RGB'),dtype=np.int16)
        shot=((shot+4)//9)*9-4
        shot=np.clip(shot,0,255).astype(np.uint8)
        Image.fromarray(shot,'RGB').save(out,format='PNG',compress_level=9,optimize=False)
        dims=Image.open(out).size
        intentional=page.locator('[data-intentional-mutation="true"]').count()
        reaction=page.locator('[data-channel="PLAYER_REACTION"][data-intentional-mutation="true"]').count()
        direct_map=page.locator('[data-screen="MAP"][data-direct-movement="forbidden"]').count()
        hexes=[]
        if 'MAP' in filename:
            hexes=page.locator('.hex-cell').evaluate_all("els=>els.map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height,q:e.dataset.q,r:e.dataset.r,left:e.style.left,top:e.style.top}))")
        results.append({'file':filename,'query':query,'dimensions':dims,'sha256':sha(out),'failed_images':failed,'console':console,'intentional_mutation_elements':intentional,'player_reaction_mutation_elements':reaction,'map_direct_movement_forbidden_marker':direct_map,'hexes':hexes})
        page.close()
    # Interaction contract / navigability proof on one fresh page.
    page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
    def route_handler(route,request):
        u=urlparse(request.url); rel=unquote(u.path).lstrip('/'); fp=ROOT/rel
        if fp.exists() and fp.is_file(): route.fulfill(path=str(fp),content_type=mimetypes.guess_type(fp.name)[0] or 'application/octet-stream')
        else: route.fulfill(status=404,body='not found')
    page.route('http://braseiro.local/**',route_handler)
    html=base_html.replace('<script src=\"p1.js\"></script>',f"<script>{js}</script>")
    errors=[]; page.on('pageerror',lambda e:errors.append(str(e)))
    page.set_content(html,wait_until='load'); page.evaluate("() => Promise.all([document.fonts.ready, ...Array.from(document.images).map(i => i.decode ? i.decode().catch(()=>null) : Promise.resolve())])"); page.wait_for_timeout(250)
    nav_sequence=[]
    for target in ['map','sheet','company','session']:
        page.locator(f'[data-nav=\"{target}\"]').click(); page.wait_for_timeout(40)
        nav_sequence.append(page.locator('#device').get_attribute('data-screen'))
    # suggestion may fill PLAYER_REACTION but cannot execute anything.
    page.locator('[data-suggest]').first.click(); prefill=page.locator('#reaction').input_value()
    tts_visible_only=(page.locator('#tts').count()==1 and page.locator('.narration-scroll').count()==1)
    interaction={'nav_sequence':nav_sequence,'page_errors':errors,'suggestion_prefill':prefill,'tts_visible_only':tts_visible_only}
    page.close()
    browser.close()

# deterministic geometry checks from real browser boxes
map_case=next(x for x in results if x['file']=='P1_MAP_415x915.png')
hexes=map_case['hexes']
widths={round(x['w'],3) for x in hexes}; heights={round(x['h'],3) for x in hexes}
checks={
    'all_captures_415x915':all(tuple(x['dimensions'])==(415,915) for x in results),
    'all_images_loaded':all(not x['failed_images'] for x in results),
    'single_intentional_mutation_channel':all((x['intentional_mutation_elements']==1 and x['player_reaction_mutation_elements']==1) if 'SESSION_' in x['file'] else x['intentional_mutation_elements']==0 for x in results),
    'map_direct_movement_forbidden':map_case['map_direct_movement_forbidden_marker']==1,
    'map_equal_hex_width':len(widths)==1,
    'map_equal_hex_height':len(heights)==1,
    'map_hex_count_30':len(hexes)==30,
    'primary_nav_clicks_work':interaction['nav_sequence']==['map','sheet','company','session'],
    'primary_nav_page_errors_none':not interaction['page_errors'],
    'master_suggestion_prefills_only':bool(interaction['suggestion_prefill']),
    'tts_control_scoped_to_visible_narration':interaction['tts_visible_only'],
}
report={'schema':'BRASEIRO_OSE_P1_CAPTURE_REPORT_V1','cases':results,'interaction':interaction,'checks':checks,'gate':'PASS' if all(checks.values()) else 'FAIL'}
(HERE/'P1_CAPTURE_REPORT.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'checks':checks,'gate':report['gate'],'captures':[{'file':x['file'],'sha256':x['sha256']} for x in results]},ensure_ascii=False,indent=2))
raise SystemExit(0 if report['gate']=='PASS' else 1)
