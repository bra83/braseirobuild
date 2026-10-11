#!/usr/bin/env python3
"""U20 genuine downstream path proof: initial UI -> adjudication/trade -> native
JourneyService -> IndexedDB -> reload -> negative replay rollback -> campaign isolation.
This is only TWO of the 82 official downstream paths. No inferred certification.
"""
import asyncio
import json
import os
import sys
import traceback
import uuid
from pathlib import Path

from playwright.async_api import async_playwright, expect
from official_ui_browser import mount

ORIGIN = os.environ.get('FL_E2E_ORIGIN', 'http://127.0.0.1:4173')
OUT = Path(os.environ.get('FL_E2E_ARTIFACTS', 'e2e-results'))
OUT.mkdir(parents=True, exist_ok=True)
STAGES = ('reachable_UI', 'preconditions', 'dice_or_combat_or_adjudication',
          'canonical_transaction', 'receipt', 'reload', 'campaign_isolation',
          'idempotency', 'rollback')
PATHS = {35: '35:1:offer_gift:gm_decision', 41: '41:1:board:follow_up'}

SEED = r'''async ({id,campaignId,eventId,heroId})=>{
 const {getDb}=await import('/src/db/db.ts');
 const {CampaignService}=await import('/src/services/CampaignService.ts');
 const {WorldStateService}=await import('/src/services/WorldStateService.ts');
 const db=await getDb();
 const campaign={...CampaignService.create(),id:campaignId,currentHexId:'u20-hex',partyCharacterIds:[heroId]};
 const world=WorldStateService.empty(campaignId);
 world.journey.events=[{id:eventId,type:'encounter',status:'open',title:`Encontro oficial ${id}`,
  day:1,period:'Morning',hexId:'u20-hex',createdAt:Date.now(),detail:'Prova real U20',
  gmDetail:'SEGREDO_MESTRE_NAO_EXIBIR',result:{mechanic:{kind:'official_random_encounter',encounterId:id}}}];
 const hero={id:heroId,campaignId,name:'Exploradora U20',status:'alive',conditions:[],
  gear:[{id:'u20-ring',name:'Anel brilhante',quantity:2,treasureCategory:'valuable'}],
  weapons:[],armor:[],coins:{gold:0,silver:20,copper:0},
  resourceDice:{food:'d8',water:'d8',arrows:null,torches:null},
  attributes:{strength:{current:3,max:3},agility:{current:3,max:3},
   wits:{current:3,max:3},empathy:{current:3,max:3}}};
 await db.put('campaign',campaign);await db.put('world_state',world);await db.put('characters',hero);
 if(id===35){await db.put('npcs',{id:`official-npc:${campaignId}:${eventId}:karg`,
  campaignId,name:'Karg',gifts:[],status:'Alive'});}
}'''

STATE = r'''async({id,campaignId,eventId,heroId})=>{
 const {getDb}=await import('/src/db/db.ts');const db=await getDb();
 const character=await db.get('characters',heroId);
 const world=await db.get('world_state',campaignId);
 const event=world?.journey?.events?.find(e=>e.id===eventId);
 const karg=id===35?await db.get('npcs',`official-npc:${campaignId}:${eventId}:karg`):null;
 return {coins:character?.coins,gear:character?.gear,eventStatus:event?.status,
  decision:event?.result?.officialSceneDecision,npcGifts:karg?.gifts||[],
  npcActivity:karg?.currentActivity||null};
}'''

# Call the REAL service to prove an already-committed operation is rejected and
# the complete persisted transaction is unchanged. This cannot be satisfied by
# simply hiding a button or by a synthetic in-memory store.
REPLAY = r'''async c=>{
 const {getDb}=await import('/src/db/db.ts');
 const {JourneyService}=await import('/src/services/JourneyService.ts');
 const db=await getDb();
 const dump=async()=>{
  const world=await db.get('world_state',c.campaignId);
  const hero=await db.get('characters',c.heroId);
  const npc=c.id===35?await db.get('npcs',`official-npc:${c.campaignId}:${c.eventId}:karg`):null;
  return JSON.stringify({world,hero,npc});
 };
 const before=await dump();let rejected=false;let message='';
 try {
  if(c.id===35)await JourneyService.offerOfficialKargGift(c.campaignId,c.eventId,c.heroId,'gear',0,
    'Repetição deliberada: presente já foi transferido.');
  else await JourneyService.buyOfficialSoulBottle(c.campaignId,c.eventId,c.heroId);
 }catch(e){rejected=true;message=String(e?.message||e);}
 const after=await dump();
 return {rejected,unchanged:before===after,message};
}'''

async def wait_state(page, case, predicate):
    for _ in range(100):
        state = await page.evaluate(STATE, case)
        if predicate(state):
            return state
        await page.wait_for_timeout(100)
    raise AssertionError(f'State transition missing: {case["id"]}; last={state}')

async def test_path(page, encounter):
    c={'id':encounter,'campaignId':'u20-'+uuid.uuid4().hex,
       'eventId':'u20-'+uuid.uuid4().hex,'heroId':'u20-'+uuid.uuid4().hex}
    other={**c,'campaignId':'u20-other-'+uuid.uuid4().hex,
           'eventId':'u20-other-'+uuid.uuid4().hex,'heroId':'u20-other-'+uuid.uuid4().hex}
    proof={'pathKey':PATHS[encounter],'encounterId':encounter,'status':'FAIL',
           'evidencePerStage':{name:'NOT_RUN' for name in STAGES},
           'browser':'Chromium','persistence':'IndexedDB',
           'testImplementation':'production JourneyPanel + JourneyService'}
    try:
        # An unrelated campaign is held constant throughout all writes and reloads.
        await page.evaluate(SEED,c)
        await page.evaluate(SEED,other)
        isolated_before=await page.evaluate(STATE,other)
        await mount(page,c['campaignId'])
        choice='offer_gift' if encounter==35 else 'board'
        button=page.get_by_test_id(f'fl-official-choice-{encounter}-{choice}')
        await expect(button).to_be_enabled()
        await button.click()
        await wait_state(page,c,lambda x:x['decision'] and x['decision']['choiceId']==choice)
        proof['evidencePerStage']['reachable_UI']='PASS'
        if encounter==35:
            stage=await wait_state(page,c,lambda x:x['decision']['stage']=='pending_external_mechanics')
            assert stage['gear'][0]['quantity']==2 and not stage['npcGifts']
            # No transfer before the GM accepts Karg's offer.
            blocked=await page.evaluate(REPLAY,c)
            assert blocked['rejected'] and blocked['unchanged'],blocked
            proof['evidencePerStage']['preconditions']='PASS'
            await page.get_by_placeholder('Adjudicação da oferta (mínimo 12 caracteres)').fill(
                'Karg aceita este objeto brilhante como presente da expedição.')
            await page.get_by_test_id(f'fl-karg-offer-{c["eventId"]}-troll_offer_accepted').click()
            await wait_state(page,c,lambda x:x['decision']['stage']=='karg_offer_accepted')
            proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS'
            await page.get_by_placeholder('Adjudicação do presente aceito (mínimo 12 caracteres)').fill(
                'A Mestra adjudicou a aceitação do anel brilhante por Karg.')
            await page.get_by_test_id(f'fl-karg-gift-{c["eventId"]}-{c["heroId"]}-gear-0').click()
            state=await wait_state(page,c,lambda x:x['eventStatus']=='resolved' and
                  x['decision']['stage']=='karg_gift_transferred')
            assert state['gear'][0]['quantity']==1 and len(state['npcGifts'])==1, state
            assert state['decision']['kargGiftTransferRequired'] is False
            assert state['npcGifts'][0]['name']=='Anel brilhante'
        else:
            await wait_state(page,c,lambda x:x['decision']['stage']=='soul_merchant_contact')
            # Stock is not supplied by the rulebook: explicit GM adjudication is required.
            input_stock=page.get_by_text('Estoque adjudicado').locator('input')
            await input_stock.fill('1')
            await page.get_by_role('button',name='Registrar estoque da cena').click()
            state=await wait_state(page,c,lambda x:x['decision']['stage']=='soul_merchant_stock_set')
            assert state['decision']['soulBottleStock']==1
            proof['evidencePerStage']['preconditions']='PASS'
            proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS'
            await page.get_by_test_id(f'fl-soul-buy-{c["eventId"]}-{c["heroId"]}').click()
            state=await wait_state(page,c,lambda x:x['decision']['soulBottleStock']==0)
            assert state['eventStatus']=='open' and len([g for g in state['gear'] if g.get('itemKind')=='official_soul_bottle'])==1
            assert 100*state['coins']['gold']+10*state['coins']['silver']+state['coins']['copper']==100
        proof['evidencePerStage']['canonical_transaction']='PASS'
        receipt=page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')
        await expect(receipt).to_have_attribute('data-resource-transfer-committed','true')
        await expect(receipt).not_to_contain_text('SEGREDO_MESTRE_NAO_EXIBIR')
        proof['evidencePerStage']['receipt']='PASS'
        after_first=await page.evaluate(STATE,c)
        after_other=await page.evaluate(STATE,other)
        assert after_other==isolated_before, 'Cross-campaign mutation detected'
        proof['evidencePerStage']['campaign_isolation']='PASS'
        # Repeat the real service transaction, not merely checking UI disablement.
        replay=await page.evaluate(REPLAY,c)
        assert replay['rejected'] and replay['unchanged'],replay
        proof['replayError']=replay['message']
        proof['evidencePerStage']['idempotency']='PASS'
        proof['evidencePerStage']['rollback']='PASS'
        await page.reload(wait_until='domcontentloaded')
        await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
        await mount(page,c['campaignId'])
        await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-resource-transfer-committed','true')
        assert await page.evaluate(STATE,c)==after_first,'State changed across reload'
        assert await page.evaluate(STATE,other)==isolated_before,'Other campaign changed across reload'
        proof['evidencePerStage']['reload']='PASS'
        proof['status']='PASS'
    except Exception as exc:
        proof['error']=str(exc)
        proof['traceback']=traceback.format_exc()
        try: await page.screenshot(path=str(OUT/f'u20-{encounter}-failure.png'),full_page=True)
        except Exception:pass
    return proof

async def main():
    result={'status':'FAIL','expected':2,'passed':0,'browserExecuted':True,
            'paths':[],'failures':[],'notCoveredDownstreamPaths':80}
    async with async_playwright() as p:
        browser=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
        page=await browser.new_page()
        try:
            response=await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded')
            if not response or response.status!=200:raise AssertionError('No Vite harness HTTP 200')
            await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
            for encounter in (35,41):
                proof=await test_path(page,encounter)
                result['paths'].append(proof)
                if proof['status']=='PASS':result['passed']+=1
                else:result['failures'].append({'pathKey':proof['pathKey'],'error':proof.get('error')})
        except Exception as exc:result['failures'].append({'fatal':str(exc),'traceback':traceback.format_exc()})
        finally:await browser.close()
    result['status']='PASS' if result['passed']==2 and not result['failures'] else 'FAIL'
    (OUT/'u20-downstream-paths.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps({'status':result['status'],'passed':result['passed'],'expected':2,
                      'failures':result['failures']},ensure_ascii=False))
    return 0 if result['status']=='PASS' else 1

if __name__=='__main__':sys.exit(asyncio.run(main()))
