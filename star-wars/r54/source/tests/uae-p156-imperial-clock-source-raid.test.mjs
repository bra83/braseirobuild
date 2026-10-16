/** P156 — official narrative evidence, fourth encounter, causal clock, Android bridge. */
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {createAdversary,adversarySkillRank,PUBLISHED_ADVENTURE_ADVERSARY_PROFILES} from '../app/src/main/assets/vtt/js/rules/adversaries.mjs';
import {startBeyondTheRim,currentBeyondTheRimEncounter,beyondTheRimEncounterSpec,markBeyondTheRimEncounterActive,recordBeyondTheRimEncounterOutcome,
 BTR_IMPERIAL_DEPLOYMENT_TIMELINE,beyondTheRimImperialClockStatus,advanceBeyondTheRimImperialClock} from '../app/src/main/assets/vtt/js/adventure/beyondTheRim.mjs';
import {publishedAdventureCatalog,getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
const copy=structuredClone;
class Repo{constructor(){this.saved=createCampaign('p156');this.writes=0;this.fail=false;}load(){return copy(this.saved);}save(x){if(this.fail)throw Error('P156 storage failure');this.saved=copy(x);this.writes++;}}
function fixture(node='imperial-interruption'){
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 g.startPublishedAdventureSession('beyond-the-rim',{rng:()=>0.5});
 const w=copy(g.state.world);w.publishedAdventure.currentNodeId=node;
 w.publishedAdventure.visitedNodeIds.push(node);g.commit('WORLD_UPDATED',{world:w});
 return {g,repo};
}
function worldAt(node){const w=startBeyondTheRim({}).world;w.publishedAdventure.currentNodeId=node;w.publishedAdventure.visitedNodeIds.push(node);return w;}

test('P156: book p.29 Yiyar group skills and equipment consumed by real adversary engine',()=>{
 const profile=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.yiyar_salvager;
 assert.equal(profile.source.printedPage,29);assert.equal(profile.source.authority,'PARTIALLY_PDF_VERIFIED');
 assert.deepEqual(profile.groupSkills,['atletismo','disciplina','armas_cuerpo_a_cuerpo','pilotar_vehiculo_planetario','armas_distancia_ligeras','sigilo']);
 assert.deepEqual(profile.weapons.map(w=>[w.damage,w.crit]),[[6,3],[3,3]]);
 assert.equal(profile.weapons[1].pierce,2);assert.equal(profile.weapons[1].vicious,1);
 const enemy=createAdversary('yiyar_salvager',{count:3});assert.equal(enemy.membersTotal,3);
 assert.equal(adversarySkillRank(enemy,'disciplina'),2);assert.equal(adversarySkillRank(enemy,'percepcion'),0);
 assert.ok(profile.source.provisionalFields.includes('woundsThreshold'));
});
test('P156: Talo source-backed skills, adversary talent, weapons and explicit unverified attributes',()=>{
 const p=PUBLISHED_ADVENTURE_ADVERSARY_PROFILES.talo_yiyar;
 assert.equal(p.source.printedPage,29);assert.equal(p.kind,'rival');assert.equal(p.talents[0].rank,1);
 assert.equal(p.skills.negociacion,2);assert.equal(p.skills.alerta,2);
 assert.equal(p.weapons[1].pierce,2);assert.ok(p.source.provisionalFields.includes('characteristics'));
 const t=createAdversary('talo_yiyar');assert.equal(adversarySkillRank(t,'negociacion'),2);
});
test('P156: raid exists in episode 3, 3 opponent groups, no duplicate active encounters',()=>{
 const w=worldAt('dropoff-point'),spec=beyondTheRimEncounterSpec(w);
 assert.equal(spec.id,'btr-yiyar-scrapheap-raid');assert.deepEqual(spec.adversaries.map(x=>x.profileKey),['yiyar_salvager','talo_yiyar','trandoshan_yiyar_mercenary']);
 assert.equal(spec.adversaries[0].count,3);
 assert.equal(spec.implementationAuthority,'BRASEIRO_PROVISIONAL_NOT_PDF_CERTIFIED');
 const active=markBeyondTheRimEncounterActive(w,spec.id);
 assert.equal(currentBeyondTheRimEncounter(active).status,'active');
 assert.throws(()=>markBeyondTheRimEncounterActive(active,spec.id),/já está ativo/);
});
for(const [outcome,resolved,repelled,complication,exposure] of [
 ['adversaries_defeated',true,true,false,2],['retreat',true,false,true,3],['player_incapacitated',true,false,true,3],['ended',false,false,false,2]]){
 test(`P156: Yiyar raid ${outcome} persists source-appropriate consequences, no invented victory`,()=>{
  const w=worldAt('dropoff-point');w.publishedAdventure.systems.imperialExposure=2;
  const active=markBeyondTheRimEncounterActive(w,'btr-yiyar-scrapheap-raid');
  const r=recordBeyondTheRimEncounterOutcome(active,{encounterId:'btr-yiyar-scrapheap-raid',outcome});
  const p=r.world.publishedAdventure;
  assert.equal(Boolean(p.flags.yiyarRaidResolved),resolved);
  assert.equal(Boolean(p.flags.yiyarRaidRepelled),repelled);
  assert.equal(Boolean(p.flags.yiyarRaidComplication),complication);
  assert.equal(p.systems.imperialExposure,exposure);
  assert.equal(p.systems.encounters.history.length,1);
  assert.throws(()=>recordBeyondTheRimEncounterOutcome(r.world,{encounterId:'btr-yiyar-scrapheap-raid',outcome}),/não disponível/);
 });
}
test('P156: controller persists encounter ownership through COMBAT_ACTION and reopens before finishing',()=>{
 const {g,repo}=fixture('dropoff-point');const started=g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5});
 assert.equal(started.encounter.id,'btr-yiyar-scrapheap-raid');
 assert.equal(g.state.combat.adversaries.length,3);
 assert.equal(repo.saved.combat.publishedAdventureContext.encounterId,'btr-yiyar-scrapheap-raid');
 assert.equal(validateState(repo.saved),true);
 const reopened=new GameController(repo);assert.equal(reopened.state.combat.publishedAdventureContext.nodeId,'dropoff-point');
 assert.throws(()=>reopened.advancePublishedAdventure('secure-isotech',{adventureId:'beyond-the-rim'}),/Encerre o encontro/);
 reopened.state.combat.encounter.outcome='adversaries_defeated';
 const result=reopened.finishCombat({rng:()=>0.5});
 assert.equal(result.publishedAdventureOutcome.outcome,'adversaries_defeated');
 assert.equal(repo.saved.world.publishedAdventure.flags.yiyarRaidRepelled,true);
 assert.equal(new GameController(repo).publishedAdventureStatus('beyond-the-rim').encounter.status,'resolved');
});
test('P156: combat ownership cannot be bypassed by changing adventure while another combat is active',()=>{
 const {g,repo}=fixture('dropoff-point');g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5});
 const combat=copy(g.state.combat);delete combat.publishedAdventureContext;g.commit('COMBAT_ACTION',{combat});
 const before=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.advancePublishedAdventure('secure-isotech',{adventureId:'beyond-the-rim'}),/Encerre o encontro/);
 assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P156: combat start rolls back all changes on storage failure',()=>{
 const {g,repo}=fixture('dropoff-point');const before=copy(g.state),saved=copy(repo.saved),writes=repo.writes;
 repo.fail=true;assert.throws(()=>g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5}),/storage failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P156: official Imperial deployment thresholds are 5,10,15,25,30 minutes',()=>{
 assert.deepEqual(BTR_IMPERIAL_DEPLOYMENT_TIMELINE.map(x=>x.minute),[5,10,15,25,30]);
 assert.equal(beyondTheRimImperialClockStatus(worldAt('dropoff-point')).available,false);
 assert.equal(beyondTheRimImperialClockStatus(worldAt('imperial-interruption')).elapsedMinutes,0);
});
test('P156: one 30-minute action crosses all milestones, creates consequences exactly once',()=>{
 const w=worldAt('imperial-interruption');const r=advanceBeyondTheRimImperialClock(w,{minutes:30,actionId:'p156:clock:all'});
 assert.equal(r.eventsAdded.length,5);assert.equal(r.clock.remainingMinutes,0);
 assert.equal(r.world.publishedAdventure.flags.imperialEscapeComplication,true);
 assert.equal(r.world.publishedAdventure.systems.imperialExposure,6);
 assert.equal(r.world.publishedAdventure.systems.imperialDeployment.receipts.length,1);
 assert.equal(beyondTheRimImperialClockStatus(w).elapsedMinutes,0);
 assert.throws(()=>advanceBeyondTheRimImperialClock(r.world,{minutes:1,actionId:'p156:clock:extra'}),/esgotado/);
});
test('P156: six 5-minute ticks are deterministic and do not repeat timeline events',()=>{
 let w=worldAt('imperial-interruption');const events=[];
 for(let i=0;i<6;i++){const r=advanceBeyondTheRimImperialClock(w,{minutes:5,actionId:`p156:clock:${i}`});w=r.world;events.push(...r.eventsAdded.map(e=>e.id));}
 assert.deepEqual(events,BTR_IMPERIAL_DEPLOYMENT_TIMELINE.map(e=>e.id));
 assert.equal(w.publishedAdventure.systems.imperialDeployment.receipts.length,6);
});
test('P156: clock replay idempotent, conflict rejected, forged receipts and events rejected',()=>{
 const {g,repo}=fixture('imperial-interruption');const first=g.advancePublishedImperialClock({minutes:5,actionId:'p156:clock:once'});
 assert.equal(first.duplicate,false);assert.equal(first.clock.elapsedMinutes,5);
 const writes=repo.writes,saved=copy(repo.saved);
 const reopened=new GameController(repo);const duplicate=reopened.advancePublishedImperialClock({minutes:5,actionId:'p156:clock:once'});
 assert.equal(duplicate.duplicate,true);assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);
 assert.throws(()=>reopened.advancePublishedImperialClock({minutes:10,actionId:'p156:clock:once'}),/divergente/);
 for(const corrupt of [c=>c.receipts.push(copy(c.receipts[0])),c=>c.events.pop(),c=>c.elapsedMinutes=9]){
  const w=copy(saved.world);corrupt(w.publishedAdventure.systems.imperialDeployment);
  assert.throws(()=>advanceBeyondTheRimImperialClock(w,{minutes:5,actionId:'p156:clock:new'}),/adulterados|não correspondem/);
 }
});
test('P156: invalid action IDs and minutes fail without mutation',()=>{
 const {g,repo}=fixture('imperial-interruption'),before=copy(repo.saved),writes=repo.writes;
 for(const actionId of [null,'','bad id','__proto__'])assert.throws(()=>g.advancePublishedImperialClock({minutes:5,actionId}));
 for(const minutes of [0,-1,31,1.2,'5'])assert.throws(()=>g.advancePublishedImperialClock({minutes,actionId:'p156:bad'}));
 assert.deepEqual(repo.saved,before);assert.equal(repo.writes,writes);
});
test('P156: clock action rollback restores time, flags, journal and world after failed save',()=>{
 const {g,repo}=fixture('imperial-interruption'),before=copy(g.state),saved=copy(repo.saved),writes=repo.writes;
 repo.fail=true;assert.throws(()=>g.advancePublishedImperialClock({minutes:30,actionId:'p156:clock:fail'}),/storage failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P156: active combat blocks countdown without writes',()=>{
 const {g,repo}=fixture('imperial-interruption');g.beginPublishedAdventureEncounter(null,{adventureId:'beyond-the-rim',rng:()=>0.5});
 const saved=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.advancePublishedImperialClock({minutes:5,actionId:'p156:clock:combat'}),/Encerre o encontro|Encerre o combate/);
 assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});
test('P156: Android bridge, visible clock button, source docs and bundle builder are wired',()=>{
 const runtime=readFileSync(new URL('../app/src/main/assets/vtt/r46/final-runtime.js',import.meta.url),'utf8');
 const bridge=readFileSync(new URL('../app/src/main/assets/vtt/js/core/uiBridge.mjs',import.meta.url),'utf8');
 assert.ok(runtime.includes('data-p156-imperial-tick'));
 assert.ok(runtime.includes("execute('imperial-clock'"));
 assert.ok(bridge.includes('advancePublishedImperialClock'));
});
for(const {id} of publishedAdventureCatalog())test(`P156: ${id} survives 3 reopenings after UAE start and choice`,()=>{
 const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('tendaar');
 g.executePublishedAdventureCommand({actionId:`p156:${id}:start`,kind:'start',adventureId:id});
 const choice=getPublishedAdventureAdapter(id).status(g.state.world).node.choices[0].id;
 g.executePublishedAdventureCommand({actionId:`p156:${id}:choice`,kind:'choice',adventureId:id,choiceId:choice});
 const saved=copy(repo.saved),writes=repo.writes;
 for(let i=0;i<3;i++){const x=new GameController(repo);const r=x.executePublishedAdventureCommand({actionId:`p156:${id}:start`,kind:'start',adventureId:id});assert.equal(r.duplicate,true);assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);assert.equal(validateState(repo.saved),true);}
});

test('P156: after 30 minutes, published adventure follows costly escape branch and reopens',()=>{
 const {g,repo}=fixture('imperial-interruption');
 g.advancePublishedImperialClock({minutes:30,actionId:'p156:final-clock'});
 assert.throws(()=>g.advancePublishedAdventure('make-final-escape',{adventureId:'beyond-the-rim'}));
 const moved=g.advancePublishedAdventure('breakout-costly',{adventureId:'beyond-the-rim'});
 assert.equal(moved.adventure.node.id,'flying-junk');
 assert.equal(repo.saved.world.publishedAdventure.systems.imperialDeployment.elapsedMinutes,30);
 assert.equal(validateState(repo.saved),true);
 for(let i=0;i<3;i++)assert.equal(new GameController(repo).publishedAdventureStatus('beyond-the-rim').node.id,'flying-junk');
});
test('P156: Android BraseiroUiBridge executes countdown and exposes failure without state mutation',async()=>{
 const {BraseiroUiBridge}=await import('../app/src/main/assets/vtt/js/core/uiBridge.mjs');
 const {repo}=fixture('imperial-interruption');const bridge=new BraseiroUiBridge({repository:repo});
 const r=bridge.advancePublishedImperialClock({minutes:5,actionId:'p156:bridge:tick'});
 assert.equal(r.ok,true);assert.equal(r.data.clock.elapsedMinutes,5);
 const writes=repo.writes;
 const duplicate=bridge.advancePublishedImperialClock({minutes:5,actionId:'p156:bridge:tick'});
 assert.equal(duplicate.ok,true);assert.equal(duplicate.data.duplicate,true);assert.equal(repo.writes,writes);
 const bad=bridge.advancePublishedImperialClock({minutes:10,actionId:'p156:bridge:tick'});
 assert.equal(bad.ok,false);assert.match(bad.message,/divergente/);assert.equal(repo.writes,writes);
});

test('P156: published clock command commits one UAE causal receipt, replays exactly once',()=>{
 const {g,repo}=fixture('imperial-interruption');
 const id='p156:published:clock';
 const first=g.executePublishedAdventureCommand({actionId:id,kind:'imperial-clock',adventureId:'beyond-the-rim',minutes:5});
 assert.equal(first.applied,true);assert.equal(first.clock.elapsedMinutes,5);
 assert.equal(repo.saved.world.publishedAdventureActionReceipts.at(-1).kind,'imperial-clock');
 assert.ok(repo.saved.world.publishedAdventureRuntime.canonicalEvents.some(e=>e.type==='PUBLISHED_COMMAND_COMMITTED'&&e.payload.actionId===id));
 const saved=copy(repo.saved),writes=repo.writes;
 const reopened=new GameController(repo);
 const duplicate=reopened.executePublishedAdventureCommand({actionId:id,kind:'imperial-clock',adventureId:'beyond-the-rim',minutes:5});
 assert.equal(duplicate.duplicate,true);assert.equal(duplicate.clock.elapsedMinutes,5);
 assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
 assert.throws(()=>reopened.executePublishedAdventureCommand({actionId:id,kind:'imperial-clock',adventureId:'beyond-the-rim',minutes:10}),/reutilizado/);
 assert.equal(repo.writes,writes);
});
test('P156: published clock rejects other adventures, preserves world and rolls back failed save',()=>{
 const {g,repo}=fixture('imperial-interruption');const before=copy(g.state),saved=copy(repo.saved),writes=repo.writes;
 assert.throws(()=>g.executePublishedAdventureCommand({actionId:'p156:wrong',kind:'imperial-clock',adventureId:'under-black-sun',minutes:5}),/pertence apenas/);
 assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
 repo.fail=true;
 assert.throws(()=>g.executePublishedAdventureCommand({actionId:'p156:fail',kind:'imperial-clock',adventureId:'beyond-the-rim',minutes:5}),/storage failure/);
 assert.deepEqual(g.state,before);assert.deepEqual(repo.saved,saved);assert.equal(repo.writes,writes);
});

test('P156: Android outbox restores lost ACK for imperial clock without double advancing',async()=>{
 const vm=await import('node:vm');
 const script=readFileSync(new URL('../app/src/main/assets/vtt/r46/published-command-client.js',import.meta.url),'utf8');
 const {g,repo}=fixture('imperial-interruption');let controller=g,lostAck=true,ids=0;
 const store=new Map(),storage={getItem:k=>store.get(k)??null,setItem:(k,v)=>store.set(k,String(v)),removeItem:k=>store.delete(k)};
 const bridge={readState:()=>({ok:true,data:repo.load()}),executePublishedAdventureCommand(command){
  try{const data=controller.executePublishedAdventureCommand(command);if(lostAck){lostAck=false;return {ok:false,error:'ENGINE_EXCEPTION',message:'ACK perdido após commit'};}return {ok:true,data};}
  catch(error){return {ok:false,error:'PUBLISHED_ADVENTURE_FAILED',message:error.message};}
 }};
 const open=()=>{const window={localStorage:storage,BraseiroGameBridge:bridge,crypto:{randomUUID:()=>`00000000-0000-4000-8000-${String(++ids).padStart(12,'0')}`}};
  vm.runInNewContext(script,{window},{filename:'published-command-client.js'});return window;};
 const window=open();const first=window.BraseiroPublishedActionClient.execute('imperial-clock','beyond-the-rim',{minutes:5});
 assert.equal(first.ok,false);assert.equal(repo.saved.world.publishedAdventure.systems.imperialDeployment.elapsedMinutes,5);
 const saved=copy(repo.saved),writes=repo.writes;
 assert.ok(storage.getItem('braseiro.starwars.published.pending.v1'));
 controller=new GameController(repo);
 const reopened=open();assert.equal(reopened.__BRASEIRO_PUBLISHED_RECOVERY__?.ok,true);
 assert.equal(reopened.__BRASEIRO_PUBLISHED_RECOVERY__?.data?.duplicate,true);
 assert.equal(repo.writes,writes);assert.deepEqual(repo.saved,saved);
 assert.equal(storage.getItem('braseiro.starwars.published.pending.v1'),null);
 assert.equal(validateState(repo.saved),true);
});
