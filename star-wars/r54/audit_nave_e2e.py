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
  runtime[route]={'title':title,'h1':h1,'scene_title':scene_title,'overflow':page.evaluate('document.documentElement.scrollWidth-window.innerWidth'),'broken':page.locator('img').evaluate_all("xs=>xs.filter(x=>x.complete&&x.naturalWidth===0&&x.src.startsWith('http://127.0.0.1:8765/')).length")}
 checks['runtime_routes']=all(v['title'] and v['overflow']<=2 and v['broken']==0 for v in runtime.values()) and not errors

 page.goto(base+'?screen=00',wait_until='networkidle',timeout=120000);page.wait_for_timeout(250)
 overlay=page.locator('#braseiro-sandbox-setup')
 if overlay.count() and overlay.is_visible():
  start=page.locator('#sandbox-start-button').first
  if start.count():
   start.click(); page.wait_for_selector('#braseiro-sandbox-setup',state='detached',timeout=10000)
 nav=page.locator('.bottom-nav-button',has_text='NAVE').first
 if nav.count(): nav.click();page.wait_for_timeout(150)
 checks['nav_to_nave']=bool(nav.count() and re.search(r'[?&]screen=10(?:&|$)',page.url))

 runtime['bridge_methods']=page.evaluate("""() => {
   const b=window.BraseiroGameBridge||{};
   const names=new Set([...Object.keys(b),...Object.getOwnPropertyNames(Object.getPrototypeOf(b)||{})]);
   return [...names].filter(k=>k!=='constructor'&&typeof b[k]==='function').sort();
 }""")

 # Use the public game API to create/select a canonical EotE starter ship when the fresh
 # sandbox has no owned vehicle. This proves the real ownership path instead of mutating state.
 ownership=page.evaluate("""() => {
   const b=window.BraseiroGameBridge;
   if(!b||typeof b.readState!=='function') return {ok:false,reason:'bridge'};
   const state=()=>{const r=b.readState();return r&&r.ok?r.data:null};
   const s0=state(),v0=s0&&s0.vehicles,owned=v0&&v0.vehicles&&typeof v0.vehicles==='object'?v0.vehicles:{};
   if(v0&&v0.activeVehicleId&&owned[v0.activeVehicleId]) return {ok:true,mode:'existing',id:v0.activeVehicleId};
   const candidates=['yt1300','wayfarer','firespray'];
   if(typeof b.addVehicle==='function'){
     for(const key of candidates){
       try{const r=b.addVehicle(key,{setActive:true,id:'audit_'+key});if(!r||r.ok!==false){const s=state(),id=s&&s.vehicles&&s.vehicles.activeVehicleId;if(id)return {ok:true,mode:'addVehicle',profile:key,id};}}catch(e){}
     }
   }
   if(typeof b.purchaseEconomyItem==='function'){
     for(const key of candidates){
       try{const r=b.purchaseEconomyItem(key,{free:true,source:'r54_nave_e2e'});if(!r||r.ok!==false){const s=state(),id=s&&s.vehicles&&s.vehicles.activeVehicleId;if(id)return {ok:true,mode:'purchaseEconomyItem',profile:key,id};}}catch(e){}
     }
   }
   return {ok:false,reason:'no_public_vehicle_path'};
 }""")
 runtime['ownership']=ownership
 checks['canonical_vehicle_ownership_path']=bool(ownership.get('ok'))

 def vehicle_snapshot():
  return page.evaluate("""() => {
    const b=window.BraseiroGameBridge;
    if(!b||typeof b.readState!=='function') return {bridge:false};
    const r=b.readState(), s=r&&r.ok?r.data:null, v=s&&s.vehicles;
    const owned=v&&v.vehicles&&typeof v.vehicles==='object'?v.vehicles:{};
    const ids=Object.keys(owned);
    const id=v&&v.activeVehicleId;
    const ship=id&&owned[id]?owned[id]:null;
    return {bridge:true,id:id||null,vehicleCount:ids.length,vehicleIds:ids,
      vehicleNames:ids.map(k=>owned[k]&&owned[k].name||k),name:ship&&ship.name||null,
      hull:(ship&&ship.hullDamage)??null,strain:(ship&&ship.systemStrain)??null,
      hyperdrive:(ship&&ship.hyperdrive)??null};
  }""")

 before=vehicle_snapshot()
 page.reload(wait_until='networkidle',timeout=120000);page.wait_for_timeout(250)
 after=vehicle_snapshot()
 checks['canonical_vehicle_state']=bool(before.get('bridge') and before.get('id') and before.get('name'))
 checks['vehicle_persists_reload']=bool(checks['canonical_vehicle_state'] and before==after)
 runtime['vehicle_before_reload']=before; runtime['vehicle_after_reload']=after
 b.close()

failed=[k for k,v in checks.items() if not v]
print(json.dumps({'checks':checks,'runtime':runtime,'errors':errors,'failed':failed},ensure_ascii=False,indent=2))
if failed: raise SystemExit('R54_NAVE_E2E_FAIL: '+', '.join(failed))
print('R54_NAVE_E2E_PASS')
