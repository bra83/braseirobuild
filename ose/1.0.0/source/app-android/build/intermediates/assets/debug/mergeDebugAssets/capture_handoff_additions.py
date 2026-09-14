from pathlib import Path
from urllib.parse import urlparse, unquote
from playwright.sync_api import sync_playwright
from PIL import Image
import hashlib, json, mimetypes

HERE=Path(__file__).resolve().parent
ROOT=HERE.parent
OUT=HERE/'captures_handoff_2026_09_13'; OUT.mkdir(exist_ok=True)
html=(HERE/'index.html').read_text()
css=(HERE/'p1.css').read_text()
js=(HERE/'p1.js').read_text()
html=html.replace('<link rel="stylesheet" href="p1.css">', f'<style>{css}</style><style>*{{animation:none!important;transition:none!important;caret-color:transparent!important}}</style>')
html=html.replace('<head>', '<head><base href="http://braseiro.local/web-ui-p1/">',1)
cases=[('session','active'),('session','encounter'),('session','combat'),('session','recovery'),('map','active'),('sheet','active'),('company','active')]
results=[]
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
    for screen,state in cases:
        page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
        def route(route,request):
            path=unquote(urlparse(request.url).path).lstrip('/')
            fp=ROOT/path
            if fp.is_file(): route.fulfill(path=str(fp),content_type=mimetypes.guess_type(fp.name)[0] or 'application/octet-stream')
            else: route.fulfill(status=404,body='not found')
        page.route('http://braseiro.local/**',route)
        doc=html.replace('<script src="p1.js"></script>',f'<script>window.__P1_CAPTURE_QUERY="?screen={screen}&state={state}";</script><script>{js}</script>')
        errors=[]; page.on('pageerror', lambda e: errors.append(str(e)))
        page.set_content(doc,wait_until='load'); page.wait_for_timeout(250)
        failed=page.locator('img').evaluate_all("els=>els.filter(e=>!e.complete||e.naturalWidth===0).map(e=>e.src)")
        fn=f'HANDOFF_{screen.upper()}_{state.upper()}_415x915.png'; fp=OUT/fn
        page.screenshot(path=str(fp),full_page=False,animations='disabled')
        hexes=page.locator('.hex-cell').evaluate_all("els=>els.map(e=>({w:e.getBoundingClientRect().width,h:e.getBoundingClientRect().height}))") if screen=='map' else []
        results.append({'file':fn,'screen':screen,'state':state,'sha256':hashlib.sha256(fp.read_bytes()).hexdigest(),'dimensions':Image.open(fp).size,'failed_images':failed,'page_errors':errors,'suggestions':page.locator('[data-suggest]').count(),'free_reaction':page.locator('#reactionInput').count(),'hexes':hexes,'nav_labels':page.locator('.navbtn').all_text_contents()})
        page.close()
    # interaction/settings proof
    page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
    page.route('http://braseiro.local/**',lambda route,request: route.fulfill(path=str(ROOT/unquote(urlparse(request.url).path).lstrip('/')),content_type=mimetypes.guess_type(request.url)[0] or 'application/octet-stream') if (ROOT/unquote(urlparse(request.url).path).lstrip('/')).is_file() else route.fulfill(status=404,body=''))
    doc=html.replace('<script src="p1.js"></script>',f'<script>{js}</script>')
    page.set_content(doc,wait_until='load'); page.wait_for_timeout(150)
    nav=[]
    for target in ['map','sheet','company','session']:
        page.locator(f'.navbtn[data-screen="{target}"]').click(); page.wait_for_timeout(30); nav.append(page.locator(f'.navbtn[data-screen="{target}"]').get_attribute('class'))
    # suggestions prefill only
    page.locator('[data-suggest]').first.click(); prefill=page.locator('#reactionInput').input_value()
    page.locator('#settingsButton').click();
    settings={'password_fields':page.locator('#settingsOverlay input[type=password]').count(),'text_label':page.locator('text=Gemini — Texto / cena').count(),'tts_label':page.locator('text=Gemini — TTS / áudio').count()}
    page.close();browser.close()

mapr=next(x for x in results if x['screen']=='map')
checks={
 'all_415x915':all(tuple(x['dimensions'])==(415,915) for x in results),
 'no_page_errors':all(not x['page_errors'] for x in results),
 'all_images_load':all(not x['failed_images'] for x in results),
 'session_exactly_3_suggestions':all(x['suggestions']==3 for x in results if x['screen']=='session'),
 'session_free_reaction_present':all(x['free_reaction']==1 for x in results if x['screen']=='session'),
 'nav_exact_labels':all([s.strip() for s in x['nav_labels']]==['⬡\nSESSÃO','🗺\nMAPA','📜\nFICHA','♟♟\nCOMPANHIA'] or [s.replace('\n','').strip() for s in x['nav_labels']]==['⬡SESSÃO','🗺MAPA','📜FICHA','♟♟COMPANHIA'] for x in results),
 'map_30_equal_hexes':len(mapr['hexes'])==30 and len({round(h['w'],3) for h in mapr['hexes']})==1 and len({round(h['h'],3) for h in mapr['hexes']})==1,
 'nav_clicks_active':all('active' in c for c in nav),
 'suggestion_prefills_only':bool(prefill),
 'two_masked_gemini_fields':settings['password_fields']==2 and settings['text_label']>=1 and settings['tts_label']>=1,
}
report={'schema':'BRASEIRO_OSE_HANDOFF_ADDITIONS_VISUAL_QA_V1','results':results,'settings':settings,'checks':checks,'gate':'PASS' if all(checks.values()) else 'FAIL'}
(HERE/'BRASEIRO_OSE_HANDOFF_ADDITIONS_VISUAL_QA.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps({'gate':report['gate'],'checks':checks,'captures':[{'file':r['file'],'sha256':r['sha256']} for r in results]},ensure_ascii=False,indent=2))
raise SystemExit(0 if report['gate']=='PASS' else 1)
