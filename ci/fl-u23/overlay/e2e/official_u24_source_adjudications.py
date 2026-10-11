#!/usr/bin/env python3
"""Source gaps use explicit, bounded GM decisions with concrete state effects.
No invented horse handling roll, no instant leg healing, no duplicate loot.
"""
import asyncio,json,sys,uuid,traceback
from playwright.async_api import async_playwright,expect
from official_ui_browser import mount,SEED
from official_u23_downstream_combat import state,reject,click,prove_atomic_rollback,OUT,ORIGIN,STAGES,NOTE
SETUP=r'''async c=>{const{getDb}=await import('/src/db/db.ts');const db=await getDb();const w=await db.get('world_state',c.campaignId);
 w.campaignPois=[{id:c.castleId,campaignId:c.campaignId,mapId:'test-map',hexId:'ui-hex',name:'Castelo já descoberto',type:'castle',tags:['castle'],description:'POI pré-existente da campanha',layer:'USER',canonical:false,discovered:true,hidden:false,status:'active',createdBy:'gm',developmentLevel:1,createdAt:Date.now(),updatedAt:Date.now()}];
 await db.put('world_state',w);await db.put('individual_hexes',{id:'ui-hex',mapId:'test-map',terrain:'plains',coordinate:'ui-hex'});
 const h=await db.get('characters',c.heroId);await db.put('characters',{...h,coins:{gold:0,silver:20,copper:0},skills:{Crafting:3,Healing:3}});
}'''
async def test(page,id,choice,kind):
 c=dict(id=id,choice=choice,visit=1,key=f'{id}:1:{choice}:{kind}',campaignId='u24-'+uuid.uuid4().hex,eventId='event-'+uuid.uuid4().hex,heroId='hero-'+uuid.uuid4().hex,castleId='castle-'+uuid.uuid4().hex)
 other={**c,'campaignId':'other-'+uuid.uuid4().hex,'eventId':'other-'+uuid.uuid4().hex,'heroId':'other-'+uuid.uuid4().hex,'castleId':'other-'+uuid.uuid4().hex}
 proof=dict(pathKey=c['key'],status='FAIL',evidencePerStage={v:'NOT_RUN' for v in STAGES},browser='Chromium',persistence='IndexedDB')
 try:
  for f in (c,other):await page.evaluate(SEED,f);await page.evaluate(SETUP,f)
  isolation=await state(page,other)
  method,args=('recordOfficialHorseCapture',[c['heroId'],NOTE]) if id==6 else ('recordOfficialBalloonAssistance',[NOTE,NOTE]) if choice=='help' else ('recordOfficialBalloonPilotResponse',[NOTE])
  await reject(page,c,method,args);await mount(page,c['campaignId']);await page.get_by_test_id(f'fl-official-choice-{id}-{choice}').click()
  proof['evidencePerStage']['reachable_UI']='PASS';proof['evidencePerStage']['preconditions']='PASS'
  if id==6:
   await click(page,c,'Confirmar contenção · Exploradora de teste','Como o cavalo foi contido? (mínimo 12 caracteres)')
   await click(page,c,'Levar cavalo sob guarda (ainda não é propriedade)')
   await click(page,c,'Exploradora de teste recolhe insígnia');await click(page,c,'Exploradora de teste recolhe VALIOSO');await click(page,c,'Associar castelo: Castelo já descoberto')
   # Real official treasure tables, controlled legal D66 to avoid artifact-gap
   # fixture; no state injection or inventory fabrication.
   await page.evaluate('window.__u24Random=Math.random;Math.random=()=>.01')
   proof['atomicRollbackEvidence']=await prove_atomic_rollback(page,c,lambda:page.evaluate(r'''async c=>{const{JourneyService:J}=await import('/src/services/JourneyService.ts');await J.returnOfficialHorseToFamily(c.campaignId,c.eventId,c.heroId);}''',c))
   await click(page,c,'Entregar cavalo e insígnia · recompensa para Exploradora de teste')
   await page.evaluate('Math.random=window.__u24Random')
   method='returnOfficialHorseToFamily';args=[c['heroId']]
  elif choice=='help':
   proof['atomicRollbackEvidence']=await prove_atomic_rollback(page,c,lambda:page.evaluate(r'''async c=>{const{JourneyService:J}=await import('/src/services/JourneyService.ts');await J.recordOfficialBalloonAssistance(c.campaignId,c.eventId,'Reparos materiais confirmados pela Mestra.','Perna estabilizada, ainda em recuperação.');}''',c),single_store=True)
   await page.get_by_placeholder('Como o balão foi reparado? (mínimo 12 caracteres)',exact=True).fill('A Mestra confirmou os reparos materiais e que o balão está apto a voar.')
   await page.get_by_placeholder('Como a perna foi atendida? (mínimo 12 caracteres)',exact=True).fill('A perna foi estabilizada e imobilizada; continuará em recuperação, sem cura instantânea.')
   await click(page,c,'Registrar reparo e atendimento')
  else:
   proof['atomicRollbackEvidence']=await prove_atomic_rollback(page,c,lambda:page.evaluate(r'''async c=>{const{JourneyService:J}=await import('/src/services/JourneyService.ts');await J.recoverOfficialBalloonLoot(c.campaignId,c.eventId,c.heroId,'A Mestra confirmou acesso real ao balão.');}''',c))
   await click(page,c,'Mestra confirma acesso · recolher para Exploradora de teste','Como a Mestra adjudicou o acesso ao balão? (12–400 caracteres)')
   await click(page,c,'Confirmar que Andred não abriu fogo','Resposta de Andred e afastamento sem combate (mínimo 12 caracteres)')
  final=await state(page,c);d=final['event']['result']['officialSceneDecision'];assert final['event']['status']=='resolved' and d.get('pending') is None,d
  hero=next(h for h in final['characters'] if h['id']==c['heroId'])
  if id==6:
   assert d['horseCaptureConfirmed'] and not d['horseCalmed'] and not d['automaticRollsApplied']
   assert d['horseInsigniaReturnedAt'] and d['horseValuableRecoveredAt'] and d['horseFamilyRewardAt'] and not d['horseFamilyArtifactPending']
   assert any(m['id']==d['horseMountId'] and m['ownership']=='awarded' for m in final['world']['journey']['mounts'])
   assert not any(g.get('id')==f'official-horse-crest:{c["eventId"]}' for g in hero['gear'])
  elif choice=='help':assert not d['andredLegAutomaticallyHealed'] and d['balloonRepairEvidence'] and d['andredLegCareEvidence'] and not d['automaticResourcesChanged']
  else:assert d['balloonLootRecoveredAt'] and d['balloonLootSimple'] and d['balloonLootValuable'] and d['andredDidNotOpenFire']
  proof['finalStage']=d['stage'];proof['decision']=d;proof['inventory']=hero['gear'];proof['mounts']=final['world']['journey']['mounts']
  proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS';proof['evidencePerStage']['canonical_transaction']='PASS'
  await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-status','resolved');proof['evidencePerStage']['receipt']='PASS'
  assert await state(page,other)==isolation;proof['evidencePerStage']['campaign_isolation']='PASS';proof['replayError']=await reject(page,c,method,args);proof['evidencePerStage']['idempotency']='PASS';proof['evidencePerStage']['rollback']='PASS'
  await page.reload(wait_until='domcontentloaded');await page.wait_for_function('typeof window.__flUiMount === "function"');await mount(page,c['campaignId']);await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-status','resolved');assert await state(page,c)==final and await state(page,other)==isolation;proof['evidencePerStage']['reload']='PASS';proof['status']='PASS'
 except Exception as e:proof['error']=str(e);proof['traceback']=traceback.format_exc()
 print(json.dumps(dict(path=c['key'],status=proof['status'],error=proof.get('error')),ensure_ascii=False),flush=True);return proof
async def main():
 result=dict(status='FAIL',expected=3,passed=0,browserExecuted=True,paths=[],failures=[])
 async with async_playwright() as p:
  b=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage']);page=await b.new_page()
  try:
   await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html');await page.wait_for_function('typeof window.__flUiMount === "function"')
   for args in [(6,'capture','item_or_skill'),(16,'help','item_or_skill'),(16,'rob','combat')]:result['paths'].append(await test(page,*args))
  finally:await b.close()
 result['passed']=sum(v['status']=='PASS' for v in result['paths']);result['failures']=[{'pathKey':v['pathKey'],'error':v['error']} for v in result['paths'] if v['status']!='PASS'];result['status']='PASS' if result['passed']==3 else 'FAIL';(OUT/'u24-source-adjudications.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='paths'},ensure_ascii=False));return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
