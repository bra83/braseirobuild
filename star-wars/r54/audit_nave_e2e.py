#!/usr/bin/env python3
from pathlib import Path
import json,re,sys
from playwright.sync_api import sync_playwright

if len(sys.argv)!=2: raise SystemExit('usage: audit_nave_e2e.py <android-project-root>')
root=Path(sys.argv[1]).resolve(); vtt=root/'app/src/main/assets/vtt'
shell=(vtt/'r46/r46-shell.bundle.js').read_text('utf-8',errors='replace')
bridge=(vtt/'js/core/uiBridge.bundle.js').read_text('utf-8',errors='replace')
manifest=(vtt/'image-library/manifest.json').read_text('utf-8',errors='replace') if (vtt/'image-library/manifest.json').exists() else ''
checks={}
checks['routes']=all(x in shell for x in ["screen(10, 'Nave — Resumo'","screen(11, 'Nave — Danos críticos'"])
checks['projection']=all(x in shell for x in ['function projectVehicle(state)','activeVehicleId','hullDamage','systemStrain','criticalHits','hyperdrive'])
checks['combat_state']=all(x in bridge for x in ['state.vehicles?.combat?.active','targetVehicleId','activeVehicleId'])
checks['historical_vehicle_library']=all((vtt/'image-library'/n).exists() for n in ['R40_KAINRATH_VEHICLE_WAVE1_INDEX.json','R41_KAINRATH_VEHICLE_WAVE2_INDEX.json','R42_KAINRATH_VEHICLE_WAVE3_INDEX.json'])
checks['manifest_vehicle_refs']=bool(re.search(r'vehicle|ship|landspeeder|airspeeder|walker',manifest,re.I))

base='http://127.0.0.1:8765/r46-shell.html'
with sync_playwright() as p:
 b=p.chromium.launch(headless=True); page=b.new_page(viewport={'width':415,'height':915}); errors=[]; page.on('pageerror',lambda e:errors.append(str(e)))
 runtime={}
 for route in ['10','11','03','04']:
  page.goto(base+'?screen='+route,wait_until='networkidle',timeout=120000);page.wait_for_timeout(250)
  h1=page.locator('h1').first.inner_text() if page.locator('h1').count() else ''
  scene_title=page.locator('.scene-context-title strong').first.inner_text() if page.locator('.scene-context-title strong').count() else ''
  title=(h1 or scene_title).strip()
  runtime[route]={'title':title,'h1':h1,'scene_title':scene_title,'overflow':page.evaluate('document.documentElement.scrollWidth-window.innerWidth'),'broken':page.locator('img').evaluate_all('xs=>xs.filter(x=>x.complete&&x.naturalWidth===0).length')}
 checks['runtime_routes']=all(v['title'] and v['overflow']<=2 and v['broken']==0 for v in runtime.values()) and not errors
 # NAVE bottom navigation is blocked by the first-run sandbox modal by design.
 # Complete that real setup flow, then prove the bottom navigation reaches route 10.
 page.goto(base+'?screen=00',wait_until='networkidle',timeout=120000);page.wait_for_timeout(250)
 overlay=page.locator('#braseiro-sandbox-setup')
 if overlay.count() and overlay.is_visible():
  start=page.locator('#sandbox-start-button').first
  if start.count():
   start.click(); page.wait_for_selector('#braseiro-sandbox-setup',state='detached',timeout=10000)
 nav=page.locator('.bottom-nav-button',has_text='NAVE').first
 if nav.count(): nav.click();page.wait_for_timeout(150)
 checks['nav_to_nave']=bool(nav.count() and re.search(r'[?&]screen=10(?:&|$)',page.url))
 b.close()

failed=[k for k,v in checks.items() if not v]
print(json.dumps({'checks':checks,'runtime':runtime,'errors':errors,'failed':failed},ensure_ascii=False,indent=2))
if failed: raise SystemExit('R54_NAVE_E2E_FAIL: '+', '.join(failed))
print('R54_NAVE_E2E_PASS')
