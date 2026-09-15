from pathlib import Path
import sys,os,threading,http.server,socketserver,json
from playwright.sync_api import sync_playwright
root=Path(sys.argv[1]).resolve();out=Path(sys.argv[2]).resolve();out.mkdir(parents=True,exist_ok=True)
os.chdir(root/'app/src/main/assets/vtt')
class Handler(http.server.SimpleHTTPRequestHandler):
 def log_message(self,*a):pass
server=socketserver.TCPServer(('127.0.0.1',0),Handler);threading.Thread(target=server.serve_forever,daemon=True).start()
init="""window.__geminiPrompt=null;window.BraseiroGemini={openGeminiWithPrompt:(p)=>{window.__geminiPrompt=String(p||'');return JSON.stringify({ok:true})},pickSceneImage:(k)=>JSON.stringify({ok:true})};const h={get:(t,p)=>{if(p==='then')return undefined;return(...a)=>JSON.stringify({ok:true,items:[],data:[],state:{}})}};for(const n of ['BraseiroUiBridge','BraseiroLibrary','BraseiroImages','BraseiroFiles','BraseiroTts'])window[n]=new Proxy({},h);"""
results=[]
try:
 with sync_playwright() as p:
  browser=p.chromium.launch(headless=True)
  for w,h in [(415,915),(360,800)]:
   page=browser.new_page(viewport={'width':w,'height':h});errors=[];page.on('pageerror',lambda e:errors.append(str(e)));page.add_init_script(init)
   page.goto(f'http://127.0.0.1:{server.server_address[1]}/r46-shell.html');page.wait_for_selector('#sandbox-custom-start')
   page.locator('#sandbox-custom-start').fill('Alderaan · ponto escolhido pelo jogador')
   page.locator('[data-start-mode="map"]').click();view=page.locator('#sandbox-map .sw-map-viewport');view.scroll_into_view_if_needed();page.wait_for_timeout(150)
   view.click(position={'x':view.bounding_box()['width']*.23,'y':view.bounding_box()['height']*.41})
   page.locator('#sandbox-map button[aria-label="Ampliar mapa"]').click();assert float(view.get_attribute('data-zoom'))>1
   box=view.bounding_box();page.mouse.move(box['x']+100,box['y']+80);page.mouse.down();page.mouse.move(box['x']+60,box['y']+60,steps=5);page.mouse.up()
   assert 'translate(0px,0px)' not in page.locator('#sandbox-map .sw-map-plane').get_attribute('style')
   page.locator('#sandbox-start-button').click();page.wait_for_timeout(700)
   s=page.evaluate('window.BraseiroGameBridge.readState().data');anchor=s['world']['sandbox']['startMapPoint'];assert abs(anchor['x']-23)<2,anchor;assert 'Alderaan' in s['scene']['title'],s['scene']
   original=s['scene']['locationId'];page.locator('.sw-open-map').click();page.wait_for_selector('#sw-live-map');page.wait_for_timeout(600)
   assert page.locator('#sw-live-map').get_attribute('data-location-id')==original
   before=page.evaluate('JSON.stringify(window.BraseiroGameBridge.readState().data)')
   page.locator('#sw-live-map button[aria-label="Ampliar mapa"]').click()
   assert before==page.evaluate('JSON.stringify(window.BraseiroGameBridge.readState().data)'),'zoom mutated campaign'
   moved=page.evaluate('''()=>{const b=window.BraseiroGameBridge,s=b.readState().data,from=s.scene.locationId,r=s.world.routes.find(r=>r.fromLocationId===from||r.toLocationId===from);if(!r)throw Error('No initial route');const to=r.fromLocationId===from?r.toLocationId:r.fromLocationId;const result=b.travelToLocation(to);if(!result.ok)throw Error(JSON.stringify(result));return to;}''')
   page.wait_for_timeout(700);assert page.locator('#sw-live-map').get_attribute('data-location-id')==moved
   assert page.locator('.sw-map-graph [data-current="true"]').get_attribute('data-location-id')==moved
   page.reload();page.wait_for_selector('#sw-live-map');page.wait_for_timeout(600);s=page.evaluate('window.BraseiroGameBridge.readState().data');assert s['scene']['locationId']==moved;assert s['world']['sandbox']['startMapPoint']==anchor
   page.screenshot(path=str(out/f'map-{w}x{h}.png'),full_page=True)
   assert not errors,errors;results.append({'viewport':f'{w}x{h}','freeStart':True,'zoom':True,'drag':True,'travelMarker':True,'reloadPersistence':True,'pageErrors':errors});page.close()
  browser.close()
finally:server.shutdown();server.server_close()
(out/'MAP_RUNTIME_QA.json').write_text(json.dumps(results,ensure_ascii=False,indent=2));print('MAP_RUNTIME_PASS',json.dumps(results))
