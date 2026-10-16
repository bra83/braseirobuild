/** P232 — SWE09 The Jewel of Yavin, printed p.92, Holo-Park Planets.
 * In NARRATIVE (non-structured) play, scenery changes every two minutes of
 * task-ledger-backed elapsed world time. Does NOT advance time or resolve chase.
 * Structured combat's existing two-round Holo-Park mechanism stays separate.
 */
import {validateTaskTimeLedger} from '../world/taskTimeLedger.mjs';
import {jewelHoloLandscapeFromD100} from './jewelHoloPark.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const SOURCE='SWE09 The Jewel of Yavin p.92, Holo-Park Planets (narrative two minutes)';
const PREFIX='JEWEL_HOLO_NARRATIVE_';
const idOK=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const SCENES=['alderaan','christophsis','coruscant','kashyyyk','projectors_off'];
const allowed={START:['atMinute','gmParkArrivalConfirmed','gmNarrativeModeConfirmed','roll100','gm47Choice','landscape'],
 SHIFT:['atMinute','dueMinute','sequence','gmStillPresentConfirmed','roll100','gm47Choice','landscape'],
 END:['atMinute','gmExitConfirmed']};
const minutes=world=>{validateTaskTimeLedger(world);const m=world.clock.elapsedMinutes??0;
 if(!Number.isFinite(m)||m<0)throw Error('Relógio mundial inválido.');return m;};
const receipts=p=>Object.entries(p?.operations?.receipts??{}).filter(([,r])=>r?.kind?.startsWith(PREFIX));
const scene=(roll,choice)=>jewelHoloLandscapeFromD100(roll,choice);
function roll100(rng){const n=Number(rng());if(!Number.isFinite(n)||n<0||n>=1)throw Error('Fonte d100 inválida.');return Math.floor(n*100)+1;}
function preflight(world,combat,actionId){
 const p=world?.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit'||
    combat?.active||!idOK(actionId)||p.operations?.receipts?.[actionId])
  throw Error('Holo-Park narrativo exige aventura ativa, sem combate e ação única.');
 minutes(world);validateJewelHoloNarrativeClock(p,world);return p;
}
export function replayJewelHoloNarrativeClock(p){
 let start=null,shifts=[],end=null,lastAt=-1,landscape=null;
 for(const [id,r] of receipts(p)){
  const kind=r.kind.slice(PREFIX.length);
  if(p?.id!=='jewel-of-yavin'||!idOK(id)||r.id!==id||r.version!==1||r.source!==SOURCE||
     !allowed[kind]||Object.keys(r).some(k=>!['id','kind','version','source',...allowed[kind]].includes(k))||
     !Number.isFinite(r.atMinute)||r.atMinute<0||r.atMinute<lastAt)
   throw Error('Recibo do relógio narrativo adulterado ou fora de ordem.');
  lastAt=r.atMinute;
  if(kind==='START'){
   if(start||shifts.length||end||r.gmParkArrivalConfirmed!==true||r.gmNarrativeModeConfirmed!==true)
    throw Error('Entrada narrativa duplicada ou sem confirmação física.');
   landscape=scene(r.roll100,r.gm47Choice);
   if(r.landscape!==landscape)throw Error('Paisagem inicial adulterada.');start=clone(r);
  }else if(kind==='SHIFT'){
   const n=shifts.length+1,due=start?.atMinute+2*n;
   if(!start||end||n>500||r.sequence!==n||r.dueMinute!==due||r.atMinute<due||
      r.gmStillPresentConfirmed!==true)throw Error('Mudança sem dois minutos reais confirmados.');
   landscape=scene(r.roll100,r.gm47Choice);
   if(r.landscape!==landscape)throw Error('Paisagem narrativa adulterada.');shifts.push(clone(r));
  }else{
   if(!start||end||r.gmExitConfirmed!==true)throw Error('Saída narrativa não confirmada.');
   end=clone(r);
  }
 }
 if(!start)return null;
 return {version:1,start,shifts,landscape,ended:Boolean(end),end,
   nextDueMinute:end?null:start.atMinute+2*(shifts.length+1)};
}
export function validateJewelHoloNarrativeClock(p,world=null){
 const value=replayJewelHoloNarrativeClock(p);
 if(!eq(value,p?.operations?.holoNarrativeClock??null))throw Error('Estado narrativo não corresponde aos recibos.');
 if(value&&world){const now=minutes(world);
  if(receipts(p).some(([,r])=>r.atMinute>now))throw Error('Recibo narrativo registrado no futuro.');
 }
 return value;
}
function commit(w,r){const p=w.publishedAdventure;p.operations??={};p.operations.receipts??={};
 p.operations.receipts[r.id]=clone(r);p.operations.holoNarrativeClock=replayJewelHoloNarrativeClock(p);
 validateJewelHoloNarrativeClock(p,w);return {world:w,adjudication:r,holoNarrativeClock:clone(p.operations.holoNarrativeClock)};}
function rolled(details){const n=roll100(details.rng??Math.random),choice=n===47?(details.gm47Choice??null):null;
 return {roll100:n,gm47Choice:choice,landscape:scene(n,choice)};}
export function enterJewelHoloNarrativeClock(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details.actionId);
 if(p.operations?.holoPark||p.operations?.holoNarrativeClock||
    details.gmParkArrivalConfirmed!==true||details.gmNarrativeModeConfirmed!==true)
  throw Error('Confirme entrada narrativa física, sem Holo-Park estruturado.');
 const r={id:details.actionId,kind:PREFIX+'START',version:1,source:SOURCE,atMinute:minutes(w),
  gmParkArrivalConfirmed:true,gmNarrativeModeConfirmed:true,...rolled(details)};
 return commit(w,r);
}
export function advanceJewelHoloNarrativeClock(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details.actionId),prior=p.operations?.holoNarrativeClock;
 if(!prior||prior.ended||details.gmStillPresentConfirmed!==true||prior.shifts.length>=500)
  throw Error('Confirme presença física no Holo-Park narrativo ativo.');
 const atMinute=minutes(w),dueMinute=prior.nextDueMinute;
 if(atMinute<dueMinute)throw Error('Ainda não transcorreram dois minutos reais de tarefas confirmadas.');
 const r={id:details.actionId,kind:PREFIX+'SHIFT',version:1,source:SOURCE,
  atMinute,dueMinute,sequence:prior.shifts.length+1,gmStillPresentConfirmed:true,...rolled(details)};
 return commit(w,r);
}
export function endJewelHoloNarrativeClock(world,combat,details={}){
 const w=clone(world),p=preflight(w,combat,details.actionId),prior=p.operations?.holoNarrativeClock;
 if(!prior||prior.ended||details.gmExitConfirmed!==true)throw Error('Saída física do Holo-Park não confirmada.');
 const r={id:details.actionId,kind:PREFIX+'END',version:1,source:SOURCE,
  atMinute:minutes(w),gmExitConfirmed:true};
 return commit(w,r);
}
