import fs from 'node:fs';
import { createRandomCharacter } from './lib/characterFactory';
import { createDefaultMasterState, normalizeMasterState } from './lib/master/state';
import { applyMasterCommands } from './lib/master/commands';
import { advanceWorldClock } from './lib/master/worldClock';
import { advanceWeatherState } from './lib/autonomy/weatherStateMachine';

type Session = { campaign:any; characters:any[]; npcs:any[] };
type Check = { id:string; ok:boolean; detail:string; group:string };
const checks:Check[]=[];
const failures:string[]=[];
const notes:string[]=[];
let assertions=0;

function check(group:string,id:string,ok:boolean,detail:string){
  assertions++;
  checks.push({group,id,ok,detail});
  if(!ok) failures.push(group+' / '+id+': '+detail);
}

function clockKey(c:any){return [c.year,c.season,c.day,c.hour,c.minute,c.elapsedMinutes].join('|');}
function heroState(h:any){
  const a=h.sheet.attributes;
  return {endurance:a.enduranceCurrent,enduranceMax:a.enduranceMax,fatigue:a.fatigue,supplies:a.supplies,hope:a.hopeCurrent,shadow:a.shadow,weary:a.weary,miserable:a.miserable,wounded:a.wounded,load:a.load};
}
function byId(s:Session,id:string){const h=s.characters.find(x=>x.id===id); if(!h) throw new Error('hero missing '+id); return h;}
function master(s:Session){return normalizeMasterState(s.campaign);}
function apply(s:Session,commands:any[],allowReject=false){
  const r=applyMasterCommands(s.campaign,s.characters,s.npcs,commands as any);
  s.campaign=r.campaign; s.characters=r.characters; s.npcs=r.npcs;
  if(!allowReject && r.rejected.length) throw new Error(r.rejected.map((x:any)=>x.reason).join(' | '));
  return r;
}
function roll(success:boolean,icons=0,feat=7){
  return {type:'one-ring',notation:'AUDIT',results:[],modifier:0,total:success?18:4,timestamp:Date.now(),featResult:feat,featResults:[feat],successResults:[],targetNumber:14,success,successLevel:success?1:0,tengwarCount:icons};
}

let sessionSeq=0;
function makeSession(partySize=4):Session{
  sessionSeq++;
  const characters:any[]=[];
  const names:string[]=[];
  for(let i=0;i<partySize;i++){
    const c:any=createRandomCharacter(names);
    names.push(c.sheet.identity.name||c.name);
    c.id='audit_'+sessionSeq+'_hero_'+i;
    c.name=c.sheet.identity.name||('Hero '+i);
    c.sheet.attributes.enduranceCurrent=c.sheet.attributes.enduranceMax;
    c.sheet.attributes.fatigue=0;
    c.sheet.attributes.supplies=100;
    c.sheet.attributes.shadow=0;
    c.sheet.attributes.wounded=false;
    c.sheet.attributes.weary=false;
    c.sheet.attributes.miserable=false;
    c.sheet.other.virtues='';
    c.sheet.other.rewards='';
    c.sheet.other.flaws='';
    characters.push(c);
  }
  const now=Date.now();
  const campaign:any={
    id:'audit_campaign_'+sessionSeq,name:'AUDIT '+sessionSeq,
    systemProfileId:'the-one-ring-2e',settingProfileId:'middle-earth-third-age',
    rulesProfileName:'O Um Anel 2e — Terra-média, Terceira Era',rulesEdition:'2e',
    characterSheet:structuredClone(characters[0].sheet),
    activeCharacterId:characters[0].id,partyCharacterIds:characters.map(c=>c.id),
    currentLocation:'Audit Start',currentChapter:'Prólogo',campaignSummary:'audit',
    recentEvents:'',clues:'',mysteries:'',quests:'',worldFlags:{},presentNpcIds:[],
    history:[],pendingDiceRoll:null,pendingTest:null,events:[],firstImageGenerated:false,
    schemaVersion:350,revision:1,createdAt:now,updatedAt:now,
  };
  campaign.masterState=createDefaultMasterState(campaign);
  campaign.masterState.position={...campaign.masterState.position,currentHexId:'H0',locationName:'Audit Start',routeHexIds:['H0']};
  campaign.masterState.worldClock={year:2965,season:'spring',day:1,hour:8,minute:0,elapsedMinutes:0};
  campaign.masterState.company={...campaign.masterState.company,eyeAwarenessEnabled:true,eyeAwareness:0,eyeAwarenessInitial:0,eyeHuntThreshold:16,eyeRevelationPending:false};
  return {campaign,characters,npcs:[]};
}

function roles(s:Session){
  const ids=s.characters.map(x=>x.id);
  return {guide:ids[0],scout:ids[Math.min(1,ids.length-1)],hunter:ids[Math.min(2,ids.length-1)],lookout:ids[Math.min(3,ids.length-1)]};
}

function startJourney(s:Session,route:string[],opts:{mounted?:boolean;forcedMarch?:boolean}={}){
  apply(s,[{type:'player_start_journey',destinationHexId:route.at(-1),roles:roles(s),routeHexIds:route,mounted:Boolean(opts.mounted),forcedMarch:Boolean(opts.forcedMarch),reason:'AUDIT start'}]);
}
function march(s:Session,success=true,icons=0){
  const guide=roles(s).guide;
  apply(s,[
    {type:'player_record_journey_roll',kind:'march',targetCharacterId:guide,skill:'Viagem',targetNumber:14,roll:roll(success,icons,7),mountVigour:0,reason:'AUDIT march'},
    {type:'player_confirm_journey_march',reason:'AUDIT confirm march'},
  ]);
}
function move(s:Session,hex:string,terrain='road'){
  apply(s,[{type:'move_party',toHexId:hex,locationName:'Local '+hex,distanceKm:10,terrain,reason:'AUDIT move'}]);
}
function finishNoFatigue(s:Session){
  apply(s,[{type:'player_complete_journey',reason:'AUDIT complete'}]);
}

function groupClockAndResources(iter:number){
  const s=makeSession(4);
  const initial=s.characters.map(heroState);
  let expected=structuredClone(master(s).worldClock);
  let total=0;
  for(let turn=0;turn<36;turn++){
    const minutes=20+((iter*53+turn*37)%101);
    total+=minutes;
    expected=advanceWorldClock(expected,minutes).clock;
    apply(s,[{type:'advance_time',minutes,reason:'AUDIT turn '+turn,source:'AUDIT'}]);
    check('clock','chunk-'+iter+'-'+turn,clockKey(master(s).worldClock)===clockKey(expected),'expected '+clockKey(expected)+' got '+clockKey(master(s).worldClock));
  }
  s.characters.forEach((h:any,idx:number)=>{
    const now=heroState(h),before=initial[idx];
    check('resource-isolation','time-endurance-'+iter+'-'+idx,now.endurance===before.endurance,'endurance '+before.endurance+' -> '+now.endurance+' after '+total+' min');
    check('resource-isolation','time-fatigue-'+iter+'-'+idx,now.fatigue===before.fatigue,'fatigue '+before.fatigue+' -> '+now.fatigue);
    check('resource-isolation','time-supplies-'+iter+'-'+idx,now.supplies===before.supplies,'supplies '+before.supplies+' -> '+now.supplies);
    check('resource-isolation','time-hope-'+iter+'-'+idx,now.hope===before.hope,'hope '+before.hope+' -> '+now.hope);
  });
  check('eye','time-does-not-raise-eye-'+iter,master(s).company.eyeAwareness===0,'Eye='+master(s).company.eyeAwareness);
}

function groupRest(iter:number){
  const s=makeSession(2);
  const id=s.characters[0].id;
  const h0=byId(s,id);
  const max=h0.sheet.attributes.enduranceMax;
  const strength=h0.sheet.attributes.strength;
  apply(s,[
    {type:'adjust_endurance',targetCharacterId:id,amount:-10,reason:'AUDIT damage'},
    {type:'adjust_fatigue',targetCharacterId:id,amount:3,reason:'AUDIT fatigue'},
    {type:'adjust_supplies',targetCharacterId:id,amount:-30,reason:'AUDIT supplies'},
  ]);
  const before=heroState(byId(s,id));
  const clockBefore=structuredClone(master(s).worldClock);
  apply(s,[{type:'resolve_rest',kind:'short',safeRefuge:false,reason:'AUDIT short',source:'AUDIT'}]);
  const afterShort=heroState(byId(s,id));
  const expectedClock=advanceWorldClock(clockBefore,60).clock;
  check('rest','short-clock-'+iter,clockKey(master(s).worldClock)===clockKey(expectedClock),'short rest clock '+clockKey(master(s).worldClock));
  check('rest','short-endurance-'+iter,afterShort.endurance===Math.min(max,before.endurance+strength),'endurance '+before.endurance+' -> '+afterShort.endurance+' strength='+strength);
  check('rest','short-fatigue-stable-'+iter,afterShort.fatigue===before.fatigue,'fatigue '+before.fatigue+' -> '+afterShort.fatigue);
  check('rest','short-supplies-stable-'+iter,afterShort.supplies===before.supplies,'supplies '+before.supplies+' -> '+afterShort.supplies);
  const repeat=apply(s,[{type:'resolve_rest',kind:'short',safeRefuge:false,reason:'AUDIT repeat short'}],true);
  check('rest','short-daily-gate-'+iter,repeat.rejected.length===1,'rejected='+repeat.rejected.map((x:any)=>x.reason).join('; '));

  apply(s,[{type:'advance_time',minutes:23*60,reason:'AUDIT next day'}]);
  const longStartClock=structuredClone(master(s).worldClock);
  const preLong=heroState(byId(s,id));
  byId(s,id).sheet.attributes.hopeCurrent=0;
  s.campaign.characterSheet=structuredClone(byId(s,s.campaign.activeCharacterId).sheet);
  apply(s,[{type:'resolve_rest',kind:'long',safeRefuge:false,reason:'AUDIT long'}]);
  const postLong=heroState(byId(s,id));
  check('rest','long-clock-'+iter,clockKey(master(s).worldClock)===clockKey(advanceWorldClock(longStartClock,480).clock),'long rest clock '+clockKey(master(s).worldClock));
  check('rest','long-endurance-full-'+iter,postLong.endurance===postLong.enduranceMax,'endurance '+postLong.endurance+'/'+postLong.enduranceMax);
  check('rest','long-hope-floor-'+iter,postLong.hope===1,'hope='+postLong.hope);
  check('rest','long-fatigue-no-refuge-'+iter,postLong.fatigue===preLong.fatigue,'fatigue '+preLong.fatigue+' -> '+postLong.fatigue);
  check('rest','long-supplies-stable-'+iter,postLong.supplies===preLong.supplies,'supplies '+preLong.supplies+' -> '+postLong.supplies);

  const refuge=makeSession(1); const rid=refuge.characters[0].id;
  apply(refuge,[{type:'adjust_fatigue',targetCharacterId:rid,amount:4,reason:'AUDIT fatigue'}]);
  const f0=heroState(byId(refuge,rid)).fatigue;
  apply(refuge,[{type:'resolve_rest',kind:'long',safeRefuge:true,reason:'AUDIT refuge'}]);
  const f1=heroState(byId(refuge,rid)).fatigue;
  check('rest','safe-refuge-fatigue-'+iter,f1===Math.max(0,f0-1),'fatigue '+f0+' -> '+f1);
}

function groupSimpleJourney(iter:number){
  for(const variant of [
    {name:'normal',mounted:false,forcedMarch:false,expectedDays:2},
    {name:'mounted',mounted:true,forcedMarch:false,expectedDays:1},
    {name:'forced',mounted:false,forcedMarch:true,expectedDays:1},
  ]){
    const s=makeSession(4);
    const startClock=structuredClone(master(s).worldClock);
    const before=s.characters.map(heroState);
    startJourney(s,['H1','H2'],variant);
    march(s,true,0);
    move(s,'H1','road');
    move(s,'H2','road');
    check('journey-time',variant.name+'-days-'+iter,master(s).worldClock.elapsedMinutes-startClock.elapsedMinutes===variant.expectedDays*1440,'elapsed delta='+(master(s).worldClock.elapsedMinutes-startClock.elapsedMinutes));
    check('journey-state',variant.name+'-await-end-'+iter,master(s).journey.awaitingEndRoll===true,'awaitingEndRoll='+master(s).journey.awaitingEndRoll);
    const pendingTotal=Object.values(master(s).journey.travelFatigueByHero||{}).reduce((a:number,b:any)=>a+Number(b||0),0);
    const expectedPending=variant.forcedMarch?s.characters.length:0;
    check('journey-state',variant.name+'-pending-fatigue-'+iter,pendingTotal===expectedPending,'pending='+JSON.stringify(master(s).journey.travelFatigueByHero||{})+' expected total='+expectedPending);
    if(pendingTotal>0){
      for(const h of s.characters){
        if(Number(master(s).journey.travelFatigueByHero?.[h.id]||0)>0){
          apply(s,[{type:'player_record_journey_roll',kind:'end',targetCharacterId:h.id,skill:'Viagem',targetNumber:14,roll:roll(true,0,7),mountVigour:0,reason:'AUDIT forced end roll'}]);
        }
      }
    }
    finishNoFatigue(s);
    check('journey-state',variant.name+'-completed-'+iter,master(s).journey.status==='completed'&&master(s).activeMode==='adventure','status='+master(s).journey.status+' mode='+master(s).activeMode);
    s.characters.forEach((h:any,idx:number)=>{
      const now=heroState(h);
      check('journey-resource-isolation',variant.name+'-endurance-'+iter+'-'+idx,now.endurance===before[idx].endurance,'endurance '+before[idx].endurance+' -> '+now.endurance);
      check('journey-resource-isolation',variant.name+'-supplies-'+iter+'-'+idx,now.supplies===before[idx].supplies,'supplies '+before[idx].supplies+' -> '+now.supplies);
      check('journey-resource-isolation',variant.name+'-fatigue-'+iter+'-'+idx,now.fatigue===before[idx].fatigue,'fatigue '+before[idx].fatigue+' -> '+now.fatigue);
    });
    check('eye',variant.name+'-travel-does-not-raise-'+iter,master(s).company.eyeAwareness===0,'Eye='+master(s).company.eyeAwareness);
  }

  const difficult=makeSession(4);
  startJourney(difficult,['D1']);
  march(difficult,true,0);
  const c0=master(difficult).worldClock.elapsedMinutes;
  move(difficult,'D1','hills');
  check('journey-time','difficult-terrain-'+iter,master(difficult).worldClock.elapsedMinutes-c0===2880,'difficult delta='+(master(difficult).worldClock.elapsedMinutes-c0));
}

function groupJourneyEvent(iter:number){
  const s=makeSession(4);
  const initial=s.characters.map(heroState);
  startJourney(s,['E1','E2','E3','E4']);
  march(s,true,0);
  move(s,'E1','road');
  move(s,'E2','hills');
  move(s,'E3','forest');
  check('journey-event','event-required-'+iter,master(s).journey.awaitingEventGeneration===true,'awaitingEventGeneration='+master(s).journey.awaitingEventGeneration);
  const scout=roles(s).scout;
  apply(s,[{type:'player_generate_journey_event',region:'wild',targetDie:1,featResults:[4],terrain:'forest',reason:'AUDIT event'}]);
  check('journey-event','mishap-generated-'+iter,master(s).journey.pendingEvent?.eventType==='mishap','event='+JSON.stringify(master(s).journey.pendingEvent));
  apply(s,[
    {type:'player_record_journey_roll',kind:'event',targetCharacterId:scout,skill:'Exploração',targetNumber:14,roll:roll(false,0,7),mountVigour:0,reason:'AUDIT failed event'},
    {type:'player_confirm_journey_event',reason:'AUDIT confirm event'},
  ]);
  const pending:any=master(s).journey.travelFatigueByHero||{};
  s.characters.forEach((h:any,idx:number)=>{
    const expected=h.id===scout?3:2;
    check('journey-event','pending-fatigue-'+iter+'-'+idx,Number(pending[h.id]||0)===expected,'pending '+h.id+'='+pending[h.id]+' expected '+expected);
    check('journey-event','sheet-fatigue-deferred-'+iter+'-'+idx,heroState(h).fatigue===initial[idx].fatigue,'sheet fatigue='+heroState(h).fatigue);
  });
  check('journey-event','mishap-adds-day-'+iter,master(s).journey.durationAdjustmentDays===1,'durationAdjustmentDays='+master(s).journey.durationAdjustmentDays);
  march(s,true,0);
  move(s,'E4','road');
  check('journey-event','destination-after-event-'+iter,master(s).journey.awaitingEndRoll===true,'awaitingEndRoll='+master(s).journey.awaitingEndRoll);
  for(const h of s.characters){
    apply(s,[{type:'player_record_journey_roll',kind:'end',targetCharacterId:h.id,skill:'Viagem',targetNumber:14,roll:roll(true,0,7),mountVigour:0,reason:'AUDIT end roll'}]);
  }
  apply(s,[{type:'player_complete_journey',reason:'AUDIT complete event journey'}]);
  s.characters.forEach((h:any,idx:number)=>{
    const expected=h.id===scout?2:1;
    check('journey-event','final-fatigue-'+iter+'-'+idx,heroState(h).fatigue===initial[idx].fatigue+expected,'final fatigue='+heroState(h).fatigue+' expected +'+expected);
    check('journey-event','supplies-stable-'+iter+'-'+idx,heroState(h).supplies===initial[idx].supplies,'supplies '+initial[idx].supplies+' -> '+heroState(h).supplies);
  });
  check('journey-event','pending-cleared-'+iter,Object.keys(master(s).journey.travelFatigueByHero||{}).length===0,'pending='+JSON.stringify(master(s).journey.travelFatigueByHero||{}));
}

function groupSuppliesAndEndurance(iter:number){
  const s=makeSession(1); const id=s.characters[0].id;
  apply(s,[{type:'adjust_supplies',targetCharacterId:id,amount:-150,reason:'AUDIT clamp low'}]);
  check('supplies','clamp-zero-'+iter,heroState(byId(s,id)).supplies===0,'supplies='+heroState(byId(s,id)).supplies);
  apply(s,[{type:'adjust_supplies',targetCharacterId:id,amount:250,reason:'AUDIT clamp high'}]);
  check('supplies','clamp-hundred-'+iter,heroState(byId(s,id)).supplies===100,'supplies='+heroState(byId(s,id)).supplies);

  const h=byId(s,id);
  h.sheet.attributes.load=Math.max(3,Number(h.sheet.attributes.load)||0);
  apply(s,[{type:'adjust_fatigue',targetCharacterId:id,amount:4,reason:'AUDIT fatigue'}]);
  const threshold=heroState(byId(s,id)).load+heroState(byId(s,id)).fatigue;
  const current=heroState(byId(s,id)).endurance;
  apply(s,[{type:'adjust_endurance',targetCharacterId:id,amount:-(Math.max(0,current-threshold)),reason:'AUDIT weary threshold'}]);
  const at=heroState(byId(s,id));
  check('endurance','weary-at-threshold-'+iter,at.endurance<=at.load+at.fatigue && at.weary===true,'endurance='+at.endurance+' load='+at.load+' fatigue='+at.fatigue+' weary='+at.weary);
  apply(s,[{type:'adjust_endurance',targetCharacterId:id,amount:-999,reason:'AUDIT clamp'}]);
  check('endurance','clamp-zero-'+iter,heroState(byId(s,id)).endurance===0,'endurance='+heroState(byId(s,id)).endurance);
  apply(s,[{type:'adjust_endurance',targetCharacterId:id,amount:999,reason:'AUDIT clamp max'}]);
  check('endurance','clamp-max-'+iter,heroState(byId(s,id)).endurance===heroState(byId(s,id)).enduranceMax,'endurance='+heroState(byId(s,id)).endurance+'/'+heroState(byId(s,id)).enduranceMax);
}

function groupEye(iter:number){
  const s=makeSession(1); const id=s.characters[0].id;
  apply(s,[{type:'advance_time',minutes:3*1440,reason:'AUDIT days'}]);
  check('eye','days-stable-'+iter,master(s).company.eyeAwareness===0,'Eye='+master(s).company.eyeAwareness);
  apply(s,[{type:'adjust_shadow',targetCharacterId:id,amount:2,reason:'AUDIT shadow'}]);
  check('eye','shadow-gain-'+iter,master(s).company.eyeAwareness===2,'Eye='+master(s).company.eyeAwareness+' shadow='+heroState(byId(s,id)).shadow);
  apply(s,[{type:'adjust_eye_awareness',amount:3,source:'magic-powerful',reason:'AUDIT explicit magic'}]);
  check('eye','explicit-magic-'+iter,master(s).company.eyeAwareness===5,'Eye='+master(s).company.eyeAwareness);
}

function groupWeather(){
  const possible:any[]=[
    {id:'rain',kind:'rain',intensity:'medium',probability:.4,reason:'audit rain',eligible:true,stage:'indications',stageStep:0,activeStepsRemaining:0},
    {id:'clear',kind:'clear',intensity:'light',probability:.6,reason:'audit clear',eligible:true,stage:'stable',stageStep:0,activeStepsRemaining:0},
  ];
  let w:any=structuredClone(possible[0]);
  const seq=[w.stage];
  for(let i=0;i<7;i++){w=advanceWeatherState(w,possible,'audit-weather-'+i);seq.push(w.stage);}
  check('weather','movement-state-machine',seq[0]==='indications'&&seq[1]==='forming'&&seq[2]==='active','sequence='+seq.join('>'));

  const app=fs.readFileSync('App.tsx','utf8');
  const env=fs.readFileSync('lib/autonomy/environment.ts','utf8');
  check('weather','movement-integration-present',app.includes('prepareEnvironmentAdvance(summarized, locationEvent.id)')||app.includes('prepareEnvironmentAdvance(reconciled.campaign, locationEvent.id)'),'movement calls environment advance');
  check('weather','weather-advance-is-movement-gated',env.includes('Uma chamada equivale exatamente a um deslocamento')&&env.includes('processedTravelActionIds'),'environment weather is explicitly movement/idempotency gated');
  check('weather','clock-reconcile-does-not-advance',env.includes('Reconciliamos posição e relógio sem avançar o clima'),'clock-only reconcile explicitly does not advance weather');
  notes.push('Clima: a arquitetura atual avança a máquina meteorológica por deslocamento confirmado, não por simples passagem de horas/dias. O teste registra isso como comportamento atual, não como correção.');
}

function seasonBoundary(){
  const input:any={year:2965,season:'spring',day:92,hour:23,minute:30,elapsedMinutes:0};
  const out=advanceWorldClock(input,60).clock;
  check('clock','season-boundary-spring-summer',out.day===93&&out.season==='summer'&&out.hour===0&&out.minute===30,'got '+clockKey(out));
  const year:any={year:2965,season:'winter',day:365,hour:23,minute:30,elapsedMinutes:0};
  const y=advanceWorldClock(year,60).clock;
  check('clock','year-boundary',y.year===2966&&y.day===1&&y.season==='spring'&&y.hour===0&&y.minute===30,'got '+clockKey(y));
}

function runScenario(label:string,fn:()=>void){
  try{fn();}
  catch(error){check('scenario-crash',label,false,error instanceof Error?error.message:String(error));}
}
for(let i=0;i<8;i++) runScenario('clock-'+i,()=>groupClockAndResources(i));
for(let i=0;i<6;i++) runScenario('rest-'+i,()=>groupRest(i));
for(let i=0;i<6;i++) runScenario('simple-journey-'+i,()=>groupSimpleJourney(i));
for(let i=0;i<6;i++) runScenario('event-journey-'+i,()=>groupJourneyEvent(i));
for(let i=0;i<6;i++) runScenario('resources-'+i,()=>groupSuppliesAndEndurance(i));
for(let i=0;i<6;i++) runScenario('eye-'+i,()=>groupEye(i));
runScenario('weather',groupWeather);
runScenario('season-boundaries',seasonBoundary);

const byGroup=Object.fromEntries(Array.from(new Set(checks.map(c=>c.group))).map(group=>{
  const all=checks.filter(c=>c.group===group); return [group,{total:all.length,pass:all.filter(c=>c.ok).length,fail:all.filter(c=>!c.ok).length}];
}));
const report={
  generatedAt:new Date().toISOString(),
  sessionsSimulated:sessionSeq,
  assertions,
  passed:checks.filter(c=>c.ok).length,
  failed:checks.filter(c=>!c.ok).length,
  byGroup,
  failures,
  notes,
  checks,
};
fs.writeFileSync('OUA_49202_TURN_FLOW_AUDIT.json',JSON.stringify(report,null,2));
const md=[
  '# OUA 4.9.202 — auditoria de fluxo de turnos (SEM CORREÇÕES)',
  '',
  '- Partidas/sessões simuladas: **'+report.sessionsSimulated+'**',
  '- Asserções: **'+assertions+'**',
  '- PASS: **'+report.passed+'**',
  '- FAIL: **'+report.failed+'**',
  '',
  '## Resumo por grupo',
  ...Object.entries(byGroup).map(([k,v]:any)=>'- '+k+': '+v.pass+'/'+v.total+' PASS; '+v.fail+' FAIL'),
  '',
  '## Falhas',
  ...(failures.length?failures.map(x=>'- '+x):['- Nenhuma']),
  '',
  '## Observações',
  ...notes.map(x=>'- '+x),
].join('\n');
fs.writeFileSync('OUA_49202_TURN_FLOW_AUDIT.md',md);
console.log(md);
console.log('\nJSON_REPORT=OUA_49202_TURN_FLOW_AUDIT.json');
process.exitCode=failures.length?2:0;
