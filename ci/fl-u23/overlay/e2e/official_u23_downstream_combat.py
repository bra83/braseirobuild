#!/usr/bin/env python3
"""Chromium: real JourneyPanel starts/postcombat UI + real EncounterService turns.
Deterministic success/miss fixtures inject legal die faces into the production
engine. No state writes after setup, no adjustStrength/setStatus/end shortcuts.
This covers one complete winning outcome per source path, not every possible
combat outcome, Android, or full manual character attack UI.
"""
import asyncio,json,os,sys,traceback,uuid
from pathlib import Path
from playwright.async_api import async_playwright,expect
from official_ui_browser import mount,SEED
from official_u22_downstream_batch import STAGES,NOTE,changed
ORIGIN=os.environ.get('FL_E2E_ORIGIN','http://127.0.0.1:4173')
OUT=Path(os.environ.get('FL_E2E_ARTIFACTS','e2e-results'));OUT.mkdir(parents=True,exist_ok=True)
CASES=[]
def add(i,choice,kind='combat',visit=1):CASES.append(dict(id=i,choice=choice,visit=visit,key=f'{i}:{visit}:{choice}:{kind}'))
for i,ch in [(3,'rescue'),(4,'cross'),(4,'attack'),(5,'bribe'),(5,'fight'),(7,'help'),(7,'challenge'),(8,'attack'),(11,'search'),(12,'fight'),(18,'attack'),(20,'attack'),(21,'attack'),(23,'protect_puppy'),(24,'attack_kurge'),(25,'confront_gonlab'),(29,'attack'),(30,'attack'),(33,'help_markut'),(33,'help_soldiers'),(35,'fight'),(37,'attack'),(40,'fight'),(41,'attack'),(42,'attack'),(43,'attack')]:add(i,ch,'gm_decision' if i==7 and ch=='help' else 'combat')
add(10,'confront');add(2,'attack');add(17,'join_fight');add(25,'accept_candle_quest','follow_up');add(5,'flee','item_or_skill');add(18,'attack',visit=2);add(18,'wait_for_melkar','gm_decision',3)
SETUP=r'''async c=>{const{getDb}=await import('/src/db/db.ts');const db=await getDb();
 const campaign=await db.get('campaign',c.campaignId);const hero=await db.get('characters',c.heroId);const ids=[c.heroId,...[1,2,3].map(i=>`${c.heroId}-${i}`)];
 for(const [i,id] of ids.entries())await db.put('characters',{...hero,id,name:i?`Aliada ${i}`:'Exploradora de teste',kin:'human',profession:'fighter',
  attributes:{strength:{current:5,max:5},agility:{current:3,max:3},wits:{current:3,max:3},empathy:{current:3,max:3}},
  skills:{Melee:3,Movement:3,Might:3,Scout:3,'Animal Handling':3},
  gear:[{id:`bait-${id}`,name:'Medalhão brilhante',quantity:1}],
  weapons:[{name:'Machado de batalha',damage:2,bonus:2,grip:'one',weight:1},{name:'Tocha acesa',damage:1,bonus:0,grip:'one',weight:1}],armor:[],coins:{gold:0,silver:20,copper:0}});
 await db.put('campaign',{...campaign,partyCharacterIds:ids});const w=await db.get('world_state',c.campaignId);w.journey.events[0].result.officialEncounterVisitNumber=c.visit;w.journey.events[0].gmDetail='SEGREDO_MESTRE_NAO_EXIBIR';await db.put('world_state',w);
}'''
STATE=r'''async c=>{const{getDb}=await import('/src/db/db.ts');const db=await getDb();const w=await db.get('world_state',c.campaignId);
 const all=async n=>(await db.getAll(n)).filter(v=>v.campaignId===c.campaignId||v.sceneId===c.eventId||v.sceneId===`official-encounter:${c.eventId}`||String(v.characterId||'').startsWith(c.heroId)).sort((a,b)=>String(a.id).localeCompare(String(b.id)));
 return{campaign:await db.get('campaign',c.campaignId),world:w,event:w.journey.events.find(v=>v.id===c.eventId),characters:await all('characters'),rolls:await all('rolls'),npcs:await all('npcs')};}'''
CALL=r'''async c=>{const{JourneyService:J}=await import('/src/services/JourneyService.ts');try{await J[c.method](c.campaignId,c.eventId,...(c.args||[]));return{rejected:false};}catch(e){return{rejected:true,message:String(e?.message||e)};}}'''
TURN=r'''async c=>{
 const{getDb}=await import('/src/db/db.ts');const{EncounterService:E}=await import('/src/services/EncounterService.ts');const{JourneyService:J}=await import('/src/services/JourneyService.ts');const{ForbiddenLandsDiceEngine:D}=await import('/src/services/ForbiddenLandsDiceEngine.ts');const db=await getDb();
 const w=await db.get('world_state',c.campaignId);const enc=w.encounter;if(enc.status!=='active')return{done:true};const a=enc.combatants[enc.turnIndex];const d=w.journey.events.find(e=>e.id===c.eventId).result.officialSceneDecision;
 const high=()=>.99,low=()=>.01;let result={actor:a.name,round:enc.round,action:'next_turn'};
 if(a.status==='active'&&a.slowActionAvailable&&a.side!=='neutral'){
  const opposite=a.side==='enemy'?'party':'enemy';const target=enc.combatants.filter(x=>x.side===opposite&&x.status==='active').sort((left,right)=>Math.hypot(Number(left.x??.5)-Number(a.x??.5),Number(left.y??.5)-Number(a.y??.5))-Math.hypot(Number(right.x??.5)-Number(a.x??.5),Number(right.y??.5)-Number(a.y??.5)))[0];
  if(c.id===30&&a.kind==='character'&&a.side==='party'){
   const r=await J.attemptOfficialTeramaldaSpikeRemoval(c.campaignId,c.eventId,a.entityId,high);return{actor:a.name,action:'remove_spike',success:r.removed,done:r.world.encounter.status==='resolved'};
  }
  if(c.id===5&&c.choice==='bribe'&&a.side==='party'&&a.entityId===c.heroId&&!d.harpyBaitThrownAt){await J.throwOfficialHarpyBait(c.campaignId,c.eventId);result.action='throw_bait';}
  else if(c.id===5&&c.choice==='bribe'&&a.side==='enemy'&&d.harpyBaitThrownAt&&!d.harpyBaitRecoveredAt){await J.reactOfficialHarpyToBait(c.campaignId,c.eventId,a.id,'pick_up',true);result.action='pick_up_bait';}
  else if(c.id===5&&c.choice==='flee'&&a.side==='party'){const r=await E.flee(c.campaignId,a.id,{distance:'short',terrain:'normal',rng:high});result.action='flee';result.success=r.success;}
  else if(c.id===10&&a.side==='party'&&!(await db.getAll('rolls')).some(r=>r.sceneId===c.eventId&&String(r.checkId||'').startsWith('fear:'))){result.action='wait_for_generated_attack';}
  else if(c.id===5&&c.choice==='bribe'&&a.side==='party'&&!d.harpyBaitRecoveredAt){result.action='wait_for_bait_reaction';}
  else if(target){
   const table=a.quickSheet?.monsterAttackTable;
   if(c.id===10&&a.side==='enemy'){await E.resolveOfficialGrelfAttack(c.campaignId,a.id,target.id,0,low);result.action='generated_demon_attack';}
   else
   if(a.kind==='monster'&&Array.isArray(table)&&table.length===6){
    const row=table.find(x=>!x.tags?.includes('area')&&!x.tags?.includes('two_targets')&&!x.failureEffect&&!x.tags?.includes('fear'))||table[0];
    const face=()=>((row.roll-1)+.1)/6;
    if(a.quickSheet?.officialProcessionEnt)await E.prepareOfficialProcessionEntAttack(c.campaignId,a.id,face);
    else if(a.quickSheet?.officialBookMonster)await E.prepareOfficialMonsterAttack(c.campaignId,a.id,face);
    let targets=row.tags?.includes('area')?enc.combatants.filter(x=>x.side===opposite&&x.status==='active').map(x=>x.id):row.tags?.includes('two_targets')?enc.combatants.filter(x=>x.side===opposite&&x.status==='active').slice(0,2).map(x=>x.id):[target.id];
    // Unprepared bestiary attacks roll their own table first, then zero-success attack dice.
    let first=true;const rng=()=>{if(first&&!a.quickSheet?.officialBookMonster&&!a.quickSheet?.officialProcessionEnt){first=false;return face();}return low();};
    await E.resolveBookOfBeastsAttack(c.campaignId,a.id,targets,rng,'wits',undefined,a.quickSheet?.officialBookMonster||a.quickSheet?.officialProcessionEnt?row.roll:undefined);result.action='official_monster_attack';
   }else if(a.kind==='npc'&&a.side==='enemy'&&[3,7,8,12,29,41,42,43].includes(c.id)){
    await E.resolveOfficialNpcMelee(c.campaignId,a.id,target.id,'none',true,low);result.action='official_npc_attack';
   }else{
    const h=a.kind==='character'?await db.get('characters',a.entityId):null;const q=a.quickSheet||{};
    const fire=a.side==='party'&&[20,21].includes(c.id);const index=fire?1:0;const weapon=(h?.weapons||q.weapons||[])[index];
    const attrs=h?.attributes||q.attributes||{};const skill=h?.skills||q.skills||{};
    const base=a.currentStrength;const skillDice=Number(skill.Melee??skill.Luta??skill.Fighting??0);const gear=Number(weapon?.bonus??0);
    const roll=D.roll({base,skill:skillDice,gear},{characterId:h?.id||null,sceneId:c.eventId,checkId:`u23-real-attack:${enc.id}:${enc.round}:${a.id}`,rng:a.side==='party'?high:low,skillName:'melee',attributeName:'strength',skillRankAtRoll:skillDice});
    const r=await E.commitBasicAttackResult(c.campaignId,a.id,target.id,{attackRoll:roll,weaponDamage:Number(weapon?.damage||1),criticalTable:fire?'other':'slash',attackKind:weapon?'strike':'unarmed',...(weapon?{weaponIndex:index}:{}),rng:low});result.action='basic_attack';result.damage=r.resolution.finalDamage;result.rollId=roll.id;
   }
  }
 }
 const fresh=(await db.get('world_state',c.campaignId)).encounter;if(fresh.status==='active')await E.nextTurn(c.campaignId);return{...result,done:fresh.status==='resolved'};
}'''
async def state(page,c):return await page.evaluate(STATE,c)
async def reject(page,c,method,args=None):
 before=await state(page,c);r=await page.evaluate(CALL,{**c,'method':method,'args':args or []});after=await state(page,c)
 assert r['rejected'] and before==after,dict(method=method,result=r,unchanged=before==after)
 return r['message']
async def prove_atomic_rollback(page,c,action,single_store=False):
 before=await state(page,c)
 await page.evaluate(r"""c=>{window.__u23Put=IDBObjectStore.prototype.put;window.__u23Writes=[];
  IDBObjectStore.prototype.put=function(value,...rest){window.__u23Writes.push(this.name);
   if(this.name==='world_state'&&value?.id===c.campaignId)throw Error('U23_INJECTED_FINAL_WRITE_FAILURE');
   return window.__u23Put.call(this,value,...rest);};}""",c)
 caught=None
 try:
  await action()
 except Exception as e:
  if 'U23_INJECTED_FINAL_WRITE_FAILURE' not in str(e):raise
  caught=str(e)
 finally:
  writes=await page.evaluate("()=>{IDBObjectStore.prototype.put=window.__u23Put;return window.__u23Writes;}")
 assert caught and 'world_state' in writes and (single_store or ({'rolls','characters'} & set(writes))),dict(caught=caught,writes=writes)
 assert await state(page,c)==before,'Atomic failure left partial character/roll/world writes'
 return dict(injectedFailure='final world_state write',attemptedStores=sorted(set(writes)),stateUnchanged=True)
async def click(page,c,name,placeholder=None):
 if placeholder:await page.get_by_placeholder(placeholder,exact=True).fill(NOTE)
 before=await state(page,c);button=page.get_by_role('button',name=name,exact=True);await expect(button).to_be_enabled();await button.click()
 for _ in range(200):
  current=await state(page,c)
  if current!=before:return current
  error=await page.evaluate('window.__flUiError')
  if error:raise AssertionError(error)
  await page.wait_for_timeout(25)
 raise AssertionError('UI commit absent')
async def test(page,recipe):
 c={**recipe,'campaignId':'u23-'+uuid.uuid4().hex,'eventId':'event-'+uuid.uuid4().hex,'heroId':'hero-'+uuid.uuid4().hex}
 other={**c,'campaignId':'other-'+uuid.uuid4().hex,'eventId':'other-'+uuid.uuid4().hex,'heroId':'other-'+uuid.uuid4().hex}
 proof=dict(pathKey=c['key'],status='FAIL',evidencePerStage={x:'NOT_RUN' for x in STAGES},browser='Chromium',persistence='IndexedDB',turns=[],scope='JourneyPanel initial/start/postcombat UI; battle actions use production API with explicit deterministic die fixtures')
 try:
  for fixture in (c,other):await page.evaluate(SEED,fixture);await page.evaluate(SETUP,fixture)
  isolation=await state(page,other);await reject(page,c,'completeOfficialSceneCombat');await mount(page,c['campaignId'])
  await page.get_by_test_id(f'fl-official-choice-{c["id"]}-{c["choice"]}').click()
  await page.wait_for_function('''async c=>{const{getDb}=await import('/src/db/db.ts');return !!(await(await getDb()).get('world_state',c.campaignId)).journey.events[0].result.officialSceneDecision;}''',arg=c)
  if c['id']==25 and c['choice']=='accept_candle_quest':await click(page,c,'Confirmar que Gonlab foi localizado','Adjudicação da busca (mínimo 12 caracteres)')
  if c['id']==20:await click(page,c,'Atacar os Ents')
  if c['id']==18 and c['visit']==3:await click(page,c,'Confirmar retorno de Melkar')
  if c['id']==5 and c['choice']=='bribe':await click(page,c,'Exploradora de teste: Medalhão brilhante')
  if c['id']==41:
   for sibling in ('ginda','aneta'):
    for label,value in [('arma corpo a corpo','broad_sword'),('arma secundária','long_spear'),('armadura','leather')]:await page.get_by_label(f'{sibling} {label}',exact=True).select_option(value)
  if c['id']==42:await page.get_by_label('Arma dos caçadores sáurios',exact=True).select_option('trident')
  if c['id']==10:
   await page.evaluate('window.__u23OriginalRandom=Math.random;Math.random=()=>.5')
   await click(page,c,'Gerar ficha de Grelf pelas tabelas oficiais')
   await page.evaluate('Math.random=window.__u23OriginalRandom')
   await click(page,c,'Iniciar combate com Grelf gerado')
  elif c['id'] in (2,17):
   if c['id']==17:
    await page.get_by_label('Convidados orcs adjudicados',exact=True).fill('2');await page.get_by_label('Convidados humanos adjudicados',exact=True).fill('2')
   await click(page,c,'Iniciar combate com perfis típicos confirmados','Confirmação da Mestra sobre os perfis adotados (mínimo 12 caracteres)')
  else:await click(page,c,'Escolta Roka · iniciar combate' if c['id']==3 else 'Iniciar combate oficial')
  started=await state(page,c);assert started['world']['encounter']['status']=='active'
  proof['evidencePerStage']['reachable_UI']='PASS';proof['evidencePerStage']['preconditions']='PASS'
  await reject(page,c,'completeOfficialSceneCombat')
  if c['id']==30:await click(page,c,'Confirmar imobilização pela MdJ','Como a MdJ imobilizou Teramalda? (mínimo 12 caracteres)')
  for turn in range(250):
   battle=(await state(page,c))['world']['encounter']
   if battle['status']=='resolved':break
   actor=battle['combatants'][battle['turnIndex']]
   decision=(await state(page,c))['event']['result']['officialSceneDecision']
   eligible=actor['side']=='party' and actor['kind'] in ('character','npc') and actor['status']=='active' and actor['slowActionAvailable']
   if c['id']==10:eligible=actor['side']=='enemy' and actor['status']=='active' and actor['slowActionAvailable']
   if c['id']==5 and c['choice']=='bribe':eligible=eligible and bool(decision.get('harpyBaitRecoveredAt'))
   if eligible and not proof.get('atomicRollbackEvidence'):
    proof['atomicRollbackEvidence']=await prove_atomic_rollback(page,c,lambda:page.evaluate(TURN,c))
   r=await page.evaluate(TURN,c);proof['turns'].append(r)
   if c['id']==40 and any(t.get('damage',0)>0 for t in proof['turns']):
    await page.get_by_label('Tentáculos cortados',exact=True).fill('4');await page.get_by_label('Total de tentáculos adjudicado',exact=True).fill('8');await page.get_by_label('Evidência da adjudicação do polvo',exact=True).fill('Mesa confirmou quatro tentáculos cortados após ataques reais; total oito adjudicado.');await click(page,c,'Confirmar fuga do polvo à metade dos tentáculos');break
  battle=(await state(page,c))['world']['encounter'];assert battle['status']=='resolved',{'turnLimit':battle}
  if c['id']==10:
   await page.get_by_placeholder('Regras situacionais de Grelf e efeitos resolvidos (mínimo 20 caracteres)',exact=True).fill('Forma aranha D66 44, asas D66 44. Ataques de rugido falharam nos dados reais; não houve toque nem veneno. A mesa confirmou ausência de símbolos sagrados, portanto a fraqueza não foi ativada. Não houve outra habilidade opcional usada.')
   await click(page,c,'Conferir regras situacionais de Grelf')
  await click(page,c,'Registrar desfecho do combate')
  final_method='completeOfficialSceneCombat';args=[]
  if c['id']==3:await click(page,c,'Libertar Sargah');final_method='resolveOfficialSargah';args=['release']
  if c['id']==11:await click(page,c,'Exploradora de teste: recolher os três objetos');final_method='claimOfficialForgottenPrinceTreasure';args=[c['heroId']]
  if c['id']==18:
   if c['visit']<3:await click(page,c,'Recolher diamante para Exploradora de teste');final_method='claimOfficialDemonicDoughDiamond';args=[c['heroId']]
   else:await click(page,c,'Libertar os halflings');final_method='resolveOfficialDoughCaptives';args=['release']
  if c['id']==24:await click(page,c,'Libertar Oker');final_method='resolveOfficialCursedOgrePrisoner';args=['free_oker']
  if c['id']==25:await click(page,c,'Confirmar retorno e receber 10 velas · Exploradora de teste','Como o filho foi recuperado e voltou? (mínimo 12 caracteres)');final_method='completeOfficialCandleQuest';args=[c['heroId'],NOTE]
  if c['id']==43:await click(page,c,'Registrar família resgatada','Como ocorreu o resgate/desfecho (mínimo 8 caracteres)');final_method='resolveOfficialBurialRescue';args=['rescued',NOTE]
  final=await state(page,c);d=final['event']['result']['officialSceneDecision'];assert final['event']['status']=='resolved' and d.get('pending') is None,d
  hero=next(h for h in final['characters'] if h['id']==c['heroId']);proof['finalStage']=d['stage'];proof['combatants']=battle['combatants'];proof['rollsPersisted']=len(final['rolls'])
  assert final['rolls'] and any(t['action'] not in ('next_turn',) for t in proof['turns'])
  if c['id']==10:assert d['grelfGeneratedProfile']['dice'] and not d['grelfGeneratedProfile']['sourceGaps'] and d['grelfSituationalEvidence'] and any(t['action']=='generated_demon_attack' for t in proof['turns'])
  if c['id']==3:assert d['sargahFate']=='release' and not d['automaticCompanionRecruitment']
  if c['id']==11:assert len(d['tombLootItemIds'])==3 and all(any(it.get('id')==key for it in hero['gear']+hero['weapons']+hero['armor']) for key in d['tombLootItemIds'])
  if c['id']==18 and c['visit']<3:assert any(it.get('id')==d['doughDiamondItemId'] and it['valueGold']==1 for it in hero['gear']) and hero['coins']['gold']==0
  if c['id']==18 and c['visit']==3:assert d['stage']=='dough_captives_released'
  if c['id']==25:assert any(it.get('id')==d['candleItemId'] and it['quantity']==10 and not it['prepared'] for it in hero['gear'])
  if c['id']==30:assert d['spikeRemoved'] and any(t['action']=='remove_spike' and t['success'] for t in proof['turns'])
  if c['id']==37:assert battle['officialEncounter']['reinforcementsPending'] is False and len([x for x in battle['combatants'] if x['side']=='enemy'])==6
  if c['id']==40:assert d['octopusRetreated'] and d['octopusTentaclesCut']*2>=d['octopusTentacleTotal']
  if c['id']==5 and c['choice']=='bribe':assert d['harpyBaitThrownAt'] and d['harpyBaitRecoveredAt'] and not any(it.get('id')==f'bait-{c["heroId"]}' for it in hero['gear'])
  if c['id']==5 and c['choice']=='flee':assert all(x['status']=='fled' for x in battle['combatants'] if x['side']=='party')
  proof['evidencePerStage']['dice_or_combat_or_adjudication']='PASS';proof['evidencePerStage']['canonical_transaction']='PASS'
  receipt=page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}');await expect(receipt).to_have_attribute('data-status','resolved');await expect(receipt).to_have_attribute('data-committed','true');await expect(receipt).not_to_contain_text('SEGREDO_MESTRE_NAO_EXIBIR');proof['evidencePerStage']['receipt']='PASS'
  assert await state(page,other)==isolation;proof['evidencePerStage']['campaign_isolation']='PASS'
  assert proof.get('atomicRollbackEvidence'),'No failed multi-store write exercised'
  proof['replayError']=await reject(page,c,final_method,args);proof['evidencePerStage']['idempotency']='PASS';proof['evidencePerStage']['rollback']='PASS'
  await page.reload(wait_until='domcontentloaded');await page.wait_for_function('typeof window.__flUiMount === "function"');await mount(page,c['campaignId']);await expect(page.get_by_test_id(f'fl-official-receipt-{c["eventId"]}')).to_have_attribute('data-status','resolved');assert await state(page,c)==final and await state(page,other)==isolation
  proof['evidencePerStage']['reload']='PASS';proof['status']='PASS'
 except Exception as e:
  proof['error']=str(e);proof['traceback']=traceback.format_exc()
  try:await page.screenshot(path=str(OUT/f'u23-{c["id"]}-{c["choice"]}-{c["visit"]}.png'),full_page=True)
  except Exception:pass
 print(json.dumps(dict(path=c['key'],status=proof['status'],error=proof.get('error')),ensure_ascii=False),flush=True);return proof
async def main():
 cases=CASES
 if os.environ.get('FL_U23_KEYS'):cases=[c for c in cases if c['key'] in os.environ['FL_U23_KEYS'].split(',')]
 result=dict(status='FAIL',expected=len(cases),passed=0,browserExecuted=True,paths=[],failures=[])
 async with async_playwright() as p:
  b=await p.chromium.launch(headless=True,args=['--no-sandbox','--disable-dev-shm-usage'])
  async def worker(chunk):
   context=await b.new_context();page=await context.new_page();page.set_default_timeout(12000)
   await page.goto(ORIGIN+'/e2e/official-ui-browser-harness.html',wait_until='domcontentloaded');await page.wait_for_function('typeof window.__flUiMount === "function"')
   proofs=[]
   for c in chunk:proofs.append(await test(page,c))
   await context.close();return proofs
  try:
   batches=await asyncio.gather(*(worker(cases[i::3]) for i in range(3)))
   result['paths']=[v for batch in batches for v in batch]
   result['passed']=sum(v['status']=='PASS' for v in result['paths'])
   result['failures']=[dict(pathKey=v['pathKey'],error=v['error']) for v in result['paths'] if v['status']!='PASS']
  except Exception as e:result['failures'].append(dict(fatal=str(e),traceback=traceback.format_exc()))
  finally:await b.close()
 result['status']='PASS' if result['passed']==len(cases) and not result['failures'] else 'FAIL';(OUT/'u23-downstream-combat.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='paths'},ensure_ascii=False));return 0 if result['status']=='PASS' else 1
if __name__=='__main__':sys.exit(asyncio.run(main()))
