import { SKILLS } from './character.mjs';
const FACES = Object.freeze({
  ability: [{}, {success:1}, {success:1}, {success:2}, {advantage:1}, {advantage:1}, {success:1,advantage:1}, {advantage:2}],
  proficiency: [{}, {success:1}, {success:1}, {success:2}, {success:2}, {advantage:1}, {success:1,advantage:1}, {success:1,advantage:1}, {success:1,advantage:1}, {advantage:2}, {advantage:2}, {triumph:1}],
  difficulty: [{}, {failure:1}, {failure:2}, {threat:1}, {threat:1}, {threat:1}, {threat:2}, {failure:1,threat:1}],
  challenge: [{}, {failure:1}, {failure:1}, {failure:2}, {failure:2}, {threat:1}, {threat:1}, {failure:1,threat:1}, {failure:1,threat:1}, {threat:2}, {threat:2}, {despair:1}],
  boost: [{}, {}, {success:1}, {success:1,advantage:1}, {advantage:2}, {advantage:1}],
  setback: [{}, {}, {failure:1}, {failure:1}, {threat:1}, {threat:1}]
});
function asRank(value,label){const n=Number(value); if(!Number.isInteger(n)||n<0||n>7) throw new Error(`${label} deve estar entre 0 e 7.`); return n;}
export function buildPositivePool(characteristic,skill){characteristic=asRank(characteristic,'Característica'); skill=asRank(skill,'Habilidade'); const total=Math.max(characteristic,skill), upgrades=Math.min(characteristic,skill); return {ability:total-upgrades,proficiency:upgrades};}
export function resolveSymbols(raw={}){const triumph=raw.triumph??0,despair=raw.despair??0; const successTotal=(raw.success??0)+triumph,failureTotal=(raw.failure??0)+despair; const netSuccess=successTotal-failureTotal,netAdvantage=(raw.advantage??0)-(raw.threat??0); return {success:netSuccess>0,netSuccess,netAdvantage,triumph,despair,raw:{success:raw.success??0,failure:raw.failure??0,advantage:raw.advantage??0,threat:raw.threat??0,triumph,despair}};}
export function rollPool(pool,rng=Math.random){const raw={success:0,failure:0,advantage:0,threat:0,triumph:0,despair:0},faces=[]; for(const type of ['ability','proficiency','difficulty','challenge','boost','setback']){const count=Number(pool[type]??0); if(!Number.isInteger(count)||count<0||count>12) throw new Error(`Quantidade inválida de dados: ${type}`); for(let i=0;i<count;i++){const die=FACES[type],index=Math.min(die.length-1,Math.floor(rng()*die.length)),face=die[index]; faces.push({type,index,face}); for(const [symbol,value] of Object.entries(face)) raw[symbol]+=value;}} return {...resolveSymbols(raw),faces,pool:{...pool}};}

export function buildAssistedCheckPool({primaryActor,assistantActor,skillKey=null,characteristicKey=null,skilled=null,baseBoost=0}={}){
  if(!primaryActor||typeof primaryActor!=='object')throw new Error('Personagem principal inválido.');
  if(!assistantActor||typeof assistantActor!=='object')throw new Error('Assistente inválido.');
  const charKey=characteristicKey||(skillKey?SKILLS[skillKey]:null);
  if(!charKey)throw new Error('Característica da perícia não especificada.');
  const primaryChar=asRank(primaryActor.characteristics?.[charKey]??0,'Característica principal');
  const primarySkill=asRank(primaryActor.skills?.[skillKey]??0,'Perícia principal');
  const assistantChar=asRank(assistantActor.characteristics?.[charKey]??0,'Característica do assistente');
  const assistantSkill=asRank(assistantActor.skills?.[skillKey]??0,'Perícia do assistente');
  const canBeSkilled=assistantChar>primaryChar||assistantSkill>primarySkill;
  const isSkilled=skilled===null?canBeSkilled:Boolean(skilled);
  let boost=Math.max(0,Number(baseBoost)||0);
  const characteristicUsed=isSkilled?Math.max(primaryChar,assistantChar):primaryChar;
  const skillUsed=isSkilled?Math.max(primarySkill,assistantSkill):primarySkill;
  if(!isSkilled)boost+=1;
  return {...buildPositivePool(characteristicUsed,skillUsed),boost,isSkilled,assistanceType:isSkilled?'skilled':'unskilled',characteristicUsed,skillUsed,primary:{characteristic:primaryChar,skill:primarySkill},assistant:{characteristic:assistantChar,skill:assistantSkill}};
}

export function buildOpposedNegativePool(opponentCharacteristic,opponentSkill){
  const characteristic=asRank(opponentCharacteristic,'Característica da oposição');
  const skill=asRank(opponentSkill,'Perícia da oposição');
  const total=Math.max(characteristic,skill),upgrades=Math.min(characteristic,skill);
  return {difficulty:total-upgrades,challenge:upgrades,opposition:{characteristic,skill,total,upgrades}};
}

export function compareCompetitiveResults(results=[]){
  const rows=Array.isArray(results)?results.filter(Boolean):[];
  const successful=rows.filter(r=>r.check?.success===true);
  if(!successful.length)return {winnerId:null,draw:true,reason:'NO_SUCCESS',ranking:rows.map(r=>({id:r.id,label:r.label,check:r.check}))};
  const score=r=>[Number(r.check?.netSuccess||0),Number(r.check?.triumph||0),Number(r.check?.netAdvantage||0)];
  const sorted=[...successful].sort((a,b)=>{const A=score(a),B=score(b);for(let i=0;i<A.length;i++){if(A[i]!==B[i])return B[i]-A[i]}return 0});
  const top=score(sorted[0]);const tied=sorted.filter(r=>{const s=score(r);return s.every((v,i)=>v===top[i])});
  return {winnerId:tied.length===1?sorted[0].id:null,draw:tied.length!==1,reason:tied.length===1?'WINNER':'TIED_AFTER_TIEBREAKERS',ranking:sorted.map(r=>({id:r.id,label:r.label,check:r.check}))};
}

export const diceFacesForTests=FACES;
