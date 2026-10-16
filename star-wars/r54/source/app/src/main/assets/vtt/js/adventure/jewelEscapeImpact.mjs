/** SWE09 printed p.90 + EotE Core Table 6-9: actual wounds and strain.
 * GM must confirm fall range; no distance, action duration or Destiny use inferred.
 * Long/Extreme are intentionally blocked until their mandatory critical adjudication.
 */
import {applyDamage,sufferStrain} from '../rules/health.mjs';
import {buildPositivePool,rollPool,resolveSymbols,diceFacesForTests} from '../rules/narrativeDice.mjs';
import {SKILLS} from '../rules/character.mjs';
import {validateJewelEscapeBomb} from './jewelEscapeBomb.mjs';
import {applyForcedFall,fallingProfile,reduceFallRange} from '../rules/environment.mjs';
const clone=structuredClone,eq=(a,b)=>JSON.stringify(a)===JSON.stringify(b);
const validId=x=>typeof x==='string'&&/^[A-Za-z0-9][A-Za-z0-9:_-]{0,79}$/.test(x)&&!['__proto__','constructor','prototype'].includes(x);
const RANGES=Object.freeze({short:{damage:10,strain:10},medium:{damage:30,strain:20}});
const RECEIPT_KEYS=['impactVersion','gmTriumphReduce','effectiveFallRange','forcedCriticalCode','id','kind','mode','actorId','fallRange','gmConfirmed','skill','characteristicKey','characteristicRank','skillRank','pool','roll','before','after','rngDraws','rawDamage','rawStrain','reducedDamage','reducedStrain','soak','woundsAdded','strainAdded'];
const init=()=>({blastApplied:false,fallApplied:false,history:[]});
function calculate(r){
 if(!r.before||String(r.before.id??r.before.name)!==r.actorId||!r.before.derived?.wounds||!r.before.derived?.strain)throw Error('Ficha anterior ao impacto inválida.');
 let rawDamage=10,rawStrain=0,reducedDamage=0,reducedStrain=0;
 let forced=false,effectiveRange=null;
 if(r.mode==='fall'){
  if(!['short','medium','long','extreme'].includes(r.fallRange)||r.gmConfirmed!==true||!['atletismo','coordinacion'].includes(r.skill)||r.characteristicKey!==SKILLS[r.skill]||
    r.characteristicRank!==r.before.characteristics?.[r.characteristicKey]||r.skillRank!==r.before.skills?.[r.skill])throw Error('Queda sem distância ou ficha confirmada.');
  const pool={...buildPositivePool(r.characteristicRank,r.skillRank),difficulty:2,challenge:0,boost:0,setback:0};
  if(!eq(pool,r.pool)||!eq(r.roll?.pool,pool))throw Error('Pool da queda adulterado.');
  const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0};let i=0;
  for(const type of ['ability','proficiency','difficulty','challenge','boost','setback'])for(let j=0;j<pool[type];j++){
   const face=r.roll?.faces?.[i++],valid=diceFacesForTests[type];
   if(!face||face.type!==type||!Number.isInteger(face.index)||face.index<0||face.index>=valid.length||!eq(face.face,valid[face.index]))throw Error('Faces da queda adulteradas.');
   for(const [key,n] of Object.entries(face.face))raw[key]+=n;
  }
  if(!Array.isArray(r.roll?.faces)||r.roll.faces.length!==i||!eq({...resolveSymbols(raw),faces:r.roll.faces,pool},r.roll))throw Error('Resultado da queda adulterado.');
  const extended=['long','extreme'].includes(r.fallRange);
  if(extended && (r.impactVersion!==2||typeof r.gmTriumphReduce!=='boolean'||!['long','extreme','medium'].includes(r.effectiveFallRange)))throw Error('Contrato de queda longa/extrema inválido.');
  if(!extended && (r.impactVersion!==undefined||r.gmTriumphReduce!==undefined||r.effectiveFallRange!==undefined||r.forcedCriticalCode!==undefined))throw Error('Contrato legado de queda alterado.');
  if(extended&&r.gmTriumphReduce&&r.roll.triumph<1)throw Error('Redução de faixa sem Triunfo real.');
  effectiveRange=extended?(r.gmTriumphReduce?({long:'medium',extreme:'long'}[r.fallRange]):r.fallRange):r.fallRange;
  if(extended&&r.effectiveFallRange!==effectiveRange)throw Error('Faixa efetiva adulterada.');
  forced=['long','extreme'].includes(effectiveRange);
  rawDamage=forced?null:(RANGES[effectiveRange]?.damage??null);
  rawStrain=forced?fallingProfile(effectiveRange==='long'?'Longo':'Extremo').strain:RANGES[effectiveRange].strain;
  reducedDamage=forced?0:Math.min(rawDamage,Math.max(0,r.roll.netSuccess));
  reducedStrain=Math.min(rawStrain,Math.max(0,r.roll.netAdvantage));
 }else if(r.mode==='blast'){
  if(r.fallRange!==null||r.skill!==null||r.pool!==null||r.roll!==null||r.gmConfirmed!==false||r.characteristicKey!==null||r.characteristicRank!==null||r.skillRank!==null||r.impactVersion!==undefined||r.gmTriumphReduce!==undefined||r.effectiveFallRange!==undefined||r.forcedCriticalCode!==undefined)
   throw Error('Explosão adulterada.');
 }else throw Error('Impacto desconhecido.');
 if(!Array.isArray(r.rngDraws)||r.rngDraws.length>10||r.rngDraws.some(n=>typeof n!=='number'||!Number.isFinite(n)||n<0||n>=1))throw Error('Dados críticos adulterados.');
 let idx=0;const criticalRng=()=>{if(idx>=r.rngDraws.length)throw Error('Rolagem crítica ausente.');return r.rngDraws[idx++];};
 let damage,strained,forcedCriticalCode=null;
 if(forced){
  const f=applyForcedFall(r.before,{range:effectiveRange==='long'?'Longo':'Extremo',rng:criticalRng,netAdvantage:reducedStrain});
  damage={effectiveSoak:0,suffered:f.character.derived.wounds.current-r.before.derived.wounds.current};
  strained={character:f.character,amount:f.strain};forcedCriticalCode=f.critical?.code??null;
 }else{
  damage=applyDamage(r.before,{damage:rawDamage-reducedDamage,reason:r.mode==='blast'?'Explosão do airspeeder SWE09':'Queda em Bespin SWE09',criticalRng});
  strained=r.mode==='fall'?sufferStrain(damage.character,rawStrain-reducedStrain):{character:damage.character,amount:0};
 }
 if((r.impactVersion===2)&&(r.forcedCriticalCode!==forcedCriticalCode))throw Error('Crítico da queda adulterado.');
 if(idx!==r.rngDraws.length||!eq(strained.character,r.after)||r.rawDamage!==rawDamage||r.rawStrain!==rawStrain||r.reducedDamage!==reducedDamage||r.reducedStrain!==reducedStrain||r.soak!==damage.effectiveSoak||r.woundsAdded!==damage.suffered||r.strainAdded!==strained.amount)
  throw Error('Ferimentos, Tensão ou recibo de impacto adulterados.');
 return strained.character;
}
export function replayJewelEscapeImpact(progress){
 const receipts=progress?.operations?.receipts??{};const list=Object.entries(receipts).filter(([,r])=>r?.kind==='JEWEL_ESCAPE_IMPACT');
 if(!list.length)return null;
 const bomb=validateJewelEscapeBomb(progress);if(!bomb)throw Error('Impacto sem bomba da aventura.');
 const n=init();
 for(const [id,r] of list){
  if(!r||Object.keys(r).some(k=>!RECEIPT_KEYS.includes(k))||id!==r.id||!validId(id)||!validId(r.actorId)||r.gmConfirmed!==(r.mode==='fall')||
   (r.mode==='blast'&&(!['EXPLODED'].includes(bomb.status)||n.blastApplied))||
   (r.mode==='fall'&&(!bomb.fallDamagePending||n.fallApplied||(bomb.status==='EXPLODED'&&!n.blastApplied))))throw Error('Impacto fora da ordem ou adulterado.');
  calculate(r);
  if(r.mode==='blast')n.blastApplied=true;else n.fallApplied=true;
  n.history.push(r);
 }
 return n;
}
export function validateJewelEscapeImpact(progress){
 const actual=progress?.operations?.escapeImpact??null,expected=replayJewelEscapeImpact(progress);
 if(!eq(actual,expected))throw Error('Impacto da fuga diverge dos recibos.');return expected;
}
export function performJewelEscapeImpact(world,character,details={}){
 const w=clone(world??{}),p=w.publishedAdventure;
 if(p?.id!=='jewel-of-yavin'||p.status!=='ACTIVE'||p.currentNodeId!=='cloud-city-pursuit')throw Error('Consequência fora da fuga de Cloud City.');
 if(!validId(details.actionId)||p.operations?.receipts?.[details.actionId])throw Error('ID de impacto duplicado ou inválido.');
 const bomb=validateJewelEscapeBomb(p),previous=validateJewelEscapeImpact(p)||init();
 const mode=details.mode,fallRange=mode==='fall'?details.fallRange:null;
 if(mode==='blast'&&(bomb?.status!=='EXPLODED'||previous.blastApplied))throw Error('Explosão indisponível ou já aplicada.');
 if(mode==='fall'&&(!bomb?.fallDamagePending||previous.fallApplied||(bomb.status==='EXPLODED'&&!previous.blastApplied)))throw Error('Queda indisponível ou fora da ordem.');
 if(mode==='fall'&&!['short','medium','long','extreme'].includes(fallRange))throw Error('Faixa de queda inválida.');
 if(mode==='fall'&&details.gmConfirmed!==true)throw Error('Mestre deve confirmar a faixa real de queda.');
 const before=clone(character),actorId=String(before?.id??before?.name??'');if(!validId(actorId))throw Error('Personagem sem identificação válida.');
 const skill=mode==='fall'?details.skill:null;
 if(mode==='fall'&&!['atletismo','coordinacion'].includes(skill))throw Error('Escolha Atletismo ou Coordenação para amortecer a queda.');
 const characteristicKey=skill?SKILLS[skill]:null,characteristicRank=skill?before.characteristics?.[characteristicKey]:null,skillRank=skill?before.skills?.[skill]:null;
 if(skill&&(!Number.isInteger(characteristicRank)||!Number.isInteger(skillRank)||characteristicRank<0||skillRank<0||characteristicRank>7||skillRank>7))throw Error('Perícia de queda inválida.');
 const pool=skill?{...buildPositivePool(characteristicRank,skillRank),difficulty:2,challenge:0,boost:0,setback:0}:null;
 const rng=details.rng??Math.random,roll=pool?rollPool(pool,rng):null;
 const rngDraws=[];const criticalRng=()=>{const n=Number(rng());if(!Number.isFinite(n)||n<0||n>=1)throw Error('Fonte aleatória inválida.');rngDraws.push(n);return n;};
 const extended=mode==='fall'&&['long','extreme'].includes(fallRange);
 const gmTriumphReduce=extended&&details.gmTriumphReduce===true;
 if(gmTriumphReduce&&roll.triumph<1)throw Error('Redução de faixa exige Triunfo real e autorização do Mestre.');
 const effectiveFallRange=extended?(gmTriumphReduce?({long:'medium',extreme:'long'}[fallRange]):fallRange):fallRange;
 const forced=extended&&['long','extreme'].includes(effectiveFallRange);
 const rawDamage=mode==='blast'?10:(forced?null:RANGES[effectiveFallRange].damage);
 const rawStrain=mode==='blast'?0:(forced?fallingProfile(effectiveFallRange==='long'?'Longo':'Extremo').strain:RANGES[effectiveFallRange].strain);
 const reducedDamage=forced?0:(roll?Math.min(rawDamage,Math.max(0,roll.netSuccess)):0);
 const reducedStrain=roll?Math.min(rawStrain,Math.max(0,roll.netAdvantage)):0;
 let damage,strained,forcedCriticalCode=null;
 if(forced){
  const f=applyForcedFall(before,{range:effectiveFallRange==='long'?'Longo':'Extremo',rng:criticalRng,netAdvantage:reducedStrain});
  damage={effectiveSoak:0,suffered:f.character.derived.wounds.current-before.derived.wounds.current};
  strained={character:f.character,amount:f.strain};forcedCriticalCode=f.critical?.code??null;
 }else{
  damage=applyDamage(before,{damage:rawDamage-reducedDamage,reason:mode==='blast'?'Explosão do airspeeder SWE09':'Queda em Bespin SWE09',criticalRng});
  strained=mode==='fall'?sufferStrain(damage.character,rawStrain-reducedStrain):{character:damage.character,amount:0};
 }
 const r={id:details.actionId,kind:'JEWEL_ESCAPE_IMPACT',mode,actorId,fallRange,gmConfirmed:mode==='fall',skill,characteristicKey,characteristicRank,skillRank,pool,roll,before,after:clone(strained.character),rngDraws,rawDamage,rawStrain,reducedDamage,reducedStrain,soak:damage.effectiveSoak,woundsAdded:damage.suffered,strainAdded:strained.amount};
 if(extended)Object.assign(r,{impactVersion:2,gmTriumphReduce,effectiveFallRange,forcedCriticalCode});
 calculate(r);
 p.operations.receipts[r.id]=r;
 const impact=clone(previous);impact[mode==='blast'?'blastApplied':'fallApplied']=true;impact.history.push(r);p.operations.escapeImpact=impact;
 validateJewelEscapeImpact(p);
 return {world:w,character:strained.character,adjudication:r,impact:clone(impact)};
}
