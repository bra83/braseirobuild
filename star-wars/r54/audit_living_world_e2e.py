#!/usr/bin/env python3
from pathlib import Path
import json, sys
from playwright.sync_api import sync_playwright

root=Path(sys.argv[1]).resolve()
base='http://127.0.0.1:8765/r46-shell.html'
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True)
    page=browser.new_page(viewport={'width':415,'height':915})
    errors=[]; page.on('pageerror',lambda e: errors.append(str(e)))
    page.goto(base,wait_until='domcontentloaded',timeout=30000)
    page.wait_for_function("()=>window.BraseiroGameBridge&&typeof window.BraseiroGameBridge.readState==='function'",timeout=15000)
    result=page.evaluate("""()=>{
      const b=window.BraseiroGameBridge;
      const collectionValues=v=>Array.isArray(v)?v:(v&&typeof v==='object'?Object.values(v):[]);
      const collectionShape=v=>Array.isArray(v)?'array':(v&&typeof v==='object'?'object':typeof v);
      let s=b.readState();
      if(s.ok&&s.data===null){const c=b.initializeCampaign();if(!c.ok)throw Error(JSON.stringify(c));}
      s=b.readState().data;
      if(!s?.world?.sandbox?.initialized){const r=b.initializeLivingSandbox({startingPoint:'Mos Eisley · Tatooine',regionLabel:'Borda Exterior',startMapPoint:{x:65,y:55}});if(!r.ok)throw Error(JSON.stringify(r));}
      const before=b.readState().data;
      const world=before.world||{}, sandbox=world.sandbox||{};
      const factions=collectionValues(world.factions);
      const npcs=collectionValues(world.npcs);
      const locations=collectionValues(world.locations);
      const routes=collectionValues(world.routes);
      const from=before.scene?.locationId;
      const route=routes.find(r=>r?.fromLocationId===from||r?.toLocationId===from);
      if(!route)throw Error('living world has no route from current location');
      const to=route.fromLocationId===from?route.toLocationId:route.fromLocationId;
      const tr=b.travelToLocation(to);if(!tr.ok)throw Error(JSON.stringify(tr));
      const after=b.readState().data;
      return {
        factions:factions.length,npcs:npcs.length,locations:locations.length,routes:routes.length,
        shapes:{factions:collectionShape(world.factions),npcs:collectionShape(world.npcs),locations:collectionShape(world.locations),routes:collectionShape(world.routes)},
        initialized:!!sandbox.initialized,from,to,travelPersisted:after.scene?.locationId===to,startMapPoint:after.world?.sandbox?.startMapPoint||null
      };
    }""")
    page.reload(wait_until='domcontentloaded',timeout=30000)
    page.wait_for_function("()=>window.BraseiroGameBridge&&window.BraseiroGameBridge.readState().ok",timeout=15000)
    persisted=page.evaluate("()=>window.BraseiroGameBridge.readState().data.scene.locationId")
    browser.close()

assert not errors,errors
assert result['initialized'],result
assert result['factions']>=6,result
assert result['npcs']>=36,result
assert result['locations']>=12,result
assert result['routes']>0,result
assert result['travelPersisted'] and persisted==result['to'],(result,persisted)
assert result['startMapPoint'],result
print('R54_LIVING_WORLD_E2E_PASS',json.dumps(result,ensure_ascii=False,sort_keys=True))
