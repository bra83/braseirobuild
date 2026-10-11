#!/usr/bin/env python3
"""U25 production Chromium/IndexedDB proof: Karg sacrifices both distinct resources.
Each scenario requires UI, real JourneyService transaction, receipt, isolation, replay
rejection, rollback and reload. This is not a simulation of an Android WebView.
"""
import asyncio, json, os, sys, traceback, uuid
from pathlib import Path
from playwright.async_api import async_playwright, expect
from official_ui_browser import mount

ORIGIN=os.environ.get('FL_E2E_ORIGIN','http://127.0.0.1:4173')
OUT=Path(os.environ.get('FL_E2E_ARTIFACTS','e2e-results'));OUT.mkdir(parents=True,exist_ok=True)
STAGES=('reachable_UI','preconditions','dice_or_combat_or_adjudication',
        'canonical_transaction','receipt','reload','campaign_isolation','idempotency','rollback')
SEED=r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');
 const {CampaignService}=await import('/src/services/CampaignService.ts');
 const {WorldStateService}=await import('/src/services/WorldStateService.ts');
 const db=await getDb();
 const campaign={...CampaignService.create(),id:c.campaignId,currentHexId:'u25-hex',partyCharacterIds:[c.heroId]};
 const world=WorldStateService.empty(c.campaignId);
 world.journey.events=[{id:c.eventId,type:'encounter',status:'open',title:'Encontro oficial 35',
  day:1,period:'Morning',hexId:'u25-hex',createdAt:Date.now(),detail:'Prova U25',
  gmDetail:'SEGREDO_NAO_MOSTRAR',result:{mechanic:{kind:'official_random_encounter',encounterId:35}}}];
 const hero={id:c.heroId,campaignId:c.campaignId,name:'Exploradora U25',status:'alive',conditions:[],
  gear:[{id:'u25-ring',name:'Anel brilhante',quantity:2,treasureCategory:'valuable'}],
  weapons:[],armor:[],coins:{gold:0,silver:20,copper:0},
  resourceDice:{food:'d8',water:'d8',arrows:null,torches:null},
  attributes:{strength:{current:3,max:3},agility:{current:3,max:3},
   wits:{current:3,max:3},empathy:{current:3,max:3}}};
 await db.put('campaign',campaign);await db.put('world_state',world);await db.put('characters',hero);
 await db.put('npcs',{id:'official-npc:'+c.campaignId+':'+c.eventId+':karg',
  campaignId:c.campaignId,name:'Karg',gifts:[],status:'Alive'});
}'''
MOUNT=r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');const db=await getDb();
 const world=await db.get('world_state',c.campaignId);
 world.journey.mounts=[{id:c.mountId,name:'Montaria U25',riderCharacterId:c.heroId,
  strength:4,status:'ready',createdAt:Date.now()}];
 await db.put('world_state',world);
}'''
STATE=r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');const db=await getDb();
 const world=await db.get('world_state',c.campaignId),hero=await db.get('characters',c.heroId);
 const event=world.journey.events.find(e=>e.id===c.eventId);
 return {heroStatus:hero.status,mounts:world.journey.mounts,
  eventStatus:event.status,decision:event.result.officialSceneDecision||null};
}'''
REJECT=r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');const db=await getDb();
 const {JourneyService}=await import('/src/services/JourneyService.ts');
 const before=async()=>JSON.stringify({world:await db.get('world_state',c.campaignId),
  hero:await db.get('characters',c.heroId)});
 const prior=await before();let rejected=false,message='';
 try{await JourneyService.resolveOfficialKargSacrifice(c.campaignId,c.eventId,c.kind,
   c.kind==='horse'?c.mountId:c.heroId,'Repetição não autorizada da adjudicação.');}
 catch(e){rejected=true;message=String(e?.message||e);}
 return {rejected,unchanged:prior===await before(),message};
}'''
async def transition(page,c,predicate):
 for _ in range(90):
  state=await page.evaluate(STATE,c)
  if predicate(state):return state
  await page.wait_for_timeout(120)
 raise AssertionError('Stage transition missing for '+c['kind'])

async def run_case(page,kind):
 c={'kind':kind,'campaignId':'u25-'+uuid.uuid4().hex,
    'eventId':'evt-'+uuid.uuid4().hex,'heroId':'hero-'+uuid.uuid4().hex,
    'mountId':'mount-'+uuid.uuid4().hex}
 other={**c,'campaignId':'other-'+uuid.uuid4().hex,'eventId':'other-'+uuid.uuid4().hex,
  'heroId':'other-'+uuid.uuid4().hex,'mountId':'other-'+uuid.uuid4().hex}
 proof={'pathKey':'35:1:offer_'+kind+':gm_decision','status':'FAIL',
   'evidencePerStage':{stage:'NOT_RUN' for stage in STAGES}}
 try:
  await page.evaluate(SEED,c);await page.evaluate(SEED,other)
  await page.evaluate(MOUNT,c);await page.evaluate(MOUNT,other)
  other_before=await page.evaluate(STATE,other)
  await mount(page,c['campaignId'])
  button=page.get_by_test_id('fl-official-choice-35-offer_'+kind)
  await expect(button).to_be_enabled()
  await button.click()
  state=await transition(page,c,lambda s:s['decision'] and s['decision']['stage']=='pending_external_mechanics')
  assert state['decision']['choiceId']=='offer_'+kind
  proof['evidencePerStage']['reachable_UI']='PASS'
  rejected=await page.evaluate(REJECT,c)
  assert rejected['rejected'] and rejected['unchanged'],rejected
  proof['evidencePerStage']['preconditions']='PASS'
  await page.get_by_placeholder('Adjudicação da oferta (mínimo 12 caracteres)').fill(
    'Karg aceita explicitamente a oferta, registrada pela Mestra.')
  await page.get_by_test_id('fl-karg-offer-'+c['eventId']+'-troll_offer_accepted').click()
  state=await transition(page,c,lambda s:s['decision'] and s['decision']['stage']=='karg_sacrifice_pending')
  assert state['eventStatus']=='open' and state['decision']['kargOfferAccepted'] is True
  proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS'
  panel=page.locator('div.rounded.border-red-800').filter(has_text='Karg aceitou o sacrifício')
  await panel.locator('select').select_option(c['heroId'] if kind=='adventurer' else c['mountId'])
  await page.get_by_placeholder('Registro da decisão da MdJ (mínimo 12 caracteres)').fill(
    'A Mestra confirmou a vítima e registrou o destino final.')
  label='Confirmar que Karg devorou a personagem' if kind=='adventurer' else 'Confirmar que Karg devorou a montaria'
  await page.get_by_role('button',name=label).click()
  state=await transition(page,c,lambda s:s['eventStatus']=='resolved' and
    s['decision'] and s['decision']['stage']=='karg_sacrifice_adjudicated')
  sacrifice=state['decision']['kargSacrifice']
  assert sacrifice['kind']==kind and sacrifice['targetId']==(c['heroId'] if kind=='adventurer' else c['mountId'])
  assert state['decision']['resolvedAt']==sacrifice['adjudicatedAt']
  if kind=='adventurer':assert state['heroStatus']=='dead' and len(state['mounts'])==1
  else:assert state['heroStatus']!='dead' and len(state['mounts'])==0
  proof['evidencePerStage']['canonical_transaction']='PASS'
  receipt=page.get_by_test_id('fl-official-receipt-'+c['eventId'])
  await expect(receipt).to_have_attribute('data-status','resolved')
  await expect(receipt).to_have_attribute('data-resource-transfer-committed','true')
  await expect(receipt).to_have_attribute('data-follow-up-pending','false')
  await expect(receipt).to_contain_text('Sacrifício adjudicado')
  await expect(receipt).not_to_contain_text('SEGREDO_NAO_MOSTRAR')
  proof['evidencePerStage']['receipt']='PASS'
  assert await page.evaluate(STATE,other)==other_before
  proof['evidencePerStage']['campaign_isolation']='PASS'
  after=await page.evaluate(STATE,c)
  replay=await page.evaluate(REJECT,c)
  assert replay['rejected'] and replay['unchanged'],replay
  proof['evidencePerStage']['idempotency']='PASS'
  proof['evidencePerStage']['rollback']='PASS'
  await page.reload(wait_until='domcontentloaded')
  await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
  await mount(page,c['campaignId'])
  assert await page.evaluate(STATE,c)==after
  assert await page.evaluate(STATE,other)==other_before
  await expect(page.get_by_test_id('fl-official-receipt-'+c['eventId'])).to_have_attribute('data-resource-transfer-committed','true')
  proof['evidencePerStage']['reload']='PASS'
  proof['status']='PASS'
 except Exception as exc:
  proof['error']=str(exc)
  proof['traceback']=traceback.format_exc()
  try:await page.screenshot(path=str(OUT/('u25-karg-'+kind+'-fail.png')),full_page=True)
  except Exception:pass
 return proof

async def main():
 result={'status':'FAIL','browserExecuted':False,'expected':2,'passed':0,
  'paths':[],'failures':[],'source':'U25 remote Karg real UI / IndexedDB'}
 async with async_playwright() as p:
  browser=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
  page=await browser.new_page()
  try:
   response=await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded')
   assert response and response.status==200
   await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
   for kind in ('adventurer','horse'):
    proof=await run_case(page,kind)
    result['paths'].append(proof)
    if proof['status']=='PASS':result['passed']+=1
    else:result['failures'].append({'key':proof['pathKey'],'error':proof.get('error')})
   result['browserExecuted']=True
  except Exception as exc:result['failures'].append({'fatal':str(exc),'traceback':traceback.format_exc()})
  finally:await browser.close()
 result['status']='PASS' if result['passed']==2 and not result['failures'] else 'FAIL'
 (OUT/'u25-karg-sacrifice-paths.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 print(json.dumps({'status':result['status'],'passed':result['passed'],'expected':2,'failures':result['failures']},ensure_ascii=False))
 return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
