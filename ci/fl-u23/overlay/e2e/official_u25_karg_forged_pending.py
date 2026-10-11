#!/usr/bin/env python3
"""U25 Chromium/IndexedDB fail-closed projection for forged #35 sacrifice saves.
A payload is not adjudication merely because the imported object exists.
"""
import asyncio,json,os,sys,traceback,uuid
from pathlib import Path
from playwright.async_api import async_playwright,expect
from official_ui_browser import mount
from official_u25_karg_sacrifice_paths import SEED
ORIGIN=os.environ.get('FL_E2E_ORIGIN','http://127.0.0.1:4173')
OUT=Path(os.environ.get('FL_E2E_ARTIFACTS','e2e-results'));OUT.mkdir(parents=True,exist_ok=True)
SETUP=r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');const db=await getDb();
 const world=await db.get('world_state',c.campaignId);
 const event=world.journey.events.find(e=>e.id===c.eventId);
 event.status='open';
 event.result.officialEncounterVisitNumber=1;
 event.result.officialSceneDecision={
  choiceId:'offer_'+c.kind,stage:'karg_sacrifice_pending',kargOfferAccepted:true,
  pending:'follow_up',kargSacrifice:{kind:c.kind,targetId:c.heroId,
   targetName:'Exploradora U25',adjudicatedAt:12345,
   evidence:'Objeto importado sem evento resolvido e sem adjudicação.'}};
 await db.put('world_state',world);
}'''
async def main():
 result={'status':'FAIL','browserExecuted':False,'expected':2,'passed':0,'cases':[],'failures':[]}
 async with async_playwright() as p:
  browser=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
  page=await browser.new_page()
  try:
   response=await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded')
   assert response and response.status==200
   await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
   for kind in ('adventurer','horse'):
    c={'kind':kind,'campaignId':'u25-pending-'+uuid.uuid4().hex,
      'eventId':'u25-e-'+uuid.uuid4().hex,'heroId':'u25-h-'+uuid.uuid4().hex}
    proof={'kind':kind,'status':'FAIL','reloads':0}
    try:
     await page.evaluate(SEED,c);await page.evaluate(SETUP,c)
     await mount(page,c['campaignId'])
     for iteration in range(2):
      receipt=page.get_by_test_id('fl-official-receipt-'+c['eventId'])
      await expect(receipt).to_have_attribute('data-follow-up-pending','true')
      await expect(receipt).to_have_attribute('data-resource-transfer-committed','false')
      await expect(receipt).not_to_contain_text('Sacrifício adjudicado')
      if iteration==0:
       await page.reload(wait_until='domcontentloaded')
       await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
       await mount(page,c['campaignId'])
       proof['reloads']+=1
     proof['status']='PASS';result['passed']+=1
    except Exception as exc:
     proof['error']=str(exc);result['failures'].append({'kind':kind,'error':str(exc)})
     try:await page.screenshot(path=str(OUT/('u25-forged-'+kind+'.png')),full_page=True)
     except Exception:pass
    result['cases'].append(proof)
   result['browserExecuted']=True
  except Exception as exc:result['failures'].append({'fatal':str(exc),'traceback':traceback.format_exc()})
  finally:await browser.close()
 result['status']='PASS' if result['passed']==2 and not result['failures'] else 'FAIL'
 (OUT/'u25-karg-forged-pending.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 print(json.dumps({'status':result['status'],'passed':result['passed'],'expected':2,'failures':result['failures']},ensure_ascii=False))
 return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
