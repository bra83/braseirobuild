from pathlib import Path
import json
from playwright.sync_api import sync_playwright

ROOT='http://127.0.0.1:8765'
OUT=Path('build/wwn-ui-captures'); OUT.mkdir(parents=True,exist_ok=True)
CASES=[('jornada.html','jornada'),('ficha.html','ficha'),('mochila.html','mochila'),('scr014.html','mapa'),('scr015.html','encontro'),('scr022.html','combate'),('diario.html','diario')]
results=[]
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage','--disable-gpu'])
    for page_name,label in CASES:
        page=browser.new_page(viewport={'width':415,'height':915},device_scale_factor=1)
        page_errors=[]; console_errors=[]
        page.on('pageerror',lambda exc,bag=page_errors:bag.append(str(exc)))
        page.on('console',lambda msg,bag=console_errors:bag.append(msg.text) if msg.type=='error' else None)
        response=page.goto(f'{ROOT}/{page_name}',wait_until='networkidle',timeout=30000)
        if page_name=='jornada.html':
            cards=page.locator('.card')
            if cards.count()>0:
                cards.first.click(); page.wait_for_timeout(500)
        page.wait_for_timeout(350)
        metrics=page.evaluate('''()=>({w:innerWidth,h:innerHeight,scrollW:document.documentElement.scrollWidth,scrollH:document.documentElement.scrollHeight,bodyW:document.body.scrollWidth})''')
        image_errors=page.locator('img').evaluate_all("els=>els.filter(e=>{const src=(e.getAttribute('src')||'').trim();return src&&(!e.complete||e.naturalWidth===0)}).map(e=>e.getAttribute('src'))")
        shot=OUT/f'{label}_415x915.png'; page.screenshot(path=str(shot),full_page=False,animations='disabled')
        result={'page':page_name,'http':response.status if response else None,'metrics':metrics,'pageErrors':page_errors,'consoleErrors':console_errors,'imageErrors':image_errors,'screenshot':str(shot)}
        results.append(result); page.close()
    browser.close()

fail=[]
for r in results:
    if r['http']!=200: fail.append(f"{r['page']}: HTTP {r['http']}")
    if r['metrics']['w']!=415 or r['metrics']['h']!=915: fail.append(f"{r['page']}: viewport {r['metrics']['w']}x{r['metrics']['h']}")
    if r['metrics']['scrollW']>415 or r['metrics']['bodyW']>415: fail.append(f"{r['page']}: horizontal overflow scrollW={r['metrics']['scrollW']} bodyW={r['metrics']['bodyW']}")
    if r['pageErrors']: fail.append(f"{r['page']}: page errors {r['pageErrors']}")
    if r['consoleErrors']: fail.append(f"{r['page']}: console errors {r['consoleErrors']}")
    if r['imageErrors']: fail.append(f"{r['page']}: broken images {r['imageErrors'][:5]}")
Path('build/wwn-ui-captures/report.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
print(json.dumps(results,ensure_ascii=False,indent=2))
if fail: raise SystemExit('\n'.join(fail))
print('WWN 415x915 player surface capture QA: OK')
