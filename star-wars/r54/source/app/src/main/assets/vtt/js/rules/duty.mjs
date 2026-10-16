const clone=v=>structuredClone(v);
function int(v,label,{min=0,max=9999}={}){const n=Number(v);if(!Number.isInteger(n)||n<min||n>max)throw new Error(`${label} inválido.`);return n;}
export function normalizedDutyEntries(group,character=null){
  const fromGroup=Array.isArray(group?.dutyEntries)?group.dutyEntries.map((e,i)=>({id:String(e.id??`group_${i}`),value:Math.max(0,Number(e.value)||0),member:String(e.member??''),type:String(e.type??''),description:String(e.description??''),origin:'group'})):[];
  if(fromGroup.length) return fromGroup;
  return (character?.duties??[]).map((e,i)=>({id:String(e.id??`character_${i}`),value:Math.max(0,Number(e.value??e.startingValue)||0),member:String(e.member??character?.name??''),type:String(e.type??''),description:String(e.description??''),origin:'character'}));
}
export function dutyTotal(group,character=null){return normalizedDutyEntries(group,character).reduce((s,e)=>s+e.value,0);}
export function dutyCheck(group,character,{rng=Math.random,roll=null}={}){
  const entries=normalizedDutyEntries(group,character); const total=entries.reduce((s,e)=>s+e.value,0);
  const result=roll===null?Math.floor(Number(rng())*100)+1:int(roll,'Rolagem de Duty',{min:1,max:100});
  const triggered=total>0&&result<=total; let selected=null,cursor=0;
  if(triggered){for(const e of entries){cursor+=e.value;if(result<=cursor){selected=e;break;}}}
  const doubles=triggered&&result%11===0;
  const groupBonus=triggered?(doubles?2:1):0;
  const characterSpecific=Boolean(selected&&selected.member&&character?.name&&selected.member.trim().toLocaleLowerCase('pt-BR')===String(character.name).trim().toLocaleLowerCase('pt-BR'));
  const characterBonus=triggered?(characterSpecific?(doubles?4:2):groupBonus):0;
  return {roll:result,total,triggered,doubles,selected:clone(selected),triggeredEntry:clone(selected),groupWoundThresholdBonus:groupBonus,characterWoundThresholdBonus:characterBonus,characterSpecific,entries:clone(entries)};
}
export function applyDutyBonus(character,result){
  const next=clone(character); if(!next?.derived?.wounds) throw new Error('Ficha sem limiar de Ferimentos.');
  const bonus=Math.max(0,Number(result?.characterWoundThresholdBonus??0));
  next.sessionEffects={...(next.sessionEffects??{})};
  if(bonus>0) next.sessionEffects.dutyWoundThresholdBonus=bonus; else delete next.sessionEffects.dutyWoundThresholdBonus;
  if(!Object.keys(next.sessionEffects).length) delete next.sessionEffects;
  return {character:next,bonus,baseThreshold:Number(next.derived.wounds.threshold),effectiveThreshold:Number(next.derived.wounds.threshold)+bonus};
}
export function restoreDutyBonus(character){
  const next=clone(character); if(next?.sessionEffects){delete next.sessionEffects.dutyWoundThresholdBonus;if(!Object.keys(next.sessionEffects).length)delete next.sessionEffects;} return next;
}
export function addDutyProgress(group,entryId,amount){
  const next=clone(group); const n=int(amount,'Ganho de Duty',{min:0,max:1000});
  if(!Array.isArray(next.dutyEntries)) next.dutyEntries=[];
  const entry=next.dutyEntries.find(e=>String(e.id)===String(entryId)); if(!entry) throw new Error('Entrada de Duty não encontrada.');
  entry.value=Math.max(0,Number(entry.value)||0)+n; return {group:next,entry:clone(entry),total:dutyTotal(next)};
}
export function contributionMilestoneReady(group,character=null){return dutyTotal(group,character)>=100;}
export function resolveContributionMilestone(group){
  const next=clone(group); const total=dutyTotal(next); if(total<100) throw new Error('Duty total ainda não atingiu 100.');
  if(next.pendingAllianceReward) throw new Error('Existe uma recompensa da Aliança pendente do marco anterior.');
  next.contributionRank=Math.max(0,Number(next.contributionRank)||0)+1;
  next.dutyEntries=(next.dutyEntries??[]).map(e=>({...e,value:0}));
  next.pendingAllianceReward={
    id:`alliance_reward_cr${next.contributionRank}`,
    contributionRank:next.contributionRank,
    maxRarity:3+next.contributionRank,
    choices:['equipment','vehicle','strategic'],
    status:'pending'
  };
  return {group:next,previousDutyTotal:total,contributionRank:next.contributionRank,reward:clone(next.pendingAllianceReward)};
}
