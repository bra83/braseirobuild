#!/usr/bin/env python3
"""Two additional REAL downstream tests: #42 peaceful parley, #43 GM rescue.
Browser production JourneyPanel -> real IndexedDB -> reload. NO combat claims.
"""
import asyncio,json,os,sys,traceback,uuid
from pathlib import Path
from playwright.async_api import async_playwright,expect
from official_ui_browser import mount,SEED
ORIGIN=os.environ.get('FL_E2E_ORIGIN','http://127.0.0.1:4173')
OUT=Path(os.environ.get('FL_E2E_ARTIFACTS','e2e-results'));OUT.mkdir(parents=True,exist_ok=True)
STAGES=('reachable_UI','preconditions','dice_or_combat_or_adjudication','canonical_transaction','receipt','reload','campaign_isolation','idempotency','rollback')
KEYS={42:'42:1:parley:gm_decision',43:'43:1:intervene:gm_decision'}
STATE=r"""async c=>{
 const {getDb}=await import('/src/db/db.ts'),db=await getDb();
 const world=await db.get('world_state',c.campaignId),hero=await db.get('characters',c.heroId);
 const event=world.journey.events.find(e=>e.id===c.eventId);
 return {wholeWorld:world,hero,status:event.status,decision:event.result.officialSceneDecision||null};
}"""
REJECTED=r"""async c=>{
 const {getDb}=await import('/src/db/db.ts'),db=await getDb();
 const {JourneyService}=await import('/src/services/JourneyService.ts');
 const state=async()=>JSON.stringify({world:await db.get('world_state',c.campaignId),hero:await db.get('characters',c.heroId)});
 const before=await state();let rejected=false,message='';
 try{
  if(c.id===42)await JourneyService.resolveOfficialSaurianParley(c.campaignId,c.eventId,'peaceful','Conversaram pacificamente.');
  else await JourneyService.resolveOfficialBurialRescue(c.campaignId,c.eventId,'rescued','Família resgatada pela Mestra.');
 }catch(e){rejected=true;message=String(e?.message||e)}
 return {rejected,unchanged:before===await state(),message};
}"""
async def await_state(page,c,predicate):
 last=None
 for _ in range(100):
  last=await page.evaluate(STATE,c)
  if predicate(last):return last
  await page.wait_for_timeout(100)
 raise AssertionError('State transition absent #'+str(c['id'])+': '+str(last)[:1000])
async def case(page,id):
 c={'id':id,'campaignId':'u21-'+uuid.uuid4().hex,'eventId':'e-'+uuid.uuid4().hex,'heroId':'h-'+uuid.uuid4().hex}
 other={**c,'campaignId':'other-'+uuid.uuid4().hex,'eventId':'other-'+uuid.uuid4().hex,'heroId':'other-'+uuid.uuid4().hex}
 proof={'pathKey':KEYS[id],'status':'FAIL','evidencePerStage':{x:'NOT_RUN' for x in STAGES},'browser':'Chromium','persistence':'IndexedDB'}
 try:
  await page.evaluate(SEED,c);await page.evaluate(SEED,other)
  isolation_before=await page.evaluate(STATE,other)
  await mount(page,c['campaignId'])
  invalid=await page.evaluate(REJECTED,c)
  assert invalid['rejected'] and invalid['unchanged'],invalid
  choice='parley' if id==42 else 'intervene'
  button=page.get_by_test_id(f'fl-official-choice-{id}-{choice}')
  await expect(button).to_be_enabled()
  await button.click()
  pending='saurian_parley_pending' if id==42 else 'burial_rescue_pending'
  state=await await_state(page,c,lambda s:s['decision'] is not None and s['decision']['stage']==pending)
  assert state['status']=='open' and state['decision']['choiceId']==choice
  proof['evidencePerStage']['reachable_UI']='PASS'
  proof['evidencePerStage']['preconditions']='PASS'
  if id==42:
   await page.get_by_placeholder('Resultado adjudicado (mínimo 8 caracteres)').fill('Conversa pacífica confirmada pela Mestra.')
   await page.get_by_role('button',name='Encerrar conversa').click()
   stage='saurian_parley_resolved'
  else:
   await page.get_by_placeholder('Como ocorreu o resgate/desfecho (mínimo 8 caracteres)').fill('A Mestra registra que a família saiu viva.')
   await page.get_by_role('button',name='Registrar família resgatada').click()
   stage='family_rescued'
  state=await await_state(page,c,lambda s:s['status']=='resolved' and s['decision'] and s['decision']['stage']==stage)
  d=state['decision']
  assert len(d['gmAdjudication'])>=8 and d['pending'] is None,d
  if id==42:assert d['automaticRollsApplied'] is False
  else:assert d['familyFate']=='rescued' and d['rescueRollInvented'] is False
  proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS'
  proof['evidencePerStage']['canonical_transaction']='PASS'
  receipt=page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')
  await expect(receipt).to_have_attribute('data-status','resolved')
  await expect(receipt).to_have_attribute('data-committed','true')
  proof['evidencePerStage']['receipt']='PASS'
  assert await page.evaluate(STATE,other)==isolation_before
  proof['evidencePerStage']['campaign_isolation']='PASS'
  after=await page.evaluate(STATE,c)
  retry=await page.evaluate(REJECTED,c)
  assert retry['rejected'] and retry['unchanged'],retry
  proof['replayError']=retry['message']
  proof['evidencePerStage']['idempotency']='PASS'
  proof['evidencePerStage']['rollback']='PASS'
  await page.reload(wait_until='domcontentloaded')
  await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
  await mount(page,c['campaignId'])
  await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-status','resolved')
  assert await page.evaluate(STATE,c)==after
  assert await page.evaluate(STATE,other)==isolation_before
  proof['evidencePerStage']['reload']='PASS'
  proof['status']='PASS'
 except Exception as e:
  proof['error']=str(e);proof['traceback']=traceback.format_exc()
  try:await page.screenshot(path=str(OUT/f'u21-{id}-error.png'),full_page=True)
  except Exception:pass
 return proof
async def main():
 result={'status':'FAIL','expected':2,'passed':0,'browserExecuted':True,'paths':[],'failures':[]}
 async with async_playwright() as p:
  browser=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
  page=await browser.new_page()
  try:
   res=await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded')
   assert res and res.status==200
   await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
   for id in (42,43):
    proof=await case(page,id);result['paths'].append(proof)
    if proof['status']=='PASS':result['passed']+=1
    else:result['failures'].append({'pathKey':proof['pathKey'],'error':proof.get('error')})
  except Exception as e:result['failures'].append({'fatal':str(e),'traceback':traceback.format_exc()})
  finally:await browser.close()
 result['status']='PASS' if result['passed']==2 and not result['failures'] else 'FAIL'
 (OUT/'u21-parley-rescue.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
 print(json.dumps({'status':result['status'],'passed':result['passed'],'expected':2,'failures':result['failures']},ensure_ascii=False))
 return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
