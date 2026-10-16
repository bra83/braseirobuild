/** SWE09 The Jewel of Yavin, Episode III "Countdown to Disaster", printed pp. 89–90.
 * Persistent source-backed thirty-second airspeeder trap. Each choice is a real
 * narrative-dice roll; the timer is a scene deadline, NOT fabricated world time.
 * Falling damage, soak and GM Destiny intervention remain explicit pending
 * adjudications, never silently deducted or awarded.
 */
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const idOk=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const STEPS={detect:{mecanica:2,computadoras:2,percepcion:3},disarm:{mecanica:3,frialdad:3},steer:{pilotar_vehiculo_planetario:2},jump:{atletismo:1,coordinacion:1},secure_jewel:{coordinacion:3}};
const keys=['id','kind','step','skill','actorId','characteristicKey','characteristicRank','skillRank','difficulty','pool','roll','success','nearPlatform','jewelHeld','jewelStowed','gmSuspicionConfirmed'];
const init=()=>({status:'UNEXAMINED',timerSeconds:30,detected:false,steered:false,escaped:false,jewelSecured:null,blastDamageBeforeSoak:0,fallDamagePending:false,history:[]});
function advance(state,r){
 const n=clone(state),step=r.step,success=r.success;
 if(step==='detect'){
   if(!['UNEXAMINED','SUSPECTED'].includes(n.status))throw Error('Detecção fora de ordem.');
   if(n.history.some(x=>x.step==='detect'&&x.actorId===r.actorId))throw Error('Personagem já examinou este speeder.');
   n.detected ||= success;n.status=n.detected?'DETECTED':'SUSPECTED';
 }else if(step==='disarm'){
   if(!n.detected||!['DETECTED','SUSPECTED'].includes(n.status))throw Error('Desarme exige bomba detectada.');
   n.status=success?'DISARMED':'EXPLODED';
   if(!success){n.blastDamageBeforeSoak=10;n.fallDamagePending=true;}
 }else if(step==='steer'){
   if(!n.detected&&!r.gmSuspicionConfirmed)throw Error('Direção de emergência exige detecção ou alerta confirmado pelo Mestre.');
   if(!['UNEXAMINED','DETECTED','SUSPECTED'].includes(n.status))throw Error('Direção fora da sequência.');
   n.steered=success;n.status=success?'NEAR_PLATFORM':'IN_FLIGHT';
 }else if(step==='jump'){
   if(!['DETECTED','SUSPECTED','NEAR_PLATFORM','IN_FLIGHT'].includes(n.status))throw Error('Salto fora da sequência.');
   if(r.difficulty!==(n.steered?1:2))throw Error('Dificuldade do salto não corresponde à posição do speeder.');
   n.escaped=success;n.status=success?'JUMPED':'FELL';
   if(!success)n.fallDamagePending=true;
   if(r.jewelHeld&&!r.jewelStowed&&success){n.jewelSecured=null;n.status='JEWEL_GRIP_PENDING';}
   else if(r.jewelHeld)n.jewelSecured=r.jewelStowed&&success?true:null;
 }else if(step==='secure_jewel'){
   if(n.status!=='JEWEL_GRIP_PENDING'||!r.jewelHeld||r.jewelStowed)throw Error('Teste de segurar a joia fora da sequência.');
   n.jewelSecured=success;n.status=success?'JUMPED_WITH_JEWEL':'JEWEL_DROPPED';
 }else throw Error('Etapa de fuga desconhecida.');
 n.history.push(r);return n;
}
function receipt({actionId,step,skill,character,rng,state,nearPlatform=false,jewelHeld=false,jewelStowed=false,gmSuspicionConfirmed=false}){
 if(!idOk(actionId)||!Object.hasOwn(STEPS,step)||!Object.hasOwn(STEPS[step],skill))throw Error('Ação ou perícia da fuga inválida.');
 const characteristicKey=SKILLS[skill],characteristicRank=character?.characteristics?.[characteristicKey],skillRank=character?.skills?.[skill];
 if(!Number.isInteger(characteristicRank)||characteristicRank<0||characteristicRank>7||!Number.isInteger(skillRank)||skillRank<0||skillRank>7)throw Error('Ficha de personagem inválida.');
 const difficulty=step==='jump'?(state.steered?1:2):STEPS[step][skill];
 const pool={...buildPositivePool(characteristicRank,skillRank),difficulty,challenge:0,boost:0,setback:0};
 const roll=rollPool(pool,rng);
 return {id:actionId,kind:'JEWEL_ESCAPE_BOMB',step,skill,actorId:String(character.id??character.name??'jogador'),characteristicKey,characteristicRank,skillRank,difficulty,pool,roll,success:roll.success,
  nearPlatform:step==='jump'?state.steered:false,jewelHeld:step==='jump'||step==='secure_jewel'?jewelHeld:false,
  jewelStowed:step==='jump'?jewelStowed:false,gmSuspicionConfirmed:step==='steer'?gmSuspicionConfirmed:false};
}
function verify(r,state){
 if(!r||Object.keys(r).some(k=>!keys.includes(k))||r.kind!=='JEWEL_ESCAPE_BOMB'||!idOk(r.id)||!Object.hasOwn(STEPS,r.step)||!Object.hasOwn(STEPS[r.step],r.skill)||
   !idOk(r.actorId)||r.characteristicKey!==SKILLS[r.skill]||![r.characteristicRank,r.skillRank].every(x=>Number.isInteger(x)&&x>=0&&x<=7)||
   !['nearPlatform','jewelHeld','jewelStowed','gmSuspicionConfirmed'].every(k=>typeof r[k]==='boolean'))throw Error('Recibo da fuga adulterado.');
 const difficulty=r.step==='jump'?(state.steered?1:2):STEPS[r.step][r.skill];
 const pool={...buildPositivePool(r.characteristicRank,r.skillRank),difficulty,challenge:0,boost:0,setback:0};
 if(r.difficulty!==difficulty||!eq(r.pool,pool)||!eq(r.roll?.pool,pool)||r.nearPlatform!==(r.step==='jump'&&state.steered)||
   (r.step!=='jump'&&r.step!=='secure_jewel'&&r.jewelHeld)||
   (r.step!=='jump'&&r.jewelStowed)||(r.step!=='steer'&&r.gmSuspicionConfirmed))throw Error('Pool ou contexto da fuga adulterado.');
 const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let i=0;
 for(const type of ['ability','proficiency','difficulty','challenge','boost','setback'])for(let j=0;j<pool[type];j++){
   const f=r.roll?.faces?.[i++],valid=diceFacesForTests[type];
   if(!f||f.type!==type||!Number.isInteger(f.index)||f.index<0||f.index>=valid.length||!eq(f.face,valid[f.index]))throw Error('Faces da fuga adulteradas.');
   for(const [key,n] of Object.entries(f.face))raw[key]+=n;
 }
 if(!Array.isArray(r.roll?.faces)||r.roll.faces.length!==i||!eq({...resolveSymbols(raw),faces:r.roll.faces,pool},r.roll)||r.success!==r.roll.success)
   throw Error('Resultado da fuga adulterado.');
 return advance(state,r);
}
export function replayJewelEscapeBomb(progress){
 const receipts=progress?.operations?.receipts??{},entries=Object.entries(receipts).filter(([,r])=>r?.kind==='JEWEL_ESCAPE_BOMB');
 if(!entries.length)return null;
 let state=init();
 for(const [id,r] of entries){if(id!==r.id)throw Error('Identidade de recibo da fuga adulterada.');state=verify(r,state);}
 return state;
}
export function validateJewelEscapeBomb(progress){
 const actual=progress?.operations?.escapeBomb??null,expected=replayJewelEscapeBomb(progress);
 if(!eq(actual,expected))throw Error('Estado da bomba diverge dos recibos.');return expected;
}
export function performJewelEscapeBomb(world,character,details={}){
 const w=clone(world??{}),p=w.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit')throw Error('A armadilha do airspeeder só pode ser resolvida na fuga de Cloud City.');
 p.operations??={};p.operations.receipts??={};
 if(!idOk(details.actionId)||Object.hasOwn(p.operations.receipts,details.actionId))throw Error('Identificador duplicado ou inválido.');
 const state=validateJewelEscapeBomb(p)||init();
 const r=receipt({...details,character,state,rng:details.rng??Math.random});
 const next=verify(r,state);
 p.operations.receipts[r.id]=r;p.operations.escapeBomb=next;p.updatedAt=new Date().toISOString();
 validateJewelEscapeBomb(p);
 return {world:w,adjudication:clone(r),escapeBomb:clone(next),progress:clone(p)};
}
