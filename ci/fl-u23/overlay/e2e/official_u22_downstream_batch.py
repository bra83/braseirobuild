#!/usr/bin/env python3
"""Real production UI/IndexedDB downstream cases. Partial coverage is explicit.
Each recipe follows its source-defined effects to a terminal outcome. No DB
mutation after setup, no synthetic combat receipts, no skipped paths counted.
"""
import asyncio,json,os,sys,traceback,uuid
from pathlib import Path
from playwright.async_api import async_playwright,expect
from official_ui_browser import mount,SEED
ORIGIN=os.environ.get('FL_E2E_ORIGIN','http://127.0.0.1:4173')
OUT=Path(os.environ.get('FL_E2E_ARTIFACTS','e2e-results'));OUT.mkdir(parents=True,exist_ok=True)
STAGES=('reachable_UI','preconditions','dice_or_combat_or_adjudication','canonical_transaction','receipt','reload','campaign_isolation','idempotency','rollback')
NOTE='A Mestra confirmou o desfecho desta cena conforme a fonte oficial.'
def op(method,args,button,placeholder=None):return dict(method=method,args=args,button=button,placeholder=placeholder)
def gm(outcome,button,placeholder='Adjudicação (mínimo 12 caracteres)'):
 return op('adjudicateOfficialEncounterFollowUp',[outcome,NOTE],button,placeholder)
CASES=[]
def add(id,choice,kind,operations,stage,visit=1,assertion=None):
 CASES.append(dict(id=id,choice=choice,key=f'{id}:{visit}:{choice}:{kind}',visit=visit,operations=operations,stage=stage,assertion=assertion))
add(3,'parley','gm_decision',[op('resolveOfficialOrcParley',['release_sargah',NOTE],'Mestra: escolta liberta Sargah','Motivo da decisão da escolta (mínimo 12 caracteres)')],'orc_parley_resolved',assertion='sargah')
for visit in (1,2):add(18,'bake_dough','gm_decision',[op('adjudicateOfficialDoughCorruption',[NOTE],'Registrar adjudicação da corrupção','Adjudicação da Mestra (12–800 caracteres)')],'dough_corruption_adjudicated',visit)
add(19,'linger','gm_decision',[op('advanceOfficialAnimalSanctuary',['approach'],'Permitir que mais animais se aproximem'),op('advanceOfficialAnimalSanctuary',['wait_for_leader'],'Esperar a chegada da Alce Negra'),op('advanceOfficialAnimalSanctuary',['leave_sanctuary'],'Deixar o santuário')],'sanctuary_left')
add(19,'attack','gm_decision',[op('advanceOfficialAnimalSanctuary',['attack_animals'],'Registrar que os animais recuaram sem dano'),op('advanceOfficialAnimalSanctuary',['wait_for_leader'],'Esperar a chegada da Alce Negra'),op('advanceOfficialAnimalSanctuary',['leave_sanctuary'],'Deixar o santuário')],'sanctuary_left')
for choice in ('respect','watch','interfere'):
 add(20,choice,'gm_decision',[op('advanceOfficialFuneralProcession',['leave'],'Deixar a procissão em paz' if choice=='interfere' else 'Prosseguir sem presente')],'procession_departed')
add(21,'speak','gm_decision',[gm('conversation_succeeded','A conversa funcionou','Adjudicação da cena (mínimo 12 caracteres)')],'resolved')
add(21,'withdraw','gm_decision',[gm('withdrawal_succeeded','O grupo conseguiu se afastar','Adjudicação da cena (mínimo 12 caracteres)')],'resolved')
add(21,'observe_dagger','gm_decision',[gm('dagger_spotted','A adaga foi notada','Adjudicação da observação (mínimo 12 caracteres)'),op('advanceOfficialVengefulGhostScene',['speak'],'Falar da adaga e da morte'),gm('conversation_succeeded','A conversa funcionou','Adjudicação da cena (mínimo 12 caracteres)')],'resolved')
add(22,'hear_request','gm_decision',[gm('decline','Recusar o pedido')],'glassmaker_request_declined')
add(22,'collect_mist','gm_decision',[gm('collection_succeeded','Registrar coleta'),op('advanceOfficialGlassmakerTimeline',['confirm_d6_hours_elapsed',NOTE],'Confirmar D6 horas transcorridas','Confirmação da passagem do tempo (mínimo 12 caracteres)'),op('advanceOfficialGlassmakerTimeline',['confirm_d6_weeks_elapsed',NOTE],'Confirmar D6 semanas transcorridas','Confirmação da passagem do tempo (mínimo 12 caracteres)')],'blood_mist_dispersed',assertion='glass')
add(23,'talk_troll','gm_decision',[gm('troll_agrees','Ghormald aceita a conversa')],'troll_negotiation_resolved')
add(24,'free_oker','gm_decision',[op('resolveOfficialCursedOgrePrisoner',['free_oker'],'Libertar Oker')],'oker_freed')
add(27,'investigate','gm_decision',[gm('linn_clearing_investigated','Registrar investigação concluída','Adjudicação da investigação (mínimo 12 caracteres)')],'linn_clearing_investigated')
add(27,'comfort','follow_up',[op('resolveOfficialLinnShelter',['remain',NOTE],'Mestra: Linn permanece','Justificativa da decisão de Linn (12–600 caracteres)')],'linn_shelter_recorded',assertion='linn')
for choice in ('search','track'):
 ops=([gm('tracks_found','Encontrar pegadas recentes','Resultado da busca/rastreamento (mínimo 12 caracteres)')] if choice=='search' else [])+[gm('captors_tracked','Alcançar os captores e descobrir a mina de prata','Resultado da busca/rastreamento (mínimo 12 caracteres)')]
 add(28,choice,'gm_decision' if choice=='search' else 'item_or_skill',ops,'silver_mine_captors_found')
add(32,'leave_baby','follow_up',[gm('baby_cry_quiet','Registrar que o choro cessou','Adjudicação da passagem de tempo (mínimo 12 caracteres)')],'rat_baby_left_unrescued')
add(32,'visit_belinda','follow_up',[gm('belinda_pressed','Pressionar; Belinda conta a verdade','Como a MdJ conduziu a conversa (mínimo 12 caracteres)')],'belinda_confessed')
for choice in ('question_captain','help_escort'):add(34,choice,'gm_decision',[gm('holmar_released_troupe','Holmar libera a trupe','Adjudicação da interação/confronto (mínimo 12 caracteres)')],'iron_clown_holmar_released_troupe')
add(35,'offer_goblin','gm_decision',[gm('troll_offer_accepted','Karg aceita a oferta','Adjudicação da oferta (mínimo 12 caracteres)')],'karg_offer_accepted')
for choice,target,label in [('offer_adventurer','adventurer','Confirmar que Karg devorou a personagem'),('offer_horse','horse','Confirmar que Karg devorou a montaria')]:
 add(35,choice,'gm_decision',[gm('troll_offer_accepted','Karg aceita a oferta','Adjudicação da oferta (mínimo 12 caracteres)'),{**op('resolveOfficialKargSacrifice',[target,'$hero' if target=='adventurer' else '$mount',NOTE],label,'Registro da decisão da MdJ (mínimo 12 caracteres)'), 'select':target}],'karg_sacrifice_adjudicated',assertion=target)
add(37,'pay_toll','follow_up',[op('payOfficialRoadBlockadeToll',[],'Pagar 3 pratas por aventureira')],'dwarf_toll_paid',assertion='toll')
add(38,'contact','follow_up',[op('resolveOfficialEncounterDiseaseExposure',['$hero','blue_plague_contact'],'Exploradora de teste: registrar contato e rolar Resiliência'),op('leaveOfficialBluePlagueScene',[],'Deixar Linnaya e prosseguir')],'blue_plague_scene_left',assertion='disease')
add(39,'board','follow_up',[op('resolveOfficialMerchantShipContact',['stories',NOTE],'Registrar troca de histórias','História, troca ou jantar adjudicado (mínimo 12 caracteres)'),op('resolveOfficialMerchantShipContact',['depart'],'Retomar viagem e embarcar o clandestino')],'stowaway_hidden_aboard',assertion='stowaway')

for id,choice,outcome,label in [
 (14,'consider_joining','invitation_declined_after_discussion','Recusar convite após conversar'),
 (17,'attend','celebration_completed_without_fight','Concluir celebração sem briga'),
 (17,'prevent_fight','wedding_fight_prevented','Registrar briga impedida'),
 (18,'tea','tea_visit_completed','Concluir visita após o chá'),
 (18,'inspect','dough_origin_explained','Registrar explicação de Melkar')]:
 add(id,choice,'gm_decision',[gm(outcome,label,'Desfecho da interação confirmado pela Mestra (12–600 caracteres)')],outcome+'_adjudicated')

for choice in ('seek_escape','endure'):
 add(1,choice,'item_or_skill',[op('startOfficialBloodMist',[],'Iniciar Névoa de Sangue · testar vigia'),{**op('attemptOfficialBloodMistEscape',['$hero'],'Rolar fuga e Discernimento'),'repeatUntil':'blood_mist_resolved'}],'blood_mist_resolved',assertion='mist')
add(6,'calm','item_or_skill',[op('calmOfficialHorse',['$hero'],'Exploradora de teste · Adestramento'),op('finishOfficialHorse',['leave'],'Deixar o cavalo')],'horse_disposition_resolved',assertion='horse_test')
add(12,'offer_camp','item_or_skill',[{**op('offerOfficialHungryBanditsCamp',['$hero','gear','gear:0',1,NOTE],'','História contada aos assaltantes (mínimo 12 caracteres)'),'testid':'fl-bandit-offer-$event-$hero-gear-gear:0'},op('resolveOfficialHungryBanditsReaction',['accept'],'Mestra: Argam aceita')],'bandit_parley_resolved',assertion='food')
add(14,'inspect_symbol','item_or_skill',[op('inspectOfficialPenitentSymbol',['$hero'],'Exploradora de teste observa o símbolo'),{**op('leaveOfficialPenitentSymbolView',[],'Deixar o símbolo fora de vista'),'ifStage':'penitent_illusion_active'}],['penitent_symbol_resisted','penitent_symbol_out_of_view'],assertion='symbol')
add(26,'escape_fire','item_or_skill',[{**op('resolveOfficialWildfireEscapeAttempt',['$hero'],'Exploradora de teste · resolver fuga'),'repeatUntil':'wildfire_escape_resolved','prefix':True}],'wildfire_escape_resolved',assertion='fire')
for choice in ('talk','seek_cure'):
 add(36,choice,'follow_up',[op('openOfficialHairlessLupineCave',[],'Explorar a caverna subterrânea próxima'),op('resolveOfficialEncounterDiseaseExposure',['$hero','hairless_crystal',1],'Exploradora de teste: rolar Resiliência, rodada 1'),op('leaveOfficialHairlessLupineCave',[],'Deixar a caverna')],'hairless_cave_left',assertion='crystal')

EXTRA=r'''async c=>{const{getDb}=await import('/src/db/db.ts');const db=await getDb();
 const h=await db.get('characters',c.heroId);await db.put('characters',{...h,coins:{gold:0,silver:20,copper:0},gear:[{id:'rations',name:'Rações',quantity:3}],skills:{Movement:3,Insight:3,Survival:3,'Animal Handling':3}});
 const w=await db.get('world_state',c.campaignId);const e=w.journey.events[0];
 e.gmDetail='SEGREDO_MESTRE_NAO_EXIBIR';e.result.officialEncounterVisitNumber=c.visit;
 w.journey.mounts=[{id:c.mountId,name:'Montaria da prova',strength:6,status:'ready',riderCharacterId:c.heroId,createdAt:Date.now()}];await db.put('world_state',w);
}'''
STATE=r'''async c=>{const{getDb}=await import('/src/db/db.ts');const db=await getDb();
 const all=async(name)=>{const rows=await db.getAll(name);return rows.filter(v=>v.campaignId===c.campaignId||v.id===c.campaignId||v.sceneId===c.eventId||v.sceneId===`official-encounter:${c.eventId}`||v.characterId===c.heroId).sort((a,b)=>String(a.id).localeCompare(String(b.id)));};
 const w=await db.get('world_state',c.campaignId);return {campaign:await db.get('campaign',c.campaignId),world:w,hero:await db.get('characters',c.heroId),npcs:await all('npcs'),rolls:await all('rolls'),event:w.journey.events.find(v=>v.id===c.eventId)};
}'''
CALL=r'''async c=>{const{JourneyService:J}=await import('/src/services/JourneyService.ts');
 const args=c.operation.args.map(v=>v==='$hero'?c.heroId:v==='$mount'?c.mountId:v);
 try{await J[c.operation.method](c.campaignId,c.eventId,...args);return{rejected:false};}catch(e){return{rejected:true,message:String(e?.message||e)};}}'''
async def state(page,c):return await page.evaluate(STATE,c)
async def rejected_unchanged(page,c,operation):
 before=await state(page,c);r=await page.evaluate(CALL,{**c,'operation':operation});after=await state(page,c)
 assert r['rejected'] and before==after,{'operation':operation,'result':r,'stateUnchanged':before==after}
 return r
async def changed(page,c,before):
 for _ in range(100):
  current=await state(page,c)
  if current!=before:return current
  error=await page.evaluate('window.__flUiError')
  if error:raise AssertionError(error)
  await page.wait_for_timeout(50)
 raise AssertionError('No canonical state change')
async def test_path(page,recipe):
 c={**recipe,'campaignId':'u22-'+uuid.uuid4().hex,'eventId':'e-'+uuid.uuid4().hex,'heroId':'h-'+uuid.uuid4().hex,'mountId':'m-'+uuid.uuid4().hex}
 other={**c,'campaignId':'other-'+uuid.uuid4().hex,'eventId':'other-'+uuid.uuid4().hex,'heroId':'other-'+uuid.uuid4().hex,'mountId':'other-'+uuid.uuid4().hex}
 proof={'pathKey':c['key'],'status':'FAIL','evidencePerStage':{x:'NOT_RUN' for x in STAGES},'browser':'Chromium','persistence':'IndexedDB','operationReceipts':[],'visit':c['visit']}
 try:
  for fixture in (c,other):await page.evaluate(SEED,fixture);await page.evaluate(EXTRA,fixture)
  isolation=await state(page,other)
  await mount(page,c['campaignId'])
  await rejected_unchanged(page,c,c['operations'][-1])
  button=page.get_by_test_id(f'fl-official-choice-{c["id"]}-{c["choice"]}')
  await expect(button).to_be_enabled();before=await state(page,c);await button.click();initial=await changed(page,c,before)
  assert initial['event']['status']=='open' and initial['event']['result']['officialSceneDecision']['choiceId']==c['choice']
  proof['evidencePerStage']['reachable_UI']='PASS';proof['evidencePerStage']['preconditions']='PASS'
  for operation in c['operations']:
   current=await state(page,c)
   if operation.get('ifStage') and current['event']['result']['officialSceneDecision']['stage']!=operation['ifStage']:continue
   if operation.get('placeholder'):await page.get_by_placeholder(operation['placeholder'],exact=True).fill(NOTE)
   if operation.get('select'):
    target=c['heroId'] if operation['select']=='adventurer' else c['mountId']
    await page.get_by_test_id(f'fl-karg-sacrifice-{c["eventId"]}').select_option(target)
   for attempt in range(12 if operation.get('repeatUntil') else 1):
    if operation.get('testid'):
     selector=operation['testid'].replace('$event',c['eventId']).replace('$hero',c['heroId']);button=page.get_by_test_id(selector)
    elif operation.get('prefix'):
     import re
     button=page.get_by_role('button',name=re.compile('^'+re.escape(operation['button'])))
    else:button=page.get_by_role('button',name=operation['button'],exact=True)
    await expect(button).to_be_enabled();before=await state(page,c);await button.click();current=await changed(page,c,before)
    proof['operationReceipts'].append({'method':operation['method'],'stage':current['event']['result']['officialSceneDecision']['stage']})
    if not operation.get('repeatUntil') or current['event']['result']['officialSceneDecision']['stage']==operation['repeatUntil']:break
  final=await state(page,c);event=final['event'];d=event['result']['officialSceneDecision']
  assert event['status']=='resolved' and d['stage'] in (c['stage'] if isinstance(c['stage'],list) else [c['stage']]),{'expectedStage':c['stage'],'event':event}
  assert d.get('pending') is None,{'unclosedPending':d}
  assertion=c['assertion']
  if assertion=='sargah':assert any(n['name']=='Sargah' and n['status']=='Alive' for n in final['npcs'])
  if assertion=='linn':assert any(n['name']=='Linn' and n['officialFollowParty'] is False for n in final['npcs'])
  if assertion=='glass':assert d['wholeHexCovered'] is False and 1<=d['glassShatterHoursD6']<=6 and 1<=d['bloodMistDispersalWeeksD6']<=6
  if assertion=='adventurer':assert final['hero']['status']=='dead' and d['kargSacrifice']['targetId']==c['heroId']
  if assertion=='horse':assert not final['world']['journey']['mounts'] and d['kargSacrifice']['targetId']==c['mountId']
  if assertion=='toll':
   coins=final['hero']['coins'];assert 100*coins['gold']+10*coins['silver']+coins['copper']==170 and d['tollSilverTotal']==3
  if assertion=='disease':assert c['heroId'] in d['bluePlagueContactCharacterIds'] and len(final['rolls'])==2
  if assertion=='stowaway':assert any(n['name']=='Meroggha' and n['hiddenFromParty'] is True for n in final['npcs'])
  if assertion=='mist':assert d['mistEscapedIds']==[c['heroId']] and len(final['rolls'])>=1
  if assertion=='horse_test':assert d['horseRollId'] and d['horseDisposition']=='leave' and len(final['rolls'])==1
  if assertion=='food':assert final['hero']['gear'][0]['quantity']==2 and d['banditReaction']=='accept' and any(n['name']=='Argam Duasbarbas' for n in final['npcs'])
  if assertion=='symbol':assert d['penitentSymbolRollId'] and not any(e.get('id','').startswith('official-penitent-illusion:') for e in final['hero'].get('statusEffects',[]))
  if assertion=='fire':assert d['fireEscapeProgress'][c['heroId']]['status'] in ('escaped','broken') and len(final['rolls'])>=1
  if assertion=='crystal':assert d['crystalExposureChecks'][0]['characterId']==c['heroId'] and d['cureClaimed'] is False and len(final['rolls'])==2
  proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS';proof['evidencePerStage']['canonical_transaction']='PASS'
  receipt=page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')
  await expect(receipt).to_have_attribute('data-status','resolved');await expect(receipt).to_have_attribute('data-committed','true')
  await expect(receipt).not_to_contain_text('SEGREDO_MESTRE_NAO_EXIBIR')
  proof['evidencePerStage']['receipt']='PASS'
  assert await state(page,other)==isolation;proof['evidencePerStage']['campaign_isolation']='PASS'
  retry=await rejected_unchanged(page,c,c['operations'][-1]);proof['replayError']=retry['message']
  proof['evidencePerStage']['idempotency']='PASS';proof['evidencePerStage']['rollback']='PASS'
  await page.reload(wait_until='domcontentloaded');await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000);await mount(page,c['campaignId'])
  await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-status','resolved')
  assert await state(page,c)==final and await state(page,other)==isolation
  proof['evidencePerStage']['reload']='PASS';proof['status']='PASS'
 except Exception as e:
  proof['error']=str(e);proof['traceback']=traceback.format_exc()
  try:await page.screenshot(path=str(OUT/f'u22-{c["id"]}-{c["choice"]}-{c["visit"]}.png'),full_page=True)
  except Exception:pass
 print(json.dumps({'path':proof['pathKey'],'status':proof['status'],'error':proof.get('error')},ensure_ascii=False),flush=True)
 return proof
async def main():
 keys=[c['key'] for c in CASES];assert len(keys)==len(set(keys))
 source=json.loads((Path(__file__).resolve().parents[1]/'reports/U19_OFFICIAL_MATRIX_43.json').read_text())
 assert set(keys)<={p['key'] for p in source['consequencePaths']},'Recipe key not in official registry'
 result={'status':'FAIL','expected':len(CASES),'passed':0,'browserExecuted':True,'paths':[],'failures':[]}
 async with async_playwright() as p:
  browser=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage']);page=await browser.new_page();page.set_default_timeout(10000)
  try:
   res=await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded');assert res and res.status==200
   await page.wait_for_function('typeof window.__flUiMount === "function"',timeout=30000)
   for recipe in CASES:
    proof=await test_path(page,recipe);result['paths'].append(proof)
    if proof['status']=='PASS':result['passed']+=1
    else:result['failures'].append({'pathKey':proof['pathKey'],'error':proof.get('error')})
  except Exception as e:result['failures'].append({'fatal':str(e),'traceback':traceback.format_exc()})
  finally:await browser.close()
 result['status']='PASS' if result['passed']==len(CASES) and not result['failures'] else 'FAIL'
 (OUT/'u22-downstream-batch.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:v for k,v in result.items() if k!='paths'},ensure_ascii=False))
 return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
