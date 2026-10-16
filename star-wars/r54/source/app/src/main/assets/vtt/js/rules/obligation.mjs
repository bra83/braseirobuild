const clone=v=>structuredClone(v);
function int(v,label,{min=0,max=9999}={}){const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error(`${label} inválido.`);return n;}
export function normalizedObligationEntries(group,character=null){
  const fromGroup=Array.isArray(group?.obligationEntries)?group.obligationEntries.map((e,i)=>({id:String(e.id??`group_${i}`),value:Math.max(0,Number(e.value)||0),member:String(e.member??''),type:String(e.type??''),description:String(e.description??''),origin:'group'})):[];
  if(fromGroup.length) return fromGroup;
  return (character?.obligations??[]).map((e,i)=>({id:String(e.id??`character_${i}`),value:Math.max(0,Number(e.value??e.startingValue)||0),member:String(e.member??character?.name??''),type:String(e.type??''),description:String(e.description??''),origin:'character'}));
}
export function obligationTotal(group,character=null){return normalizedObligationEntries(group,character).reduce((s,e)=>s+e.value,0);}
export function obligationCheck(group,character,{rng=Math.random,roll=null}={}){
  const entries=normalizedObligationEntries(group,character);const total=entries.reduce((s,e)=>s+e.value,0);const result=roll===null?Math.floor(Number(rng())*100)+1:int(roll,'Rolagem de Obligation',{min:1,max:100});const triggered=total>0&&result<=total;let selected=null,cursor=0;if(triggered){for(const e of entries){cursor+=e.value;if(result<=cursor){selected=e;break;}}}
  const doubles=result%11===0;const groupPenalty=triggered?(doubles?2:1):0;const characterSpecific=Boolean(selected&&selected.member&&character?.name&&selected.member.trim().toLocaleLowerCase('pt-BR')===String(character.name).trim().toLocaleLowerCase('pt-BR'));const characterPenalty=triggered?(characterSpecific?(doubles?4:2):groupPenalty):0;
  return {roll:result,total,triggered,doubles,selected:clone(selected),triggeredEntry:clone(selected),groupPenalty,allStrainThresholdPenalty:groupPenalty,characterPenalty,playerStrainThresholdPenalty:characterPenalty,characterSpecific,progressionBlocked:total>100,entries:clone(entries)};
}
export function applyObligationPenalty(character,result){
  const next=clone(character);if(!next?.derived?.strain)throw new Error('Ficha sem limiar de Tensão.');
  const prior=Number(next.derived.strain.threshold);const penalty=Math.max(0,Number(result?.characterPenalty??result?.playerStrainThresholdPenalty??0));
  next.sessionEffects={...(next.sessionEffects??{})};
  if(penalty>0)next.sessionEffects.obligationStrainThresholdPenalty=penalty;else delete next.sessionEffects.obligationStrainThresholdPenalty;
  if(!Object.keys(next.sessionEffects).length)delete next.sessionEffects;
  return {character:next,priorThreshold:prior,penalty,appliedThreshold:Math.max(0,prior-penalty)};
}
export function restoreObligationPenalty(character,sessionObligation){
  const next=clone(character);if(!next?.derived?.strain)return next;
  // Migração segura de sessões salvas pela implementação anterior, que reduzia o limiar-base.
  const prior=Number(sessionObligation?.priorThreshold),applied=Number(sessionObligation?.appliedThreshold);
  if(Number.isInteger(prior)&&prior>=0&&Number.isInteger(applied)&&next.derived.strain.threshold===applied&&prior>applied)next.derived.strain.threshold=prior;
  if(next.sessionEffects){delete next.sessionEffects.obligationStrainThresholdPenalty;if(!Object.keys(next.sessionEffects).length)delete next.sessionEffects;}
  return next;
}
export function rollObligationForSession(group,character,options={}){return obligationCheck(group,character,options);}
export function obligationProgressionLocked(group,character=null){return obligationTotal(group,character)>100;}

