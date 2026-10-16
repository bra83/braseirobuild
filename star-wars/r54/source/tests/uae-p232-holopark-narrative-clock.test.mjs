import test from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {GameController} from '../app/src/main/assets/vtt/js/core/controller.mjs';
import {createCampaign,validateState} from '../app/src/main/assets/vtt/js/core/state.mjs';
import {recordResolvedTaskTime,validateTaskTimeLedger} from '../app/src/main/assets/vtt/js/world/taskTimeLedger.mjs';
import {getPublishedAdventureAdapter} from '../app/src/main/assets/vtt/js/adventure/publishedAdventureRegistry.mjs';
import {enterJewelHoloNarrativeClock,advanceJewelHoloNarrativeClock,endJewelHoloNarrativeClock,validateJewelHoloNarrativeClock} from '../app/src/main/assets/vtt/js/adventure/jewelHoloNarrativeClock.mjs';
const clone=structuredClone;
class Repo{constructor(){this.state=createCampaign('P232');this.saves=0;}load(){return clone(this.state);}save(s){this.state=clone(s);this.saves++;}}
function fixture(){const repo=new Repo(),g=new GameController(repo);g.loadStarterCharacter('cael');g.startPublishedAdventureSession('jewel-of-yavin',{rng:()=>.5});
 for(const id of ['protect-aris','reach-tourist-district','survey-cloud-city','prepare-heist','prepare-grand-prix','take-race-start','race-aftermath','go-to-museum','attend-cloud-nine'])g.advancePublishedAdventure(id);
 for(let i=0;i<10;i++){if(g.state.world.publishedAdventure.operations?.auction?.completed)break;g.adjudicateJewelOperation('auction_round',{actionId:'p232-auction-'+i});}
 g.advancePublishedAdventure('bank-operation');g.advancePublishedAdventure('skip-bank-transfer');g.advancePublishedAdventure('retreat-without-jewel');
 return {g,repo};}
const start={actionId:'p232-start',gmParkArrivalConfirmed:true,gmNarrativeModeConfirmed:true,rng:()=>0};
const shift=(id,n=40)=>({actionId:id,gmStillPresentConfirmed:true,rng:()=>((n-.5)/100)});
function task(g,id,minutes){const r=recordResolvedTaskTime(g.state.world,{checkId:'task_'+id,skillKey:'percepcion',minutes,baseMinutes:minutes});g.commit('WORLD_UPDATED',{world:r.world});return r;}
function atomic(g,fn){const before=clone(g.state),saves=g.repository.saves;assert.throws(fn);assert.deepEqual(g.state,before);assert.equal(g.repository.saves,saves);}
test('P232 refuses narrative park without real scene, GM confirmation or outside structured combat',()=>{
 const {g}=fixture();atomic(g,()=>g.enterJewelHoloNarrativeClock({...start,gmParkArrivalConfirmed:false}));
 atomic(g,()=>g.enterJewelHoloNarrativeClock({...start,gmNarrativeModeConfirmed:false}));
 atomic(g,()=>enterJewelHoloNarrativeClock(g.state.world,{active:true},start));
 assert.equal(g.enterJewelHoloNarrativeClock(start).holoNarrativeClock.landscape,'alderaan');
 atomic(g,()=>g.enterJewelHoloNarrativeClock({...start,actionId:'p232-duplicate'}));
});
test('P232 two minutes come ONLY from audited task-time receipts, no passive time',()=>{
 const {g}=fixture();g.enterJewelHoloNarrativeClock(start);const before=g.state.world.clock.elapsedMinutes??0;
 atomic(g,()=>g.advanceJewelHoloNarrativeClock(shift('early')));
 assert.equal(g.state.world.clock.elapsedMinutes??0,before);
 task(g,'p232_1',1);atomic(g,()=>g.advanceJewelHoloNarrativeClock(shift('still-early')));
 task(g,'p232_2',1);const out=g.advanceJewelHoloNarrativeClock(shift('p232-shift1',40));
 assert.equal(out.adjudication.dueMinute,before+2);assert.equal(out.adjudication.atMinute,before+2);
 assert.equal(out.holoNarrativeClock.landscape,'christophsis');
 assert.equal(g.state.world.clock.elapsedMinutes,before+2);validateTaskTimeLedger(g.state.world);
 atomic(g,()=>g.advanceJewelHoloNarrativeClock(shift('same-time')));
});
test('P232 overdue 6-minute clock produces three ordered landscape events without skipping or clock ticks',()=>{
 const {g}=fixture();g.enterJewelHoloNarrativeClock(start);task(g,'p232_6',6);
 const scenes=[];for(let i=0;i<3;i++){
  const out=g.advanceJewelHoloNarrativeClock(shift('p232-'+i,[50,80,98][i]));scenes.push(out.holoNarrativeClock.landscape);
  assert.equal(out.adjudication.dueMinute,2*(i+1));assert.equal(out.adjudication.atMinute,6);
 }
 assert.deepEqual(scenes,['coruscant','kashyyyk','projectors_off']);
 assert.equal(g.state.world.clock.elapsedMinutes,6);
 atomic(g,()=>g.advanceJewelHoloNarrativeClock(shift('extra')));
 assert.equal(validateState(g.state),true);
});
test('P232 printed d100=47 overlap requires explicit GM choice and receipts survive three reopenings',()=>{
 const {g,repo}=fixture();atomic(g,()=>g.enterJewelHoloNarrativeClock({...start,rng:()=>.465}));
 g.enterJewelHoloNarrativeClock({...start,rng:()=>.465,gm47Choice:'coruscant'});
 assert.equal(g.state.world.publishedAdventure.operations.holoNarrativeClock.landscape,'coruscant');
 task(g,'p232_47',2);g.advanceJewelHoloNarrativeClock({...shift('p232-47',47),gm47Choice:'christophsis'});
 for(let i=0;i<3;i++)assert.equal(new GameController(repo).state.world.publishedAdventure.operations.holoNarrativeClock.landscape,'christophsis');
});
test('P232 tampered receipt, clock, chronology or derived landscape rejected',()=>{
 const {g}=fixture();g.enterJewelHoloNarrativeClock(start);task(g,'p232_2',2);g.advanceJewelHoloNarrativeClock(shift('p232-change'));
 const mutations=[p=>p.operations.receipts['p232-change'].dueMinute=3,
  p=>p.operations.receipts['p232-change'].roll100=100,
  p=>p.operations.holoNarrativeClock.landscape='alderaan',
  p=>p.operations.receipts['p232-change'].atMinute=999];
 for(const mutation of mutations){const bad=clone(g.state);mutation(bad.world.publishedAdventure);assert.throws(()=>validateState(bad));}
 const bad=clone(g.state);bad.world.clock.elapsedMinutes=999;assert.throws(()=>validateState(bad));
});
test('P232 physical exit ends narrative mode without escape, XP, NPC or encounter fabrication',()=>{
 const {g}=fixture();g.enterJewelHoloNarrativeClock(start);const xp=g.state.player.character.xp.total;
 atomic(g,()=>g.endJewelHoloNarrativeClock({actionId:'bad',gmExitConfirmed:false}));
 const out=g.endJewelHoloNarrativeClock({actionId:'p232-exit',gmExitConfirmed:true});
 assert.equal(out.holoNarrativeClock.ended,true);assert.equal(g.state.player.character.xp.total,xp);
 assert.equal(g.state.world.publishedAdventure.operations.holoPark??null,null);
 assert.equal(g.state.combat.active,false);atomic(g,()=>g.advanceJewelHoloNarrativeClock(shift('after-exit')));
});
test('P232 Android PT-BR UI, controller, bridge, offline bundle, save validation and version wired',()=>{
 const root=new URL('../app/src/main/assets/vtt/',import.meta.url);
 for(const [file,needle] of [['r46/final-runtime.js','data-p232-shift'],['r46/final-runtime.js','dois minutos reais'],
  ['js/core/controller.mjs','advanceJewelHoloNarrativeClock'],['js/core/uiBridge.mjs','JEWEL_HOLO_NARRATIVE_SHIFT_FAILED'],
  ['js/core/uiBridge.bundle.js','jewelHoloNarrativeClock.mjs'],['js/core/state.mjs','validateJewelHoloNarrativeClock'],
  ['js/adventure/jewelOfYavin.mjs','holoNarrativeClock: clone']])
  assert.ok(readFileSync(new URL(file,root),'utf8').includes(needle),file);
 const version=readFileSync(new URL('../app/build.gradle.kts',import.meta.url),'utf8');
 assert.ok(Number(version.match(/versionCode = (\d+)/)?.[1])>=10232,'P232 requires versionCode >=10232');
});

test('P232 published adventure status replays narrative receipts and keeps older P229-P231 receipt kinds valid',()=>{
 const {g}=fixture();g.enterJewelHoloNarrativeClock(start);task(g,'p232_status',2);g.advanceJewelHoloNarrativeClock(shift('p232-status-shift'));
 const status=getPublishedAdventureAdapter('jewel-of-yavin').status(g.state.world);
 assert.equal(status.holoNarrativeClock.shifts.length,1);assert.equal(status.holoNarrativeClock.landscape,'christophsis');
 assert.equal(validateState(g.state),true);
});
